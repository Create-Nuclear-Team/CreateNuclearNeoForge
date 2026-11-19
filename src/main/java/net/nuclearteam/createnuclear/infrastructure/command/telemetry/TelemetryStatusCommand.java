package net.nuclearteam.createnuclear.infrastructure.command.telemetry;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetrySession;
import net.nuclearteam.createnuclear.foundation.telemetry.api.SessionInfo;

/**
 * {@code /cn telemetry status}: the running session, its elapsed and remaining time, and its event
 * count. Without a session, replies so and returns 0, which is not an error.
 */
final class TelemetryStatusCommand {
    private TelemetryStatusCommand() {}

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("status").executes(TelemetryStatusCommand::run);
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        TelemetrySession session = TelemetrySession.current();
        if (session == null) {
            TelemetryCommandText.reply(ctx, TelemetryCommandText.NO_SESSION);
            return 0;
        }
        SessionInfo info = session.info();
        TelemetryCommandText.reply(ctx, "Telemetry session " + info.id() + ": "
                + TelemetryCommandText.seconds(session.tick()) + " s / "
                + TelemetryCommandText.seconds(info.durationTicks()) + " s ("
                + TelemetryCommandText.seconds(session.remainingTicks()) + " s left), modules "
                + session.recordedModules() + ", sinks " + session.sinkIds() + ", " + session.eventCount()
                + " events. Files: " + session.displayDirectory());
        return Command.SINGLE_SUCCESS;
    }
}
