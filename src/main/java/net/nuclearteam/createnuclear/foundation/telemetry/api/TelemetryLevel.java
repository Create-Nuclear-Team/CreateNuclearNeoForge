package net.nuclearteam.createnuclear.foundation.telemetry.api;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Verbosity of a telemetry event, and threshold of a category.
 * <p>
 * Constants are ordered from the least to the most verbose. An event is recorded when its level
 * is at most the level of its category (see {@link #allows(TelemetryLevel)}).
 * {@link #OFF} as a threshold records nothing, and is never a valid event level.
 */
public enum TelemetryLevel {
    /** Threshold only: records nothing. */
    OFF,
    /** Unexpected error in the code, e.g. a caught exception. */
    ERROR,
    /** Abnormal situation or incident, e.g. an invariant violation. */
    WARN,
    /** Rare state change, e.g. assembly or {@code canRun} flipping. */
    INFO,
    /** Useful, infrequent detail, e.g. an output speed change. */
    DEBUG,
    /** Very frequent, up to every tick. */
    TRACE;

    /**
     * Tells whether a category set to this level records an event.
     *
     * @param eventLevel level of the event being emitted
     * @return {@code true} if an event of {@code eventLevel} is recorded by a category set to {@code this}
     */
    public boolean allows(TelemetryLevel eventLevel) {
        return this != OFF && eventLevel != OFF && eventLevel.ordinal() <= ordinal();
    }

    /**
     * Returns the id used in configs and commands.
     *
     * @return the lower-case name, e.g. {@code "debug"}
     */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses a level name, ignoring case.
     *
     * @param name the name to parse, e.g. {@code "TRACE"} or {@code "trace"}
     * @return the matching level, or {@code null} if the name is unknown
     */
    @Nullable
    public static TelemetryLevel parse(String name) {
        for (TelemetryLevel level : values())
            if (level.name().equalsIgnoreCase(name)) return level;
        return null;
    }
}
