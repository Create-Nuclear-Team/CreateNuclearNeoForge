package net.nuclearteam.createnuclear.content.logistics;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.nuclearteam.createnuclear.api.ReactorFluidTypesValue;
import net.nuclearteam.createnuclear.api.multiblock.fluid.ReactorFluidType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class BigFluidStack {
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
        CompoundTag tag = new CompoundTag();
        // Utilisation de la méthode de sauvegarde moderne avec le provider de registres
        tag.put("Fluid", stack.save(registries, new CompoundTag()));
        tag.putInt("Amount", amount);
        return tag;
    }

    public static BigFluidStack read(HolderLookup.Provider registries, CompoundTag tag) {
        // Utilisation du codec/parseur moderne pour charger le FluidStack depuis le NBT
        FluidStack parsedStack = FluidStack.parse(registries, tag.getCompound("Fluid")).orElse(FluidStack.EMPTY);
        return new BigFluidStack(parsedStack, tag.getInt("Amount"));
    }

    public void send(RegistryFriendlyByteBuf buffer) {
        // Utilisation du StreamCodec natif de NeoForge pour le réseau
        FluidStack.STREAM_CODEC.encode(buffer, stack);
        buffer.writeVarInt(amount);
    }

    public boolean isInfinite() {
        return amount >= INF;
    }

    public static BigFluidStack receive(RegistryFriendlyByteBuf buffer) {
        FluidStack parsedStack = FluidStack.STREAM_CODEC.decode(buffer);
        return new BigFluidStack(parsedStack, buffer.readVarInt());
    }

    public static Comparator<? super BigFluidStack> comparator() {
        return (i1, i2) -> Integer.compare(i2.amount, i1.amount);
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

    public static List<BigFluidStack> duplicateWrappers(List<BigFluidStack> list) {
        List<BigFluidStack> copy = new ArrayList<>();
        for (BigFluidStack bigFluidStack : list)
            copy.add(new BigFluidStack(bigFluidStack.stack.copy(), bigFluidStack.amount));
        return copy;
    }

    public ReactorFluidType getFluidtype(@Nullable Level level) {
        if (level == null) {
            return ReactorFluidTypesValue.getReactorFluidType(this.stack.getFluid());
        }
        return ReactorFluidType.resolveReactorFluidType(this.stack.getFluid(), level);
    }
}