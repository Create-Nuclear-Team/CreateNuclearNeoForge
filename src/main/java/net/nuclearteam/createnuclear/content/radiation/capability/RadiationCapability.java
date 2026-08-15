package net.nuclearteam.createnuclear.content.radiation.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.nuclearteam.createnuclear.*;
import net.nuclearteam.createnuclear.CNTags.CNEntityTags;
import net.nuclearteam.createnuclear.api.radiation.IRadiationSource;
import net.nuclearteam.createnuclear.api.radiation.RadiationRegistry;
import net.nuclearteam.createnuclear.foundation.utility.ConfigValueResolver;
import net.nuclearteam.createnuclear.foundation.utility.InventoryHashUtil;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;

import java.util.*;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID)
public class RadiationCapability {
   public static final Codec<RadiationCapability> CODEC = RecordCodecBuilder.create(i -> i.group(
       Codec.DOUBLE.optionalFieldOf("radiation", 0D).forGetter(RadiationCapability::getRadiation),
       Codec.LONG.optionalFieldOf("hash", 0L).forGetter(RadiationCapability::getInventoryHash),
       ResourceLocation.CODEC.optionalFieldOf("lastBiome").forGetter(c -> Optional.ofNullable(c.getLastBiomeLocation())),
       Codec.DOUBLE.optionalFieldOf("contagionDose", 0D).forGetter(RadiationCapability::getContagionDose),
       Codec.INT.optionalFieldOf("contagionTicks", 0).forGetter(RadiationCapability::getContagionTicks)
   ).apply(i, RadiationCapability::create));

   public static final StreamCodec<RegistryFriendlyByteBuf, RadiationCapability> STREAM_CODEC = StreamCodec.composite(
       ByteBufCodecs.DOUBLE, RadiationCapability::getRadiation,
       ByteBufCodecs.VAR_LONG, RadiationCapability::getInventoryHash,
       ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), c -> Optional.ofNullable(c.getLastBiomeLocation()),
       ByteBufCodecs.DOUBLE, RadiationCapability::getContagionDose,
       ByteBufCodecs.VAR_INT, RadiationCapability::getContagionTicks,
       RadiationCapability::create
   );

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

    private static RadiationCapability create(double radiation, long hash, Optional<ResourceLocation> lastBiome, double contagionDose, int contagionTicks) {
        RadiationCapability cap = new RadiationCapability();
        cap.setRadiation(radiation);
        cap.setInventoryHash(hash);
        cap.setLastBiomeLocation(lastBiome.orElse(null));
        cap.setContagionDose(contagionDose);
        cap.setContagionTicks(contagionTicks);

        return cap;
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living) {
            tickRadiation(living);
        }
    }

    public static void applyContagion(LivingEntity entity, double doseValue, int durationTicks) {
        RadiationCapability cap = entity.getData(CNAttachmentTypes.RADIATION);
        cap.setContagionDose(doseValue);
        cap.setContagionTicks(durationTicks);
    }

    public static void tickRadiation(LivingEntity entity) {
        Level level = entity.level();
        if (level.isClientSide) return;

        // Checked before getData: getData creates and stores the attachment on first access, so
        // guarding first keeps immune/blacklisted entities from accumulating one every tick.
        if (!canBeIrradiated(entity)) return;

        RadiationCapability cap = entity.getData(CNAttachmentTypes.RADIATION);

        if (entity instanceof Player player) {
            long newHash = InventoryHashUtil.compute(player);
            if (newHash != cap.getInventoryHash()) {
                cap.setInventoryHash(newHash);
                cap.setRadiation(Math.max(0, computeItemRadiation(player)));
            }
            player.syncData(CNAttachmentTypes.RADIATION);
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
        return RadiationRegistry.getRadiation(biomeKey, null);
    }

    public static boolean canBeIrradiated(LivingEntity entity) {
        if (entity.isSpectator()) return false;
        if (entity.getType().is(CNEntityTags.IRRADIATED_IMMUNE.tag)) return false;
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
