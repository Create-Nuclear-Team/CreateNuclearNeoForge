package net.nuclearteam.createnuclear.content.radiation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.nuclearteam.createnuclear.CNEffects;
import net.nuclearteam.createnuclear.CNTags;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.radiation.capability.RadiationCapability;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class RadiationEffect extends MobEffect {

    /**
     * Constructs the RadiationEffect with harmful category and color.
     * Also applies attribute modifiers to reduce speed, attack damage, and attack speed.
     */
    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 15453236); // Custom color (hex value)

        // Reduces movement speed by 20%
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(CreateNuclear.MOD_ID, "radiation_movement_speed"), -0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // Reduces attack damage by 20%
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                ResourceLocation.fromNamespaceAndPath(CreateNuclear.MOD_ID, "radiation_attack_damage"), -0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // Reduces attack speed by 20%
        this.addAttributeModifier(Attributes.ATTACK_SPEED,
                ResourceLocation.fromNamespaceAndPath(CreateNuclear.MOD_ID, "radiation_attack_speed"), -0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
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
        // If the entity is immune to radiation, remove the effect
        if (livingEntity.getType().is(CNTags.CNEntityTags.IRRADIATED_IMMUNE.tag)) {
            livingEntity.removeEffect(CNEffects.RADIATION);
            return true;
        }

        // Full irradiated resistance (anti-radiation armor, iodine, ...) cancels the effect entirely,
        // whatever its source (radiation dose, potion, explosion).
        if (RadiationCapability.getRadiationResistance(livingEntity) >= 1.0) {
            livingEntity.removeEffect(CNEffects.RADIATION);
            return true;
        }

        // Apply radiation damage (magic type), scaled by amplifier
        int damage = 1 << amplifier;
        livingEntity.hurt(livingEntity.damageSources().magic(), damage);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration >= 1;
    }
}