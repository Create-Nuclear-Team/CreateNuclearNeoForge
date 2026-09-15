package net.nuclearteam.createnuclear.content.equipment.armor;

import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CNTags;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nuclearteam.createnuclear.api.data.recipe.SmithingClothRecipeBuilder;
import net.nuclearteam.createnuclear.content.equipment.cloth.ClothItem;
import org.jetbrains.annotations.ApiStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class CNArmorMaterials {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, CreateNuclear.MOD_ID);

    public static final Holder<ArmorMaterial>  ANTI_RADIATION_SUIT = register(
        "anti_radiation_suit",
        new int[]{2, 4, 3, 1, 4 },
        12,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        0.0f,
        0.0f,
        () -> Ingredient.of(CNItems.LEAD_INGOT)
    );

    private static Holder<ArmorMaterial> register(
            String name,
            int[] defense,
            int enchantmentValue,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            Supplier<Ingredient> repairIngredient
    ) {
        List<ArmorMaterial.Layer> list = List.of(new ArmorMaterial.Layer(CreateNuclear.asResource(name)));
        return register(name, defense, enchantmentValue, equipSound, toughness, knockbackResistance, repairIngredient, list);
    }

    private static Holder<ArmorMaterial> register(String name, int[] defense, int enchantmentValue, Holder<SoundEvent> equipSound, float toughness,
                                                  float knockbackResistance,
                                                  Supplier<Ingredient> repairIngredient,
                                                  List<ArmorMaterial.Layer> layers) {
        EnumMap<Type, Integer> enumMap = new EnumMap<>(Type.class);
        for (Type armorItem : Type.values()) {
            enumMap.put(armorItem, defense[armorItem.ordinal()]);
        }

        return ARMOR_MATERIALS.register(name,
                () -> new ArmorMaterial(enumMap, enchantmentValue, equipSound, repairIngredient, layers, toughness, knockbackResistance)
        );
    }

    @ApiStatus.Internal
    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }

    public static <T extends Item, P> NonNullUnaryOperator<ItemBuilder<T, P>> setArmorDurability(Type type, int factor) {
        return b -> b.properties(p -> p.durability(type.getDurability(factor)));
    }

    public static <T extends Item, P> NonNullUnaryOperator<ItemBuilder<T, P>> setArmorDurability(Type type) {
        return setArmorDurability(type, 15);
    }

    /**
     * Must be called from within an item's own {@code .recipe(...)} callback (not as a
     * {@code .transform(...)}) since Registrate keeps only one recipe data generator per entry —
     * registering this via a separate {@code .transform}/{@code .recipe} call would silently
     * overwrite (or be overwritten by) the piece's shaped recipe.
     */
    public static <T extends Item> void registerClothSmithingVariants(DataGenContext<Item, T> c, RegistrateRecipeProvider p) {
        for (ClothItem.Cloths cloth : ClothItem.Cloths.values()) {
            if (cloth == ClothItem.Cloths.DEFAULT) continue;
            SmithingClothRecipeBuilder
                .smithingCloth(
                    Ingredient.EMPTY,
                    Ingredient.of(c.get()),
                    Ingredient.of(cloth.getItem()),
                    RecipeCategory.COMBAT,
                    new ItemStack(c.get())
                )
                .unlocks("has_cloth", RegistrateRecipeProvider.has(CNTags.CNItemTags.CLOTH.tag))
                .save(p, CreateNuclear.asResource("smithing/" + c.getName() + "_" + cloth.getSerializedName()));
        }
    }

}
