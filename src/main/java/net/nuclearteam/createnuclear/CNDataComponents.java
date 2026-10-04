package net.nuclearteam.createnuclear;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nuclearteam.createnuclear.content.biome.BiomeIrradiationExtractorItem;
import net.nuclearteam.createnuclear.content.equipment.cloth.ClothItem.ClothItemStack;
import net.nuclearteam.createnuclear.content.equipment.cloth.ClothItem.Cloths;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintData;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.UnaryOperator;

public class CNDataComponents {
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreateNuclear.MOD_ID);

    /**
     * Reactor heat stored on the blueprint stack, written by the controller on every tick.
     * <p>
     * Plain {@code Codec.FLOAT}, deliberately not {@code ExtraCodecs.POSITIVE_FLOAT}: zero is
     * the normal value for an idle or stopped reactor, and a persistent codec that can reject
     * a value crashes the world save rather than the write. That is exactly what happened with
     * {@code POSITIVE_FLOAT} ("Value must be positive: 0.0" while serializing the blueprint).
     * <p>
     * The non-negative invariant is enforced upstream instead, where it can't take a save down:
     * {@code DefaultHeatCalculator#computeHeat} floors its result at 0. This also matches the
     * Forge branch, which stores the value in a plain NBT double.
     */
    public static final DataComponentType<Float> HEAT = register(
            "heat",
            builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT)
    );

    public static final DataComponentType<ReactorBluePrintData> REACTOR_BLUE_PRINT_DATA = register(
            "reactor_blue_print_data",
            builder -> builder.persistent(ReactorBluePrintData.CODEC).networkSynchronized(ReactorBluePrintData.STREAM_CODEC)
    );

    public static final DataComponentType<Cloths> CLOTH_COLOR = register(
        "cloth_color",
        b -> b.persistent(StringRepresentable.fromEnum(Cloths::values)).networkSynchronized(NeoForgeStreamCodecs.enumCodec(Cloths.class))
    );

    public static final DataComponentType<ClothItemStack> CLOTH_ITEM = register(
        "cloth_item",
        b -> b.persistent(ClothItemStack.CODEC).networkSynchronized(ClothItemStack.STREAM_CODEC)
    );

    public static final DataComponentType<Integer> CHARGE_BIOME_IRRADIATION_EXTRACTOR = register(
        BiomeIrradiationExtractorItem.TAG,
        builder -> builder.persistent(Codec.INT)
    );

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DATA_COMPONENTS.register(name, () -> type);
        return type;
    }

    @ApiStatus.Internal
    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
