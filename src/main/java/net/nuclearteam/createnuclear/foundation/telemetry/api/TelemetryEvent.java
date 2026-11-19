package net.nuclearteam.createnuclear.foundation.telemetry.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A dated, structured telemetry fact. Immutable.
 * <p>
 * Part of the public contract used by addons: any change must be noted in part 2 of the README.
 *
 * @param tick       server ticks since the session started
 * @param timeMillis wall-clock time, in epoch milliseconds
 * @param level      level of the event, never {@link TelemetryLevel#OFF}
 * @param module     id of the module that emitted the event
 * @param category   category id, possibly a {@link BuiltinCategory}
 * @param subject    what the event is about
 * @param message    formatted message; {@code null} is stored as an empty string
 * @param attributes key/value pairs in insertion order; {@code null} is stored as an empty map.
 *                   Stored as an unmodifiable copy
 */
public record TelemetryEvent(
        long tick,
        long timeMillis,
        TelemetryLevel level,
        String module,
        String category,
        Subject subject,
        String message,
        Map<String, Object> attributes
) {
    public TelemetryEvent {
        attributes = attributes == null || attributes.isEmpty()
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        if (message == null) message = "";
    }

    /**
     * Returns a copy of this event with another message and other attributes.
     * Used by the PrivacyFilter to clean an event.
     *
     * @param newMessage    the replacement message
     * @param newAttributes the replacement attributes
     * @return a new event; tick, time, level, module, category and subject are unchanged
     */
    public TelemetryEvent withContent(String newMessage, Map<String, Object> newAttributes) {
        return new TelemetryEvent(tick, timeMillis, level, module, category, subject, newMessage, newAttributes);
    }
}
