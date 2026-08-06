package net.nuclearteam.createnuclear.infrastructure.worldgen.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.nuclearteam.createnuclear.CreateNuclear;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class PersistentIrradiatedZones extends SavedData {
    private static final String DATA = String.join("_", CreateNuclear.MOD_ID, "irradiated", "zones");

    private final Set<ChunkPos> chunks = new HashSet<>();

    public static PersistentIrradiatedZones get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), DATA);
    }

    private static SavedData.Factory<PersistentIrradiatedZones> factory() {
        return new SavedData.Factory<>(PersistentIrradiatedZones::new, PersistentIrradiatedZones::load, DataFixTypes.LEVEL);
    }

    public void addChunks(Iterable<ChunkPos> newChunks) {
        boolean changed = false;
        for (ChunkPos pos : newChunks) {
            changed |= chunks.add(pos);
        }

        if (changed) setDirty();
    }

    public boolean isInsideAnyZone(BlockPos pos) {
        return chunks.contains(new ChunkPos(pos));
    }

    public boolean containsChunk(ChunkPos pos) {
        return chunks.contains(pos);
    }

    public void removeChunk(ChunkPos pos) {
        if (chunks.remove(pos)) setDirty();
    }

    public static PersistentIrradiatedZones load(CompoundTag nbt, HolderLookup.Provider registries) {
        PersistentIrradiatedZones data = new PersistentIrradiatedZones();
        data.readFrom(nbt);

        return data;
    }

    public void readFrom(CompoundTag nbt) {
        chunks.clear();
        ListTag list = nbt.getList("chunks", Tag.TAG_COMPOUND);
        list.forEach(tag -> {
            CompoundTag e = (CompoundTag) tag;

            chunks.add(new ChunkPos(e.getInt("x"), e.getInt("z")));
        });
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        ListTag list = new ListTag();
        chunks.forEach(pos -> {
            CompoundTag e = new CompoundTag();
            e.putInt("x", pos.x);
            e.putInt("z", pos.z);
            list.add(e);
        });
        nbt.put("chunks", list);
        return nbt;
    }
}
