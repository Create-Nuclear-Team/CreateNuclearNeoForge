package net.nuclearteam.createnuclear.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import net.nuclearteam.createnuclear.CNRecipeTypes;
import net.nuclearteam.createnuclear.compat.Mods;

public class CNKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(CNKubeJSEvents.GROUP);
    }

    @Override
    public void registerRecipeComponents(RecipeComponentTypeRegistry registry) {
        registry.register(ProcessingOutputComponent.TYPE);
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        registry.register(CNRecipeTypes.ENRICHED.getId(), CNRecipeSchemas.fanProcessing());
        if (!Mods.CREATE_DRAGONS_PLUS.isLoaded())
            registry.register(CNRecipeTypes.SNOW_POWDER.getId(), CNRecipeSchemas.fanProcessing());
    }
}
