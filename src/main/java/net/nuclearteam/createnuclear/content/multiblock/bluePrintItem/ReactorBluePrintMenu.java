package net.nuclearteam.createnuclear.content.multiblock.bluePrintItem;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nuclearteam.createnuclear.*;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType.TypeRodPredicate;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;

import static net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintItem.getItemStorage;

public class ReactorBluePrintMenu extends GhostItemMenu<ItemStack> {

    /**
     * Grid position of each of the 57 pattern slots, index-matched to {@link PatternData#slot()}
     * and to {@code DefaultHeatCalculator#FORMATTED_PATTERN} — the reactor's proximity-heat
     * calculation resolves neighbours by this exact index, so the array must never be reordered
     * or resized independently of that grid.
     */
    private static final int[][] POSITIONS = {
            {3, 0}, {4, 0}, {5, 0},
            {2, 1}, {3, 1}, {4, 1}, {5, 1}, {6, 1},
            {1, 2}, {2, 2}, {3, 2}, {4, 2}, {5, 2}, {6, 2}, {7, 2},
            {0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3}, {7, 3}, {8, 3},
            {0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4}, {6, 4}, {7, 4}, {8, 4},
            {0, 5}, {1, 5}, {2, 5}, {3, 5}, {4, 5}, {5, 5}, {6, 5}, {7, 5}, {8, 5},
            {1, 6}, {2, 6}, {3, 6}, {4, 6}, {5, 6}, {6, 6}, {7, 6},
            {2, 7}, {3, 7}, {4, 7}, {5, 7}, {6, 7},
            {3, 8}, {4, 8}, {5, 8}
    };

    private ReactorBluePrintData reactorBluePrintData;

    public ReactorBluePrintMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public ReactorBluePrintMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public static ReactorBluePrintMenu create(int id, Inventory inv, ItemStack stack) {
        return new ReactorBluePrintMenu(CNMenus.REACTOR_BLUEPRINT_MENU.get(), id, inv, stack);
    }

    public ReactorBluePrintData getReactorBluePrintData() {
        return reactorBluePrintData;
    }

    @Override
    protected boolean allowRepeats() {
        return false;
    }

    @Override
    protected void initAndReadInventory(ItemStack contentHolder) {
        super.initAndReadInventory(contentHolder);

        ItemStack glassPane = new ItemStack(Items.GLASS_PANE);
        PatternData[] emptyPattern = new PatternData[POSITIONS.length];
        for (int i = 0; i < emptyPattern.length; i++) {
            emptyPattern[i] = new PatternData(i, glassPane);
        }

        ReactorBluePrintData defaultData = new ReactorBluePrintData(
                0, 0,
                CNConfigs.server().rods.graphiteRodLifetime.get(),
                CNConfigs.server().rods.uraniumRodLifetime.get(),
                emptyPattern, emptyPattern
        );
        reactorBluePrintData = contentHolder.getOrDefault(CNDataComponents.REACTOR_BLUE_PRINT_DATA, defaultData);

        // Classified via RodType, not via the CNItemTags.COOLER/FUEL item tags: a rod can be
        // registered as fuel/cooler through RodType.Builder without also carrying the matching
        // item tag (e.g. the thorium rod), which made saved rods vanish on reopen when this used
        // to filter on the tags instead.
        Level level = playerInventory.player.level();
        PatternData[] pattern = reactorBluePrintData.pattern();
        for (int i = 0; i < POSITIONS.length; i++) {
            ItemStack stack = pattern[i].stack();
            boolean isRod = TypeRodPredicate.isFuel(stack, level) || TypeRodPredicate.isCooled(stack, level);
            ghostInventory.setStackInSlot(i, isRod ? stack : ItemStack.EMPTY);
        }
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        return getItemStorage(contentHolder);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(getPlayerInventoryXOffset(), getPlayerInventoryYOffset());
        addPatternSlots();
    }

    private void addPatternSlots() {
        int startWidth = 8 + 23;
        int startHeight = 45;
        int incr = 18;

        for (int i = 0; i < POSITIONS.length; i++) {
            int[] pos = POSITIONS[i];
            this.addSlot(new SlotItemHandler(ghostInventory, i, startWidth + incr * pos[0], startHeight + incr * pos[1]));
        }
    }

    @Override
    protected void saveData(ItemStack contentHolder) {
        Level level = playerInventory.player.level();

        // `pattern` keeps the grid exactly as the player laid it out; `patternAll` is the
        // normalized view where every empty slot AND every non-rod item becomes a glass
        // pane, so consumers can assume it only ever holds real rods (see PatternReader).
        PatternData[] pattern = new PatternData[POSITIONS.length];
        PatternData[] patternAll = new PatternData[POSITIONS.length];
        ItemStack glassPane = new ItemStack(Items.GLASS_PANE);
        int countGraphiteRod = 0;
        int countUraniumRod = 0;

        for (int i = 0; i < POSITIONS.length; i++) {
            ItemStack stack = ghostInventory.getStackInSlot(i);

            if (stack.isEmpty() || stack.getCount() < 1 || stack.getCount() > 99) {
                pattern[i] = new PatternData(i, glassPane);
                patternAll[i] = new PatternData(i, glassPane);
                continue;
            }

            boolean isFuel = TypeRodPredicate.isFuel(stack, level);
            boolean isCooler = TypeRodPredicate.isCooled(stack, level);

            if (isCooler) countGraphiteRod++;
            if (isFuel) countUraniumRod++;

            pattern[i] = new PatternData(i, stack);
            patternAll[i] = new PatternData(i, (isFuel || isCooler) ? stack : glassPane);
        }

        ReactorBluePrintData data = new ReactorBluePrintData(
                countGraphiteRod, countUraniumRod,
                CNConfigs.server().rods.graphiteRodLifetime.get(),
                CNConfigs.server().rods.uraniumRodLifetime.get(),
                pattern, patternAll
        );
        contentHolder.set(CNDataComponents.REACTOR_BLUE_PRINT_DATA, data);
    }

    protected int getPlayerInventoryXOffset() {
        return 31;
    }

    protected int getPlayerInventoryYOffset() {
        return 231;
    }

    @Override
    public boolean stillValid(Player player) {
        return playerInventory.getSelected() == contentHolder;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
        if (clickTypeIn == ClickType.THROW) {
            if (slotId >= 0 && slotId < 9) {
                clickTypeIn = ClickType.PICKUP;
                super.clicked(slotId, dragType, clickTypeIn, player);
            }
            return;
        }
        super.clicked(slotId, dragType, clickTypeIn, player);
    }
}
