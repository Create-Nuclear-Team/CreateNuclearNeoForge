package net.nuclearteam.createnuclear.foundation.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.CompactingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.nuclearteam.createnuclear.CNFluids;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CNTags;
import net.nuclearteam.createnuclear.CreateNuclear;

import java.util.concurrent.CompletableFuture;

public class CNCompactingRecipeGen extends CompactingRecipeGen {
    GeneratedRecipe
        YELLOWCAKE = create(CreateNuclear.asResource("uranium_fluid_to_yellowcake"), b -> b
            .require(CNFluids.URANIUM.get(), 100)
            .output(CNItems.YELLOWCAKE, 1)
        ),
        THORIUM = create(CreateNuclear.asResource("thorium_fluid_to_thorium_ingot"), b -> b
                .require(CNTags.CNFluidTags.THORIUM.tag, 200)
                .output(CNItems.THORIUM_INGOT, 1)
        )
    ;


    public CNCompactingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateNuclear.MOD_ID);
    }

    @Override
    protected AllRecipeTypes getRecipeType() {
        return AllRecipeTypes.COMPACTING;
    }
}
