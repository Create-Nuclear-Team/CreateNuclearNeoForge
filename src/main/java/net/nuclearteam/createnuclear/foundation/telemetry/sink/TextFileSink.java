package net.nuclearteam.createnuclear.foundation.telemetry.sink;

import net.nuclearteam.createnuclear.foundation.telemetry.api.ITelemetrySink;
import net.nuclearteam.createnuclear.foundation.telemetry.api.RegisterTelemetrySink;
import net.nuclearteam.createnuclear.foundation.telemetry.api.SessionInfo;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryEvent;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Built-in sink {@value #ID}: writes every recorded event to {@value #FILE_NAME} in the session
 * directory, whatever {@code latest_log_level}.
 * <p>
 * The file starts with one header line beginning with {@code #}; every other line is one event in
 * the {@link TelemetryFormat#line} layout. UTF-8, {@code \n} line endings on every OS. An existing
 * file is never overwritten.
 * <p>
 * The buffer is flushed at once for {@code ERROR} and {@code WARN}, otherwise by {@link #flush},
 * which the session calls every 20 ticks even when no event arrives, and on close. Past the size
 * limit, a {@value #TRUNCATED} line is written and later events are dropped without failing.
 * A write error ({@link UncheckedIOException} from {@link #accept}, {@link IOException} from
 * {@link #flush}) removes the sink from the session.
 * <p>
 * Thread-safe.
 */
@RegisterTelemetrySink
public final class TextFileSink implements ITelemetrySink {
    /** Sink id, used for the {@code sink_text} config key. */
    public static final String ID = "text";
    /** Name of the file created in the session directory. */
    public static final String FILE_NAME = "telemetry.log";
    /** Version of the line layout, written in the header. */
    public static final int FORMAT_VERSION = 1;
    /** Default size limit: 256 Mi characters, about 256 MiB for ASCII text. */
    public static final long DEFAULT_MAX_CHARS = 256L * 1024 * 1024;
    /** Last line written when the size limit is reached. */
    public static final String TRUNCATED = "# truncated: size limit reached";

    /** Size limit of this instance, in characters. */
    private final long maxChars;

    // All guarded by this.
    /** Open file, or {@code null} before {@link #open} and after {@link #close}. */
    @Nullable
    private Writer writer;
    /** Characters written so far, line endings included. */
    private long written;
    /** Set once the size limit is reached; later events are dropped. */
    private boolean truncated;
    /** Set when lines were written since the last flush. */
    private boolean dirty;

    /** Called by the registry, once per session. */
    public TextFileSink() {
        this(DEFAULT_MAX_CHARS);
    }

    /**
     * Creates a sink with another size limit. For tests.
     *
     * @param maxChars size limit, in characters; must be positive
     * @throws IllegalArgumentException if {@code maxChars} is not positive
     */
    @ApiStatus.Internal
    public TextFileSink(long maxChars) {
        if (maxChars <= 0) throw new IllegalArgumentException("maxChars must be positive: " + maxChars);
        this.maxChars = maxChars;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Writes every recorded event to " + FILE_NAME + " in the session folder.";
    }

    /**
     * Creates {@value #FILE_NAME} and writes the header.
     *
     * @param info the session information; its directory is created if missing
     * @throws IOException if the file already exists or cannot be written; the file is then closed,
     *                     since the session will not call {@link #close}
     * @throws IllegalStateException if the sink is already open
     */
    @Override
    public synchronized void open(SessionInfo info) throws IOException {
        if (writer != null) throw new IllegalStateException("Sink already open");
        Files.createDirectories(info.directory());
        Path file = info.directory().resolve(FILE_NAME);
        writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try {
            write("# CreateNuclear telemetry, format " + FORMAT_VERSION + ", session " + info.id()
                    + ", started " + info.startedAt() + ", duration " + info.durationTicks() + " ticks"
                    + ", modules " + info.modules() + ", launcher " + info.launcher());
            writer.flush();
            dirty = false;
        } catch (IOException | RuntimeException e) {
            Writer w = writer;
            writer = null;
            try {
                w.close();
            } catch (IOException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    /**
     * Appends the event. Ignored before {@link #open}, after {@link #close} or once truncated.
     *
     * @param event the cleaned event
     * @throws UncheckedIOException if the file cannot be written
     */
    @Override
    public synchronized void accept(TelemetryEvent event) {
        if (writer == null || truncated) return;
        try {
            String line = TelemetryFormat.line(event);
            if (written + line.length() + 1 > maxChars) {
                truncated = true;
                write(TRUNCATED);
                writer.flush();
                dirty = false;
                return;
            }
            write(line);
            if (event.level() == TelemetryLevel.ERROR || event.level() == TelemetryLevel.WARN) {
                writer.flush();
                dirty = false;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + FILE_NAME, e);
        }
    }

    /**
     * Writes the buffered lines to the file, if any. Called by the session every 20 ticks.
     *
     * @throws IOException if the file cannot be written; the session then removes the sink
     */
    @Override
    public synchronized void flush() throws IOException {
        if (writer == null || !dirty) return;
        writer.flush();
        dirty = false;
    }

    /**
     * Flushes and closes the file. Safe to call more than once.
     *
     * @param info the session information, unused
     * @throws UncheckedIOException if the file cannot be closed
     */
    @Override
    public synchronized void close(SessionInfo info) {
        Writer w = writer;
        writer = null;
        if (w == null) return;
        try {
            w.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot close " + FILE_NAME, e);
        }
    }

    /** Writes one line and counts it towards the size limit. Caller holds the lock and an open writer. */
    private void write(String line) throws IOException {
        writer.write(line);
        writer.write('\n');
        written += line.length() + 1;
        dirty = true;
    }
}
