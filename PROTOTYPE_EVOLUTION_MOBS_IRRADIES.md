# Évolution dynamique des mobs irradiés — Prototype d'architecture

> Document généré à partir d'une session de conception. Le code ci-dessous est un **prototype
> d'évaluation architecturale**, pas une PR : il montre jusqu'où le principe "aucune paire
> d'entités codée à l'avance" peut être tenu, avec les vraies limites rencontrées en cours de
> route. Rien de ce code n'est intégré au build (pas de fichiers `.java` créés, pas d'enregistrement
> dans `CreateNuclear.onCtor`).

---

## Prompt de vérification

À utiliser (par moi plus tard, par un autre agent, ou par un dev qui relit) pour confirmer ou
infirmer ce qui est écrit dans ce document avant toute implémentation réelle :

```
Contexte : CreateNuclearNeoForge, mod NeoForge pour Minecraft 1.21.1 (neo_version=21.1.247,
mappings officiels + Parchment 2024.11.17), package racine net.nuclearteam.createnuclear.

Le document PROTOTYPE_EVOLUTION_MOBS_IRRADIES.md propose une architecture "Trait" pour un système
où un mob irradié acquiert dynamiquement des attributs/comportements/apparence des entités qu'il
tue, sans code écrit par paire d'entités. Vérifie les points suivants contre l'état réel du projet
et des sources Minecraft décompilées (via ./gradlew, cache neoformruntime sous
~/.gradle/caches/neoformruntime/intermediate_results/*_output.jar) :

1. Les signatures d'API citées sont-elles toujours exactes dans la version actuelle du projet
   (mappings/version MC ont-ils changé) ? En particulier :
   - LivingEntity#getAttribute(Holder<Attribute>), AttributeMap#hasAttribute/getInstance
   - AttributeInstance#addOrReplacePermanentModifier, le record AttributeModifier(ResourceLocation,
     double, AttributeModifier.Operation)
   - PartDefinition#bake(int, int), EntityModelSet#bakeLayer(ModelLayerLocation), ModelPart#getChild
   - KeyframeAnimations#animate (fail-safe via Optional/getAnyDescendantWithName)
   - SimpleJsonResourceReloadListener(Gson, String) + AddReloadListenerEvent
   - VibrationSystem / VibrationSystem.User (Warden.java, Warden$VibrationUser)
2. Le mécanisme du "sonar" du Warden (tendrilAnimation, entity event byte 61, onReceiveVibration
   dans Warden$VibrationUser) est-il toujours implémenté ainsi, ou a-t-il été refactoré ?
3. TurtleModel#createBodyLayer définit-il toujours "shell" comme un nom de cube (CubeListBuilder
   .addBox("shell", ...)) à l'intérieur du part "body", plutôt que comme un PartDefinition séparé ?
   Si oui, la limite "on ne peut pas extraire juste la carapace via ModelPart#getChild" tient
   toujours. Si Mojang a changé le modèle, cette limite doit être réévaluée.
4. Les conventions du projet citées sont-elles toujours d'actualité :
   - RadiationCapability (net.nuclearteam.createnuclear.content.radiation.capability) comme
     référence de style pour un attachment Codec/StreamCodec
   - CNAttachmentTypes.java comme point d'enregistrement des AttachmentType
   - CreateNuclear.onCtor(...) comme point d'enregistrement séquentiel des sous-systèmes
   - Absence de LivingDeathEvent existant dans le mod (donc pas de conflit avec un hook existant)
   - Absence de GeckoLib (rendu 100% vanilla EntityModel/HierarchicalModel)
5. Le code des fichiers proposés compile-t-il tel quel une fois copié dans le projet (imports,
   noms de packages, méthodes réellement publiques) ? Signale toute erreur de compilation probable.
6. Y a-t-il, depuis, une meilleure primitive vanilla/NeoForge pour l'un des trois tiers (attributs,
   comportement, apparence) qui rendrait une partie de cette architecture obsolète ou plus simple ?

Réponds point par point : CONFIRMÉ / INFIRMÉ / À REVÉRIFIER, avec la source (fichier + ligne) à
l'appui pour chaque point.
```

---

## Contexte du projet (vérifié dans les sources au moment de l'écriture)

- Mod NeoForge, Minecraft **1.21.1**, `neo_version=21.1.247`, mappings officiels + Parchment
  `2024.11.17` (`gradle.properties`).
- Package racine : `net.nuclearteam.createnuclear`, classe mod `CreateNuclear.java`
  (`@Mod(CreateNuclear.MOD_ID)`, `MOD_ID = "createnuclear"`).
- Structure : `api/` (points d'extension publics), `content/` (fonctionnalités), `foundation/`
  (utilitaires/bases), `infrastructure/` (config, worldgen, datagen).
- Trois mobs irradiés existent déjà : `IrradiatedWolf`, `IrradiatedChicken`, `IrradiatedCat`
  (tous `TamableAnimal`, pas pensés à l'origine pour "chasser et évoluer"). Aucun mob
  `Monster`/"Mutant" générique n'existe encore.
- `RadiationCapability` (`content/radiation/capability/RadiationCapability.java`) est le modèle de
  style pour un attachment NeoForge : `Codec` via `RecordCodecBuilder`, `StreamCodec` via
  `StreamCodec.composite`, enregistré dans `CNAttachmentTypes.java`
  (`AttachmentType.builder(...).serialize(CODEC).sync(STREAM_CODEC)`).
- Aucun `LivingDeathEvent` n'est utilisé nulle part dans le mod — point d'insertion libre.
- Aucune dépendance GeckoLib — rendu 100% vanilla (`EntityModel`/`HierarchicalModel` +
  `Renderer`/`Model` classiques, ex. `IrradiatedWolfModel.java`/`IrradiatedWolfRenderer.java`).
- Pas de `JsonDataLoader` maison existant ; le mod suit surtout le framework de recipe-gen de
  Create. Le loader JSON proposé ici (`EntityTraitMapping`) suit le patron vanilla
  `SimpleJsonResourceReloadListener`, pas un patron déjà présent dans le repo.

---

## Résumé de la discussion

### Question initiale

Faire évoluer dynamiquement des mobs irradiés : un mob irradié qui tue une autre entité récupère
certaines de ses caractéristiques (capacités, comportements, attributs, apparence) **sans que
chaque interaction soit codée à l'avance**. Exemples donnés : tuer un Warden → attaque à distance +
détection sonore ; tuer une tortue → carapace adaptée à sa taille.

### Ce qui est faisable, et à quel prix

**Attributs (santé, dégâts, vitesse...)** — totalement générique. L'API `Attribute`/
`AttributeInstance` permet de lire n'importe quelle entité tuée et de fusionner ses valeurs sur le
mob irradié sans code par paire.

**Comportements/capacités** — semi-générique. Les `Goal`/`Brain` vanilla sont écrits en dur pour
une classe précise (cast interne), donc pas copiables tels quels par réflexion. Solution : un
système de **Traits** enregistrés en Java (une classe par capacité, pas par paire), activés soit
par une **table datapack JSON** `EntityType -> [traitId]`, soit par une **sonde réflexive**
(`instanceof VibrationSystem`, etc.) quand c'est fiable.

**Apparence/texture/modèle** — le plus contraint. Les modèles vanilla (`LayerDefinition`/
`PartDefinition`, depuis 1.17) forment un arbre de parties nommées, introspectable à l'exécution
via `EntityModelSet`. On peut extraire une sous-partie nommée et sa texture d'origine sans aucune
fusion de texture. Limite réelle découverte en lisant `TurtleModel.java` : la carapace de la
tortue n'est **pas** un `ModelPart` séparé, c'est un **nom de cube** (`addBox("shell", ...)`) à
l'intérieur du part `"body"` — ce nom n'est pas conservé après `bake()`, et `ModelPart` n'expose
aucun accès public à sa liste de cubes. Donc "récupérer juste la carapace" n'est pas possible avec
l'API publique telle quelle ; il faudrait re-modéliser cette partie soi-même en sous-part séparée.

### Le détour Warden : sonar vs sonic boom

Deux mécanismes séparés, vérifiés dans les sources décompilées
(`net/minecraft/world/entity/monster/warden/Warden.java`,
`net/minecraft/world/entity/ai/behavior/warden/SonicBoom.java`,
`net/minecraft/client/model/WardenModel.java`) :

- **Sonic boom (attaque)** : une `Behavior<Warden>` pilotée par `MemoryModuleType` (Brain), donc
  liée au système `Brain` — pas portable telle quelle sur un mob à `goalSelector` classique sans
  réimplémentation (le code source fait ~20 lignes utiles, facilement réécrit en `Goal`). La
  particule part du point d'ancrage `EntityAttachment.WARDEN_CHEST` (la poitrine, pas les yeux ni
  la tête). L'animation associée (`WardenAnimation.WARDEN_SONIC_BOOM`) cible surtout les parties
  `"body"` et `"right_ribcage"`, pas `"head"`.
- **Sonar / écoute (ce qui intéressait l'utilisateur)** : beaucoup plus simple à généraliser.
  `Warden implements VibrationSystem` (interface générique de Mojang, faite pour être implémentée
  par n'importe quelle entité). `VibrationUser#onReceiveVibration` (ligne ~644 de `Warden.java`)
  fait juste `level.broadcastEntityEvent(this, (byte)61)`. Côté client,
  `handleEntityEvent(id==61)` met `this.tendrilAnimation = 10` — **un simple compteur entier qui
  décroît**, pas une `AnimationDefinition`/keyframes. `getTendrilAnimation(partialTick)` le lisse
  en 0→1, et `WardenModel.animateTendrils()` applique
  `rot = tendrilAnimation × cos(ageInTicks × 2.25) × π × 0.1` sur une paire de parties symétriques.
  Cette formule ne dépend d'aucune géométrie spécifique au Warden : elle est directement réutilisable
  sur n'importe quelle paire de parties nommées d'un autre modèle.

### La question "peut-on éviter de tout prévoir" — la vraie réponse

Deux choses différentes :

- **Extraction générique de données au moment du kill** (attributs via l'API `Attribute`,
  capacités via `instanceof`/structure de `PartDefinition`) : déjà "automatique", un seul moteur
  écrit une fois interprète n'importe quelle victime.
- **Génération de code à la volée** (bytecode au runtime via ASM/ByteBuddy) : techniquement
  possible sur la JVM mais inutile et fragile ici — personne ne fait ça en modding Minecraft pour
  cette raison (versions MC changeantes, debug quasi impossible).

La bonne architecture : extraction générique → objet de données neutre → **un seul moteur
générique** qui l'interprète et agit (attributs, activation de capacité, greffe de partie de
modèle). Aucune classe spécifique par entité rencontrée.

**Limite non contournable** : le JVM ne permet pas d'attacher une interface Java (ex.
`VibrationSystem`) à une instance déjà créée. Le mob irradié doit **déjà** implémenter cette
interface (câblage dormant, toujours présent, une seule fois sur sa classe) ; un `Trait` ne fait
qu'activer un booléen consulté à l'intérieur de ce câblage. C'est la seule "prévision"
incontournable, et elle porte sur le **tueur**, jamais sur la victime.

---

## Architecture proposée

```
api/trait/
  Trait.java                 — interface, une implémentation par capacité (pas par paire d'entités)
  IrradiatedEvolver.java     — interface que le mob tueur doit implémenter pour participer
  TraitRegistry.java         — registry Java des Trait + sondes de réflexion (fallback)
  EntityTraitMapping.java    — reload listener JSON : EntityType -> liste de traits
  AcquiredTraits.java        — attachment NeoForge : quels traits un mob a acquis (persisté + synchro)
  AttributeMerger.java       — Tier 1, 100% générique, aucune donnée à fournir

content/traits/
  TraitAcquisitionHandler.java — hook LivingDeathEvent qui orchestre tout
  VibrationSensingTrait.java   — Tier 2 : exemple "sonar du Warden"
  appearance/
    PartGraftExtractor.java    — Tier 3 : extraction générique d'une partie de modèle
    ShellAppearanceTrait.java  — Tier 3 : exemple "carapace de tortue"
    GraftedPartRenderLayer.java

data/createnuclear/trait_mappings/*.json   — la config datapack
```

---

## Code — Tier 1 : Attributs (100% générique, zéro donnée à fournir)

```java
package net.nuclearteam.createnuclear.api.trait;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Merges a fraction of the victim's attribute values into the acquirer's.
 * Nothing here is specific to any entity: it walks the global Attribute registry and only acts on
 * attributes both the victim and the acquirer actually have, discovered at kill time.
 */
public class AttributeMerger {
    private static final double SHARE = 0.15; // fraction of the victim's value merged in

    public static void merge(LivingEntity acquirer, LivingEntity victim) {
        ResourceLocation victimTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());

        BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
            if (!victim.getAttributes().hasAttribute(holder) || !acquirer.getAttributes().hasAttribute(holder)) return;

            double victimValue = victim.getAttributeValue(holder);
            AttributeInstance target = acquirer.getAttribute(holder);
            if (target == null || victimValue <= 0) return;

            ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(holder.value());
            // Stable id per (attribute, source species): re-killing the same species updates
            // instead of stacking; killing a different species adds a separate modifier.
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                "createnuclear", "trait_attr/" + attrId.getPath() + "/" + victimTypeId.getPath());

            target.addOrReplacePermanentModifier(
                new AttributeModifier(modifierId, victimValue * SHARE, AttributeModifier.Operation.ADD_VALUE));
        });
    }
}
```

**Pourquoi ça marche sans rien prévoir** : `BuiltInRegistries.ATTRIBUTE` liste tous les attributs
existants (vanilla + modés), `hasAttribute()`/`getAttribute()` sont des méthodes publiques de
`LivingEntity`/`AttributeMap` qui répondent correctement pour n'importe quelle entité. Aucune
classe à écrire pour "Warden" ou "Tortue" — tuer une tortue transfère sa vitesse de nage si
l'attribut existe sur les deux, tuer un Warden transfère un peu de sa vie max, etc.

---

## Code — Tier 2 : Comportements/capacités (registry de Traits, datapack JSON, fallback réflexif)

```java
package net.nuclearteam.createnuclear.api.trait;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

/**
 * Implemented by a mob class that can acquire traits from its kills.
 * The JVM cannot attach a new interface to an existing object at runtime, so every capability a
 * Trait might activate (vibration sensing, a ranged attack...) has to be pre-wired - present but
 * dormant - on the class itself. Traits only flip data on/off; they never add new Java behaviour.
 */
public interface IrradiatedEvolver {
    Set<ResourceLocation> getAcquiredTraitIds();

    default boolean hasTrait(ResourceLocation id) {
        return getAcquiredTraitIds().contains(id);
    }
}
```

```java
package net.nuclearteam.createnuclear.api.trait;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Code-side registry of Trait implementations - populated once at startup, not per entity pair. */
public class TraitRegistry {
    private static final Map<ResourceLocation, Trait> TRAITS = new LinkedHashMap<>();
    private static final List<ReflectiveTraitProbe> PROBES = new ArrayList<>();

    private TraitRegistry() {}

    public static void register(Trait trait) {
        TRAITS.put(trait.id(), trait);
    }

    public static void registerProbe(ReflectiveTraitProbe probe) {
        PROBES.add(probe);
    }

    public static Optional<Trait> get(ResourceLocation id) {
        return Optional.ofNullable(TRAITS.get(id));
    }

    /** datapack mapping ∪ reflective auto-detection, de-duplicated. */
    public static List<Trait> resolve(LivingEntity victim) {
        List<Trait> result = new ArrayList<>();
        for (ResourceLocation id : EntityTraitMapping.get(victim.getType())) {
            get(id).ifPresent(result::add);
        }
        for (ReflectiveTraitProbe probe : PROBES) {
            probe.probe(victim).ifPresent(t -> {
                if (!result.contains(t)) result.add(t);
            });
        }
        return result;
    }

    public interface ReflectiveTraitProbe {
        Optional<Trait> probe(LivingEntity victim);
    }

    /** Built-in registration, called once from the mod constructor. */
    public static void registerBuiltins() {
        register(VibrationSensingTrait.INSTANCE);
        register(ShellAppearanceTrait.INSTANCE);

        // No JSON needed for this one: any victim implementing VibrationSystem (Warden, or any
        // future modded entity) grants the sensing trait automatically, without a datapack entry.
        registerProbe(victim -> victim instanceof VibrationSystem
            ? Optional.of(VibrationSensingTrait.INSTANCE)
            : Optional.empty());
    }
}
```

```java
package net.nuclearteam.createnuclear.api.trait;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.*;

/**
 * Datapack-driven EntityType -> Trait ids, loaded from data/&lt;namespace&gt;/trait_mappings/*.json.
 * Adding a new interaction is a JSON file, not a Java class.
 */
public class EntityTraitMapping extends SimpleJsonResourceReloadListener {
    private static Map<EntityType<?>, List<ResourceLocation>> MAPPING = Map.of();

    public EntityTraitMapping() {
        super(new Gson(), "trait_mappings");
    }

    public static List<ResourceLocation> get(EntityType<?> type) {
        return MAPPING.getOrDefault(type, List.of());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<EntityType<?>, List<ResourceLocation>> result = new HashMap<>();

        for (JsonElement element : resources.values()) {
            JsonObject obj = element.getAsJsonObject();
            ResourceLocation entityId = ResourceLocation.parse(GsonHelper.getAsString(obj, "entity"));
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
            if (type == null) continue;

            List<ResourceLocation> traits = new ArrayList<>();
            obj.getAsJsonArray("traits").forEach(je -> traits.add(ResourceLocation.parse(je.getAsString())));
            result.merge(type, traits, (a, b) -> {
                List<ResourceLocation> merged = new ArrayList<>(a);
                merged.addAll(b);
                return merged;
            });
        }

        MAPPING = result;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((AddReloadListenerEvent event) -> event.addListener(new EntityTraitMapping()));
    }
}
```

```java
package net.nuclearteam.createnuclear.api.trait;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/** Attachment: which trait ids a mob has acquired. Same pattern as RadiationCapability. */
public class AcquiredTraits {
    public static final Codec<AcquiredTraits> CODEC = ResourceLocation.CODEC.listOf()
        .xmap(list -> create(new HashSet<>(list)), c -> List.copyOf(c.traitIds));

    public static final StreamCodec<RegistryFriendlyByteBuf, AcquiredTraits> STREAM_CODEC = ByteBufCodecs
        .collection(HashSet::new, ResourceLocation.STREAM_CODEC)
        .map(AcquiredTraits::create, c -> c.traitIds);

    private final Set<ResourceLocation> traitIds;

    private AcquiredTraits(Set<ResourceLocation> traitIds) {
        this.traitIds = traitIds;
    }

    public AcquiredTraits() {
        this(new HashSet<>());
    }

    private static AcquiredTraits create(Set<ResourceLocation> ids) {
        return new AcquiredTraits(ids);
    }

    public Set<ResourceLocation> ids() {
        return traitIds;
    }

    public boolean add(ResourceLocation id) {
        return traitIds.add(id);
    }
}
```

L'exemple concret du sonar, exactement la formule vérifiée dans `Warden.java`/`WardenModel.java`
(compteur qui décroît + oscillation), généralisée à n'importe quelle paire de parties nommées :

```java
package net.nuclearteam.createnuclear.content.traits;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.trait.IrradiatedEvolver;
import net.nuclearteam.createnuclear.api.trait.Trait;

public class VibrationSensingTrait implements Trait {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateNuclear.MOD_ID, "vibration_sensing");
    public static final VibrationSensingTrait INSTANCE = new VibrationSensingTrait();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onAcquire(ServerLevel level, LivingEntity acquirer, LivingEntity victim) {
        if (acquirer instanceof IrradiatedEvolver evolver) {
            evolver.getAcquiredTraitIds().add(ID);
            // The VibrationSystem.User implementation itself (getListenerRadius, canReceiveVibration,
            // onReceiveVibration) is pre-wired once on the mob class (see notes below) and simply
            // checks evolver.hasTrait(ID) before doing anything - this call only flips that flag on.
            // Nothing here is Warden-specific.
        }
    }
}
```

**Rappel de la limite** : `Warden implements VibrationSystem` est une interface Java sur la
*classe*. Le mob irradié doit **déjà** implémenter `VibrationSystem` (câblage dormant, toujours
présent), et le Trait ne fait qu'activer un booléen consulté dans `canReceiveVibration()`.

---

## Code — Tier 3 : Apparence (extraction générique de partie de modèle, avec ses vraies limites)

```java
package net.nuclearteam.createnuclear.content.traits.appearance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Optional;

/**
 * Extracts a named ModelPart subtree from any registered entity model and keeps the source
 * entity's own texture reference alongside it - no texture stitching needed, since a ModelPart's
 * cubes carry UVs relative to whichever texture the source model was baked for.
 */
public class PartGraftExtractor {
    // Only vanilla entries are listed here for the prototype; a modded EntityType would register
    // its ModelLayerLocation the same way Warden/Turtle do, via a small lookup table like this one -
    // still data, not a new class per entity.
    private static final Map<EntityType<?>, ModelLayerLocation> MODEL_LAYERS = Map.of(
        EntityType.TURTLE, ModelLayers.TURTLE,
        EntityType.WARDEN, ModelLayers.WARDEN
    );

    public record PartGraft(ModelPart part, ResourceLocation sourceTexture) {}

    public static Optional<PartGraft> extract(Entity source, String... pathToPart) {
        ModelLayerLocation layer = MODEL_LAYERS.get(source.getType());
        if (layer == null) return Optional.empty();

        ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(layer);
        ModelPart current = root;
        for (String name : pathToPart) {
            current = current.getChild(name);
            if (current == null) return Optional.empty(); // fails safe, same as vanilla's own lookup
        }

        ResourceLocation texture = Minecraft.getInstance().getEntityRenderDispatcher()
            .getRenderer(source).getTextureLocation(source);
        return Optional.of(new PartGraft(current, texture));
    }
}
```

```java
package net.nuclearteam.createnuclear.content.traits.appearance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.trait.IrradiatedEvolver;
import net.nuclearteam.createnuclear.api.trait.Trait;

public class ShellAppearanceTrait implements Trait {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateNuclear.MOD_ID, "turtle_shell");
    public static final ShellAppearanceTrait INSTANCE = new ShellAppearanceTrait();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onAcquire(ServerLevel level, LivingEntity acquirer, LivingEntity victim) {
        // Server side only stores that the trait was acquired (synced via AcquiredTraits).
        // The client resolves *which* geometry that means by calling PartGraftExtractor itself,
        // at render time, from the trait id - see GraftedPartRenderLayer.
        if (acquirer instanceof IrradiatedEvolver evolver) {
            evolver.getAcquiredTraitIds().add(ID);
        }
    }
}
```

```java
package net.nuclearteam.createnuclear.content.traits.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.nuclearteam.createnuclear.api.trait.IrradiatedEvolver;

/**
 * Renders whichever PartGraft the acquirer currently holds, anchored at a fixed offset.
 * Proof-of-concept: the anchor is a single translation, and the grafted geometry is drawn in its
 * static bind pose (no animation retargeting) - good enough for a shell, not for a limb that needs
 * to swing with the walk cycle.
 */
public class GraftedPartRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public GraftedPartRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                        float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                        float netHeadYaw, float headPitch) {
        if (!(entity instanceof IrradiatedEvolver evolver) || !evolver.hasTrait(ShellAppearanceTrait.ID)) return;

        // In a full implementation this graft would be resolved once (on trait acquisition) and
        // cached per-entity rather than re-extracted every frame; kept inline here for clarity.
        PartGraftExtractor.extract(entity, EntityType.TURTLE.toString()) // placeholder source lookup
            .ifPresent(graft -> {
                poseStack.pushPose();
                poseStack.translate(0.0, -0.4, 0.3); // anchor point on the acquirer's own body - this is the one thing you predefine per acquirer, not per victim
                float scale = entity.getBbWidth() / 1.2F; // crude size adaptation
                poseStack.scale(scale, scale, scale);
                VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(graft.sourceTexture()));
                graft.part().render(poseStack, consumer, packedLight, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
                poseStack.popPose();
            });
    }
}
```

---

## Le hook qui relie tout

```java
package net.nuclearteam.createnuclear.content.traits;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.trait.AttributeMerger;
import net.nuclearteam.createnuclear.api.trait.IrradiatedEvolver;
import net.nuclearteam.createnuclear.api.trait.Trait;
import net.nuclearteam.createnuclear.api.trait.TraitRegistry;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID)
public class TraitAcquisitionHandler {

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        LivingEntity victim = event.getEntity();

        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return;
        if (!(killer instanceof IrradiatedEvolver)) return; // opt-in, no class-by-class check

        AttributeMerger.merge(killer, victim);             // tier 1, always
        for (Trait trait : TraitRegistry.resolve(victim)) { // tier 2/3, data + reflection driven
            trait.onAcquire(level, killer, victim);
        }
    }
}
```

Datapack JSON — c'est ici, et seulement ici, qu'on "prévoit" une interaction, en donnée pure :

```json
// data/createnuclear/trait_mappings/warden.json
{
  "entity": "minecraft:warden",
  "traits": ["createnuclear:vibration_sensing"]
}
```

```json
// data/createnuclear/trait_mappings/turtle.json
{
  "entity": "minecraft:turtle",
  "traits": ["createnuclear:turtle_shell"]
}
```

(Le trait `vibration_sensing` serait en fait accordé même sans ce fichier, via la sonde réflexive
`instanceof VibrationSystem` enregistrée dans `TraitRegistry.registerBuiltins()`.)

---

## Ce que ce prototype prouve, et ce qu'il ne résout pas

**Ça marche, générique, sans code par paire :**
- Tier 1 (attributs) : totalement automatique, zéro configuration.
- Tier 2 (comportement/capacité) : une seule classe de Trait par capacité, activable sur n'importe
  quelle victime via JSON *ou* réflexion, sans jamais écrire "si Warden alors...".
- Tier 3 (apparence) : `PartDefinition.bake()` / `EntityModelSet.bakeLayer()` + `ModelPart.getChild()`
  sont de vraies API publiques qui permettent d'extraire une sous-partie nommée de n'importe quel
  modèle enregistré, texture d'origine comprise, sans rien recompiler.

**Ce qui reste une limite réelle, pas un détail :**
- `IrradiatedEvolver`/`VibrationSystem` doivent être câblés une fois sur la classe du mob tueur —
  le JVM ne permet pas d'attacher une interface à une instance existante. C'est la seule
  "prévision" incontournable, et elle est côté tueur, pas côté victime.
- La granularité générique s'arrête au `ModelPart` nommé. En creusant `TurtleModel.java`, la
  carapace n'est pas un `ModelPart` séparé mais un `addBox("shell", ...)` *nommé* à l'intérieur du
  part `"body"` — ce nom n'est pas conservé après `bake()` (`ModelPart` n'expose aucun accès public
  à ses cubes). Donc récupérer *juste* la carapace de la tortue par cette méthode n'est pas
  possible tel quel ; il faudrait soit prendre le `"body"` entier (carapace + ventre), soit
  redécouper le modèle vanilla toi-même en sous-parties dans un modèle custom que tu contrôles.
- `MODEL_LAYERS` (EntityType → ModelLayerLocation) et l'ancrage de greffe restent une table à
  maintenir — petite, mais réelle : il n'existe pas d'API vanilla qui fasse cette résolution
  automatiquement pour toi.

---

## Addendum — le tier 2 "flag" ne suffit pas : synthèse générative par observation

> Ajouté lors d'une session de suite. Objection soulevée par l'utilisateur, légitime : le tier 2
> ci-dessus reste un système où **le développeur écrit l'interaction** (`VibrationSensingTrait`
> sait exactement quoi faire) et le mob se contente de cocher un flag qu'un code déjà présent
> consulte. Ce n'est pas "le mob écrit une nouvelle interaction pour un scénario X" — c'est un
> menu de déblocage. Cette section explore comment déplacer réellement le point de généricité.

### La distinction qui compte : effecteurs (finis, pré-écrits) vs composition (générée)

Deux couches à ne pas confondre, sous peine de se mentir sur ce qui est "généré" :

- **Effecteurs** — bouger vers un point, infliger des dégâts de zone, jouer une particule/son,
  appliquer un `MobEffect`. Doivent être pré-écrits en Java, une fois, génériques, en nombre fini.
  Aucune architecture sur la JVM ne contourne ça sans génération de bytecode à la volée (ASM/
  ByteBuddy), déjà écartée plus haut pour cause de fragilité/indébogabilité.
- **Composition** — quand déclencher quel effecteur, dans quel ordre, sous quelle condition, avec
  quels paramètres. C'est cette couche, et seulement elle, qui peut être 100% générée au moment du
  kill, sans qu'aucune ligne n'ait été écrite pour la paire (killer, victime) précise.

Le tier 2 initial code les deux couches en dur dans la même classe (`Trait.onAcquire`). L'addendum
sépare : un **alphabet de primitives génériques** (écrit une fois) + un **synthétiseur** qui
observe une victime et assemble un graphe *que personne n'a écrit pour ce cas* + un **interpréteur
unique** qui exécute n'importe quel graphe fait de ces primitives.

### Alphabet générique (écrit une fois, jamais par monstre)

```java
package net.nuclearteam.createnuclear.api.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

public sealed interface BehaviorNode permits Sensor, Condition, Action, Sequence, Selector {}

public non-sealed interface Sensor extends BehaviorNode {
    // ex: distance à la cible, dernier event de vibration reçu, ligne de vue...
    double sample(LivingEntity self, LivingEntity target);
}

public non-sealed interface Condition extends BehaviorNode {
    boolean test(double sampled);
}

public non-sealed interface Action extends BehaviorNode {
    void run(ServerLevel level, LivingEntity self, LivingEntity target);
}

public record Sequence(List<BehaviorNode> steps) implements BehaviorNode {}
public record Selector(List<BehaviorNode> options) implements BehaviorNode {} // premier qui matche
```

Un interpréteur unique (`BehaviorInterpreter`, non détaillé ici) parcourt ce graphe à chaque tick
ou sur événement — même mécanique que le tier 2 initial, sauf que rien n'y est encore lié à un
monstre précis : c'est une machine à graphes, pas un catalogue de capacités.

### Ce qui bascule réellement : la construction du graphe au moment du kill

```java
package net.nuclearteam.createnuclear.api.behavior;

import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Aucune connaissance de "Warden" ou "Blaze" ici — uniquement des motifs structurels génériques
 * déduits d'un historique de combat (CombatLog, à concevoir), pas d'une classe d'entité.
 */
public final class BehaviorSynthesizer {

    public static Optional<BehaviorNode> synthesize(LivingEntity victim, CombatLog log) {
        List<BehaviorNode> observed = new ArrayList<>();

        // 1) Motif structurel : portée de hit anormalement grande par rapport à la hitbox de la
        //    victime -> pas déclaré "attaque à distance", déduit de la donnée.
        double reach = log.maxHitDistanceFrom(victim);
        if (reach > victim.getBbWidth() * 3) {
            observed.add(new Sequence(List.of(
                new DistanceSensor(),
                new ThresholdCondition(reach * 0.8),
                new ProjectileTowardTargetAction(log.dominantDamageType(victim))
            )));
        }

        // 2) Motif comportemental : la victime a engagé le killer sans ligne de vue directe ->
        //    déduit de l'observation pure, marche même sans VibrationSystem/interface Mojang.
        if (log.engagedWithoutLineOfSight(victim)) {
            observed.add(new Sequence(List.of(
                new VibrationSensor(),
                new AlwaysTrueCondition(),
                new AlertAndPursueAction()
            )));
        }

        if (observed.isEmpty()) return Optional.empty();
        return Optional.of(observed.size() == 1 ? observed.get(0) : new Selector(observed));
    }
}
```

Point central : `synthesize()` ne référence aucun `EntityType` précis. Appliquée sans modification
à n'importe quelle entité future (vanilla ou moddée) présentant un pattern de combat similaire,
elle produit un graphe différent — c'est ça, "le mob écrit une interaction pour le scénario X" sans
que X ait été prévu à l'avance, au sens strict où aucune paire (killer, victime) n'est nommée dans
le code.

`CombatLog` (fenêtre glissante d'événements de combat : dégâts reçus/infligés, distance au moment
du hit, ligne de vue, type de dégât, timestamp) est la pièce manquante qui détermine ce que le
système peut réellement "apprendre" — alimentée par `LivingHurtEvent`, pas seulement
`LivingDeathEvent`. **Non conçue dans ce document, prochaine étape.**

### La combinaison devient de l'algèbre sur les graphes, pas un OR de flags

```java
public static BehaviorNode combine(BehaviorNode existing, BehaviorNode newOne) {
    // Fusion plate : déjà une vraie combinaison au sens où l'arbre résultant (ex: sonar-du-Warden +
    // détonation-du-Creeper, priorisés) n'a été écrit par personne pour ce cas précis, seulement
    // assemblé. Aller plus loin (fusionner deux Sequence qui partagent un Sensor, muter les seuils
    // numériques d'une Condition selon le succès des combats suivants, crossover entre deux graphes
    // acquis) est une piste type "programmation génétique légère" — déterministe et bornable
    // (profondeur max, dédoublonnage, bornes numériques serveur), contrairement à un réseau de
    // neurones. Non explorée plus loin ici.
    return new Selector(List.of(existing, newOne));
}
```

### Ce que ça change par rapport à l'architecture tier 1/2/3 ci-dessus

- Le tier 2 initial (`Trait` + `TraitRegistry` + mapping JSON) devient un **cas particulier
  dégénéré** de ce système : un `Trait` équivaut à un `BehaviorNode` pré-assemblé à la main plutôt
  que synthétisé. Les sondes réflexives (`instanceof VibrationSystem`) restent valables comme *un*
  générateur de `Sensor` parmi d'autres, plus rapide à écrire quand l'interface Mojang existe déjà
  — mais l'observation comportementale pure (`CombatLog`) est la voie générale, qui marche même sur
  des entités moddées n'implémentant aucune interface exploitable.
- "Dépendances d'un comportement" devient trivial une fois les comportements représentés en
  graphe : un `Action` synthétisé référence un `Sensor` évalué avant lui dans le `Sequence`/
  `Selector` — c'est de l'ordre topologique d'un DAG, pas une dépendance ad hoc à câbler à la main.
- Le tier 1 (attributs) et le tier 3 (apparence) ne sont pas remis en cause par cet addendum — ils
  restent 100% génériques par nature (API `Attribute`/`ModelPart` déjà introspectables), sans
  besoin de synthèse.

---

## Points ouverts pour la suite

- **Concevoir `CombatLog`** : quelles données capturer (dégâts reçus/infligés, distance, ligne de
  vue, type de dégât, timestamp), sur quelle fenêtre glissante, avec quel coût mémoire par mob
  vivant — c'est la pièce qui détermine tout ce que `BehaviorSynthesizer` peut déduire. **Prochaine
  étape identifiée, pas encore creusée.**
- Câbler `IrradiatedEvolver` sur un futur mob "chasseur" générique plutôt que sur les mobs
  existants (Wolf/Cat/Chicken irradiés, tous `TamableAnimal`, pas pensés à l'origine pour ça).
- Décider si l'attachment `AcquiredTraits` doit vivre sur `CNAttachmentTypes.java` aux côtés de
  `RadiationCapability`, ou dans un fichier dédié.
- Vérifier le comportement de `EntityTraitMapping` en multi-datapack (fusion de listes déjà gérée
  via `result.merge(...)`, à tester avec de vrais conflits).
- Décider comment gérer les traits qui modifient réellement des `Goal`/`targetSelector` (pas
  couvert dans ce prototype, qui se limite à un flag `hasTrait` consulté par du code déjà présent).
- Écrire `BehaviorInterpreter` (exécution du graphe `BehaviorNode`, à quelle fréquence : par tick,
  sur événement, ou hybride) et les implémentations concrètes de `DistanceSensor`,
  `VibrationSensor`, `ThresholdCondition`, `AlwaysTrueCondition`, `ProjectileTowardTargetAction`,
  `AlertAndPursueAction` esquissées ci-dessus (signatures illustratives, pas encore vérifiées contre
  les vraies contraintes de `ServerLevel`/threading).
- Définir les garde-fous de la synthèse (profondeur max du graphe, dédoublonnage de nœuds
  redondants, bornes numériques serveur) avant d'envisager mutation/crossover entre graphes acquis.

---

# Addendum 2 — Évolution temporelle par paliers de contamination

> Ajouté lors d'une session de suite, sur une question différente de l'acquisition par kill
> (Tiers 1/2/3 et addendum ci-dessus) : faire évoluer un mob irradié **en fonction de son temps
> d'exposition**, pas de qui il a tué. Question initiale de l'utilisateur, légitime : un simple
> timer qui ajoute des attributs aléatoires à chaque tick est-il une voie viable ? Réponse courte
> posée en discussion : non — non-déterministe, pas testable, et sans levier naturel sur
> l'apparence/le comportement. Ce document décrit l'alternative retenue : un compteur de temps
> converti en **paliers discrets** (comme `RadiationCapability` le fait déjà pour les seuils
> d'effet, voir `radiationLevel1/2/3` → `amplifierLevel0/1/2` dans
> [`RadiationCapability.java`](src/main/java/net/nuclearteam/createnuclear/content/radiation/capability/RadiationCapability.java#L209-L212)).
>
> Statut identique au reste du document : **prototype d'évaluation, aucun fichier `.java` créé,
> rien enregistré dans `CreateNuclear.onCtor`**. Tout le code de cette section a été écrit en
> discussion, pas vérifié à la compilation.

## Cahier des charges posé par l'utilisateur

- Nombre de paliers : pas de plafond fixe fixé a priori (`Integer.MAX_VALUE` en théorie).
- Progression comportementale par palier : agressivité envers **toute** entité → puis restreinte
  aux entités de **même espèce/tag** → puis **formation de meute** → puis **fusion par combat**
  (absorber le niveau d'un mutant de même espèce tué).
- Changement de texture/modèle uniquement aux paliers "importants", pas à chaque niveau.
- Temps pour passer de niveau 1→2 : 10 min. De 2→3 : 30 min. Au-delà, niveau `n` = `e^n` minutes.
- Objection soulevée pendant la discussion, tranchée par l'utilisateur : `e^n` minutes littéral
  rend le niveau ~15 (≈7 ans d'exposition continue) déjà quasi inatteignable et le niveau ~30
  dépasse l'âge de l'univers. **Décision retenue : exponentielle jusqu'à un niveau plafond, puis
  croissance linéaire au-delà**, pour que la progression reste atteignable sur le très long terme
  sans jamais heurter un mur numérique.

## Modèle à deux axes : niveau continu vs phase discrète

Point de conception central, pour ne pas répéter l'erreur "un timer qui ajoute des trucs au
hasard" sous une autre forme : **le niveau (entier non borné) et la phase de comportement (une
poignée de valeurs finies) sont deux choses séparées.** Le niveau est un curseur continu qui monte
tout seul avec le temps ; la phase est une petite table de seuils écrite une fois, qui lit ce
curseur. C'est exactement le même principe que `radiationLevel1/2/3 → amplifierLevel0/1/2` déjà
dans le mod — pas une nouveauté architecturale, juste appliqué à un axe temporel au lieu d'un axe
d'intensité.

Autre principe repris du reste du repo (cohérent avec le refacto récent
`d2a92a4 refactor(blueprint): drop stored graphite/uranium timers and derive patternAll on
demand`) : **seuls `level` et `ticksInLevel` sont persistés/synchronisés. Phase et apparence sont
des fonctions pures de `level`, recalculées à la demande, jamais stockées séparément** — pas de
risque de désynchronisation entre "niveau" et "apparence affichée".

## Courbe de temps par niveau — exponentielle bornée puis linéaire

```java
package net.nuclearteam.createnuclear.api.mutation;

/**
 * Ticks nécessaires pour passer du niveau n à n+1. Exponentielle (e^n minutes) jusqu'à LEVEL_CAP,
 * puis linéaire au-delà - continue à la jonction (chaque niveau après le plafond coûte exactement
 * ce qu'a coûté l'atteinte du plafond), pour que les niveaux élevés restent théoriquement
 * atteignables au lieu de heurter un plafond numérique (au-delà de e^~709 un double devient
 * infini, et 1200 ticks/min × e^32 dépasse déjà Long.MAX_VALUE ticks).
 */
public final class LevelCurve {
    private static final int LEVEL_CAP = 10;
    private static final double TICKS_PER_MINUTE = 1200.0;
    private static final double LEVEL_1_MINUTES = 10.0;
    private static final double LEVEL_2_MINUTES = 30.0;
    private static final double CAP_MINUTES = Math.exp(LEVEL_CAP); // ~22026 min (~15,3 jours)

    private LevelCurve() {}

    /** Ticks à passer au niveau n avant de basculer au niveau n+1. */
    public static long ticksToNextLevel(int level) {
        double minutes;
        if (level <= 1) minutes = LEVEL_1_MINUTES;
        else if (level == 2) minutes = LEVEL_2_MINUTES;
        else if (level < LEVEL_CAP) minutes = Math.exp(level);
        else minutes = CAP_MINUTES + (double) (level - LEVEL_CAP) * CAP_MINUTES; // linéaire, pente = CAP_MINUTES/niveau

        return (long) Math.min(minutes * TICKS_PER_MINUTE, Long.MAX_VALUE / 2);
    }
}
```

À `level == LEVEL_CAP` (10), la branche linéaire donne `CAP_MINUTES + 0`, identique à ce qu'aurait
donné l'exponentielle — la jonction est continue, pas de saut brutal au palier 10. Au-delà, chaque
niveau coûte un palier fixe de ~15,3 jours en plus : atteignable sur plusieurs mois/années de jeu
persistant, jamais un mur.

## Attachment persisté (même patron que `RadiationCapability`)

```java
package net.nuclearteam.createnuclear.content.mutation.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Persisté : niveau + temps passé au niveau courant. Phase et apparence sont dérivées à la
 *  demande (voir MutationPhase/VisualStage), jamais stockées. */
public class MutationCapability {
    public static final Codec<MutationCapability> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.optionalFieldOf("level", 0).forGetter(MutationCapability::getLevel),
        Codec.LONG.optionalFieldOf("ticksInLevel", 0L).forGetter(MutationCapability::getTicksInLevel)
    ).apply(i, MutationCapability::create));

    public static final StreamCodec<RegistryFriendlyByteBuf, MutationCapability> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MutationCapability::getLevel,
        ByteBufCodecs.VAR_LONG, MutationCapability::getTicksInLevel,
        MutationCapability::create
    );

    private int level;
    private long ticksInLevel;

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public long getTicksInLevel() { return ticksInLevel; }
    public void setTicksInLevel(long ticks) { this.ticksInLevel = ticks; }

    private static MutationCapability create(int level, long ticksInLevel) {
        MutationCapability cap = new MutationCapability();
        cap.setLevel(level);
        cap.setTicksInLevel(ticksInLevel);
        return cap;
    }
}
```

À enregistrer dans `CNAttachmentTypes.java` à côté de `RADIATION`, même style :
`AttachmentType.builder(MutationCapability::new).serialize(CODEC).sync(STREAM_CODEC)`.

## Phases — petite table finie, pas un palier par niveau

```java
package net.nuclearteam.createnuclear.api.mutation;

public enum MutationPhase {
    SOLO_AGGRESSIVE(1),    // cible toute entité vivante à portée
    SPECIES_AGGRESSIVE(2), // se restreint aux entités de même espèce/tag
    PACK_FORMING(4),       // rejoint/forme une meute de mutants en phase PACK_FORMING+
    COMBAT_FUSION(8);      // absorbe le niveau des mutants de même espèce tués

    public final int minLevel;
    MutationPhase(int minLevel) { this.minLevel = minLevel; }

    public static MutationPhase forLevel(int level) {
        MutationPhase result = SOLO_AGGRESSIVE;
        for (MutationPhase phase : values()) {
            if (level >= phase.minLevel) result = phase;
        }
        return result;
    }
}
```

Les seuils (`1, 2, 4, 8`) sont un point de configuration à trancher plus tard (constantes, config
serveur, ou JSON comme `EntityTraitMapping` — un seul fichier de config, pas un système par
palier). Rien n'empêche `PACK_FORMING`/`COMBAT_FUSION` de rester actifs à tous les niveaux
au-dessus de leur seuil (pas de fenêtre fermante) : un mutant de très haut niveau garde toutes les
capacités des phases précédentes.

## Le hook de tick — accumulation, passage de niveau, dispatch

```java
package net.nuclearteam.createnuclear.content.mutation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.nuclearteam.createnuclear.CNAttachmentTypes;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.mutation.LevelCurve;
import net.nuclearteam.createnuclear.api.mutation.MutatingHost;
import net.nuclearteam.createnuclear.api.mutation.MutationPhase;
import net.nuclearteam.createnuclear.content.mutation.capability.MutationCapability;
import net.nuclearteam.createnuclear.content.radiation.capability.RadiationCapability;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID)
public class MutationHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living && living instanceof MutatingHost host) {
            tick(living, host);
        }
    }

    private static void tick(LivingEntity entity, MutatingHost host) {
        Level level = entity.level();
        if (level.isClientSide) return;
        if (!RadiationCapability.canBeIrradiated(entity)) return;

        RadiationCapability radiation = entity.getData(CNAttachmentTypes.RADIATION);
        boolean contaminated = radiation.getRadiation() > 0 || radiation.getContagionTicks() > 0;
        if (!contaminated) return; // pas d'exposition active = pas de progression, pas de régression non plus

        MutationCapability mutation = entity.getData(CNAttachmentTypes.MUTATION);
        long threshold = LevelCurve.ticksToNextLevel(mutation.getLevel());
        long ticks = mutation.getTicksInLevel() + 1;

        if (ticks >= threshold) {
            int previousLevel = mutation.getLevel();
            mutation.setLevel(previousLevel + 1);
            mutation.setTicksInLevel(0);
            onLevelUp(entity, host, previousLevel, mutation.getLevel());
        } else {
            mutation.setTicksInLevel(ticks);
        }
    }

    private static void onLevelUp(LivingEntity entity, MutatingHost host, int previousLevel, int newLevel) {
        AttributeScaling.apply(entity, newLevel); // toujours : id de modifier stable -> replace, jamais d'empilement

        MutationPhase previousPhase = MutationPhase.forLevel(previousLevel);
        MutationPhase newPhase = MutationPhase.forLevel(newLevel);
        if (newPhase != previousPhase) {
            PhaseGoals.swap(host, newPhase); // change de comportement seulement au changement de phase, pas à chaque niveau
        }
        // VisualStage.forLevel(newLevel) est une fonction pure lue côté client directement depuis
        // MutationCapability#getLevel() synchronisé - aucun champ "visualStage" supplémentaire à stocker/synchroniser.
    }
}
```

`AttributeScaling.apply` : même idée que `AttributeMerger` du Tier 1 plus haut — un
`AttributeModifier` par attribut concerné, id stable (`"mutation_level/" + attributeId`), posé via
`addOrReplacePermanentModifier` avec une valeur recalculée `f(newLevel)` à chaque palier. Remplace
au lieu d'empiler : relire les mêmes 10 minutes de jeu après un crash/reload ne double jamais le
bonus. Non détaillé ici (fonction `f` à définir : linéaire, ou elle-même plafonnée comme
`LevelCurve`).

## Limite réelle non contournable : `goalSelector`/`targetSelector` sont `protected`

Même famille de contrainte JVM que `IrradiatedEvolver` dans l'addendum précédent, pour la même
raison : `Mob#goalSelector` et `Mob#targetSelector` (vanilla) sont des champs `protected`,
inaccessibles depuis une classe hors du package `net.minecraft.world.entity`. Un moteur générique
extérieur ne peut donc pas ajouter/retirer un `Goal` sur un mob quelconque — **le mob mutant doit
exposer lui-même ses sélecteurs**, une fois, sur sa classe :

```java
package net.nuclearteam.createnuclear.api.mutation;

import net.minecraft.world.entity.ai.goal.GoalSelector;

/** À implémenter par toute classe de mob mutant, en renvoyant ses champs goalSelector/
 *  targetSelector protégés existants - le seul câblage "prévu à l'avance" nécessaire, côté mob
 *  mutant lui-même, jamais par palier ni par espèce croisée. */
public interface MutatingHost {
    GoalSelector getMutableGoalSelector();
    GoalSelector getMutableTargetSelector();
}
```

## Swap de comportement au changement de phase

```java
package net.nuclearteam.createnuclear.api.mutation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.nuclearteam.createnuclear.CNTags.CNEntityTags;

/** Un seul Goal de ciblage actif à la fois, remplacé (pas empilé) à chaque changement de phase. */
public final class PhaseGoals {
    private PhaseGoals() {}

    public static void swap(MutatingHost host, MutationPhase newPhase) {
        if (!(host instanceof Mob mob)) return;
        GoalSelector targetSelector = host.getMutableTargetSelector();

        targetSelector.removeAllGoals(goal -> goal instanceof PhaseTargetGoal);

        switch (newPhase) {
            case SOLO_AGGRESSIVE -> targetSelector.addGoal(1,
                new PhaseTargetGoal(mob, e -> true)); // toute entité vivante
            case SPECIES_AGGRESSIVE, PACK_FORMING, COMBAT_FUSION -> targetSelector.addGoal(1,
                new PhaseTargetGoal(mob, e ->
                    !e.getType().is(CNEntityTags.IRRADIATED_IMMUNE.tag) && e.getType() == mob.getType()));
        }
        // PACK_FORMING/COMBAT_FUSION ajoutent en plus leurs propres Goals sans toucher au
        // ciblage - voir les deux sections suivantes, non finalisées.
    }

    /** Marqueur pour retrouver/retirer le Goal installé par la phase précédente. */
    private static class PhaseTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        PhaseTargetGoal(Mob mob, java.util.function.Predicate<LivingEntity> predicate) {
            super(mob, LivingEntity.class, 10, true, false, (e, l) -> predicate.test(e));
        }
    }
}
```

**Point à vérifier avant implémentation réelle** (marqué comme tel, pas confirmé) : la signature
exacte du dernier paramètre de `NearestAttackableTargetGoal` (prédicat `TargetingConditions`-based
vs `BiPredicate<LivingEntity, ServerLevel>` vs autre) dépend des mappings/version exacts — à
recouper avec les sources décomposées comme indiqué dans le prompt de vérification tout en haut du
document, avant de copier ce code tel quel.

## Apparence — paliers "importants", pas un par niveau

```java
package net.nuclearteam.createnuclear.api.mutation;

/** Fonction pure : quel "stage" visuel afficher pour un niveau donné. Paliers rapprochés au
 *  début (1, 2, 4, 8, correspondant aux changements de phase), puis en puissances de 2 - cohérent
 *  avec le fait que le temps entre niveaux explose déjà après LEVEL_CAP, donc les changements
 *  visuels ralentissent naturellement avec la progression. */
public final class VisualStage {
    private VisualStage() {}

    public static int forLevel(int level) {
        if (level < 1) return 0;
        if (level < 8) return Integer.numberOfTrailingZeros(Integer.highestOneBit(Math.max(level, 1))) + 1;
        return 31 - Integer.numberOfLeadingZeros(level); // floor(log2(level)), paliers en puissances de 2 au-delà
    }
}
```

Chaque valeur de `forLevel` pointe vers un couple `(ModelLayerLocation, ResourceLocation texture)`
pré-modélisé à la main dans une petite table statique (`Map<Integer, VisualStageAssets>`) — **pas
de génération procédurale de modèle**, cohérent avec la limite déjà posée dans l'addendum
précédent sur `ModelPart`/`PartDefinition`. Le renderer du mob mutant lit
`VisualStage.forLevel(mutation.getLevel())` à chaque frame à partir du `level` déjà synchronisé,
sans champ supplémentaire.

## Formation de meute et fusion par combat — précision de l'utilisateur

> Clarification apportée en discussion : `COMBAT_FUSION` n'est pas "tuer n'importe quel mutant de
> même espèce absorbe son niveau" (ce que le brouillon ci-dessus laissait entendre). Le vrai
> scénario : **deux meutes, chacune avec un chef de même niveau ; les deux chefs s'affrontent, et
> le vainqueur fusionne la meute du vaincu dans la sienne.** Ça couple `PACK_FORMING` et
> `COMBAT_FUSION` — la fusion n'a de sens qu'une fois la notion de meute/chef posée, donc les deux
> sont détaillées ensemble ici plutôt que traitées comme deux bullets séparés.
>
> Précision d'interprétation à confirmer avec l'utilisateur si erronée : le chef vaincu meurt dans
> l'affrontement (c'est un combat, pas une négociation), donc la meute résultante ne peut pas être
> dirigée par lui — le code ci-dessous fait toujours atterrir les membres absorbés sous le chef
> **vainqueur**, quel que soit le sens exact de "fusionne sa meute à celle du vaincu" dans la
> formulation d'origine.

### Registre de meutes (support commun aux deux phases)

```java
package net.nuclearteam.createnuclear.api.mutation;

import java.util.*;

/**
 * Registre serveur en mémoire : quelles meutes existent, qui est le chef, qui sont les membres.
 * Pas de persistance dans ce prototype (à porter sur SavedData si retenu - une meute qui
 * disparaît au redémarrage du serveur n'est pas forcément un problème, à trancher plus tard).
 */
public final class PackCoordinator {
    private static final Map<UUID, Pack> PACKS_BY_ID = new HashMap<>();
    private static final Map<UUID, UUID> PACK_BY_MEMBER = new HashMap<>(); // mutantId -> packId

    private PackCoordinator() {}

    public static final class Pack {
        final UUID id = UUID.randomUUID();
        UUID leaderId;
        final Set<UUID> memberIds = new HashSet<>();
    }

    /** Un mutant qui atteint PACK_FORMING sans meute à proximité devient chef de sa propre meute
     *  solo - le Goal de ralliement (non détaillé ici) le fait rejoindre une meute existante si
     *  un autre membre de même phase est trouvé à portée avant l'appel à cette méthode. */
    public static Pack createSoloPack(UUID leaderId) {
        Pack pack = new Pack();
        pack.leaderId = leaderId;
        pack.memberIds.add(leaderId);
        PACKS_BY_ID.put(pack.id, pack);
        PACK_BY_MEMBER.put(leaderId, pack.id);
        return pack;
    }

    public static Optional<Pack> packOf(UUID mutantId) {
        return Optional.ofNullable(PACK_BY_MEMBER.get(mutantId)).map(PACKS_BY_ID::get);
    }

    public static boolean isLeader(UUID mutantId) {
        return packOf(mutantId).map(p -> p.leaderId.equals(mutantId)).orElse(false);
    }

    /** Le pack du vainqueur absorbe tous les membres du pack du vaincu (chef vaincu inclus, mort
     *  mais gardé en trace) ; le pack du vaincu disparaît. */
    public static void mergeLoserIntoWinner(UUID winnerId, UUID loserId) {
        Pack winnerPack = packOf(winnerId).orElseGet(() -> createSoloPack(winnerId));
        packOf(loserId).ifPresent(loserPack -> {
            for (UUID memberId : loserPack.memberIds) {
                winnerPack.memberIds.add(memberId);
                PACK_BY_MEMBER.put(memberId, winnerPack.id);
            }
            PACKS_BY_ID.remove(loserPack.id);
        });
    }
}
```

### Ciblage prioritaire chef-contre-chef, ajouté à `PhaseGoals.swap`

Le ciblage `SPECIES_AGGRESSIVE` déjà en place couvre les membres non-chefs ; `COMBAT_FUSION` ajoute
un second `Goal`, **de priorité plus haute** (donc évalué en premier par le `GoalSelector` — plus
petit nombre = plus prioritaire, convention vanilla), actif seulement si le mutant est lui-même
chef, et qui ne cible que d'autres chefs de même espèce **et même niveau exact** :

```java
// dans PhaseGoals, case COMBAT_FUSION du switch précédent :
case COMBAT_FUSION -> {
    targetSelector.addGoal(1, new PhaseTargetGoal(mob, speciesPredicate(mob))); // hérité de SPECIES_AGGRESSIVE
    if (PackCoordinator.isLeader(mob.getUUID())) {
        targetSelector.addGoal(0, new PhaseTargetGoal(mob, rivalLeaderPredicate(mob)));
    }
}
```

```java
private static java.util.function.Predicate<LivingEntity> speciesPredicate(Mob mob) {
    return e -> !e.getType().is(CNTags.CNEntityTags.IRRADIATED_IMMUNE.tag) && e.getType() == mob.getType();
}

/** Ne cible que les chefs de meute rivaux, de même espèce, au niveau exactement identique - la
 * condition "même niveau" posée par l'utilisateur, pas "n'importe quel chef ennemi". */
private static java.util.function.Predicate<LivingEntity> rivalLeaderPredicate(Mob self) {
    int selfLevel = self.getData(CNAttachmentTypes.MUTATION).getLevel();
    return e -> e.getType() == self.getType()
        && e instanceof MutatingHost
        && PackCoordinator.isLeader(e.getUUID())
        && e.getData(CNAttachmentTypes.MUTATION).getLevel() == selfLevel;
}
```

### Le hook de fusion — `LivingDeathEvent`, même point d'insertion que l'addendum précédent

```java
package net.nuclearteam.createnuclear.content.mutation;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.nuclearteam.createnuclear.CNAttachmentTypes;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.mutation.MutatingHost;
import net.nuclearteam.createnuclear.api.mutation.PackCoordinator;
import net.nuclearteam.createnuclear.content.mutation.capability.MutationCapability;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID)
public class PackFusionHandler {

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return;
        if (!(victim instanceof MutatingHost) || !(killer instanceof MutatingHost)) return;

        // Fusion réservée aux duels chef contre chef - un chef qui tue un simple membre de meute
        // rivale (via le Goal SPECIES_AGGRESSIVE hérité) ne déclenche aucune fusion.
        if (!PackCoordinator.isLeader(victim.getUUID()) || !PackCoordinator.isLeader(killer.getUUID())) return;

        MutationCapability victimCap = victim.getData(CNAttachmentTypes.MUTATION);
        MutationCapability killerCap = killer.getData(CNAttachmentTypes.MUTATION);
        if (victimCap.getLevel() != killerCap.getLevel()) return; // condition posée par l'utilisateur : même niveau exact

        PackCoordinator.mergeLoserIntoWinner(killer.getUUID(), victim.getUUID());
    }
}
```

Le `Goal` de cohésion des membres absorbés (non détaillé ici, mentionné dans le "non conçu"
ci-dessous) n'a rien à faire de spécial au moment de la fusion : il relit `PackCoordinator.packOf`
en continu, donc un membre dont le pack a changé d'ID se met à suivre le nouveau chef dès le tick
suivant, sans événement dédié à câbler.

## Ce qui reste à l'état d'esquisse, pas de code vérifié

- **Ralliement/cohésion de meute** : le `Goal` de recherche du membre le plus proche de même phase
  à portée (pour rejoindre une meute existante plutôt que rester en `createSoloPack`), et le `Goal`
  de cohésion (`FollowPackLeaderGoal` ou équivalent, qui relit `PackCoordinator.packOf` en continu
  comme noté plus haut). Pas différent dans l'esprit des meutes de loups vanilla, mais rien de
  réutilisable tel quel ici (`Wolf` vanilla n'expose pas son mécanisme de meute publiquement).
  **Non conçu dans ce document.**
- **Persistance de `PackCoordinator`** : le registre proposé est en mémoire pure (`HashMap`
  statique) — une meute ne survit pas à un redémarrage serveur. À porter sur `SavedData` si retenu
  en l'état, ou accepter la perte comme comportement voulu. **Non tranché.**
- **Cas des chefs de niveaux différents** : la condition de fusion posée par l'utilisateur exige un
  niveau exactement identique. Rien n'est spécifié pour un duel entre chefs de niveaux différents
  (pas de fusion, mais le combat lui-même a-t-il lieu ? Le `Goal` `rivalLeaderPredicate` ci-dessus
  ne les cible simplement jamais l'un l'autre en priorité — ils peuvent toujours s'entre-tuer via le
  `Goal` `SPECIES_AGGRESSIVE` hérité, sans fusion). **Comportement par défaut, pas explicitement
  demandé.**
- **`AttributeScaling.apply`** : la fonction `f(level) -> valeur de modifier` par attribut n'est
  pas définie ici — à choisir en cohérence avec `LevelCurve` (elle-même plafonnée) pour éviter
  qu'un mutant de très haut niveau devienne un one-shot-kill par simple empilement linéaire non
  borné.
- Enregistrement réel : `MutationCapability` dans `CNAttachmentTypes.java`, `MutationHandler`/
  `PhaseGoals`/`VisualStage` dans `CreateNuclear.onCtor(...)`, et **au moins un mob candidat qui
  implémente `MutatingHost`** — aucun des trois mobs irradiés existants (`IrradiatedWolf/Cat/
  Chicken`, tous `TamableAnimal`) n'a été pensé pour ça à l'origine (même remarque que dans les
  "points ouverts" de l'addendum précédent).

---

# Addendum 3 — Justification fictionnelle du gain de comportement par kill (pas encore tranchée)

> Note issue d'une session de suite, sur une question de cohérence narrative (pas de réalisme,
> de fiction) posée par l'utilisateur à propos de l'addendum "synthèse générative par observation"
> ci-dessus : est-ce que le mécanisme "un mob irradié tue un Warden, en récupère une capacité type
> sonar" tient debout comme histoire pour une créature *mutée par la radiation*, ou est-ce un
> emprunt technique (le `VibrationSystem` du Warden existe déjà côté vanilla) habillé après coup ?

## Le problème identifié

"Absorber la capacité de ce qu'on tue" est un trope de body-horror/looter générique (The Thing,
Rogue chez Marvel, les jeux où manger un ennemi donne son pouvoir) — **pas** un trope spécifique à
la fiction nucléaire. Godzilla, les goules de Fallout, les mutants de S.T.A.L.K.E.R. mutent
l'organisme **lui-même** (gigantisme, déformation, nouvelles capacités émergeant de sa propre
biologie) ; aucun de ces référents ne donne de mécanisme pour voler un trait à un tiers. Justifier
le sonar par "il a tué un Warden donc il a son sonar" est donc plausible en survol, mais plaqué :
ça raconte une histoire de vol de pouvoir qui se trouve avoir de la radiation en toile de fond, pas
une histoire de radiation.

## Piste de recadrage (non tranchée)

Remplacer la causalité "transfert direct depuis la victime" par "la radiation accélère le taux de
mutation, et le stress extrême d'un combat de survie contre une menace inhabituelle **déclenche**
cette mutation accélérée dans une direction qui répond à cette menace précise" :

- La radiation est l'accélérateur d'évolution dirigée — cohérent avec le reste du corpus (c'est
  déjà son rôle dans `RadiationCapability`/Addendum 2).
- Le combat contre la victime est le **signal de stress** qui oriente la mutation suivante vers une
  réponse à ce type de menace précis — pas une pompe qui transfère littéralement une capacité.
- Mécaniquement, **aucun changement** : c'est toujours `LivingDeathEvent` → `CombatLog` du combat →
  motif structurel détecté → `BehaviorNode` synthétisé (cf. addendum "synthèse générative" plus
  haut). Un kill de Warden produit déjà, sans qu'aucune ligne ne nomme "Warden", le motif
  structurel *"la victime a engagé le killer sans ligne de vue directe"* — la même signature
  qu'un Warden traquant par vibration laisserait dans le `CombatLog`. Le sonar "s'hérite" donc du
  vécu du combat, pas d'un `if victim instanceof Warden` explicite : fiction recadrée et contrainte
  de généricité (aucune connaissance d'`EntityType` précis dans le synthétiseur) restent
  compatibles sans effort supplémentaire.

## Statut

Idée jugée intéressante par l'utilisateur, **pas implémentée, pas définitivement adoptée** — à
retenir comme angle par défaut si/quand ce pan du prototype est repris, mais rien dans le code des
addendums précédents ne dépend de ce choix narratif précis (il ne change que le texte de flavor,
pas `BehaviorSynthesizer`/`CombatLog`).
