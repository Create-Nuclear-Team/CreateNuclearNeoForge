package net.nuclearteam.createnuclear.content.multiblock.controller.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

public interface ReactorAlarmManagerI extends ReactorIOManager {
    /**
     * Returns an immutable copy of the valid alarm positions in the current world.
     */
    List<BlockPos> getBlocksPosition(Level level, BlockPos controllerPos);
}