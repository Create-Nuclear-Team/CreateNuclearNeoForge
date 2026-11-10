package net.nuclearteam.createnuclear.foundation.telemetry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.nuclearteam.createnuclear.CreateNuclear;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID)
public final class TelemetryServerEvents {
    private TelemetryServerEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TelemetrySession.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TelemetrySession.onServerStopping(event.getServer());
    }
}
