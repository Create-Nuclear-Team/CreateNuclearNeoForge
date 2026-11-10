package net.nuclearteam.createnuclear.foundation.telemetry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryEvent;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Fluent builder of one telemetry event, obtained from {@link Telemetry}. Ends with {@link #emit()}.
 * <p>
 * When the event will not be recorded, the facade returns {@link #NOOP}: every method then
 * returns immediately, without computing or allocating anything. A builder is not thread-safe
 * and is meant to be used in a single statement.
 * <p>
 * Arguments are still evaluated by the caller. Two caller-side allocations remain possible on
 * {@link #NOOP}: the array of {@link #msg(String, Object...)} (more than three placeholders), and
 * the boxing of primitive {@code msg} arguments ({@code int} outside -128..127, {@code long},
 * {@code double}...). Prefer the primitive {@link #attr} overloads, {@link #attrLazy}, or a
 * {@link Telemetry#isEnabled} guard in hot code.
 */
public final class EventBuilder {
    /** Shared builder that ignores everything. */
    public static final EventBuilder NOOP = new EventBuilder(null, "", "", TelemetryLevel.OFF);

    @Nullable
    private final TelemetryRecorder recorder;
    private final String moduleId;
    private final String categoryId;
    private final TelemetryLevel level;

    @Nullable
    private Subject subject;
    @Nullable
    private String pattern;
    @Nullable
    private Object[] args;
    @Nullable
    private Map<String, Object> attributes;
    private boolean dropped;

    /**
     * Creates a builder. Only the {@link Telemetry} facade creates builders, after checking the level.
     *
     * @param recorder   the session that will receive the event; {@code null} only for {@link #NOOP}
     * @param moduleId   module id
     * @param categoryId category id
     * @param level      level of the event
     */
    EventBuilder(@Nullable TelemetryRecorder recorder, String moduleId, String categoryId, TelemetryLevel level) {
        this.recorder = recorder;
        this.moduleId = moduleId;
        this.categoryId = categoryId;
        this.level = level;
    }

    // ---- Subject --------------------------------------------------------------------------------

    /**
     * Sets the subject. Without one, the event is about {@link Subject#SERVER}.
     *
     * @param subject what the event is about
     * @return this builder
     */
    public EventBuilder subject(Subject subject) {
        if (this != NOOP) this.subject = subject;
        return this;
    }

    /**
     * Sets a block subject, built only if the event is recorded.
     * On a client-side level the event is dropped, so integrated servers do not record it twice.
     *
     * @param type  snake_case subject type, e.g. {@code reactor}
     * @param level the level holding the block
     * @param pos   the block position
     * @return this builder
     */
    public EventBuilder atBlock(String type, @Nullable Level level, BlockPos pos) {
        if (this == NOOP) return this;
        if (level == null || level.isClientSide()) {
            dropped = true;
            return this;
        }
        try {
            this.subject = Subject.atBlock(type, level, pos);
        } catch (Throwable t) {
            Telemetry.reportFailure("atBlock(" + type + ")", t);
            dropped = true;
        }
        return this;
    }

    // ---- Message --------------------------------------------------------------------------------

    /**
     * Sets a constant message.
     *
     * @param message the message
     * @return this builder
     */
    public EventBuilder msg(String message) {
        if (this != NOOP) {
            this.pattern = message;
            this.args = null;
        }
        return this;
    }

    /**
     * Sets a message with one {@code {}} placeholder, formatted only if the event is recorded.
     *
     * @param pattern SLF4J-style pattern
     * @param arg     placeholder value
     * @return this builder
     */
    public EventBuilder msg(String pattern, @Nullable Object arg) {
        if (this != NOOP) {
            this.pattern = pattern;
            this.args = new Object[]{arg};
        }
        return this;
    }

    /**
     * Sets a message with two {@code {}} placeholders, formatted only if the event is recorded.
     *
     * @param pattern SLF4J-style pattern
     * @param arg1    first placeholder value
     * @param arg2    second placeholder value
     * @return this builder
     */
    public EventBuilder msg(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
        if (this != NOOP) {
            this.pattern = pattern;
            this.args = new Object[]{arg1, arg2};
        }
        return this;
    }

    /**
     * Sets a message with three {@code {}} placeholders, formatted only if the event is recorded.
     *
     * @param pattern SLF4J-style pattern
     * @param arg1    first placeholder value
     * @param arg2    second placeholder value
     * @param arg3    third placeholder value
     * @return this builder
     */
    public EventBuilder msg(String pattern, @Nullable Object arg1, @Nullable Object arg2, @Nullable Object arg3) {
        if (this != NOOP) {
            this.pattern = pattern;
            this.args = new Object[]{arg1, arg2, arg3};
        }
        return this;
    }

    /**
     * Sets a message with any number of {@code {}} placeholders, formatted only if the event is recorded.
     * <p>
     * <b>Allocates even when the event is not recorded:</b> the varargs array is created by the
     * caller, before this method runs. In hot code with more than three arguments, guard the call
     * with {@link Telemetry#isEnabled} or move values to primitive {@link #attr} overloads.
     *
     * @param pattern SLF4J-style pattern
     * @param args    placeholder values
     * @return this builder
     */
    public EventBuilder msg(String pattern, Object... args) {
        if (this != NOOP) {
            this.pattern = pattern;
            this.args = args;
        }
        return this;
    }

    // ---- Attributes -----------------------------------------------------------------------------

    /**
     * Adds an attribute. A key set twice keeps its first position and its last value.
     *
     * @param key   snake_case attribute key
     * @param value attribute value; prefer numbers, booleans, strings and enums
     * @return this builder
     */
    public EventBuilder attr(String key, @Nullable Object value) {
        if (this != NOOP) attributes().put(key, value);
        return this;
    }

    /**
     * Adds an {@code int} attribute, boxed only if the event is recorded.
     *
     * @param key   snake_case attribute key
     * @param value attribute value
     * @return this builder
     */
    public EventBuilder attr(String key, int value) {
        if (this != NOOP) attributes().put(key, value);
        return this;
    }

    /**
     * Adds a {@code long} attribute, boxed only if the event is recorded.
     *
     * @param key   snake_case attribute key
     * @param value attribute value
     * @return this builder
     */
    public EventBuilder attr(String key, long value) {
        if (this != NOOP) attributes().put(key, value);
        return this;
    }

    /**
     * Adds a {@code double} attribute, boxed only if the event is recorded.
     *
     * @param key   snake_case attribute key
     * @param value attribute value
     * @return this builder
     */
    public EventBuilder attr(String key, double value) {
        if (this != NOOP) attributes().put(key, value);
        return this;
    }

    /**
     * Adds a {@code boolean} attribute, boxed only if the event is recorded.
     *
     * @param key   snake_case attribute key
     * @param value attribute value
     * @return this builder
     */
    public EventBuilder attr(String key, boolean value) {
        if (this != NOOP) attributes().put(key, value);
        return this;
    }

    /**
     * Adds an attribute computed only when the event is emitted and recorded.
     * If the supplier throws, the value becomes {@code <error: ExceptionClass>}.
     *
     * @param key   snake_case attribute key
     * @param value computes the attribute value
     * @return this builder
     */
    public EventBuilder attrLazy(String key, Supplier<?> value) {
        if (this != NOOP) attributes().put(key, new Lazy(value));
        return this;
    }

    /**
     * Attaches an exception as the {@code exception} and {@code exception_message} attributes.
     * The stack trace is not recorded.
     * <p>
     * Never throws: a {@code null} exception is recorded as {@code exception=null}, so the
     * instrumentation bug stays visible, and a failing {@link Throwable#getMessage()} gives
     * {@code <error: ExceptionClass>}.
     *
     * @param error the caught exception, possibly {@code null}
     * @return this builder
     */
    public EventBuilder exception(@Nullable Throwable error) {
        if (this == NOOP) return this;
        if (error == null) {
            attributes().put("exception", "null");
            return this;
        }
        attributes().put("exception", error.getClass().getName());
        try {
            attributes().put("exception_message", error.getMessage());
        } catch (Throwable t) {
            attributes().put("exception_message", "<error: " + t.getClass().getSimpleName() + ">");
        }
        return this;
    }

    // ---- Emit -----------------------------------------------------------------------------------

    /**
     * Formats and records the event. Never throws; failures are logged.
     * Calling it more than once records the event only once.
     */
    public void emit() {
        TelemetryRecorder r = recorder;
        if (this == NOOP || r == null || dropped) return;
        dropped = true; // single emission

        try {
            String message = pattern == null ? ""
                    : args == null || args.length == 0 ? pattern
                    : MessageFormatter.arrayFormat(pattern, args).getMessage();
            r.record(new TelemetryEvent(r.tick(), System.currentTimeMillis(), level, moduleId, categoryId,
                    subject == null ? Subject.SERVER : subject, message, resolveAttributes()));
        } catch (Throwable t) {
            Telemetry.reportFailure("emit(" + moduleId + "/" + categoryId + ")", t);
        }
    }

    private Map<String, Object> attributes() {
        if (attributes == null) attributes = new LinkedHashMap<>();
        return attributes;
    }

    @Nullable
    private Map<String, Object> resolveAttributes() {
        if (attributes == null) return null;
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            if (e.getValue() instanceof Lazy lazy) {
                try {
                    e.setValue(lazy.supplier().get());
                } catch (Throwable t) {
                    e.setValue("<error: " + t.getClass().getSimpleName() + ">");
                }
            }
        }
        return attributes;
    }

    /** Marker for a value computed at emission. */
    private record Lazy(Supplier<?> supplier) {}
}
