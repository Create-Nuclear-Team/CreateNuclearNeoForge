package net.nuclearteam.createnuclear.foundation.telemetry.api;

import java.io.IOException;

/**
 * Destination of telemetry events, such as {@code latest.log} or a session file.
 * <p>
 * Annotate the implementation with {@link RegisterTelemetrySink} and give it a public no-arg
 * constructor. It then gets a {@code sink_<id>} line in {@code createnuclear-telemetry.toml}.
 * A new instance is created for each session, so files can be kept open in fields.
 * <p>
 * Rules:
 * <ul>
 *   <li>write files only inside {@link SessionInfo#directory()}, never elsewhere;</li>
 *   <li>never emit telemetry from a sink, to avoid feedback loops;</li>
 *   <li>received events are already cleaned by the PrivacyFilter;</li>
 *   <li>any exception removes the sink from the session and {@link #close} is called; the game
 *       continues. If {@link #open} fails, {@link #close} is not called: release what was opened before throwing.</li>
 * </ul>
 * Calls to {@link #accept}, {@link #flush} and {@link #close} are never concurrent: the session
 * serializes them, though {@link #accept} may come from any thread.
 * Part of the public contract used by addons: new methods must have a default implementation.
 */
public interface ITelemetrySink {
    /**
     * Returns the sink id.
     *
     * @return a snake_case id, used for the {@code sink_<id>} config key; never changes once published
     */
    String id();

    /**
     * Returns a short description, used as the config comment.
     *
     * @return a one-line, human-readable description
     */
    String description();

    /**
     * Tells whether the sink is enabled when the config is first generated.
     *
     * @return the initial value of {@code sink_<id>}; {@code true} by default
     */
    default boolean enabledByDefault() {
        return true;
    }

    /**
     * Called once when a session starts, before any event.
     *
     * @param info the session information, including the directory to write into
     * @throws IOException if the sink cannot open its output; the sink is then not used for this session
     */
    void open(SessionInfo info) throws IOException;

    /**
     * Receives one recorded event. Called on the thread that emitted it, usually the server thread,
     * so it must stay cheap.
     *
     * @param event the cleaned event
     */
    void accept(TelemetryEvent event);

    /**
     * Writes buffered events to their destination. Called by the session every 20 server ticks
     * (one second at 20 TPS), on the server thread, after the events of that tick, so that a crash
     * loses at most that much; never after {@link #close}. Does nothing by default, for sinks that
     * write without buffering.
     *
     * @throws IOException if the buffered events cannot be written; the sink is then removed from the session
     */
    default void flush() throws IOException {
    }

    /**
     * Called once when the session stops, after the last event. Must release every resource.
     *
     * @param info the same session information as passed to {@link #open}
     */
    void close(SessionInfo info);
}
