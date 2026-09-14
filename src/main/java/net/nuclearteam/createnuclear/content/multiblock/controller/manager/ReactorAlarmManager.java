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
    private static final String NBT_KEY = "ReactorAlarms";

    @Override
    public void write(CompoundTag compound) {
        ListTag list = new ListTag();
        for (BlockPos pos : positions) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("p", pos.asLong());
            list.add(tag);
        }
        compound.put(NBT_KEY, list);
    }

    @Override
    public void read(CompoundTag compound) {
        positions.clear();
        if (!compound.contains(NBT_KEY)) return;
        ListTag list = compound.getList(NBT_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); ++i) {
            positions.add(BlockPos.of(list.getCompound(i).getLong("p")));
        }
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
