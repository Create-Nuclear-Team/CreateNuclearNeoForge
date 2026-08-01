package net.nuclearteam.createnuclear.api.data.recipe;

import com.simibubi.create.api.data.recipe.ProcessingRecipeGen;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.nuclearteam.createnuclear.CNRecipeTypes;

import java.util.concurrent.CompletableFuture;

public abstract class EnrichedRecipeGen extends ProcessingRecipeGen {

    public EnrichedRecipeGen(PackOutput generator, CompletableFuture<HolderLookup.Provider> registries, String defaultNamespace) {
        super(generator, registries, defaultNamespace);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return CNRecipeTypes.ENRICHED;
    }
}
