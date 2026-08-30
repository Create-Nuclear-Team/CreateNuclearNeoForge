//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.nuclearteam.createnuclear.foundation.utility;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public class Maths {
    public Maths() {
    }

    private static final SimplexNoise NOISE = new SimplexNoise(RandomSource.create(0));

    public static float smin(float a, float b, float k) {
        float h = Math.max(k - Math.abs(a - b), 0.0F) / k;
        return Math.min(a, b) - h * h * k * 0.25F;
    }

    public static float sampleNoise3D(float x, float y, float z, float simplexSampleRate) {
        return (float) NOISE.getValue(((x + simplexSampleRate) / simplexSampleRate), ((y + simplexSampleRate) / simplexSampleRate), ((z + simplexSampleRate) / simplexSampleRate));
    }
}
