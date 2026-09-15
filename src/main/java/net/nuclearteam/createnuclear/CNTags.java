package net.nuclearteam.createnuclear;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.function.Function;

import static net.nuclearteam.createnuclear.CNTags.NameSpace.*;

public class CNTags {
    public static <T> TagKey<T> optionalTag(Registry<T> registry, ResourceLocation id) {
        return TagKey.create(registry.key(), id);
    }

    public static <T> TagKey<T> neoForgeTag(Registry<T> registry, String path) {
        return optionalTag(registry, ResourceLocation.fromNamespaceAndPath(NEO_FORGE.id, path));
    }

    private static <T> TagKey<T> resolve(Registry<T> registry, NameSpace nameSpace, String path, boolean optional, Function<ResourceLocation, TagKey<T>> nonOptionalFactory) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(nameSpace.id, path);

        return optional ? optionalTag(registry, id) : nonOptionalFactory.apply(id);
    }

    public static TagKey<Block> neoForgeBlockTag(String path) {
        return neoForgeTag(BuiltInRegistries.BLOCK, path);
    }

    public static TagKey<Item> neoForgeItemTag(String path) {
        return neoForgeTag(BuiltInRegistries.ITEM, path);
    }

    public static TagKey<Fluid> neoForgeFluidTag(String path) {
        return neoForgeTag(BuiltInRegistries.FLUID, path);
    }

    public enum NameSpace {
        MOD(CreateNuclear.MOD_ID, false, true),
        COMMON("c"),
        CREATE("create"),
        NEO_FORGE(COMMON.id),
        MINECRAFT("minecraft")
        ;

        public final String id;
        public final boolean optionalDefault;
        public final boolean alwaysDatagenDefault;

        NameSpace(String id) {
            this(id, true, false);
        }

        NameSpace(String id, boolean optionalDefault, boolean alwaysDatagenDefault) {
            this.id = id;
            this.optionalDefault = optionalDefault;
            this.alwaysDatagenDefault = alwaysDatagenDefault;
        }
    }

    public enum CNBlockTags {
        FAN_PROCESSING_CATALYSTS_ENRICHED(MOD, "fan_processing_catalysts/enriched"),
        FAN_PROCESSING_CATALYSTS_SNOW_POWDER(MOD, "fan_processing_catalysts/snow_powder"),
        ENRICHING_FIRE_BASE_BLOCKS,
        ;

        public final TagKey<Block> tag;
        public final boolean alwaysDatagen;

        CNBlockTags() {
            this(MOD);
        }

        CNBlockTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        CNBlockTags(NameSpace nameSpace, String path) {
            this(nameSpace, path, nameSpace.optionalDefault, nameSpace.alwaysDatagenDefault);
        }

        CNBlockTags(NameSpace nameSpace, boolean optional, boolean alwaysDatagenDefault) {
            this(nameSpace, null, optional, alwaysDatagenDefault);
        }

        CNBlockTags(NameSpace nameSpace, String path, boolean optional, boolean alwaysDatagenDefault) {
            this.tag = resolve(BuiltInRegistries.BLOCK, nameSpace, path == null ? Lang.asId(name()) : path, optional, BlockTags::create);
            this.alwaysDatagen = alwaysDatagenDefault;
        }

        public boolean matches(BlockState state) {
            return state.is(tag);
        }
    }

    public enum CNItemTags {
        CLOTH,
        FUEL,
        COOLER,
        ANTI_RADIATION_ARMOR,
        ANTI_RADIATION_HELMET,
        THORIUM_ORES,
        ;

        public final TagKey<Item> tag;
        public final boolean alwaysDatagen;

        CNItemTags() {
            this(MOD);
        }

        CNItemTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        CNItemTags(NameSpace nameSpace, boolean optional, boolean alwaysDatagenDefault) {
            this(nameSpace, null, optional, alwaysDatagenDefault);
        }

        CNItemTags(NameSpace nameSpace, String path, boolean optional, boolean alwaysDatagenDefault) {
            this.tag = resolve(BuiltInRegistries.ITEM, nameSpace, path == null ? Lang.asId(name()) : path, optional, ItemTags::create);
            this.alwaysDatagen = alwaysDatagenDefault;
        }
    }

    public enum CNFluidTags {
        URANIUM,
        THORIUM,
        NITROGEN
        ;

        public final TagKey<Fluid> tag;
        public final boolean alwaysDatagen;

        CNFluidTags() {
            this(MOD);
        }

        CNFluidTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        CNFluidTags(NameSpace nameSpace, boolean optional, boolean alwaysDatagenDefault) {
            this(nameSpace, null, optional, alwaysDatagenDefault);
        }

        CNFluidTags(NameSpace nameSpace, String path, boolean optional, boolean alwaysDatagenDefault) {
            this.tag = resolve(BuiltInRegistries.FLUID, nameSpace, path == null ? Lang.asId(name()) : path, optional, FluidTags::create);
            this.alwaysDatagen = alwaysDatagenDefault;
        }
    }

    public enum CNEntityTags {
        IRRADIATED_IMMUNE
        ;

        public final TagKey<EntityType<?>> tag;
        public final boolean alwaysDatagen;

        CNEntityTags() {
            this(MOD);
        }

        CNEntityTags(NameSpace nameSpace) {
            this(nameSpace, nameSpace.optionalDefault, nameSpace.alwaysDatagenDefault);
        }

        CNEntityTags(NameSpace nameSpace, boolean optional, boolean alwaysDatagenDefault) {
            this(nameSpace, null, optional, alwaysDatagenDefault);
        }

        CNEntityTags(NameSpace nameSpace, String path, boolean optional, boolean alwaysDatagenDefault) {
            this.tag = resolve(BuiltInRegistries.ENTITY_TYPE, nameSpace, path == null ? Lang.asId(name()) : path, optional, id -> TagKey.create(Registries.ENTITY_TYPE, id));
            this.alwaysDatagen = alwaysDatagenDefault;
        }
    }

    public enum CNRecipeSerializerTags {
        AUTOMATION_IGNORE,
        ;

        public final TagKey<RecipeSerializer<?>> tag;
        public final boolean alwaysDatagen;

        CNRecipeSerializerTags() {
            this(MOD);
        }

        CNRecipeSerializerTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        CNRecipeSerializerTags(NameSpace namespace, boolean optional, boolean alwaysDatagen) {
            this(namespace, null, optional, alwaysDatagen);
        }

        CNRecipeSerializerTags(NameSpace nameSpace, String path, boolean optional, boolean alwaysDatagen) {
            this.tag = resolve(BuiltInRegistries.RECIPE_SERIALIZER, nameSpace, path == null ? Lang.asId(name()) : path, optional, id -> TagKey.create(Registries.RECIPE_SERIALIZER, id));
            this.alwaysDatagen = alwaysDatagen;
        }
    }


    public static void init() {
        CreateNuclear.LOGGER.info("Registering mod tags for " + CreateNuclear.MOD_ID);
        CNBlockTags.values();
        CNItemTags.values();
        CNFluidTags.values();
        CNEntityTags.values();
        CNRecipeSerializerTags.values();
    }
}
