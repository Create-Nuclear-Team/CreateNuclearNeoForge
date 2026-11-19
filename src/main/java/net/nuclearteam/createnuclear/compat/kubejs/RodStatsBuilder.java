package net.nuclearteam.createnuclear.compat.kubejs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType.TypeRod;

public class RodStatsBuilder {
    private final ResourceLocation id;
    private final Item item;
    private final RodType.Builder builder;

    RodStatsBuilder(ResourceLocation id, Item item, TypeRod type) {
        this.id = id;
        this.item = item;
        this.builder = new RodType.Builder().item(item).type(type);
    }

    public RodStatsBuilder baseRodHeat(int baseRodHeat) {
        builder.baseRodHeat(baseRodHeat);
        return this;
    }

    public RodStatsBuilder proximityRodHeat(float proximityRodHeat) {
        builder.proximityRodHeat(proximityRodHeat);
        return this;
    }

    public RodStatsBuilder rodTimer(int rodTimer) {
        builder.rodTimer(rodTimer);
        return this;
    }

    public RodStatsBuilder ratio(int ratio) {
        builder.ratio(ratio);
        return this;
    }

    ResourceLocation id() {
        return id;
    }

    Item item() {
        return item;
    }

    RodType build() {
        return builder.build();
    }
}

