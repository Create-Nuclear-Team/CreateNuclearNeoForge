package net.nuclearteam.createnuclear.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;

public interface CNKubeJSEvents {
    EventGroup GROUP = EventGroup.of("CreateNuclearEvents");

    EventHandler FLUID_ROD_STATS = GROUP.startup("fluidRodStats", () -> FluidRodStatsKubeEvent.class);

    static void postFluidRodStats() {
        FluidRodStatsKubeEvent event = new FluidRodStatsKubeEvent();
        FLUID_ROD_STATS.post(ScriptType.STARTUP, event);
        event.register();
    }
}
