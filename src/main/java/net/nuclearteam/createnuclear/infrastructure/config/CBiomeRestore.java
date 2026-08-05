package net.nuclearteam.createnuclear.infrastructure.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

@MethodsReturnNonnullByDefault
public class CBiomeRestore extends ConfigBase {
    public final ConfigBool restoreInCircle = b(true, "restore_in_circle", Comments.restoreInCircle);
    public final ConfigInt restoreRadiusChunks = i(4, 0, 32, "restore_radius_chunks", Comments.restoreRadiusChunks);

    @Override
    public String getName() {
        return "biomeRestore";
    }

    private static class Comments {
        static String restoreInCircle = "When enabled, restoring an irradiated biome also restores the surrounding chunks within restore_radius_chunks. When disabled, only the chunk being targeted is restored.";
        static String restoreRadiusChunks = "Radius, in chunks, of the circular area restored around the targeted chunk. Only used when restore_in_circle is enabled.";
    }
}