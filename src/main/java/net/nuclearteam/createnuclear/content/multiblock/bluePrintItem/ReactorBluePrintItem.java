package net.nuclearteam.createnuclear.content.multiblock.bluePrintItem;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.nuclearteam.createnuclear.CNDataComponents;
import net.nuclearteam.createnuclear.CNItems;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorBluePrintItem extends Item implements MenuProvider {

    public ReactorBluePrintItem(Properties properties) {
        super(properties);
    }


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.translatable("item.createnuclear.reactor_blueprint.tooltip")
                .withStyle(ChatFormatting.GRAY));

        // Adjust the tooltip text to hint at the available action
        tooltipComponents.add(Component.translatable("item.createnuclear.reactor_blueprint.tooltip_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("reactor.item.gui.name");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ItemStack heldItem = player.getMainHandItem();
        return ReactorBluePrintMenu.create(id, inv, heldItem);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        // Plain right-click -> Opens the Blueprint screen
        if (!player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            if (!world.isClientSide && player instanceof ServerPlayer)
                player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, heldItem));
            return InteractionResultHolder.success(heldItem);
        }
        // Shift + right-click -> Sends a clickable link in chat!
        else if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            if (!world.isClientSide) {
                MutableComponent message = Component.translatable("item.createnuclear.reactor_blueprint.chat_info")
                    .withStyle(ChatFormatting.GREEN)
                    .append(" ")
                    .append(Component.translatable("item.createnuclear.reactor_blueprint.wiki_link")
                    .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://wiki.createnuclear.net/wiki/block&items/reactor_blueprint_item"))
                    ));

                player.sendSystemMessage(message);
            }
            return InteractionResultHolder.success(heldItem);
        }
        return InteractionResultHolder.pass(heldItem);
    }

    public static ItemStackHandler getItemStorage(ItemStack stack) {
        final int slotCount = 57;
        ItemStackHandler inventory = new ItemStackHandler(slotCount);

        if (stack.getItem() != CNItems.REACTOR_BLUEPRINT.get()) {
            throw new IllegalArgumentException("Cannot get configured items from non-blueprint item: " + stack);
        }

        ReactorBluePrintData data = stack.get(CNDataComponents.REACTOR_BLUE_PRINT_DATA);
        if (data == null || data.pattern().length != slotCount) {
            return inventory;
        }

        PatternData[] pattern = data.pattern();
        for (int i = 0; i < slotCount; i++) {
            inventory.setStackInSlot(i, pattern[i].stack());
        }

        return inventory;
    }
}
