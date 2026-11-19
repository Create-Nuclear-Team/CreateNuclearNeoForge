package net.nuclearteam.createnuclear.infrastructure.command.telemetry;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetryRegistry;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetrySession;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryConfigs;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * {@code /cn telemetry level <module> <category> <level>}: changes a category threshold and saves
 * it in the module's TOML. A running session applies it at once, since levels are read on each call.
 */
final class TelemetryLevelCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateNuclear/Telemetry");

    private static final String MODULE = "module";
    private static final String CATEGORY = "category";
    private static final String LEVEL = "level";

    private TelemetryLevelCommand() {}

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("level")
                .then(Commands.argument(MODULE, StringArgumentType.word())
                        .suggests(TelemetrySuggestions::modules)
                        .then(Commands.argument(CATEGORY, StringArgumentType.word())
                                .suggests(TelemetrySuggestions.categories(MODULE))
                                .then(Commands.argument(LEVEL, StringArgumentType.word())
                                        .suggests(TelemetrySuggestions::levels)
                                        .executes(TelemetryLevelCommand::run))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        String moduleId = StringArgumentType.getString(ctx, MODULE);
        String categoryId = StringArgumentType.getString(ctx, CATEGORY);
        String levelName = StringArgumentType.getString(ctx, LEVEL);

        if (TelemetryRegistry.module(moduleId) == null) {
            TelemetryCommandText.fail(ctx, "Unknown telemetry module '" + moduleId + "'. See /cn telemetry modules.");
            return 0;
        }
        TelemetryModuleConfig config = TelemetryConfigs.module(moduleId);
        if (config == null) {
            TelemetryCommandText.fail(ctx, "Module '" + moduleId + "' has no config.");
            return 0;
        }
        TelemetryLevel level = TelemetryLevel.parse(levelName);
        if (level == null) {
            TelemetryCommandText.fail(ctx, "Unknown level '" + levelName + "'. Use one of: "
                    + Arrays.stream(TelemetryLevel.values()).map(TelemetryLevel::id).collect(Collectors.joining(", ")) + ".");
            return 0;
        }
        if (!config.categoryIds().contains(categoryId)) {
            TelemetryCommandText.fail(ctx, "Unknown category '" + categoryId + "' in module '" + moduleId
                    + "'. Categories: " + String.join(", ", config.categoryIds()) + ".");
            return 0;
        }

        TelemetryLevel before = config.level(categoryId);
        try {
            config.setLevel(categoryId, level); // catnip's set() also saves the file
        } catch (RuntimeException | AssertionError e) {
            LOGGER.error("Cannot change telemetry level {}/{}", moduleId, categoryId, e);
            TelemetryCommandText.fail(ctx, "Cannot save the level (" + e.getClass().getSimpleName() + "). See the log.");
            return 0;
        }
        TelemetrySession session = TelemetrySession.current();
        if (session != null) session.checkConfig(); // writes "config changed" now, not at the next flush
        boolean live = session != null && session.recordedModules().contains(moduleId);
        TelemetryCommandText.reply(ctx, moduleId + "/" + categoryId + ": " + before.id() + " -> " + level.id()
                + ", saved in " + TelemetryModuleConfig.fileName(moduleId) + "."
                + (live ? " Applies now to the running session." : ""));
        return Command.SINGLE_SUCCESS;
    }
}
