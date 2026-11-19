package net.nuclearteam.createnuclear.foundation.telemetry.config;

import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetryRegistry;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shared telemetry settings, stored in {@code config/createnuclear-telemetry.toml} (COMMON).
 * <p>
 * Contains the session limits, the {@code latest.log} threshold and one {@code sink_<id>} toggle
 * per registered sink. Every setting is tracked by {@link TelemetryConfigBase} for the session's
 * {@code config} and {@code config changed} events.
 */
public final class TelemetryCoreConfig extends TelemetryConfigBase {
    /** Config file name, relative to the config directory. */
    public static final String FILE_NAME = String.join("-", CreateNuclear.MOD_ID, "telemetry.toml");

    // Defaults, each used twice: for the declaration and for the tracking (TelemetryConfigBase).
    private static final int DEFAULT_SESSION_SECONDS = 300;
    private static final int DEFAULT_MAX_SESSION_SECONDS = 1800;
    private static final TelemetryLevel DEFAULT_LATEST_LOG_LEVEL = TelemetryLevel.WARN;

    /** Session length used when {@code /cn telemetry start} is given no duration. */
    public final ConfigInt defaultSessionSeconds = trackInt(i(DEFAULT_SESSION_SECONDS, 10, 86400, "default_session_seconds",
            "Length of a telemetry session, in seconds, when /cn telemetry start is given no duration."),
            DEFAULT_SESSION_SECONDS);

    /** Hard limit of a session; longer requests are shortened to this value. */
    public final ConfigInt maxSessionSeconds = trackInt(i(DEFAULT_MAX_SESSION_SECONDS, 10, 86400, "max_session_seconds",
            "Maximum length of a telemetry session, in seconds. Longer requests are shortened to this value.",
            "A session always stops by itself after this time, even if it was forgotten."),
            DEFAULT_MAX_SESSION_SECONDS);

    /** Minimum level copied to the game log by the {@code log} sink; read when a session starts. */
    public final ConfigEnum<TelemetryLevel> latestLogLevel = trackLevel(e(DEFAULT_LATEST_LOG_LEVEL, "latest_log_level",
            "Minimum level of the telemetry events copied to the game log by the 'log' sink. OFF copies nothing.",
            "latest.log only shows INFO and above; DEBUG and TRACE go to debug.log.",
            "Read when a session starts. The session file always contains every recorded event, whatever this value."),
            DEFAULT_LATEST_LOG_LEVEL);

    private final Map<String, ConfigBool> sinkToggles;

    /**
     * Creates the config, with one toggle per sink.
     *
     * @param sinks the registered sinks, in the order their toggles are written
     */
    public TelemetryCoreConfig(Collection<TelemetryRegistry.SinkEntry> sinks) {
        Map<String, ConfigBool> toggles = new LinkedHashMap<>();
        for (TelemetryRegistry.SinkEntry sink : sinks)
            toggles.put(sink.id(), trackBool(b(sink.enabledByDefault(), String.join("_", "sink", sink.id()),
                    "Write telemetry sessions to the '" + sink.id() + "' sink.",
                    Objects.requireNonNullElse(sink.description(), "")), sink.enabledByDefault()));
        this.sinkToggles = Collections.unmodifiableMap(toggles);
    }

    /**
     * Tells whether a sink is enabled for new sessions.
     *
     * @param sinkId sink id
     * @return the value of {@code sink_<id>}, or {@code false} for an unknown sink
     */
    public boolean isSinkEnabled(String sinkId) {
        ConfigBool toggle = sinkToggles.get(sinkId);
        return toggle != null && toggle.get();
    }

    @Override
    public String getName() {
        return "telemetry";
    }
}
