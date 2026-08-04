package net.nuclearteam.createnuclear.foundation.events;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.nuclearteam.createnuclear.CNPotions;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.multiblock.input.item.ReactorRodInputEntity;
import net.nuclearteam.createnuclear.content.multiblock.input.fluid.ReactorFluidInputEntity;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommentEvents {
    @SubscribeEvent
    public static void onBrewingRecipeRegister(RegisterBrewingRecipesEvent event) {
        CNPotions.registerPotionsRecipes(event);
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            ReactorRodInputEntity.registerCapabilities(event);
            ReactorFluidInputEntity.registerCapabilities(event);
        }
    }

}
