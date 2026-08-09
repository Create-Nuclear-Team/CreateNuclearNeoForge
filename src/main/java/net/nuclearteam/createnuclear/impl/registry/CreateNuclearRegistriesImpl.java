package net.nuclearteam.createnuclear.impl.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.CreateNuclearRegistries;
import net.nuclearteam.createnuclear.api.multiblock.fluid.ReactorFluidType;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Declares the mod's custom datapack registries. Without this, the registries have no codec
 * registered, so datagen cannot clone them ("No cloner for createnuclear:fluids/type") and they
 * would never be loaded from datapacks at runtime.
 */
@EventBusSubscriber(modid = CreateNuclear.MOD_ID)
public class CreateNuclearRegistriesImpl {
    private CreateNuclearRegistriesImpl() {}

    @Internal
    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(
            CreateNuclearRegistries.ROD_TYPE,
            RodType.CODEC,
            RodType.CODEC
        );

        event.dataPackRegistry(
            CreateNuclearRegistries.FLUID_TYPE,
            ReactorFluidType.CODEC,
            ReactorFluidType.CODEC
        );
    }
}