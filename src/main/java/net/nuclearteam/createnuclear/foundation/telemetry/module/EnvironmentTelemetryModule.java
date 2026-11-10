package net.nuclearteam.createnuclear.foundation.telemetry.module;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.EventBuilder;
import net.nuclearteam.createnuclear.foundation.telemetry.Telemetry;
import net.nuclearteam.createnuclear.foundation.telemetry.api.ITelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.api.RegisterTelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryContext;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.metrics.MetricSet;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

/**
 * Built-in module {@value #ID}: the context of the server during a session, never data about the
 * user's machine (README, decision D10).
 * <ul>
 *   <li>at session start: versions of Create Nuclear, Minecraft and NeoForge, dedicated or
 *       integrated server, player and mod counts, then the sorted mod list;</li>
 *   <li>on metrics ticks: {@code mspt} (average over the last 100 ticks), {@code tps} (derived
 *       from it, at most 20), {@code memory_used_mb} (heap used by the game, never the machine's
 *       memory), {@code players}, {@code loaded_chunks};</li>
 *   <li>on health ticks: invariant {@value #TICK_OVER_BUDGET};</li>
 *   <li>every tick: an event when the number of online players changes.</li>
 * </ul>
 * Every event and metric is about {@link Subject#SERVER}. One instance for the whole game; hooks
 * run on the server thread.
 */
@RegisterTelemetryModule
public final class EnvironmentTelemetryModule implements ITelemetryModule {
    /** Module id, used for {@code createnuclear-telemetry-environment.toml} and commands. */
    public static final String ID = "environment";
    /** Invariant violated while the average tick time exceeds {@code tick_budget_ms}. */
    public static final String TICK_OVER_BUDGET = "tick_over_budget";

    private static final double NANOS_PER_MS = 1_000_000.0;
    private static final long BYTES_PER_MB = 1024L * 1024;

    /** Categories of the module, in config order; each gets a {@code level_<id>} line. */
    public enum Category implements TelemetryCategory {
        /** Written once per session. */
        SERVER(TelemetryLevel.INFO, "Versions, server type and mod list, once per session."),
        /** Number of online players; never names. */
        PLAYERS(TelemetryLevel.INFO, "Changes of the number of online players (never names).");

        private final TelemetryLevel defaultLevel;
        private final String description;

        Category(TelemetryLevel defaultLevel, String description) {
            this.defaultLevel = defaultLevel;
            this.description = description;
        }

        @Override
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public TelemetryLevel defaultLevel() {
            return defaultLevel;
        }

        @Override
        public String description() {
            return description;
        }
    }

    /** Option {@code include_java_version}; replaced in {@link #defineOptions}. */
    private BooleanSupplier includeJavaVersion = () -> false;
    /** Option {@code tick_budget_ms}; replaced in {@link #defineOptions}. */
    private DoubleSupplier tickBudgetMs = () -> 50.0;
    /** Player count seen at the previous tick; reset at session start. Server thread only. */
    private int lastPlayerCount;

    /** Called by the registry, once per game. */
    public EnvironmentTelemetryModule() {}

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Server context: versions, mods, tick time, memory used by the game, player count.";
    }

    @Override
    public List<? extends TelemetryCategory> categories() {
        return List.of(Category.values());
    }

    /** Declares {@code include_java_version} and {@code tick_budget_ms}. */
    @Override
    public void defineOptions(Options options) {
        includeJavaVersion = options.bool("include_java_version", false,
                "Add the Java version to the 'environment' event. Off by default: it describes the user's machine.");
        tickBudgetMs = options.decimal("tick_budget_ms", 50.0, 1.0, 10_000.0,
                "Average tick time over the last 100 ticks, in milliseconds, above which the " + TICK_OVER_BUDGET
                        + " invariant is violated. 50 ms = 20 TPS.");
    }

    /** Writes the {@code environment} and {@code mods} events, and resets the player count. */
    @Override
    public void onSessionStart(TelemetryContext context) {
        MinecraftServer server = context.server();
        lastPlayerCount = server.getPlayerCount();

        EventBuilder event = Telemetry.info(Category.SERVER)
                .msg("environment")
                .attr("createnuclear", modVersion(CreateNuclear.MOD_ID))
                .attr("minecraft", SharedConstants.getCurrentVersion().getName())
                .attr("neoforge", modVersion("neoforge"))
                .attr("server", server.isDedicatedServer() ? "dedicated" : "integrated")
                .attr("players", lastPlayerCount)
                .attr("mod_count", ModList.get().getMods().size());
        if (includeJavaVersion.getAsBoolean()) event.attr("java", System.getProperty("java.version"));
        event.emit();

        Telemetry.info(Category.SERVER)
                .msg("mods")
                .attrLazy("mods", EnvironmentTelemetryModule::modList)
                .emit();
    }

    /**
     * Emits an event when the player count changes; writes the metrics and checks
     * {@value #TICK_OVER_BUDGET} only on the ticks where they are due.
     */
    @Override
    public void onServerTick(TelemetryContext context) {
        MinecraftServer server = context.server();

        int players = server.getPlayerCount();
        if (players != lastPlayerCount) {
            Telemetry.info(Category.PLAYERS)
                    .msg("players {} -> {}", lastPlayerCount, players)
                    .attr("online", players)
                    .emit();
            lastPlayerCount = players;
        }

        boolean metricsDue = Telemetry.isMetricsDue(ID);
        boolean healthDue = Telemetry.isHealthCheckDue(ID);
        if (!metricsDue && !healthDue) return;

        double mspt = server.getAverageTickTimeNanos() / NANOS_PER_MS;
        if (metricsDue) {
            Runtime runtime = Runtime.getRuntime();
            MetricSet m = Telemetry.metrics(ID, Subject.SERVER);
            m.gauge("mspt", mspt);
            m.gauge("tps", mspt <= 0 ? 20.0 : Math.min(20.0, 1000.0 / mspt));
            m.gauge("memory_used_mb", (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB);
            m.gauge("players", players);
            m.gauge("loaded_chunks", loadedChunks(server));
        }
        if (healthDue) {
            double budget = tickBudgetMs.getAsDouble();
            Telemetry.health(ID, Subject.SERVER)
                    .check(TICK_OVER_BUDGET, mspt > budget,
                            () -> "mspt=" + String.format(Locale.ROOT, "%.2f", mspt) + " budget=" + budget)
                    .commit();
        }
    }

    /** Version of a loaded mod, or {@code "unknown"}. */
    private static String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    /**
     * Every loaded mod as {@code id@version}, sorted by id, joined by {@code ,}. Not {@code +}:
     * versions may contain one ({@code 1.0.82+mc1.21.1}), never a comma.
     */
    private static String modList() {
        return ModList.get().getMods().stream()
                .sorted(Comparator.comparing(IModInfo::getModId))
                .map(m -> m.getModId() + "@" + m.getVersion())
                .collect(Collectors.joining(","));
    }

    /** Loaded chunks over every dimension; one cheap call per dimension, no world iteration. */
    private static int loadedChunks(MinecraftServer server) {
        int total = 0;
        for (ServerLevel level : server.getAllLevels()) total += level.getChunkSource().getLoadedChunksCount();
        return total;
    }
}
