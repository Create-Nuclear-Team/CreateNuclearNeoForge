package net.nuclearteam.createnuclear.foundation.data.recipe;

import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.nuclearteam.createnuclear.CNFluids;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CNTags;
import net.nuclearteam.createnuclear.CreateNuclear;

import java.util.concurrent.CompletableFuture;

public class CNMixingRecipeGen extends MixingRecipeGen {

    GeneratedRecipe
        STEEL = create("steel", b -> b
            .require(CNTags.forgeItemTag("dusts/coal"))
            .require(Tags.Items.INGOTS_IRON)
            .output(CNItems.STEEL_INGOT)
        ),

        URANIUM_FLUID = create("uranium_fluid", b -> b
            .require(CNTags.forgeItemTag("dusts/uranium"))
            .output(CNFluids.URANIUM.get(), 25)
        ),

        NITROGEN = create("liquid_nitrogen", b -> b
            .require(CNItems.COOLED_NITROGEN_CONCENTRATE)
            .duration(20)
            .require(Items.ICE)
            .output(CNFluids.LIQUID_NITROGEN.get(), 100)
        )
    ;

    public CNMixingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateNuclear.MOD_ID);
    }

}
