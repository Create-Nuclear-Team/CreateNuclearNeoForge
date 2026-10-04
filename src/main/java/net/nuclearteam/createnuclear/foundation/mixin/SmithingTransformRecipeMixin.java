package net.nuclearteam.createnuclear.foundation.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.nuclearteam.createnuclear.CNTags;
import net.nuclearteam.createnuclear.foundation.utility.ClothTagHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmithingTransformRecipe.class)
public class SmithingTransformRecipeMixin {

    @Shadow
    @Final
    ItemStack result;

    @Inject(at = @At("HEAD"), method = "assemble", cancellable = true)
    public void CN$assemble(SmithingRecipeInput pInput, HolderLookup.Provider pRegistries, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack baseItem = pInput.base();
        if (baseItem.is(CNTags.CNItemTags.ANTI_RADIATION_ARMOR.tag)) {
            ItemStack resultItem = this.result.copy();
            ItemStack additionItem = pInput.addition();

            ClothTagHelper.initClothTagsForResult(resultItem, baseItem, additionItem);

            cir.setReturnValue(resultItem);
        }
    }
}
