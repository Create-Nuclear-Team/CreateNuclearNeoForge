package net.nuclearteam.createnuclear.api.multiblock.rods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.CNTags.CNItemTags;
import net.nuclearteam.createnuclear.api.CreateNuclearRegistries;
import net.nuclearteam.createnuclear.api.ItemRodTypesValue;
import net.nuclearteam.createnuclear.content.multiblock.rod.CNRodTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents a rod type used by the mod's multiblock.
 * <p>
 * A {@code RodType} holds a set of items that can represent this rod type,
 * heat-related values, a timing value, and a {@link TypeRod} indicating the
 * category (fuel, cooler, or none).
 * <p>
 * Every numeric value is stored as a {@link Supplier} so it can be backed by a
 * live source such as a config option: the value is re-read on each accessor
 * call instead of being frozen at registration time (see {@code CNItems}).
 */
@MethodsReturnNonnullByDefault
public record RodType(HolderSet<Item> items,
                      Supplier<Integer> baseRodHeat,
                      Supplier<Float> proximityRodHeat,
                      Supplier<Integer> rodTimer,
                      Supplier<Integer> ratio,
                      TypeRod type) {

    public RodType(HolderSet<Item> items,
                   int baseRodHeat, float proximityRodHeat,
                   int rodTimer, TypeRod type) {
        this(items, baseRodHeat, proximityRodHeat, rodTimer, type, 1);
    }

    public RodType(HolderSet<Item> items,
                   int baseRodHeat, float proximityRodHeat,
                   int rodTimer, TypeRod type, int ratio) {
        this(items, () -> baseRodHeat, () -> proximityRodHeat, () -> rodTimer, () -> ratio, type);
    }

    /**
     * Serialization codec for saving/loading {@link RodType} instances.
     * <p>
     * {@code ratio} is optional and defaults to {@code 1} so datapacks written
     * before it existed keep loading unchanged.
     */
    public static final Codec<RodType> CODEC = RecordCodecBuilder.create(i -> i.group(
        RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(RodType::items),
        Codec.INT.fieldOf("baseRodHeat").forGetter(rt -> rt.baseRodHeat().get()),
        Codec.FLOAT.fieldOf("proximityRodHeat").forGetter(rt -> rt.proximityRodHeat().get()),
        Codec.INT.fieldOf("rodTimer").forGetter(rt -> rt.rodTimer().get()),
        StringRepresentable.fromEnum(TypeRod::values).fieldOf("type").forGetter(RodType::type),
        Codec.INT.optionalFieldOf("ratio", 1).forGetter(rt -> rt.ratio().get())
    ).apply(i, RodType::new));

    /**
     * Searches the {@code ROD_TYPE} registry for a {@link RodType} matching the
     * provided {@link Item}.
     *
     * @param registryAccess registry access for the world
     * @param item           the item to look up
     * @return an {@link Optional} containing a {@link Reference} to the matching
     *         {@code RodType} if found, otherwise {@link Optional#empty()}
     */
    public static Optional<Reference<RodType>> getTypeForItem(RegistryAccess registryAccess, Item item) {
        return registryAccess.lookupOrThrow(CreateNuclearRegistries.ROD_TYPE)
            .listElements()
            .filter(ref -> ref.value().items.contains(item.builtInRegistryHolder()))
            .findFirst();
    }

    /**
     * Resolves the {@code RodType} for a given {@link Item}. The method first
     * checks the world's registries, then falls back to {@link ItemRodTypesValue},
     * and lastly returns a fallback registry value if none is found.
     *
     * @param item  the item whose rod type should be resolved
     * @param world the world (level) used to access registries
     * @return the resolved {@code RodType} (never {@code null})
     */
    public static RodType resolveRodType(Item item, Level world) {
        return RodType.getTypeForItem(world.registryAccess(), item)
            .map(Reference::value)
            .orElseGet(() -> {
                RodType fromItem = ItemRodTypesValue.getRodType(item);
                return fromItem.isNotEmptyItem()
                    ? fromItem
                    : world.registryAccess()
                        .registryOrThrow(CreateNuclearRegistries.ROD_TYPE)
                        .getHolderOrThrow(CNRodTypes.FALLBACK)
                        .value();
            });
    }

    /**
     * Returns whether this {@code RodType} has no associated items.
     *
     * @return {@code true} if at least one item is defined, {@code false} otherwise
     */
    public boolean isNotEmptyItem() {
        return this.items.size() >= 1;
    }

    /**
     * Fluent builder for creating an immutable {@link RodType} instance.
     * <p>
     * Every numeric value ({@code baseRodHeat}, {@code proximityRodHeat},
     * {@code rodTimer}, {@code ratio}) can be set either as a fixed primitive
     * (evaluated once, at build time) or as a {@link Supplier} (re-evaluated on
     * every call to the corresponding accessor). The primitive overloads are
     * pure convenience: {@code baseRodHeat(5)} is equivalent to
     * {@code baseRodHeat(() -> 5)}. Use the {@link Supplier} overloads to back a
     * value with a live source such as a config option, so changes are picked up
     * without rebuilding the {@code RodType}.
     */
    public static class Builder {
        private final List<Holder<Item>> items = new ArrayList<>();
        private Supplier<Integer> baseRodHeat = () -> 0;
        private Supplier<Float> proximityRodHeat = () -> 0f;
        private Supplier<Integer> rodTimer = () -> 0;
        private Supplier<Integer> ratio = () -> 1;
        private TypeRod type = TypeRod.FUEL;

        private boolean itemsSet = false;
        private boolean baseRodHeatSet = false;
        private boolean proximityRodHeatSet = false;
        private boolean rodTimerSet = false;
        private boolean typeSet = false;

        /**
         * Sets the base heat for the rod.
         *
         * @param baseRodHeat base heat value
         * @return this builder
         */
        public Builder baseRodHeat(int baseRodHeat) {
            return baseRodHeat(() -> baseRodHeat);
        }

        /**
         * Sets the base heat for the rod, backed by a live source.
         *
         * @param baseRodHeat supplier of the base heat value
         * @return this builder
         */
        public Builder baseRodHeat(Supplier<Integer> baseRodHeat) {
            this.baseRodHeat = baseRodHeat;
            this.baseRodHeatSet = true;
            return this;
        }

        /**
         * Sets the heat contributed by nearby rods.
         *
         * @param proximityRodHeat proximity heat value
         * @return this builder
         */
        public Builder proximityRodHeat(float proximityRodHeat) {
            return proximityRodHeat(() -> proximityRodHeat);
        }

        /**
         * Sets the heat contributed by nearby rods, backed by a live source.
         *
         * @param proximityRodHeat supplier of the proximity heat value
         * @return this builder
         */
        public Builder proximityRodHeat(Supplier<Float> proximityRodHeat) {
            this.proximityRodHeat = proximityRodHeat;
            this.proximityRodHeatSet = true;
            return this;
        }

        /**
         * Sets the timer (duration) for the rod's behavior.
         *
         * @param rodTimer duration in ticks or mod-specific units
         * @return this builder
         */
        public Builder rodTimer(int rodTimer) {
            return rodTimer(() -> rodTimer);
        }

        /**
         * Sets the timer (duration) for the rod's behavior, backed by a live source.
         *
         * @param rodTimer supplier of the duration
         * @return this builder
         */
        public Builder rodTimer(Supplier<Integer> rodTimer) {
            this.rodTimer = rodTimer;
            this.rodTimerSet = true;
            return this;
        }

        /**
         * Sets the weight this rod carries when resolving the reactor's
         * fuel/cooler thermal balance (see {@code HeatBalance}).
         *
         * @param ratio weight of a single rod of this type
         * @return this builder
         */
        public Builder ratio(int ratio) {
            return ratio(() -> ratio);
        }

        /**
         * Sets the thermal balance weight, backed by a live source.
         *
         * @param ratio supplier of the weight
         * @return this builder
         */
        public Builder ratio(Supplier<Integer> ratio) {
            this.ratio = ratio;
            return this;
        }

        /**
         * Sets the rod type to {@link TypeRod#COOLER}.
         *
         * @return this builder
         */
        public Builder coolerRodType() {
            this.type = TypeRod.COOLER;
            this.typeSet = true;
            return this;
        }

        /**
         * Sets the rod type to {@link TypeRod#FUEL}.
         *
         * @return this builder
         */
        public Builder fuelRodType() {
            this.type = TypeRod.FUEL;
            this.typeSet = true;
            return this;
        }

        /**
         * Adds one or more items that represent this rod type.
         *
         * @param items array of {@link ItemLike} elements to associate
         * @return this builder
         */
        public Builder addItems(ItemLike... items) {
            for (ItemLike provider : items)
                this.items.add(provider.asItem().builtInRegistryHolder());
            if (items.length > 0) this.itemsSet = true;
            return this;
        }

        /**
         * Builds the immutable {@link RodType} instance.
         *
         * @throws IllegalStateException if required fields are missing
         * @return the created instance
         */
        public RodType build() {
            List<String> missing = new ArrayList<>();
            if (!itemsSet || items.isEmpty()) missing.add("items");
            if (!typeSet) missing.add("type");
            if (!baseRodHeatSet) missing.add("baseRodHeat");
            if (!proximityRodHeatSet) missing.add("proximityRodHeat");
            if (!rodTimerSet) missing.add("rodTimer");

            if (!missing.isEmpty())
                throw new IllegalStateException("Missing required RodType fields: " + String.join(", ", missing));

            return new RodType(HolderSet.direct(items), baseRodHeat, proximityRodHeat, rodTimer, ratio, type);
        }
    }

    public enum TypeRod implements StringRepresentable {
        /**
         * Fuel rod: generates heat via reaction.
         */
        FUEL,
        /**
         * Cooler rod: reduces or absorbs heat.
         */
        COOLER,
        /**
         * Sentinel value for the fallback/default rod type — no real category.
         */
        NONE
        ;

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public static final class TypeRodPredicate {
        public static final Predicate<ItemStack> IS_NOT_NULL = Objects::nonNull;

        public static final Predicate<ItemStack> IS_FUEL = s -> {
            TypeRod type = ItemRodTypesValue.getRodType(s.getItem()).type;
            return IS_NOT_NULL.test(s) && (s.is(CNItemTags.FUEL.tag) || type == TypeRod.FUEL);
        };

        public static final Predicate<ItemStack> IS_COOLED = s -> {
            TypeRod type = ItemRodTypesValue.getRodType(s.getItem()).type;
            return IS_NOT_NULL.test(s) && (s.is(CNItemTags.COOLER.tag) || type == TypeRod.COOLER);
        };

        /** @return whether the given resolved rod type is a fuel rod. */
        public static boolean isFuel(RodType rodType) {
            return rodType != null && rodType.type() == TypeRod.FUEL;
        }

        /** @return whether the given resolved rod type is a cooler rod. */
        public static boolean isCooled(RodType rodType) {
            return rodType != null && rodType.type() == TypeRod.COOLER;
        }

        /**
         * Level-aware variant of {@link #IS_FUEL}: resolves the rod type through
         * the world registries first, so datapack-defined rod types are honoured.
         */
        public static boolean isFuel(ItemStack stack, Level level) {
            if (!IS_NOT_NULL.test(stack) || stack.isEmpty()) return false;
            if (stack.is(CNItemTags.FUEL.tag)) return true;
            return isFuel(resolveRodType(stack.getItem(), level));
        }

        /**
         * Level-aware variant of {@link #IS_COOLED}: resolves the rod type through
         * the world registries first, so datapack-defined rod types are honoured.
         */
        public static boolean isCooled(ItemStack stack, Level level) {
            if (!IS_NOT_NULL.test(stack) || stack.isEmpty()) return false;
            if (stack.is(CNItemTags.COOLER.tag)) return true;
            return isCooled(resolveRodType(stack.getItem(), level));
        }

        public static String tooltipKey(ItemStack stack) {
            if (!IS_NOT_NULL.test(stack)) return "unknown";
            if (IS_FUEL.test(stack)) return "fuel";
            if (IS_COOLED.test(stack)) return "cooled";
            return "unknown";
        }

        public static String tooltipKey(ItemStack stack, Level level) {
            if (!IS_NOT_NULL.test(stack)) return "unknown";
            if (isFuel(stack, level)) return "fuel";
            if (isCooled(stack, level)) return "cooled";
            return "unknown";
        }
    }

    @Override
    public String toString() {
        return "RodType [items: " + this.items() +
                ", baseRodHeat: " + this.baseRodHeat().get() +
                ", proximityRodHeat: " + this.proximityRodHeat().get() +
                ", rodTimer: " + this.rodTimer().get() +
                ", ratio: " + this.ratio().get() +
                ", type: " + this.type() + "]";
    }
}
