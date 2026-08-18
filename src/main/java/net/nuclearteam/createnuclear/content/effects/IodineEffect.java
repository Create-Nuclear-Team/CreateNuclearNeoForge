package net.nuclearteam.createnuclear.content.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import net.nuclearteam.createnuclear.CNAttributes;
import net.nuclearteam.createnuclear.CreateNuclear;

public class IodineEffect extends MobEffect {
    protected IodineEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);

        this.addAttributeModifier(
            CNAttributes.IRRADIATED_RESISTANCE,
            ResourceLocation.fromNamespaceAndPath(
                    CreateNuclear.MOD_ID,
                    "iodine_effect"
            ),
            1D,
            AttributeModifier.Operation.ADD_VALUE
        );
    }

    public IodineEffect() {
        this(MobEffectCategory.BENEFICIAL, 7328217);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int pDuration, int pAmplifier) {
        return true;
    }
}