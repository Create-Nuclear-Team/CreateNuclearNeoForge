package net.nuclearteam.createnuclear.content.multiblock.controller.service;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlockEntity;

/**
 * Reads and writes the controller's own state (multiblock size/facing/bounds,
 * blueprint stack, and either the inventory or the client display snapshot).
 * <p>
 * Both methods take a {@link HolderLookup.Provider} on top of the Forge
 * signature: from 1.21 onwards, serializing an {@code ItemStack} or a
 * {@code FluidStack} requires registry access.
 */
public interface IPersistenceService {
    void readBasicState(ReactorControllerBlockEntity owner, CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket);
    void writeBasicState(ReactorControllerBlockEntity owner, CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket);
}
