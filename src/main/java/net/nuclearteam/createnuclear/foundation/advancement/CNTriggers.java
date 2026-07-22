package net.nuclearteam.createnuclear.foundation.advancement;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nuclearteam.createnuclear.CreateNuclear;

import java.util.function.Supplier;

public class CNTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, CreateNuclear.MOD_ID);

    public static SimpleCreateNuclearTrigger addSimple(String id) {
        SimpleCreateNuclearTrigger trigger = new SimpleCreateNuclearTrigger();
        TRIGGERS.register(id, () -> trigger);
        return trigger;
    }

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }
}