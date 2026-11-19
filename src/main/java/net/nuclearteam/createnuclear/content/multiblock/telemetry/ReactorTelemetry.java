package net.nuclearteam.createnuclear.content.multiblock.telemetry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlockEntity;
import net.nuclearteam.createnuclear.content.multiblock.controller.manager.ReactorOutputManagerI;
import net.nuclearteam.createnuclear.content.multiblock.output.ReactorOutputEntity;
import net.nuclearteam.createnuclear.foundation.telemetry.Telemetry;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.metrics.MetricSet;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Reactor-side helpers of the {@value ReactorTelemetryModule#ID} module: subjects, invariant names,
 * the periodic controller check and the orphan output check.
 * <p>
 * Every method is cheap outside a session and never throws into the reactor code.
 */
public final class ReactorTelemetry {
    /** Subject type of a reactor, located at its controller. */
    public static final String REACTOR = "reactor";
    /** Subject type of a reactor output. */
    public static final String REACTOR_OUTPUT = "reactor_output";

    /** Assembled, not running, yet an output still generates speed. */
    public static final String OUTPUT_SPINNING_WHILE_STOPPED = "output_spinning_while_stopped";
    /** Not assembled, yet an output still generates speed. */
    public static final String OUTPUT_SPINNING_WHILE_DISASSEMBLED = "output_spinning_while_disassembled";
    /** A tracked output, in a loaded chunk, has no output block entity any more. */
    public static final String IO_MISSING_OUTPUT = "io_missing_output";
    /** An output spins although its controller is gone; checked by the output itself. */
    public static final String ORPHAN_OUTPUT_SPINNING = "orphan_output_spinning";

    private ReactorTelemetry() {}

    /**
     * Returns the subject of a reactor.
     *
     * @param level         the reactor's level
     * @param controllerPos the controller position
     * @return {@code reactor@dimension[x,y,z]}
     */
    public static Subject subject(Level level, BlockPos controllerPos) {
        return Subject.atBlock(REACTOR, level, controllerPos);
    }

    /**
     * Returns the subject of a reactor output.
     *
     * @param level     the output's level
     * @param outputPos the output position
     * @return {@code reactor_output@dimension[x,y,z]}
     */
    public static Subject outputSubject(Level level, BlockPos outputPos) {
        return Subject.atBlock(REACTOR_OUTPUT, level, outputPos);
    }

    /**
     * Tells whether {@link #afterControllerTick} has anything to do this tick.
     *
     * @return {@code true} if the reactor module's metrics or health checks are due
     */
    public static boolean isDue() {
        return Telemetry.isMetricsDue(ReactorTelemetryModule.ID) || Telemetry.isHealthCheckDue(ReactorTelemetryModule.ID);
    }

    /**
     * State of a controller at the end of its tick, built by the controller only when {@link #isDue()}.
     *
     * @param assembled whether the structure is assembled
     * @param running   whether the reactor can run ({@code canRun})
     * @param danger    whether the heat is in the danger zone
     * @param heat      current heat
     * @param size      multiblock size
     * @param fuelRods  fuel rods required by the pattern
     */
    public record ControllerSample(boolean assembled, boolean running, boolean danger, int heat, int size, int fuelRods) {}

    /**
     * Writes the controller metrics and checks its invariants, each only when due. Reads the speed
     * of the tracked outputs; never iterates over the world. Never throws: a failure is reported
     * with {@link Telemetry#reportFailure}.
     * <p>
     * Does nothing on the client: the session is static, so in single player the client and the
     * integrated server see the same one, and a client call would record everything twice.
     *
     * @param level         the controller's level
     * @param controllerPos the controller position
     * @param sample        the controller state
     * @param outputs       the controller's output manager
     */
    public static void afterControllerTick(Level level, BlockPos controllerPos, ControllerSample sample, ReactorOutputManagerI outputs) {
        try {
            if (level.isClientSide()) return;
            boolean metricsDue = Telemetry.isMetricsDue(ReactorTelemetryModule.ID);
            boolean healthDue = Telemetry.isHealthCheckDue(ReactorTelemetryModule.ID);
            if (!metricsDue && !healthDue) return;

            OutputScan scan = OutputScan.of(level, controllerPos, outputs);
            Subject subject = subject(level, controllerPos);

            if (metricsDue) {
                MetricSet m = Telemetry.metrics(ReactorTelemetryModule.ID, subject);
                m.gauge("heat", sample.heat());
                m.gauge("size", sample.size());
                m.gauge("fuel_rods", sample.fuelRods());
                m.gauge("assembled", sample.assembled() ? 1 : 0);
                m.gauge("running", sample.running() ? 1 : 0);
                m.gauge("danger", sample.danger() ? 1 : 0);
                m.gauge("outputs_tracked", scan.tracked());
                m.gauge("outputs_loaded", scan.loaded());
                m.gauge("output_speed_max", scan.maxSpeed());
                m.gauge("output_speed_total", scan.totalSpeed());
            }
            if (healthDue) {
                Telemetry.health(ReactorTelemetryModule.ID, subject)
                        .check(OUTPUT_SPINNING_WHILE_STOPPED,
                                sample.assembled() && !sample.running() && scan.maxSpeed() != 0,
                                () -> "max_speed=" + scan.maxSpeed() + " heat=" + sample.heat())
                        .check(OUTPUT_SPINNING_WHILE_DISASSEMBLED,
                                !sample.assembled() && scan.maxSpeed() != 0,
                                () -> "max_speed=" + scan.maxSpeed())
                        .check(IO_MISSING_OUTPUT, scan.missing() > 0,
                                () -> "missing=" + scan.missing() + " tracked=" + scan.tracked())
                        .commit();
            }
        } catch (Throwable t) {
            Telemetry.reportFailure("reactor controller telemetry", t);
        }
    }

    /**
     * Outputs of one controller, read once per due tick.
     *
     * @param tracked    tracked positions
     * @param loaded     tracked positions holding a loaded output
     * @param missing    tracked positions in a loaded chunk without an output
     * @param maxSpeed   highest absolute generated speed among loaded outputs
     * @param totalSpeed sum of the absolute generated speeds
     */
    private record OutputScan(int tracked, int loaded, int missing, float maxSpeed, float totalSpeed) {
        /** Reads every tracked output; positions in unloaded chunks are skipped. */
        static OutputScan of(Level level, BlockPos controllerPos, ReactorOutputManagerI outputs) {
            List<BlockPos> positions = outputs.getAbsolutePositions(controllerPos);
            int loaded = 0;
            int missing = 0;
            float max = 0;
            float total = 0;
            for (BlockPos p : positions) {
                if (!level.isLoaded(p)) continue;
                if (level.getBlockEntity(p) instanceof ReactorOutputEntity output) {
                    loaded++;
                    float speed = Math.abs(output.getGeneratedSpeed());
                    max = Math.max(max, speed);
                    total += speed;
                } else {
                    missing++;
                }
            }
            return new OutputScan(positions.size(), loaded, missing, max, total);
        }
    }

    /**
     * Checks {@value #ORPHAN_OUTPUT_SPINNING} for one output, only when health checks are due and
     * after {@code orphan_grace_ticks}. Skipped, without commit, while the controller's chunk is
     * not loaded, so an open violation keeps its state. Does nothing on the client. Never throws.
     *
     * @param level          the output's level
     * @param outputPos      the output position
     * @param controllerPos  the controller that last drove it, or {@code null}
     * @param generatedSpeed the speed the output generates
     * @param ticksSinceLoad server ticks since the output was loaded or placed
     */
    public static void afterOutputTick(Level level, BlockPos outputPos, @Nullable BlockPos controllerPos,
                                       float generatedSpeed, int ticksSinceLoad) {
        try {
            if (level.isClientSide()) return;
            if (!Telemetry.isHealthCheckDue(ReactorTelemetryModule.ID)) return;
            if (ticksSinceLoad < ReactorTelemetryModule.orphanGraceTicks()) return;

            ControllerLink link = ControllerLink.of(level, outputPos, controllerPos);
            if (link == ControllerLink.UNLOADED) return;
            Telemetry.health(ReactorTelemetryModule.ID, outputSubject(level, outputPos))
                    .check(ORPHAN_OUTPUT_SPINNING, generatedSpeed != 0 && link != ControllerLink.OK,
                            () -> link.detail + ": speed=" + generatedSpeed
                                    + (controllerPos == null ? "" : " controller=" + controllerPos.toShortString().replace(" ", "")))
                    .commit();
        } catch (Throwable t) {
            Telemetry.reportFailure("reactor output telemetry", t);
        }
    }

    /** How an output relates to the controller that last drove it. */
    private enum ControllerLink {
        /** The controller exists and tracks this output. */
        OK("controller ok"),
        /** The controller's chunk is not loaded: nothing can be said. */
        UNLOADED("controller chunk not loaded"),
        /** No controller drove this output since it was placed. */
        NONE("no controller recorded"),
        /** The controller block entity is gone: the original bug. */
        MISSING("controller block entity missing"),
        /** The controller exists but no longer tracks this output. */
        UNTRACKED("controller does not track this output");

        /** Text used in the violation detail. */
        final String detail;

        ControllerLink(String detail) {
            this.detail = detail;
        }

        /** Resolves the link; reads one block entity at most. */
        static ControllerLink of(Level level, BlockPos outputPos, @Nullable BlockPos controllerPos) {
            if (controllerPos == null) return NONE;
            if (!level.isLoaded(controllerPos)) return UNLOADED;
            if (!(level.getBlockEntity(controllerPos) instanceof ReactorControllerBlockEntity controller)) return MISSING;
            return controller.getOutputManager().contains(outputPos.subtract(controllerPos)) ? OK : UNTRACKED;
        }
    }
}
