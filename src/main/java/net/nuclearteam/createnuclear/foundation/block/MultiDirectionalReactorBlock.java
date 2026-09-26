package net.nuclearteam.createnuclear.foundation.block;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

@MethodsReturnNonnullByDefault
public abstract class MultiDirectionalReactorBlock extends DirectionalReactorBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MultiDirectionalReactorBlock(BlockBehaviour.Properties properties) {
        super(properties, FACING);
    }
}
