package net.nuclearteam.createnuclear.infrastructure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.nuclearteam.createnuclear.CreateNuclear;

public class CNDensityFunctions {
    public static final class Irradiated {
        public static final ResourceKey<DensityFunction> EROSION = createKey("irradiated/erosion");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("irradiated/final_density");
    }

    private static ResourceKey<DensityFunction> createKey(String id) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, CreateNuclear.asResource(id));
    }

    // Same expression for both slots: EROSION and FINAL_DENSITY play different roles in the
    // noise router (vanilla's are never identical), but nothing in the codebase currently gives
    // either one distinct shaping - CNNoiseData.EROSION (a separate NoiseParameters registration)
    // is only consumed by IrradiatedSurfaceRules, not here. Kept as one shared expression until
    // the worldgen is tuned to actually differentiate them.
    private static DensityFunction irradiatedBaseDensity() {
        return DensityFunctions.add(
                DensityFunctions.yClampedGradient(0, 90, 1, -1),
                BlendedNoise.createUnseeded(0.25, 0.375, 80.0, 160.0, 8.0)
        );
    }

    public static void bootstrapRegistries(BootstrapContext<DensityFunction> context) {
        context.register(Irradiated.EROSION, irradiatedBaseDensity());
        context.register(Irradiated.FINAL_DENSITY, irradiatedBaseDensity());
    }

    private static DensityFunction registerAndWrap(BootstrapContext<DensityFunction> context, ResourceKey<DensityFunction> key, DensityFunction densityFunction) {
        return new DensityFunctions.HolderHolder(context.register(key, densityFunction));
    }

    public static DensityFunction getFunction(HolderGetter<DensityFunction> densityFunctions, ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(key));
    }
}