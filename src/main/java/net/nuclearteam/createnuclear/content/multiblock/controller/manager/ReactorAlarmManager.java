package net.nuclearteam.createnuclear.content.multiblock.controller.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nuclearteam.createnuclear.content.multiblock.alarm.ReactorAlarmEntity;

import java.util.ArrayList;
import java.util.List;

public class ReactorAlarmManager extends AbstractReactorIOManager implements ReactorAlarmManagerI {
    @Override
    protected String nbtKey() {
        return "ReactorAlarms";
    }

    @Override
    public void clearInvalid(Level level, BlockPos controllerPos) {
        if (level == null) return;
        List<BlockPos> toRemove = new ArrayList<>();

        for (BlockPos offset : positions) {
            BlockPos p = controllerPos.offset(offset);
            if (!level.isLoaded(p)) continue; // Don't remove if the chunk is just unloaded

            BlockEntity be = level.getBlockEntity(p);
            if (be == null || !(be instanceof ReactorAlarmEntity)) {
                toRemove.add(offset);
            }
        }
        positions.removeAll(toRemove);
    }

    @Override
    public List<BlockPos> getBlocksPosition(Level level, BlockPos controllerPos) {
        return filterByType(level, controllerPos, ReactorAlarmEntity.class);
    }
}
