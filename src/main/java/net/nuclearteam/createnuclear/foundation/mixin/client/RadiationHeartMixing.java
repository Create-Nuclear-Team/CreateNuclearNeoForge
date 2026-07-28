/*package net.nuclearteam.createnuclear.foundation.mixin.client;

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
public class RadiationHeartMixing {

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

        // En 1.20.2+, Minecraft utilise des sprites individuels pour chaque type de coeur (ex: "hud/heart/full").
        if (player != null && player.hasEffect(CNEffects.RADIATION)) {
            String path = originalTexture.getPath();
            // On vérifie si la texture d'origine est un coeur vanilla
            if (originalTexture.getNamespace().equals("minecraft") && path.startsWith("hud/heart/")) {
                // On remplace "hud/heart/..." par "hud/heart/radiation_..." ou on utilise un nouveau sprite.
                // Par exemple, si le path est "hud/heart/full", on charge "createnuclear:hud/heart/radiation_full"
                // Assurez-vous de créer ces sprites dans: assets/createnuclear/textures/gui/sprites/hud/heart/
                String heartType = path.substring("hud/heart/".length());
                return CreateNuclear.asResource("hud/heart/radiation_" + heartType);
            }
        }
        return originalTexture;
    }
}*/