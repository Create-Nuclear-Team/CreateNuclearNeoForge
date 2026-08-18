package net.nuclearteam.createnuclear.content.contraptions.irradiated.chicken;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.nuclearteam.createnuclear.CNEntityType;
import net.nuclearteam.createnuclear.CNTags;
import net.nuclearteam.createnuclear.content.contraptions.irradiated.IrradiatedAnimal;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class IrradiatedChicken extends Animal implements IrradiatedAnimal {
    private static final EntityDimensions BABY_DIMENSIONS;
    private static final EntityDataAccessor<Boolean> DATA_CONVERTING_ID = SynchedEntityData.defineId(IrradiatedChicken.class, EntityDataSerializers.BOOLEAN);
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    public float flapping = 1.0F;
    private float nextFlap = 1.0F;
    public int eggTime;
    public boolean isChickenJockey;
    private int conversionTime;

    public IrradiatedChicken(EntityType<? extends IrradiatedChicken> entityType, Level level) {
        super(entityType, level);
        this.eggTime = this.random.nextInt(6000) + 6000;
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CONVERTING_ID, false);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new BreedGoal(this, (double)1.0F));
        this.goalSelector.addGoal(3, new TemptGoal(this, (double)1.0F, (p_335679_) -> p_335679_.is(CNTags.CNItemTags.FUEL.tag), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, (double)1.0F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, (double)4.0F)
                .add(Attributes.MOVEMENT_SPEED, (double)0.25F);
    }

    public void aiStep() {
        super.aiStep();
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed += (this.onGround() ? -1.0F : 4.0F) * 0.3F;
        this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
        if (!this.onGround() && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }

        this.flapping *= 0.9F;
        Vec3 vec3 = this.getDeltaMovement();
        if (!this.onGround() && vec3.y < (double)0.0F) {
            this.setDeltaMovement(vec3.multiply((double)1.0F, 0.6, (double)1.0F));
        }

        this.flap += this.flapping * 2.0F;
        if (!this.level().isClientSide && this.isAlive() && !this.isBaby() && !this.isChickenJockey() && --this.eggTime <= 0) {
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.spawnAtLocation(Items.EGG);
            this.gameEvent(GameEvent.ENTITY_PLACE);
            this.eggTime = this.random.nextInt(6000) + 6000;
        }

    }

    protected boolean isFlapping() {
        return this.flyDist > this.nextFlap;
    }

    protected void onFlap() {
        this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHICKEN_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.CHICKEN_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }

    @Nullable
    public IrradiatedChicken getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return CNEntityType.IRRADIATED_CHICKEN.create(level);
    }

    public boolean isFood(ItemStack stack) {
        return stack.is(CNTags.CNItemTags.FUEL.tag);
    }

    protected int getBaseExperienceReward() {
        return this.isChickenJockey() ? 10 : super.getBaseExperienceReward();
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.isChickenJockey = compound.getBoolean("IsChickenJockey");
        if (compound.contains("EggLayTime")) {
            this.eggTime = compound.getInt("EggLayTime");
        }

        if (compound.contains("ConversionTime", Tag.TAG_ANY_NUMERIC) && compound.getInt("ConversionTime") > -1) {
            this.startConverting(compound.getInt("ConversionTime"));
        }
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("IsChickenJockey", this.isChickenJockey);
        compound.putInt("EggLayTime", this.eggTime);
        compound.putInt("ConversionTime", this.isConverting() ? this.conversionTime : -1);
    }

    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return this.isChickenJockey();
    }

    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof LivingEntity) {
            ((LivingEntity)passenger).yBodyRot = this.yBodyRot;
        }

    }

    public boolean isChickenJockey() {
        return this.isChickenJockey;
    }

    public void setChickenJockey(boolean isChickenJockey) {
        this.isChickenJockey = isChickenJockey;
    }

    @Override
    public EntityType<? extends Animal> getNormalVariant() {
        return EntityType.CHICKEN;
    }

    @Override
    public void readFromVanilla(Animal animal) {
        if (animal instanceof Chicken chicken) {
            this.setChickenJockey(chicken.isChickenJockey());
        }
    }

    @Override
    public void writeToVanilla(Animal animal) {
        if (animal instanceof Chicken chicken) {
            chicken.setChickenJockey(this.isChickenJockey());
        }
    }

    @Override
    public boolean isConverting() {
        return this.entityData.get(DATA_CONVERTING_ID);
    }

    @Override
    public void setConverting() {
        this.entityData.set(DATA_CONVERTING_ID, true);
    }

    @Override
    public void setConversionTime(int conversionTime) {
        this.conversionTime = conversionTime;
    }

    @Override
    public int getConversionTime() {
        return this.conversionTime;
    }

    static {
        BABY_DIMENSIONS = CNEntityType.IRRADIATED_CHICKEN.get().getDimensions().scale(0.5F).withEyeHeight(0.2975F);
    }
}