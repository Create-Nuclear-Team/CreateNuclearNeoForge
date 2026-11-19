package net.nuclearteam.createnuclear.foundation.telemetry;

import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryEvent;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.health.HealthTracker;
import net.nuclearteam.createnuclear.foundation.telemetry.metrics.MetricSet;

import javax.annotation.Nullable;

/**
 * Receiver of everything the {@link Telemetry} facade lets through. Implemented by the running
 * session (step 8) and installed with {@link Telemetry#install}.
 * <p>
 * Package-private: only the session may record. Implementations must be safe to call from the
 * server thread at any time while installed.
 */
interface TelemetryRecorder {
    /**
     * Returns the session-relative time.
     *
     * @return server ticks since the session started
     */
    long tick();

    /**
     * Tells whether a module is recorded by this session.
     *
     * @param moduleId module id
     * @return {@code true} if the module takes part in the session
     */
    boolean isRecording(String moduleId);

    /**
     * Returns the current threshold of a category.
     *
     * @param moduleId   module id
     * @param categoryId category id, built-in ones included
     * @return the threshold, or {@link TelemetryLevel#OFF} if the module is not recorded
     */
    TelemetryLevel level(String moduleId, String categoryId);

    /**
     * Records an event. The event is not yet cleaned by the PrivacyFilter.
     *
     * @param event the event to record
     */
    void record(TelemetryEvent event);

    /**
     * Returns the metric set of a subject, creating it if needed.
     *
     * @param moduleId module id, known to be recorded
     * @param subject  the subject the metrics describe
     * @return the subject's metric set for this session
     */
    MetricSet metrics(String moduleId, Subject subject);

    /**
     * Tells whether the module's metrics will be written at the end of the current tick.
     *
     * @param moduleId module id
     * @return {@code true} on a metrics tick of a recorded module
     */
    boolean isMetricsDue(String moduleId);

    /**
     * Tells whether the module's invariants should be checked during the current tick.
     *
     * @param moduleId module id
     * @return {@code true} on a health-check tick of a recorded module
     */
    boolean isHealthCheckDue(String moduleId);

    /**
     * Returns the health tracker of a module.
     *
     * @param moduleId module id
     * @return the module's tracker for this session, or {@code null} if the module is not recorded
     */
    @Nullable
    HealthTracker health(String moduleId);
}
