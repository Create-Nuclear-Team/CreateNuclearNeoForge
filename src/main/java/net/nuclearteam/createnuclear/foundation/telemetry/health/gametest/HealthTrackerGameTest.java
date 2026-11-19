package net.nuclearteam.createnuclear.foundation.telemetry.health.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.health.HealthCheck;
import net.nuclearteam.createnuclear.foundation.telemetry.health.HealthListener;
import net.nuclearteam.createnuclear.foundation.telemetry.health.HealthTracker;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * GameTest coverage for {@link HealthTracker}.
 * <p>
 * The tracker is built directly with a manual clock and a {@link RecordingListener} that keeps every
 * transition. No session and no facade are involved: the path {@code Telemetry.health} →
 * {@code HealthEventEmitter} → event is covered by the session test. The world is not used; the
 * structure only satisfies the framework.
 */
@GameTestHolder(CreateNuclear.MOD_ID)
@PrefixGameTestTemplate(false)
public class HealthTrackerGameTest {

    private static final String STRUCTURE = "empty_platform";
    private static final String MODULE = "test_health";
    private static final Subject SUBJECT = Subject.of("test_subject");
    private static final double DELTA = 1e-9;

    // ---------- test fixtures ----------

    /** One recorded transition. {@code value} is the detail, the duration or the alert flag. */
    private record Transition(String kind, String invariant, @Nullable Object value) {}

    /** Keeps every transition reported by the tracker. */
    private static final class RecordingListener implements HealthListener {
        final List<Transition> transitions = new ArrayList<>();

        @Override
        public void violationStarted(Subject subject, String invariant, @Nullable String detail) {
            transitions.add(new Transition("started", invariant, detail));
        }

        @Override
        public void violationResolved(Subject subject, String invariant, long durationTicks) {
            transitions.add(new Transition("resolved", invariant, durationTicks));
        }

        @Override
        public void burnRateChanged(Subject subject, boolean alerting, double sli, double sloTarget, double burnRate, int window) {
            transitions.add(new Transition(alerting ? "alert" : "recovered", "", burnRate));
        }

        long count(String kind) {
            return transitions.stream().filter(t -> t.kind().equals(kind)).count();
        }

        Transition first(String kind) {
            return transitions.stream().filter(t -> t.kind().equals(kind)).findFirst()
                    .orElseThrow(() -> new GameTestAssertException("No '" + kind + "' transition"));
        }
    }

    /** A tracker with a manual clock and a recording listener. */
    private static final class Fixture {
        long tick;
        final RecordingListener listener = new RecordingListener();
        final HealthTracker tracker;

        Fixture(int window, double sloTarget) {
            this.tracker = new HealthTracker(MODULE, window, sloTarget, () -> tick, listener);
        }

        void commit(String invariant, boolean violated) {
            new HealthCheck(tracker, SUBJECT).check(invariant, violated, () -> "detail of " + invariant).commit();
        }

        void repeat(int times, String invariant, boolean violated) {
            for (int i = 0; i < times; i++) commit(invariant, violated);
        }

        HealthTracker.SubjectHealth state() {
            return tracker.snapshot().get(0);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    // ---------- tests ----------

    /** A violation opens once, does not restart while still violated, and closes with its duration. */
    @GameTest(template = STRUCTURE)
    public static void violationOpensThenCloses(GameTestHelper helper) {
        Fixture f = new Fixture(100, 0.99);

        f.tick = 10;
        f.commit("rule_a", true);
        check(f.state().openViolations().contains("rule_a"), "rule_a should be open");
        check(f.listener.count("started") == 1, "expected one 'started' transition");
        check("detail of rule_a".equals(f.listener.first("started").value()),
                "detail should be computed when the violation starts");

        f.tick = 50;
        f.commit("rule_a", true);
        check(f.listener.count("started") == 1, "an open violation must not start again");

        f.tick = 70;
        f.commit("rule_a", false);
        check(f.state().openViolations().isEmpty(), "rule_a should be closed");
        Object duration = f.listener.first("resolved").value();
        check(Long.valueOf(60).equals(duration), "duration should be 60 ticks, got " + duration);
        check(f.state().violationCounts().get("rule_a") == 1L, "rule_a should count one violation");
        check("rule_a".equals(f.state().lastViolated()), "last violated should be rule_a");

        helper.succeed();
    }

    /** An invariant missing from a commit keeps its state: only an explicit healthy check closes it. */
    @GameTest(template = STRUCTURE)
    public static void absentInvariantKeepsState(GameTestHelper helper) {
        Fixture f = new Fixture(100, 0.99);

        f.commit("rule_a", true);
        new HealthCheck(f.tracker, SUBJECT).check("rule_b", false).commit();

        check(f.state().openViolations().contains("rule_a"), "rule_a must stay open when not checked");
        check(f.listener.count("resolved") == 0, "rule_a must not be resolved");
        check(f.state().checks() == 2 && f.state().failedChecks() == 1,
                "second commit is healthy: expected 2 checks / 1 failed, got "
                        + f.state().checks() + " / " + f.state().failedChecks());

        helper.succeed();
    }

    /** No burn-rate alert before 10 checks, even at 100% failure; one alert at the 10th. */
    @GameTest(template = STRUCTURE)
    public static void alertOnlyAfterTenChecks(GameTestHelper helper) {
        Fixture f = new Fixture(100, 0.99);

        f.repeat(9, "rule_a", true);
        check(!f.state().alerting(), "no alert before " + HealthTracker.MIN_CHECKS_FOR_ALERT + " checks");
        check(f.listener.count("alert") == 0, "no alert transition before 10 checks");

        f.commit("rule_a", true);
        check(f.state().alerting(), "alert expected at the 10th check");
        check(f.listener.count("alert") == 1, "exactly one alert transition expected");

        f.commit("rule_a", true);
        check(f.listener.count("alert") == 1, "an open alert must not be raised again");

        helper.succeed();
    }

    /** With a window smaller than 10, the alert threshold is the window size. */
    @GameTest(template = STRUCTURE)
    public static void alertThresholdFollowsSmallWindow(GameTestHelper helper) {
        Fixture f = new Fixture(5, 0.99);

        f.repeat(4, "rule_a", true);
        check(!f.state().alerting(), "no alert before the 5-check window is full");
        f.commit("rule_a", true);
        check(f.state().alerting(), "alert expected once the 5-check window is full");

        helper.succeed();
    }

    /** slo_target = 1.0: zero budget, so a single failure gives an infinite burn rate. */
    @GameTest(template = STRUCTURE)
    public static void sloTargetOneHasNoBudget(GameTestHelper helper) {
        Fixture f = new Fixture(10, 1.0);

        f.repeat(9, "rule_a", false);
        check(f.state().burnRate() == 0, "burn rate should be 0 without failure");
        check(Math.abs(f.state().budgetRemaining() - 1.0) < DELTA, "budget should be intact");

        f.commit("rule_a", true);
        check(Double.isInfinite(f.state().burnRate()), "burn rate should be infinite, got " + f.state().burnRate());
        check(f.state().budgetRemaining() == 0, "budget should be exhausted");
        check(f.state().alerting(), "alert expected with an exhausted zero budget");

        helper.succeed();
    }

    /** The window slides: old failures leave it, the session SLI keeps them, the alert recovers once. */
    @GameTest(template = STRUCTURE)
    public static void windowSlidesAndCountsFailures(GameTestHelper helper) {
        Fixture f = new Fixture(10, 0.5);

        f.repeat(10, "rule_a", true);
        check(Math.abs(f.state().windowSli()) < DELTA, "window SLI should be 0 after 10 failures");
        check(f.state().alerting(), "burn rate 2 should alert");

        f.repeat(10, "rule_a", false);
        check(Math.abs(f.state().windowSli() - 1.0) < DELTA, "window SLI should be 1 once failures left the window");
        check(Math.abs(f.state().sessionSli() - 0.5) < DELTA, "session SLI should be 0.5, got " + f.state().sessionSli());
        check(f.state().checks() == 20 && f.state().failedChecks() == 10, "expected 20 checks / 10 failed");
        check(!f.state().alerting(), "alert should be closed");
        check(f.listener.count("recovered") == 1, "exactly one recovery transition expected");

        helper.succeed();
    }
}
