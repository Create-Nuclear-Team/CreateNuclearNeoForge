package net.nuclearteam.createnuclear.content.multiblock.controller.consumable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nuclearteam.createnuclear.CNDataComponents;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.PatternData;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads the rods a configured blueprint asks for, as a count per item.
 * <p>
 * The Forge implementation parsed the raw {@code patternAll}/{@code pattern} NBT
 * elements off the blueprint stack. On NeoForge the blueprint carries a typed
 * {@link ReactorBluePrintData} data component instead, so the parsing is replaced
 * by a direct read of {@link ReactorBluePrintData#patternAll()}.
 * <p>
 * {@code patternAll} is the normalized view written by
 * {@code ReactorBluePrintMenu#saveData}: empty slots and non-rod items are stored
 * as glass panes, which is why glass panes are the only value skipped here.
 * {@link ReactorBluePrintData#pattern()} (the raw grid) is used as a fallback for
 * blueprints written before the two views diverged.
 */
public final class PatternReader {

    private PatternReader() {
    }

    public static Map<Item, Integer> readItemCounts(ItemStack configuredPattern) {
        if (configuredPattern == null || configuredPattern.isEmpty())
            return Collections.emptyMap();

        ReactorBluePrintData data = configuredPattern.get(CNDataComponents.REACTOR_BLUE_PRINT_DATA);
        if (data == null)
            return Collections.emptyMap();

        PatternData[] slots = data.patternAll();
        if (slots == null || slots.length == 0)
            slots = data.pattern();
        if (slots == null || slots.length == 0)
            return Collections.emptyMap();

        Map<Item, Integer> counts = new HashMap<>();
        for (PatternData slot : slots) {
            if (slot == null)
                continue;
            ItemStack stack = slot.stack();
            if (stack != null && !stack.isEmpty() && !stack.is(Items.GLASS_PANE))
                counts.merge(stack.getItem(), 1, Integer::sum);
        }
        return counts;
    }
}
