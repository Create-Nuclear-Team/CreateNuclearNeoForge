# Parité Forge → NeoForge — ce qu'il reste à porter

Ce document liste tout ce qui sépare encore `CreateNuclearNeoForge` de `CreateNuclearForge`
en termes de **features**, et comment porter chaque bloc.

État au **7 août 2026**, branche `V2-Reacteur`.
Pour le domaine réacteur, déjà porté et validé, voir [`PORTAGE_REACTEUR.md`](PORTAGE_REACTEUR.md).

| | Forge | NeoForge |
|---|---|---|
| Fichiers `.java` | 296 | 290 |
| Ressources | 1 132 | 1 115 |

### Journal

| Date | Chantier | Commit |
|---|---|---|
| 7 août | §3.1 Jauges DisplayLink — **fait** | `3f99c87` |
| 7 août | Bug de persistance du contrôleur (hors roadmap, trouvé en chemin) | `a8d1f1b` |
| 7 août | §1 Alignement des 8 noms de fichiers divergents — **fait** | voir §1 |

---

## 0. Comment lire ce document

Un `diff` brut des arborescences donne **42 fichiers Forge sans équivalent NeoForge**.
Ce chiffre est trompeur :

- **8 existent, sous un autre nom** → §1, à ne jamais reporter comme manquants ;
- **4 sont remplacés par un équivalent 1.21 meilleur** → §2, à ne surtout pas « re-porter » ;
- **30 manquent réellement** → §3, c'est le vrai travail.

S'y ajoutent des écarts *à l'intérieur* de fichiers présents des deux côtés (§4), invisibles
d'un diff d'arborescence — et c'est là qu'ont été trouvés la moitié des bugs du réacteur.

---

## 1. ~~Faux positifs — présents, mais renommés~~ ✅ RÉSOLU le 7 août

**Les 8 noms sont désormais identiques des deux côtés.** Cette section ne décrit plus un écart
à connaître, mais un travail fait — elle est conservée pour l'historique.

Sept renommages ont eu lieu **côté NeoForge**, pour s'aligner sur Forge (référence) :

| Avant (NeoForge) | Après (= Forge) | Motif |
|---|---|---|
| `content/rod/CNRodTypes` | `content/multiblock/rod/CNRodTypes` | package différent |
| `foundation/damage**s**Types/` | `foundation/damageTypes/` | le **dossier** seul était fautif — le `package` déclaré dans le fichier était déjà correct, donc dossier et package se contredisaient |
| `CNArmorMaterials` | `ArmorMaterials` | préfixe superflu |
| `infrastructure/config/CExplo**s**e` | `CExplode` | faute de frappe |
| `PlayerInteract**e**ReactorFluidInput` | `PlayerInteractReactorFluidInput` | faute de frappe |
| `IrradiatedCatLieOnBedGoal` | `CatLieOnBedGoal` | préfixe superflu |
| `IrradiatedCatSitOnBlockGoal` | `CatSitOnBlockGoal` | préfixe superflu |

Le huitième était l'inverse — **la faute était côté Forge**, donc c'est **Forge** qui a été
renommé, pour que les deux convergent vers l'orthographe correcte :

| Avant (Forge) | Après (= NeoForge) |
|---|---|
| `content/biome/BiomeIrrad**a**tionExtractorItem` | `BiomeIrrad**ia**tionExtractorItem` |

> ⚠️ **Trois de ces noms masquent volontairement une classe vanilla** :
> `ArmorMaterials` (`net.minecraft.world.item.ArmorMaterials`), `CatLieOnBedGoal` et
> `CatSitOnBlockGoal` (`net.minecraft.world.entity.ai.goal.*`). Les fichiers concernés importent
> le package vanilla en wildcard (`import net.minecraft.world.item.*;`,
> `import net.minecraft.world.entity.ai.goal.*;`) — c'est **sans danger** parce qu'en Java une
> classe du même package l'emporte sur un import à la demande, et qu'aucun de ces fichiers
> n'utilise la classe vanilla homonyme. Forge vit avec cette situation depuis toujours.
> **Mais si un jour l'un de ces fichiers a besoin de la version vanilla, il faudra la qualifier
> complètement** — un `import` simple ne suffira pas.
>
> `BiomeIrradiationExtractorItem.TAG` vaut `"biome_restore"` et l'item est enregistré sous
> `biome_irradiation_extractor` : le renommage Forge **ne touche donc aucune ressource**, ni
> aucun monde existant.

---

## 2. Volontairement non portés — l'équivalent 1.21 est meilleur

**Ne pas porter ces fichiers.** Les recréer serait une régression.

| Fichier Forge | Remplacé côté NeoForge par |
|---|---|
| `content/radiation/capability/IRadiationCapability` | **Data attachments NeoForge** — `RadiationCapability` expose `AttachmentType<RadiationCapability> RADIATION`, lu via `entity.getData(RADIATION)` |
| `content/radiation/capability/RadiationProvider` | idem — la plomberie `ICapabilityProvider` de Forge n'a pas d'équivalent, ni de raison d'exister |
| `foundation/advancement/CriterionTriggerBase` | **`SimpleCriterionTrigger` vanilla** — `SimpleCreateNuclearTrigger` en hérite directement |
| `foundation/utility/SimplexNoise` | **`net.minecraft.world.level.levelgen.synth.SimplexNoise`** — `Maths` utilise déjà celui de vanilla |

> ⚠️ La radiation mérite quand même une **vérification fonctionnelle** : `RadiationCapability`
> diverge de 186 lignes entre les deux versions. Le mécanisme est correct, mais rien ne dit que
> le comportement (seuils, décroissance, persistance à la mort) est identique. Ce n'est pas un
> portage de fichier, c'est un test en jeu à faire.

---

## 3. Les 30 fichiers réellement manquants

Classés par feature, du plus au moins impactant.

---

### 3.1 ~~Jauges DisplayLink du réacteur — 9 fichiers~~ ✅ FAIT le 7 août (`3f99c87`)

> **Ce que le diagnostic ci-dessous sous-estimait.** La feature n'était pas *incomplète* côté
> NeoForge, elle était **entièrement morte** : `ReactorSummaryDisplaySource` existait mais
> n'était référencée nulle part — ni `CNDisplaySources`, ni le moindre
> `.transform(displaySource(...))` dans `CNBlocks`. Aucune source n'était donc proposée sur un
> Display Link, pas même le résumé.
>
> Deux bugs trouvés au passage dans `ReactorSummaryDisplaySource` :
> - carburant, refroidisseur, fluide et chaleur étaient **commentés** (faute de
>   `ReactorGaugeRenderer`), ainsi que les branches « gauge » de `formatValue`/`formatFluid` ;
> - la chaleur était lue via `getConfiguredPattern().get(CUSTOM_DATA).copyTag()` — le piège n°1
>   du §6 — et valait donc **toujours 0**. Remplacé par `getConfiguredPatternHeat()`.
>
> La question du doublon `ReactorGaugeOverrides` est tranchée : **pas de doublon**. C'est un
> générateur de modèle d'item (datagen) pour la jauge du bloc `reactor_frame`, sans rapport avec
> `ReactorGaugeRenderer` qui dessine des barres `█▒` en texte.
>
> Toutes les clés de lang `display_source.*` étaient déjà présentes dans `interface.json`,
> identiques à Forge. Aucune ressource à ajouter.

<details>
<summary>Diagnostic d'origine</summary>

#### Jauges DisplayLink du réacteur — 9 fichiers ⭐ priorité 1

`CNDisplaySources` · `AbstractReactorStatDisplaySource` · `HeatDisplaySource` ·
`FuelDisplaySource` · `CoolerDisplaySource` · `LiquidLevelDisplaySource` ·
`ReactorSizeDisplaySource` · `ReactorDisplayConstants` · `ReactorGaugeRenderer`

**Ce qui manque en jeu :** impossible de brancher un Display Link sur une statistique précise
du réacteur. NeoForge n'a que `ReactorSummary` + `ReactorSummaryDisplaySource`, soit un résumé
global — pas la chaleur seule, le carburant seul, le niveau de liquide, ni la taille.

**Points d'accroche.** Côté Forge, `CNBlocks` attache **six sources** au réacteur, à deux
endroits (le contrôleur et un second bloc, lignes 84-89 et 196-201) :

```java
.transform(displaySource(CNDisplaySources.HEAT))
.transform(displaySource(CNDisplaySources.LIQUID_LEVEL))
.transform(displaySource(CNDisplaySources.FUEL))
.transform(displaySource(CNDisplaySources.COOLER))
.transform(displaySource(CNDisplaySources.REACTOR_SIZE))
.transform(displaySource(CNDisplaySources.REACTOR_SUMMARY))
```

`CNDisplaySources.register()` doit aussi être appelé depuis `CreateNuclear.java`.

**Difficulté : faible.** Aucun prérequis — le lot 6 a déjà mis en place tout ce dont ces sources
ont besoin : `ReactorDisplayState`, `getMultiblockSize()`, `getInputFluidManager()`,
`getConfiguredPatternHeat()`. C'est de la traduction d'API quasi pure.

**À savoir :** NeoForge possède en plus un `content/multiblock/frame/ReactorGaugeOverrides`
qui n'existe pas côté Forge — vérifier s'il fait doublon avec `ReactorGaugeRenderer` avant de
porter, pour ne pas se retrouver avec deux systèmes de jauges.

</details>

---

### 3.2 Poudre de neige (Snow Powder) — 4 fichiers ⭐ priorité 2

`content/kinetics/fan/processing/SnowPowderRecipe` · `api/data/recipe/SnowPowderRecipeGen` ·
`foundation/data/recipe/CNSnowPowderRecipeGen` · `compat/jei/category/FanSnowPowderCategory`

**Ce qui manque en jeu :** un type de recette de ventilateur entier, avec sa catégorie JEI.
Aucune trace côté NeoForge — `CNRecipeTypes` n'a pas d'entrée `SNOW_POWDER`.

**Points d'accroche :**
- `CNRecipeTypes` — ajouter `SNOW_POWDER(SnowPowderRecipe::new)`
- `CreateNuclearJEI` — enregistrer la catégorie (`builder(SnowPowderRecipe.class)`)
- le datagen `CNSnowPowderRecipeGen` doit être branché au provider de recettes
- vérifier les ressources associées (textures/lang de la catégorie JEI)

**Difficulté : moyenne.** L'API des recettes Create a changé en 1.21 ; s'inspirer d'un type de
recette déjà porté (`CNEnrichedRecipeGen`, `EnrichedRecipeGen`) plutôt que de traduire à l'aveugle.

---

### 3.3 Vache irradiée + abstraction des animaux — 5 fichiers ⭐ priorité 3

`IrradiatedCow` · `IrradiatedCowModel` · `IrradiatedCowRenderer` ·
`IrradiatedAnimal` · `AnimalUtil`

**Ce qui manque en jeu :** NeoForge a le **chat**, le **poulet** et le **loup** irradiés
(`CNEntityType` : `IRRADIATED_CAT`, `IRRADIATED_CHICKEN`, `IRRADIATED_WOLF`) — **pas la vache**.

**Le vrai enjeu est l'abstraction.** Forge a factorisé le comportement commun dans
`IrradiatedAnimal` + `AnimalUtil` ; NeoForge ne les a pas, donc **chaque animal duplique la
logique**. C'est ce qui explique les plus gros diffs hors réacteur (`IrradiatedCat` 384 lignes,
`IrradiatedWolf` 333). Porter l'abstraction **en premier**, puis y ramener chat/poulet/loup,
puis ajouter la vache — sinon on ajoute une quatrième copie du même code.

**Points d'accroche :**
- `CNEntityType` — `EntityEntry<IrradiatedCow>`, renderer, `createAttributes`
- `CNModelLayers` — `IRRADIATED_COW` + `registerLayerDefinition(..., IrradiatedCowModel::createBodyLayer)`
- ressources : modèle, texture, lang, table de butin, œuf d'apparition

**Difficulté : moyenne à élevée.** Le refactor de l'abstraction touche trois entités existantes
qui fonctionnent aujourd'hui — à faire avec un test en jeu de chaque animal.

---

### 3.4 Worldgen piloté par la config — 2 fichiers

`infrastructure/worldgen/CNPlacementModifiers` · `infrastructure/worldgen/ConfigPlacementFilter`

**Ce qui manque en jeu :** un filtre de placement qui lit la config avant de générer un minerai.
Sans lui, **les options de `CWorldGen` ne sont probablement pas respectées** : désactiver un
minerai dans la config n'a aucun effet.

**Points d'accroche :**
- `CreateNuclear.java:80` → `CNPlacementModifiers.register(modEventBus)`
- `CNPlacedFeatures` (lignes 51 et 60 côté Forge) → `ConfigPlacementFilter.INSTANCE` dans la
  liste des modificateurs de placement

**Difficulté : faible à moyenne.** Le `PlacementModifierType` se déclare de la même façon en
1.21, mais l'enregistrement passe par un `DeferredRegister` NeoForge.

⚠️ **À vérifier avant de porter** : NeoForge n'a peut-être aucun filtre, ou en a un autre. Un
`runData` sera nécessaire, et les features placées changeront — donc **monde de test neuf**.

---

### 3.5 Datagen de recettes — 2 fichiers

`foundation/data/recipe/CNDeployingRecipeGen` · `foundation/data/recipe/CNProcessingRecipeGen`

**Ce qui manque en jeu :** des recettes qui existent côté Forge et pas côté NeoForge —
déployeur, et une base commune de recettes de traitement.

**Difficulté : faible**, mais **impact direct sur la progression du joueur** : des objets
peuvent être tout simplement non craftables. À vérifier en jeu, JEI en main, en comparant les
deux versions côte à côte.

---

### 3.6 Compat Alex's Caves — 2 fichiers

`compat/Mods` · `compat/alexscave/AlexscaveCompat`

**Ce qui manque en jeu :** l'explosion nucléaire n'a aucune intégration avec Alex's Caves.

**Point d'accroche.** `NuclearExplosionEntity` côté Forge :

```java
if (Mods.ALEXS_CAVE.isLoaded()) {
    this.alexscaveHandler = new AlexscaveCompat();
}
```

Le champ est typé `Object` volontairement, pour que la classe se charge même sans Alex's Caves.
Côté NeoForge, `NuclearExplosionEntity` n'en a aucune trace.

`Mods` est un petit utilitaire générique de détection de mods — utile bien au-delà de cette compat.

**Difficulté : faible.** `ModList.get().isLoaded(id)` existe à l'identique en NeoForge.

---

### 3.7 Mixins client — 2 fichiers

`foundation/mixin/client/CameraAccessor` · `foundation/mixin/client/GameRendererMixin`

**Ce qui manque en jeu :** un effet visuel, très probablement lié au rendu de la radiation ou
de l'explosion (accès à la caméra + hook sur le `GameRenderer`).

NeoForge a à la place `AntiRadiationArmorTextureMixin`, qui n'existe pas côté Forge : les deux
versions ont donc **divergé sur les mixins client**. Comparer les deux `createnuclear.mixins.json`
avant de porter.

**Difficulté : moyenne.** Les mixins sont sensibles aux versions et cassent silencieusement.
À porter avec un test visuel explicite.

---

### 3.8 Divers — 4 fichiers

| Fichier | Ce qui manque en jeu | Point d'accroche |
|---|---|---|
| `foundation/item/RodsStats` | tooltip détaillé des barres (chaleur, durée de vie) | `CreateNuclear.java` — chaîné après `KineticStats.create(item)` dans le `TooltipModifier`. NeoForge a `KineticStats` mais **pas** `RodsStats` |
| `foundation/events/RodsTooltipHandler` | idem, côté événement | à brancher sur le bus d'événements |
| `content/uraniumOre/UraniumOreItem` | l'item de minerai d'uranium irradie le joueur qui le porte | `CNBlocks` lignes 378 et 444 côté Forge : `.item((b, p) -> new UraniumOreItem(b, p, 3))`. NeoForge n'utilise ni `UraniumOreItem` ni `RadiationItem` sur ces blocs |
| `CNOpenPipeEffectHandlers` | effets appliqués par les tuyaux ouverts Create | `CreateNuclear.java:105` — `CNOpenPipeEffectHandlers.registerDefaults()` |

**Difficulté : faible** pour les quatre. Ce sont de bons premiers tickets.

---

## 4. Écarts internes aux fichiers partagés — non audités

Ces fichiers existent des deux côtés mais divergent fortement. **Un gros diff n'est pas une
preuve de feature manquante** : l'API 1.21 en explique une grande part. Aucun n'a été audité
ligne à ligne, contrairement au domaine réacteur.

| Fichier | Lignes divergentes |
|---|---|
| `CNBlocks` | 701 |
| `CNItems` | 457 |
| `foundation/data/recipe/CNStandardRecipeGen` | 395 |
| `foundation/advancement/CNAdvancement` | 394 |
| `content/contraptions/irradiated/cat/IrradiatedCat` | 384 |
| `compat/jei/CreateNuclearJEI` | 344 |
| `content/contraptions/irradiated/wolf/IrradiatedWolf` | 333 |
| `CNCreativeModeTabs` | 249 |
| `content/radiation/capability/RadiationCapability` | 186 |

> **Ce n'est pas théorique.** Sur les six bugs trouvés en testant le réacteur en jeu, **deux
> étaient cachés dans `CNBlocks`** : la capacité de stress de la sortie à `10240` au lieu de
> `64000` (SU divisé par 6,25) et quatre `addLayer` manquants — dont `reinforced_glass` qui
> rendait donc opaque. Aucun des deux n'était visible dans un diff d'arborescence.
>
> **Méthode qui a marché :** ne pas lire le diff en entier, mais partir d'un symptôme observé en
> jeu et remonter. C'est plus rapide et ça ne trouve que des bugs réels.

---

## 5. Ordre de travail suggéré

| # | Chantier | Fichiers | Difficulté | Pourquoi ce rang |
|---|---|---|---|---|
| ~~1~~ | ~~**Jauges DisplayLink**~~ ✅ | 9 | faible | Fait le 7 août — `3f99c87` |
| 2 | **Divers (§3.8)** | 4 | faible | Quatre gains rapides et indépendants |
| 3 | **Datagen recettes** | 2 | faible | Peut bloquer la progression du joueur |
| 4 | **Worldgen config** | 2 | faible/moy. | Silencieux mais fausse la génération |
| 5 | **Compat Alex's Caves** | 2 | faible | Isolé, sans risque |
| 6 | **Snow Powder** | 4 | moyenne | Feature complète, API recettes à traduire |
| 7 | **Abstraction animaux + vache** | 5 | moy./élevée | Refactor de 3 entités qui marchent |
| 8 | **Mixins client** | 2 | moyenne | Sensible, à faire avec test visuel |
| 9 | **Audit `CNBlocks` / `CNItems`** | — | continu | À faire au fil des symptômes, pas d'un bloc |

---

## 6. Méthode de portage — ce qui a marché sur le réacteur

À reprendre pour chaque chantier ci-dessus.

1. **Porter à l'identique par défaut.** Copier le fichier Forge, ne changer que ce que l'API
   1.21 impose. Sur le réacteur, ~35 fichiers sur 50 sont restés **identiques à l'octet**.
2. **Vérifier chaque fichier par un `diff`** contre son original Forge, et **justifier chaque
   ligne qui diffère**. C'est ce qui a permis de garder les deux versions synchronisables.
3. **Compiler après chaque lot**, pas à la fin.
4. **Un commit par lot**, poussé avant d'enchaîner.
5. **Documenter toute divergence assumée** dans le commit, pour qu'une future synchro ne la
   « corrige » pas par erreur.
6. **Tester en jeu.** Tout le portage du réacteur compilait et passait les gametests avant que
   six bugs bien réels ne sortent au premier test manuel.

### Pièges 1.20.1 → 1.21 déjà rencontrés

| Forge 1.20.1 | NeoForge 1.21 |
|---|---|
| `net.minecraftforge.*` | `net.neoforged.neoforge.*` / `net.neoforged.api.*` |
| `ForgeRegistries.X.getKey(v)` | `BuiltInRegistries.X.getKey(v)` |
| `ForgeRegistries.X.getValue(rl)` | `BuiltInRegistries.X.getOptional(rl)` |
| `new ResourceLocation(s)` | `ResourceLocation.parse(s)` |
| `read/write(CompoundTag, boolean)` | `read/write(CompoundTag, HolderLookup.Provider, boolean)` |
| `ItemStack.of(tag)` | `ItemStack.parse(provider, tag)` → `Optional` |
| `stack.serializeNBT()` | `stack.saveOptional(provider)` |
| `stack.getOrCreateTag()` | **DataComponents** — pas d'équivalent mutable |
| `use(...)` → `InteractionResult` | `useItemOn(...)` → `ItemInteractionResult` |
| `NetworkHooks.openScreen(player, be, buf)` | `player.openMenu(be, buf)` |
| `isPathfindable(state, getter, pos, type)` | `isPathfindable(state, type)` |
| `data/<ns>/structures/` | `data/<ns>/structure/` *(singulier)* |
| capabilities `getCapability(...)` | `level.getCapability(Capabilities.X.BLOCK, pos, ctx)` |
| `ForgeCatnipServices.FLUID_RENDERER` | `CatnipServices.FLUID_RENDERER` — typé `<?>`, **cast requis** |

### Deux pièges qui ne cassent pas la compilation

Ce sont les plus coûteux : le code compile, ne lève rien, et se comporte mal.

1. **`stack.get(DataComponents.CUSTOM_DATA).copyTag()` renvoie une copie.** Traduire
   `stack.getOrCreateTag().putX(...)` littéralement produit une écriture **silencieusement
   perdue**. Passer par un composant typé et `stack.set(...)`.

2. **Un codec persistant qui peut refuser une valeur fait planter la sauvegarde du monde**,
   pas l'écriture — donc très loin de la cause. `CNDataComponents.HEAT` était déclaré
   `ExtraCodecs.POSITIVE_FLOAT`, refusant `0.0`, la valeur normale d'un réacteur à l'arrêt.
   **Valider les invariants en amont, pas dans le codec.**

   > **Ce piège a resservi le 7 août** (`a8d1f1b`), sur `PatternData` cette fois :
   > `ItemStack.CODEC` **refuse la stack vide**, et un motif de réacteur fait 57 slots qui ne
   > sont presque jamais tous remplis. Résultat : dès qu'un blueprint se trouvait dans un
   > contrôleur, `ReactorControllerBlockEntity.write` levait à la sauvegarde du chunk et
   > Minecraft désactivait silencieusement la persistance du block entity
   > (« It will not persist ») — le contrôleur perdait son blueprint à chaque rechargement.
   > Même défaut sur le chemin réseau avec `ItemStack.STREAM_CODEC`.
   > **Réflexe 1.21 : pour un `ItemStack` qui peut être vide, c'est
   > `ItemStack.OPTIONAL_CODEC` / `OPTIONAL_STREAM_CODEC`, jamais les variantes strictes.**
   >
   > Ce bug ne s'était pas vu aux gametests parce qu'ils **ne rechargent pas le monde** :
   > l'erreur ne sortait que dans le log de `runGameTestServer`, à l'arrêt du serveur, pendant
   > que « All 29 required tests passed » s'affichait juste au-dessus. **Lire le log, pas
   > seulement le verdict.**

---

## 7. Vérifier l'état à tout moment

```bash
# Fichiers Forge sans équivalent NeoForge (attention aux 8 renommés de §1)
cd ~/Documents/Ynov/Ydays
diff <(cd CreateNuclearForge/src && find . -name '*.java' | sort) \
     <(cd CreateNuclearNeoForge/src && find . -name '*.java' | sort)

# Écart interne d'un fichier partagé
diff CreateNuclearForge/src/main/java/.../X.java \
     CreateNuclearNeoForge/src/main/java/.../X.java

# Non-régression du réacteur — doit sortir en 0 des deux côtés
cd CreateNuclearNeoForge && ./gradlew runGameTestServer   # 31 tests
cd CreateNuclearForge   && ./gradlew runGameTestServer    # 28 tests
```
