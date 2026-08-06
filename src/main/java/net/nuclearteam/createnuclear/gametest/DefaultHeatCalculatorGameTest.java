package net.nuclearteam.createnuclear.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nuclearteam.createnuclear.CNBlocks;
import net.nuclearteam.createnuclear.CNDataComponents;
import net.nuclearteam.createnuclear.CNItems;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.PatternData;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintData;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlockEntity;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerInventory;
import net.nuclearteam.createnuclear.content.multiblock.controller.display.ReactorDisplayState;
import net.nuclearteam.createnuclear.content.multiblock.reactorLogic.DefaultHeatCalculator;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;

import java.util.List;
import java.util.Map;

/**
 * GameTest coverage for {@link DefaultHeatCalculator#computeHeat}, run against a real
 * {@code ServerLevel}, a real {@link ReactorControllerBlockEntity} and real registered
 * rod items (no mocks) — plain JUnit cannot exercise this method at all: {@code Item},
 * {@code ItemStack} and {@code RodType} resolution all depend on Minecraft/NeoForge registries
 * that only exist once the game has bootstrapped.
 * <p>
 * All tests share the "empty_platform" structure and place a single
 * {@code CNBlocks.REACTOR_CONTROLLER} to obtain a real {@link ReactorControllerInventory}.
 * The reactor pattern (57 slots, indices per {@code DefaultHeatCalculator.FORMATTED_PATTERN})
 * is written into the blueprint's {@link ReactorBluePrintData} component and set into inventory
 * slot 0, exactly as {@code ReactorBluePrintItem.getItemStorage} reads it back.
 * <p>
 * This is where the port diverges from Forge: Forge writes the pattern into the blueprint's
 * raw {@code pattern} NBT tag, which has no NeoForge equivalent since the blueprint carries a
 * typed data component instead.
 * <p>
 * Expected heat values are computed from the live {@code CNConfigs.server().rods} values
 * rather than hardcoded, so these tests stay correct if the mod's default balance changes.
 */
@GameTestHolder(CreateNuclear.MOD_ID)
@PrefixGameTestTemplate(false)
public class DefaultHeatCalculatorGameTest {

    private static final String STRUCTURE = "empty_platform";
    private static final double DELTA = 1e-9;
    private static final int PATTERN_SLOTS = 57;

    // ---------- test fixtures ----------

    private static ReactorControllerInventory placeController(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, CNBlocks.REACTOR_CONTROLLER.get().defaultBlockState());
        ReactorControllerBlockEntity be = (ReactorControllerBlockEntity) helper.getBlockEntity(rel);
        return be.getInventoryObject();
    }

    /**
     * Writes {@code rodsBySlot} into the blueprint's {@link ReactorBluePrintData} component and
     * loads it into slot 0. Every one of the 57 slots must be present, since
     * {@code ReactorBluePrintItem.getItemStorage} bails out on a shorter array.
     */
    private static void loadPattern(ReactorControllerInventory inventory, Map<Integer, Item> rodsBySlot) {
        PatternData[] pattern = new PatternData[PATTERN_SLOTS];
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            Item rod = rodsBySlot.get(slot);
            pattern[slot] = new PatternData(slot, rod == null ? ItemStack.EMPTY : new ItemStack(rod));
        }

        ItemStack blueprint = new ItemStack(CNItems.REACTOR_BLUEPRINT.get());
        blueprint.set(CNDataComponents.REACTOR_BLUE_PRINT_DATA,
                new ReactorBluePrintData(0, 0, 0, 0, pattern, pattern));
        inventory.setStackInSlot(0, blueprint);
    }

    private static final DefaultHeatCalculator CALCULATOR = new DefaultHeatCalculator();

    // ================================================================
    // 0. the heat component must survive being saved at zero
    // ================================================================

    /**
     * Regression test for a world-save crash: {@code CNDataComponents.HEAT} was registered with
     * {@code ExtraCodecs.POSITIVE_FLOAT}, which rejects {@code 0.0}. The controller writes the
     * heat on every tick, so as soon as a blueprint sat in a stopped reactor the chunk save blew
     * up with "Value must be positive: 0.0" and the block entity silently stopped persisting.
     * <p>
     * A persistent codec that can refuse a value fails at save time, not at write time, which is
     * why this is asserted here rather than left to the non-negative floor in
     * {@link DefaultHeatCalculator#computeHeat}.
     */
    @GameTest(template = STRUCTURE)
    public static void heatComponent_atZero_survivesSerialization(GameTestHelper helper) {
        ItemStack blueprint = new ItemStack(CNItems.REACTOR_BLUEPRINT.get());
        blueprint.set(CNDataComponents.HEAT, 0f);

        var registries = helper.getLevel().registryAccess();
        Tag saved;
        try {
            saved = blueprint.save(registries);
        } catch (RuntimeException e) {
            throw new GameTestAssertException(
                    "a blueprint carrying heat=0 must be serializable, got: " + e.getMessage());
        }

        ItemStack restored = ItemStack.parse(registries, saved).orElse(ItemStack.EMPTY);
        helper.assertTrue(restored.getOrDefault(CNDataComponents.HEAT, -1f) == 0f,
                "heat=0 should round-trip through save/parse, got "
                        + restored.getOrDefault(CNDataComponents.HEAT, -1f));
        helper.succeed();
    }

    // ================================================================
    // 1. single fuel rod, no neighbors
    // ================================================================

    @GameTest(template = STRUCTURE)
    public static void singleFuelRod_noNeighbors_addsOnlyItsOwnBaseRodHeat(GameTestHelper helper) {
        Item thorium = CNItems.THORIUM_ROD.get();
        int baseRodHeat = CNConfigs.server().rods.baseValueThorium.get();

        ReactorControllerInventory inventory = placeController(helper, new BlockPos(1, 1, 1));
        // slot 18: middle row, no adjacent slot populated -> no neighbor bonus possible
        loadPattern(inventory, Map.of(18, thorium));

        ReactorDisplayState displayState = new ReactorDisplayState(Map.of(thorium, 1), List.of(), 0);
        double overHeat = 10.0;

        double heat = CALCULATOR.computeHeat(null, null, inventory, overHeat, displayState, helper.getLevel());

        helper.assertTrue(Math.abs(heat - (baseRodHeat + overHeat)) < DELTA,
                "an isolated fuel rod should only contribute its own baseRodHeat, plus overHeat: expected "
                        + (baseRodHeat + overHeat) + ", got " + heat);
        helper.succeed();
    }

    // ================================================================
    // 2. single cooler rod, no neighbors
    // ================================================================

    @GameTest(template = STRUCTURE)
    public static void singleCoolerRod_noNeighbors_addsOnlyItsOwnBaseRodHeat(GameTestHelper helper) {
        Item graphite = CNItems.GRAPHITE_ROD.get();
        int baseRodHeat = CNConfigs.server().rods.graphiteBaseValue.get();

        ReactorControllerInventory inventory = placeController(helper, new BlockPos(1, 1, 1));
        loadPattern(inventory, Map.of(18, graphite));

        ReactorDisplayState displayState = new ReactorDisplayState(Map.of(graphite, 1), List.of(), 0);

        // computeHeat clamps its result to a minimum of 0 (Math.max(0, heat+overHeat)); an isolated
        // cooler's own baseRodHeat is negative by default, so a large overHeat is added here to keep
        // the total positive and actually observe baseRodHeat's contribution instead of it being
        // swallowed by the floor.
        double overHeat = 50.0;

        double heat = CALCULATOR.computeHeat(null, null, inventory, overHeat, displayState, helper.getLevel());

        helper.assertTrue(Math.abs(heat - (baseRodHeat + overHeat)) < DELTA,
                "a cooler contributes its own baseRodHeat exactly like a fuel rod would, when isolated: expected "
                        + (baseRodHeat + overHeat) + ", got " + heat);
        helper.succeed();
    }

    // ================================================================
    // 3. fuel/cooler mix adjacency: cooler scans its own fuel neighbors symmetrically
    //    to a fuel rod, matching the wiki calculator / external spec
    //    ("Graphite extra: -1/4Q of the heating rod")
    // ================================================================

    /**
     * Layout (row 3 of {@code FORMATTED_PATTERN}, three consecutive slots):
     * uranium A (18) - thorium B (19) - graphite C (20).
     * <p>
     * Expected total = sum of each rod's own baseRodHeat, PLUS proximity contributions from
     * both fuel-fuel and cooler-fuel adjacency (cooler-cooler and fuel-cooler-from-the-fuel-side
     * contribute nothing):
     * <ul>
     *   <li>A's scan (fuel, neighbor B is fuel) -&gt; + A.proximityRodHeat()</li>
     *   <li>B's scan (fuel, neighbor A is fuel) -&gt; + B.proximityRodHeat()</li>
     *   <li>B's scan (fuel, neighbor C is cooler) -&gt; contributes nothing (fuel only scores against fuel neighbors)</li>
     *   <li>C's scan (cooler, neighbor B is fuel) -&gt; + B.baseRodHeat() * C.proximityRodHeat()</li>
     * </ul>
     */
    @GameTest(template = STRUCTURE)
    public static void fuelCoolerMix_coolerScansItsOwnFuelNeighborsSymmetrically(GameTestHelper helper) {
        Item uranium = CNItems.URANIUM_ROD.get();
        Item thorium = CNItems.THORIUM_ROD.get();
        Item graphite = CNItems.GRAPHITE_ROD.get();

        int uraniumBase = CNConfigs.server().rods.uraniumBaseValue.get();
        float uraniumProxy = CNConfigs.server().rods.uraniumProximityBonus.get();
        int thoriumBase = CNConfigs.server().rods.baseValueThorium.get();
        float thoriumProxy = CNConfigs.server().rods.thoriumProxyBonus.get();
        int graphiteBase = CNConfigs.server().rods.graphiteBaseValue.get();
        float graphiteProxy = CNConfigs.server().rods.graphiteProximityMalus.getF();

        ReactorControllerInventory inventory = placeController(helper, new BlockPos(1, 1, 1));
        loadPattern(inventory, Map.of(18, uranium, 19, thorium, 20, graphite));

        ReactorDisplayState displayState = new ReactorDisplayState(
                Map.of(uranium, 1, thorium, 1, graphite, 1), List.of(), 0);

        double heat = CALCULATOR.computeHeat(null, null, inventory, /*overHeat*/ 0.0, displayState, helper.getLevel());

        double expected = (uraniumBase + thoriumBase + graphiteBase)  // each rod's own baseRodHeat
                + uraniumProxy              // A's scan: fuel neighbor B (thorium) -> addition
                + thoriumProxy              // B's scan: fuel neighbor A (uranium) -> addition
                + (thoriumBase * graphiteProxy);
                // C's scan: fuel neighbor B (thorium) -> multiplication (neighbor's base * cooler's own proximity)
                // B's scan towards C contributes 0: fuel only scores against fuel neighbors

        helper.assertTrue(Math.abs(heat - expected) < DELTA,
                "cooler must score its own malus against its fuel neighbor: expected "
                        + expected + ", got " + heat);
        helper.succeed();
    }

    // ================================================================
    // 4. 3x3 diamond of rods: cross-checked against the community wiki calculator
    // ================================================================

    /**
     * Layout (rows 3-5, columns 3-5 of {@code FORMATTED_PATTERN} — a fully interior 3x3 block,
     * every position outside it stays empty so neighbor counts match an isolated 3x3 grid):
     * <pre>
     * graphite(18) thorium(19) graphite(20)
     * thorium(27)  uranium(28) thorium(29)
     * graphite(36) thorium(37) graphite(38)
     * </pre>
     * With the mod's default balance values this totals {@code 128}, matching the value produced
     * by the community wiki calculator for the same pattern — but the assertion below is derived
     * from the live config, not hardcoded, so it stays correct if the default balance changes:
     * <ul>
     *   <li>4 corner graphites, each with 2 thorium neighbors -&gt;
     *       {@code graphiteBase + 2 * (thoriumBase * graphiteProxy)} each</li>
     *   <li>4 edge thoriums, each with 2 graphite neighbors (ignored, cooler) + 1 uranium neighbor (fuel) -&gt;
     *       {@code thoriumBase + thoriumProxy} each</li>
     *   <li>1 center uranium, with 4 thorium neighbors (all fuel) -&gt;
     *       {@code uraniumBase + 4 * uraniumProxy}</li>
     * </ul>
     */
    @GameTest(template = STRUCTURE)
    public static void threeByThreeDiamond_matchesWikiCalculatorReferenceValue(GameTestHelper helper) {
        Item uranium = CNItems.URANIUM_ROD.get();
        Item thorium = CNItems.THORIUM_ROD.get();
        Item graphite = CNItems.GRAPHITE_ROD.get();

        int uraniumBase = CNConfigs.server().rods.uraniumBaseValue.get();
        float uraniumProxy = CNConfigs.server().rods.uraniumProximityBonus.get();
        int thoriumBase = CNConfigs.server().rods.baseValueThorium.get();
        float thoriumProxy = CNConfigs.server().rods.thoriumProxyBonus.get();
        int graphiteBase = CNConfigs.server().rods.graphiteBaseValue.get();
        float graphiteProxy = CNConfigs.server().rods.graphiteProximityMalus.getF();

        ReactorControllerInventory inventory = placeController(helper, new BlockPos(1, 1, 1));
        loadPattern(inventory, Map.ofEntries(
                Map.entry(18, graphite), Map.entry(19, thorium), Map.entry(20, graphite),
                Map.entry(27, thorium), Map.entry(28, uranium), Map.entry(29, thorium),
                Map.entry(36, graphite), Map.entry(37, thorium), Map.entry(38, graphite)
        ));

        ReactorDisplayState displayState = new ReactorDisplayState(
                Map.of(uranium, 1, thorium, 4, graphite, 4), List.of(), 0);

        double heat = CALCULATOR.computeHeat(null, null, inventory, /*overHeat*/ 0.0, displayState, helper.getLevel());

        double cornerGraphite = graphiteBase + 2 * (thoriumBase * graphiteProxy);
        double edgeThorium = thoriumBase + thoriumProxy;
        double centerUranium = uraniumBase + 4 * uraniumProxy;
        double expected = 4 * cornerGraphite + 4 * edgeThorium + centerUranium;

        helper.assertTrue(Math.abs(heat - expected) < DELTA,
                "3x3 diamond should match the wiki calculator's reference value for this pattern: expected "
                        + expected + " (128 with default balance), got " + heat);
        helper.succeed();
    }
}
