package net.nuclearteam.createnuclear.foundation.events;


import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.nuclearteam.createnuclear.CNAttributes;
import net.nuclearteam.createnuclear.CNFluids;
import net.nuclearteam.createnuclear.CNPotions;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.multiblock.input.item.ReactorRodInputEntity;
import net.nuclearteam.createnuclear.content.multiblock.input.fluid.ReactorFluidInputEntity;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID)
public class CommentEvents {
    @SubscribeEvent
    public static void onBrewingRecipeRegister(RegisterBrewingRecipesEvent event) {
        CNPotions.registerPotionsRecipes(event);
    }

    @EventBusSubscriber()
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            ReactorRodInputEntity.registerCapabilities(event);
            ReactorFluidInputEntity.registerCapabilities(event);
            event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new FluidBucketWrapper(stack),
                CNFluids.URANIUM.get().getBucket());
        }
    }

    @EventBusSubscriber(modid = CreateNuclear.MOD_ID)
    public static class RadiationEvent {
        @SubscribeEvent
        public static void onEntityAttribute(EntityAttributeModificationEvent event) {
            for (EntityType<? extends LivingEntity> type : event.getTypes()) {
                event.add(type, CNAttributes.IRRADIATED_RESISTANCE);
            }
        }
    }

}

