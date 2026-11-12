package net.nuclearteam.createnuclear.foundation.telemetry;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.api.BuiltinCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.api.ITelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.api.ITelemetrySink;
import net.nuclearteam.createnuclear.foundation.telemetry.api.SessionInfo;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryContext;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryEvent;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryConfigs;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryCoreConfig;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryModuleConfig;
import net.nuclearteam.createnuclear.foundation.telemetry.health.HealthEventEmitter;
import net.nuclearteam.createnuclear.foundation.telemetry.health.HealthTracker;
import net.nuclearteam.createnuclear.foundation.telemetry.metrics.MetricSet;
import net.nuclearteam.createnuclear.foundation.telemetry.privacy.PrivacyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * A time-limited telemetry recording. At most one session runs at a time.
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>{@link #start}: checks the duration and modules, creates the session directory, opens the
 *       enabled sinks, installs itself in the {@link Telemetry} facade, emits {@code session started},
 *       then {@code config} (the settings that differ from their default), and calls
 *       {@link ITelemetryModule#onSessionStart};</li>
 *   <li>every server tick: calls {@link ITelemetryModule#onServerTick}, writes the metrics on
 *       metrics ticks; every {@value #SINK_FLUSH_INTERVAL_TICKS} ticks, writes a
 *       {@code config changed} event per changed setting ({@link #checkConfig}) and flushes the
 *       sinks; stops once the duration has elapsed;</li>
 *   <li>{@link #stop}: writes the last metrics and the health summary of every subject, calls
 *       {@link ITelemetryModule#onSessionStop}, emits {@code session stopped}, uninstalls itself
 *       and closes the sinks.</li>
 * </ol>
 * A sink or module that throws is removed from the session; the game continues. The session's
 * own events use the reserved module id {@value #SESSION_MODULE} and are never level-filtered.
 */
public final class TelemetrySession implements TelemetryRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateNuclear/Telemetry");

    /** Module id of the session's own events; reserved, no module may use it. */
    public static final String SESSION_MODULE = "telemetry";
    /** Category id of the session's own events. */
    public static final String SESSION_CATEGORY = "session";
    /** Parent of every session directory, relative to the game directory. */
    public static final Path BASE_DIRECTORY = Path.of("debug", CreateNuclear.MOD_ID, "telemetry");
    /** Interval between two {@link ITelemetrySink#flush} calls, in server ticks: one second at 20 TPS. */
    public static final int SINK_FLUSH_INTERVAL_TICKS = 20;

    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int TICKS_PER_SECOND = 20;

    @Nullable
    private static volatile TelemetrySession current;

    private final MinecraftServer server;
    private final SessionInfo info;
    private final Map<String, ModuleRun> modules;
    private final List<SinkRun> sinks; // guarded by itself
    @Nullable
    private final Consumer<Component> notifier;
    private final AtomicLong eventCount = new AtomicLong();

    private volatile long tick;
    private volatile boolean stopping;
    private volatile boolean sinksClosed;
    private volatile boolean noSinkLeft;
    /**
     * Last known value of every tracked setting, keyed {@code <config>.<key>}; compared by
     * {@link #checkConfig}. Server thread only.
     */
    private Map<String, Object> configSnapshot = Map.of();

    private final PrivacyFilter privacy;

    private TelemetrySession(MinecraftServer server, SessionInfo info, List<ModuleSelection> selected,
                             List<SinkRun> openedSinks, @Nullable Consumer<Component> notifier) {
        this.server = server;
        this.info = info;
        this.notifier = notifier;
        this.sinks = new ArrayList<>(openedSinks);

        Map<String, ModuleRun> runs = new LinkedHashMap<>();
        for (ModuleSelection s : selected) runs.put(s.entry().id(), new ModuleRun(s));
        this.modules = Collections.unmodifiableMap(runs);

        this.privacy = PrivacyFilter.forServer(server);
    }

    // ---- Start / stop ---------------------------------------------------------------------------

    /**
     * Starts a session. Never throws: every problem is reported in the result.
     *
     * @param server           the running server
     * @param seconds          requested duration, or {@code null} for {@code default_session_seconds};
     *                         longer than {@code max_session_seconds} is shortened, with a warning
     * @param moduleSpec       {@code "all"}, {@code null}, or module ids joined by {@code +}
     * @param launchedByPlayer {@code true} if a player started it; only "player" or "console" is recorded
     * @param notifier         receives messages for the launcher (e.g. automatic stop), or {@code null}.
     *                         Kept in memory only
     * @return the started session and warnings, or an error
     */
    public static synchronized StartResult start(MinecraftServer server, @Nullable Integer seconds,
                                                 @Nullable String moduleSpec, boolean launchedByPlayer,
                                                 @Nullable Consumer<Component> notifier) {
        if (current != null) return StartResult.failure("A telemetry session is already running.");
        TelemetryCoreConfig core = TelemetryConfigs.core();
        if (core == null || !TelemetryRegistry.isBootstrapped())
            return StartResult.failure("Telemetry is not initialised.");

        List<String> warnings = new ArrayList<>();
        List<SinkRun> opened = new ArrayList<>();
        SessionInfo info = null;
        Path createdDirectory = null;
        try {
            int max = core.maxSessionSeconds.get();
            int duration = seconds != null ? seconds : core.defaultSessionSeconds.get();
            if (duration < 1) return StartResult.failure("The duration must be at least 1 second.");
            if (duration > max) {
                warnings.add("Duration shortened from " + duration + " s to max_session_seconds = " + max + " s.");
                duration = max;
            }

            List<ModuleSelection> selected = new ArrayList<>();
            String error = selectModules(moduleSpec, selected, warnings);
            if (error != null) return StartResult.failure(error);
            if (selected.isEmpty()) return StartResult.failure("No telemetry module to record.");

            Path root = server.getServerDirectory().resolve(BASE_DIRECTORY);
            String id = uniqueId(root, LocalDateTime.now().format(ID_FORMAT));
            Path directory = root.resolve(id).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            createdDirectory = directory;

            info = new SessionInfo(id, directory, BASE_DIRECTORY.resolve(id), Instant.now(),
                    (long) duration * TICKS_PER_SECOND,
                    selected.stream().map(s -> s.entry().id()).toList(),
                    launchedByPlayer ? "player" : "console");

            openSinks(core, info, opened, warnings);
            if (opened.isEmpty()) {
                deleteIfEmpty(createdDirectory);
                return StartResult.failure("No telemetry sink could be opened. Check the sink_<id> lines of "
                        + TelemetryCoreConfig.FILE_NAME + ".");
            }

            TelemetrySession session = new TelemetrySession(server, info, selected, opened, notifier);
            Telemetry.install(session);
            current = session;

            session.emitLifecycle(TelemetryLevel.INFO, "session started", attributes(
                    "session", id,
                    "duration_s", duration,
                    "modules", String.join("+", info.modules()),
                    "sinks", String.join("+", session.sinkIds()),
                    "launcher", info.launcher()));
            session.emitConfig();
            for (ModuleRun run : session.modules.values())
                run.call("onSessionStart", m -> m.onSessionStart(run));

            return StartResult.success(session, warnings);
        } catch (Throwable t) {
            LOGGER.error("Cannot start a telemetry session", t);
            if (current == null) {
                for (SinkRun run : opened) closeQuietly(run, info);
                deleteIfEmpty(createdDirectory);
            }
            return StartResult.failure("Cannot start the telemetry session (" + t.getClass().getSimpleName() + ").");
        }
    }

    /**
     * Stops the running session, if any.
     *
     * @param reason why the session stops
     * @return {@code false} if no session was running
     */
    public static boolean stopCurrent(StopReason reason) {
        TelemetrySession session = current;
        if (session == null) return false;
        session.stop(reason);
        return true;
    }

    /**
     * Returns the running session.
     *
     * @return the session, or {@code null} if none is running
     */
    @Nullable
    public static TelemetrySession current() {
        return current;
    }

    /**
     * Stops this session. Never throws; calling it again does nothing.
     *
     * @param reason why the session stops
     */
    public void stop(StopReason reason) {
        synchronized (TelemetrySession.class) {
            if (stopping) return;
            stopping = true;
        }
        try {
            for (ModuleRun run : modules.values()) if (run.active) dumpMetrics(run);
            for (ModuleRun run : modules.values()) emitHealthSummary(run);
            for (ModuleRun run : modules.values()) run.call("onSessionStop", m -> m.onSessionStop(run));
            emitLifecycle(TelemetryLevel.INFO, "session stopped", attributes(
                    "reason", reason.id(),
                    "duration_ticks", tick,
                    "events", eventCount.get()));
        } catch (Throwable t) {
            LOGGER.error("Telemetry session failed while stopping", t);
        } finally {
            Telemetry.uninstall(this);
            synchronized (TelemetrySession.class) {
                if (current == this) current = null;
            }
            closeSinks();
            notifyStop(reason);
        }
    }

    // ---- Server hooks (called by TelemetryServerEvents) -----------------------------------------

    /**
     * End of a server tick. Never throws.
     *
     * @param server the ticking server
     */
    static void onServerTick(MinecraftServer server) {
        TelemetrySession session = current;
        if (session == null || session.server != server) return;
        try {
            session.endOfTick();
        } catch (Throwable t) {
            LOGGER.error("Telemetry session failed during a tick", t);
            session.stop(StopReason.ERROR);
        }
    }

    /**
     * The server is stopping: the session stops with it. Never throws.
     *
     * @param server the stopping server
     */
    static void onServerStopping(MinecraftServer server) {
        TelemetrySession session = current;
        if (session != null && session.server == server) session.stop(StopReason.SERVER_STOPPING);
    }

    private void endOfTick() {
        if (stopping) return;
        long now = tick;
        for (ModuleRun run : modules.values()) run.call("onServerTick", m -> m.onServerTick(run));
        for (ModuleRun run : modules.values())
            if (run.active && now % run.metricsInterval == 0) dumpMetrics(run);
        if (now % SINK_FLUSH_INTERVAL_TICKS == 0) {
            checkConfig();
            flushSinks();
        }
        tick = now + 1;

        if (noSinkLeft) stop(StopReason.NO_SINK);
        else if (tick >= info.durationTicks()) stop(StopReason.DURATION);
    }

    // ---- TelemetryRecorder ----------------------------------------------------------------------

    @Override
    public long tick() {
        return tick;
    }

    @Override
    public boolean isRecording(String moduleId) {
        ModuleRun run = modules.get(moduleId);
        return run != null && run.active;
    }

    @Override
    public TelemetryLevel level(String moduleId, String categoryId) {
        ModuleRun run = modules.get(moduleId);
        return run == null || !run.active ? TelemetryLevel.OFF : run.config.level(categoryId);
    }

    @Override
    public void record(TelemetryEvent event) {
        if (sinksClosed) return;
        TelemetryEvent cleaned;
        try {
            cleaned = clean(event);
        } catch (Throwable t) {
            Telemetry.reportFailure("privacy filter", t); // never write an unfiltered event
            return;
        }

        eventCount.incrementAndGet();
        synchronized (sinks) {
            Iterator<SinkRun> it = sinks.iterator();
            while (it.hasNext()) {
                SinkRun run = it.next();
                try {
                    run.sink().accept(cleaned);
                } catch (Throwable t) {
                    it.remove();
                    LOGGER.error("Telemetry sink '{}' failed and was removed from the session", run.id(), t);
                    closeQuietly(run, info);
                }
            }
            if (sinks.isEmpty()) noSinkLeft = true;
        }
    }

    /** Calls {@link ITelemetrySink#flush} on every sink; a sink that throws is removed, like in {@link #record}. */
    private void flushSinks() {
        synchronized (sinks) {
            if (sinksClosed) return;
            Iterator<SinkRun> it = sinks.iterator();
            while (it.hasNext()) {
                SinkRun run = it.next();
                try {
                    run.sink().flush();
                } catch (Throwable t) {
                    it.remove();
                    LOGGER.error("Telemetry sink '{}' failed to flush and was removed from the session", run.id(), t);
                    closeQuietly(run, info);
                }
            }
            if (sinks.isEmpty()) noSinkLeft = true;
        }
    }

    @Override
    public MetricSet metrics(String moduleId, Subject subject) {
        ModuleRun run = modules.get(moduleId);
        if (run == null || !run.active) return MetricSet.NOOP;
        synchronized (run.metrics) {
            return run.metrics.computeIfAbsent(subject, s -> new MetricSet(moduleId, s));
        }
    }

    @Override
    public boolean isMetricsDue(String moduleId) {
        ModuleRun run = modules.get(moduleId);
        return run != null && run.active && tick % run.metricsInterval == 0;
    }

    @Override
    public boolean isHealthCheckDue(String moduleId) {
        ModuleRun run = modules.get(moduleId);
        return run != null && run.active && tick % run.healthInterval == 0;
    }

    @Nullable
    @Override
    public HealthTracker health(String moduleId) {
        ModuleRun run = modules.get(moduleId);
        return run == null || !run.active ? null : run.tracker;
    }

    // ---- Status (for /cn telemetry status and stats) --------------------------------------------

    /**
     * Returns the session information.
     *
     * @return the information given to sinks and modules
     */
    public SessionInfo info() {
        return info;
    }

    /**
     * Returns the number of recorded events.
     *
     * @return events sent to the sinks so far
     */
    public long eventCount() {
        return eventCount.get();
    }

    /**
     * Returns the remaining duration.
     *
     * @return ticks left before the automatic stop, at least 0
     */
    public long remainingTicks() {
        return Math.max(0, info.durationTicks() - tick);
    }

    /**
     * Returns the modules still recorded.
     *
     * @return ids of the modules not removed after a failure
     */
    public List<String> recordedModules() {
        return modules.values().stream().filter(r -> r.active).map(ModuleRun::moduleId).toList();
    }

    /**
     * Returns the sinks still in use.
     *
     * @return ids of the sinks not removed after a failure
     */
    public List<String> sinkIds() {
        synchronized (sinks) {
            return sinks.stream().map(SinkRun::id).toList();
        }
    }

    /**
     * Returns the health of every subject of a module.
     *
     * @param moduleId module id
     * @return one entry per checked subject; empty if the module is not part of the session
     */
    public List<HealthTracker.SubjectHealth> healthSnapshot(String moduleId) {
        ModuleRun run = modules.get(moduleId);
        return run == null ? List.of() : run.tracker.snapshot();
    }

    /**
     * Returns the session directory as shown to players.
     *
     * @return the directory relative to the game directory, with {@code /} separators
     */
    public String displayDirectory() {
        return info.displayDirectory().toString().replace('\\', '/');
    }

    // ---- Internals ------------------------------------------------------------------------------

    /** Removes personal data; see {@link PrivacyFilter}. Called by {@link #record} before any sink. */
    private TelemetryEvent clean(TelemetryEvent event) {
        return privacy.clean(event);
    }

    /**
     * Writes the metrics of every subject of a module updated since the last dump, then resets their
     * histograms. A subject that stopped updating (a broken reactor) is skipped: see {@link MetricSet#drain}.
     */
    private void dumpMetrics(ModuleRun run) {
        try {
            List<Map.Entry<Subject, MetricSet>> sets;
            synchronized (run.metrics) {
                sets = new ArrayList<>(run.metrics.entrySet());
            }
            for (Map.Entry<Subject, MetricSet> e : sets) {
                Map<String, Object> values = e.getValue().drain();
                if (values.isEmpty()) continue;
                EventBuilder b = Telemetry.event(run.moduleId(), BuiltinCategory.METRICS, TelemetryLevel.INFO)
                        .subject(e.getKey())
                        .msg("metrics");
                for (Map.Entry<String, Object> v : values.entrySet()) b.attr(v.getKey(), v.getValue());
                b.emit();
            }
        } catch (Throwable t) {
            LOGGER.error("Cannot write the metrics of telemetry module '{}'", run.moduleId(), t);
        }
    }

    /**
     * Emits one {@code health summary} per subject: WARN if a check failed, INFO otherwise.
     * <p>
     * Sent straight to the sinks rather than through the facade, so that a module removed after a
     * failure still gets its summary (the facade answers OFF for it). The module's
     * {@code level_health} is still honoured. A removed module's summary carries {@code module_removed=true}.
     */
    private void emitHealthSummary(ModuleRun run) {
        try {
            TelemetryLevel threshold = run.config.level(BuiltinCategory.HEALTH.id());
            for (HealthTracker.SubjectHealth h : run.tracker.snapshot()) {
                TelemetryLevel level = h.failedChecks() > 0 ? TelemetryLevel.WARN : TelemetryLevel.INFO;
                if (!threshold.allows(level)) continue;

                Map<String, Object> attrs = attributes(
                        "checks", h.checks(),
                        "failed_checks", h.failedChecks(),
                        "session_sli", h.sessionSli(),
                        "window_sli", h.windowSli(),
                        "burn_rate", h.burnRate(),
                        "open_violations", String.join(",", new TreeSet<>(h.openViolations())),
                        "violations", new TreeMap<>(h.violationCounts()).toString(),
                        "last_violated", h.lastViolated());
                if (!run.active) attrs.put("module_removed", true);

                record(new TelemetryEvent(tick, System.currentTimeMillis(), level, run.moduleId(),
                        BuiltinCategory.HEALTH.id(), h.subject(), "health summary", attrs));
            }
        } catch (Throwable t) {
            LOGGER.error("Cannot write the health summary of telemetry module '{}'", run.moduleId(), t);
        }
    }

    /**
     * Emits {@code config}: the tracked settings of the core config and of the recorded modules
     * that differ from their default, or {@code all_default=true}. Takes the snapshot compared by
     * {@link #checkConfig}. Never throws.
     */
    private void emitConfig() {
        try {
            configSnapshot = configValues(false);
            Map<String, Object> changed = configValues(true);
            emitLifecycle(TelemetryLevel.INFO, "config", changed.isEmpty() ? attributes("all_default", true) : changed);
        } catch (Throwable t) {
            LOGGER.error("Cannot write the telemetry config", t);
        }
    }

    /**
     * Writes one {@code config changed} event ({@code setting}, {@code from}, {@code to}) per
     * tracked setting changed since the last check, by a command or by editing a TOML. Called every
     * {@value #SINK_FLUSH_INTERVAL_TICKS} ticks, and by {@code /cn telemetry level} so that the
     * event has the right tick. Some settings are only read when a session starts (intervals, SLO,
     * {@code latest_log_level}, sinks): their change is recorded, but applies to the next session.
     * Server thread only; never throws.
     */
    public void checkConfig() {
        if (stopping) return;
        try {
            Map<String, Object> now = configValues(false);
            for (Map.Entry<String, Object> e : now.entrySet()) {
                Object before = configSnapshot.get(e.getKey());
                if (!Objects.equals(before, e.getValue()))
                    emitLifecycle(TelemetryLevel.INFO, "config changed",
                            attributes("setting", e.getKey(), "from", before, "to", e.getValue()));
            }
            configSnapshot = now;
        } catch (Throwable t) {
            LOGGER.error("Cannot check the telemetry config", t);
        }
    }

    /**
     * Returns the tracked settings of the core config and of the recorded modules.
     *
     * @param changedOnly {@code true} for only the settings that differ from their default
     * @return values keyed {@code <config>.<key>}, e.g. {@code reactor.level_health}
     */
    private Map<String, Object> configValues(boolean changedOnly) {
        Map<String, Object> out = new LinkedHashMap<>();
        TelemetryCoreConfig core = TelemetryConfigs.core();
        if (core != null) putPrefixed(out, core.getName(), changedOnly ? core.changedValues() : core.values());
        for (ModuleRun run : modules.values())
            putPrefixed(out, run.moduleId(), changedOnly ? run.config.changedValues() : run.config.values());
        return out;
    }

    private static void putPrefixed(Map<String, Object> out, String prefix, Map<String, Object> values) {
        for (Map.Entry<String, Object> e : values.entrySet()) out.put(prefix + "." + e.getKey(), e.getValue());
    }

    /** Session events go straight to the sinks: never level-filtered. */
    private void emitLifecycle(TelemetryLevel level, String message, Map<String, Object> attributes) {
        try {
            record(new TelemetryEvent(tick, System.currentTimeMillis(), level, SESSION_MODULE, SESSION_CATEGORY,
                    Subject.SERVER, message, attributes));
        } catch (Throwable t) {
            LOGGER.error("Cannot record telemetry session event '{}'", message, t);
        }
    }

    private void closeSinks() {
        synchronized (sinks) {
            sinksClosed = true;
            for (SinkRun run : sinks) closeQuietly(run, info);
            sinks.clear();
        }
    }

    private void notifyStop(StopReason reason) {
        if (notifier == null || reason == StopReason.COMMAND || reason == StopReason.SERVER_STOPPING) return;
        try {
            notifier.accept(Component.literal("Telemetry session stopped (" + reason.id() + "). Files: "
                    + displayDirectory()));
        } catch (Throwable t) {
            LOGGER.error("Cannot notify the telemetry session launcher", t);
        }
    }

    /**
     * Resolves the module spec into modules to record.
     *
     * @return an error message, or {@code null} if the spec is valid
     */
    @Nullable
    private static String selectModules(@Nullable String spec, List<ModuleSelection> selected, List<String> warnings) {
        boolean all = spec == null || spec.isBlank() || spec.equalsIgnoreCase("all");
        Collection<String> ids = all
                ? TelemetryRegistry.modules().stream().map(TelemetryRegistry.ModuleEntry::id).toList()
                : new LinkedHashSet<>(Arrays.stream(spec.split("\\+")).map(String::trim).filter(s -> !s.isEmpty()).toList());

        for (String id : ids) {
            TelemetryRegistry.ModuleEntry entry = TelemetryRegistry.module(id);
            if (entry == null) return "Unknown telemetry module '" + id + "'. See /cn telemetry modules.";
            TelemetryModuleConfig config = TelemetryConfigs.module(id);
            if (config == null) {
                warnings.add("Module '" + id + "' skipped: its config could not be created.");
            } else if (!config.enabled.get()) {
                warnings.add("Module '" + id + "' skipped: enabled = false in " + TelemetryModuleConfig.fileName(id) + ".");
            } else {
                selected.add(new ModuleSelection(entry, config));
            }
        }
        return null;
    }

    private static void openSinks(TelemetryCoreConfig core, SessionInfo info, List<SinkRun> opened, List<String> warnings) {
        for (TelemetryRegistry.SinkEntry entry : TelemetryRegistry.sinks()) {
            if (!core.isSinkEnabled(entry.id())) continue;
            try {
                ITelemetrySink sink = entry.factory().get();
                sink.open(info);
                opened.add(new SinkRun(entry.id(), sink));
            } catch (Throwable t) {
                LOGGER.error("Telemetry sink '{}' could not be opened", entry.id(), t);
                warnings.add("Sink '" + entry.id() + "' could not be opened and is not used.");
            }
        }
    }

    /**
     * Removes the directory of a session that did not start, if nothing was written in it.
     * A sink that wrote before failing keeps its files, for diagnosis.
     */
    private static void deleteIfEmpty(@Nullable Path directory) {
        if (directory == null) return;
        try {
            Files.deleteIfExists(directory);
        } catch (DirectoryNotEmptyException e) {
            // keep partial files
        } catch (Throwable t) {
            LOGGER.warn("Cannot remove the empty telemetry directory of a failed session", t);
        }
    }

    /** Appends {@code _2}, {@code _3}... when two sessions start within the same second. */
    private static String uniqueId(Path root, String base) {
        String id = base;
        for (int n = 2; Files.exists(root.resolve(id)); n++) id = base + "_" + n;
        return id;
    }

    private static void closeQuietly(SinkRun run, @Nullable SessionInfo info) {
        if (info == null) return;
        try {
            run.sink().close(info);
        } catch (Throwable t) {
            LOGGER.error("Telemetry sink '{}' failed to close", run.id(), t);
        }
    }

    private static Map<String, Object> attributes(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) map.put((String) keyValues[i], keyValues[i + 1]);
        return map;
    }

    // ---- Types ----------------------------------------------------------------------------------

    /** A module chosen for the session, with its config. */
    private record ModuleSelection(TelemetryRegistry.ModuleEntry entry, TelemetryModuleConfig config) {}

    /** An open sink. */
    private record SinkRun(String id, ITelemetrySink sink) {}

    /** A recorded module: settings read at start, metrics, health and its {@link TelemetryContext}. */
    private final class ModuleRun implements TelemetryContext {
        final TelemetryRegistry.ModuleEntry entry;
        final TelemetryModuleConfig config;
        final int metricsInterval;
        final int healthInterval;
        final HealthTracker tracker;
        final Map<Subject, MetricSet> metrics = new LinkedHashMap<>(); // guarded by itself
        volatile boolean active = true;

        ModuleRun(ModuleSelection selection) {
            this.entry = selection.entry();
            this.config = selection.config();
            this.metricsInterval = Math.max(1, config.metricsIntervalTicks.get());
            this.healthInterval = Math.max(1, config.healthCheckIntervalTicks.get());
            this.tracker = new HealthTracker(entry.id(), config.sloWindowChecks.get(), config.sloTarget.get(),
                    TelemetrySession.this::tick, new HealthEventEmitter(entry.id()));
        }

        /** Runs a module hook; on exception the module is removed from the session. */
        void call(String hook, Consumer<ITelemetryModule> action) {
            if (!active) return;
            try {
                action.accept(entry.module());
            } catch (Throwable t) {
                active = false;
                LOGGER.error("Telemetry module '{}' failed in {} and was removed from the session", entry.id(), hook, t);
                emitLifecycle(TelemetryLevel.WARN, "module removed", attributes(
                        "module", entry.id(), "hook", hook, "exception", t.getClass().getName()));
            }
        }

        @Override public MinecraftServer server() { return server; }
        @Override public SessionInfo session() { return info; }
        @Override public long tick() { return TelemetrySession.this.tick; }
        @Override public String moduleId() { return entry.id(); }
    }

    /**
     * Why a session stopped; {@link #id()} is written in {@code session stopped}.
     */
    public enum StopReason {
        /** The planned duration has elapsed. */
        DURATION("duration_elapsed"),
        /** {@code /cn telemetry stop}. */
        COMMAND("command"),
        /** The server is stopping. */
        SERVER_STOPPING("server_stopping"),
        /** Every sink failed. */
        NO_SINK("no_sink_left"),
        /** Unexpected internal error. */
        ERROR("internal_error");

        private final String id;

        StopReason(String id) {
            this.id = id;
        }

        /** @return the snake_case id written in the session file */
        public String id() {
            return id;
        }
    }

    /**
     * Outcome of {@link #start}.
     *
     * @param session  the started session, or {@code null} on failure
     * @param error    why the session did not start, or {@code null} on success
     * @param warnings non-blocking remarks (shortened duration, skipped module, failed sink)
     */
    public record StartResult(@Nullable TelemetrySession session, @Nullable String error, List<String> warnings) {
        static StartResult success(TelemetrySession session, List<String> warnings) {
            return new StartResult(session, null, List.copyOf(warnings));
        }

        static StartResult failure(String error) {
            return new StartResult(null, error, List.of());
        }

        /** @return {@code true} if the session started */
        public boolean started() {
            return session != null;
        }
    }
}
