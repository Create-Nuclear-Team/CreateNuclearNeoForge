package net.nuclearteam.createnuclear.content.contraptions.irradiated.spider;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

/**
 * Stage 2: start of the mutation (cracked chitin, first veins, extra eyes).
 */
public class IrradiatedSpiderStage2 extends AbstractIrradiatedSpider {
    /** The death animation lasts 1.2 s (24 ticks). */
    private static final int DEATH_DURATION_TICKS = 28;

    public IrradiatedSpiderStage2(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    @Override
    protected int getDeathDurationTicks() {
        return DEATH_DURATION_TICKS;
    }
}
