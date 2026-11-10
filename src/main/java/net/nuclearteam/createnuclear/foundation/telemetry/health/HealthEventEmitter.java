package net.nuclearteam.createnuclear.foundation.telemetry.health;

import net.nuclearteam.createnuclear.foundation.telemetry.Telemetry;
import net.nuclearteam.createnuclear.foundation.telemetry.api.BuiltinCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;

import javax.annotation.Nullable;

/**
 * Turns health transitions into events of the {@link BuiltinCategory#HEALTH} category, through the
 * {@link Telemetry} facade (level filtering and exception safety included):
 * <ul>
 *   <li>{@code violation started: <invariant>} (WARN), attributes {@code invariant}, {@code detail};</li>
 *   <li>{@code violation resolved: <invariant>} (INFO), attributes {@code invariant}, {@code duration_ticks};</li>
 *   <li>{@code slo burn rate alert} (WARN) and {@code slo burn rate recovered} (INFO), attributes
 *       {@code sli}, {@code slo_target}, {@code burn_rate}, {@code window}.</li>
 * </ul>
 * Used by the session for every recorded module.
 */
public final class HealthEventEmitter implements HealthListener {
    private final String moduleId;

    /**
     * Creates the emitter of a module.
     *
     * @param moduleId module whose health category receives the events
     */
    public HealthEventEmitter(String moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public void violationStarted(Subject subject, String invariant, @Nullable String detail) {
        Telemetry.event(moduleId, BuiltinCategory.HEALTH, TelemetryLevel.WARN)
                .subject(subject)
                .msg("violation started: {}", invariant)
                .attr("invariant", invariant)
                .attr("detail", detail)
                .emit();
    }

    @Override
    public void violationResolved(Subject subject, String invariant, long durationTicks) {
        Telemetry.event(moduleId, BuiltinCategory.HEALTH, TelemetryLevel.INFO)
                .subject(subject)
                .msg("violation resolved: {}", invariant)
                .attr("invariant", invariant)
                .attr("duration_ticks", durationTicks)
                .emit();
    }

    @Override
    public void burnRateChanged(Subject subject, boolean alerting, double sli, double sloTarget, double burnRate, int window) {
        Telemetry.event(moduleId, BuiltinCategory.HEALTH, alerting ? TelemetryLevel.WARN : TelemetryLevel.INFO)
                .subject(subject)
                .msg(alerting ? "slo burn rate alert" : "slo burn rate recovered")
                .attr("sli", sli)
                .attr("slo_target", sloTarget)
                .attr("burn_rate", burnRate)
                .attr("window", window)
                .emit();
    }
}
