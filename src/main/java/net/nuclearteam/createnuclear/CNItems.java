package net.nuclearteam.createnuclear;

import static net.nuclearteam.createnuclear.CNTags.CNItemTags;
import static net.nuclearteam.createnuclear.content.equipment.armor.AntiRadiationArmorItem.setColorComponent;
import static net.nuclearteam.createnuclear.foundation.data.CNBuilderTransformers.*;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.Tags;
import net.nuclearteam.createnuclear.api.ItemRodTypesValue;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType;
import net.nuclearteam.createnuclear.content.biome.BiomeIrradiationExtractorItem;
import net.nuclearteam.createnuclear.content.equipment.armor.AntiRadiationArmorItem.Helmet;
import net.nuclearteam.createnuclear.content.equipment.armor.AntiRadiationArmorItem.Chestplate;
import net.nuclearteam.createnuclear.content.equipment.armor.AntiRadiationArmorItem.Leggings;
import net.nuclearteam.createnuclear.content.equipment.armor.AntiRadiationArmorItem.Boot;
import net.nuclearteam.createnuclear.content.equipment.armor.CNArmorMaterials;
import net.nuclearteam.createnuclear.foundation.data.recipe.CNMaterialTags;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;
import net.nuclearteam.createnuclear.content.equipment.cloth.ClothItem;
import net.nuclearteam.createnuclear.content.equipment.cloth.ClothItem.Cloths;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintItem;
import net.nuclearteam.createnuclear.content.radiation.RadiationItem;
import net.nuclearteam.createnuclear.foundation.data.CNBuilderTransformers;
import net.nuclearteam.createnuclear.foundation.item.DyedItemsList;
import net.nuclearteam.createnuclear.foundation.utility.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "deprecation"})
public class CNItems {
    static {
        CreateNuclear.REGISTRATE.setCreativeTab(CNCreativeModeTabs.MAIN);
    }

    public static final ItemEntry<RadiationItem>
        YELLOWCAKE = CreateNuclear.REGISTRATE
            .item("yellowcake", p -> new RadiationItem(p, 4))
            .properties(p -> p.food(new FoodProperties.Builder()
                .nutrition(20)
                .saturationModifier(0.3F)
                .alwaysEdible()
                .effect((new MobEffectInstance(CNEffects.RADIATION.getDelegate(),600,2)) , 1.0F)
                .build())
            )
            .register(),

        ENRICHED_YELLOWCAKE = CreateNuclear.REGISTRATE
            .item("enriched_yellowcake", p -> new RadiationItem(p, 2))
            .register(),

        RAW_URANIUM = CreateNuclear.REGISTRATE
            .item("raw_uranium", p -> new RadiationItem(p, 3))
            .tag(CNTags.neoForgeItemTag("raw_ores"), Tags.Items.RAW_MATERIALS, CNMaterialTags.URANIUM.rawMaterials())
            .recipe(decompactingRecipe("has_storage_blocks_raw_uranium", CNMaterialTags.URANIUM.rawStorageBlocks().items()))
            .register(),

        URANIUM_POWDER = CreateNuclear.REGISTRATE
            .item("uranium_powder", p -> new RadiationItem(p, 2))
            .tag(Tags.Items.DUSTS, CNMaterialTags.URANIUM.dusts())
            .register(),

        URANIUM_ROD = CreateNuclear.REGISTRATE
            .item("uranium_rod", p -> new RadiationItem(p, 100))
            .onRegister(ItemRodTypesValue.setRodTypeInfos(new RodType.Builder()
                .baseRodHeat(() -> CNConfigs.server().rods.uraniumBaseValue.get())
                .proximityRodHeat(() -> (float) CNConfigs.server().rods.uraniumProximityBonus.get())
                .rodTimer(() -> CNConfigs.server().rods.uraniumRodLifetime.get())
                .ratio(() -> CNConfigs.server().rods.uraniumHeatRatio.get())
                .fuelRodType()))
            .tag(Tags.Items.RODS, CNItemTags.FUEL.tag)
            .register();
    
    public static final ItemEntry<Item>
        RAW_LEAD = CreateNuclear.REGISTRATE
            .item("raw_lead", Item::new)
            .tag(CNTags.neoForgeItemTag("raw_ores"), Tags.Items.RAW_MATERIALS, CNMaterialTags.LEAD.rawMaterials())
            .recipe(decompactingRecipe("has_storage_blocks_raw_lead", CNMaterialTags.LEAD.rawStorageBlocks().items()))
            .register(),

        STEEL_INGOT = CreateNuclear.REGISTRATE
            .item("steel_ingot", Item::new)
            .tag(Tags.Items.INGOTS, CNMaterialTags.STEEL.ingots())
            .recipe(decompactingRecipe("has_storage_blocks_steel", CNMaterialTags.STEEL.storageBlocks().items()))
            .register(),

        COAL_DUST = CreateNuclear.REGISTRATE
            .item("coal_dust", Item::new)
            .tag(Tags.Items.DUSTS, CNMaterialTags.COAL.dusts())
            .register(),

        GRAPHITE_ROD = CreateNuclear.REGISTRATE
            .item("graphite_rod", Item::new)
            .onRegister(ItemRodTypesValue.setRodTypeInfos(new RodType.Builder()
                .baseRodHeat(() -> CNConfigs.server().rods.graphiteBaseValue.get())
                .proximityRodHeat(() -> CNConfigs.server().rods.graphiteProximityMalus.getF())
                .rodTimer(() -> CNConfigs.server().rods.graphiteRodLifetime.get())
                .ratio(() -> CNConfigs.server().rods.graphiteHeatRatio.get())
                .coolerRodType()))
            .tag(Tags.Items.RODS, CNItemTags.COOLER.tag)
            .register(),

        LEAD_INGOT = CreateNuclear.REGISTRATE
            .item("lead_ingot", Item::new)
            .tag(Tags.Items.INGOTS, CNMaterialTags.LEAD.ingots())
            .recipe(decompactingRecipe("has_storage_blocks_lead", CNMaterialTags.LEAD.storageBlocks().items()))
            .register(),

        STEEL_NUGGET = CreateNuclear.REGISTRATE
            .item("steel_nugget", Item::new)
            .tag(Tags.Items.NUGGETS, CNMaterialTags.STEEL.nuggets())
            .recipe(decompactingRecipe("has_storage_blocks_steel_nugget", CNMaterialTags.STEEL.ingots()))
            .register(),

        LEAD_NUGGET = CreateNuclear.REGISTRATE
            .item("lead_nugget", Item::new)
            .tag(Tags.Items.NUGGETS, CNMaterialTags.LEAD.nuggets())
            .recipe(decompactingRecipe("has_storage_blocks_lead_nugget", CNMaterialTags.LEAD.ingots()))
            .register(),

        GRAPHENE = CreateNuclear.REGISTRATE
            .item("graphene", Item::new)
            .register(),

        RAW_THORIUM = CreateNuclear.REGISTRATE
            .item("raw_thorium", Item::new)
            .tag(CNTags.neoForgeItemTag("raw_ores"), Tags.Items.RAW_MATERIALS, CNMaterialTags.THORIUM.rawMaterials())
            .recipe(decompactingRecipe("has_storage_blocks_raw_thorium", CNMaterialTags.THORIUM.rawStorageBlocks().items()))
            .register(),

        THORIUM_DUST = CreateNuclear.REGISTRATE
            .item("thorium_dust", Item::new)
            .tag(Tags.Items.DUSTS, CNMaterialTags.THORIUM.dusts())
            .register(),

        THORIUM_NUGGET = CreateNuclear.REGISTRATE
            .item("thorium_nugget", Item::new)
            .model((c, p) -> p.generated(c, CreateNuclear.asResource("item/thorium_nugget")))
            .tag(Tags.Items.NUGGETS, CNMaterialTags.THORIUM.nuggets())
            .recipe(decompactingRecipe("has_storage_blocks_thorium_nugget", CNMaterialTags.THORIUM.ingots()))
            .register(),

        THORIUM_INGOT = CreateNuclear.REGISTRATE
            .item("thorium_ingot", Item::new)
            .model((c, p) -> p.generated(c, CreateNuclear.asResource("item/thorium_ingot")))
            .tag(Tags.Items.INGOTS, CNMaterialTags.THORIUM.ingots())
            .recipe(decompactingRecipe("has_storage_blocks_thorium", CNMaterialTags.THORIUM.storageBlocks().items()))
            .register(),

        THORIUM_ROD = CreateNuclear.REGISTRATE
            .item("thorium_rod", Item::new)
            .onRegister(ItemRodTypesValue.setRodTypeInfos(new RodType.Builder()
                .baseRodHeat(() -> CNConfigs.server().rods.baseValueThorium.get())
                .proximityRodHeat(() -> (float) CNConfigs.server().rods.thoriumProxyBonus.get())
                .rodTimer(() -> CNConfigs.server().rods.thoriumRodLifetime.get())
                .ratio(() -> CNConfigs.server().rods.thoriumHeatRatio.get())
                .fuelRodType()))
            .tag(Tags.Items.RODS, CNItemTags.FUEL.tag)
            .register(),

        NITRATE = CreateNuclear.REGISTRATE
            .item("nitrate", Item::new)
            .lang("Nitrate")
            .register(),

        NITROGEN_CONCENTRATE = CreateNuclear.REGISTRATE
            .item("nitrogen_concentrate", Item::new)
            .lang("Nitrogen Concentrate")
            .register(),

        COOLED_NITROGEN_CONCENTRATE = CreateNuclear.REGISTRATE
            .item("cooled_nitrogen_concentrate", Item::new)
            .lang("Cooled Nitrogen Concentrate")
            .register()
    ;

    public static final ItemEntry<Helmet> ANTI_RADIATION_HELMETS = CreateNuclear.REGISTRATE
        .item("default_anti_radiation_helmet", Helmet::new)
        .properties(p -> p.stacksTo(1))
        .tag(
            Tags.Items.ARMORS,
            CNTags.neoForgeItemTag("armors/helmets"),
            CNItemTags.ANTI_RADIATION_ARMOR.tag,
            CNItemTags.ANTI_RADIATION_HELMET.tag
        )
        .transform(setColorComponent(Cloths.DEFAULT))
        .transform(CNArmorMaterials.setArmorDurability(ArmorItem.Type.HELMET))
        .recipe((c, p) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, c.get())
                .unlockedBy("has_cloth", RegistrateRecipeProvider.has(CNItemTags.CLOTH.tag))
                .define('X', CNMaterialTags.LEAD.ingots())
                .define('Y', CommonMetal.BRASS.ingots)
                .define('Z', CNBlocks.REINFORCED_GLASS.asItem())
                .pattern("YXY")
                .pattern("XZX")
                .showNotification(true)
                .save(p, CreateNuclear.asResource("crafting/items/armors/" + c.getName()));
            CNArmorMaterials.registerClothSmithingVariants(c, p);
        })
        .lang("Anti Radiation Helmet")
        .model(coloredArmorModel("helmet", "layer0", "particle"))
        .register();

    public static final ItemEntry<Chestplate> ANTI_RADIATION_CHESTPLATES = CreateNuclear.REGISTRATE
            .item("default_anti_radiation_chestplate", Chestplate::new)
            .properties(p -> p.stacksTo(1))
            .tag(
                Tags.Items.ARMORS,
                CNTags.neoForgeItemTag("armors/chestplates"),
                CNTags.CNItemTags.ANTI_RADIATION_ARMOR.tag
            )
            .transform(setColorComponent(Cloths.DEFAULT))
            .transform(CNArmorMaterials.setArmorDurability(ArmorItem.Type.CHESTPLATE))
            .recipe((c, p) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, c.get())
                    .unlockedBy("has_cloth", RegistrateRecipeProvider.has(CNItemTags.CLOTH.tag))
                    .define('X', CNMaterialTags.LEAD.ingots())
                    .define('Y', CommonMetal.BRASS.ingots)
                    .define('Z', CNItems.GRAPHITE_ROD)
                    .pattern("Y Y")
                    .pattern("XXX")
                    .pattern("ZXZ")
                    .showNotification(true)
                    .save(p, CreateNuclear.asResource("crafting/items/armors/" + c.getName()));
                CNArmorMaterials.registerClothSmithingVariants(c, p);
            })
            .lang("Anti Radiation Chestplate")
            .model(coloredArmorModel("chestplate", "14"))
            .register();

    public static final ItemEntry<Leggings> ANTI_RADIATION_LEGGINGS = CreateNuclear.REGISTRATE
            .item("default_anti_radiation_leggings", Leggings::new)
            .properties(p -> p.stacksTo(1))
            .tag(
                Tags.Items.ARMORS,
                CNTags.neoForgeItemTag("armors/leggings"),
                CNTags.CNItemTags.ANTI_RADIATION_ARMOR.tag
            )
            .transform(setColorComponent(Cloths.DEFAULT))
            .transform(CNArmorMaterials.setArmorDurability(ArmorItem.Type.LEGGINGS))
            .recipe((c, p) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, c.get())
                    .unlockedBy("has_cloth", RegistrateRecipeProvider.has(CNItemTags.CLOTH.tag))
                    .define('X', CNMaterialTags.LEAD.ingots())
                    .define('Y', CommonMetal.BRASS.ingots)
                    .pattern("YXY")
                    .pattern("X X")
                    .pattern("Y Y")
                    .showNotification(true)
                    .save(p, CreateNuclear.asResource("crafting/items/armors/" + c.getName()));
                CNArmorMaterials.registerClothSmithingVariants(c, p);
            })
            .lang("Anti Radiation Leggings")
            .model(coloredArmorModel("leggings", "14"))
            .register();

    public static final ItemEntry<Boot> ANTI_RADIATION_BOOTS = CreateNuclear.REGISTRATE
            .item("default_anti_radiation_boots", Boot::new)
            .properties(p -> p.stacksTo(1))
            .tag(
                Tags.Items.ARMORS,
                CNTags.neoForgeItemTag("armors/boots"),
                CNTags.CNItemTags.ANTI_RADIATION_ARMOR.tag
            )
            .transform(setColorComponent(Cloths.DEFAULT))
            .transform(CNArmorMaterials.setArmorDurability(ArmorItem.Type.BOOTS))
            .recipe((c, p) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, c.get())
                    .unlockedBy("has_cloth", RegistrateRecipeProvider.has(CNItemTags.CLOTH.tag))
                    .define('X', CNMaterialTags.LEAD.ingots())
                    .define('Y', CommonMetal.BRASS.ingots)
                    .pattern("Y Y")
                    .pattern("X X")
                    .showNotification(true)
                    .save(p, CreateNuclear.asResource("crafting/items/armors/" + c.getName()));
                CNArmorMaterials.registerClothSmithingVariants(c, p);
            })
            .lang("Anti Radiation Boots")
            .model(coloredArmorModel("boots", "14"))
            .register();

    public static final DyedItemsList<ClothItem> CLOTHS = new DyedItemsList<>(color -> {
        String colorName = color.getSerializedName();
        List<Item> ingredients = new ArrayList<>(Arrays.asList(Items.WHITE_DYE, Items.ORANGE_DYE, Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE, Items.YELLOW_DYE, Items.LIME_DYE, Items.PINK_DYE, Items.GRAY_DYE, Items.LIGHT_GRAY_DYE, Items.CYAN_DYE, Items.PURPLE_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.GREEN_DYE, Items.RED_DYE, Items.BLACK_DYE));

        return CreateNuclear.REGISTRATE.item(colorName+ "_cloth", p -> new ClothItem(p, color))
            .tag(CNItemTags.CLOTH.tag)
            .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, c.get())
                .unlockedBy("has_white_cloth", RegistrateRecipeProvider.has(ClothItem.Cloths.WHITE_CLOTH.getItem()))
                .requires(Ingredient.of(Arrays.stream(DyeColor.values())
                    .filter(o -> o != color)
                    .map(o -> ClothItem.Cloths.getByColor(o).get())
                    .toArray(ClothItem[]::new)
                ))
                .requires(ingredients.get(color.ordinal()))
                .save(p, CreateNuclear.asResource("shapeless/cloth/" + c.getName()))
            )
            .lang(TextUtils.titleCaseConversion(color.getName()) + " Cloth")
            .model((c, p) -> p.generated(c, CreateNuclear.asResource("item/cloth/" + colorName + "_cloth")))
            .register();
    });

    public static final ItemEntry<DeferredSpawnEggItem> SPAWN_WOLF = CNBuilderTransformers.spawnEgg("wolf_irradiated_spawn_egg", CNEntityType.IRRADIATED_WOLF, 0x42452B, 0x4C422B, "Irradiated Wolf Spawn Egg");
    public static final ItemEntry<DeferredSpawnEggItem> SPAWN_CAT = CNBuilderTransformers.spawnEgg("cat_irradiated_spawn_egg", CNEntityType.IRRADIATED_CAT, 0x382C19, 0x742728, "Irradiated Cat Spawn Egg");
    public static final ItemEntry<DeferredSpawnEggItem> SPAWN_CHICKEN = CNBuilderTransformers.spawnEgg("chicken_irradiated_spawn_egg", CNEntityType.IRRADIATED_CHICKEN, 0x6B9455, 0x95393C, "Irradiated Chicken Spawn Egg");

    public static final ItemEntry<ReactorBluePrintItem> REACTOR_BLUEPRINT = CreateNuclear.REGISTRATE
        .item("reactor_blueprint_item", ReactorBluePrintItem::new)
        .lang("Reactor Blueprint")
        .recipe((c, p) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                .unlockedBy("has_reactor_controller", RegistrateRecipeProvider.has(CNBlocks.REACTOR_CONTROLLER.get()))
                .define('S', CNMaterialTags.STEEL.ingots())
                .define('D', AllBlocks.DISPLAY_BOARD)
                .define('P', AllItems.PRECISION_MECHANISM)
                .define('E', AllItems.EMPTY_SCHEMATIC)
                .pattern("SDS")
                .pattern("SPS")
                .pattern("SES")
                .save(p, CreateNuclear.asResource("crafting/" + c.getName()));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get())
                .unlockedBy("has_reactor_blueprint", RegistrateRecipeProvider.has(CNBlocks.REACTOR_CONTROLLER.get()))
                .requires(CNItems.REACTOR_BLUEPRINT)
                .save(p, CreateNuclear.asResource("shapeless/" + c.getName() + "_clear"));
        })
        .model((c, p) -> p.generated(c, CreateNuclear.asResource("item/reactor_blueprint")))
        .properties(p -> p.stacksTo(1))
        .register();

    public static final ItemEntry<Item> REINFORCED_GLASS_BOTTLE = CreateNuclear.REGISTRATE
        .item("reinforced_glass_bottle", Item::new)
        .lang("Reinforced Glass Bottle")
        .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.BREWING, c.get(), 3)
            .unlockedBy("has_reinforced_glass", RegistrateRecipeProvider.has(CNBlocks.REINFORCED_GLASS.get()))
            .define('G', CNBlocks.REINFORCED_GLASS)
            .pattern("G G")
            .pattern(" G ")
            .save(p, CreateNuclear.asResource("crafting/" + c.getName())))
        .properties(p -> p.stacksTo(16))
        .register();

    public static final ItemEntry<BiomeIrradiationExtractorItem> IRRADIATION_BIOME_EXTRACTOR = CreateNuclear.REGISTRATE
        .item("biome_irradiation_extractor", BiomeIrradiationExtractorItem::new)
        .lang("Biome Irradiation Extractor")
        .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 8)
            .unlockedBy("has_reinforced_glass_bottle", RegistrateRecipeProvider.has(CNItems.REINFORCED_GLASS_BOTTLE.get()))
            .define('B', CNItems.REINFORCED_GLASS_BOTTLE)
            .define('S', Items.NETHER_STAR)
            .pattern("BBB")
            .pattern("BSB")
            .pattern("BBB")
            .save(p, CreateNuclear.asResource("crafting/" + c.getName())))
        .properties(p -> p.stacksTo(16).fireResistant().setNoRepair())
        .model(biomeRestoreModel())
        .register();

    public static void register() {}
}
