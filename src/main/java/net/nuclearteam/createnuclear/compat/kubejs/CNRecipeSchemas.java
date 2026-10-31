package net.nuclearteam.createnuclear.compat.kubejs;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class CNRecipeSchemas {
    /**
     * Schema for Create Nuclear's fan processing recipes (enriched, snow powder),
     * matching Create's processing recipe JSON: {@code results} then {@code ingredients}.
     */
    public static RecipeSchema fanProcessing() {
        RecipeKey<List<ProcessingOutput>> results = ProcessingOutputComponent.TYPE.instance().asListOrSelf().outputKey("results");
        RecipeKey<List<Ingredient>> ingredients = IngredientComponent.INGREDIENT.instance().asListOrSelf().inputKey("ingredients");

        return new RecipeSchema(results, ingredients).uniqueId(results);
    }
}
