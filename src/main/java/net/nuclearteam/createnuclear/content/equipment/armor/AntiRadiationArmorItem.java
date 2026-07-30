package net.nuclearteam.createnuclear.content.equipment.armor;

import com.simibubi.create.content.equipment.armor.BaseArmorItem;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CNTags.CNItemTags;
import net.nuclearteam.createnuclear.CreateNuclear;

import java.util.Locale;

@SuppressWarnings("unused")
public class AntiRadiationArmorItem {

    public static final ArmorItem.Type HELMET = ArmorItem.Type.HELMET;
    public static final ArmorItem.Type CHESTPLATE = ArmorItem.Type.CHESTPLATE;
    public static final ArmorItem.Type LEGGINGS = ArmorItem.Type.LEGGINGS;
    public static final ArmorItem.Type BOOTS = ArmorItem.Type.BOOTS;
    public static final Holder<ArmorMaterial> ARMOR_MATERIAL = CNArmorMaterials.ANTI_RADIATION_SUIT;


    public static class Helmet extends BaseArmorItem {
        protected final DyeColor color;

        public Helmet(Properties properties, DyeColor color) {
            super(
                    CNArmorMaterials.ANTI_RADIATION_SUIT,
                    HELMET,
                    properties,
                    CreateNuclear.asResource(String.format(Locale.ROOT, "%s_anti_radiation_suit", color.getName()))
            );
            this.color = color;
        }

        public static TagKey<Item> getHelmetTag(String key) {
            return key.equals("white")
                    ? CNItemTags.ANTI_RADIATION_ARMOR.tag
                    : CNItemTags.ANTI_RADIATION_HELMET_DYE.tag;
        }
    }


    public static class Chestplate extends BaseArmorItem {
        protected final DyeColor color;

        public Chestplate(Properties properties, DyeColor color) {
            super(
                    CNArmorMaterials.ANTI_RADIATION_SUIT,
                    CHESTPLATE,
                    properties,
                    CreateNuclear.asResource(String.format(Locale.ROOT, "%s_anti_radiation_suit", color.getName()))
            );
            this.color = color;

        }

        public static TagKey<Item> getChestplateTag(String key) {
            return key.equals("white")
                    ? CNItemTags.ANTI_RADIATION_ARMOR.tag
                    : CNItemTags.ANTI_RADIATION_CHESTPLATE_DYE.tag;
        }
    }

    public static class Leggings extends BaseArmorItem {
        protected final DyeColor color;

        public Leggings(Properties properties, DyeColor color) {
            super(
                    CNArmorMaterials.ANTI_RADIATION_SUIT,
                    LEGGINGS,
                    properties,
                    CreateNuclear.asResource(String.format(Locale.ROOT, "%s_anti_radiation_suit", color.getName()))
            );
            this.color = color;

        }


        public static TagKey<Item> getLeggingsTag(String key) {
            return key.equals("white")
                    ? CNItemTags.ANTI_RADIATION_ARMOR.tag
                    : CNItemTags.ANTI_RADIATION_LEGGINGS_DYE.tag;
        }
    }

    public static class Boot extends BaseArmorItem {
        protected final DyeColor color;

        public Boot(Properties properties, DyeColor color) {
            super(
                    CNArmorMaterials.ANTI_RADIATION_SUIT,
                    BOOTS,
                    properties,
                    CreateNuclear.asResource(String.format(Locale.ROOT, "%s_anti_radiation_suit", color.getName()))
            );
            this.color = color;
        }
    }

    public static boolean isArmored(ItemStack item) {
        return item.is(CNItems.ANTI_RADIATION_HELMETS.get())
                || item.is(CNItems.ANTI_RADIATION_CHESTPLATES.get())
                || item.is(CNItems.ANTI_RADIATION_LEGGINGS.get())
                || item.is(CNItems.ANTI_RADIATION_BOOTS.get());
    }
}
