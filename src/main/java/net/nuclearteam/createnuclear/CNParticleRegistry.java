package net.nuclearteam.createnuclear;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CNParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> DEF_REG;

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_MUSHROOM_CLOUD;
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_MUSHROOM_CLOUD_SMOKE;
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_MUSHROOM_CLOUD_EXPLOSION;

    static {
        DEF_REG = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, CreateNuclear.MOD_ID);
        NUCLEAR_MUSHROOM_CLOUD = DEF_REG.register("nuclear_mushroom_cloud", () -> new SimpleParticleType(false));
        NUCLEAR_MUSHROOM_CLOUD_SMOKE = DEF_REG.register("nuclear_mushroom_cloud_smoke", () -> new SimpleParticleType(false));
        NUCLEAR_MUSHROOM_CLOUD_EXPLOSION = DEF_REG.register("nuclear_mushroom_cloud_explosion", () -> new SimpleParticleType(false));
    }

}
