package net.nuclearteam.createnuclear.content.radiation.gas;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.nuclearteam.createnuclear.CNBlocks;
import net.nuclearteam.createnuclear.CreateNuclear;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID)
public class RadiationGasManager {
    private static final int UPDATE_INTERVAL = 20;
    // UPDATE_KNOWN_SHAPE : pas d'updates de forme en cascade sur les voisins
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Map<ResourceKey<Level>, RadiationGasManager> MANAGERS = new HashMap<>();

    private final Map<BlockPos, RadiationCloud> clouds = new HashMap<>();
    private final Long2IntOpenHashMap placed = new Long2IntOpenHashMap();

    public static RadiationGasManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level.dimension(), k -> new RadiationGasManager());
    }

    public void setSource(BlockPos pos, int strength) {
        clouds.computeIfAbsent(pos.immutable(), RadiationCloud::new).setStrength(strength);
    }

    public void removeSource(BlockPos pos) {
        clouds.remove(pos);
    }

    public boolean isTracked(BlockPos pos) {
        return placed.containsKey(pos.asLong());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % UPDATE_INTERVAL == 0)
            get(level).update(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) MANAGERS.remove(level.dimension());
    }

    private void update(ServerLevel level) {
        if (clouds.isEmpty() && placed.isEmpty()) return;

        // Plusieurs sources qui se chevauchent : on garde la concentration la plus forte
        Long2IntOpenHashMap target = new Long2IntOpenHashMap();
        for (RadiationCloud cloud : clouds.values())
            cloud.update(level).long2IntEntrySet().fastForEach(e -> target.merge(e.getLongKey(), e.getIntValue(), Math::max));

        BlockState gas = CNBlocks.RADIATION_GAS.getDefaultState();

        // Retirer le gaz qui n'est plus atteint (on ne touche pas à un bloc posé par le joueur entre temps)
        placed.keySet().forEach(key -> {
            if (target.containsKey(key)) return;
            BlockPos pos = BlockPos.of(key);
            if (level.getBlockState(pos).is(CNBlocks.RADIATION_GAS.get()))
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        });

        // Poser / mettre à jour seulement ce qui change
        target.long2IntEntrySet().fastForEach(e -> {
            BlockPos pos = BlockPos.of(e.getLongKey());
            BlockState wanted = gas.setValue(RadiationGasBlock.CONCENTRATION, e.getIntValue());
            if (level.getBlockState(pos) != wanted) level.setBlock(pos, wanted, FLAGS);
        });

        placed.clear();
        placed.putAll(target);
    }
}
