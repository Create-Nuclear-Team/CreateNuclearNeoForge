package net.nuclearteam.createnuclear.content.multiblock.bluePrintItem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record PatternData(int slot, ItemStack stack) {
    private static final ItemStack DEFAULT_STACK = ItemStack.EMPTY;

    /**
     * Uses {@link ItemStack#OPTIONAL_CODEC}, not {@link ItemStack#CODEC}: a reactor pattern is 57
     * slots and is almost never full, and {@code ItemStack.CODEC} refuses the empty stack
     * ("Item must not be minecraft:air" / "Value must be within range [1;99]: 0"). With the strict
     * codec, any blueprint sitting in a controller made the block entity throw at chunk-save time,
     * so the controller silently lost its blueprint on every world reload.
     * <p>
     * The encoding of non-empty stacks is identical between the two codecs — only the empty stack
     * changes, from "rejected" to {@code {}} — so blueprints already on disk still load.
     */
    public static final Codec<PatternData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("slot").forGetter(PatternData::slot),
            ItemStack.OPTIONAL_CODEC
                .fieldOf("Stack")
                .forGetter(PatternData::stack)
        ).apply(i, PatternData::new)
    );

    /**
     * Uses {@link ItemStack#OPTIONAL_STREAM_CODEC} for the same reason as {@link #CODEC}:
     * {@code ItemStack.STREAM_CODEC} throws {@code EncoderException("Empty ItemStack not allowed")}
     * on the empty stack, and this component is {@code networkSynchronized}, so an unfilled pattern
     * failed to encode as soon as the blueprint was sent to a client.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PatternData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, PatternData::slot,
                    ItemStack.OPTIONAL_STREAM_CODEC, PatternData::stack,
                    PatternData::new
            );

    public static ItemStack getDefaultStack() {
        return DEFAULT_STACK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatternData(int slot1, ItemStack stack1))) return false;
        return slot == slot1 && ItemStack.isSameItem(this.stack, stack1);
    }

    @Override
    public int hashCode() {
        return 31 * Integer.hashCode(slot) + (stack == null ? 0 : stack.getItem().hashCode());
    }
}
