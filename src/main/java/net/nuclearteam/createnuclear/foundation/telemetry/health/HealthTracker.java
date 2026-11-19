package net.nuclearteam.createnuclear.foundation.telemetry.health;

import net.nuclearteam.createnuclear.foundation.telemetry.Telemetry;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryIds;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Health state of every subject of one module during one session: open violations, SLI over a
 * sliding window, error budget and burn rate.
 * <p>
 * Created by the session with the module's {@code slo_window_checks} and {@code slo_target}
 * read at session start; config changes apply to the next session. Transitions are reported to a
 * {@link HealthListener} (the session uses {@link HealthEventEmitter}):
 * <ul>
 *   <li>a violation starts when an invariant becomes violated, and is resolved only when it is
 *       checked again and found healthy;</li>
 *   <li>a burn-rate alert opens when the burn rate goes above 1, once the window holds at least
 *       {@link #MIN_CHECKS_FOR_ALERT} checks (or is full, if smaller), and closes when it goes back
 *       to 1 or less.</li>
 * </ul>
 * Statistics are always computed; the listener decides what to do with transitions.
 * Thread-safe; the listener and detail suppliers run outside the lock.
 */
public final class HealthTracker {
    /** Minimum number of checks in the window before a burn-rate alert can be raised. */
    public static final int MIN_CHECKS_FOR_ALERT = 10;

    private final String moduleId;
    private final int windowSize;
    private final double sloTarget;
    private final LongSupplier clock;
    private final HealthListener listener;
    private final Map<Subject, SubjectState> subjects = new LinkedHashMap<>();

    /**
     * Creates the tracker of a module for one session. <b>Internal:</b> created by the session (or
     * by tests); public because the session lives in another package.
     *
     * @param moduleId   module id
     * @param windowSize number of most recent checks used for the SLI, at least 1
     * @param sloTarget  target share of healthy checks, between 0 and 1
     * @param clock      session tick, used for violation durations
     * @param listener   receives the transitions
     */
    @ApiStatus.Internal
    public HealthTracker(String moduleId, int windowSize, double sloTarget, LongSupplier clock, HealthListener listener) {
        this.moduleId = moduleId;
        this.windowSize = Math.max(1, windowSize);
        this.sloTarget = Math.clamp(sloTarget, 0, 1);
        this.clock = clock;
        this.listener = listener;
    }

    /**
     * Returns the module this tracker belongs to.
     *
     * @return the module id
     */
    public String moduleId() {
        return moduleId;
    }

    /**
     * Applies one check: updates violations, window and alert state, then reports the transitions.
     * Called by {@link HealthCheck#commit()}.
     *
     * @param subject the checked subject
     * @param results invariant results of the check
     */
    void commit(Subject subject, List<HealthCheck.Result> results) {
        List<Runnable> transitions = new ArrayList<>(2);

        synchronized (this) {
            SubjectState s = subjects.computeIfAbsent(subject, k -> new SubjectState(windowSize));
            long now = clock.getAsLong();

            Map<String, HealthCheck.Result> merged = new LinkedHashMap<>();
            for (HealthCheck.Result r : results) {
                if (!TelemetryIds.isValid(r.invariant())) {
                    Telemetry.reportFailure("invariant '" + r.invariant() + "' of " + moduleId,
                            new IllegalArgumentException("Invalid invariant name, expected [a-z0-9_]+"));
                    continue;
                }
                merged.merge(r.invariant(), r, (a, b) -> a.violated() ? a : b);
            }

            boolean healthy = true;
            for (HealthCheck.Result r : merged.values()) {
                String invariant = r.invariant();
                if (r.violated()) {
                    healthy = false;
                    if (s.openSince.putIfAbsent(invariant, now) == null) {
                        s.violationCounts.merge(invariant, 1L, Long::sum);
                        s.lastViolated = invariant;
                        Supplier<String> detail = r.detail();
                        transitions.add(() -> listener.violationStarted(subject, invariant, evaluate(detail)));
                    }
                } else {
                    Long since = s.openSince.remove(invariant);
                    if (since != null) {
                        long duration = now - since;
                        transitions.add(() -> listener.violationResolved(subject, invariant, duration));
                    }
                }
            }

            s.record(healthy);

            double burn = s.burnRate(sloTarget);
            if (!s.alerting && s.filled >= Math.min(MIN_CHECKS_FOR_ALERT, windowSize) && burn > 1) {
                s.alerting = true;
                double sli = s.windowSli();
                transitions.add(() -> listener.burnRateChanged(subject, true, sli, sloTarget, burn, windowSize));
            } else if (s.alerting && burn <= 1) {
                s.alerting = false;
                double sli = s.windowSli();
                transitions.add(() -> listener.burnRateChanged(subject, false, sli, sloTarget, burn, windowSize));
            }
        }

        for (Runnable t : transitions) {
            try {
                t.run();
            } catch (Throwable e) {
                Telemetry.reportFailure("health listener of " + moduleId, e);
            }
        }
    }

    /**
     * Returns the current health of every checked subject, for {@code /cn telemetry stats}
     * and the end-of-session summary.
     *
     * @return one entry per subject, in first-check order
     */
    public synchronized List<SubjectHealth> snapshot() {
        List<SubjectHealth> out = new ArrayList<>(subjects.size());
        for (Map.Entry<Subject, SubjectState> e : subjects.entrySet()) {
            SubjectState s = e.getValue();
            out.add(new SubjectHealth(e.getKey(), s.checks, s.failedChecks, s.windowSli(), s.sessionSli(),
                    s.burnRate(sloTarget), s.budgetRemaining(sloTarget, windowSize), s.alerting,
                    Set.copyOf(s.openSince.keySet()), Map.copyOf(s.violationCounts), s.lastViolated));
        }
        return out;
    }

    /** Runs a detail supplier; never throws. */
    @Nullable
    private static String evaluate(@Nullable Supplier<String> detail) {
        if (detail == null) return null;
        try {
            return detail.get();
        } catch (Throwable t) {
            return "<error: " + t.getClass().getSimpleName() + ">";
        }
    }

    // ---- State ----------------------------------------------------------------------------------

    /**
     * Health of one subject at a given moment.
     *
     * @param subject          the subject
     * @param checks           checks committed during the session
     * @param failedChecks     checks with at least one violated invariant
     * @param windowSli        share of healthy checks in the sliding window; 1 if none yet
     * @param sessionSli       share of healthy checks over the whole session; 1 if none yet
     * @param burnRate         window failure rate divided by the error budget; infinite if the budget is 0
     *                         and a check failed
     * @param budgetRemaining  share of the window's error budget still unused, between 0 and 1
     * @param alerting         {@code true} while a burn-rate alert is open
     * @param openViolations   invariants currently violated
     * @param violationCounts  number of violations started, per invariant
     * @param lastViolated     last invariant whose violation started, or {@code null}
     */
    public record SubjectHealth(Subject subject, long checks, long failedChecks, double windowSli,
                                double sessionSli, double burnRate, double budgetRemaining, boolean alerting,
                                Set<String> openViolations, Map<String, Long> violationCounts,
                                @Nullable String lastViolated) {}

    /** Mutable state of one subject. Guarded by the tracker's lock. */
    private static final class SubjectState {
        final boolean[] window;
        int position;
        int filled;
        int windowFailures;
        long checks;
        long failedChecks;
        boolean alerting;
        @Nullable String lastViolated;
        final Map<String, Long> openSince = new LinkedHashMap<>();
        final Map<String, Long> violationCounts = new LinkedHashMap<>();

        SubjectState(int windowSize) {
            this.window = new boolean[windowSize];
        }

        /** Pushes a check into the circular window ({@code true} = healthy). */
        void record(boolean healthy) {
            if (filled == window.length) {
                if (!window[position]) windowFailures--;
            } else {
                filled++;
            }
            window[position] = healthy;
            if (!healthy) windowFailures++;
            position = (position + 1) % window.length;

            checks++;
            if (!healthy) failedChecks++;
        }

        double windowSli() {
            return filled == 0 ? 1.0 : 1.0 - (double) windowFailures / filled;
        }

        double sessionSli() {
            return checks == 0 ? 1.0 : 1.0 - (double) failedChecks / checks;
        }

        double burnRate(double sloTarget) {
            if (filled == 0 || windowFailures == 0) return 0;
            double budget = 1.0 - sloTarget;
            if (budget <= 0) return Double.POSITIVE_INFINITY;
            return ((double) windowFailures / filled) / budget;
        }

        double budgetRemaining(double sloTarget, int windowSize) {
            double allowed = (1.0 - sloTarget) * windowSize;
            if (allowed <= 0) return windowFailures == 0 ? 1.0 : 0.0;
            return Math.max(0.0, 1.0 - windowFailures / allowed);
        }
    }
}
