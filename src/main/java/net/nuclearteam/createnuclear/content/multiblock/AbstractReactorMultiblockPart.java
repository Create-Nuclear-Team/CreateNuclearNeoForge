package net.nuclearteam.createnuclear.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.nuclearteam.createnuclear.content.multiblock.pattern.ReactorPattern;
import org.jetbrains.annotations.Nullable;

public class AbstractReactorMultiblockPart extends Block {
    protected final ReactorPattern pattern = new ReactorPattern();

    public AbstractReactorMultiblockPart(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        pattern.findController(pos, level, true);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        MultiblockHelpers.handleAdvancedPlacedBy(pos, level, placer);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (shouldRescanOnRemove(state, newState)) {
            pattern.findController(pos, level, false);
        }
    }

    protected boolean shouldRescanOnRemove(BlockState state, BlockState newState) {
        return true;
    }
}
