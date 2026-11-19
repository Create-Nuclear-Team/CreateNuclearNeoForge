package net.nuclearteam.createnuclear.infrastructure.command.telemetry;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.nuclearteam.createnuclear.foundation.telemetry.TelemetrySession;
import net.nuclearteam.createnuclear.foundation.telemetry.health.HealthTracker;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * {@code /cn telemetry stats [module]}: health totals per module, then the subjects with an open
 * violation or an alert. Covers removed modules too, whose health is kept.
 */
final class TelemetryStatsCommand {
    private static final String MODULE = "module";

    /** Subjects listed per module; the others are only counted. */
    static final int MAX_LISTED_SUBJECTS = 8;

    private TelemetryStatsCommand() {}

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("stats")
                .executes(ctx -> run(ctx, null))
                .then(Commands.argument(MODULE, StringArgumentType.word())
                        .suggests(TelemetrySuggestions::sessionModules)
                        .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, MODULE))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, @Nullable String moduleId) {
        TelemetrySession session = TelemetrySession.current();
        if (session == null) {
            TelemetryCommandText.fail(ctx, TelemetryCommandText.NO_SESSION);
            return 0;
        }
        List<String> recorded = session.info().modules();
        if (moduleId != null && !recorded.contains(moduleId)) {
            TelemetryCommandText.fail(ctx, "Module '" + moduleId + "' is not part of this session: " + recorded);
            return 0;
        }
        StringBuilder out = new StringBuilder("Telemetry health:");
        for (String id : moduleId == null ? recorded : List.of(moduleId))
            append(out, id, session.healthSnapshot(id));
        TelemetryCommandText.reply(ctx, out.toString());
        return Command.SINGLE_SUCCESS;
    }

    /** Appends one module's totals, then up to {@link #MAX_LISTED_SUBJECTS} subjects in trouble, worst first. */
    private static void append(StringBuilder out, String moduleId, List<HealthTracker.SubjectHealth> subjects) {
        out.append("\n  ").append(moduleId).append(": ");
        if (subjects.isEmpty()) {
            out.append("no checks yet");
            return;
        }
        long checks = subjects.stream().mapToLong(HealthTracker.SubjectHealth::checks).sum();
        long failed = subjects.stream().mapToLong(HealthTracker.SubjectHealth::failedChecks).sum();
        out.append(subjects.size()).append(" subjects, ").append(checks).append(" checks, ").append(failed)
                .append(" failed (session SLI ")
                .append(TelemetryCommandText.percent(checks == 0 ? 1 : 1 - (double) failed / checks)).append(")");

        List<HealthTracker.SubjectHealth> troubled = subjects.stream()
                .filter(h -> h.alerting() || !h.openViolations().isEmpty())
                .sorted(Comparator.comparingDouble(HealthTracker.SubjectHealth::burnRate).reversed()
                        .thenComparing(h -> h.subject().toString()))
                .toList();
        if (troubled.isEmpty()) {
            out.append("\n    all subjects healthy");
            return;
        }
        for (HealthTracker.SubjectHealth h : troubled.subList(0, Math.min(MAX_LISTED_SUBJECTS, troubled.size()))) {
            out.append("\n    ").append(h.subject())
                    .append(": window SLI ").append(TelemetryCommandText.percent(h.windowSli()))
                    .append(", burn rate ").append(TelemetryCommandText.number(h.burnRate()))
                    .append(", budget ").append(TelemetryCommandText.percent(h.budgetRemaining()));
            if (!h.openViolations().isEmpty()) out.append(", open ").append(h.openViolations());
            if (h.alerting()) out.append(", ALERT");
        }
        if (troubled.size() > MAX_LISTED_SUBJECTS)
            out.append("\n    ... and ").append(troubled.size() - MAX_LISTED_SUBJECTS).append(" more");
    }
}
