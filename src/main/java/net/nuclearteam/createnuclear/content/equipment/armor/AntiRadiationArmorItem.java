package net.nuclearteam.createnuclear.content.equipment.armor;

import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.nuclearteam.createnuclear.CNAttributes;
import net.nuclearteam.createnuclear.CNDataComponents;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.equipment.cloth.ClothItem.Cloths;

import net.minecraft.world.level.Level;


@SuppressWarnings("unused")
@MethodsReturnNonnullByDefault
public class AntiRadiationArmorItem extends ArmorItem {
    public static final double RADIATION_VALUE = 0.25;

    public AntiRadiationArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return super.getDefaultAttributeModifiers().withModifierAdded(
            CNAttributes.IRRADIATED_RESISTANCE,
            // The id must be unique per slot: modifiers are keyed by ResourceLocation, so sharing one id
            // between the 4 pieces would make them overwrite each other instead of stacking to 1.0.
            new AttributeModifier(CreateNuclear.asResource("armor_resistance_irradiation_" + this.getType().getName()), RADIATION_VALUE, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.bySlot(this.getType().getSlot())
        );
    }


    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (!stack.has(CNDataComponents.CLOTH_COLOR)) return;
//        if (CNAdvancement.DYE_ANTI_RADIATION_ARMOR.isAlreadyAwardedTo(player)) return;
//        CNAdvancement.DYE_ANTI_RADIATION_ARMOR.awardTo(player);
    }

    public static <T extends Item, P>NonNullUnaryOperator<ItemBuilder<T, P>> setColorComponent(Cloths cloths) {
        return b -> b
            .properties(p -> p
                .component(CNDataComponents.CLOTH_COLOR, Cloths.DEFAULT)
            );
    }

    public static class Helmet extends AntiRadiationArmorItem implements IGoggleHelmet {
        public Helmet(Properties p) {
            super(ArmorMaterials.ANTI_RADIATION_SUIT, Type.HELMET, p);
        }
    }

    public static class Chestplate extends AntiRadiationArmorItem {
        public Chestplate(Properties p) {
            super(ArmorMaterials.ANTI_RADIATION_SUIT, Type.CHESTPLATE, p);
        }
    }

    public static class Leggings extends AntiRadiationArmorItem {
        public Leggings(Properties p) {
            super(ArmorMaterials.ANTI_RADIATION_SUIT, Type.LEGGINGS, p);
        }
    }

    public static class Boot extends AntiRadiationArmorItem {
        public Boot(Properties p) {
            super(ArmorMaterials.ANTI_RADIATION_SUIT, Type.BOOTS, p);
        }
    }

    public interface IGoggleHelmet {
        static boolean isGoggleHelmet(LivingEntity entity) {
            ItemStack headSlot = entity.getItemBySlot(EquipmentSlot.HEAD);
            return CNItems.ANTI_RADIATION_HELMETS.isIn(headSlot);
        }
    }
}
