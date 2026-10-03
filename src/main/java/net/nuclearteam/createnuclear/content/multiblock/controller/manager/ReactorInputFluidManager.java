package net.nuclearteam.createnuclear.content.multiblock.controller.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.multiblock.input.fluid.ReactorFluidInputEntity;
import net.nuclearteam.createnuclear.content.multiblock.input.fluid.VirtualReactorInputFluid;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager that tracks fluid input blocks for a reactor.
 * Responsible for serializing tracked positions, validating
 * handlers, reporting aggregated inventory and extracting fluids.
 */
public class ReactorInputFluidManager extends AbstractReactorIOManager implements ReactorInputFluidManagerI {
    @Override
    protected String nbtKey() {
        return "ReactorInputFluid";
    }

    /**
     * Remove any tracked positions that are no longer valid in the given level.
     */
    @Override
    public void clearInvalid(Level level, BlockPos controllerPos) {
        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos offset : positions) {
            BlockPos p = controllerPos.offset(offset);
            if (level == null || !level.isLoaded(p)) {
                toRemove.add(offset);
                continue;
            }

            BlockEntity be = level.getBlockEntity(p);
            if (be == null) {
                toRemove.add(offset);
                continue;
            }

            IFluidHandler cap = level.getCapability(Capabilities.FluidHandler.BLOCK, p, null);
            if (cap == null) toRemove.add(offset);
        }

        positions.removeAll(toRemove);
    }

    /**
     * Return an immutable list of tracked block positions that correspond
     * to fluid input entities in the given level.
     */
    @Override
    public List<BlockPos> getBlocksPosition(Level level, BlockPos controllerPos) {
        return filterByType(level, controllerPos, ReactorFluidInputEntity.class);
    }

    /**
     * Collect and return fluid handler capabilities for all tracked positions.
     */
    @Override
    public List<IFluidHandler> getFuildHandlers(Level level, BlockPos controllerPos) {
        List<IFluidHandler> handlers = new ArrayList<>();
        for (BlockPos offset : new ArrayList<>(positions)) {
            BlockPos p = controllerPos.offset(offset);
            if (level == null || !level.isLoaded(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (be == null) continue;
            IFluidHandler cap = level.getCapability(Capabilities.FluidHandler.BLOCK, p, null);
            if (cap != null) {
                handlers.add(cap);
            }
        }

        return handlers;
    }

    /**
     * Build and return a virtual aggregated inventory of all input fluids.
     */
    @Override
    public VirtualReactorInputFluid getInventory(Level level, BlockPos controllerPos) {
        VirtualReactorInputFluid virtualReactorInputFluid = new VirtualReactorInputFluid();
        List<IFluidHandler> handlers = this.getFuildHandlers(level, controllerPos);
        if (handlers.isEmpty()) return new VirtualReactorInputFluid();

        for (IFluidHandler h : handlers) {
            virtualReactorInputFluid.addFluid(h.getFluidInTank(0));
        }

        return virtualReactorInputFluid;
    }

    /**
     * Extracts up to {@code fluidNeeded} units of fluid, spread across the tracked handlers.
     * <p>
     * The requested amount is a TOTAL across every input, not a per-input quota: each handler
     * only ever drains what is still missing, so a request of 10 units against two inputs
     * holding 10 each removes 10 in total, not 20.
     *
     * @return {@code true} if at least one unit was actually drained. A partial extraction still
     *         returns {@code true}: the reactor consumes whatever coolant it can reach rather
     *         than refusing to run.
     */
    @Override
    public boolean extractFluids(Level level, BlockPos controllerPos, int fluidNeeded) {
        if (level == null || fluidNeeded <= 0) return false;
        List<IFluidHandler> handlers = getFuildHandlers(level, controllerPos);
        if (handlers.isEmpty()) return false;

        int remaining = fluidNeeded;

        for (IFluidHandler handler : handlers) {
            if (remaining <= 0) break;

            FluidStack stack = handler.getFluidInTank(0);
            if (stack.isEmpty()) continue;

            int toExtract = Math.min(remaining, stack.getAmount());
            if (toExtract <= 0) continue;

            // Subtract what was actually drained, not what was asked for: a handler is free to
            // hand back less than requested.
            remaining -= handler.drain(toExtract, FluidAction.EXECUTE).getAmount();
        }

        return remaining < fluidNeeded;
    }
}
