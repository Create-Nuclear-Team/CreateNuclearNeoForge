package net.nuclearteam.createnuclear.foundation.mixin.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.nuclearteam.createnuclear.CNDataComponents;
import net.nuclearteam.createnuclear.content.equipment.armor.AntiRadiationArmorItem;
import net.nuclearteam.createnuclear.foundation.utility.ClothTagHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientHooks.class)
public class AntiRadiationArmorTextureMixin {
    @Inject(
            method = "getArmorTexture(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ArmorMaterial$Layer;ZLnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void CN$overrideClothTexture(Entity entity, ItemStack stack, ArmorMaterial.Layer layer,
                                                boolean innerModel, EquipmentSlot slot,
                                                CallbackInfoReturnable<ResourceLocation> cir) {
        if (stack.getItem() instanceof AntiRadiationArmorItem && stack.has(CNDataComponents.CLOTH_COLOR)) {
            cir.setReturnValue(ClothTagHelper.getArmorTexturePath(stack, "anti_radiation_suit.png"));
        }
    }
}
