package net.nuclearteam.createnuclear.infrastructure.config;

import net.createmod.catnip.config.ConfigBase;

public class CRods extends ConfigBase {
    public final ConfigInt uraniumRodLifetime = i(3600, 100, 5000, "Uranium rod lifespan", Comments.UraniumRodLifetime, Comments.hintTick);
    public final ConfigInt graphiteRodLifetime = i(3600, 100, 5000, "Graphite rod lifespan", Comments.GraphiteRodLifetime, Comments.hintTick);
    public final ConfigInt maxHeat = i(1000, 200, 1000, "Maximum reactor heat", Comments.maxHeat, Comments.hintHeat);

    @Override
    public String getName() {
        return "Rods";
    }

    private static class Comments {
        static String hintTick = "20 ticks = 1 second";
        static String UraniumRodLifetime = "Uranium rod lifespan in reactor";
        static String GraphiteRodLifetime = "Graphite rod lifespan in reactor";
        static String maxHeat = "Maximum heat a reactor block can handle";
        static String hintHeat = "Avoids reactor failure due to excessive heat";
    }
}
