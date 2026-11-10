package net.nuclearteam.createnuclear.foundation.telemetry.api;

import java.util.Locale;

/** Categories added automatically to every module. */
public enum BuiltinCategory implements TelemetryCategory {
    HEALTH(TelemetryLevel.INFO, "Invariant violations, SLI and burn-rate alerts."),
    METRICS(TelemetryLevel.INFO, "Periodic dump of counters, gauges and histograms.")
    ;

    private final TelemetryLevel defaultLevel;
    private final String description;

    BuiltinCategory(TelemetryLevel defaultLevel, String description) {
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

    /** True if a module category id would clash with a built-in one. */
    public static boolean isReserved(String id) {
        for (BuiltinCategory c : values())
            if (c.id().equals(id)) return true;
        return false;
    }
}
