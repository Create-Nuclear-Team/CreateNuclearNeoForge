package net.nuclearteam.createnuclear.foundation.telemetry.metrics.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.metrics.MetricSet;

import java.util.Map;
import java.util.Objects;

/** GameTest coverage for {@link MetricSet#drain}. No session is needed. */
@GameTestHolder(CreateNuclear.MOD_ID)
@PrefixGameTestTemplate(false)
public class MetricSetGameTest {

    private static final String STRUCTURE = "empty_platform";

    private static void checkEquals(Object expected, Object actual, String what) {
        if (!Objects.equals(expected, actual))
            throw new GameTestAssertException(what + ": expected <" + expected + "> but got <" + actual + ">");
    }

    /**
     * A set is drained only after an update: a subject that stopped updating (a broken reactor) is
     * no longer written. A later update writes every metric again, with the last gauge value.
     */
    @GameTest(template = STRUCTURE)
    public static void drainSkipsSetsNotUpdated(GameTestHelper helper) {
        MetricSet m = new MetricSet("test", Subject.SERVER);
        checkEquals(Map.of(), m.drain(), "new set");

        m.gauge("heat", 5).inc("events");
        checkEquals(Map.of("heat", 5.0, "events", 1L), m.drain(), "after an update");
        checkEquals(Map.of(), m.drain(), "nothing updated since");

        m.observe("tick_ms", 2);
        Map<String, Object> values = m.drain();
        checkEquals(5.0, values.get("heat"), "gauge kept");
        checkEquals(1L, values.get("events"), "counter kept");
        if (!values.containsKey("tick_ms_count"))
            throw new GameTestAssertException("histogram missing after an observation: " + values);
        checkEquals(Map.of(), m.drain(), "nothing updated after the histogram");

        m.gauge("heat", 5);
        checkEquals(5.0, m.drain().get("heat"), "same value counts as an update");
        helper.succeed();
    }
}
