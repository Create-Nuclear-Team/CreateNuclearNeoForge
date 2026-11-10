package net.nuclearteam.createnuclear.foundation.telemetry.sink.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.api.Subject;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryEvent;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.sink.TelemetryFormat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** GameTest coverage for {@link net.nuclearteam.createnuclear.foundation.telemetry.sink.TelemetryFormat}: line layout, quoting, decimals, one line per event. */
@GameTestHolder(CreateNuclear.MOD_ID)
@PrefixGameTestTemplate(false)
public class TelemetryFormatGameTest {

    private static final String STRUCTURE = "empty_platform";

    private static void checkEquals(Object expected, Object actual, String what) {
        if (!Objects.equals(expected, actual))
            throw new GameTestAssertException(what + ": expected <" + expected + "> but got <" + actual + ">");
    }

    /** The README example of section 1.4, byte for byte. */
    @GameTest(template = STRUCTURE)
    public static void lineMatchesReadmeLayout(GameTestHelper helper) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("assembled", true);
        attrs.put("outputs", 2);
        TelemetryEvent e = new TelemetryEvent(37, 0, TelemetryLevel.INFO, "reactor", "assembly",
                new Subject("reactor", "minecraft:overworld", new BlockPos(120, 64, -30)), "controller removed", attrs);
        checkEquals("[+37][INFO][reactor/assembly][reactor@minecraft:overworld[120,64,-30]] controller removed {assembled=true, outputs=2}",
                TelemetryFormat.line(e), "line");
        checkEquals("[+0][WARN][telemetry/session][server] stopped",
                TelemetryFormat.line(new TelemetryEvent(0, 0, TelemetryLevel.WARN, "telemetry", "session", Subject.SERVER, "stopped", null)),
                "line without attributes");
        helper.succeed();
    }

    /** Values that would break parsing are quoted; decimals are compact. */
    @GameTest(template = STRUCTURE)
    public static void valuesAreQuotedAndRounded(GameTestHelper helper) {
        checkEquals("\"controller missing speed=12.0\"", TelemetryFormat.value("controller missing speed=12.0"), "quoted");
        checkEquals("\"say \\\"hi\\\"\"", TelemetryFormat.value("say \"hi\""), "escaped quotes");
        checkEquals("\"\"", TelemetryFormat.value(""), "empty");
        checkEquals("plain_value", TelemetryFormat.value("plain_value"), "unquoted");
        checkEquals("null", TelemetryFormat.value(null), "null");
        checkEquals("0.99", TelemetryFormat.value(0.9900000095367432), "rounded");
        checkEquals("12.0", TelemetryFormat.value(12.0), "integral");
        checkEquals("1.0E-9", TelemetryFormat.value(1e-9), "tiny");
        checkEquals("inf", TelemetryFormat.value(Double.POSITIVE_INFINITY), "infinite");
        checkEquals("NaN", TelemetryFormat.value(Double.NaN), "nan");
        helper.succeed();
    }

    /** An event always fits on one line. */
    @GameTest(template = STRUCTURE)
    public static void lineBreaksAreEscaped(GameTestHelper helper) {
        TelemetryEvent e = new TelemetryEvent(1, 0, TelemetryLevel.ERROR, "reactor", "state", Subject.SERVER,
                "first\nsecond", Map.of("trace", "a\r\nb"));
        String line = TelemetryFormat.line(e);
        checkEquals(-1, line.indexOf('\n'), "no line feed");
        checkEquals("[+1][ERROR][reactor/state][server] first\\nsecond {trace=a\\nb}", line, "escaped");
        helper.succeed();
    }

    /** A backslash is escaped before line breaks, identically inside and outside quotes. */
    @GameTest(template = STRUCTURE)
    public static void backslashesAreEscapedConsistently(GameTestHelper helper) {
        checkEquals("a\\nb", TelemetryFormat.value("a\nb"), "line break, unquoted");
        checkEquals("\"a b\\nc\"", TelemetryFormat.value("a b\nc"), "line break, quoted");
        checkEquals("C:\\\\mods\\\\x", TelemetryFormat.value("C:\\mods\\x"), "backslash, unquoted");
        checkEquals("\"C:\\\\my mods\"", TelemetryFormat.value("C:\\my mods"), "backslash, quoted");
        checkEquals("a\\\\nb", TelemetryFormat.value("a\\nb"), "literal backslash-n differs from a line break");
        checkEquals("\"x\\\\\\\"y\"", TelemetryFormat.value("x\\\"y"), "backslash then quote");
        checkEquals("[+0][INFO][reactor/state][server] path C:\\\\mods",
                TelemetryFormat.line(new TelemetryEvent(0, 0, TelemetryLevel.INFO, "reactor", "state", Subject.SERVER, "path C:\\mods", null)),
                "message");
        helper.succeed();
    }
}
