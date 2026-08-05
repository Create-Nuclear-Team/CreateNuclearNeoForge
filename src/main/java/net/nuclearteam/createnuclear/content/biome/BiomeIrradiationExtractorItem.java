package net.nuclearteam.createnuclear.content.biome;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;
import net.nuclearteam.createnuclear.infrastructure.worldgen.biome.BiomeIrradiationService;

import java.util.List;

public class BiomeIrradiationExtractorItem extends Item {
    public static final String TAG = "biome_restore";
    private static final int CHARGE_PER_CLICK = 1;

    public BiomeIrradiationExtractorItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        ItemStack stack = player.getItemInHand(interactionHand);

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        if (getCharge(stack) >= getMaxCharge())
        {
            return InteractionResultHolder.pass(stack);
        }

        boolean restored = BiomeIrradiationService.restoreArea(serverLevel, player.blockPosition());
        if (!restored)
        {
            return InteractionResultHolder.pass(stack);
        }

        ItemStack charged = stack.copyWithCount(1);
        addCharge(charged, CHARGE_PER_CLICK);

        stack.shrink(1);
        if (stack.isEmpty()) {
            return InteractionResultHolder.success(charged);
        }

        if (!player.getInventory().add(charged)) {
            player.drop(charged, false);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (tooltipFlag.isAdvanced() && getCharge(stack) > 0) {
            tooltipComponents.add(
                    CreateNuclearLang.translateDirect("tooltip.biome_irradiation_extractor." + TAG, getCharge(stack), getMaxCharge())
                            .withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getCharge(stack) > 0 ? 1 : this.getDefaultMaxStackSize();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getCharge(stack) / getMaxCharge());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4A90D9;
    }

    public static int getCharge(ItemStack stack) {
        return getChargeTag(stack, 0);
    }

    public static int getMaxCharge() {
        return CNConfigs.server().biomeRestore.maxCharge.get();
    }

    public static void addCharge(ItemStack stack, int amount) {
        int current = getCharge(stack);
        int next = Mth.clamp(current + amount, 0, getMaxCharge());

        // Utilisation de la nouvelle API des composants pour stocker les données NBT
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY, customData ->
                customData.update(tag -> tag.putInt(TAG, next))
        );
    }

    public static int getChargeTag(ItemStack stack, int defaultValue) {
        if (stack == null || stack.isEmpty()) return defaultValue;

        // Récupération sécurisée via le composant CustomData de la 1.21
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) return defaultValue;

        CompoundTag tag = customData.copyTag();
        return (tag.contains(TAG)) ? tag.getInt(TAG) : defaultValue;
    }
}
