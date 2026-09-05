package net.nuclearteam.createnuclear.content.multiblock.controller.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Function;

/**
 * Generic manager interface for reactor IO positions (inputs or outputs).
 * <p>
 * Tracked positions are stored RELATIVE to the reactor controller (an offset,
 * not a world-absolute BlockPos), so the reactor keeps working correctly after
 * the whole multiblock is physically relocated (e.g. Aeronautics/Sable, or a
 * Create Contraption) without needing to re-run assembly detection. Any
 * method that needs to touch the actual {@link Level} therefore takes the
 * controller's CURRENT position ({@code controllerPos}) so it can resolve
 * each offset back to a world position on demand.
 */
public interface ReactorIOManager {
    /** Returns true when the relative offset is already tracked. */
    boolean contains(BlockPos relativeOffset);

    /** Current number of tracked positions. */
    int size();

    /** Reads state from an NBT tag (deserialization). */
    void read(CompoundTag compound);

    /** Writes state to an NBT tag (serialization). */
    void write(CompoundTag compound);

    /** Adds a relative offset; no-op if already tracked. */
    void addBlock(BlockPos relativeOffset);

    /** Removes a relative offset if present. */
    void removeBlock(BlockPos relativeOffset);

    /** Returns an immutable copy of the tracked relative offsets. */
    List<BlockPos> getBlocksPosition();

    /**
     * Resolves every tracked offset to its current world position, given the
     * controller's current {@code controllerPos}.
     */
    List<BlockPos> getAbsolutePositions(BlockPos controllerPos);

    /** Removes offsets that no longer resolve to a valid block in {@code level}. */
    void clearInvalid(Level level, BlockPos controllerPos);

    /**
     * Resolves each tracked offset into an instance using {@code resolver} and
     * returns the list of non-null results.
     */
    <T> List<T> resolveBlock(Level level, BlockPos controllerPos, Function<BlockPos, T> resolver);
}
