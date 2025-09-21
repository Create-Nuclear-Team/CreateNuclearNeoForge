package net.nuclearteam.createnuclear;

import com.tterrag.registrate.builders.MenuBuilder.ScreenFactory;
import com.tterrag.registrate.builders.MenuBuilder.ForgeMenuFactory;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintItemScreen;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintMenu;
import net.nuclearteam.createnuclear.content.multiblock.input.ReactorInputMenu;
import net.nuclearteam.createnuclear.content.multiblock.input.ReactorInputScreen;

public class CNMenus {
    public static final MenuEntry<ReactorBluePrintMenu> REACTOR_BLUEPRINT_MENU = menu("reactor_blueprint_menu", ReactorBluePrintMenu::new, () -> ReactorBluePrintItemScreen::new);
    public static final MenuEntry<ReactorInputMenu> SLOT_ITEM_STORAGE = menu("slot_item_menu", ReactorInputMenu::new, () -> ReactorInputScreen::new);

    private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> MenuEntry<C> menu(String name, ForgeMenuFactory<C> factory, NonNullSupplier<ScreenFactory<C, S>> screenFactory) {
        return CreateNuclear.REGISTRATE
                .menu(name, factory, screenFactory)
                .register();
    }

    public static void register() {}
}
