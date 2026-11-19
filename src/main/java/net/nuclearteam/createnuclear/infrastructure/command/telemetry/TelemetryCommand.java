package net.nuclearteam.createnuclear.infrastructure.command.telemetry;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /cn telemetry}: starts, stops and inspects telemetry sessions, and changes category levels.
 * <p>
 * Requires permission 2. Each sub-command lives in its own class of this package. Messages are in
 * English, like the session's own messages, and are sent to the command source only: they are
 * never broadcast, so no player name reaches the log.
 */
public final class TelemetryCommand {
    private TelemetryCommand() {}

    /**
     * Builds the {@code telemetry} node, added under {@code /cn} by {@code CNCommands}.
     *
     * @return the command node
     */
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("telemetry")
                .requires(cs -> cs.hasPermission(2))
                .then(TelemetryModulesCommand.register())
                .then(TelemetryStartCommand.register())
                .then(TelemetryStopCommand.register())
                .then(TelemetryStatusCommand.register())
                .then(TelemetryStatsCommand.register())
                .then(TelemetryLevelCommand.register());
    }
}
