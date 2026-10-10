package net.nuclearteam.createnuclear.content.equipment.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.nuclearteam.createnuclear.content.contraptions.irradiated.CNModelLayers;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public final class AntiRadiationArmorClientExtensions implements IClientItemExtensions {
    private final Map<EquipmentSlot, AntiRadiationArmorModel> models = new EnumMap<>(EquipmentSlot.class);

    @Override
    public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        return models.computeIfAbsent(equipmentSlot, slot -> new AntiRadiationArmorModel(
                Minecraft.getInstance().getEntityModels().bakeLayer(CNModelLayers.ANTI_RADIATION_ARMOR), slot));
    }
}
