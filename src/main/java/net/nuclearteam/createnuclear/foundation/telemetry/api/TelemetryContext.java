package net.nuclearteam.createnuclear.foundation.telemetry.api;

import net.minecraft.server.MinecraftServer;

/**
 * Passed to the lifecycle hooks of an {@link ITelemetryModule}.
 * <p>
 * Events and metrics are not emitted through the context but through the Telemetry facade,
 * like everywhere else in the code.
 */
public interface TelemetryContext {
    /**
     * Returns the running server.
     *
     * @return the server the session runs on
     */
    MinecraftServer server();

    /**
     * Returns information about the current session.
     *
     * @return the current session's information
     */
    SessionInfo session();

    /**
     * Returns the session-relative time.
     *
     * @return server ticks since the session started (20 ticks = 1 second)
     */
    long tick();

    /**
     * Returns the module this context was built for.
     *
     * @return the id of the module receiving this context
     */
    String moduleId();
}
