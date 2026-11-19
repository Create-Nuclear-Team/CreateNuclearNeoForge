package net.nuclearteam.createnuclear.foundation.telemetry.health;

import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;

import javax.annotation.Nullable;

/**
 * Receives the health transitions computed by a {@link HealthTracker}.
 * <p>
 * The session uses {@link HealthEventEmitter}, which turns them into telemetry events; tests use a
 * recording implementation. Called outside the tracker's lock; an exception is logged and ignored.
 */
public interface HealthListener {
    /**
     * An invariant just became violated for a subject.
     *
     * @param subject   the checked subject
     * @param invariant the violated invariant
     * @param detail    explanation computed when the violation started, or {@code null}
     */
    void violationStarted(Subject subject, String invariant, @Nullable String detail);

    /**
     * A violated invariant was checked again and found healthy.
     *
     * @param subject       the checked subject
     * @param invariant     the invariant that holds again
     * @param durationTicks session ticks between the start and the end of the violation
     */
    void violationResolved(Subject subject, String invariant, long durationTicks);

    /**
     * The burn-rate alert of a subject opened or closed.
     *
     * @param subject   the checked subject
     * @param alerting  {@code true} when the burn rate went above 1, {@code false} when it went back to 1 or less
     * @param sli       share of healthy checks in the window at that moment
     * @param sloTarget target share of healthy checks
     * @param burnRate  window failure rate divided by the error budget
     * @param window    size of the sliding window, in checks
     */
    void burnRateChanged(Subject subject, boolean alerting, double sli, double sloTarget, double burnRate, int window);
}
