package net.nuclearteam.createnuclear.compat.aeronotics.sable;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.compat.Mods;

public final class SableCompat {
    private SableCompat() {}

    /**
     * Converts a BlockPos that may be expressed in a Sable sub-level's local coordinate
     * space into the real, global position a player actually perceives.
     * Returns {@code localPos} unchanged if Sable isn't loaded, or if the position
     * isn't inside a sub-level.
     */
    public static BlockPos toGlobal(Level level, BlockPos localPos) {
        return Mods.SABLE.runIfInstalled(() -> () -> {
            Vec3 global = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(localPos));

            return BlockPos.containing(global);
        }).orElse(localPos);
    }
}
