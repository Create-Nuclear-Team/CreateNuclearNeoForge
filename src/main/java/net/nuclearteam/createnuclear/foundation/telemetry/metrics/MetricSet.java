package net.nuclearteam.createnuclear.foundation.telemetry.metrics;

import net.nuclearteam.createnuclear.foundation.telemetry.Telemetry;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryIds;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metrics of one subject in one module, for the running session. Obtained from
 * {@link Telemetry#metrics(String, Subject)}.
 * <p>
 * Three kinds, each name being bound to a single kind:
 * <ul>
 *   <li><b>counter</b> ({@link #inc}): only increases, cumulated over the whole session;</li>
 *   <li><b>gauge</b> ({@link #gauge}): instantaneous value, the last one is kept;</li>
 *   <li><b>histogram</b> ({@link #observe}): distribution, reset after each metrics dump.</li>
 * </ul>
 * A subject is written at a metrics dump only if one of its metrics was updated since the
 * previous dump (see {@link #drain}).
 * <p>
 * Names are snake_case. A misuse (invalid name, kind conflict) is logged and ignored.
 * Thread-safe; {@link #NOOP} ignores everything.
 */
public final class MetricSet {
    /** Shared metric set that ignores everything. */
    public static final MetricSet NOOP = new MetricSet("", Subject.SERVER);

    private final String moduleId;
    private final Subject subject;
    private final Map<String, Object> metrics = new LinkedHashMap<>();
    /** Whether a metric was updated since the last {@link #drain}; guarded by this. */
    private boolean updated;

    /**
     * Creates an empty metric set. <b>Internal:</b> only the session creates metric sets; public
     * because the session lives in another package. Instrumented code uses {@link Telemetry#metrics}.
     *
     * @param moduleId module id
     * @param subject  the subject the metrics describe
     */
    @ApiStatus.Internal
    public MetricSet(String moduleId, Subject subject) {
        this.moduleId = moduleId;
        this.subject = subject;
    }

    /**
     * Returns the module these metrics belong to.
     *
     * @return the module id
     */
    public String moduleId() {
        return moduleId;
    }

    /**
     * Returns the subject these metrics describe.
     *
     * @return the subject
     */
    public Subject subject() {
        return subject;
    }

    /**
     * Adds 1 to a counter.
     *
     * @param name snake_case counter name
     * @return this metric set
     */
    public MetricSet inc(String name) {
        return inc(name, 1);
    }

    /**
     * Adds to a counter. Negative deltas are ignored: a counter only increases.
     *
     * @param name  snake_case counter name
     * @param delta amount to add
     * @return this metric set
     */
    public MetricSet inc(String name, long delta) {
        if (this == NOOP || delta < 0) return this;
        synchronized (this) {
            Counter c = get(name, Counter.class);
            if (c != null) {
                c.value += delta;
                updated = true;
            }
        }
        return this;
    }

    /**
     * Sets a gauge.
     *
     * @param name  snake_case gauge name
     * @param value current value
     * @return this metric set
     */
    public MetricSet gauge(String name, double value) {
        if (this == NOOP) return this;
        synchronized (this) {
            Gauge g = get(name, Gauge.class);
            if (g != null) {
                g.value = value;
                updated = true;
            }
        }
        return this;
    }

    /**
     * Records a value in a histogram.
     *
     * @param name  snake_case histogram name
     * @param value observed value
     * @return this metric set
     */
    public MetricSet observe(String name, double value) {
        if (this == NOOP) return this;
        synchronized (this) {
            Histogram h = get(name, Histogram.class);
            if (h != null) {
                h.observe(value);
                updated = true;
            }
        }
        return this;
    }

    /**
     * Returns the current values as event attributes, then resets the histograms.
     * <b>Internal:</b> called by the session at each metrics dump; public because the session
     * lives in another package.
     * <p>
     * A counter or gauge {@code x} gives {@code x}; a histogram {@code x} gives {@code x_count},
     * and when not empty {@code x_p50}, {@code x_p95}, {@code x_p99} and {@code x_max}.
     * <p>
     * Nothing is returned if no metric was updated since the previous call: a subject that stopped
     * updating its metrics (a broken reactor, an unloaded chunk) is no longer written, and its last
     * line stays its last known state. Setting a gauge to the same value counts as an update.
     *
     * @return attributes in declaration order; empty if no metric was updated since the last call
     */
    @ApiStatus.Internal
    public synchronized Map<String, Object> drain() {
        if (!updated) return Map.of();
        updated = false;
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : metrics.entrySet()) {
            String name = e.getKey();
            Object m = e.getValue();
            if (m instanceof Counter c) {
                out.put(name, c.value);
            } else if (m instanceof Gauge g) {
                out.put(name, g.value);
            } else if (m instanceof Histogram h) {
                Histogram.Snapshot s = h.snapshot();
                out.put(name + "_count", s.count());
                if (s.count() > 0) {
                    out.put(name + "_p50", s.p50());
                    out.put(name + "_p95", s.p95());
                    out.put(name + "_p99", s.p99());
                    out.put(name + "_max", s.max());
                }
                h.reset();
            }
        }
        return out;
    }

    /** Returns the metric of that name, creating it; {@code null} on misuse. Caller holds the lock. */
    @Nullable
    private <T> T get(String name, Class<T> kind) {
        Object existing = metrics.get(name);
        if (existing == null) {
            if (!TelemetryIds.isValid(name)) {
                Telemetry.reportFailure("metric '" + name + "' of " + moduleId,
                        new IllegalArgumentException("Invalid metric name, expected [a-z0-9_]+"));
                return null;
            }
            T created = newMetric(kind);
            metrics.put(name, created);
            return created;
        }
        if (!kind.isInstance(existing)) {
            Telemetry.reportFailure("metric '" + name + "' of " + moduleId, new IllegalArgumentException(
                    "Already a " + existing.getClass().getSimpleName() + ", used as a " + kind.getSimpleName()));
            return null;
        }
        return kind.cast(existing);
    }

    private static <T> T newMetric(Class<T> kind) {
        if (kind == Counter.class) return kind.cast(new Counter());
        if (kind == Gauge.class) return kind.cast(new Gauge());
        return kind.cast(new Histogram());
    }

    private static final class Counter {
        long value;
    }

    private static final class Gauge {
        double value;
    }
}
