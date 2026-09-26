package net.nuclearteam.createnuclear.content.contraptions.irradiated.cat;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.content.contraptions.irradiated.AnimalUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class IrradiatedCat extends TamableAnimal {
    private static final Ingredient TEMPT_INGREDIENT;
    private static final EntityDataAccessor<Boolean> IS_LYING;
    private static final EntityDataAccessor<Boolean> RELAX_STATE_ONE;
    @Nullable
    private TemptGoal temptGoal;
    private float lieDownAmount;
    private float lieDownAmountO;

    public IrradiatedCat(EntityType<? extends IrradiatedCat> entityType, Level level) {
        super(entityType, level);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void registerGoals() {
        this.temptGoal = new CatTemptGoal(this, 0.6, TEMPT_INGREDIENT, true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new CatRelaxOnOwnerGoal(this));
        this.goalSelector.addGoal(4, this.temptGoal);
        this.goalSelector.addGoal(5, new CatLieOnBedGoal(this, 1.1, 8));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 5.0F));
        this.goalSelector.addGoal(7, new CatSitOnBlockGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LeapAtTargetGoal(this, 0.3F));
        this.goalSelector.addGoal(9, new OcelotAttackGoal(this));
        this.goalSelector.addGoal(10, new BreedGoal(this, 0.8));
        this.goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 0.8, 1.0000001E-5F));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.targetSelector.addGoal(1, new NonTameRandomTargetGoal<>(this, Rabbit.class, false, (Predicate)null));
        this.targetSelector.addGoal(1, new NonTameRandomTargetGoal<>(this, Turtle.class, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    public void setLying(boolean lying) {
        this.entityData.set(IS_LYING, lying);
    }

    public boolean isLying() {
        return this.entityData.get(IS_LYING);
    }

    public void setRelaxStateOne(boolean relaxStateOne) {
        this.entityData.set(RELAX_STATE_ONE, relaxStateOne);
    }

    public boolean isRelaxStateOne() {
        return this.entityData.get(RELAX_STATE_ONE);
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_LYING, false);
        builder.define(RELAX_STATE_ONE, false);
    }

    public void customServerAiStep() {
        if (this.getMoveControl().hasWanted()) {
            double d = this.getMoveControl().getSpeedModifier();
            if (d == 0.6) {
                this.setPose(Pose.CROUCHING);
                this.setSprinting(false);
            } else if (d == 1.33) {
                this.setPose(Pose.STANDING);
                this.setSprinting(true);
            } else {
                this.setPose(Pose.STANDING);
                this.setSprinting(false);
            }
        } else {
            this.setPose(Pose.STANDING);
            this.setSprinting(false);
        }

    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        if (this.isTame()) {
            if (this.isInLove()) {
                return SoundEvents.CAT_PURR;
            } else {
                return this.random.nextInt(4) == 0 ? SoundEvents.CAT_PURREOW : SoundEvents.CAT_AMBIENT;
            }
        } else {
            return SoundEvents.CAT_STRAY_AMBIENT;
        }
    }

    public int getAmbientSoundInterval() {
        return 120;
    }

    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.CAT_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_DEATH;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.30000001192092896).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    protected void usePlayerItem(Player player, InteractionHand hand, ItemStack stack) {
        if (this.isFood(stack)) {
            this.playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);
        }

        super.usePlayerItem(player, hand, stack);
    }

    private float getAttackDamage() {
        return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public boolean doHurtTarget(Entity target) {
        return target.hurt(this.damageSources().mobAttack(this), this.getAttackDamage());
    }

    public void tick() {
        super.tick();
        if (this.temptGoal != null && this.temptGoal.isRunning() && !this.isTame() && this.tickCount % 100 == 0) {
            this.playSound(SoundEvents.CAT_BEG_FOR_FOOD, 1.0F, 1.0F);
        }

        this.handleLieDown();
    }

    private void handleLieDown() {
        if ((this.isLying() || this.isRelaxStateOne()) && this.tickCount % 5 == 0) {
            this.playSound(SoundEvents.CAT_PURR, 0.6F + 0.4F * (this.random.nextFloat() - this.random.nextFloat()), 1.0F);
        }

        this.updateLieDownAmount();
    }

    private void updateLieDownAmount() {
        this.lieDownAmountO = this.lieDownAmount;
        if (this.isLying()) {
            this.lieDownAmount = Math.min(1.0F, this.lieDownAmount + 0.15F);
        } else {
            this.lieDownAmount = Math.max(0.0F, this.lieDownAmount - 0.22F);
        }

    }

    public float getLieDownAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.lieDownAmountO, this.lieDownAmount);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        Cat cat = EntityType.CAT.create(level);
        if (cat != null && otherParent instanceof Cat) {

            if (this.isTame()) {
                cat.setOwnerUUID(this.getOwnerUUID());
                cat.setTame(true, false);
            }
        }

        return cat;
    }

    public boolean canMate(Animal otherAnimal) {
        if (!this.isTame()) {
            return false;
        } else if (!(otherAnimal instanceof Cat)) {
            return false;
        } else {
            Cat cat = (Cat)otherAnimal;
            return cat.isTame() && super.canMate(otherAnimal);
        }
    }


    @SuppressWarnings("null")
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        Item item = itemStack.getItem();
        if (this.level().isClientSide) {
            if (this.isTame() && this.isOwnedBy(player)) {
                return InteractionResult.SUCCESS;
            } else {
                return !this.isFood(itemStack) || !(this.getHealth() < this.getMaxHealth()) && this.isTame() ? InteractionResult.PASS : InteractionResult.SUCCESS;
            }
        } else {
            InteractionResult interactionResult;
            if (this.isTame()) {
                if (this.isOwnedBy(player)) {
                    if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
                        this.usePlayerItem(player, hand, itemStack);
                        this.heal(item.getFoodProperties(new ItemStack(item), null).saturation());
                        return InteractionResult.CONSUME;
                    }

                    interactionResult = super.mobInteract(player, hand);
                    if (!interactionResult.consumesAction() || this.isBaby()) {
                        this.setOrderedToSit(!this.isOrderedToSit());
                    }

                    return interactionResult;
                }
            } else if (itemStack.is(CNItems.YELLOWCAKE.get())) {
                return AnimalUtil.blockTamingWip(player, this.level());
            }

            interactionResult = super.mobInteract(player, hand);
            if (interactionResult.consumesAction()) {
                this.setPersistenceRequired();
            }

            return interactionResult;
        }
    }

    public boolean isFood(ItemStack stack) {
        return TEMPT_INGREDIENT.test(stack);
    }


    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTame() && this.tickCount > 2400;
    }

    public boolean isSteppingCarefully() {
        return this.isCrouching() || super.isSteppingCarefully();
    }

    static {
        TEMPT_INGREDIENT = Ingredient.of(CNItems.YELLOWCAKE);
        IS_LYING = SynchedEntityData.defineId(IrradiatedCat.class, EntityDataSerializers.BOOLEAN);
        RELAX_STATE_ONE = SynchedEntityData.defineId(IrradiatedCat.class, EntityDataSerializers.BOOLEAN);
    }


    static class CatRelaxOnOwnerGoal extends Goal {
        private final IrradiatedCat cat;
        @Nullable
        private Player ownerPlayer;
        @Nullable
        private BlockPos goalPos;
        private int onBedTicks;

        public CatRelaxOnOwnerGoal(IrradiatedCat cat) {
            this.cat = cat;
        }

        public boolean canUse() {
            if (!this.cat.isTame()) {
                return false;
            } else if (this.cat.isOrderedToSit()) {
                return false;
            } else {
                LivingEntity livingEntity = this.cat.getOwner();
                if (livingEntity instanceof Player) {
                    this.ownerPlayer = (Player)livingEntity;
                    if (!livingEntity.isSleeping()) {
                        return false;
                    }

                    if (this.cat.distanceToSqr(this.ownerPlayer) > 100.0) {
                        return false;
                    }

                    BlockPos blockPos = this.ownerPlayer.blockPosition();
                    BlockState blockState = this.cat.level().getBlockState(blockPos);
                    if (blockState.is(BlockTags.BEDS)) {
                        this.goalPos = blockState.getOptionalValue(BedBlock.FACING).map((direction) -> blockPos.relative(direction.getOpposite())).orElseGet(() -> new BlockPos(blockPos));
                        return !this.spaceIsOccupied();
                    }
                }

                return false;
            }
        }

        private boolean spaceIsOccupied() {
            assert this.goalPos != null;
            List<IrradiatedCat> list = this.cat.level().getEntitiesOfClass(IrradiatedCat.class, (new AABB(this.goalPos)).inflate(2.0));
            Iterator<IrradiatedCat> var2 = list.iterator();

            IrradiatedCat cat;
            do {
                do {
                    if (!var2.hasNext()) {
                        return false;
                    }

                    cat = var2.next();
                } while(cat == this.cat);
            } while(!cat.isLying() && !cat.isRelaxStateOne());

            return true;
        }

        public boolean canContinueToUse() {
            return this.cat.isTame() && !this.cat.isOrderedToSit() && this.ownerPlayer != null && this.ownerPlayer.isSleeping() && this.goalPos != null && !this.spaceIsOccupied();
        }

        public void start() {
            if (this.goalPos != null) {
                this.cat.setInSittingPose(false);
                this.cat.getNavigation().moveTo(this.goalPos.getX(), this.goalPos.getY(), this.goalPos.getZ(), 1.100000023841858);
            }

        }

        public void stop() {
            this.cat.setLying(false);
            float f = this.cat.level().getTimeOfDay(1.0F);
            if (this.ownerPlayer.getSleepTimer() >= 100 && (double)f > 0.77 && (double)f < 0.8 && (double)this.cat.level().getRandom().nextFloat() < 0.7) {
                this.giveMorningGift();
            }

            this.onBedTicks = 0;
            this.cat.setRelaxStateOne(false);
            this.cat.getNavigation().stop();
        }

        private void giveMorningGift() {
            RandomSource randomSource = this.cat.getRandom();
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            mutableBlockPos.set(this.cat.isLeashed() ? this.cat.getLeashHolder().blockPosition() : this.cat.blockPosition());
            this.cat.randomTeleport(mutableBlockPos.getX() + randomSource.nextInt(11) - 5, mutableBlockPos.getY() + randomSource.nextInt(5) - 2, (double)(mutableBlockPos.getZ() + randomSource.nextInt(11) - 5), false);
            mutableBlockPos.set(this.cat.blockPosition());
            LootTable lootTable = this.cat.level().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.CAT_MORNING_GIFT);
            LootParams lootParams = (new LootParams.Builder((ServerLevel)this.cat.level())).withParameter(LootContextParams.ORIGIN, this.cat.position()).withParameter(LootContextParams.THIS_ENTITY, this.cat).create(LootContextParamSets.GIFT);
            List<ItemStack> list = lootTable.getRandomItems(lootParams);

            for (ItemStack itemStack : list) {
                this.cat.level().addFreshEntity(new ItemEntity(this.cat.level(), (double) mutableBlockPos.getX() - (double) Mth.sin(this.cat.yBodyRot * 0.017453292F), (double) mutableBlockPos.getY(), (double) mutableBlockPos.getZ() + (double) Mth.cos(this.cat.yBodyRot * 0.017453292F), itemStack));
            }

        }

        public void tick() {
            if (this.ownerPlayer != null && this.goalPos != null) {
                this.cat.setInSittingPose(false);
                this.cat.getNavigation().moveTo(this.goalPos.getX(), this.goalPos.getY(), this.goalPos.getZ(), 1.100000023841858);
                if (this.cat.distanceToSqr(this.ownerPlayer) < 2.5) {
                    ++this.onBedTicks;
                    if (this.onBedTicks > this.adjustedTickDelay(16)) {
                        this.cat.setLying(true);
                        this.cat.setRelaxStateOne(false);
                    } else {
                        this.cat.lookAt(this.ownerPlayer, 45.0F, 45.0F);
                        this.cat.setRelaxStateOne(true);
                    }
                } else {
                    this.cat.setLying(false);
                }
            }

        }
    }

    private static class CatTemptGoal extends TemptGoal {
        @Nullable
        private Player selectedPlayer;
        private final IrradiatedCat cat;

        public CatTemptGoal(IrradiatedCat cat, double speedModifier, Ingredient items, boolean canScare) {
            super(cat, speedModifier, items, canScare);
            this.cat = cat;
        }

        public void tick() {
            super.tick();
            if (this.selectedPlayer == null && this.mob.getRandom().nextInt(this.adjustedTickDelay(600)) == 0) {
                this.selectedPlayer = this.player;
            } else if (this.mob.getRandom().nextInt(this.adjustedTickDelay(500)) == 0) {
                this.selectedPlayer = null;
            }

        }

        protected boolean canScare() {
            return this.selectedPlayer != null && this.selectedPlayer.equals(this.player) ? false : super.canScare();
        }

        public boolean canUse() {
            return super.canUse() && !this.cat.isTame();
        }
    }
}
