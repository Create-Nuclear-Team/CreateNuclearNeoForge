package net.nuclearteam.createnuclear;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;

@OnlyIn(Dist.CLIENT)
public class CNClientProxy {

    public static final ResourceLocation BOMB_FLASH = CreateNuclear.asResource("textures/misc/bomb_flash.png");

    public static int muteNonNukeSoundsFor = 0;
    public static int renderNukeFlashFor = 0;
    public static int renderNukeSkyDarkFor = 0;
    public static float prevNukeFlashAmount = 0;
    public static float nukeFlashAmount = 0;

    public static int lastTremorTick = -1;
    public static float[] randomTremorOffsets = new float[3];

    public static final Int2ObjectMap<AbstractTickableSoundInstance> ENTITY_SOUND_INSTANCE_MAP = new Int2ObjectOpenHashMap<>();

    public static float getNukeFlashAmount(float partialTicks) {
        return prevNukeFlashAmount + (nukeFlashAmount - prevNukeFlashAmount) * partialTicks;
    }

    public static void preScreenRender(float partialTick) {
        float screenEffectIntensity = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float currentNukeFlash = getNukeFlashAmount(partialTick);

        if (currentNukeFlash > 0 && CNConfigs.client().nuclearBombFlash.get()) {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, currentNukeFlash * screenEffectIntensity);
            RenderSystem.setShaderTexture(0, BOMB_FLASH);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.addVertex(0.0F, (float)screenHeight, -90.0F).setUv(0.0F, 1.0F);
            bufferbuilder.addVertex((float)screenWidth, (float)screenHeight, -90.0F).setUv(1.0F, 1.0F);
            bufferbuilder.addVertex((float)screenWidth, 0.0F, -90.0F).setUv(1.0F, 0.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, -90.0F).setUv(0.0F, 0.0F);
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static boolean isFarFromCamera(double x, double y, double z) {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z) >= 256.0D;
    }
}