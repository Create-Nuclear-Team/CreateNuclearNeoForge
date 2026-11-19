package net.nuclearteam.createnuclear.infrastructure.command.telemetry;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetryRegistry;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryConfigs;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryCoreConfig;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryModuleConfig;

import java.util.Objects;
import java.util.stream.Collectors;

/** {@code /cn telemetry modules}: every module with its state and category levels, then every sink. */
final class TelemetryModulesCommand {
    private TelemetryModulesCommand() {}

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("modules").executes(TelemetryModulesCommand::run);
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        StringBuilder out = new StringBuilder("Telemetry modules:");
        for (TelemetryRegistry.ModuleEntry entry : TelemetryRegistry.modules()) {
            TelemetryModuleConfig config = TelemetryConfigs.module(entry.id());
            out.append("\n  ").append(entry.id());
            if (config == null) {
                out.append(" (no config)");
                continue;
            }
            out.append(config.enabled.get() ? " (enabled)" : " (disabled)");
            String description = describe(entry);
            if (!description.isEmpty()) out.append(" - ").append(description);
            out.append("\n    ").append(entry.categories().stream()
                    .map(TelemetryCategory::id)
                    .map(id -> id + "=" + config.level(id).id())
                    .collect(Collectors.joining(", ")));
        }
        TelemetryCoreConfig core = TelemetryConfigs.core();
        out.append("\nSinks: ").append(TelemetryRegistry.sinks().stream()
                .map(s -> s.id() + (core != null && core.isSinkEnabled(s.id()) ? " (on)" : " (off)"))
                .collect(Collectors.joining(", ")));
        TelemetryCommandText.reply(ctx, out.toString());
        return Command.SINGLE_SUCCESS;
    }

    /** Module description; a failing or missing one gives an empty string. */
    private static String describe(TelemetryRegistry.ModuleEntry entry) {
        try {
            return Objects.requireNonNullElse(entry.module().description(), "");
        } catch (Throwable t) {
            return "";
        }
    }
}
