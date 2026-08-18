package net.nuclearteam.createnuclear.foundation.data.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.CrushingRecipeGen;
import com.simibubi.create.content.decoration.palettes.AllPaletteStoneTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.nuclearteam.createnuclear.CNBlocks;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CreateNuclear;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class CNCrushingRecipeGen extends CrushingRecipeGen {

    GeneratedRecipe
        COAL_DUST = create("coal", b -> b
            .duration(250)
            .require(ItemTags.COALS)
            .output(.50f, CNItems.COAL_DUST)
        ),

        GRANITE_URANIUM_POWDER = create(() -> Items.GRANITE, b -> b.duration(250)
            .output(.5f, CNItems.URANIUM_POWDER)
            .output(1f, Blocks.RED_SAND)
        ),

        CRUSHED_URANIUM_POWDER = create(CreateNuclear.MOD_ID, () -> AllItems.CRUSHED_URANIUM::get, b -> b
                .duration(255)
                .output(1, CNItems.URANIUM_POWDER, 9)
        ),


        RAW_URANIUM_BLOCK = create(() -> CNBlocks.RAW_URANIUM_BLOCK, b -> b
            .duration(250)
            .output(1, CNItems.URANIUM_POWDER,81)
        ),

        RAW_THORIUM_BLOCK = create(() -> CNBlocks.RAW_THORIUM_BLOCK, b -> b
                .duration(250)
                .output(1, CNItems.THORIUM_DUST, 9)
                .output(0.5f, CNItems.THORIUM_DUST, 72)
        ),

        RAW_THORIUM_ITEM = create(() -> CNItems.RAW_THORIUM, b -> b
                .duration(125)
                .output(1, CNItems.THORIUM_DUST, 1)
                .output(0.5f, CNItems.THORIUM_DUST, 8)
        ),

        RAW_ZINC = create(() -> AllItems.RAW_ZINC, b -> b.duration(250)
            .output(1, AllItems.CRUSHED_ZINC, 1)
            .output(.75f, AllItems.EXP_NUGGET, 1)
            .output(.25f, CNItems.LEAD_NUGGET,1)
        ),


        RAW_COPPER = create(() -> Items.RAW_COPPER, b -> b.duration(250)
            .output(1, AllItems.CRUSHED_COPPER, 1)
            .output(.75f, AllItems.EXP_NUGGET, 1)
            .output(.15f, CNItems.LEAD_NUGGET,1)
        ),

         NITRATE = create("nitrate", b -> b
            .require(AllPaletteStoneTypes.LIMESTONE.materialTag)
            .duration(250)
                .output(.6f, CNItems.NITRATE, 1)
                .output(.4f, CNItems.LEAD_NUGGET, 1)
        )
    ;

    public CNCrushingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateNuclear.MOD_ID);
    }

    @Override
    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient,
                                     UnaryOperator<StandardProcessingRecipe.Builder<CrushingRecipe>> transform) {
        return create(CreateNuclear.MOD_ID, singleIngredient, transform);
    }
}
