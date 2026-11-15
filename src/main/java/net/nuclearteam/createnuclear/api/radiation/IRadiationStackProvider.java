package net.nuclearteam.createnuclear.api.radiation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Stack-aware radiation source, registered through {@link RadiationRegistry#registerStackProvider}.
 * <p>
 * Unlike {@link IRadiationSource} (implemented by the item class) or the item/tag values of
 * {@link RadiationRegistry}, a provider can look at the whole stack, including its data
 * components. This is what addons need when radiation is carried by a component placed on
 * an item they don't own (e.g. an irradiated vanilla sword).
 * <p>
 * The returned value is the radiation of the <b>whole stack</b> (multiply by the count
 * yourself if relevant). Return {@code 0} when the stack is not concerned.
 * <p>
 * Player radiation is only recomputed when the inventory hash changes; that hash includes
 * data components, so a provider that only reads components is always up to date.
 */
@FunctionalInterface
public interface IRadiationStackProvider {
    double getRadiation(ItemStack stack, @Nullable LivingEntity entity);
}
