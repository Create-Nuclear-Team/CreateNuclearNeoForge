package net.nuclearteam.createnuclear.content.multiblock.bluePrintItem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType.TypeRodPredicate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record ReactorBluePrintData(int countCooledRod, int countFuelRod, PatternData[] pattern) {
    public static final ReactorBluePrintData EMPTY = new ReactorBluePrintData(0, 0, new PatternData[0]);

    private static final Codec<PatternData[]> PATTERN_ARRAY_CODEC = PatternData.CODEC.listOf().xmap(
        list -> list.toArray(PatternData[]::new),
        List::of
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, PatternData[]> STREAM_PATTERN_ARRAY_CODEC = CatnipStreamCodecBuilders.array(PatternData.STREAM_CODEC, PatternData.class);

    public static final Codec<ReactorBluePrintData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("countCooledRod").forGetter(ReactorBluePrintData::countCooledRod),
            Codec.INT.fieldOf("countFuelRod").forGetter(ReactorBluePrintData::countFuelRod),
            PATTERN_ARRAY_CODEC.fieldOf("pattern").forGetter(ReactorBluePrintData::pattern)
        ).apply(instance, ReactorBluePrintData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ReactorBluePrintData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ReactorBluePrintData::countCooledRod,
            ByteBufCodecs.INT, ReactorBluePrintData::countFuelRod,
            STREAM_PATTERN_ARRAY_CODEC, ReactorBluePrintData::pattern,
            ReactorBluePrintData::new
    );

    /**
     * Normalized view of {@link #pattern()}. Recomputed on demand, never stored.
     */
    public PatternData[] patternAll(Level level) {
        PatternData[] result = new PatternData[pattern().length];
        ItemStack glassPane = new ItemStack(Items.GLASS_PANE);

        for (int i = 0; i < pattern().length; i++) {
            ItemStack stack = pattern()[i].stack();
            boolean isRods = TypeRodPredicate.isFuel(stack, level) || TypeRodPredicate.isCooled(stack, level);

            result[i] = new PatternData(i, isRods ? stack : glassPane);
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReactorBluePrintData(int cooledRod, int fuelRod, PatternData[] pattern1))) return false;

        return countCooledRod == cooledRod
                && countFuelRod == fuelRod
                && Arrays.equals(pattern, pattern1);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(countCooledRod, countFuelRod);
        result = 31 * result + Arrays.hashCode(pattern);
        return result;
    }
}
