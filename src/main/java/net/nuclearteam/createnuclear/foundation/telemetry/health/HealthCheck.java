package net.nuclearteam.createnuclear.foundation.telemetry.health;

import net.nuclearteam.createnuclear.foundation.telemetry.Telemetry;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One health check of a subject: a set of invariant results, ended by {@link #commit()}.
 * Obtained from {@link Telemetry#health(String, Subject)}.
 * <p>
 * {@code violated = true} means the rule is broken. Name invariants after the broken rule,
 * e.g. {@code output_spinning_while_stopped}. Always call {@link #commit()}, even when every
 * invariant holds: a healthy check counts in the SLI and closes open violations. An invariant
 * left out of a check keeps its current state.
 * <p>
 * Outside a session the facade returns {@link #NOOP}, which ignores everything. Never throws.
 * Not thread-safe; meant to be used in a single statement.
 */
public final class HealthCheck {
    /** Shared check that ignores everything. */
    public static final HealthCheck NOOP = new HealthCheck(null, Subject.SERVER);

    @Nullable
    private final HealthTracker tracker;
    private final Subject subject;
    private final List<Result> results = new ArrayList<>(2);
    private boolean committed;

    /**
     * Creates a check. <b>Internal:</b> instrumented code uses {@link Telemetry#health}; public
     * because the facade lives in another package.
     *
     * @param tracker the module's tracker; {@code null} only for {@link #NOOP}
     * @param subject the subject being checked
     */
    @ApiStatus.Internal
    public HealthCheck(@Nullable HealthTracker tracker, Subject subject) {
        this.tracker = tracker;
        this.subject = subject;
    }

    /**
     * Records the result of an invariant, without detail.
     *
     * @param invariant snake_case invariant name
     * @param violated  {@code true} if the rule is broken
     * @return this check
     */
    public HealthCheck check(String invariant, boolean violated) {
        return check(invariant, violated, null);
    }

    /**
     * Records the result of an invariant. Checking the same invariant twice in one check counts
     * it as violated if either result is.
     *
     * @param invariant snake_case invariant name; an invalid name is logged and ignored at commit
     * @param violated  {@code true} if the rule is broken
     * @param detail    computes a short explanation; called only when a violation starts.
     *                  If it throws, the detail becomes {@code <error: ExceptionClass>}
     * @return this check
     */
    public HealthCheck check(String invariant, boolean violated, @Nullable Supplier<String> detail) {
        if (this != NOOP && !committed) results.add(new Result(invariant, violated, detail));
        return this;
    }

    /**
     * Sends the results to the module's tracker. Never throws; calling it again does nothing.
     */
    public void commit() {
        HealthTracker t = tracker;
        if (this == NOOP || t == null || committed) return;
        committed = true;

        try {
            t.commit(subject, results);
        } catch (Throwable e) {
            Telemetry.reportFailure("health commit(" + t.moduleId() + ")", e);
        }
    }

    /**
     * Result of one invariant in one check.
     *
     * @param invariant invariant name, not yet validated
     * @param violated  {@code true} if the rule is broken
     * @param detail    lazy explanation, or {@code null}
     */
    record Result(String invariant, boolean violated, @Nullable Supplier<String> detail) {}
}
