package net.nuclearteam.createnuclear.content.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.nuclearteam.createnuclear.api.ReactorFluidTypesValue;
import net.nuclearteam.createnuclear.api.multiblock.fluid.ReactorFluidType;

import javax.annotation.Nullable;

public class BigFluidStack {
    public static final Codec<BigFluidStack> CODEC = RecordCodecBuilder.create(i -> i.group(
        FluidStack.OPTIONAL_CODEC.fieldOf("fluid_stack").forGetter(s -> s.stack),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("amount").forGetter(s -> s.amount)
    ).apply(i, BigFluidStack::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BigFluidStack> STREAM_CODEC = StreamCodec.composite(
        FluidStack.OPTIONAL_STREAM_CODEC, s -> s.stack,
        ByteBufCodecs.VAR_INT, s -> s.amount,
        BigFluidStack::new
    );

    public static final int INF = 1_000_000_000;

    public FluidStack stack;
    public int amount;

    public BigFluidStack(FluidStack stack) {
        this(stack, 1);
    }

    public BigFluidStack(FluidStack stack, int amount) {
        this.stack = stack;
        this.amount = amount;
    }

    public CompoundTag write(HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this)
            .getOrThrow();
    }

    public static BigFluidStack read(HolderLookup.Provider registries, CompoundTag tag) {
        return CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
            .getOrThrow();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof BigFluidStack other)
            return FluidStack.isSameFluidSameComponents(stack, other.stack) && amount == other.amount;
        return false;
    }

    @Override
    public int hashCode() {
        return (nullHash(stack) * 31) ^ Integer.hashCode(amount);
    }

    int nullHash(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    @Override
    public String toString() {
        return "(" + stack.getHoverName().getString() + " x" + amount + ")";
    }

    public ReactorFluidType getFluidtype(@Nullable Level level) {
        if (level == null) {
            return ReactorFluidTypesValue.getReactorFluidType(this.stack.getFluid());
        }
        return ReactorFluidType.resolveReactorFluidType(this.stack.getFluid(), level);
    }
}
