package net.nuclearteam.createnuclear.foundation.data.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.WashingRecipeGen;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CreateNuclear;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class CNWashingRecipeGen extends WashingRecipeGen {

    GeneratedRecipe
        CRUSHED_LEAD = moddedCrushedOreCustom(AllItems.CRUSHED_LEAD, CNItems.LEAD_NUGGET::get, AllItems.EXP_NUGGET::get, .5f)
    ;

    public GeneratedRecipe moddedCrushedOreCustom(ItemEntry<? extends Item> crushed, Supplier<ItemLike> nugget, Supplier<ItemLike> secondary,
                                            float secondaryChance) {
        return create(crushed::get, b -> b.output(nugget.get(), 9)
                .output(secondaryChance, secondary.get(), 1));
    }


    public CNWashingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateNuclear.MOD_ID);
    }

    @Override
    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient,
                                      UnaryOperator<StandardProcessingRecipe.Builder<SplashingRecipe>> transform) {
        return create(CreateNuclear.MOD_ID, singleIngredient, transform);
    }
}
