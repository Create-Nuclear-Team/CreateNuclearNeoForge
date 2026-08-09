package net.nuclearteam.createnuclear.foundation.advancement;

import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.minecraft.core.HolderLookup.Provider;


import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class CreateNuclearAdvancement {

    static final ResourceLocation BACKGROUND = CreateNuclear.asResource("textures/gui/advancements/backgrounds/background_advancement.png");
    static final String LANG = "advancement." + CreateNuclear.MOD_ID + ".";
    static final String SECRET_SUFFIX = "\n\u00A77(Hidden Advancement)";

    private final Advancement.Builder builder = Advancement.Builder.advancement();
    private SimpleCreateNuclearTrigger builtinTrigger;
    private CreateNuclearAdvancement parent;
    private final Builder createNuclearBuilder = new Builder();

    AdvancementHolder datagenResult;

    final String id;
    private String title;
    private String description;


    public CreateNuclearAdvancement(String id, UnaryOperator<Builder> b) {
        this.id = id;

        b.apply(createNuclearBuilder);

        if (!createNuclearBuilder.externalTrigger) {
            builtinTrigger = CNTriggers.addSimple(id + "_builtin");
            builder.addCriterion("0", builtinTrigger.createCriterion(builtinTrigger.instance()));
        }

        if (createNuclearBuilder.type == TaskType.SECRET)
            description += SECRET_SUFFIX;

        CNAdvancement.ENTRIES.add(this);
    }

    private String titleKey() {
        return LANG + id;
    }

    private String descriptionKey() {
        return titleKey() + ".desc";
    }

    public boolean isAlreadyAwardedTo(Player player) {
        if (!(player instanceof ServerPlayer sp))
            return true;
        AdvancementHolder advancement = sp.getServer()
                .getAdvancements()
                .get(CreateNuclear.asResource(id));
        if (advancement == null)
            return true;
        return sp.getAdvancements()
                .getOrStartProgress(advancement)
                .isDone();
    }

    public void awardTo(Player player) {
        if (!(player instanceof ServerPlayer sp))
            return;
        if (builtinTrigger == null)
            throw new UnsupportedOperationException(
                    "Advancement " + id + " uses external Triggers, it cannot be awarded directly");
        builtinTrigger.trigger(sp);
    }

    void save(Consumer<AdvancementHolder> t, HolderLookup.Provider registries) {
        if (parent != null)
            builder.parent(parent.datagenResult);

        if (createNuclearBuilder.func != null)
            createNuclearBuilder.icon(createNuclearBuilder.func.apply(registries));

        builder.display(
                createNuclearBuilder.icon,
                Component.translatable(titleKey()),
                Component.translatable(descriptionKey()).withStyle(s -> s.withColor(0xDBA213)),
                id.equals("root") ? BACKGROUND : null,
                createNuclearBuilder.type.advancementType,
                createNuclearBuilder.type.toast,
                createNuclearBuilder.type.announce,
                createNuclearBuilder.type.hide
        );
        datagenResult = builder.save(t, CreateNuclear.asResource(id).toString());
    }

    void provideLang(BiConsumer<String, String> consumer) {
        consumer.accept(titleKey(), title);
        consumer.accept(descriptionKey(), description);
    }

    enum TaskType {

        SILENT(AdvancementType.TASK, false, false, false),
        NORMAL(AdvancementType.TASK, true, false, false),
        NOISY(AdvancementType.TASK, true, true, false),
        EXPERT(AdvancementType.GOAL, true, true, false),
        SECRET(AdvancementType.GOAL, true, true, true),

        ;

        private final AdvancementType advancementType;
        private final boolean toast;
        private final boolean announce;
        private final boolean hide;

        TaskType(AdvancementType frame, boolean toast, boolean announce, boolean hide) {
            this.advancementType = frame;
            this.toast = toast;
            this.announce = announce;
            this.hide = hide;
        }
    }

    class Builder {

        private TaskType type = TaskType.NORMAL;
        private boolean externalTrigger;
        private int keyIndex;
        private ItemStack icon;
        private Function<Provider, ItemStack> func;

        Builder special(TaskType type) {
            this.type = type;
            return this;
        }

        Builder after(CreateNuclearAdvancement other) {
            CreateNuclearAdvancement.this.parent = other;
            return this;
        }

        Builder icon(ItemProviderEntry<?, ?> item) {
            return icon(item.asStack());
        }

        Builder icon(ItemLike item) {
            return icon(new ItemStack(item));
        }

        Builder icon(ItemStack stack) {
            icon = stack;
            return this;
        }

        Builder icon(Function<Provider, ItemStack> func) {
            this.func = func;
            return this;
        }

        Builder title(String title) {
            CreateNuclearAdvancement.this.title = title;
            return this;
        }

        Builder description(String description) {
            CreateNuclearAdvancement.this.description = description;
            return this;
        }

        Builder whenBlockPlaced(Block block) {
            return externalTrigger(ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
        }

        Builder whenIconCollected() {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(icon.getItem()));
        }

        Builder whenItemCollected(ItemProviderEntry<?, ?> item) {
            return whenItemCollected(item.asStack()
                    .getItem());
        }

        Builder whenItemCollected(ItemLike itemProvider) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(itemProvider));
        }

        Builder whenItemCollected(TagKey<Item> tag) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tag).build()));
        }

        Builder awardedForFree() {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[] {}));
        }

        Builder externalTrigger(Criterion<?> trigger) {
            builder.addCriterion(String.valueOf(keyIndex), trigger);
            externalTrigger = true;
            keyIndex++;
            return this;
        }

    }
}