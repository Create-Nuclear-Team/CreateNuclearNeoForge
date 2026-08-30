package net.nuclearteam.createnuclear.content.explosion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CNBasicModelPart {
    public float textureWidth;
    public float textureHeight;
    public int textureOffsetX;
    public int textureOffsetY;
    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;
    public boolean mirror;
    public boolean showModel;
    private final ObjectList<CNBasicModelPart> childModels;

    public CNBasicModelPart(CNBasicEntityModel model) {
        this.textureWidth = 64.0F;
        this.textureHeight = 32.0F;
        this.showModel = true;
        this.childModels = new ObjectArrayList();
        this.setTextureSize(model.textureWidth, model.textureHeight);
    }

    public CNBasicModelPart(CNBasicEntityModel model, int texOffX, int texOffY) {
        this(model.textureWidth, model.textureHeight, texOffX, texOffY);
    }

    public CNBasicModelPart(int textureWidthIn, int textureHeightIn, int textureOffsetXIn, int textureOffsetYIn) {
        this.textureWidth = 64.0F;
        this.textureHeight = 32.0F;
        this.showModel = true;
        this.childModels = new ObjectArrayList();
        this.setTextureSize(textureWidthIn, textureHeightIn);
        this.setTextureOffset(textureOffsetXIn, textureOffsetYIn);
    }

    public void addChild(CNBasicModelPart renderer) {
        this.childModels.add(renderer);
    }

    public CNBasicModelPart setTextureOffset(int x, int y) {
        this.textureOffsetX = x;
        this.textureOffsetY = y;
        return this;
    }

    public void setRotationPoint(float rotationPointXIn, float rotationPointYIn, float rotationPointZIn) {
        this.rotationPointX = rotationPointXIn;
        this.rotationPointY = rotationPointYIn;
        this.rotationPointZ = rotationPointZIn;
    }

    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn) {
        this.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (this.showModel && !this.childModels.isEmpty()) {
            matrixStackIn.pushPose();
            this.translateRotate(matrixStackIn);
            ObjectListIterator var9 = this.childModels.iterator();

            while(var9.hasNext()) {
                CNBasicModelPart CNBasicModelPart = (CNBasicModelPart)var9.next();
                CNBasicModelPart.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
            }

            matrixStackIn.popPose();
        }

    }

    public void translateRotate(PoseStack matrixStackIn) {
        matrixStackIn.translate(this.rotationPointX / 16.0F, (double)(this.rotationPointY / 16.0F), (double)(this.rotationPointZ / 16.0F));
        if (this.rotateAngleZ != 0.0F) {
            matrixStackIn.mulPose(Axis.ZP.rotation(this.rotateAngleZ));
        }

        if (this.rotateAngleY != 0.0F) {
            matrixStackIn.mulPose(Axis.YP.rotation(this.rotateAngleY));
        }

        if (this.rotateAngleX != 0.0F) {
            matrixStackIn.mulPose(Axis.XP.rotation(this.rotateAngleX));
        }

    }

    public CNBasicModelPart setTextureSize(int textureWidthIn, int textureHeightIn) {
        this.textureWidth = (float)textureWidthIn;
        this.textureHeight = (float)textureHeightIn;
        return this;
    }
}
