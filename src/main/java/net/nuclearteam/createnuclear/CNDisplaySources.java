package net.nuclearteam.createnuclear;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.nuclearteam.createnuclear.content.redstone.displayLink.source.*;

import java.util.function.Supplier;

public class CNDisplaySources {
    private static final CreateRegistrate REGISTRATE = CreateNuclear.REGISTRATE;

    public static final RegistryEntry<DisplaySource, HeatDisplaySource> HEAT = simple("heat", HeatDisplaySource::new);
    public static final RegistryEntry<DisplaySource, LiquidLevelDisplaySource> LIQUID_LEVEL = simple("liquid_level", LiquidLevelDisplaySource::new);
    public static final RegistryEntry<DisplaySource, ReactorSummaryDisplaySource> REACTOR_SUMMARY = simple("reactor_summary", ReactorSummaryDisplaySource::new);
    public static final RegistryEntry<DisplaySource, FuelDisplaySource> FUEL = simple("fuel", FuelDisplaySource::new);
    public static final RegistryEntry<DisplaySource, CoolerDisplaySource> COOLER = simple("cooler", CoolerDisplaySource::new);
    public static final RegistryEntry<DisplaySource, ReactorSizeDisplaySource> REACTOR_SIZE = simple("reactor_size", ReactorSizeDisplaySource::new);

    /**
     * Divergence assumee vs Forge : en 1.21 Registrate type ses entrees
     * {@code RegistryEntry<R, T>} (type du registre + type de l'entree) la ou 1.20.1
     * n'avait que {@code RegistryEntry<T>}.
     */
    private static <T extends DisplaySource> RegistryEntry<DisplaySource, T> simple(String name, Supplier<T> supplier) {
        return REGISTRATE.displaySource(name, supplier).register();
    }

    public static void register() {
    }
}
