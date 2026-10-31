package net.nuclearteam.createnuclear.compat.kubejs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.nuclearteam.createnuclear.api.multiblock.fluid.ReactorFluidType;

public class FluidStatsBuilder {
    private final ResourceLocation id;
    private final Fluid fluid;
    private final ReactorFluidType.Builder builder;

    FluidStatsBuilder(ResourceLocation id, Fluid fluid) {
        this.id = id;
        this.fluid = fluid;
        this.builder = new ReactorFluidType.Builder().fluid(fluid);
    }

    public FluidStatsBuilder maxHeat(int maxHeat) {
        builder.maxHeat(maxHeat);
        return this;
    }

    public FluidStatsBuilder efficiency(int efficiency) {
        builder.efficiency(efficiency);
        return this;
    }

    ResourceLocation id() {
        return id;
    }

    Fluid fluid() {
        return fluid;
    }

    ReactorFluidType build() {
        return builder.build();
    }
}

