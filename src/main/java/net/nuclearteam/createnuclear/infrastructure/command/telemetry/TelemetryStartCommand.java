package net.nuclearteam.createnuclear.infrastructure.command.telemetry;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetrySession;
import net.nuclearteam.createnuclear.foundation.telemetry.api.SessionInfo;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * {@code /cn telemetry start [seconds] [modules]}: starts a session. The session applies every
 * rule (maximum duration, disabled modules, sinks); this command only shows the result.
 */
final class TelemetryStartCommand {
    private static final String SECONDS = "seconds";
    private static final String MODULES = "modules";

    private TelemetryStartCommand() {}

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("start")
                .executes(ctx -> run(ctx, null, null))
                .then(Commands.argument(SECONDS, IntegerArgumentType.integer(1))
                        .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, SECONDS), null))
                        .then(Commands.argument(MODULES, StringArgumentType.word())
                                .suggests(TelemetrySuggestions::moduleSpec)
                                .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, SECONDS), StringArgumentType.getString(ctx, MODULES)))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, @Nullable Integer seconds, @Nullable String modules) {
        CommandSourceStack source = ctx.getSource();
        TelemetrySession.StartResult result = TelemetrySession.start(source.getServer(), seconds, modules, source.isPlayer(), source::sendSystemMessage);
        for (String warning : result.warnings())
            source.sendSystemMessage(Component.literal(warning).withStyle(ChatFormatting.YELLOW));
        if (!result.started()) {
            TelemetryCommandText.fail(ctx, Objects.requireNonNullElse(result.error(), "The telemetry session did not start."));
            return 0;
        }

        TelemetrySession session = Objects.requireNonNull(result.session());
        SessionInfo info = session.info();
        TelemetryCommandText.reply(ctx, "Telemetry session " + info.id() + " started: "
                + TelemetryCommandText.seconds(info.durationTicks()) + " s, modules " + info.modules()
                + ", sinks " + session.sinkIds() + ". Files: " + session.displayDirectory());
        return Command.SINGLE_SUCCESS;
    }
}
