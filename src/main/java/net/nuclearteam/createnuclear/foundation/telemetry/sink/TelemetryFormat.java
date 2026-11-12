package net.nuclearteam.createnuclear.foundation.telemetry.sink;

import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryEvent;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Text layout of an event, shared by the built-in sinks and available to addon sinks:
 * <pre>[+tick][LEVEL][module/category][subject] message {key=value, ...}</pre>
 * <ul>
 *   <li>one event is always one line: in the message and in every value, {@code \} becomes
 *       {@code \\}, line breaks {@code \n}, tabs {@code \t}, the same way quoted or not;</li>
 *   <li>attributes keep their order; the block is omitted when there are none;</li>
 *   <li>a value containing a space, {@code ,}, {@code =}, <code>{</code>, <code>}</code> or {@code "},
 *       or an empty value, is quoted, with {@code "} escaped as {@code \"};</li>
 *   <li>decimals are rounded to 4 places without trailing zeros; integral values keep {@code .0};
 *       values below {@code 1e-4} keep scientific notation; {@code NaN}, {@code inf} and {@code -inf}
 *       are written as such.</li>
 * </ul>
 * Expects events already cleaned by the PrivacyFilter. Stateless and thread-safe.
 */
public final class TelemetryFormat {
    /** Below this absolute value, decimals keep scientific notation instead of rounding to 0. */
    private static final double TINY = 1e-4;

    private TelemetryFormat() {}

    /**
     * Formats a whole event.
     *
     * @param event the (already cleaned) event
     * @return one line, without line terminator
     */
    public static String line(TelemetryEvent event) {
        StringBuilder b = new StringBuilder(128)
                .append("[+").append(event.tick()).append("][")
                .append(event.level().name()).append("][")
                .append(event.module()).append('/').append(event.category()).append("][")
                .append(event.subject()).append("] ")
                .append(oneLine(event.message()));
        if (!event.attributes().isEmpty()) b.append(' ').append(attributes(event.attributes()));
        return b.toString();
    }

    /**
     * Formats an attribute block.
     *
     * @param attributes the attributes, in order
     * @return {@code {key=value, ...}}, or {@code {}} if empty
     */
    public static String attributes(Map<String, ?> attributes) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> e : attributes.entrySet()) {
            if (!first) b.append(", ");
            first = false;
            b.append(e.getKey()).append('=').append(value(e.getValue()));
        }
        return b.append('}').toString();
    }

    /**
     * Formats one attribute value.
     *
     * @param value the value, possibly {@code null}
     * @return its text form, quoted when needed
     */
    public static String value(@Nullable Object value) {
        if (value == null) return "null";
        if (value instanceof Double d) return decimal(d);
        if (value instanceof Float f) return decimal(f);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return quoteIfNeeded(oneLine(value.toString()));
    }

    /**
     * Formats a decimal number.
     *
     * @param d the number
     * @return e.g. {@code 0.99}, {@code 12.0}, {@code 1.0E-9}, {@code NaN}, {@code inf}
     */
    public static String decimal(double d) {
        if (Double.isNaN(d)) return "NaN";
        if (Double.isInfinite(d)) return d > 0 ? "inf" : "-inf";
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return (long) d + ".0";
        if (Math.abs(d) < TINY) return Double.toString(d);
        String s = BigDecimal.valueOf(d).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return s.contains(".") ? s : s + ".0";
    }

    /**
     * Escapes in one pass so that an event stays on one line and stays unambiguous: {@code \} becomes
     * {@code \\}, a line break ({@code \r\n}, {@code \n} or {@code \r}) {@code \n}, a tab {@code \t}.
     * Escaping {@code \} first is what lets a reader tell an escaped line break from a literal
     * {@code \n}, quoted or not.
     */
    private static String oneLine(String s) {
        if (s.indexOf('\\') < 0 && s.indexOf('\n') < 0 && s.indexOf('\r') < 0 && s.indexOf('\t') < 0) return s;
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> b.append("\\\\");
                case '\t' -> b.append("\\t");
                case '\n' -> b.append("\\n");
                case '\r' -> {
                    b.append("\\n");
                    if (i + 1 < s.length() && s.charAt(i + 1) == '\n') i++;
                }
                default -> b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Quotes a value that is empty or contains a character used by the layout, escaping {@code "}.
     * Expects a value already passed through {@link #oneLine}, so {@code \} is already escaped.
     */
    private static String quoteIfNeeded(String s) {
        if (!s.isEmpty() && s.chars().noneMatch(c -> c == ' ' || c == ',' || c == '=' || c == '{' || c == '}' || c == '"'))
            return s;
        return '"' + s.replace("\"", "\\\"") + '"';
    }
}
