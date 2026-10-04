package net.nuclearteam.createnuclear.infrastructure.worldgen.biome;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters;
import net.nuclearteam.createnuclear.CreateNuclear;

public class CNNoiseData {
    public static final ResourceKey<NoiseParameters> EROSION = createKey("irradiated/erosion");

    private static ResourceKey<NoiseParameters> createKey(String id) {
        return ResourceKey.create(Registries.NOISE, CreateNuclear.asResource(id));
    }

    public static void bootstrapRegistries(BootstrapContext<NoiseParameters> context) {
        register(context, EROSION, -9, 1.0, 1.0, 0.0, 1.0, 1.0);
    }

    private static void register(
            BootstrapContext<NoiseParameters> context,
            ResourceKey<NoiseParameters> key,
            int firstOctave,
            double amplitude,
            double... otherAmplitudes
    ) {
        context.register(key, new NoiseParameters(firstOctave, amplitude, otherAmplitudes));
    }
}