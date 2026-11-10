package net.nuclearteam.createnuclear.infrastructure.command.telemetry;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetrySession;

/** {@code /cn telemetry stop}: stops the running session and tells where its files are. */
final class TelemetryStopCommand {
    private TelemetryStopCommand() {}

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("stop").executes(TelemetryStopCommand::run);
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        TelemetrySession session = TelemetrySession.current();
        if (session == null) {
            TelemetryCommandText.fail(ctx, TelemetryCommandText.NO_SESSION);
            return 0;
        }

        String directory = session.displayDirectory(); // read before stop: current() is null afterwards
        session.stop(TelemetrySession.StopReason.COMMAND);
        TelemetryCommandText.reply(ctx, "Telemetry session " + session.info().id() + " stopped after "
                + TelemetryCommandText.seconds(session.tick()) + " s, " + session.eventCount()
                + " events. Files: " + directory);
        return Command.SINGLE_SUCCESS;
    }
}
