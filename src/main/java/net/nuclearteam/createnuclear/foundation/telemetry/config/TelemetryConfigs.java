package net.nuclearteam.createnuclear.foundation.telemetry.config;

import net.createmod.catnip.config.ConfigBase;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetryRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

/**
 * Holds the telemetry configs and forwards config load and reload events to them.
 * <p>
 * Kept apart from {@code CNConfigs}, whose map stores a single config per type, while telemetry
 * registers one COMMON config for the core plus one per module.
 */
public final class TelemetryConfigs {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateNuclear/Telemetry");

    private static TelemetryCoreConfig core;
    private static Map<String, TelemetryModuleConfig> modules = Map.of();
    private static List<ConfigBase> all = List.of();

    private TelemetryConfigs() {}

    /**
     * Builds and registers the core config and one config per registered module.
     * Called by {@link TelemetryRegistry#bootstrap} once the registry is indexed.
     * <p>
     * A module whose config cannot be built is logged and gets no config; sessions skip it.
     * <p>
     * <b>Internal:</b> public only because the registry lives in another package.
     *
     * @param container Create Nuclear's mod container
     * @throws IllegalStateException if called twice
     */
    @ApiStatus.Internal
    public static synchronized void register(ModContainer container) {
        if (core != null) throw new IllegalStateException("Telemetry configs already registered");
        List<ConfigBase> registered = new ArrayList<>();

        core = build(() -> new TelemetryCoreConfig(TelemetryRegistry.sinks()));
        container.registerConfig(ModConfig.Type.COMMON, core.specification, TelemetryCoreConfig.FILE_NAME);
        registered.add(core);

        Map<String, TelemetryModuleConfig> map = new LinkedHashMap<>();
        for (TelemetryRegistry.ModuleEntry entry : TelemetryRegistry.modules()) {
            try {
                TelemetryModuleConfig config = build(() -> new TelemetryModuleConfig(entry));
                container.registerConfig(ModConfig.Type.COMMON, config.specification, TelemetryModuleConfig.fileName(entry.id()));
                map.put(entry.id(), config);
                registered.add(config);
            } catch (Throwable t) {
                LOGGER.error("Cannot create the config of telemetry module '{}'", entry.id(), t);
            }
        }

        modules = Collections.unmodifiableMap(map);
        all = List.copyOf(registered);
    }

    /**
     * Builds a catnip config and its spec, the same way {@code CNConfigs} does.
     *
     * @param factory creates the config; called inside the spec builder
     * @param <T>     config type
     * @return the config, with its {@code specification} set
     */
    private static <T extends ConfigBase> T build(Supplier<T> factory) {
        Pair<T, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = pair.getLeft();
        config.specification = pair.getRight();
        return config;
    }

    /**
     * Forwards a config load to the matching telemetry config, if any.
     * Called from {@code CNConfigs.onLoad}.
     *
     * @param config the loaded config
     */
    public static void onLoad(ModConfig config) {
        for (ConfigBase c : all)
            if (c.specification == config.getSpec()) c.onLoad();
    }

    /**
     * Forwards a config reload to the matching telemetry config, if any.
     * Called from {@code CNConfigs.onReload}.
     *
     * @param config the reloaded config
     */
    public static void onReload(ModConfig config) {
        for (ConfigBase c : all)
            if (c.specification == config.getSpec()) c.onReload();
    }

    /**
     * Returns the shared telemetry settings.
     *
     * @return the core config, or {@code null} before bootstrap
     */
    @Nullable
    public static TelemetryCoreConfig core() {
        return core;
    }

    /**
     * Returns the config of a module.
     *
     * @param moduleId module id
     * @return the module config, or {@code null} if the module is unknown or its config failed
     */
    @Nullable
    public static TelemetryModuleConfig module(String moduleId) {
        return modules.get(moduleId);
    }
}
