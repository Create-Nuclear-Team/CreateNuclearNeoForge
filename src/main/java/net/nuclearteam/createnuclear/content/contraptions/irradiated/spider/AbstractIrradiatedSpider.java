package net.nuclearteam.createnuclear.content.contraptions.irradiated.spider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared base for the GeckoLib irradiated spider stages. Keeps the vanilla Spider AI (wall climbing,
 * night aggression); every stage uses the same skeleton and animation names.
 */
public abstract class AbstractIrradiatedSpider extends Spider implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.irradiated_spider.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.irradiated_spider.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.irradiated_spider.attack");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.irradiated_spider.death");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractIrradiatedSpider(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    /** Ticks the corpse stays before being removed, so the death animation can play out. */
    protected abstract int getDeathDurationTicks();

    /** Client side, called for each custom instruction keyframe ("timeline" in Blockbench). */
    protected void onAnimationInstruction(String instructions) {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isDeadOrDying())
                return state.setAndContinue(DEATH);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }).setCustomInstructionKeyframeHandler(event -> onAnimationInstruction(event.getKeyframeData().getInstructions())));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
            .triggerableAnim("attack", ATTACK));
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= getDeathDurationTicks() && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit)
            this.triggerAnim("attack", "attack");
        return hit;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
