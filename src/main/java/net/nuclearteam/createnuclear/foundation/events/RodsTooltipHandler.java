package net.nuclearteam.createnuclear.foundation.events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.item.RodsStats;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID, value = Dist.CLIENT)
public class RodsTooltipHandler {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        Player player = event.getEntity();

        if (player == null) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        // Mod items already get their rod tooltip via Registrate's setTooltipModifierFactory
        // (CreateNuclear.REGISTRATE). This handler only serves EXTERNAL items (other mods or
        // datapack-defined RodTypes resolved at runtime via RodType.resolveRodType), which
        // setTooltipModifierFactory cannot cover. Do NOT remove/invert: doing so double-tooltips mod rods.
        if (id != null && CreateNuclear.MOD_ID.equals(id.getNamespace())) return;

        RodsStats rodsStats = RodsStats.create(item);
        rodsStats.modify(event);
    }
}