package net.nuclearteam.createnuclear.content.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor;

public class SmallNuclearExplosionParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private boolean hasFadeColor = false;
    private float fadeR;
    private float fadeG;
    private float fadeB;
    protected SmallNuclearExplosionParticle(ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites, boolean shortLifespan, int color1) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.setSize(0.5F, 0.5F);
        this.quadSize = (shortLifespan ? 1 : 0.8F) + world.random.nextFloat() * 0.3F;
        this.lifetime = shortLifespan ? 5 + world.random.nextInt(3) : 15 + world.random.nextInt(10);
        this.friction = 0.96F;
        float randCol = world.random.nextFloat() * 0.05F;
        this.sprites = sprites;
        this.setColor(Math.min(FastColor.ARGB32.red(color1) / 255F + randCol, 1), Math.min(1F, FastColor.ARGB32.green(color1) / 255F + randCol), Math.min(1F, FastColor.ARGB32.blue(color1) / 255F + randCol));
    }

    public void setFadeColor(int i){
        hasFadeColor = true;
        this.fadeR = (float) ((i & 16711680) >> 16) / 255.0F;
        this.fadeG = (float) ((i & '\uff00') >> 8) / 255.0F;
        this.fadeB = (float) ((i & 255) >> 0) / 255.0F;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.setSpriteFromAge(this.sprites);
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            if(hasFadeColor){
                this.rCol += (fadeR - this.rCol) * 0.2F;
                this.gCol += (fadeG - this.gCol) * 0.2F;
                this.bCol += (fadeB - this.bCol) * 0.2F;
            }else{
                this.rCol = this.rCol * 0.95F;
                this.gCol = this.gCol * 0.95F;
                this.bCol = this.bCol * 0.95F;
            }
            this.move(this.xd, this.yd, this.zd);
            this.xd *= this.friction;
            this.yd *= this.friction;
            this.zd *= this.friction;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public float getQuadSize(float scaleFactor) {
        return super.getQuadSize(scaleFactor);
    }

    public int getLightColor(float partialTicks) {
        return 240;
    }

    public static class NukeFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public NukeFactory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            SmallNuclearExplosionParticle particle = new SmallNuclearExplosionParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet, false, 0XFFB300);
            particle.setSpriteFromAge(spriteSet);
            return particle;
        }
    }
}
