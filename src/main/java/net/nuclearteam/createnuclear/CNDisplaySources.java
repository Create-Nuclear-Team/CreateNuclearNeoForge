package net.nuclearteam.createnuclear;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.nuclearteam.createnuclear.content.redstone.displayLink.source.CoolerDisplaySource;
import net.nuclearteam.createnuclear.content.redstone.displayLink.source.FuelDisplaySource;
import net.nuclearteam.createnuclear.content.redstone.displayLink.source.HeatDisplaySource;
import net.nuclearteam.createnuclear.content.redstone.displayLink.source.LiquidLevelDisplaySource;
import net.nuclearteam.createnuclear.content.redstone.displayLink.source.ReactorSizeDisplaySource;
import net.nuclearteam.createnuclear.content.redstone.displayLink.source.ReactorSummaryDisplaySource;

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
     * Assumed divergence vs Forge: in 1.21 Registrate types its entries as
     * {@code RegistryEntry<R, T>} (registry type + entry type), whereas 1.20.1
     * only had {@code RegistryEntry<T>}.
     */
    private static <T extends DisplaySource> RegistryEntry<DisplaySource, T> simple(String name, Supplier<T> supplier) {
        return REGISTRATE.displaySource(name, supplier).register();
    }

    public static void register() {
    }
}
