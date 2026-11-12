package net.nuclearteam.createnuclear.foundation.telemetry.api;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Read-only information about a telemetry session, given to sinks and modules.
 * <p>
 * Contains no player identifier and no absolute path meant for display (README, decision D11).
 * Part of the public contract used by addons.
 *
 * @param id               folder name, e.g. {@code 2026-11-10_14-03-22}
 * @param directory        absolute directory where sinks write; never shown to players
 * @param displayDirectory the same directory relative to the game directory; safe to show
 * @param startedAt        wall-clock start time
 * @param durationTicks    planned length of the session, in server ticks
 * @param modules          ids of the recorded modules; stored as an unmodifiable copy
 * @param launcher         {@code "player"} or {@code "console"}, never a name
 */
public record SessionInfo(
        String id,
        Path directory,
        Path displayDirectory,
        Instant startedAt,
        long durationTicks,
        List<String> modules,
        String launcher
) {
    public SessionInfo {
        modules = List.copyOf(modules);
    }
}
