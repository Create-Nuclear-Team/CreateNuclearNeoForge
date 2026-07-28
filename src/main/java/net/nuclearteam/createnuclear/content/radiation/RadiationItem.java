package net.nuclearteam.createnuclear.content.radiation;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.CNEffects;
import net.nuclearteam.createnuclear.api.radiation.IRadiationSource;

public class RadiationItem extends Item implements IRadiationSource {

    private final double radiation;

    public RadiationItem(Item.Properties settings, double radiation) {
        super(settings);
        this.radiation = radiation;
    }

    @Override
    public double getRadiation(ItemStack stack, LivingEntity entity) {
        return this.radiation * stack.getCount();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            if (this.radiation > 0) {
                livingEntity.addEffect(new MobEffectInstance(CNEffects.RADIATION.getDelegate(), 200, 0, false, true, true));
            }
        }
    }
}