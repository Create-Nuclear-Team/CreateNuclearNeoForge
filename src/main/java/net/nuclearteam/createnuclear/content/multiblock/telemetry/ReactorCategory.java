package net.nuclearteam.createnuclear.content.multiblock.telemetry;

import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;

import java.util.Locale;

/**
 * Categories of the {@code reactor} telemetry module. Each gets a {@code level_<id>} line in
 * {@code createnuclear-telemetry-reactor.toml}; all record {@code INFO} and above by default.
 */
public enum ReactorCategory implements TelemetryCategory {
    /** Assembly, disassembly, controller removal, IO blocks added or removed. */
    ASSEMBLY(TelemetryLevel.INFO, "Assembly, disassembly, controller removal and IO blocks."),
    /** {@code canRun} and {@code ACTIVE} changes, danger transitions, meltdown. */
    STATE(TelemetryLevel.INFO, "canRun and ACTIVE changes, danger transitions, meltdown."),
    /** Output speed changes (DEBUG) and orphan outputs. */
    OUTPUT(TelemetryLevel.INFO, "Output speed changes (DEBUG) and orphan outputs."),
    /** Fuel rods consumed and fluid consumption (DEBUG). */
    FUEL(TelemetryLevel.INFO, "Fuel rods consumed and fluid consumption (DEBUG)."),
    /** Heat variations, up to every tick (TRACE). */
    HEAT(TelemetryLevel.INFO, "Heat variations, up to every tick (TRACE).");

    private final TelemetryLevel defaultLevel;
    private final String description;

    ReactorCategory(TelemetryLevel defaultLevel, String description) {
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
