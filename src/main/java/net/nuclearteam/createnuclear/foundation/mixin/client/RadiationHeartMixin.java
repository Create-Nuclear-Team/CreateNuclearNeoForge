package net.nuclearteam.createnuclear.foundation.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.nuclearteam.createnuclear.CNEffects;
import net.nuclearteam.createnuclear.CreateNuclear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public class RadiationHeartMixin {

    @ModifyArg(
            method = "renderHeart",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
            ),
            index = 0
    )
    private ResourceLocation CN$changeHeartTexture(ResourceLocation originalTexture) {
        Player player = Minecraft.getInstance().player;

        if (player != null && player.hasEffect(CNEffects.RADIATION)) {
            String path = originalTexture.getPath();
            if (originalTexture.getNamespace().equals("minecraft") && path.startsWith("hud/heart/")) {
                String heartType = path.substring("hud/heart/".length());
                String baseType = heartType.replace("_blinking", "");
                
                if (baseType.equals("full") || baseType.equals("half") || baseType.equals("hardcore_full") || baseType.equals("hardcore_half")) {
                    return CreateNuclear.asResource("hud/heart/radiation_" + baseType);
                }
            }
        }
        return originalTexture;
    }
}