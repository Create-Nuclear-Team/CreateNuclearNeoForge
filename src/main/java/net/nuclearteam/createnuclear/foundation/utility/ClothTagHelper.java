package net.nuclearteam.createnuclear.foundation.utility;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.nuclearteam.createnuclear.CNDataComponents;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.equipment.cloth.ClothItem;
import net.nuclearteam.createnuclear.content.equipment.cloth.ClothItem.Cloths;

public final class ClothTagHelper {
    private ClothTagHelper() {}

    public static void initClothTagsForResult(ItemStack resultItem, ItemStack baseItem, ItemStack additionItem) {
        if (!resultItem.has(CNDataComponents.CLOTH_COLOR) && baseItem.has(CNDataComponents.CLOTH_COLOR)) {
            resultItem.set(CNDataComponents.CLOTH_COLOR, baseItem.get(CNDataComponents.CLOTH_COLOR));
        }

        if (!resultItem.has(CNDataComponents.CLOTH_ITEM) && baseItem.has(CNDataComponents.CLOTH_ITEM)) {
            resultItem.set(CNDataComponents.CLOTH_ITEM, baseItem.get(CNDataComponents.CLOTH_ITEM));
        }

        if (additionItem != null && additionItem.getItem() instanceof ClothItem clothItem) {
            resultItem.set(CNDataComponents.CLOTH_COLOR, Cloths.of(clothItem.getColor()));
            resultItem.set(CNDataComponents.CLOTH_ITEM, additionItem.copy());
        }
    }

    public static DyeColor getClothColor(ItemStack stack, DyeColor defaultColor) {
        if (stack == null) return defaultColor;
        Cloths cloth = stack.get(CNDataComponents.CLOTH_COLOR);
        DyeColor color = cloth != null ? cloth.getDyeColor() : null;
        return color != null ? color : defaultColor;
    }

    public static ResourceLocation getArmorTexturePath(ItemStack stack, String armorFileName) {
        DyeColor color = getClothColor(stack, null);
        String prefix = color != null ? color.getSerializedName() : "default";
        String textureFile = prefix + "_" + armorFileName;
        return CreateNuclear.asResource("textures/models/armor/" + textureFile);
    }
}
