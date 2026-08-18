package net.nuclearteam.createnuclear.infrastructure.worldgen.biome.surfacerule;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.nuclearteam.createnuclear.CreateNuclear;
import org.jetbrains.annotations.NotNull;

public record BiomeTagRule(@NotNull TagKey<Biome> tag) implements SurfaceRules.ConditionSource {
    public static final MapCodec<BiomeTagRule> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(TagKey.codec(Registries.BIOME).fieldOf("tag").forGetter(rule -> rule.tag)).apply(instance, BiomeTagRule::new));

    /**
     * A custom {@link SurfaceRules.ConditionSource} is only usable once its codec lives in the
     * {@code MATERIAL_CONDITION} dispatch registry; without this, (de)serialising any rule that
     * contains a BiomeTagRule fails with "Unregistered type in dispatch codec".
     */
    public static void register(@NotNull RegisterEvent event) {
        event.register(Registries.MATERIAL_CONDITION, helper ->
            helper.register(CreateNuclear.asResource("biome_tag"), CODEC));
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return KeyDispatchDataCodec.of(BiomeTagRule.CODEC);
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        return new Predicate(context);
    }

    private class Predicate extends SurfaceRules.LazyYCondition {
        protected Predicate(SurfaceRules.Context context) {
            super(context);
        }

        @Override
        protected boolean compute() {
            return this.context.biome.get().is(BiomeTagRule.this.tag);
        }
    }
}