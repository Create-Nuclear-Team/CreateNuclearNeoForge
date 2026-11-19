package net.nuclearteam.createnuclear.compat.kubejs;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.UniqueIdBuilder;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ItemMatch;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatchInfo;
import dev.latvian.mods.kubejs.util.OpsContainer;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.nuclearteam.createnuclear.CreateNuclear;

import java.util.Map;

/**
 * Recipe component for Create's {@link com.simibubi.create.content.processing.recipe.ProcessingOutput}. Accepts anything KubeJS can turn
 * into an item stack ({@code 'minecraft:stone'}, {@code '2x minecraft:stone'}, ...), or an
 * object with a chance: {@code {item: 'minecraft:stone', count: 2, chance: 0.5}}.
 */
public record ProcessingOutputComponent(RecipeComponentType<?> type) implements RecipeComponent<ProcessingOutput> {
    public static final RecipeComponentType<ProcessingOutput> TYPE = RecipeComponentType.unit(CreateNuclear.asResource("processing_output"), ProcessingOutputComponent::new);
    private static final TypeInfo TYPE_INFO = TypeInfo.of(ProcessingOutput.class).or(ItemWrapper.TYPE_INFO);

    @Override
    public Codec<ProcessingOutput> codec() {
        return ProcessingOutput.CODEC_NEW;
    }

    @Override
    public TypeInfo typeInfo() {
        return TYPE_INFO;
    }

    @Override
    public boolean hasPriority(RecipeMatchContext cx, Object from) {
        return from instanceof ProcessingOutput || ItemWrapper.isItemStackLike(from);
    }

    @Override
    public ProcessingOutput wrap(RecipeScriptContext cx, Object from) {
        if (from instanceof ProcessingOutput output)
            return output;

        if (from instanceof Map<?,?> map && map.get("chance") instanceof Number chance) {
            ItemStack stack = ItemWrapper.wrap(cx.cx(), map.containsKey("item") ? map.get("item") : map.get("id"));
            if (map.get("count") instanceof Number count)
                stack.setCount(count.intValue());
            return new ProcessingOutput(stack, chance.floatValue());
        }

        return new ProcessingOutput(ItemWrapper.wrap(cx.cx(), from), 1f);
    }

    @Override
    public boolean matches(RecipeMatchContext cx, ProcessingOutput value, ReplacementMatchInfo match) {
        return match.match() instanceof ItemMatch m && !value.getStack().isEmpty() && m.matches(cx, value.getStack(), match.exact());
    }

    @Override
    public boolean isEmpty(ProcessingOutput value) {
        return value.getStack().isEmpty();
    }

    @Override
    public void buildUniqueId(UniqueIdBuilder builder, ProcessingOutput value) {
        if (!value.getStack().isEmpty())
            builder.append(BuiltInRegistries.ITEM.getKey(value.getStack().getItem()));
    }

    @Override
    public String toString() {
        return type.toString();
    }

    @Override
    public String toString(OpsContainer ops, ProcessingOutput value) {
        ItemStack stack = value.getStack();
        String item = stack.getCount() + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem());
        return value.getChance() < 1f ? item + " (" + value.getChance() * 100 + "%)" : item;
    }
}
