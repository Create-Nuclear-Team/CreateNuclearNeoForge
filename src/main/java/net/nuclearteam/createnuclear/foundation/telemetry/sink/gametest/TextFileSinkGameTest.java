package net.nuclearteam.createnuclear.foundation.telemetry.sink.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.api.SessionInfo;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryEvent;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.sink.TextFileSink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/** GameTest coverage for {@link TextFileSink}: header, one line per event, flushing, no overwrite, size limit, close. */
@GameTestHolder(CreateNuclear.MOD_ID)
@PrefixGameTestTemplate(false)
public class TextFileSinkGameTest {

    private static final String STRUCTURE = "empty_platform";

    private static void checkEquals(Object expected, Object actual, String what) {
        if (!Objects.equals(expected, actual))
            throw new GameTestAssertException(what + ": expected <" + expected + "> but got <" + actual + ">");
    }

    /** Session information pointing at a temporary directory. */
    private static SessionInfo info(Path dir) {
        return new SessionInfo("test", dir, Path.of("test"), Instant.EPOCH, 200, List.of("reactor"), "console");
    }

    /** A {@code reactor/state} event on the server, without attributes. */
    private static TelemetryEvent event(long tick, TelemetryLevel level, String message) {
        return new TelemetryEvent(tick, 0, level, "reactor", "state", Subject.SERVER, message, Map.of());
    }

    /** Lines currently on disk, header included; buffered lines are not visible. */
    private static List<String> lines(Path dir) throws IOException {
        return Files.readAllLines(dir.resolve(TextFileSink.FILE_NAME), StandardCharsets.UTF_8);
    }

    /** Deletes the temporary directory; failures are ignored. */
    private static void delete(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
        }
    }

    /** Header first, then one line per event in the shared layout; nothing accepted after close. */
    @GameTest(template = STRUCTURE)
    public static void writesHeaderAndEvents(GameTestHelper helper) throws IOException {
        Path dir = Files.createTempDirectory("cn-telemetry-");
        try {
            TextFileSink sink = new TextFileSink();
            sink.open(info(dir));
            sink.accept(event(1, TelemetryLevel.INFO, "first"));
            sink.accept(event(2, TelemetryLevel.TRACE, "second"));
            sink.close(info(dir));
            sink.accept(event(3, TelemetryLevel.ERROR, "after close"));
            sink.close(info(dir));

            List<String> lines = lines(dir);
            checkEquals(3, lines.size(), "line count");
            checkEquals("# CreateNuclear telemetry, format 1, session test, started 1970-01-01T00:00:00Z, duration 200 ticks, modules [reactor], launcher console",
                    lines.get(0), "header");
            checkEquals("[+1][INFO][reactor/state][server] first", lines.get(1), "first event");
            checkEquals("[+2][TRACE][reactor/state][server] second", lines.get(2), "trace is not filtered");
        } finally {
            delete(dir);
        }
        helper.succeed();
    }

    /** INFO stays buffered until flush; WARN is written at once. */
    @GameTest(template = STRUCTURE)
    public static void flushesIncidentsAtOnceAndTheRestOnFlush(GameTestHelper helper) throws IOException {
        Path dir = Files.createTempDirectory("cn-telemetry-");
        try {
            TextFileSink sink = new TextFileSink();
            sink.open(info(dir));
            sink.accept(event(1, TelemetryLevel.INFO, "buffered"));
            checkEquals(1, lines(dir).size(), "info still buffered");
            sink.flush();
            checkEquals(2, lines(dir).size(), "info written by flush");
            sink.accept(event(2, TelemetryLevel.WARN, "incident"));
            checkEquals(3, lines(dir).size(), "warn written at once");
            sink.close(info(dir));
            sink.flush(); // after close: no-op
        } finally {
            delete(dir);
        }
        helper.succeed();
    }

    /** An existing file is never overwritten. */
    @GameTest(template = STRUCTURE)
    public static void neverOverwrites(GameTestHelper helper) throws IOException {
        Path dir = Files.createTempDirectory("cn-telemetry-");
        try {
            Files.writeString(dir.resolve(TextFileSink.FILE_NAME), "keep me\n");
            try {
                new TextFileSink().open(info(dir));
                throw new GameTestAssertException("open should refuse an existing file");
            } catch (FileAlreadyExistsException expected) {
                // expected
            }
            checkEquals(List.of("keep me"), lines(dir), "content kept");
        } finally {
            delete(dir);
        }
        helper.succeed();
    }

    /** Past the limit, one truncation line is written and later events are dropped without failing. */
    @GameTest(template = STRUCTURE)
    public static void stopsAtSizeLimit(GameTestHelper helper) throws IOException {
        Path dir = Files.createTempDirectory("cn-telemetry-");
        try {
            TextFileSink sink = new TextFileSink(250);
            sink.open(info(dir));
            for (int i = 0; i < 20; i++) sink.accept(event(i, TelemetryLevel.INFO, "event " + i));
            sink.close(info(dir));

            List<String> lines = lines(dir);
            checkEquals(TextFileSink.TRUNCATED, lines.get(lines.size() - 1), "last line");
            checkEquals(1L, lines.stream().filter(TextFileSink.TRUNCATED::equals).count(), "truncation written once");
            if (lines.size() >= 21) throw new GameTestAssertException("events past the limit were written");
        } finally {
            delete(dir);
        }
        helper.succeed();
    }
}
