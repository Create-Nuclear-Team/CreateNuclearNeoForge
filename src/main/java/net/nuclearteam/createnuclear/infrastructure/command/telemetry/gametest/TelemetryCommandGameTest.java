package net.nuclearteam.createnuclear.infrastructure.command.telemetry.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.multiblock.telemetry.ReactorTelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetrySession;
import net.nuclearteam.createnuclear.foundation.telemetry.module.EnvironmentTelemetryModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** GameTest coverage for {@code /cn telemetry}, through the real command dispatcher. */
@GameTestHolder(CreateNuclear.MOD_ID)
@PrefixGameTestTemplate(false)
public class TelemetryCommandGameTest {

    private static final String STRUCTURE = "empty_platform";

    /** A command source that keeps every message it receives. */
    private static final class Capture implements CommandSource {
        final List<String> messages = new ArrayList<>();

        @Override public void sendSystemMessage(Component message) { messages.add(message.getString()); }
        @Override public boolean acceptsSuccess() { return true; }
        @Override public boolean acceptsFailure() { return true; }
        @Override public boolean shouldInformAdmins() { return false; }
    }

    /** Runs a command and returns what it printed. */
    private static String run(MinecraftServer server, int permission, String command) {
        Capture capture = new Capture();
        CommandSourceStack source = server.createCommandSourceStack().withSource(capture).withPermission(permission);
        server.getCommands().performPrefixedCommand(source, command);
        return String.join("\n", capture.messages);
    }

    /**
     * Returns the completions of {@code command} at its end, as the server computes them when a
     * client asks for suggestions.
     */
    private static List<String> suggest(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSource(new Capture()).withPermission(2);
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        return dispatcher.getCompletionSuggestions(dispatcher.parse(command, source)).join()
                .getList().stream().map(Suggestion::getText).toList();
    }

    /** Fails if the completions of {@code command} do not contain {@code expected}. */
    private static void checkSuggests(MinecraftServer server, String command, String expected) {
        List<String> suggestions = suggest(server, command);
        if (!suggestions.contains(expected))
            throw new GameTestAssertException("'" + command + "' should suggest <" + expected + ">, got " + suggestions);
    }

    /** Fails if {@code output}, printed by {@code command}, does not contain {@code expected}. */
    private static void checkContains(String output, String expected, String command) {
        if (!output.contains(expected))
            throw new GameTestAssertException("'" + command + "' should print <" + expected + ">, got:\n" + output);
    }

    /** Deletes the session directory; failures are ignored. */
    private static void delete(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
        }
    }

    /**
     * status, start, status, stats, a bad level, stop, status again; a permission 0 source cannot
     * start a session.
     */
    @GameTest(template = STRUCTURE, batch = "telemetry_session_command")
    public static void commandDrivesASession(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        String env = EnvironmentTelemetryModule.ID;
        Path dir = null;
        try {
            checkContains(run(server, 2, "cn telemetry status"), "No telemetry session running.", "status");

            run(server, 0, "cn telemetry start 10 " + env);
            if (TelemetrySession.current() != null)
                throw new GameTestAssertException("a permission 0 source started a session");

            String started = run(server, 2, "cn telemetry start 10 " + env);
            TelemetrySession session = TelemetrySession.current();
            if (session == null) throw new GameTestAssertException("start did not start a session:\n" + started);
            dir = session.info().directory();
            checkContains(started, "Telemetry session " + session.info().id() + " started", "start");
            checkContains(started, "Files: debug/createnuclear/telemetry/", "start");

            checkContains(run(server, 2, "cn telemetry status"), "Telemetry session " + session.info().id() + ":", "status");
            checkContains(run(server, 2, "cn telemetry stats " + env), "  " + env + ": ", "stats");
            checkContains(run(server, 2, "cn telemetry level " + env + " no_such_category debug"),
                    "Unknown category 'no_such_category'", "level");

            String stopped = run(server, 2, "cn telemetry stop");
            checkContains(stopped, "Telemetry session " + session.info().id() + " stopped", "stop");
            if (TelemetrySession.current() != null) throw new GameTestAssertException("stop left the session running");
            checkContains(run(server, 2, "cn telemetry status"), "No telemetry session running.", "status after stop");
        } finally {
            TelemetrySession.stopCurrent(TelemetrySession.StopReason.COMMAND);
            if (dir != null) delete(dir);
        }
        helper.succeed();
    }

    /**
     * Without a session, every argument suggests something: module lists of start (including
     * repeated ids and empty parts, which must not throw), stats, level and its categories and levels.
     */
    @GameTest(template = STRUCTURE)
    public static void commandSuggestsArguments(GameTestHelper helper) {
        if (TelemetrySession.current() != null) throw new GameTestAssertException("a telemetry session is running");
        MinecraftServer server = helper.getLevel().getServer();
        String env = EnvironmentTelemetryModule.ID;
        String reactor = ReactorTelemetryModule.ID;

        checkSuggests(server, "cn telemetry start 10 ", "all");
        checkSuggests(server, "cn telemetry start 10 ", env);
        checkSuggests(server, "cn telemetry start 10 " + env + "+", env + "+" + reactor);
        checkSuggests(server, "cn telemetry start 10 " + env + "+" + env + "+", env + "+" + env + "+" + reactor);
        checkSuggests(server, "cn telemetry start 10 ++" + reactor + "+", "++" + reactor + "+" + env);
        if (suggest(server, "cn telemetry start 10 " + env + "+").contains(env + "+" + env))
            throw new GameTestAssertException("start suggests a module already listed");

        checkSuggests(server, "cn telemetry stats ", reactor);
        checkSuggests(server, "cn telemetry level ", env);
        checkSuggests(server, "cn telemetry level " + env + " ", EnvironmentTelemetryModule.Category.SERVER.id());
        checkSuggests(server, "cn telemetry level " + env + " server ", "debug");
        helper.succeed();
    }
}
