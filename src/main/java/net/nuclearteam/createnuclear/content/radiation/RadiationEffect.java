package net.nuclearteam.createnuclear.content.radiation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.effects.VicinityEffect;
import net.nuclearteam.createnuclear.content.radiation.capability.RadiationCapability;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.nuclearteam.createnuclear.foundation.damagesTypes.CNDamageSources;

public class RadiationEffect extends VicinityEffect {
    private static final int CONTAGION_DURATION_TICKS = 300;

    /**
     * Constructs the RadiationEffect with harmful category and color.
     * Also applies attribute modifiers to reduce speed, attack damage, and attack speed.
     */
    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 15453236,
            amplifier -> 10,
            RadiationCapability::canBeIrradiated); // Custom color (hex value)

        // Reduces movement speed by 20%
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            ResourceLocation.fromNamespaceAndPath(CreateNuclear.MOD_ID, "radiation_movement_speed"),
            -0.2D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        // Reduces attack damage by 20%
        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            ResourceLocation.fromNamespaceAndPath(CreateNuclear.MOD_ID, "radiation_attack_damage"),
            -0.2D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        // Reduces attack speed by 20%
        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            ResourceLocation.fromNamespaceAndPath(CreateNuclear.MOD_ID, "radiation_attack_speed"),
            -0.2D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    protected void onContaminate(LivingEntity nearby) {
        RadiationCapability.applyContagion(nearby, 15D, CONTAGION_DURATION_TICKS);
    }

    /**
     * Applies the radiation effect to the entity.
     * - Does nothing if the entity is immune via tag.
     * - Skips damage if the entity is fully protected (anti-radiation armor, iodine effect, ...).
     * - Otherwise, applies magic damage based on the amplifier.
     *
     * @param livingEntity The affected living entity.
     * @param amplifier    The strength (level) of the effect.
     */
    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        super.applyEffectTick(livingEntity, amplifier);

        double resistance = RadiationCapability.getRadiationResistance(livingEntity);

        float damage = (float) ((1 << amplifier) * (1.0 - resistance));

        if (damage > 0f) {
            livingEntity.hurt(CNDamageSources.radiation(livingEntity.level()), damage);
        }

        return true;
    }
}
