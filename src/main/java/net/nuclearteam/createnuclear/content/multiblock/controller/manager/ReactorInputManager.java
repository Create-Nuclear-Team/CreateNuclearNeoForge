package net.nuclearteam.createnuclear.content.multiblock.controller.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType.TypeRodPredicate;
import net.nuclearteam.createnuclear.content.multiblock.input.item.ReactorRodInputEntity;
import net.nuclearteam.createnuclear.content.multiblock.input.item.VirtualReactorInputsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for reactor input positions (`ReactorInput`).
 *
 * Serializes positions as x/y/z triplets and provides utilities to
 * obtain valid `IItemHandler` instances present at those positions.
 */
public class ReactorInputManager extends AbstractReactorIOManager implements ReactorInputManagerI {
    private static final String NBT_KEY = "ReactorInput";

    @Override
    public void write(CompoundTag compound) {
        ListTag list = new ListTag();
        for (BlockPos pos : positions) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            list.add(tag);
        }
        compound.put(NBT_KEY, list);
    }

    @Override
    public void read(CompoundTag compound) {
        positions.clear();
        if (!compound.contains(NBT_KEY)) return;
        ListTag list = compound.getList(NBT_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag tag = list.getCompound(i);
            positions.add(new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
        }
    }

    /**
     * Retrieves all `IItemHandler` instances located at the input positions,
     * resolved from the controller's current position.
     * Returns an empty list when no handlers are found.
     */
    @Override
    public List<IItemHandler> getItemHandlers(Level level, BlockPos controllerPos) {
        List<IItemHandler> handlers = new ArrayList<>();
        for (BlockPos offset : new ArrayList<>(positions)) {
            BlockPos p = controllerPos.offset(offset);
            if (level == null || !level.isLoaded(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (be == null) continue;
            IItemHandler cap = level.getCapability(Capabilities.ItemHandler.BLOCK, p, null);
            if (cap != null) {
                handlers.add(cap);
            }
        }

        return handlers;
    }

    @Override
    public VirtualReactorInputsItem getInventory(Level level, BlockPos controllerPos) {
        List<IItemHandler> handlers = this.getItemHandlers(level, controllerPos);
        if (handlers.isEmpty()) return new VirtualReactorInputsItem();

        int totalFuel = 0;
        int totalCooler = 0;
        for (IItemHandler h : handlers) {
            int slots = h.getSlots();
            for (int s = 0; s < slots; s++) {
                ItemStack st = h.getStackInSlot(s);

                if (TypeRodPredicate.isFuel(st, level)) totalFuel += st.getCount();
                else if (TypeRodPredicate.isCooled(st, level)) totalCooler += st.getCount();
            }
        }

        return new VirtualReactorInputsItem(totalFuel, totalCooler);
    }

    @Override
    public boolean extractItems(Level level, BlockPos controllerPos, int fuelNeeded, int coolerNeeded) {
        if (level == null) return false;
        List<IItemHandler> handlers = getItemHandlers(level, controllerPos);
        if (handlers.isEmpty()) return false;

        int fuelRemaining = fuelNeeded;
        int coolerRemaining = coolerNeeded;

        for (IItemHandler handler : handlers) {
            int slots = handler.getSlots();
            for (int s = 0; s < slots && (fuelRemaining > 0 || coolerRemaining > 0); s++) {
                ItemStack stack = handler.getStackInSlot(s);
                if (stack.isEmpty()) continue;


                if (fuelRemaining > 0 && TypeRodPredicate.isFuel(stack, level)) {
                    int toExtract = Math.min(fuelRemaining, stack.getCount());
                    handler.extractItem(s, toExtract, false);
                    fuelRemaining -= toExtract;
                } else if (coolerRemaining > 0 && TypeRodPredicate.isCooled(stack, level)) {
                    int toExtract = Math.min(coolerRemaining, stack.getCount());
                    handler.extractItem(s, toExtract, false);
                    coolerRemaining -= toExtract;
                }
            }
            if (fuelRemaining <= 0 && coolerRemaining <= 0) break;
        }

        return fuelRemaining <= 0 && coolerRemaining <= 0;
    }

    @Override
    public boolean extractItemByName(Level level, BlockPos controllerPos, String itemName) {
        if (level == null || itemName == null) return false;

        List<IItemHandler> handlers = getItemHandlers(level, controllerPos);
        if (handlers.isEmpty()) return false;

        for (IItemHandler handler : handlers) {
            int slots = handler.getSlots();
            for (int s = 0; s < slots; s++) {
                ItemStack stack = handler.getStackInSlot(s);
                if (stack.isEmpty()) continue;

                // On récupère le nom de l'item (ex: "uranium_rod")
                String registryPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

                // Comparaison intelligente : on ignore la casse et les underscores (_)
                if (isMatching(registryPath, itemName)) {
                    // On tente d'extraire 1 unité
                    ItemStack extracted = handler.extractItem(s, 1, false);

                    // Si l'extraction a réussi, on s'arrête là et on renvoie true
                    if (!extracted.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false; // On n'a pas trouvé l'item demandé
    }

    /**
     * Helper pour comparer "GraphiteRod" avec "graphite_rod"
     */
    private boolean isMatching(String registryPath, String configName) {
        String cleanPath = registryPath.replace("_", "").toLowerCase();
        String cleanConfig = configName.replace("_", "").toLowerCase();
        return cleanPath.equals(cleanConfig);
    }

    /**
     * Cleans up invalid positions (unloaded chunk, missing block entity,
     * or not a `Container`).
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
            if (be == null || !(be instanceof Container)) toRemove.add(offset);
        }

        positions.removeAll(toRemove);
    }

    @Override
    public List<BlockPos> getBlocksPosition(Level level, BlockPos controllerPos) {
        List<BlockPos> result = new ArrayList<>();

        for (BlockPos offset : positions) {
            BlockPos p = controllerPos.offset(offset);
            if (level.getBlockEntity(p) instanceof ReactorRodInputEntity) result.add(p);
        }
        return List.copyOf(result);
    }
}
