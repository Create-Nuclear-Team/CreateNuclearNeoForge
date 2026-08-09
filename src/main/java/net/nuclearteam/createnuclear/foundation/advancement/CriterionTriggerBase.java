package net.nuclearteam.createnuclear.foundation.advancement;

import com.google.common.collect.Maps;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.nuclearteam.createnuclear.CreateNuclear;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public abstract class CriterionTriggerBase<T extends CriterionTriggerBase.Instance> implements CriterionTrigger<T> {
    private final ResourceLocation id;
    protected final Map<PlayerAdvancements, Set<Listener<T>>> listeners = Maps.newHashMap();

    public CriterionTriggerBase(String id) {
        this.id = CreateNuclear.asResource(id);
    }

    @Override
    public void addPlayerListener(PlayerAdvancements playerAdvancements, Listener<T> listener) {
        Set<Listener<T>> playerListeners = this.listeners.computeIfAbsent(playerAdvancements, k -> new HashSet<>());

        playerListeners.add(listener);
    }

    @Override
    public void removePlayerListener(PlayerAdvancements playerAdvancements, Listener<T> listener) {
        Set<Listener<T>> playerListeners = this.listeners.get(playerAdvancements);

        if (playerListeners != null) {
            playerListeners.remove(listener);
            if (playerListeners.isEmpty()) {
                this.listeners.remove(playerAdvancements);
            }
        }
    }

    @Override
    public void removePlayerListeners(PlayerAdvancements playerAdvancements) {
        this.listeners.remove(playerAdvancements);
    }

    public ResourceLocation getId() {
        return id;
    }

    protected void trigger(ServerPlayer player, @Nullable List<Supplier<Object>> suppliers) {
        PlayerAdvancements playerAdvancements = player.getAdvancements();
        Set<Listener<T>> playerListeners = this.listeners.get(playerAdvancements);
        if (playerListeners != null) {
            List<Listener<T>> list = new LinkedList<>();

            for (Listener<T> listener : playerListeners) {
                if (listener.trigger().test(suppliers)) {
                    list.add(listener);
                }
            }

            list.forEach(listener -> listener.run(playerAdvancements));
        }
    }

    public abstract static class Instance implements SimpleCriterionTrigger.SimpleInstance {
        protected abstract boolean test(@Nullable List<Supplier<Object>> suppliers);
    }
}
