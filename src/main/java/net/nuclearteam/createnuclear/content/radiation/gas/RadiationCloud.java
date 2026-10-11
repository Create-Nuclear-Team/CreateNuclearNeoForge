package net.nuclearteam.createnuclear.content.radiation.gas;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;

public class RadiationCloud {
    private static final int MAX_VOLUME = 4096;           // blocs max pour un nuage
    private static final int EXPANSION_PER_UPDATE = 48;   // vitesse de fuite : blocs gagnés par update
    private static final int VOLUME_PER_STRENGTH = 40;    // force 15 => sature ~600 blocs
    private static final double FALLOFF = 0.06;           // atténuation par bloc de distance

    private final BlockPos origin;
    private int strength;  // 0..15
    private int volume;    // nombre de blocs que le gaz a le droit d'occuper actuellement

    public RadiationCloud(BlockPos origin) {
        this.origin = origin;
    }

    public void setStrength(int strength) {
        this.strength = Math.clamp(strength, 0, RadiationGasBlock.MAX_CONCENTRATION);
    }

    /** @return position (long) -> concentration voulue */
    public Long2IntOpenHashMap update(ServerLevel level) {
        Long2IntOpenHashMap result = new Long2IntOpenHashMap();
        if (strength <= 0) return result;

        volume = Math.min(volume + EXPANSION_PER_UPDATE, MAX_VOLUME);

        // BFS : du plus proche au plus loin => la zone autour de la source est remplie en priorité
        Long2IntOpenHashMap distances = new Long2IntOpenHashMap();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction dir : Direction.values())
            visit(level, origin.relative(dir), 1, distances, queue);

        while (!queue.isEmpty() && distances.size() < volume) {
            BlockPos pos = queue.poll();
            int d = distances.get(pos.asLong());
            for (Direction dir : Direction.values()) {
                if (distances.size() >= volume) break;
                visit(level, pos.relative(dir), d + 1, distances, queue);
            }
        }

        // Plus de voisins libres avant d'atteindre le budget = zone fermée.
        // On cale le volume dessus : si un mur casse plus tard, la fuite repart progressivement.
        if (queue.isEmpty()) volume = distances.size();

        int cells = distances.size();
        if (cells == 0) return result; // source totalement emmurée : rien ne sort

        // Même quantité de gaz répartie sur plus de blocs => dilution quand ça fuit
        double density = Math.min(1.0, (double) (strength * VOLUME_PER_STRENGTH) / cells);

        distances.long2IntEntrySet().fastForEach(e -> {
            double falloff = 1.0 / (1.0 + FALLOFF * e.getIntValue());
            int c = (int) Math.round(strength * density * falloff);
            if (c >= 1) result.put(e.getLongKey(), Math.min(c, RadiationGasBlock.MAX_CONCENTRATION));
        });
        return result;
    }

    private static void visit(ServerLevel level, BlockPos pos, int d, Long2IntOpenHashMap distances, ArrayDeque<BlockPos> queue) {
        long key = pos.asLong();
        if (distances.containsKey(key)) return;
        if (!level.isLoaded(pos)) return;                 // gère aussi la hauteur du monde
        BlockState state = level.getBlockState(pos);
        // WIP : tout bloc autre que l'air ou le gaz lui-même = 100% de résistance
        if (!state.isAir() && !(state.getBlock() instanceof RadiationGasBlock)) return;
        distances.put(key, d);
        queue.add(pos);
    }
}
