package net.nuclearteam.createnuclear.foundation.events;


import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.nuclearteam.createnuclear.CNEntityType;

@EventBusSubscriber(value = Dist.CLIENT)
public class CommentEventClients {
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        CNEntityType.registerModelLayer(event);
    }


}
