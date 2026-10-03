package net.nuclearteam.createnuclear.api.multiblock;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RegistryTypeResolver {
    /**
     * Resolves a registry-backed type in 3 steps: an explicit registry lookup,
     * then a value-derived fallback, then a sentinel registry entry.
     *
     * @param lookup        result of the explicit registry lookup (e.g. {@code getTypeForFluid}/{@code getTypeForItem})
     * @param fallbackValue supplies the value-derived fallback if the lookup is empty
     * @param isNotEmpty    tests whether that fallback is a "real" (non-sentinel) value
     * @param registryAccess registry access used for the final fallback
     * @param registryKey   key of the registry holding {@code T}
     * @param fallbackKey   key of the sentinel/fallback entry within that registry
     */
    public static <T> T resolve(Optional<Holder.Reference<T>> lookup, Supplier<T> fallbackValue,
                                Predicate<T> isNotEmpty, RegistryAccess registryAccess,
                                ResourceKey<Registry<T>> registryKey, ResourceKey<T> fallbackKey) {
        return lookup
            .map(Holder.Reference::value)
            .orElseGet(() -> {
                T value = fallbackValue.get();
                return isNotEmpty.test(value)
                    ? value
                    : registryAccess.registryOrThrow(registryKey)
                      .getHolderOrThrow(fallbackKey).value();
            });
    }
}
