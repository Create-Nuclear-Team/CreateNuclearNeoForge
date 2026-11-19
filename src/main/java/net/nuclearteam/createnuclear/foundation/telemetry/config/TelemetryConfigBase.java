package net.nuclearteam.createnuclear.foundation.telemetry.config;

import net.createmod.catnip.config.ConfigBase;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Base of the telemetry configs. Remembers the key, default and current value of every setting,
 * so that a session can write the settings that differ from their default ({@code config}) and
 * the changes made while it runs ({@code config changed}).
 * <p>
 * Settings are tracked where they are declared, because catnip keeps the default value of a
 * {@code CValue} out of reach. Only typed values are tracked: boolean, integer, decimal and level.
 * Free text is never tracked, since it could hold a path or an address (privacy rules, README
 * section 1.6).
 */
public abstract class TelemetryConfigBase extends ConfigBase {
    /** A tracked setting; {@code current} is read on each call. */
    private record Setting(String key, Object defaultValue, Supplier<?> current) {}

    private final List<Setting> settings = new ArrayList<>();

    /**
     * Tracks a boolean setting.
     *
     * @param value        the declared setting
     * @param defaultValue its default value
     * @return {@code value}
     */
    protected ConfigBool trackBool(ConfigBool value, boolean defaultValue) {
        settings.add(new Setting(value.getName(), defaultValue, value::get));
        return value;
    }

    /**
     * Tracks an integer setting.
     *
     * @param value        the declared setting
     * @param defaultValue its default value
     * @return {@code value}
     */
    protected ConfigInt trackInt(ConfigInt value, int defaultValue) {
        settings.add(new Setting(value.getName(), defaultValue, value::get));
        return value;
    }

    /**
     * Tracks a decimal setting.
     *
     * @param value        the declared setting
     * @param defaultValue its default value
     * @return {@code value}
     */
    protected CValue<Double, ModConfigSpec.DoubleValue> trackDecimal(CValue<Double, ModConfigSpec.DoubleValue> value,
                                                                     double defaultValue) {
        settings.add(new Setting(value.getName(), defaultValue, value::get));
        return value;
    }

    /**
     * Tracks a level setting.
     *
     * @param value        the declared setting
     * @param defaultValue its default value
     * @return {@code value}
     */
    protected ConfigEnum<TelemetryLevel> trackLevel(ConfigEnum<TelemetryLevel> value, TelemetryLevel defaultValue) {
        settings.add(new Setting(value.getName(), defaultValue, value::get));
        return value;
    }

    /**
     * Returns the current value of every tracked setting.
     *
     * @return key to value, in declaration order; levels as their lower-case id
     */
    public Map<String, Object> values() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Setting s : settings) out.put(s.key(), normalize(s.current().get()));
        return out;
    }

    /**
     * Returns the tracked settings whose current value differs from their default.
     *
     * @return key to current value, in declaration order; levels as their lower-case id
     */
    public Map<String, Object> changedValues() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Setting s : settings) {
            Object value = s.current().get();
            if (!Objects.equals(value, s.defaultValue())) out.put(s.key(), normalize(value));
        }
        return out;
    }

    /** A level is written as its id ({@code info}), like in the commands. */
    private static Object normalize(Object value) {
        return value instanceof TelemetryLevel level ? level.id() : value;
    }
}
