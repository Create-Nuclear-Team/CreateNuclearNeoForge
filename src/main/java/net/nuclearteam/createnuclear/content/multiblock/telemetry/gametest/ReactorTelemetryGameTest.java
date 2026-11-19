package net.nuclearteam.createnuclear.content.multiblock.telemetry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nuclearteam.createnuclear.CNBlocks;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.multiblock.controller.manager.ReactorOutputManager;
import net.nuclearteam.createnuclear.content.multiblock.output.ReactorOutputEntity;
import net.nuclearteam.createnuclear.content.multiblock.telemetry.ReactorTelemetry;
import net.nuclearteam.createnuclear.content.multiblock.telemetry.ReactorTelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetrySession;
import net.nuclearteam.createnuclear.foundation.telemetry.sink.TextFileSink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * GameTest coverage for {@link ReactorTelemetry#afterControllerTick} and
 * {@link ReactorTelemetry#afterOutputTick} inside a real {@code reactor} session. Each test starts
 * its own session, so each has its own batch.
 */
@GameTestHolder(CreateNuclear.MOD_ID)
@PrefixGameTestTemplate(false)
public class ReactorTelemetryGameTest {

    private static final String STRUCTURE = "empty_platform";

    /** Fails if {@code text} does not contain {@code expected}. */
    private static void checkContains(String text, String expected) {
        if (!text.contains(expected))
            throw new GameTestAssertException("telemetry.log should contain <" + expected + ">:\n" + text);
    }

    /** Deletes the session directory; failures are ignored. */
    private static void delete(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
        }
    }

    /**
     * A tracked output position holding no output is reported as {@code io_missing_output}, and the
     * controller metrics are written. Outside a session, the call does nothing.
     */
    @GameTest(template = STRUCTURE, batch = "telemetry_session_reactor")
    public static void missingOutputIsReported(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controller = helper.absolutePos(new BlockPos(1, 2, 1));
        ReactorOutputManager outputs = new ReactorOutputManager();
        outputs.addBlock(new BlockPos(1, 0, 0)); // air: tracked, but no output block entity
        ReactorTelemetry.ControllerSample sample = new ReactorTelemetry.ControllerSample(true, false, false, 0, 3, 0);

        ReactorTelemetry.afterControllerTick(level, controller, sample, outputs); // no session: no-op

        TelemetrySession.StartResult result = TelemetrySession.start(level.getServer(), 10, ReactorTelemetryModule.ID, false, null);
        if (!result.started()) throw new GameTestAssertException("session did not start: " + result.error());
        TelemetrySession session = result.session();
        Path dir = session.info().directory();
        String content;
        // The outer finally deletes the session directory on every path, including an early failure.
        try {
            try {
                if (!session.sinkIds().contains(TextFileSink.ID))
                    throw new GameTestAssertException("sink_text is disabled in the test config");
                // Session tick 0: metrics and health checks are both due.
                ReactorTelemetry.afterControllerTick(level, controller, sample, outputs);
            } finally {
                session.stop(TelemetrySession.StopReason.COMMAND);
            }
            content = Files.readString(dir.resolve(TextFileSink.FILE_NAME), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GameTestAssertException("cannot read telemetry.log: " + e);
        } finally {
            delete(dir);
        }

        String subject = ReactorTelemetry.subject(level, controller).toString();
        checkContains(content, "[reactor/metrics][" + subject + "] metrics {");
        checkContains(content, "outputs_tracked=");
        checkContains(content, "[reactor/health][" + subject + "] violation started: " + ReactorTelemetry.IO_MISSING_OUTPUT);
        if (content.contains("violation started: output_spinning"))
            throw new GameTestAssertException("no output spins, yet an output_spinning invariant was violated:\n" + content);
        helper.succeed();
    }

    /**
     * A lone controller placed during a reactor session is checked by afterControllerTick, and
     * breaking it reports "controller removed". Nothing is violated.
     */
    @GameTest(template = STRUCTURE, timeoutTicks = 100, batch = "telemetry_session_reactor_controller")
    public static void loneControllerIsObserved(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 2, 1);
        BlockPos controller = helper.absolutePos(rel);

        TelemetrySession.StartResult result = TelemetrySession.start(level.getServer(), 10, ReactorTelemetryModule.ID, false, null);
        if (!result.started()) throw new GameTestAssertException("session did not start: " + result.error());
        TelemetrySession session = result.session();
        Path dir = session.info().directory();
        if (!session.sinkIds().contains(TextFileSink.ID)) {
            session.stop(TelemetrySession.StopReason.COMMAND);
            delete(dir);
            throw new GameTestAssertException("sink_text is disabled in the test config");
        }

        helper.setBlock(rel, CNBlocks.REACTOR_CONTROLLER.get());
        helper.runAfterDelay(25, () -> {
            String content;
            try {
                try {
                    helper.destroyBlock(rel);
                } finally {
                    session.stop(TelemetrySession.StopReason.COMMAND);
                }
                content = Files.readString(dir.resolve(TextFileSink.FILE_NAME), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new GameTestAssertException("cannot read telemetry.log: " + e);
            } finally {
                delete(dir);
            }

            String subject = ReactorTelemetry.subject(level, controller).toString();
            checkContains(content, "[reactor/health][" + subject + "] health summary {checks=");
            checkContains(content, "[reactor/assembly][" + subject + "] controller removed {assembled=false, outputs=0}");
            if (content.contains("violation started"))
                throw new GameTestAssertException("a lone controller violated an invariant:\n" + content);
            helper.succeed();
        });
    }

    /** Timeout of {@link #spinningOutputWithoutControllerIsOrphan}; an annotation needs a constant. */
    private static final int ORPHAN_TEST_TIMEOUT = 300;
    /** Ticks waited after the grace delay: one health check interval (20) plus a margin. */
    private static final int ORPHAN_CHECK_MARGIN = 45;

    /**
     * A spinning output that no controller ever drove is reported as orphan_output_spinning
     * ("no controller recorded") once orphan_grace_ticks have passed. Fails at once, with a clear
     * message, if the configured grace delay does not fit in the test timeout.
     */
    @GameTest(template = STRUCTURE, timeoutTicks = ORPHAN_TEST_TIMEOUT, batch = "telemetry_session_reactor_output")
    public static void spinningOutputWithoutControllerIsOrphan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 2, 1);
        BlockPos output = helper.absolutePos(rel);
        int grace = ReactorTelemetryModule.orphanGraceTicks();
        int checkAt = grace + ORPHAN_CHECK_MARGIN;
        if (checkAt >= ORPHAN_TEST_TIMEOUT)
            throw new GameTestAssertException("orphan_grace_ticks=" + grace + " is too large for this test: at most "
                    + (ORPHAN_TEST_TIMEOUT - ORPHAN_CHECK_MARGIN - 1) + " (timeout " + ORPHAN_TEST_TIMEOUT + " ticks)");

        TelemetrySession.StartResult result = TelemetrySession.start(level.getServer(), 30, ReactorTelemetryModule.ID, false, null);
        if (!result.started()) throw new GameTestAssertException("session did not start: " + result.error());
        TelemetrySession session = result.session();
        Path dir = session.info().directory();
        if (!session.sinkIds().contains(TextFileSink.ID)) {
            session.stop(TelemetrySession.StopReason.COMMAND);
            delete(dir);
            throw new GameTestAssertException("sink_text is disabled in the test config");
        }

        helper.setBlock(rel, CNBlocks.REACTOR_OUTPUT.get());
        // After the first tick, kinetics are initialised and the speed can be set.
        helper.runAfterDelay(5, () -> {
            if (!(level.getBlockEntity(output) instanceof ReactorOutputEntity entity))
                throw new GameTestAssertException("no reactor output block entity");
            entity.setSpeedAndUpdate(16);
        });
        // Grace delay, plus one health check interval (20 ticks) and a margin.
        helper.runAfterDelay(checkAt, () -> {
            String content;
            try {
                session.stop(TelemetrySession.StopReason.COMMAND);
                content = Files.readString(dir.resolve(TextFileSink.FILE_NAME), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new GameTestAssertException("cannot read telemetry.log: " + e);
            } finally {
                delete(dir);
            }

            String subject = ReactorTelemetry.outputSubject(level, output).toString();
            checkContains(content, "[reactor/health][" + subject + "] violation started: " + ReactorTelemetry.ORPHAN_OUTPUT_SPINNING);
            checkContains(content, "no controller recorded: speed=16.0");
            helper.succeed();
        });
    }
}

