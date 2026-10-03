package net.nuclearteam.createnuclear;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.neoforged.neoforge.registries.DeferredHolder;

public class CNAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, CreateNuclear.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> IRRADIATED_RESISTANCE = ATTRIBUTES.register("generic.irradiated_resistance", () -> new RangedAttribute("attribute.name.createnuclear.generic.irradiated_resistance", 0, 0, 1).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}