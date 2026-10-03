package net.nuclearteam.createnuclear.foundation.utility;

import net.minecraft.world.inventory.ClickType;

import java.util.function.IntPredicate;

public class MenuClickUtil {
    public static boolean isThrowRedirectedToPickup(ClickType clickType, int slot, IntPredicate redirectedSlots) {
        return clickType == ClickType.THROW && redirectedSlots.test(slot);
    }
}
