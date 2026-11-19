package net.nuclearteam.createnuclear.foundation.telemetry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetrySession;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryConfigs;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryModuleConfig;
import net.nuclearteam.createnuclear.foundation.telemetry.module.EnvironmentTelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.sink.TextFileSink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * End-to-end GameTest of a real session: {@code environment} recorded through the {@code text} sink,
 * from {@link TelemetrySession#start} to {@code session stopped}.
 */
@GameTestHolder(CreateNuclear.MOD_ID)
@PrefixGameTestTemplate(false)
public class TelemetrySessionGameTest {

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

    /** Start, 30 ticks, stop: the file holds the whole timeline, without the absolute game directory. */
    @GameTest(template = STRUCTURE, timeoutTicks = 200, batch = "telemetry_session_environment")
    public static void environmentSessionWritesTimeline(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        TelemetrySession.StartResult result = TelemetrySession.start(server, 10, EnvironmentTelemetryModule.ID, false, null);
        if (!result.started()) throw new GameTestAssertException("session did not start: " + result.error());
        TelemetrySession session = result.session();
        Path dir = session.info().directory();
        if (!session.sinkIds().contains(TextFileSink.ID)) {
            session.stop(TelemetrySession.StopReason.COMMAND);
            delete(dir);
            throw new GameTestAssertException("sink_text is disabled in the test config");
        }

        helper.runAfterDelay(30, () -> {
            session.stop(TelemetrySession.StopReason.COMMAND);
            String content;
            try {
                content = Files.readString(dir.resolve(TextFileSink.FILE_NAME), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new GameTestAssertException("cannot read telemetry.log: " + e);
            } finally {
                delete(dir);
            }

            checkContains(content, "# CreateNuclear telemetry, format 1");
            checkContains(content, "session started");
            checkContains(content, "[environment/server][server] environment {createnuclear=");
            checkContains(content, "[environment/server][server] mods {mods=");
            checkContains(content, "[environment/metrics][server] metrics {");
            checkContains(content, "mspt=");
            checkContains(content, "[environment/health][server] health summary");
            checkContains(content, "session stopped {reason=command");
            if (content.contains(server.getServerDirectory().toAbsolutePath().normalize().toString()))
                throw new GameTestAssertException("the absolute game directory leaked into telemetry.log");
            if (TelemetrySession.current() != null) throw new GameTestAssertException("session still installed");
            helper.succeed();
        });
    }

    /**
     * A level changed before the session is listed in {@code config}; a level changed during the
     * session gives {@code config changed}. The level is restored, and saved again, in every case.
     */
    @GameTest(template = STRUCTURE, batch = "telemetry_session_config")
    public static void configIsRecorded(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        String env = EnvironmentTelemetryModule.ID;
        String players = EnvironmentTelemetryModule.Category.PLAYERS.id();
        TelemetryModuleConfig config = TelemetryConfigs.module(env);
        if (config == null) throw new GameTestAssertException("no config for module " + env);
        TelemetryLevel original = config.level(players);

        Path dir = null;
        String content;
        try {
            config.setLevel(players, TelemetryLevel.DEBUG);
            TelemetrySession.StartResult result = TelemetrySession.start(server, 10, env, false, null);
            if (!result.started()) throw new GameTestAssertException("session did not start: " + result.error());
            TelemetrySession session = result.session();
            dir = session.info().directory();
            if (!session.sinkIds().contains(TextFileSink.ID))
                throw new GameTestAssertException("sink_text is disabled in the test config");

            config.setLevel(players, TelemetryLevel.WARN);
            session.checkConfig();
            session.stop(TelemetrySession.StopReason.COMMAND);
            content = Files.readString(dir.resolve(TextFileSink.FILE_NAME), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GameTestAssertException("cannot read telemetry.log: " + e);
        } finally {
            TelemetrySession.stopCurrent(TelemetrySession.StopReason.COMMAND);
            config.setLevel(players, original);
            if (dir != null) delete(dir);
        }

        String key = env + ".level_" + players; // <config>.<key>
        checkContains(content, "[telemetry/session][server] config {");
        checkContains(content, key + "=debug");
        checkContains(content, "config changed {setting=" + key + ", from=debug, to=warn}");
        helper.succeed();
    }
}
