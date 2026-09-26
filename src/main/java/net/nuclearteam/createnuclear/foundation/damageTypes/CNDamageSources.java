package net.nuclearteam.createnuclear.foundation.damageTypes;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.nuclearteam.createnuclear.CNDamageTypes;

public class CNDamageSources {
    public static DamageSource radiation(Level level) {
        return source(CNDamageTypes.RADIATION, level);
    }
    public static DamageSource fanRadiation(Level level) {
        return source(CNDamageTypes.FAN_RADIATION, level);
    }

    private static DamageSource source(ResourceKey<DamageType> key, LevelReader level) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolderOrThrow(key));
    }
}
