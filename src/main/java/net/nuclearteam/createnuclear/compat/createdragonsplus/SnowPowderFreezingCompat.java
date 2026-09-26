package net.nuclearteam.createnuclear.compat.createdragonsplus;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.CNRecipeTypes;
import net.nuclearteam.createnuclear.content.kinetics.fan.processing.SnowPowderRecipe;
import plus.dragons.createdragonsplus.common.kinetics.fan.freezing.FreezingRecipe;
import plus.dragons.createdragonsplus.integration.CDPIntegrationContributions;
import plus.dragons.createdragonsplus.integration.CDPIntegrationContributions.StandardFanProcessingCompat;

import java.util.List;
import java.util.Optional;

/**
 * Lets Create Dragons Plus' bulk freezing fan run Create Nuclear's snow powder recipes,
 * since its freezing type takes priority over ours on powder snow.
 */
public class SnowPowderFreezingCompat implements StandardFanProcessingCompat<FreezingRecipe> {
    public static void register() {
        CDPIntegrationContributions.registerFreezingCompat(new SnowPowderFreezingCompat());
    }

    @Override
    public boolean isValidAt(Level level, BlockPos pos) {
        return false;
    }

    @Override
    public boolean canProcess(ItemStack stack, Level level) {
        return findRecipe(stack, level).isPresent();
    }

    @Override
    public Optional<List<ItemStack>> process(ItemStack stack, Level level) {
        return findRecipe(stack, level)
            .map(holder -> RecipeApplier.applyRecipeOn(level, stack, holder.value(), true));
    }

    @Override
    public void gatherJeiRecipes(RecipeManager manager, List<RecipeHolder<FreezingRecipe>> recipes) {
        manager.<SingleRecipeInput, SnowPowderRecipe>getAllRecipesFor(CNRecipeTypes.SNOW_POWDER.getType())
            .forEach(holder -> recipes.add(new RecipeHolder<>(holder.id(), FreezingRecipe.builder(holder.id())
                .withItemIngredients(holder.value().getIngredients())
                .withItemOutputs(holder.value().getRollableResults().toArray(ProcessingOutput[]::new))
                .build())));
    }

    private static Optional<RecipeHolder<SnowPowderRecipe>> findRecipe(ItemStack stack, Level level) {
        return CNRecipeTypes.SNOW_POWDER.find(new SingleRecipeInput(stack), level);
    }
}
