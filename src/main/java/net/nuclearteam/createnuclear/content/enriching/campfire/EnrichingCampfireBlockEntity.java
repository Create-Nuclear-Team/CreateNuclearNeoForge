package net.nuclearteam.createnuclear.content.enriching.campfire;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

@SuppressWarnings("unused")
public class EnrichingCampfireBlockEntity extends SmartBlockEntity {
    public EnrichingCampfireBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void particleTick(Level level, BlockPos pos, BlockState state, EnrichingCampfireBlockEntity blockEntity) {
        int i;
        RandomSource randomSource = level.random;
        if (randomSource.nextFloat() < 0.11f) {
            for (i = 0; i < randomSource.nextInt(2) + 2; ++i) {
                EnrichingCampfireBlock.makeParticles(level, pos);
            }
        }
        i = state.getValue(EnrichingCampfireBlock.FACING).get2DDataValue();
    }

    private void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    public void dowse() {
        if (this.level != null) {
            this.markUpdated();
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }
}
