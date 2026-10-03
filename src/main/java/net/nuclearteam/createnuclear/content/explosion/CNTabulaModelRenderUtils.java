package net.nuclearteam.createnuclear.content.explosion;

import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

public class CNTabulaModelRenderUtils {
    @OnlyIn(Dist.CLIENT)
    static class PositionTextureVertex {
        public final Vector3f position;
        public final float textureU;
        public final float textureV;

        public PositionTextureVertex(float x, float y, float z, float texU, float texV) {
            this(new Vector3f(x, y, z), texU, texV);
        }

        public PositionTextureVertex setTextureUV(float texU, float texV) {
            return new PositionTextureVertex(this.position, texU, texV);
        }

        public PositionTextureVertex(Vector3f position, float texU, float texV) {
            this.position = position;
            this.textureU = texU;
            this.textureV = texV;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class TexturedQuad {
        public final PositionTextureVertex[] vertexPositions;
        public final Vector3f normal;

        public TexturedQuad(PositionTextureVertex[] vertices, float u1, float v1, float u2, float v2, float textureWidth, float textureHeight, boolean mirror, Direction direction) {
            this.vertexPositions = vertices;
            float uOffset = 0.0F / textureWidth;
            float vOffset = 0.0F / textureHeight;
            vertices[0] = vertices[0].setTextureUV(u2 / textureWidth - uOffset, v1 / textureHeight + vOffset);
            vertices[1] = vertices[1].setTextureUV(u1 / textureWidth + uOffset, v1 / textureHeight + vOffset);
            vertices[2] = vertices[2].setTextureUV(u1 / textureWidth + uOffset, v2 / textureHeight - vOffset);
            vertices[3] = vertices[3].setTextureUV(u2 / textureWidth - uOffset, v2 / textureHeight - vOffset);
            if (mirror) {
                int vertexCount = vertices.length;

                for(int i = 0; i < vertexCount / 2; ++i) {
                    PositionTextureVertex swap = vertices[i];
                    vertices[i] = vertices[vertexCount - 1 - i];
                    vertices[vertexCount - 1 - i] = swap;
                }
            }

            this.normal = direction.step();
            if (mirror) {
                this.normal.mul(-1.0F, 1.0F, 1.0F);
            }

        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ModelBox {
        public final TexturedQuad[] quads;
        public final float posX1;
        public final float posY1;
        public final float posZ1;
        public final float posX2;
        public final float posY2;
        public final float posZ2;

        public ModelBox(int texOffX, int texOffY, float x, float y, float z, float width, float height, float depth, float growX, float growY, float growZ, boolean mirror, float texWidth, float texHeight) {
            this.posX1 = x;
            this.posY1 = y;
            this.posZ1 = z;
            this.posX2 = x + width;
            this.posY2 = y + height;
            this.posZ2 = z + depth;
            this.quads = new TexturedQuad[6];
            float x2 = x + width;
            float y2 = y + height;
            float z2 = z + depth;
            x -= growX;
            y -= growY;
            z -= growZ;
            x2 += growX;
            y2 += growY;
            z2 += growZ;
            if (mirror) {
                float tmp = x2;
                x2 = x;
                x = tmp;
            }

            PositionTextureVertex vertex1 = new PositionTextureVertex(x, y, z, 0.0F, 0.0F);
            PositionTextureVertex vertex2 = new PositionTextureVertex(x2, y, z, 0.0F, 8.0F);
            PositionTextureVertex vertex3 = new PositionTextureVertex(x2, y2, z, 8.0F, 8.0F);
            PositionTextureVertex vertex4 = new PositionTextureVertex(x, y2, z, 8.0F, 0.0F);
            PositionTextureVertex vertex5 = new PositionTextureVertex(x, y, z2, 0.0F, 0.0F);
            PositionTextureVertex vertex6 = new PositionTextureVertex(x2, y, z2, 0.0F, 8.0F);
            PositionTextureVertex vertex7 = new PositionTextureVertex(x2, y2, z2, 8.0F, 8.0F);
            PositionTextureVertex vertex8 = new PositionTextureVertex(x, y2, z2, 8.0F, 0.0F);
            float texU0 = (float)texOffX;
            float texU1 = (float)texOffX + depth;
            float texU2 = (float)texOffX + depth + width;
            float texU3 = (float)texOffX + depth + width + width;
            float texU4 = (float)texOffX + depth + width + depth;
            float texU5 = (float)texOffX + depth + width + depth + width;
            float texV0 = (float)texOffY;
            float texV1 = (float)texOffY + depth;
            float texV2 = (float)texOffY + depth + height;
            this.quads[2] = new TexturedQuad(new PositionTextureVertex[]{vertex6, vertex5, vertex1, vertex2}, texU1, texV0, texU2, texV1, texWidth, texHeight, mirror, Direction.DOWN);
            this.quads[3] = new TexturedQuad(new PositionTextureVertex[]{vertex3, vertex4, vertex8, vertex7}, texU2, texV1, texU3, texV0, texWidth, texHeight, mirror, Direction.UP);
            this.quads[1] = new TexturedQuad(new PositionTextureVertex[]{vertex1, vertex5, vertex8, vertex4}, texU0, texV1, texU1, texV2, texWidth, texHeight, mirror, Direction.WEST);
            this.quads[4] = new TexturedQuad(new PositionTextureVertex[]{vertex2, vertex1, vertex4, vertex3}, texU1, texV1, texU2, texV2, texWidth, texHeight, mirror, Direction.NORTH);
            this.quads[0] = new TexturedQuad(new PositionTextureVertex[]{vertex6, vertex2, vertex3, vertex7}, texU2, texV1, texU4, texV2, texWidth, texHeight, mirror, Direction.EAST);
            this.quads[5] = new TexturedQuad(new PositionTextureVertex[]{vertex5, vertex6, vertex7, vertex8}, texU4, texV1, texU5, texV2, texWidth, texHeight, mirror, Direction.SOUTH);
        }
    }
}