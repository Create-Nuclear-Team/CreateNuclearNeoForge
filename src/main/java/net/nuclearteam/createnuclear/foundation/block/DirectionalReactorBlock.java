package net.nuclearteam.createnuclear.foundation.block;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Shared {@code rotate}/{@code mirror} implementation for reactor blocks whose
 * orientation is tracked by a single {@link DirectionProperty}, whichever one
 * it is ({@link net.minecraft.world.level.block.state.properties.BlockStateProperties#HORIZONTAL_FACING}
 * vs {@link net.minecraft.world.level.block.state.properties.BlockStateProperties#FACING}).
 */
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public abstract class DirectionalReactorBlock extends Block {
    private final DirectionProperty facingProperty;

    protected DirectionalReactorBlock(BlockBehaviour.Properties properties, DirectionProperty facingProperty) {
        super(properties);
        this.facingProperty = facingProperty;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(facingProperty, rotation.rotate(state.getValue(facingProperty)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(facingProperty)));
    }
}
