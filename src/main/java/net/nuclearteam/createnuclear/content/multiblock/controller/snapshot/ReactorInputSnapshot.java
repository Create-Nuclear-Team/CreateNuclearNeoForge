package net.nuclearteam.createnuclear.content.multiblock.controller.snapshot;

import net.minecraft.world.item.Item;
import net.nuclearteam.createnuclear.content.logistics.BigFluidStack;

import java.util.List;
import java.util.Map;

/**
 * Snapshot of the reactor's inputs, collected once per tick.
 * <p>
 * {@code items} reflects the items actually present (for tooltip display).
 * {@code fluids} holds one entry per distinct fluid type present, aggregated
 * across every fluid input, and {@code maxFluidCapacity} is the summed tank
 * capacity across every fluid input.
 * <p>
 * The elements of {@code fluids} ({@link BigFluidStack}) remain mutable
 * (public fields); this snapshot only guarantees immutability of the
 * list/map structure, not of their contents.
 */

public record ReactorInputSnapshot(Map<Item, Integer> items, List<BigFluidStack> fluids, long maxFluidCapacity) {
    public static final ReactorInputSnapshot EMPTY = new ReactorInputSnapshot(
            Map.of(), List.of(), 0
    );

    public ReactorInputSnapshot {
        items = Map.copyOf(items);
        fluids = List.copyOf(fluids);
    }
}
