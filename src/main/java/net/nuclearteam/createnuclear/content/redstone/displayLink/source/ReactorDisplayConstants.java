package net.nuclearteam.createnuclear.content.redstone.displayLink.source;

final class ReactorDisplayConstants {
    static final int MAX_FUEL = 64;
    static final int MAX_COOLER = 64;
    static final int MAX_FLUID = 16000;
    static final int MAX_HEAT = 1000;

    private static final int SIZE_SMALL_THRESHOLD = 5;
    private static final int SIZE_MEDIUM_THRESHOLD = 7;

    /** @return 1 (small), 2 (medium) or 3 (large) depending on the reactor's multiblock size. */
    static int sizeTier(int size) {
        return size <= SIZE_SMALL_THRESHOLD ? 1 : size <= SIZE_MEDIUM_THRESHOLD ? 2 : 3;
    }

    /** @return the {@code display_source.reactor.size.<key>} translation key suffix for a size tier. */
    static String sizeTierKey(int tier) {
        return tier == 1 ? "small" : tier == 2 ? "medium" : "large";
    }

    private ReactorDisplayConstants() {}
}
