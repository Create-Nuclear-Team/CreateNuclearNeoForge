package net.nuclearteam.createnuclear.foundation.telemetry.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetryRegistry;
import net.nuclearteam.createnuclear.foundation.telemetry.api.ITelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryIds;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * Settings of one telemetry module, stored in {@code config/createnuclear-telemetry-<id>.toml} (COMMON).
 * <p>
 * Contains the common module settings, one {@code level_<category>} line per category (built-in ones
 * included) and the options declared by {@link ITelemetryModule#defineOptions}. Also implements
 * {@link ITelemetryModule.Options}, so it is passed directly to {@code defineOptions}. Every
 * setting, options included, is tracked by {@link TelemetryConfigBase} for the session's
 * {@code config} and {@code config changed} events.
 */
public final class TelemetryModuleConfig extends TelemetryConfigBase implements ITelemetryModule.Options {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateNuclear/Telemetry");

    /** Keys used by this class; a module option cannot take them. */
    private static final Set<String> RESERVED_KEYS = Set.of(
            "enabled", "metrics_interval_ticks", "health_check_interval_ticks", "slo_window_checks", "slo_target");
    private static final String LEVEL_PREFIX = "level_";

    // Defaults, each used twice: for the declaration and for the tracking (TelemetryConfigBase).
    private static final int DEFAULT_METRICS_INTERVAL = 200;
    private static final int DEFAULT_HEALTH_INTERVAL = 20;
    private static final int DEFAULT_SLO_WINDOW = 100;
    private static final double DEFAULT_SLO_TARGET = 0.99;

    /** Whether the module can be recorded at all. */
    public final ConfigBool enabled = trackBool(b(true, "enabled",
            "Allow telemetry sessions to record this module.",
            "When false, the module is skipped by /cn telemetry start, even if named explicitly."), true);

    /** Interval between two metric dumps. */
    public final ConfigInt metricsIntervalTicks = trackInt(i(DEFAULT_METRICS_INTERVAL, 1, 72000, "metrics_interval_ticks",
            "Ticks between two writes of this module's metrics to the session (20 ticks = 1 second)."),
            DEFAULT_METRICS_INTERVAL);

    /** Interval between two health checks. */
    public final ConfigInt healthCheckIntervalTicks = trackInt(i(DEFAULT_HEALTH_INTERVAL, 1, 72000, "health_check_interval_ticks",
            "Ticks between two checks of this module's invariants (20 ticks = 1 second)."),
            DEFAULT_HEALTH_INTERVAL);

    /** Size of the sliding window used for the SLI. */
    public final ConfigInt sloWindowChecks = trackInt(i(DEFAULT_SLO_WINDOW, 10, 100000, "slo_window_checks",
            "Number of most recent invariant checks used to compute the SLI (share of healthy checks)."),
            DEFAULT_SLO_WINDOW);

    /** Target share of healthy checks. */
    public final CValue<Double, ModConfigSpec.DoubleValue> sloTarget = trackDecimal(decimalValue("slo_target",
            DEFAULT_SLO_TARGET, 0.5, 1.0,
            "SLO: target share of healthy checks over the window. 0.99 means 99% of checks must pass.",
            "The error budget is 1 - slo_target. A burn rate above 1 means the budget is used up too fast, and raises an alert."),
            DEFAULT_SLO_TARGET);

    private final String moduleId;
    private final Map<String, ConfigEnum<TelemetryLevel>> levels;
    private final Set<String> optionKeys = new HashSet<>();
    private boolean frozen;

    /**
     * Creates the config of a module and lets it declare its options.
     * <p>
     * An exception thrown by {@link ITelemetryModule#defineOptions} is logged; options declared
     * before it are kept. Options can no longer be declared once this constructor returns.
     *
     * @param entry the registered module
     */
    public TelemetryModuleConfig(TelemetryRegistry.ModuleEntry entry) {
        this.moduleId = entry.id();

        Map<String, ConfigEnum<TelemetryLevel>> map = new LinkedHashMap<>();
        for (TelemetryCategory category : entry.categories())
            map.put(category.id(), trackLevel(e(category.defaultLevel(), LEVEL_PREFIX + category.id(),
                    "Most verbose level recorded for '" + moduleId + "/" + category.id() + "'. OFF records nothing.",
                    Objects.requireNonNullElse(category.description(), "")), category.defaultLevel()));
        this.levels = Collections.unmodifiableMap(map);

        try {
            entry.module().defineOptions(this);
        } catch (Throwable t) {
            LOGGER.error("Telemetry module '{}' failed to define its options", moduleId, t);
        } finally {
            frozen = true;
        }
    }

    /**
     * Returns the config file name of a module.
     *
     * @param moduleId module id
     * @return {@code createnuclear-telemetry-<id>.toml}
     */
    public static String fileName(String moduleId) {
        return String.join("-", CreateNuclear.MOD_ID, "telemetry", String.join(".", moduleId, "toml"));
    }

    /**
     * Returns the id of the module this config belongs to.
     *
     * @return the module id
     */
    public String moduleId() {
        return moduleId;
    }

    /**
     * Returns the current threshold of a category.
     *
     * @param categoryId category id, built-in ones included
     * @return the configured level, or {@link TelemetryLevel#OFF} for an unknown category
     */
    public TelemetryLevel level(String categoryId) {
        ConfigEnum<TelemetryLevel> value = levels.get(categoryId);
        return value == null ? TelemetryLevel.OFF : value.get();
    }

    /**
     * Changes the threshold of a category and saves it to disk.
     *
     * @param categoryId category id, built-in ones included
     * @param level      the new level
     * @return {@code false} if the category is unknown; nothing is changed then
     */
    public boolean setLevel(String categoryId, TelemetryLevel level) {
        ConfigEnum<TelemetryLevel> value = levels.get(categoryId);
        if (value == null) return false;
        value.set(level);
        return true;
    }

    /**
     * Returns the ids of every configurable category.
     *
     * @return an unmodifiable set, in declaration order, built-in categories last
     */
    public Set<String> categoryIds() {
        return levels.keySet();
    }

    // ---- ITelemetryModule.Options ---------------------------------------------------------------

    @Override
    public BooleanSupplier bool(String key, boolean defaultValue, String comment) {
        ConfigBool value = trackBool(b(defaultValue, checkOptionKey(key), comments(comment)), defaultValue);
        return value::get;
    }

    @Override
    public IntSupplier integer(String key, int defaultValue, int min, int max, String comment) {
        ConfigInt value = trackInt(i(defaultValue, min, max, checkOptionKey(key), comments(comment)), defaultValue);
        return value::get;
    }

    @Override
    public DoubleSupplier decimal(String key, double defaultValue, double min, double max, String comment) {
        CValue<Double, ModConfigSpec.DoubleValue> value = trackDecimal(
                decimalValue(checkOptionKey(key), defaultValue, min, max, comments(comment)), defaultValue);
        return value::get;
    }

    /**
     * Validates a module option key.
     *
     * @param key the key passed to an {@link ITelemetryModule.Options} method
     * @return {@code key}
     * @throws IllegalStateException    if called after {@link ITelemetryModule#defineOptions} returned
     * @throws IllegalArgumentException if the key is invalid, reserved or already used
     */
    private String checkOptionKey(String key) {
        if (frozen)
            throw new IllegalStateException("Telemetry module '" + moduleId + "' declared option '" + key + "' after defineOptions");
        TelemetryIds.require(key, "option");
        if (RESERVED_KEYS.contains(key) || key.startsWith(LEVEL_PREFIX))
            throw new IllegalArgumentException("Option key '" + key + "' is reserved in module '" + moduleId + "'");
        if (!optionKeys.add(key))
            throw new IllegalArgumentException("Duplicate option key '" + key + "' in module '" + moduleId + "'");
        return key;
    }

    /**
     * Turns an optional comment into catnip's comment array.
     *
     * @param comment a comment, possibly {@code null}
     * @return an empty array for {@code null}, otherwise a one-element array
     */
    private static String[] comments(@Nullable String comment) {
        return comment == null ? new String[0] : new String[]{comment};
    }

    /**
     * Declares a real double value. catnip's {@code f(...)} goes through {@code float}, so 0.99 would
     * be written as 0.9900000095367432.
     *
     * @param key          config key
     * @param defaultValue value written when the config is first generated
     * @param min          minimum accepted value, inclusive
     * @param max          maximum accepted value, inclusive
     * @param comments     config comment lines
     * @return the config value
     */
    private CValue<Double, ModConfigSpec.DoubleValue> decimalValue(String key, double defaultValue, double min,
                                                                    double max, String... comments) {
        return new CValue<>(key, builder -> builder.defineInRange(key, defaultValue, min, max), comments);
    }

    @Override
    public String getName() {
        return moduleId;
    }
}
