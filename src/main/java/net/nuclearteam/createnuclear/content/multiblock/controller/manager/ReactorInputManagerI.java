package net.nuclearteam.createnuclear.content.multiblock.controller.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.nuclearteam.createnuclear.content.multiblock.input.item.VirtualReactorInputsItem;

import java.util.List;

/**
 * Interface exposing operations specific to reactor inputs.
 * Currently provides access to `IItemHandler` instances at resolved input positions.
 */
public interface ReactorInputManagerI extends ReactorIOManager {
    /**
     * Retrieves valid item handlers for the given `level`.
     * May return an empty list if no valid positions exist.
     */
    List<IItemHandler> getItemHandlers(Level level, BlockPos controllerPos);

    /** Returns an immutable copy of tracked positions. */
    List<BlockPos> getBlocksPosition(Level level, BlockPos controllerPos);

    VirtualReactorInputsItem getInventory(Level level, BlockPos controllerPos);

    boolean extractItems(Level level, BlockPos controllerPos, int fuelNeeded, int coolerNeeded);
    boolean extractItemByName(Level level, BlockPos controllerPos, String itemName);
}
