package net.nuclearteam.createnuclear.foundation.events;


import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CNParticleRegistry;
import net.nuclearteam.createnuclear.CNParticleTypes;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.equipment.armor.AntiRadiationArmorClientExtensions;
import net.nuclearteam.createnuclear.content.particles.NuclearMushroomCloudParticle;
import net.nuclearteam.createnuclear.content.particles.SmallNuclearExplosionParticle;
import net.nuclearteam.createnuclear.foundation.events.overlay.IrradiatedOverlayRendererVision;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID, value = Dist.CLIENT)
public class CNClientEvent {
    private static final HudRenderer HUD_RENDERER = new HudRenderer();

    @SubscribeEvent
    public static void onRegisterGui(RegisterGuiLayersEvent event) {
        HUD_RENDERER.onHudRender(event);
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, CreateNuclear.asResource("irradiated_vision"), IrradiatedOverlayRendererVision::renderOverlay);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        AntiRadiationArmorClientExtensions extensions = new AntiRadiationArmorClientExtensions();
        event.registerItem(extensions,
                CNItems.ANTI_RADIATION_HELMETS.get(),
                CNItems.ANTI_RADIATION_CHESTPLATES.get(),
                CNItems.ANTI_RADIATION_LEGGINGS.get(),
                CNItems.ANTI_RADIATION_BOOTS.get()
        );
    }
  
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        CNParticleTypes.registerFactories(event);
        event.registerSpecial(CNParticleRegistry.NUCLEAR_MUSHROOM_CLOUD.get(), new NuclearMushroomCloudParticle.Factory());
        event.registerSpriteSet(CNParticleRegistry.NUCLEAR_MUSHROOM_CLOUD_SMOKE.get(), SmallNuclearExplosionParticle.NukeFactory::new);
        event.registerSpriteSet(CNParticleRegistry.NUCLEAR_MUSHROOM_CLOUD_EXPLOSION.get(), SmallNuclearExplosionParticle.NukeFactory::new);
    }
}
