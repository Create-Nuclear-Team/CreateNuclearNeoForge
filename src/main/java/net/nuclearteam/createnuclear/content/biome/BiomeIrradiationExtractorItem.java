package net.nuclearteam.createnuclear.content.biome;

import net.minecraft.ChatFormatting;
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
import net.nuclearteam.createnuclear.CNDataComponents;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;
import net.nuclearteam.createnuclear.infrastructure.worldgen.biome.BiomeIrradiationService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
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

        if (getCharge(stack) >= getMaxCharge()) {
            return InteractionResultHolder.pass(stack);
        }

        boolean restored = BiomeIrradiationService.restoreArea(serverLevel, player.blockPosition());
        if (!restored) {
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
                CreateNuclearLang
                    .translateDirect("tooltip.biome_irradiation_extractor." + TAG, getCharge(stack), getMaxCharge())
                    .withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getCharge(stack) > 0 ? 1 : this.getDefaultMaxStackSize();
    }

    public static int getCharge(ItemStack stack) {
        return getChargeDataComponents(stack, 0);
    }

    public static int getMaxCharge() {
        return CNConfigs.server().biomeRestore.maxCharge.get();
    }

    public static void addCharge(ItemStack stack, int amount) {
        int current = getCharge(stack);
        int next = Mth.clamp(current + amount, 0, getMaxCharge());

        stack.set(CNDataComponents.CHARGE_BIOME_IRRADIATION_EXTRACTOR, next);
    }

    public static int getChargeDataComponents(ItemStack stack, int defaultValue) {
        return stack.getOrDefault(CNDataComponents.CHARGE_BIOME_IRRADIATION_EXTRACTOR, defaultValue);
    }
}
