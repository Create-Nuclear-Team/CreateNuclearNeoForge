package net.nuclearteam.createnuclear.foundation.advancement;

import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;

public class CNAdvancementBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<CNAdvancementBehaviour> TYPE = new BehaviourType<>();

    private UUID playerId;
    private final Set<CreateNuclearAdvancement> advancements;

    public CNAdvancementBehaviour(SmartBlockEntity be, CreateNuclearAdvancement ...advancement) {
        super(be);
        this.advancements = new HashSet<>();
        add(advancement);
    }

    public void add(CreateNuclearAdvancement ...advancement) {
        Collections.addAll(this.advancements, advancement);
    }

    public boolean isOwnerPresent() {
        return playerId != null;
    }

    public void setPlayer(UUID id) {
        Player player = getWorld().getPlayerByUUID(id);
        if (player == null) return;
        playerId = id;
        removeAwarded();
        blockEntity.setChanged();
    }

    @Override
    public void initialize() {
        super.initialize();
        removeAwarded();
    }

    private void removeAwarded() {
        Player player = getPlayer();
        if (player == null) return;

        advancements.removeIf(c -> c.isAlreadyAwardedTo(player));
        if (advancements.isEmpty()) {
            playerId = null;
            blockEntity.setChanged();
        }
    }

    public void awardPlayerIfNear(CreateNuclearAdvancement advancement, int maxDistance) {
        Player player = getPlayer();
        if (player == null)
            return;
        if (player.distanceToSqr(Vec3.atCenterOf(getPos())) > maxDistance * maxDistance)
            return;
        award(advancement, player);
    }

    public void awardPlayer(CreateNuclearAdvancement advancement) {
        Player player = getPlayer();
        if (player == null)
            return;
        award(advancement, player);
    }

    private void award(CreateNuclearAdvancement advancement, Player player) {
        if (!advancements.contains(advancement)) advancement.awardTo(player);
        removeAwarded();
    }

    private Player getPlayer() {
        if (playerId == null) return null;
        return getWorld().getPlayerByUUID(playerId);
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);

        if (playerId != null)
            nbt.putUUID("Owner", playerId);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);

        if (nbt.contains("Owner"))
            playerId = nbt.getUUID("Owner");
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public static void setPlacedBy(Level worldIn, BlockPos pos, LivingEntity placer) {
        CNAdvancementBehaviour behaviour = BlockEntityBehaviour.get(worldIn, pos, TYPE);
        if (behaviour == null)
            return;
        if (placer instanceof FakePlayer)
            return;
        if (placer instanceof ServerPlayer)
            behaviour.setPlayer(placer.getUUID());
    }
}
