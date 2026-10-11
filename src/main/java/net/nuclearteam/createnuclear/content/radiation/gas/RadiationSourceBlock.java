package net.nuclearteam.createnuclear.content.radiation.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * WIP / debug : source de gaz radioactif. Clic droit pour changer la force (1..15).
 * Se ré-enregistre toutes les secondes via un scheduled tick (sauvegardé avec le monde),
 * donc la source survit au rechargement alors que le manager est en mémoire.
 */
public class RadiationSourceBlock extends Block {
    public static final IntegerProperty STRENGTH = IntegerProperty.create("strength", 1, RadiationGasBlock.MAX_CONCENTRATION);
    private static final int REFRESH_TICKS = 20;

    public RadiationSourceBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STRENGTH, RadiationGasBlock.MAX_CONCENTRATION));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STRENGTH);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server) {
            RadiationGasManager.get(server).setSource(pos, state.getValue(STRENGTH));
            server.scheduleTick(pos, this, REFRESH_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        RadiationGasManager.get(level).setSource(pos, state.getValue(STRENGTH));
        level.scheduleTick(pos, this, REFRESH_TICKS);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server)
            RadiationGasManager.get(server).removeSource(pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int next = state.getValue(STRENGTH) % RadiationGasBlock.MAX_CONCENTRATION + 1;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(STRENGTH, next), Block.UPDATE_ALL);
            player.displayClientMessage(Component.literal("Radiation source strength: " + next), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
