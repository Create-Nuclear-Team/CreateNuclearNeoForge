package net.nuclearteam.createnuclear.foundation.data;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.Tags;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.biome.BiomeIrradiationExtractorItem;
import net.nuclearteam.createnuclear.foundation.data.recipe.CNMaterialTags;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class CNBuilderTransformers {

    /**
     * Generates the item model for a dyeable anti-radiation armor piece.
     * <p>
     * The undyed ("default") piece uses a flat 2D icon ({@code item/generated} with the
     * {@code item/armors/default_anti_radiation_<slot>} sprite) instead of the 3D Blockbench
     * model, since the dropped/inventory 3D rendering was problematic. The worn armor on the
     * body is unaffected (handled by {@code AntiRadiationArmorModel}).
     * <p>
     * One model override per {@link DyeColor} is added, driven by the client-side
     * {@code createnuclear:cloth_color} item property (see {@code CreateNuclearClient}). Each
     * per-color child model still re-textures the 3D Blockbench geometry ({@code item/<name>/item})
     * with the matching {@code item/armors/<color>_anti_radiation_suit} sheet.
     * <p>
     * The texture keys differ per slot: the helmet geometry uses {@code layer0}/{@code particle},
     * the other pieces use {@code 14} — hence {@code textureKeys} is passed explicitly.
     */
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> coloredArmorModel(String slot, String... textureKeys) {
        return (c, p) -> {
            ResourceLocation baseParent = p.modLoc("item/" + c.getName() + "/item");
            ItemModelBuilder outer = p.generated(c, CreateNuclear.asResource("item/armors/default_anti_radiation_" + slot));
            // DyeColor.values() is ordered by id (0..15); overrides must be ascending by predicate
            // value so vanilla override resolution selects the exact color (returns last match <= value).cloth
            // The cloth_color property is clamped to [0,1] (see CreateNuclearClient), hence the
            // (id+1)/16 normalization here must match the value returned there exactly.
            for (DyeColor color : DyeColor.values()) {
                String colorName = color.getSerializedName();
                ItemModelBuilder child = p.withExistingParent("item/colored/" + colorName + "_anti_radiation_" + slot, baseParent);
                for (String key : textureKeys) {
                    child.texture(key, "models/armor/" + colorName + "_anti_radiation_suit");
                }
                outer.override()
                        .predicate(CreateNuclear.asResource("cloth_color"), (color.getId() + 1) / 16f)
                        .model(child)
                        .end();
            }
        };
    }

    /**
     * Generates the per-tier model overrides for the biome restore cell, driven by the
     * client-side {@code createnuclear:charge} item property (see {@code CreateNuclearClient}).
     * Overrides must stay ascending by predicate value (vanilla resolves to the last match <= value),
     * and the thresholds here must match the ratio returned by {@code biomeRestoreItemProperties}.
     */
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> biomeRestoreModel() {
        return (c, p) -> {
            ItemModelBuilder outer = p.generated(c, CreateNuclear.asResource("item/biome_irradiation_extractor/empty"));

            record Tier(String name, float threshold) {}
            List<Tier> tiers = List.of(
                    new Tier("quarter", 0.25f),
                    new Tier("half", 0.5f),
                    new Tier("three_quarters", 0.75f),
                    new Tier("full", 1.0f)
            );

            for (Tier tier : tiers) {
                ItemModelBuilder child = p.withExistingParent("item/biome_irradiation_extractor/" + tier.name(), p.mcLoc("item/generated"))
                    .texture("layer0", CreateNuclear.asResource("item/biome_irradiation_extractor/" + tier.name()));

                outer.override()
                    .predicate(CreateNuclear.asResource(BiomeIrradiationExtractorItem.TAG), tier.threshold())
                    .model(child)
                    .end();
            }
        };
    }

    /**
     * Shapeless "decompacting" recipe (storage block/ingot → 9 of this item), the pattern
     * repeated for every raw material/ingot/nugget that has a coarser source to decompact.
     */
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateRecipeProvider> decompactingRecipe(String unlockedByName, TagKey<Item> source) {
        return (c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 9)
            .unlockedBy(unlockedByName, RegistrateRecipeProvider.has(source))
            .requires(source)
            .save(p, CreateNuclear.asResource("crafting/" + c.getName() + "_from_decompacting"));
    }

    public static ItemEntry<DeferredSpawnEggItem> spawnEgg(String name, Supplier<? extends EntityType<? extends Mob>> entity, int backgroundColor, int highlightColor, String nameItems) {
        return CreateNuclear.REGISTRATE
                .item(name, p -> new DeferredSpawnEggItem(entity, backgroundColor, highlightColor, p))
                .lang(nameItems)
                .model((c, p) -> p.withExistingParent(c.getName(), ResourceLocation.parse("item/template_spawn_egg")))
                .register();
    }

    /**
     * Shared block-side setup (properties, pickaxe requirement, loot table, block tags) for an
     * ore that drops a random count of an item scaled by a uniform Fortune bonus - i.e. every
     * ore in the mod except nitrate (see {@link #oreBlocksSingleDrop}). {@code propsFunc} may be
     * {@code null} when the block needs no extra {@code properties(...)} call (e.g. no light
     * emission), matching blocks that previously skipped that call entirely.
     */
    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> oreBlocks(
            NonNullSupplier<? extends Block> initialProperties,
            @Nullable NonNullUnaryOperator<BlockBehaviour.Properties> propsFunc,
            ItemLike lootReturn,
            float minItem, float maxItem,
            int bonusMultiplier,
            boolean needsDiamondTool,
            boolean isDeepslate,
            CNMaterialTags materialTags
    ) {
        return builder -> {
            BlockBuilder<B, P> withProps = builder.initialProperties(initialProperties);
            if (propsFunc != null) {
                withProps = withProps.properties(propsFunc);
            }
            BlockBuilder<B, P> withLoot = withProps
                .transform(pickaxeOnly())
                .loot((lt, b) -> lt.add(b,
                    lt.createSilkTouchDispatchTable(b,
                        lt.applyExplosionDecay(b, LootItem.lootTableItem(lootReturn)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(minItem, maxItem)))
                            .apply(ApplyBonusCount.addUniformBonusCount(lt.getRegistries().holderOrThrow(Enchantments.FORTUNE), bonusMultiplier))))));
            return applyOreTags(withLoot, needsDiamondTool, isDeepslate, materialTags);
        };
    }

    /**
     * Same as {@link #oreBlocks} but for an ore that always drops a single item, scaled only by
     * {@link ApplyBonusCount#addOreBonusCount} (nitrate ore, both variants) rather than a random
     * count range with a uniform Fortune bonus.
     */
    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> oreBlocksSingleDrop(
            NonNullSupplier<? extends Block> initialProperties,
            ItemLike lootReturn,
            boolean needsDiamondTool,
            boolean isDeepslate,
            CNMaterialTags materialTags
    ) {
        return builder -> {
            BlockBuilder<B, P> withLoot = builder
                .initialProperties(initialProperties)
                .transform(pickaxeOnly())
                .loot((lt, b) -> lt.add(b,
                    lt.createSilkTouchDispatchTable(b,
                        lt.applyExplosionDecay(b, LootItem.lootTableItem(lootReturn)
                            .apply(ApplyBonusCount.addOreBonusCount(lt.getRegistries().holderOrThrow(Enchantments.FORTUNE)))))));
            return applyOreTags(withLoot, needsDiamondTool, isDeepslate, materialTags);
        };
    }

    private static <B extends Block, P> BlockBuilder<B, P> applyOreTags(BlockBuilder<B, P> builder, boolean needsDiamondTool, boolean isDeepslate, CNMaterialTags materialTags) {
        TagKey<Block> tagGround = isDeepslate ? Tags.Blocks.ORES_IN_GROUND_DEEPSLATE : Tags.Blocks.ORES_IN_GROUND_STONE;
        return needsDiamondTool
            ? builder.tag(BlockTags.NEEDS_DIAMOND_TOOL, BlockTags.NEEDS_IRON_TOOL, Tags.Blocks.ORES, tagGround, materialTags.ores().blocks())
            : builder.tag(BlockTags.NEEDS_IRON_TOOL, Tags.Blocks.ORES, tagGround, materialTags.ores().blocks());
    }
}
