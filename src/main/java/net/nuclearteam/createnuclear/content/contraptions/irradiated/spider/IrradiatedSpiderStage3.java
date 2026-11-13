package net.nuclearteam.createnuclear.content.contraptions.irradiated.spider;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Stage 3: full mutation (swollen abdomen with radioactive sacs, bone spikes). The abdomen bursts
 * during the death animation.
 */
public class IrradiatedSpiderStage3 extends AbstractIrradiatedSpider {
    /** The death animation lasts 1.8 s (36 ticks). */
    private static final int DEATH_DURATION_TICKS = 42;
    private static final DustParticleOptions NUCLEAR_DUST = new DustParticleOptions(new Vector3f(0.0F, 254.0F / 255.0F, 4.0F / 255.0F), 1.6F);

    public IrradiatedSpiderStage3(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    @Override
    protected int getDeathDurationTicks() {
        return DEATH_DURATION_TICKS;
    }

    @Override
    protected void onAnimationInstruction(String instructions) {
        if (instructions.contains("burst"))
            spawnRadiationBurst();
    }

    /** Fired by the "burst" keyframe at 1.0 s of the death animation. */
    private void spawnRadiationBurst() {
        Level level = this.level();
        double cx = this.getX();
        double cy = this.getY() + 0.6D;
        double cz = this.getZ();
        for (int i = 0; i < 60; i++) {
            double vx = (this.random.nextDouble() - 0.5D) * 0.6D;
            double vy = this.random.nextDouble() * 0.5D;
            double vz = (this.random.nextDouble() - 0.5D) * 0.6D;
            level.addParticle(NUCLEAR_DUST, cx, cy, cz, vx, vy, vz);
        }
        // Placeholder sound until a dedicated burst sound exists
        level.playLocalSound(cx, cy, cz, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F, false);
    }
}
