package net.nuclearteam.createnuclear.content.multiblock.input.item;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.nuclearteam.createnuclear.CNBlockEntityTypes;

import javax.annotation.Nullable;
import java.util.List;

public class ReactorRodInputEntity extends SmartBlockEntity implements MenuProvider {
    protected BlockPos block;

    public ReactorRodInputInventory inventory;


    public ReactorRodInputEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new ReactorRodInputInventory(this);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CNBlockEntityTypes.REACTOR_INPUT.get(),
                (be, context) -> be.inventory
        );
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) { }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if (!clientPacket) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        super.read(tag, registries, clientPacket);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if (!clientPacket) {
            tag.put("Inventory", inventory.serializeNBT(registries));
        }
        super.write(tag, registries, clientPacket);
    }


    @Nullable
    @Override
    public Level getLevel() {
        return super.getLevel();
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.createnuclear.reactor_input.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return ReactorRodInputMenu.create(i, inventory, this);
    }

    @Override
    public void tick() {
        super.tick();
    }

}
