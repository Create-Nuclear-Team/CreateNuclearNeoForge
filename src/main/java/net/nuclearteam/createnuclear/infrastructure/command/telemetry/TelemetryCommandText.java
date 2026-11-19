package net.nuclearteam.createnuclear.infrastructure.command.telemetry;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Text shared by the {@code /cn telemetry} sub-commands. */
final class TelemetryCommandText {
    /** Reply of {@code status}, {@code stop} and {@code stats} when no session is running. */
    static final String NO_SESSION = "No telemetry session running.";

    private static final int TICKS_PER_SECOND = 20;

    private TelemetryCommandText() {}

    /** Sends a reply to the source only, never broadcast (D90). */
    static void reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    /** Sends an error to the source, in red. */
    static void fail(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendFailure(Component.literal(text));
    }

    /** Whole seconds in a tick count, at 20 ticks per second. */
    static long seconds(long ticks) {
        return ticks / TICKS_PER_SECOND;
    }

    /** A share in [0, 1] as a percentage, e.g. {@code 99.5%}. */
    static String percent(double share) {
        return String.format(Locale.ROOT, "%.1f%%", share * 100);
    }

    /** A number with one decimal; infinity becomes {@code inf}. */
    static String number(double value) {
        return Double.isInfinite(value) ? "inf" : String.format(Locale.ROOT, "%.1f", value);
    }
}
