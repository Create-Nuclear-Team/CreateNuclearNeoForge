package net.nuclearteam.createnuclear.content.multiblock.rod;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.CreateNuclearRegistries;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType;

import static net.nuclearteam.createnuclear.api.ItemRodTypesValue.DEFAULT_ROD_TYPE;

/**
 * Static registry of rod types ({@code RodType}) for the CreateNuclear mod.
 *
 * @see RodType
 * @see CreateNuclearRegistries
 */
public class CNRodTypes {
    /**
     * Bootstrap method invoked by the datapack system to register the rod types
     * ({@link ResourceKey}s) used by the multiblock (for example, fuel rods and
     * cooler rods). Add more entries here following the pattern below.
     *
     * <p>Example — Custom Rod:</p>
     * <pre>{@code
     * register(ctx, "name", new RodType.Builder()
     *     .baseRodHeat(int)
     *     .proximityRodHeat(int)
     *     .rodTimer(int)
     *     .item(Item)
     *     .fuelRodType()
     *     .build());
     * }</pre>
     */
    public static void bootstrap(BootstrapContext<RodType> ctx) {
        register(ctx, "fallback", DEFAULT_ROD_TYPE);
    }

    private static void register(BootstrapContext<RodType> ctx, String name, RodType type) {
        ctx.register(ResourceKey.create(CreateNuclearRegistries.ROD_TYPE, CreateNuclear.asResource(name)), type);
    }
}
