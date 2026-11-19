package net.nuclearteam.createnuclear.foundation.telemetry.metrics;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Distribution of observed values over one metrics interval.
 * <p>
 * Count, min, max and mean are exact. Percentiles are computed on a uniform sample of at most
 * {@link #CAPACITY} values (reservoir sampling, algorithm R), so memory stays bounded even for a
 * value observed every tick. Not thread-safe: guarded by its {@link MetricSet}.
 */
public final class Histogram {
    /** Maximum number of values kept for percentiles. */
    public static final int CAPACITY = 1024;

    private final double[] sample = new double[CAPACITY];
    private final SplittableRandom random = new SplittableRandom();
    private int size;
    private long count;
    private double sum;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    /**
     * Records a value. {@code NaN} and infinite values are ignored.
     *
     * @param value the observed value
     */
    public void observe(double value) {
        if (!Double.isFinite(value)) return;
        count++;
        sum += value;
        if (value < min) min = value;
        if (value > max) max = value;

        if (size < CAPACITY) {
            sample[size++] = value;
        } else {
            long j = random.nextLong(count); // keep each value with probability CAPACITY / count
            if (j < CAPACITY) sample[(int) j] = value;
        }
    }

    /**
     * Returns the number of values observed since the last reset.
     *
     * @return the exact count
     */
    public long count() {
        return count;
    }

    /**
     * Computes the statistics of the current interval.
     *
     * @return the statistics, with every value at 0 if nothing was observed
     */
    public Snapshot snapshot() {
        if (count == 0) return new Snapshot(0, 0, 0, 0, 0, 0, 0);
        double[] sorted = Arrays.copyOf(sample, size);
        Arrays.sort(sorted);
        return new Snapshot(count, min, max, sum / count,
                percentile(sorted, 0.50), percentile(sorted, 0.95), percentile(sorted, 0.99));
    }

    /** Clears every value; called after each metrics dump. */
    public void reset() {
        size = 0;
        count = 0;
        sum = 0;
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
    }

    /** Nearest-rank percentile on a sorted, non-empty array. */
    private static double percentile(double[] sorted, double p) {
        int rank = (int) Math.ceil(p * sorted.length);
        return sorted[Math.max(0, Math.min(sorted.length - 1, rank - 1))];
    }

    /**
     * Statistics of one interval.
     *
     * @param count exact number of observed values
     * @param min   exact minimum
     * @param max   exact maximum
     * @param mean  exact mean
     * @param p50   median, from the sample
     * @param p95   95th percentile, from the sample
     * @param p99   99th percentile, from the sample
     */
    public record Snapshot(long count, double min, double max, double mean, double p50, double p95, double p99) {}
}
