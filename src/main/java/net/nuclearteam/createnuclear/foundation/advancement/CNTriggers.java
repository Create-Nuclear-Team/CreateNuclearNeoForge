package net.nuclearteam.createnuclear.foundation.advancement;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.LinkedList;
import java.util.List;

public class CNTriggers {
    private static final List<CriterionTriggerBase<?>> triggers = new LinkedList<>();

    public static SimpleCreateNuclearTrigger addSimple(String id) {
        return add(new SimpleCreateNuclearTrigger(id));
    }

    private static <T extends CriterionTriggerBase<?>> T add(T instance) {
        triggers.add(instance);

        return instance;
    }

    public static void register() {
        triggers.forEach(triggers -> {
            Registry.register(BuiltInRegistries.TRIGGER_TYPES, triggers.getId(), triggers);
        });
    }
}