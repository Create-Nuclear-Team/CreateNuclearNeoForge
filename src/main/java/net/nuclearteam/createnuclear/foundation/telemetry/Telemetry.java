package net.nuclearteam.createnuclear.foundation.telemetry;

import net.nuclearteam.createnuclear.foundation.telemetry.api.BuiltinCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.health.HealthCheck;
import net.nuclearteam.createnuclear.foundation.telemetry.health.HealthTracker;
import net.nuclearteam.createnuclear.foundation.telemetry.metrics.MetricSet;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry point of instrumented code: events, metrics, health checks and due-checks.
 * <p>
 * Outside a session, every method costs a single volatile read and returns a no-op object
 * ({@link EventBuilder#NOOP}, {@link MetricSet#NOOP}, {@link HealthCheck#NOOP}).
 * <p>
 * No public method ever throws into gameplay code: every call to the session or to a category is
 * guarded, and a failure is logged with {@link #reportFailure} and answered with the safe default
 * ({@code false}, {@link EventBuilder#NOOP}, {@link MetricSet#NOOP} or {@link HealthCheck#NOOP}).
 */
public final class Telemetry {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateNuclear/Telemetry");
    private static final int MAX_REPORTED_FAILURES = 10;
    private static final AtomicInteger FAILURES = new AtomicInteger();

    @Nullable
    private static volatile TelemetryRecorder recorder;

    private Telemetry() {}

    // ---- State ----------------------------------------------------------------------------------

    /**
     * Tells whether a session is running.
     *
     * @return {@code true} while a session is recording
     */
    public static boolean isActive() {
        return recorder != null;
    }

    /**
     * Tells whether a module is recorded by the running session.
     *
     * @param moduleId module id
     * @return {@code false} outside a session or if the module is not part of it
     */
    public static boolean isRecording(String moduleId) {
        TelemetryRecorder r = recorder;
        if (r == null) return false;
        try {
            return r.isRecording(moduleId);
        } catch (Throwable t) {
            reportFailure("isRecording(" + moduleId + ")", t);
            return false;
        }
    }

    /**
     * Tells whether an event of the given category and level would be recorded.
     * Use it to guard a block of expensive attribute computations.
     *
     * @param category a module category, resolved with {@link TelemetryRegistry#ownerOf}
     * @param level    level of the event
     * @return {@code true} if the event would be recorded
     */
    public static boolean isEnabled(TelemetryCategory category, TelemetryLevel level) {
        TelemetryRecorder r = recorder;
        if (r == null) return false;
        try {
            TelemetryRegistry.ModuleEntry owner = TelemetryRegistry.ownerOf(category);
            return owner != null && r.level(owner.id(), category.id()).allows(level);
        } catch (Throwable t) {
            reportFailure("isEnabled", t);
            return false;
        }
    }

    /**
     * Same as {@link #isEnabled(TelemetryCategory, TelemetryLevel)} with an explicit module,
     * required for {@link BuiltinCategory built-in categories}.
     *
     * @param moduleId module id
     * @param category one of the module's categories, or a built-in one
     * @param level    level of the event
     * @return {@code true} if the event would be recorded
     */
    public static boolean isEnabled(String moduleId, TelemetryCategory category, TelemetryLevel level) {
        TelemetryRecorder r = recorder;
        if (r == null) return false;
        try {
            return belongsTo(moduleId, category) && r.level(moduleId, category.id()).allows(level);
        } catch (Throwable t) {
            reportFailure("isEnabled(" + moduleId + ")", t);
            return false;
        }
    }

    // ---- Events ---------------------------------------------------------------------------------

    /**
     * Starts an event in a module category. The module is resolved from the category instance.
     *
     * @param category a module category; built-in ones need {@link #event(String, TelemetryCategory, TelemetryLevel)}
     * @param level    level of the event, not {@link TelemetryLevel#OFF}
     * @return a builder, or {@link EventBuilder#NOOP} if the event will not be recorded
     */
    public static EventBuilder event(TelemetryCategory category, TelemetryLevel level) {
        TelemetryRecorder r = recorder;
        if (r == null) return EventBuilder.NOOP;
        try {
            TelemetryRegistry.ModuleEntry owner = TelemetryRegistry.ownerOf(category);
            if (owner == null) return EventBuilder.NOOP;
            return start(r, owner.id(), category, level);
        } catch (Throwable t) {
            reportFailure("event", t);
            return EventBuilder.NOOP;
        }
    }

    /**
     * Starts an event with an explicit module, required for {@link BuiltinCategory built-in categories}.
     *
     * @param moduleId module id
     * @param category one of the module's categories, or a built-in one
     * @param level    level of the event, not {@link TelemetryLevel#OFF}
     * @return a builder, or {@link EventBuilder#NOOP} if the event will not be recorded
     */
    public static EventBuilder event(String moduleId, TelemetryCategory category, TelemetryLevel level) {
        TelemetryRecorder r = recorder;
        if (r == null) return EventBuilder.NOOP;
        try {
            if (!belongsTo(moduleId, category)) return EventBuilder.NOOP;
            return start(r, moduleId, category, level);
        } catch (Throwable t) {
            reportFailure("event(" + moduleId + ")", t);
            return EventBuilder.NOOP;
        }
    }

    /**
     * Starts an {@link TelemetryLevel#ERROR} event.
     *
     * @param category a module category
     * @return a builder, or {@link EventBuilder#NOOP}
     */
    public static EventBuilder error(TelemetryCategory category) {
        return event(category, TelemetryLevel.ERROR);
    }

    /**
     * Starts a {@link TelemetryLevel#WARN} event.
     *
     * @param category a module category
     * @return a builder, or {@link EventBuilder#NOOP}
     */
    public static EventBuilder warn(TelemetryCategory category) {
        return event(category, TelemetryLevel.WARN);
    }

    /**
     * Starts an {@link TelemetryLevel#INFO} event.
     *
     * @param category a module category
     * @return a builder, or {@link EventBuilder#NOOP}
     */
    public static EventBuilder info(TelemetryCategory category) {
        return event(category, TelemetryLevel.INFO);
    }

    /**
     * Starts a {@link TelemetryLevel#DEBUG} event.
     *
     * @param category a module category
     * @return a builder, or {@link EventBuilder#NOOP}
     */
    public static EventBuilder debug(TelemetryCategory category) {
        return event(category, TelemetryLevel.DEBUG);
    }

    /**
     * Starts a {@link TelemetryLevel#TRACE} event.
     *
     * @param category a module category
     * @return a builder, or {@link EventBuilder#NOOP}
     */
    public static EventBuilder trace(TelemetryCategory category) {
        return event(category, TelemetryLevel.TRACE);
    }

    /**
     * Creates a real builder if the category's threshold lets the level through.
     * May throw: callers guard it.
     *
     * @return a builder, or {@link EventBuilder#NOOP}
     */
    private static EventBuilder start(TelemetryRecorder r, String moduleId, TelemetryCategory category, TelemetryLevel level) {
        if (level == TelemetryLevel.OFF || !r.level(moduleId, category.id()).allows(level)) return EventBuilder.NOOP;
        return new EventBuilder(r, moduleId, category.id(), level);
    }

    /**
     * A module may only emit its own categories and the built-in ones. May throw: callers guard it.
     *
     * @return {@code true} if {@code category} is built-in or owned by {@code moduleId}
     */
    private static boolean belongsTo(String moduleId, TelemetryCategory category) {
        if (category instanceof BuiltinCategory) return true;
        TelemetryRegistry.ModuleEntry owner = TelemetryRegistry.ownerOf(category);
        return owner != null && owner.id().equals(moduleId);
    }

    // ---- Metrics and due-checks -----------------------------------------------------------------

    /**
     * Returns the metric set of a subject for the running session.
     *
     * @param moduleId module id
     * @param subject  what the metrics describe
     * @return the subject's metric set, or {@link MetricSet#NOOP} if the module is not recorded
     */
    public static MetricSet metrics(String moduleId, Subject subject) {
        TelemetryRecorder r = recorder;
        if (r == null) return MetricSet.NOOP;
        try {
            if (!r.isRecording(moduleId)) return MetricSet.NOOP;
            return r.metrics(moduleId, subject);
        } catch (Throwable t) {
            reportFailure("metrics(" + moduleId + ")", t);
            return MetricSet.NOOP;
        }
    }

    /**
     * Tells whether the module's metrics are written at the end of this tick.
     * Use it to compute expensive gauges only when they are actually written.
     *
     * @param moduleId module id
     * @return {@code true} on a metrics tick of a recorded module
     */
    public static boolean isMetricsDue(String moduleId) {
        TelemetryRecorder r = recorder;
        if (r == null) return false;
        try {
            return r.isMetricsDue(moduleId);
        } catch (Throwable t) {
            reportFailure("isMetricsDue(" + moduleId + ")", t);
            return false;
        }
    }

    /**
     * Tells whether the module's invariants should be checked during this tick.
     *
     * @param moduleId module id
     * @return {@code true} on a health-check tick of a recorded module
     */
    public static boolean isHealthCheckDue(String moduleId) {
        TelemetryRecorder r = recorder;
        if (r == null) return false;
        try {
            return r.isHealthCheckDue(moduleId);
        } catch (Throwable t) {
            reportFailure("isHealthCheckDue(" + moduleId + ")", t);
            return false;
        }
    }

    /**
     * Starts a health check of a subject. Call it when {@link #isHealthCheckDue(String)} is true,
     * and always end it with {@link HealthCheck#commit()}, even when every invariant holds.
     *
     * @param moduleId module id
     * @param subject  the checked subject
     * @return a check, or {@link HealthCheck#NOOP} if the module is not recorded
     */
    public static HealthCheck health(String moduleId, Subject subject) {
        TelemetryRecorder r = recorder;
        if (r == null) return HealthCheck.NOOP;
        try {
            HealthTracker tracker = r.health(moduleId);
            return tracker == null ? HealthCheck.NOOP : new HealthCheck(tracker, subject);
        } catch (Throwable t) {
            reportFailure("health(" + moduleId + ")", t);
            return HealthCheck.NOOP;
        }
    }

    // ---- Internal -------------------------------------------------------------------------------

    /**
     * Installs the recorder of a starting session and resets the failure counter.
     *
     * @param newRecorder the session
     * @throws IllegalStateException if another recorder is installed
     */
    static synchronized void install(TelemetryRecorder newRecorder) {
        if (recorder != null) throw new IllegalStateException("A telemetry session is already running");
        FAILURES.set(0);
        recorder = newRecorder;
    }

    /**
     * Removes the recorder of a stopping session. Does nothing if another recorder is installed.
     *
     * @param oldRecorder the session that stops
     */
    static synchronized void uninstall(TelemetryRecorder oldRecorder) {
        if (recorder == oldRecorder) recorder = null;
    }

    /**
     * Logs a telemetry failure. Only the first {@value #MAX_REPORTED_FAILURES} of a session are
     * logged, to keep latest.log readable. Never emits telemetry (no feedback loop).
     * <p>
     * <b>Internal:</b> used by the telemetry packages ({@code health}, {@code metrics}) and by the
     * built-in modules; public because they live in other packages.
     *
     * @param what what failed, for the log message
     * @param t    the cause
     */
    @ApiStatus.Internal
    public static void reportFailure(String what, Throwable t) {
        int n = FAILURES.incrementAndGet();
        if (n <= MAX_REPORTED_FAILURES)
            LOGGER.error("Telemetry failure in {} ({}/{} reported)", what, n, MAX_REPORTED_FAILURES, t);
    }
}
