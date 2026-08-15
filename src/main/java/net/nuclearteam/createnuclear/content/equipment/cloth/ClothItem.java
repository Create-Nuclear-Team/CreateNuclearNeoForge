package net.nuclearteam.createnuclear.content.equipment.cloth;

import com.mojang.serialization.Codec;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.nuclearteam.createnuclear.CNItems;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings("unused")
public class ClothItem extends Item {
    private final DyeColor color;

    public ClothItem(Item.Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    /**
     * Wraps an ItemStack with content-based equals/hashCode, since ItemStack itself only offers
     * identity equality and NeoForge requires data component values to implement both properly.
     */
    public record ClothItemStack(ItemStack stack) {
        public static final Codec<ClothItemStack> CODEC = ItemStack.CODEC.xmap(ClothItemStack::new, ClothItemStack::stack);
        public static final StreamCodec<RegistryFriendlyByteBuf, ClothItemStack> STREAM_CODEC = ItemStack.STREAM_CODEC.map(ClothItemStack::new, ClothItemStack::stack);

        @Override
        public boolean equals(Object other) {
            return other instanceof ClothItemStack(ItemStack stack1)
                && ItemStack.isSameItemSameComponents(this.stack, stack1);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.stack);
        }
    }

    @MethodsReturnNonnullByDefault
    public enum Cloths implements StringRepresentable {
        WHITE_CLOTH(DyeColor.WHITE),
        YELLOW_CLOTH(DyeColor.YELLOW),
        RED_CLOTH(DyeColor.RED),
        BLUE_CLOTH(DyeColor.BLUE),
        GREEN_CLOTH(DyeColor.GREEN),
        BLACK_CLOTH(DyeColor.BLACK),
        ORANGE_CLOTH(DyeColor.ORANGE),
        PURPLE_CLOTH(DyeColor.PURPLE),
        BROWN_CLOTH(DyeColor.BROWN),
        PINK_CLOTH(DyeColor.PINK),
        CYAN_CLOTH(DyeColor.CYAN),
        LIGHT_GRAY_CLOTH(DyeColor.LIGHT_GRAY),
        GRAY_CLOTH(DyeColor.GRAY),
        LIGHT_BLUE_CLOTH(DyeColor.LIGHT_BLUE),
        LIME_CLOTH(DyeColor.LIME),
        MAGENTA_CLOTH(DyeColor.MAGENTA),
        DEFAULT(null, "default");

        private static Map<DyeColor, ItemEntry<ClothItem>> clothMap;

        private static Map<DyeColor, ItemEntry<ClothItem>> clothMap() {
            if (clothMap == null) {
                clothMap = new EnumMap<>(DyeColor.class);
                for (DyeColor color : DyeColor.values()) {
                    clothMap.put(color, CNItems.CLOTHS.get(color));
                }
            }
            return clothMap;
        }

        @Nullable
        private final DyeColor color;
        private final String serializedName;

        Cloths(DyeColor color) {
            this(color, color.getSerializedName());
        }

        Cloths(@Nullable DyeColor color, String serializedName) {
            this.color = color;
            this.serializedName = serializedName;
        }

        @Nullable
        public ItemEntry<ClothItem> getItem() {
            return color != null ? clothMap().get(color) : null;
        }

        public static ItemEntry<ClothItem> getByColor(DyeColor color) {
            return clothMap().get(color);
        }

        public static Cloths of(@Nullable DyeColor color) {
            if (color == null) return DEFAULT;
            for (Cloths c : values()) {
                if (c.color == color) return c;
            }
            return DEFAULT;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        @Nullable
        public DyeColor getDyeColor() {
            return color;
        }
    }
}