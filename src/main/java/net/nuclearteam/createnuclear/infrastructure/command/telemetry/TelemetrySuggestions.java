package net.nuclearteam.createnuclear.infrastructure.command.telemetry;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetryRegistry;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetrySession;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryConfigs;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryModuleConfig;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/** Tab completion of the {@code /cn telemetry} arguments. */
final class TelemetrySuggestions {
    private TelemetrySuggestions() {}

    private static Stream<String> moduleIds() {
        return TelemetryRegistry.modules().stream().map(TelemetryRegistry.ModuleEntry::id);
    }

    /** Module list of {@code start}: {@code all}, then module ids; after a {@code +}, the ids not yet listed. */
    static CompletableFuture<Suggestions> moduleSpec(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int plus = remaining.lastIndexOf('+');
        String prefix = plus < 0 ? "" : remaining.substring(0, plus + 1);
        // copyOf, not Set.of: a repeated id or several '+' give duplicates, which Set.of rejects
        Set<String> listed = Set.copyOf(Arrays.asList(prefix.split("\\+")));
        Stream<String> ids = moduleIds().filter(id -> !listed.contains(id)).map(id -> prefix + id);
        return SharedSuggestionProvider.suggest(plus < 0 ? Stream.concat(Stream.of("all"), ids) : ids, builder);
    }

    /** Every registered module. */
    static CompletableFuture<Suggestions> modules(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(moduleIds(), builder);
    }

    /**
     * The modules of the running session. Without a session, every registered module: the player
     * still sees what to type, and the command then says that no session is running.
     */
    static CompletableFuture<Suggestions> sessionModules(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        TelemetrySession session = TelemetrySession.current();
        return SharedSuggestionProvider.suggest(session == null ? moduleIds() : session.info().modules().stream(), builder);
    }

    /**
     * The categories of the module typed in the argument {@code moduleArgument}.
     *
     * @param moduleArgument name of the module argument
     * @return a suggestion provider
     */
    static SuggestionProvider<CommandSourceStack> categories(String moduleArgument) {
        return (ctx, builder) -> {
            TelemetryModuleConfig config = TelemetryConfigs.module(StringArgumentType.getString(ctx, moduleArgument));
            return config == null ? builder.buildFuture() : SharedSuggestionProvider.suggest(config.categoryIds(), builder);
        };
    }

    /** Every level, lower case. */
    static CompletableFuture<Suggestions> levels(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(TelemetryLevel.values()).map(TelemetryLevel::id), builder);
    }
}
