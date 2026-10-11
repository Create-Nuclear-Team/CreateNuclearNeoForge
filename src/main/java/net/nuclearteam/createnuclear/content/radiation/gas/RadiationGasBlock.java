package net.nuclearteam.createnuclear.content.radiation.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class RadiationGasBlock extends Block {
    public static final int MAX_CONCENTRATION = 15;
    public static final IntegerProperty CONCENTRATION = IntegerProperty.create("concentration", 1, MAX_CONCENTRATION);

    private static final DustParticleOptions DEBUG_PARTICLE = new DustParticleOptions(new Vector3f(0.4F, 1F, 0.2F), 1F);

    public RadiationGasBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CONCENTRATION, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONCENTRATION);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // Comme le verre : pas de faces internes entre deux blocs de gaz, le nuage se rend comme un seul volume
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    // Nettoyage des blocs orphelins (le manager est en mémoire pour le WIP : après un reboot il ne les connaît plus)
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!RadiationGasManager.get(level).isTracked(pos))
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    // WIP : particules pour visualiser le gaz, plus denses quand la concentration est haute
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int c = state.getValue(CONCENTRATION);
        if (random.nextInt(MAX_CONCENTRATION + 2 - c) != 0) return;
        level.addParticle(DEBUG_PARTICLE, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0, 0);
    }
}
