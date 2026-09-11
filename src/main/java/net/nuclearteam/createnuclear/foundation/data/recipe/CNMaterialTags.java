package net.nuclearteam.createnuclear.foundation.data.recipe;

import net.createmod.catnip.lang.Lang;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.nuclearteam.createnuclear.CNTags;

import java.util.*;

/**
 * The idea is to have the common ("c:") tags generated on the fly, without any typo/writing
 * issues each time a new one is added: a material only declares which {@link ResourceType}
 * shapes it actually has, and the matching {@code TagKey}s are derived and cached from that.
 * <p>
 * This class should be used and extended for every common item/block tag created by this mod.
 * A common tag already provided by Create, such as brass, should instead go through Create's
 * own {@code com.simibubi.create.foundation.data.recipe.CommonMetal} class.
 */
public enum CNMaterialTags {
    URANIUM(ResourceType.ORE, ResourceType.STORAGE_BLOCK, ResourceType.RAW_STORAGE_BLOCK,
            ResourceType.RAW_MATERIAL, ResourceType.DUST, ResourceType.BUCKET, ResourceType.FLUID),
    LEAD(ResourceType.ORE, ResourceType.STORAGE_BLOCK, ResourceType.RAW_STORAGE_BLOCK,
            ResourceType.RAW_MATERIAL, ResourceType.INGOT, ResourceType.NUGGET),
    THORIUM(ResourceType.ORE, ResourceType.STORAGE_BLOCK, ResourceType.RAW_STORAGE_BLOCK,
            ResourceType.RAW_MATERIAL, ResourceType.INGOT, ResourceType.NUGGET, ResourceType.DUST, ResourceType.BUCKET, ResourceType.FLUID),
    STEEL(ResourceType.STORAGE_BLOCK, ResourceType.INGOT, ResourceType.NUGGET),
    NITRATE(ResourceType.ORE),
    COAL(ResourceType.DUST),
    NITROGEN(ResourceType.BUCKET, ResourceType.FLUID),
    ;

    private final String name;
    private final Set<ResourceType> available;
    private final Map<ResourceType, Object> cache = new EnumMap<>(ResourceType.class);

    CNMaterialTags(ResourceType... types) {
        this.name = Lang.asId(name());
        this.available = types.length == 0 ? EnumSet.noneOf(ResourceType.class) : EnumSet.copyOf(Arrays.asList(types));
    }

    public String getName() {
        return name;
    }

    private void require(ResourceType type) {
        if (!available.contains(type))
            throw new IllegalStateException(this + " has no " + type + " tag");
    }

    private ItemLikeTag itemLike(ResourceType type) {
        require(type);
        return (ItemLikeTag) cache.computeIfAbsent(type, t -> new ItemLikeTag(t.path(getName())));
    }

    @SuppressWarnings("unchecked")
    private TagKey<Item> itemOnly(ResourceType type) {
        require(type);
        return (TagKey<Item>) cache.computeIfAbsent(type, t -> CNTags.neoForgeItemTag(t.path(getName())));
    }

    public ItemLikeTag ores() { return itemLike(ResourceType.ORE); }

    public ItemLikeTag storageBlocks() { return itemLike(ResourceType.STORAGE_BLOCK); }

    public ItemLikeTag rawStorageBlocks() { return itemLike(ResourceType.RAW_STORAGE_BLOCK); }

    public TagKey<Item> rawMaterials() { return itemOnly(ResourceType.RAW_MATERIAL); }

    public TagKey<Item> ingots() { return itemOnly(ResourceType.INGOT); }

    public TagKey<Item> nuggets() { return itemOnly(ResourceType.NUGGET); }

    public TagKey<Item> dusts() { return itemOnly(ResourceType.DUST); }

    public TagKey<Item> buckets() { return itemOnly(ResourceType.BUCKET); }

    @SuppressWarnings("unchecked")
    public TagKey<Fluid> fluid() {
        require(ResourceType.FLUID);
        return (TagKey<Fluid>) cache.computeIfAbsent(ResourceType.FLUID, t -> CNTags.neoForgeFluidTag(name));
    }

    @Override
    public String toString() { return name; }

    private enum ResourceType {
        ORE("ores/"),
        STORAGE_BLOCK("storage_blocks/"),
        RAW_STORAGE_BLOCK("raw_storage_blocks/"),
        RAW_MATERIAL("raw_materials/"),
        INGOT("ingots/"),
        NUGGET("nuggets/"),
        DUST("dusts/"),
        BUCKET("buckets/"),
        FLUID(""),
        ;

        private final String prefix;
        ResourceType(String prefix) {
            this.prefix = prefix;
        }

        String path(String materialName) {
            return prefix + materialName;
        }
    }

    public record ItemLikeTag(TagKey<Item> items, TagKey<Block> blocks) {
        private ItemLikeTag(String path) {
            this(CNTags.neoForgeItemTag(path), CNTags.neoForgeBlockTag(path));
        }
    }
}
