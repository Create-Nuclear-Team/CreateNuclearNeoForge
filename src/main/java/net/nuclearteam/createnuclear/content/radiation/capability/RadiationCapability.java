package net.nuclearteam.createnuclear.content.radiation.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.nuclearteam.createnuclear.CNAttributes;
import net.nuclearteam.createnuclear.CNEffects;
import net.nuclearteam.createnuclear.CNTags;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.radiation.IRadiationSource;
import net.nuclearteam.createnuclear.api.radiation.RadiationRegistry;
import net.nuclearteam.createnuclear.foundation.utility.ConfigValueResolver;
import net.nuclearteam.createnuclear.foundation.utility.InventoryHashUtil;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class RadiationCapability implements INBTSerializable<CompoundTag> {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CreateNuclear.MOD_ID);
    // serializable(), not builder(): builder() attaches no serializer, so the state was never
    // written to disk and never restored on load.
    public static final Supplier<AttachmentType<RadiationCapability>> RADIATION = ATTACHMENT_TYPES.register("radiation",
            () -> AttachmentType.serializable(RadiationCapability::new).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    private double radiation;
    private long inventoryHash;
    private ResourceLocation lastBiomeLocation;
    private double contagionDose;
    private int contagionTicks;

    public double getRadiation() { return this.radiation; }
    public void setRadiation(double value) { this.radiation = value; }

    public long getInventoryHash() { return this.inventoryHash; }
    public void setInventoryHash(long hash) { this.inventoryHash = hash; }

    public ResourceLocation getLastBiomeLocation() { return this.lastBiomeLocation; }
    public void setLastBiomeLocation(ResourceLocation location) { this.lastBiomeLocation = location; }

    public double getContagionDose() { return this.contagionDose; }
    public void setContagionDose(double dose) { this.contagionDose = dose; }

    public int getContagionTicks() { return this.contagionTicks; }
    public void setContagionTicks(int ticks) { this.contagionTicks = ticks; }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("radiation", this.getRadiation());
        tag.putLong("hash", this.getInventoryHash());
        if (this.getLastBiomeLocation() != null) {
            tag.putString("lastBiome", this.getLastBiomeLocation().toString());
        }
        tag.putDouble("contagionDose", this.getContagionDose());
        tag.putInt("contagionTicks", this.getContagionTicks());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.setRadiation(nbt.getDouble("radiation"));
        this.setInventoryHash(nbt.getLong("hash"));
        this.setLastBiomeLocation(nbt.contains("lastBiome") ? ResourceLocation.tryParse(nbt.getString("lastBiome")) : null);
        this.setContagionDose(nbt.getDouble("contagionDose"));
        this.setContagionTicks(nbt.getInt("contagionTicks"));
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living) {
            tickRadiation(living);
        }
    }

    public static void applyContagion(LivingEntity entity, double doseValue, int durationTicks) {
        RadiationCapability cap = entity.getData(RADIATION);
        cap.setContagionDose(doseValue);
        cap.setContagionTicks(durationTicks);
    }

    public static void tickRadiation(LivingEntity entity) {
        Level level = entity.level();
        if (level.isClientSide) return;

        // Checked before getData: getData creates and stores the attachment on first access, so
        // guarding first keeps immune/blacklisted entities from accumulating one every tick.
        if (!canBeIrradiated(entity)) return;

        RadiationCapability cap = entity.getData(RADIATION);

        if (entity instanceof Player player) {
            long newHash = InventoryHashUtil.compute(player);
            if (newHash != cap.getInventoryHash()) {
                cap.setInventoryHash(newHash);
                cap.setRadiation(Math.max(0, computeItemRadiation(player)));
            }
        } else {
            cap.setRadiation(Math.max(0, computeItemRadiation(entity)));
        }

        ResourceKey<Biome> biomeKey = level.getBiome(entity.blockPosition()).unwrapKey().orElse(null);
        ResourceLocation biomeLoc = biomeKey != null ? biomeKey.location() : null;
        if (!Objects.equals(biomeLoc, cap.getLastBiomeLocation())) {
            cap.setLastBiomeLocation(biomeLoc);
        }

        if (cap.getContagionTicks() > 0) {
            cap.setContagionTicks(cap.getContagionTicks() - 1);
        }

        double contagionDose = cap.getContagionTicks() > 0 ? cap.getContagionDose() : 0;

        double totalRaw = cap.getRadiation() + getRawBiomeRadiation(biomeKey) + contagionDose;
        double resistance = getRadiationResistance(entity);
        double totalRadiation = totalRaw * (1.0 - resistance);

        applyEffects(entity, totalRadiation);
    }

    private static double computeItemRadiation(Player player) {
        double radiation = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof IRadiationSource source)
                radiation += source.getRadiation(stack, player);
            radiation += RadiationRegistry.getRadiation(stack, player);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof IRadiationSource source)
                radiation += source.getRadiation(stack, player);
            radiation += RadiationRegistry.getRadiation(stack, player);
        }
        return radiation;
    }

    private static double getStackRadiation(ItemStack stack, LivingEntity entity) {
        double radiation = 0;
        if (stack.getItem() instanceof IRadiationSource source) radiation += source.getRadiation(stack, entity);
        radiation += RadiationRegistry.getRadiation(stack, entity);
        return radiation;
    }

    private static double computeItemRadiation(LivingEntity entity) {
        double radiation = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            radiation += getStackRadiation(stack, entity);
        }
        radiation += getStackRadiation(entity.getMainHandItem(), entity);
        radiation += getStackRadiation(entity.getOffhandItem(), entity);
        return radiation;
    }

    private static double getRawBiomeRadiation(ResourceKey<Biome> biomeKey) {
        if (biomeKey == null) return 0;
        return RadiationRegistry.get(biomeKey);
    }

    public static boolean canBeIrradiated(LivingEntity entity) {
        if (entity.isSpectator()) return false;
        if (entity.getType().is(CNTags.CNEntityTags.IRRADIATED_IMMUNE.tag)) return false;
        if (!CNConfigs.server().radiation.enabledItemRadiation.get()) return false;
        if (getEntityBlacklist().contains(entity.getType())) return false;
        return getRadiationResistance(entity) < 1.0;
    }

    private static List<? extends String> cachedBlacklistSource;
    private static Set<EntityType<?>> cachedBlacklist = Set.of();

    private static Set<EntityType<?>> getEntityBlacklist() {
        List<? extends String> source = CNConfigs.server().radiation.configuredLists.getEntityBlackList();
        if (source != cachedBlacklistSource) {
            Set<EntityType<?>> resolved = new HashSet<>();
            ConfigValueResolver.loadValuesInSet(source, resolved,
                    entry -> BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(entry)));
            cachedBlacklist = resolved;
            cachedBlacklistSource = source;
        }
        return cachedBlacklist;
    }

    public static double getRadiationResistance(LivingEntity entity) {
        double resistance = 0d;
        AttributeInstance attribute = entity.getAttribute(CNAttributes.IRRADIATED_RESISTANCE);
        if (attribute != null) resistance += attribute.getValue();
        return Mth.clamp(resistance, 0.0, 1.0);
    }

    private static void applyEffects(LivingEntity entity, double radiation) {
        final double radiation_desactive = 0;
        if (radiation <= radiation_desactive) return;

        int amp;
        if (radiation < CNConfigs.server().radiation.radiationLevel1.get()) amp = CNConfigs.server().radiation.amplifierLevel0.get();
        else if (radiation < CNConfigs.server().radiation.radiationLevel2.get()) amp = CNConfigs.server().radiation.amplifierLevel1.get();
        else if (radiation < CNConfigs.server().radiation.radiationLevel3.get()) amp = CNConfigs.server().radiation.amplifierLevel2.get();
        else amp = CNConfigs.server().radiation.amplifierLevel2.get();

        MobEffectInstance current = entity.getEffect(CNEffects.RADIATION);

        if (current != null && current.getAmplifier() != amp) {
            entity.removeEffect(CNEffects.RADIATION);
            current = null;
        }

        if (current == null || current.getDuration() <= 40) {
            entity.addEffect(new MobEffectInstance(CNEffects.RADIATION, 100, amp, true, true));
        }
    }
}