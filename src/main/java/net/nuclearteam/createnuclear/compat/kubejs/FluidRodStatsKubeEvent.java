package net.nuclearteam.createnuclear.compat.kubejs;

import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.nuclearteam.createnuclear.api.ItemRodTypesValue;
import net.nuclearteam.createnuclear.api.ReactorFluidTypesValue;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType.TypeRod;

import java.util.ArrayList;
import java.util.List;

/**
 * Startup event letting scripts give reactor stats to items (fuel/cooler rods) and fluids
 * through chainable builders. Builders are registered once every script has run, since a
 * chain gives no signal when it is complete.
 * <p>
 * Entries already defined (by Create Nuclear or another mod) are skipped, since the
 * underlying registries reject duplicates: override those with a datapack instead.
 */
public class FluidRodStatsKubeEvent implements KubeStartupEvent {
    private final List<RodStatsBuilder> rods = new ArrayList<>();
    private final List<FluidStatsBuilder> fluids = new ArrayList<>();

    public RodStatsBuilder fuel(ResourceLocation item) {
        return rod(item, TypeRod.FUEL);
    }

    public RodStatsBuilder cooler(ResourceLocation item) {
        return rod(item, TypeRod.COOLER);
    }

    public FluidStatsBuilder fluid(ResourceLocation fluid) {
        Fluid value = BuiltInRegistries.FLUID.getOptional(fluid)
                .orElseThrow(() -> new IllegalArgumentException("Unknown fluid: " + fluid));
        FluidStatsBuilder builder = new FluidStatsBuilder(fluid, value);
        fluids.add(builder);

        return builder;
    }

    public RodStatsBuilder rod(ResourceLocation item, TypeRod type) {
        Item value = BuiltInRegistries.ITEM.getOptional(item)
                .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + item));
        RodStatsBuilder builder = new RodStatsBuilder(item, value, type);
        rods.add(builder);

        return builder;
    }

    void register() {
        for (RodStatsBuilder rod : rods) {
            if (ItemRodTypesValue.ROD_TYPE.get(rod.item()) != null) {
                ConsoleJS.STARTUP.warn("Item " + rod.id() + " already has rod stats, skipping");
                continue;
            }

            try {
                ItemRodTypesValue.ROD_TYPE.register(rod.item(), rod.build());
            } catch (IllegalStateException e) {
                ConsoleJS.STARTUP.error("Invalid rod stats for " + rod.id(), e);
            }
        }

        for (FluidStatsBuilder fluid : fluids) {
            if (ReactorFluidTypesValue.REACTOR_FLUID_TYPE.get(fluid.fluid()) != null) {
                ConsoleJS.STARTUP.warn("Fluid " + fluid.id() + " already has reactor stats, skipping");
                continue;
            }
            try {
                ReactorFluidTypesValue.REACTOR_FLUID_TYPE.register(fluid.fluid(), fluid.build());
            } catch (IllegalStateException e) {
                ConsoleJS.STARTUP.error("Invalid reactor stats for fluid " + fluid.id(), e);
            }
        }
    }
}
