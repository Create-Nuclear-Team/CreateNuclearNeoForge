package net.nuclearteam.createnuclear.content.explosion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class CNAdvancedModelBox extends CNBasicModelPart {
    public float defaultRotationX;
    public float defaultRotationY;
    public float defaultRotationZ;
    public float defaultPositionX;
    public float defaultPositionY;
    public float defaultPositionZ;
    public float scaleX;
    public float scaleY;
    public float scaleZ;
    public int textureOffsetX;
    public int textureOffsetY;
    public boolean scaleChildren;
    private CNAdvancedEntityModel model;
    private CNAdvancedModelBox parent;
    public ObjectList<CNTabulaModelRenderUtils.ModelBox> cubeList;
    public ObjectList<CNBasicModelPart> childModels;
    private float textureWidth;
    private float textureHeight;
    public String boxName;

    public CNAdvancedModelBox(CNAdvancedEntityModel model, String name) {
        super(model);
        this.scaleX = 1.0F;
        this.scaleY = 1.0F;
        this.scaleZ = 1.0F;
        this.boxName = "";
        this.textureWidth = (float)model.texWidth;
        this.textureHeight = (float)model.texHeight;
        this.model = model;
        this.cubeList = new ObjectArrayList();
        this.childModels = new ObjectArrayList();
        this.boxName = name;
    }

    public CNAdvancedModelBox(CNAdvancedEntityModel model) {
        this(model, null);
        this.textureWidth = (float)model.texWidth;
        this.textureHeight = (float)model.texHeight;
        this.cubeList = new ObjectArrayList();
        this.childModels = new ObjectArrayList();
    }

    public CNBasicModelPart addBox(String partName, float x, float y, float z, int width, int height, int depth, float grow, int texOffX, int texOffY) {
        this.setTextureOffset(texOffX, texOffY);
        this.addBox(this.textureOffsetX, this.textureOffsetY, x, y, z, (float)width, (float)height, (float)depth, grow, grow, grow, this.mirror, false);
        return this;
    }

    public CNBasicModelPart addBox(float x, float y, float z, float width, float height, float depth) {
        this.addBox(this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, 0.0F, 0.0F, 0.0F, this.mirror, false);
        return this;
    }

    public CNBasicModelPart addBox(float x, float y, float z, float width, float height, float depth, boolean mirror) {
        this.addBox(this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, 0.0F, 0.0F, 0.0F, mirror, false);
        return this;
    }

    public void addBox(float x, float y, float z, float width, float height, float depth, float grow) {
        this.addBox(this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, grow, grow, grow, this.mirror, false);
    }

    public void addBox(float x, float y, float z, float width, float height, float depth, float growX, float growY, float growZ) {
        this.addBox(this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, growX, growY, growZ, this.mirror, false);
    }

    public void addBox(float x, float y, float z, float width, float height, float depth, float grow, boolean mirror) {
        this.addBox(this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, grow, grow, grow, mirror, false);
    }

    private void addBox(int texOffX, int texOffY, float x, float y, float z, float width, float height, float depth, float growX, float growY, float growZ, boolean mirror, boolean unusedFlag) {
        this.cubeList.add(new CNTabulaModelRenderUtils.ModelBox(texOffX, texOffY, x, y, z, width, height, depth, growX, growY, growZ, mirror, this.textureWidth, this.textureHeight));
    }

    public void setScale(float scaleX, float scaleY, float scaleZ) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    public void updateDefaultPose() {
        this.defaultRotationX = this.rotateAngleX;
        this.defaultRotationY = this.rotateAngleY;
        this.defaultRotationZ = this.rotateAngleZ;
        this.defaultPositionX = this.rotationPointX;
        this.defaultPositionY = this.rotationPointY;
        this.defaultPositionZ = this.rotationPointZ;
    }

    public void resetToDefaultPose() {
        this.rotateAngleX = this.defaultRotationX;
        this.rotateAngleY = this.defaultRotationY;
        this.rotateAngleZ = this.defaultRotationZ;
        this.rotationPointX = this.defaultPositionX;
        this.rotationPointY = this.defaultPositionY;
        this.rotationPointZ = this.defaultPositionZ;
    }

    public void addChild(CNBasicModelPart child) {
        super.addChild(child);
        this.childModels.add(child);
        if (child instanceof CNAdvancedModelBox advancedChild) {
            advancedChild.setParent(this);
        }
    }

    public CNAdvancedModelBox getParent() {
        return this.parent;
    }

    public void setParent(CNAdvancedModelBox parent) {
        this.parent = parent;
    }

    public void translateAndRotate(PoseStack matrixStackIn) {
        matrixStackIn.translate(this.rotationPointX / 16.0F, this.rotationPointY / 16.0F, (double)(this.rotationPointZ / 16.0F));
        if (this.rotateAngleZ != 0.0F) {
            matrixStackIn.mulPose(Axis.ZP.rotation(this.rotateAngleZ));
        }

        if (this.rotateAngleY != 0.0F) {
            matrixStackIn.mulPose(Axis.YP.rotation(this.rotateAngleY));
        }

        if (this.rotateAngleX != 0.0F) {
            matrixStackIn.mulPose(Axis.XP.rotation(this.rotateAngleX));
        }

        matrixStackIn.scale(this.scaleX, this.scaleY, this.scaleZ);
    }

    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (this.showModel && (!this.cubeList.isEmpty() || !this.childModels.isEmpty())) {
            matrixStackIn.pushPose();
            this.translateAndRotate(matrixStackIn);
            this.doRender(matrixStackIn.last(), bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
            ObjectListIterator var9 = this.childModels.iterator();
            if (!this.scaleChildren) {
                matrixStackIn.scale(1.0F / Math.max(this.scaleX, 1.0E-4F), 1.0F / Math.max(this.scaleY, 1.0E-4F), 1.0F / Math.max(this.scaleZ, 1.0E-4F));
            }

            while(var9.hasNext()) {
                CNBasicModelPart child = (CNBasicModelPart)var9.next();
                child.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
            }

            matrixStackIn.popPose();
        }

    }

    private void doRender(PoseStack.Pose pose, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        ObjectListIterator var11 = this.cubeList.iterator();

        while(var11.hasNext()) {
            CNTabulaModelRenderUtils.ModelBox box = (CNTabulaModelRenderUtils.ModelBox)var11.next();

            for(CNTabulaModelRenderUtils.TexturedQuad quad : box.quads) {
                Vector3f normal = new Vector3f(quad.normal);
                normal.mul(normalMatrix);
                float normalX = normal.x();
                float normalY = normal.y();
                float normalZ = normal.z();

                for(int i = 0; i < 4; ++i) {
                    CNTabulaModelRenderUtils.PositionTextureVertex vertex = quad.vertexPositions[i];
                    float x = vertex.position.x() / 16.0F;
                    float y = vertex.position.y() / 16.0F;
                    float z = vertex.position.z() / 16.0F;
                    Vector4f transformed = new Vector4f(x, y, z, 1.0F);
                    transformed.mul(poseMatrix);
                    bufferIn.addVertex(transformed.x(), transformed.y(), transformed.z()).setColor(red, green, blue, alpha).setUv(vertex.textureU, vertex.textureV).setOverlay(packedOverlayIn).setLight(packedLightIn).setNormal(normalX, normalY, normalZ);
                }
            }
        }

    }

    public CNAdvancedEntityModel getModel() {
        return this.model;
    }

    public CNAdvancedModelBox setTextureOffset(int textureOffsetX, int textureOffsetY) {
        this.textureOffsetX = textureOffsetX;
        this.textureOffsetY = textureOffsetY;
        return this;
    }
}