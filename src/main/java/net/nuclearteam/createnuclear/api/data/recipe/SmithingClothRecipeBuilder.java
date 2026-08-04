package net.nuclearteam.createnuclear.api.data.recipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

import java.util.LinkedHashMap;
import java.util.Map;

public class SmithingClothRecipeBuilder {
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final RecipeCategory category;
    private final ItemStack result;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    public SmithingClothRecipeBuilder(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, RecipeCategory pCategory, ItemStack pResult) {
        this.category = pCategory;
        this.template = pTemplate;
        this.base = pBase;
        this.addition = pAddition;
        this.result = pResult;
    }

    public static SmithingClothRecipeBuilder smithingCloth(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, RecipeCategory pCategory, ItemStack pResult) {
        return new SmithingClothRecipeBuilder(pTemplate, pBase, pAddition, pCategory, pResult);
    }

    public SmithingClothRecipeBuilder unlocks(String pKey, Criterion<?> pCriterion) {
        this.criteria.put(pKey, pCriterion);
        return this;
    }

    public void save(RecipeOutput pRecipeOutput, ResourceLocation pLocation) {
        this.ensureValid(pLocation);
        Advancement.Builder advancement = pRecipeOutput.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pLocation))
                .rewards(AdvancementRewards.Builder.recipe(pLocation))
                .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement::addCriterion);
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(this.template, this.base, this.addition, this.result);
        pRecipeOutput.accept(pLocation, recipe, advancement.build(pLocation.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }

    private void ensureValid(ResourceLocation pLocation) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pLocation);
        }
    }
}
