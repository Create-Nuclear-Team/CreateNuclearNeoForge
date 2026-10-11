package net.nuclearteam.createnuclear.content.radiation.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class RadiationGasExposure {
    private static final double CURVE = 4.0;     // plus c'est grand, plus la montée est brutale près de la source
    private static final double MAX_DOSE = 60.0; // dose à 100% (> radiation_level_3 = 50 => Radiation IV)

    private RadiationGasExposure() {}

    /** 0..100, exponentiel : la fin de la courbe (proche de la source) grimpe très vite. */
    public static double getExposurePercent(Level level, LivingEntity entity) {
        int c = Math.max(concentrationAt(level, entity.blockPosition()),
                concentrationAt(level, BlockPos.containing(entity.getEyePosition())));
        if (c <= 0) return 0;
        double x = c / (double) RadiationGasBlock.MAX_CONCENTRATION;
        return 100.0 * (Math.exp(CURVE * x) - 1) / (Math.exp(CURVE) - 1);
    }

    public static double getDose(Level level, LivingEntity entity) {
        return getExposurePercent(level, entity) / 100.0 * MAX_DOSE;
    }

    private static int concentrationAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof RadiationGasBlock ? state.getValue(RadiationGasBlock.CONCENTRATION) : 0;
    }
}
