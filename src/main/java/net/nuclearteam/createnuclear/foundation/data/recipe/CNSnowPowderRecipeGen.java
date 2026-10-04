package net.nuclearteam.createnuclear.foundation.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.data.recipe.SnowPowderRecipeGen;

import java.util.concurrent.CompletableFuture;

public class CNSnowPowderRecipeGen extends SnowPowderRecipeGen {

    GeneratedRecipe
        COOLED_NITROGEN_CONCENTRATE = convert(CNItems.NITROGEN_CONCENTRATE, CNItems.COOLED_NITROGEN_CONCENTRATE)
    ;

    public CNSnowPowderRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateNuclear.MOD_ID);
    }

}