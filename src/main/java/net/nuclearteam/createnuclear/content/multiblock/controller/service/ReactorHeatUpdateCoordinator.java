package net.nuclearteam.createnuclear.content.multiblock.controller.service;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.CNDataComponents;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType;
import net.nuclearteam.createnuclear.content.logistics.BigFluidStack;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintData;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerInventory;
import net.nuclearteam.createnuclear.content.multiblock.controller.consumable.PatternReader;
import net.nuclearteam.createnuclear.content.multiblock.controller.display.ReactorDisplayState;
import net.nuclearteam.createnuclear.content.multiblock.controller.manager.ReactorInputFluidManagerI;
import net.nuclearteam.createnuclear.content.multiblock.reactorLogic.HeatBalance;

import java.util.Collections;
import java.util.Map;

/**
 * Default implementation of {@link IReactorHeatUpdateCoordinator}.
 * Delegates raw heat calculation to {@link IHeatService} and only adds the
 * pattern-availability logic (fuel-only vs. full pattern).
 */
public class ReactorHeatUpdateCoordinator implements IReactorHeatUpdateCoordinator {
    private final IHeatService heatService;

    public ReactorHeatUpdateCoordinator(IHeatService heatService) {
        this.heatService = heatService;
    }

    /**
     * A blueprint counts as unconfigured when it carries no
     * {@link ReactorBluePrintData}. Replaces the Forge check on an empty NBT tag.
     */
    private static boolean isEmptyPattern(ItemStack configuredPattern) {
        return configuredPattern.isEmpty()
                || configuredPattern.get(CNDataComponents.REACTOR_BLUE_PRINT_DATA) == null;
    }

    /**
     * Stores the computed heat on the blueprint stack.
     * <p>
     * The Forge original wrote into {@code configuredPattern.getOrCreateTag()},
     * mutating the stack's tag in place. On NeoForge the equivalent read returns a
     * defensive copy, so the write has to go through the typed data component or it
     * would be silently discarded and the heat would stay pinned at zero.
     */
    private static void writeHeat(ItemStack configuredPattern, int heat) {
        if (configuredPattern.isEmpty()) return;
        configuredPattern.set(CNDataComponents.HEAT, (float) heat);
    }

    @Override
    public HeatBalance calculateHeatBalance(ItemStack configuredPattern, ReactorDisplayState displayState, Level level) {
        Map<Item, Integer> patternCounts = PatternReader.readItemCounts(configuredPattern);
        Map<Item, Integer> currentItems = displayState != null && displayState.items() != null
                ? displayState.items() : Collections.emptyMap();

        int heatPoints = 0;
        int coolingPoints = 0;

        for (Map.Entry<Item, Integer> entry : patternCounts.entrySet()) {
            Item item = entry.getKey();
            int requiredCount = entry.getValue();
            int availableCount = currentItems.getOrDefault(item, 0);
            int countToUse = Math.min(requiredCount, availableCount);
            if (countToUse <= 0) continue;

            RodType rodType = RodType.resolveRodType(item, level);
            if (rodType == null || !rodType.isNotEmptyItem()) continue;

            int weighted = rodType.ratio().get() * countToUse;
            if (rodType.type() == RodType.TypeRod.FUEL) heatPoints += weighted;
            else if (rodType.type() == RodType.TypeRod.COOLER) coolingPoints += weighted;
        }

        return new HeatBalance(heatPoints, coolingPoints);
    }

    /** Predicate applied to each pattern entry when checking availability. */
    @FunctionalInterface
    private interface  PatternEntryCheck {
        boolean test(Item item, int requiredCount, int availableCount);
    }

    /**
     * Iterates over the items required by the configured pattern and applies
     * {@code check} to each entry, stopping as soon as one entry fails.
     *
     * @return {@code true} if {@code check} succeeded for every pattern entry
     */
    private static boolean checkPatternAvailability(ItemStack configuredPattern, ReactorDisplayState displayState,
                                                    PatternEntryCheck check) {
        Map<Item, Integer> patternCounts = PatternReader.readItemCounts(configuredPattern);
        Map<Item, Integer> currentItems = displayState != null && displayState.items() != null
                ? displayState.items() : Collections.emptyMap();

        for (Map.Entry<Item, Integer> entry : patternCounts.entrySet()) {
            Item item = entry.getKey();
            int requiredCount = entry.getValue();
            int availableCount = currentItems.getOrDefault(item, 0);
            if (!check.test(item, requiredCount, availableCount)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canRun(ItemStack configuredPattern, ReactorDisplayState displayState,
                          ReactorInputFluidManagerI inputFluidManager, Level level, boolean assembled) {
        if (isEmptyPattern(configuredPattern) || inputFluidManager.size() == 0 || !assembled) {
            return false;
        }

        boolean[] hasAnyFuel = {false};
        boolean allFuelAvailable = checkPatternAvailability(configuredPattern, displayState,
            (item, requiredCount, availableCount) -> {
                RodType rodType = RodType.resolveRodType(item, level);
                if (rodType.isNotEmptyItem() && rodType.type() == RodType.TypeRod.FUEL) {
                    hasAnyFuel[0] = true;
                    return availableCount > 0;
                }
                return true; // non-fuel entries don't block this gate
            }
        );

        return allFuelAvailable && hasAnyFuel[0];
    }

    @Override
    public int updateHeatOnly(ItemStack configuredPattern, ReactorDisplayState displayState, BigFluidStack fluidStack,
                              HeatBalance heatBalance, int previousHeat, ReactorControllerInventory inventory, Level level, boolean assembled) {
        int heat = 0;
        boolean emptyPattern = isEmptyPattern(configuredPattern);
        if (!emptyPattern && assembled) {
            boolean allAvailable = checkPatternAvailability(configuredPattern, displayState,
                    (item, requiredCount, availableCount) -> availableCount >= requiredCount);
            if (allAvailable) {
                heat = (int) heatService.calculateHeat(fluidStack, heatBalance, previousHeat, inventory, level, displayState);
            }
        }
        writeHeat(configuredPattern, heat);
        return heat;
    }

    @Override
    public int calculateAndWriteHeat(ItemStack configuredPattern, BigFluidStack fluidStack, HeatBalance heatBalance, int previousHeat,
                                     ReactorControllerInventory inventory, Level level, ReactorDisplayState displayState) {
        int heat = (int) heatService.calculateHeat(fluidStack, heatBalance, previousHeat, inventory, level, displayState);
        writeHeat(configuredPattern, heat);
        return heat;
    }
}
