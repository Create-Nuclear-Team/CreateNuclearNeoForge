package net.nuclearteam.createnuclear.content.multiblock.cooler;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.nuclearteam.createnuclear.content.multiblock.AbstractReactorMultiblockPart;
import net.nuclearteam.createnuclear.content.multiblock.MultiblockHelpers;
import net.nuclearteam.createnuclear.content.multiblock.pattern.ReactorPattern;

import javax.annotation.Nullable;

public class ReactorCooler extends AbstractReactorMultiblockPart implements IWrenchable {
    public ReactorCooler(Properties properties) {
        super(properties);
    }
}
