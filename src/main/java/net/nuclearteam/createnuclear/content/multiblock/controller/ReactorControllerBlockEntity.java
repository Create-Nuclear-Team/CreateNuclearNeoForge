package net.nuclearteam.createnuclear.content.multiblock.controller;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import lib.multiblock.SimpleMultiBlockAislePatternBuilder;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import net.nuclearteam.createnuclear.*;
import net.nuclearteam.createnuclear.content.logistics.BigFluidStack;
import net.nuclearteam.createnuclear.content.multiblock.CNMultiblock;
import net.nuclearteam.createnuclear.content.multiblock.IHeat;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.PatternData;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintData;
import net.nuclearteam.createnuclear.content.multiblock.controller.manager.*;
import net.nuclearteam.createnuclear.content.multiblock.input.fluid.FluidLockManager;
import net.nuclearteam.createnuclear.content.multiblock.input.fluid.PersistentFluidLocks;
import net.nuclearteam.createnuclear.content.multiblock.input.fluid.ReactorFluidInputEntity;
import net.nuclearteam.createnuclear.content.multiblock.input.item.ReactorInputEntity;
import net.nuclearteam.createnuclear.content.multiblock.output.ReactorOutput;
import net.nuclearteam.createnuclear.content.multiblock.output.ReactorOutputEntity;
import net.nuclearteam.createnuclear.content.multiblock.pattern.ReactorPattern;
import net.nuclearteam.createnuclear.foundation.advancement.CNAdvancementBehaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.nuclearteam.createnuclear.content.multiblock.CNMultiblock.*;
import static net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlock.ASSEMBLED;

@SuppressWarnings({"unused"})
public class ReactorControllerBlockEntity extends SmartBlockEntity implements IInteractionChecker, IHaveGoggleInformation {
    public boolean destroyed = false;
    public boolean created = false;
    public boolean test = true;
    public int speed = 16; // This is the result speed of the reactor, change this to change the total capacity

    public boolean sendUpdate;

    public ReactorControllerBlock controller;

    public ReactorControllerInventory inventory;


    //private boolean powered;
    public State powered = State.OFF;
    public float reactorPower;
    public float lastReactorPower;
    int overFlowHeatTimer = 0;
    int overFlowLimiter = 30;
    double overHeat = 0;
    public int baseUraniumHeat = 25;
    public int baseGraphiteHeat = -10;
    public int proximityUraniumHeat = 5;
    public int proximityGraphiteHeat = -5;
    public int maxUraniumPerGraphite = 3;
    public int graphiteTimer = 3600;
    public int uraniumTimer = 3600;
    public int tmpGraphiteTimer = graphiteTimer;
    public int tmpUraniumTimer = uraniumTimer;
    public int countUraniumRod;
    public int countGraphiteRod;
    public int heat;
    public double total;
    private boolean isTotal = false;
    public CompoundTag screen_pattern = new CompoundTag();
    public ItemStack configuredPattern;

    private ItemStack fuelItem;
    private ItemStack coolerItem;

    private final int[][] formattedPattern = new int[][]{
            {99,99,99,0,1,2,99,99,99},
            {99,99,3,4,5,6,7,99,99},
            {99,8,9,10,11,12,13,14,99},
            {15,16,17,18,19,20,21,22,23},
            {24,25,26,27,28,29,30,31,32},
            {33,34,35,36,37,38,39,40,41},
            {99,42,43,44,45,46,47,48,99},
            {99,99,49,50,51,52,53,99,99},
            {99,99,99,54,55,56,99,99,99}
    };
    private final int[][] offsets = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };

    /*
    FORGE ARGUMENTS PART
     */

    private final ReactorPattern pattern = new ReactorPattern();
    private int explosionCountdown = 0;
    private boolean isExploding = false;

    //private final ConsumptionCycleManager cycleManager = new ConsumptionCycleManager();
    private double liquidLife;

    private BigItemStack bigFuelItem;
    private BigItemStack bigCoolerItem;
    private List<BigFluidStack> bigFluidStack;

    private int reactorSize = 0;
    private String reactorFacing = "null";
    // les pos sont [xMin, xMax, yMin, yMax, zMin, zMax]
    private int[] reactorPos;
    // Vertical span (block Y) of the reactor frame columns, used to map the
    // fluid fill ratio onto the liquid actually drawn in the frame windows.
    private int frameColumnMinY = Integer.MAX_VALUE;
    private int frameColumnMaxY = Integer.MIN_VALUE;
    // Per-tick cache for the fluid drawn in the frame windows. Recomputed from the
    // live (synced) input tanks so the displayed fluid and its fill ratio always
    // come from the same source and stay consistent for every fluid type.
    private long frameFluidCacheTick = -1;
    private FluidStack frameFluidCache = FluidStack.EMPTY;
    private float frameFluidFillRatioCache = 0f;
    private boolean needsToResolveEntities = false;
    private double fluidBuffer = 0.0;

    private CNAdvancementBehaviour advancement;

    private final ReactorInputManagerI inputManager;
    private final ReactorOutputManagerI outputManager;
    private final ReactorInputFluidManagerI inputFluidManager;
    private final ReactorAlarmManagerI alarmManager;

    // Client Display Data (Synced via NBT)
    private Map<Item, Integer> clientDisplayItems = new HashMap<>();
    private List<BigFluidStack> clientDisplayFluids = new ArrayList<>();
    private long clientMaxFluidCapacity = 0;

    // services (dependencies) - abstracted behind interfaces to follow DIP
    //private final IHeatService heatService;
    //private final IPersistenceService persistenceService;

    // service fields are injected; implementations live in separate classes

    // --- Accessors used by external services (persistence) ---
    public ReactorControllerInventory getInventoryObject() {
        return this.inventory;
    }

    /*public void deserializeInventory(CompoundTag tag) {
        this.inventory.deserializeNBT(tag);
    }

    public CompoundTag serializeInventory() {
        return this.inventory.serializeNBT();
    }*/

    public ItemStack getConfiguredPattern() {
        return this.configuredPattern;
    }

    public void setConfiguredPattern(ItemStack stack) {
        this.configuredPattern = stack;
    }

    public CompoundTag getConfiguredPatternTag() {
        return this.configuredPattern.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
    }

    public int[] getMultiblockPos() {
        return this.reactorPos;
    }

    public BigItemStack getBigFuelItem() {
        return this.bigFuelItem;
    }

    public void setBigFuelItem(BigItemStack b) {
        this.bigFuelItem = b;
    }

    public BigItemStack getBigCoolerItem() {
        return this.bigCoolerItem;
    }

    public void setBigCoolerItem(BigItemStack b) {
        this.bigCoolerItem = b;
    }

    public List<BigFluidStack> getBigFluidStack() {
        return this.bigFluidStack;
    }

    public void setBigFluidStack(List<BigFluidStack> b) {
        this.bigFluidStack = b;
    }

    private void refreshFrameFluidCache() {
        if (level == null) return;
        long now = level.getGameTime();
        if (now == frameFluidCacheTick) return;
        frameFluidCacheTick = now;

        long amount = 0;
        long capacity = 0;
        FluidStack fluid = FluidStack.EMPTY;
        for (IFluidHandler handler : inputFluidManager.getFuildHandlers(level)) {
            int tanks = handler.getTanks();
            for (int t = 0; t < tanks; t++) {
                FluidStack stack = handler.getFluidInTank(t);
                capacity += handler.getTankCapacity(t);
                amount += stack.getAmount();
                if (fluid.isEmpty() && !stack.isEmpty()) fluid = stack.copy();
            }
        }

        frameFluidCache = fluid;
        frameFluidFillRatioCache = capacity > 0 ? Math.min(1f, (float) amount / (float) capacity) : 0f;
    }

    public FluidStack getDisplayedFluid() {
        refreshFrameFluidCache();
        return frameFluidCache;
    }

    /**
     * How full the reactor's fluid input is, in the range {@code [0, 1]}, used by
     * the frame renderer to size the visible liquid column.
     */
    public float getDisplayedFluidFillRatio() {
        refreshFrameFluidCache();
        return frameFluidFillRatioCache;
    }

    /** Records the lowest and highest frame block-Y of the assembled reactor. */
    public void setFrameColumn(int minY, int maxY) {
        if (this.frameColumnMinY == minY && this.frameColumnMaxY == maxY) return;
        this.frameColumnMinY = minY;
        this.frameColumnMaxY = maxY;
        notifyUpdate();
    }

    /** @return the lowest frame block-Y, or {@link Integer#MAX_VALUE} if unknown. */
    public int getFrameColumnMinY() {
        return frameColumnMinY;
    }

    /** @return the highest frame block-Y, or {@link Integer#MIN_VALUE} if unknown. */
    public int getFrameColumnMaxY() {
        return frameColumnMaxY;
    }

    public boolean hasFrameColumn() {
        return frameColumnMinY != Integer.MAX_VALUE && frameColumnMaxY != Integer.MIN_VALUE
                && frameColumnMaxY >= frameColumnMinY;
    }

    public int getMultiblockSize() {
        return this.reactorSize;
    }

    public void setMultiblockSize(int s) {
        this.reactorSize = s;
    }

    public String getMultiblockFacing() {
        return this.reactorFacing;
    }

    public void setMultiblockFacing(String f) {
        this.reactorFacing = f;
    }

    public CNAdvancementBehaviour getAdvancement() {
        return this.advancement;
    }

    public void setMultiblockStructure(int[] p) {
        this.reactorPos = p;
    }

    public double getLiquidLife() {
        return this.liquidLife;
    }

    public void setLiquidLife(double l) {
        this.liquidLife = l;
    }

    /**
     * Main constructor allowing dependency injection for testability and DIP
     * compliance.
     */
    public ReactorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.inventory = new ReactorControllerInventory(this);
        this.configuredPattern = ItemStack.EMPTY;

        this.inputManager = new ReactorInputManager();
        this.outputManager = new ReactorOutputManager();
        this.inputFluidManager = new ReactorInputFluidManager();
        this.alarmManager = new ReactorAlarmManager();

        this.bigFuelItem = new BigItemStack(ItemStack.EMPTY);
        this.bigCoolerItem = new BigItemStack(ItemStack.EMPTY);
        this.bigFluidStack = new ArrayList<>();

        //this.heatService = new DefaultHeatService(new HeatManager());
        //this.persistenceService = new DefaultPersistenceService();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    public boolean getAssembled() { // permet de savoir si le réacteur est formé ou pas.
        BlockState state = getBlockState();
        return state.getValue(ASSEMBLED);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(!configuredPattern.isEmpty()) {
            CreateLang.translate("gui.gauge.info_header").style(ChatFormatting.GRAY).forGoggles(tooltip);
            IHeat.HeatLevel.getName("reactor_controller").style(ChatFormatting.GRAY).forGoggles(tooltip);

            IHeat.HeatLevel.getFormattedHeatText(heat).forGoggles(tooltip);

            if (fuelItem.isEmpty()) {
                // if rod empty we initialize it at 1 (and display it as 0) to avoid having air item displayed instead of the rod
                IHeat.HeatLevel.getFormattedItemText(new ItemStack(CNItems.URANIUM_ROD.asItem(), 1), true).forGoggles(tooltip);
            } else {
                IHeat.HeatLevel.getFormattedItemText(fuelItem, false).forGoggles(tooltip);
            }

            if (fuelItem.isEmpty()) {
                // if rod empty we initialize it at 1 (and display it as 0) to avoid having air item displayed instead of the rod
                IHeat.HeatLevel.getFormattedItemText(new ItemStack(CNItems.GRAPHITE_ROD.asItem(), 1), true).forGoggles(tooltip);
            } else {
                IHeat.HeatLevel.getFormattedItemText(coolerItem, false).forGoggles(tooltip);
            }
        }

        return true;
    }

    //(Si les methode read et write ne sont pas implémenté alors lorsque l'on relance le monde minecraft les items dans le composant auront disparu !)
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) { //Permet de stocker les item 1/2
        if (!clientPacket) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        configuredPattern = ItemStack.EMPTY;
        if (tag.contains("configuredPattern")) {
            ItemStack.parse(registries, tag.getCompound("configuredPattern")).ifPresent(i -> configuredPattern = i);
        }

        coolerItem = ItemStack.EMPTY;
        if (tag.contains("coolerItem")) {
            ItemStack.parse(registries, tag.getCompound("coolerItem")).ifPresent(i -> coolerItem = i);
        }

        fuelItem = ItemStack.EMPTY;
        if (tag.contains("fuelItem")) {
            ItemStack.parse(registries, tag.getCompound("fuelItem")).ifPresent(i -> fuelItem = i);
        }

        total = tag.getDouble("total");
        heat = tag.getInt("heat");
        super.read(tag, registries, clientPacket);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) { //Permet de stocker les item 2/2
        if (!clientPacket) {
            compound.put("inventory", inventory.serializeNBT(registries));
        }

        if (configuredPattern != null) compound.put("configuredPattern", configuredPattern.saveOptional(registries));
        if (coolerItem != null) compound.put("coolerItem", coolerItem.saveOptional(registries));
        if (fuelItem != null) compound.put("fuelItem", fuelItem.saveOptional(registries));

        compound.putDouble("total", Double.isNaN(total) ? total : calculateProgress());
        compound.putInt("heat", heat);
        super.write(compound, registries, clientPacket);
    }

    public boolean isAssembled() {
        if (level == null)
            return false;
        try {
            return level.getBlockState(worldPosition).getValue(ASSEMBLED);
        } catch (Exception e) {
            return false;
        }
    }

    public void setAssembled(boolean assembled) {
        if (level == null)
            return;
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ASSEMBLED, assembled));
        this.setChanged();
    }

    public enum State {
        ON, OFF
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide)
            return;

        if (isEmptyConfiguredPattern()) {
            ReactorBluePrintData data = getReactorBluePrintData();
            countGraphiteRod = data.countGraphiteRod();
            countUraniumRod = data.countUraniumRod();

            if (!isTotal) {
                total = calculateProgress();
                isTotal = true;
            }
            BlockEntity blockEntity = level.getBlockEntity(getBlockPosForReactor('I'));

            if (blockEntity instanceof ReactorInputEntity be) {
                fuelItem = be.inventory.getStackInSlot(0);
                coolerItem = be.inventory.getStackInSlot(1);

                IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), Direction.NORTH.getOpposite());
                if (capability == null)
                    capability = EmptyItemHandler.INSTANCE;
                if (tmpUraniumTimer >= 0) {
                    tmpUraniumTimer -= 1 * countUraniumRod;
                } else {
                    ItemStack extractItem1 = capability.extractItem(0, 1, false);
                    tmpUraniumTimer = uraniumTimer;
                }
                if (tmpGraphiteTimer >= 0) {
                    tmpGraphiteTimer -= 1 * countGraphiteRod;
                } else {
                    ItemStack extractItem2 = capability.extractItem(1, 1, false);
                    tmpGraphiteTimer = graphiteTimer;
                }

                if (!fuelItem.isEmpty() && !coolerItem.isEmpty()) {
                    heat = (int) calculateHeat(inventory.getItem(0));
                    if (updateTimers()) {

                        if (IHeat.HeatLevel.of(heat) == IHeat.HeatLevel.SAFETY || IHeat.HeatLevel.of(heat) == IHeat.HeatLevel.CAUTION || IHeat.HeatLevel.of(heat) == IHeat.HeatLevel.WARNING) {
                            this.rotate(getBlockState(), new BlockPos(getBlockPos().getX(), getBlockPos().getY() + FindController('O').getY(), getBlockPos().getZ()), getLevel(), heat/4, true);
                            return;
                        } else {
                            EventTriggerPacket packet = new EventTriggerPacket(600);
                            CreateNuclear.LOGGER.warn("hum EventTriggerBlock ? {}", packet);
                            CatnipServices.NETWORK.sendToClientsAround((ServerLevel) level, getBlockPos(), 32, packet);

                            this.rotate(getBlockState(), new BlockPos(getBlockPos().getX(), getBlockPos().getY() + FindController('O').getY(), getBlockPos().getZ()), getLevel(), 0, false);
                            isTotal = false;
                            return;
                        }
                    } else {
                        this.rotate(getBlockState(), new BlockPos(getBlockPos().getX(), getBlockPos().getY() + FindController('O').getY(), getBlockPos().getZ()), getLevel(), 0, false);
                        isTotal = false;
                        return;
                    }
                }
                this.notifyUpdate();
            }
        }
    }

    private boolean isEmptyConfiguredPattern() {
        return !configuredPattern.isEmpty();// || !configuredPattern.getOrCreateTag().isEmpty();
    }

    private boolean updateTimers() {
        total -= 1;
        return total >= 0;//(total/constTotal) >= 0;
    }

    private ReactorBluePrintData getDefaultReactorBluePrintData() {
        return new ReactorBluePrintData(
                0, 0, 0, 0,
                new PatternData[0], new PatternData[0]
        );
    }
    private ReactorBluePrintData getReactorBluePrintData() {
        return configuredPattern.getOrDefault(CNDataComponents.REACTOR_BLUE_PRINT_DATA, getDefaultReactorBluePrintData());
    }

    private double calculateProgress() {
        ReactorBluePrintData data = getReactorBluePrintData();
        countGraphiteRod = data.countGraphiteRod();
        countUraniumRod = data.countUraniumRod();
        graphiteTimer = data.graphiteTime();
        uraniumTimer = data.uraniumTime();

        double progressGraphite = countGraphiteRod  > 0
                ? (double) graphiteTimer  / countGraphiteRod
                : 0.0;
        double progressUranium  = countUraniumRod > 0
                ? (double) uraniumTimer   / countUraniumRod
                : 0.0;

        double tmp = progressGraphite + progressUranium;

        return progressGraphite + progressUranium;
    }

    private double calculateHeat(ItemStack input) {
        ReactorBluePrintData data = input.getOrDefault(CNDataComponents.REACTOR_BLUE_PRINT_DATA, getDefaultReactorBluePrintData());

        countGraphiteRod = data.countGraphiteRod();
        countUraniumRod = data.countUraniumRod();
        heat = 0;

        // if more than maxUraniumPerGraphite of the rods are uranium, the reactor will overheat
        if (countUraniumRod > countGraphiteRod * maxUraniumPerGraphite) {
            overFlowHeatTimer++;
            if (overFlowHeatTimer >= overFlowLimiter) {
                overHeat++;
                overFlowHeatTimer = 0;
                if (overFlowLimiter > 2) {
                    overFlowLimiter--;
                }
            }
        } else {
            overFlowHeatTimer = 0;
            overFlowLimiter     = 30;
            overHeat = Math.max(0, overHeat - 2);
        }

        PatternData[] patternDataAll = data.patternAll();
        for (PatternData pd : patternDataAll) {
            char currentRod = '\0';
            ItemStack stack = pd.stack();

            if (stack.is(CNItems.URANIUM_ROD)) {
                heat += baseUraniumHeat;
                currentRod = 'u';
            } else if (stack.is(CNItems.GRAPHITE_ROD)) {
                heat += baseGraphiteHeat;
                currentRod = 'g';
            }

            if (currentRod != '\0') {
                pattern:
                for (int i = 0; i < formattedPattern.length; i++) {
                    for (int j = 0; j < formattedPattern[i].length; j++) {
                        if (formattedPattern[i][j] == pd.slot()) {
                            // the offsets for the four directions (down, up, right, left) is int[][] offsets = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} }; (defined at the top of the class)
                            for (int[] offset : offsets) {
                                int ni = i + offset[0], nj = j + offset[1];
                                if (ni < 0 || ni >= formattedPattern.length || nj < 0 || nj >= formattedPattern[i].length) continue;

                                int neighborSlot = formattedPattern[ni][nj];
                                for (PatternData pd2 : patternDataAll) {
                                    if (pd2.slot() == neighborSlot) {
                                        ItemStack stack2 = pd.stack();
                                        if (currentRod == 'u') {
                                            heat += stack2.is(CNItems.URANIUM_ROD) ? proximityUraniumHeat : proximityGraphiteHeat;
                                        }
                                        break;
                                    }
                                }
                            }
                            break pattern;
                        }
                    }
                }
            }
        }

        return heat + overHeat;
    }

    private BlockPos getBlockPosForReactor(char character) {
        BlockPos pos = FindController(character);
        BlockPos posController = getBlockPos();
        BlockPos posInput = new BlockPos(posController.getX(), posController.getY(), posController.getZ());

        int[][] directions = {
                {0,0, pos.getX()}, // NORTH
                {0,0, -pos.getX()}, // SOUTH
                {-pos.getX(),0,0}, // EAST
                {pos.getX(),0,0} // WEST
        };


        for (int[] direction : directions) {
            BlockPos newPos = posController.offset(direction[0], direction[1], direction[2]);
            if (level.getBlockState(newPos).is(CNBlocks.REACTOR_INPUT.get())) {
                posInput = newPos;
                break;
            }
        }

        return posInput;
    }

    private CompoundTag convertePattern(CompoundTag compoundTag) {
        ListTag pattern = compoundTag.getList("Items", Tag.TAG_COMPOUND);

        int[][] list = new int[][]{
                {99,99,99,0,1,2,99,99,99},
                {99,99,3,4,5,6,7,99,99},
                {99,8,9,10,11,12,13,14,99},
                {15,16,17,18,19,20,21,22,23},
                {24,25,26,27,28,29,30,31,32},
                {33,34,35,36,37,38,39,40,41},
                {99,42,43,44,45,46,47,48,99},
                {99,99,49,50,51,52,53,99,99},
                {99,99,99,54,55,56,99,99,99}
        };


        return null;
    }

    private static BlockPos FindController(char character) {
        return SimpleMultiBlockAislePatternBuilder.start()
                .aisle(AAAAA, AAAAA, AAAAA, AAAAA, AAAAA)
                .aisle(AABAA, ADADA, BACAB, ADADA, AABAA)
                .aisle(AABAA, ADADA, BACAB, ADADA, AABAA)
                .aisle(AAIAA, ADADA, BACAB, ADADA, AAAA)
                .aisle(AABAA, ADADA, BACAB, ADADA, AABAA)
                .aisle(AABAA, ADADA, BACAB, ADADA, AABAA)
                .aisle(AAAAA, AAAAA, AAAAA, AAAAA, AAOAA)
                .where('A', a -> a.getState().is(CNBlocks.REACTOR_CASING.get()))
                .where('B', a -> a.getState().is(CNBlocks.REACTOR_FRAME.get()))
                .where('C', a -> a.getState().is(CNBlocks.REACTOR_CORE.get()))
                .where('D', a -> a.getState().is(CNBlocks.REACTOR_COOLER.get()))
                .where('*', a -> a.getState().is(CNBlocks.REACTOR_CONTROLLER.get()))
                .where('O', a -> a.getState().is(CNBlocks.REACTOR_OUTPUT.get()))
                .where('I', a -> a.getState().is(CNBlocks.REACTOR_INPUT.get()))
                .getDistanceController(character);
    }

    public void rotate(BlockState state, BlockPos pos, Level level, int rotation, boolean isActif) {
        rotation = rotation > 0 ? rotation : heat/4;
        if (level.getBlockState(pos).is(CNBlocks.REACTOR_OUTPUT.get()) && rotation > 0 && isActif) {
            if (level.getBlockState(pos).getBlock() instanceof ReactorOutput block) {
                ReactorOutputEntity entity = block.getBlockEntityType().getBlockEntity(level, pos);
                if (state.getValue(ASSEMBLED)) { // Starting the energy
                    entity.speed = rotation;
                    entity.heat = rotation;
                } else { // stopping the energy
                    entity.speed = 0;
                    entity.heat = 0;
                }
                entity.updateSpeed = true;
                entity.updateGeneratedRotation();
                entity.setSpeed(rotation);

            }
        }
        else {
            if (level.getBlockState(pos).getBlock() instanceof ReactorOutput block) {
                ReactorOutputEntity entity = block.getBlockEntityType().getBlockEntity(level, pos);
                entity.setSpeed(0);
                entity.heat = 0;
                entity.updateSpeed = true;
                entity.updateGeneratedRotation();
            }
        }
    }

    @Deprecated
    public int[] getStructureBounds(BlockPos startPos, int structureSize, String facing) {
        int[] northOffsets5x5 = new int[] { -2, 2, -3, 3, 0, 4 };
        int[] northOffsets7x7 = new int[] { -3, 3, -4, 4, 0, 6 };
        int[] northOffsets9x9 = new int[] { -4, 4, -5, 5, 0, 8 };

        int[] eastOffsets5x5 = new int[] { -4, 0, -3, 3, -2, 2 };
        int[] eastOffsets7x7 = new int[] { -6, 0, -4, 4, -3, 3 };
        int[] eastOffsets9x9 = new int[] { -8, 0, -5, 5, -4, 4 };

        int[] southOffsets5x5 = new int[] { -2, 2, -3, 3, -4, 0 };
        int[] southOffsets7x7 = new int[] { -3, 3, -4, 4, -6, 0 };
        int[] southOffsets9x9 = new int[] { -4, 4, -5, 5, -8, 0 };

        int[] westOffsets5x5 = new int[] { 0, 4, -3, 3, -2, 2 };
        int[] westOffsets7x7 = new int[] { 0, 6, -4, 4, -3, 3 };
        int[] westOffsets9x9 = new int[] { 0, 8, -5, 5, -4, 4 };

        switch (facing) {
            case "north":
                switch (structureSize) {
                    case 5:
                        return applyOffset(startPos, northOffsets5x5);
                    case 7:
                        return applyOffset(startPos, northOffsets7x7);
                    case 9:
                        return applyOffset(startPos, northOffsets9x9);
                }
            case "east":
                switch (structureSize) {
                    case 5:
                        return applyOffset(startPos, eastOffsets5x5);
                    case 7:
                        return applyOffset(startPos, eastOffsets7x7);
                    case 9:
                        return applyOffset(startPos, eastOffsets9x9);
                }
            case "south":
                switch (structureSize) {
                    case 5:
                        return applyOffset(startPos, southOffsets5x5);
                    case 7:
                        return applyOffset(startPos, southOffsets7x7);
                    case 9:
                        return applyOffset(startPos, southOffsets9x9);
                }
            case "west":
                switch (structureSize) {
                    case 5:
                        return applyOffset(startPos, westOffsets5x5);
                    case 7:
                        return applyOffset(startPos, westOffsets7x7);
                    case 9:
                        return applyOffset(startPos, westOffsets9x9);
                }
            default:
                return new int[] { 0, 0, 0, 0, 0, 0 };
        }
    }

    @Deprecated
    private int[] applyOffset(BlockPos pos, int[] offset) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        return new int[] { x + offset[0], x + offset[1], y + offset[2], y + offset[3], z + offset[4], z + offset[5] };
    }

    public void addInput(BlockPos inputPos) {
        this.inputManager.addBlock(inputPos);
        this.setChanged();
    }

    public void removeInput(BlockPos inputPos) {
        this.inputManager.removeBlock(inputPos);
        this.setChanged();
    }

    public void addOutput(BlockPos outputPos) {
        this.outputManager.addBlock(outputPos);
        this.setChanged();
    }

    public void removeOutput(BlockPos outputPos) {
        this.outputManager.removeBlock(outputPos);
        this.setChanged();
    }

    public void addInputFluid(BlockPos outputPos) {
        this.inputFluidManager.addBlock(outputPos);
        this.setChanged();
    }

    public void removeInputFluid(BlockPos outputPos) {
        this.inputFluidManager.removeBlock(outputPos);
        this.setChanged();
    }

    public void addAlarm(BlockPos alarmPos) {
        this.alarmManager.addBlock(alarmPos);
        this.setChanged();
    }

    public void removeAlarm(BlockPos alarmPos) {
        this.alarmManager.removeBlock(alarmPos);
        this.setChanged();
    }

    public void removeIOAll() {
        this.inputManager.clearInvalid(level);
        this.outputManager.clearInvalid(level);
        this.inputFluidManager.clearInvalid(level);
        this.alarmManager.clearInvalid(level);
        this.setChanged();
    }

    /** Try to lock this controller to the given Fluid. Returns true if allowed. */
    public boolean tryLockFluid(Fluid fluid) {
        // server-persistent approach (preferred): use PersistentFluidLocks when on
        // server
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            return PersistentFluidLocks.get(serverLevel).tryLock(getBlockPos(), fluid);
        }
        // fallback to in-memory manager (single-server-run)
        return FluidLockManager.tryLock(getBlockPos(), fluid);
    }

    /** Returns whether the given FluidStack is acceptable for this controller. */
    public boolean canAcceptFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty())
            return true;
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            return PersistentFluidLocks.get(serverLevel).canAccept(getBlockPos(), stack.getFluid());
        }
        return FluidLockManager.canAccept(getBlockPos(), stack);
    }

    /** Force-clear the lock on this controller. */
    public void clearLock() {
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            PersistentFluidLocks.get(serverLevel).clearLock(getBlockPos());
        } else {
            FluidLockManager.clearLock(getBlockPos());
        }
        setChanged();
        sendData();
    }

    public void clearLockIfAllInputsEmpty() {
        if (level == null || level.isClientSide)
            return;

        final int SCAN_RADIUS = CNMultiblock.REGISTRATE_MULTIBLOCK.findStructure(level, getBlockPos(), this).data()
                .getSize(); // adapte selon la taille max du multiblock
        BlockPos center = getBlockPos();
        boolean anyNonEmpty = false;

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS && !anyNonEmpty; dx++) {
            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS && !anyNonEmpty; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS && !anyNonEmpty; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(p);
                    if (!(be instanceof ReactorFluidInputEntity))
                        continue;

                    IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, p, null);
                    if (handler == null)
                        continue;

                    for (int t = 0; t < handler.getTanks(); t++) {
                        if (!handler.getFluidInTank(t).isEmpty()) {
                            anyNonEmpty = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!anyNonEmpty)
            clearLock();
    }
}