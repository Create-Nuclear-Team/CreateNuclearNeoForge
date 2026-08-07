# Parité Forge → NeoForge — ce qu'il reste à porter

Ce document liste tout ce qui sépare encore `CreateNuclearNeoForge` de `CreateNuclearForge`
en termes de **features**, et comment porter chaque bloc.

**Ce fichier ne contient que du travail restant.** Ce qui est fait en sort — l'historique
est dans les commits.

État au **7 août 2026**, branche `V2-Reacteur`.
Pour le domaine réacteur, déjà porté et validé, voir [`PORTAGE_REACTEUR.md`](PORTAGE_REACTEUR.md).

| | Forge | NeoForge |
|---|---|---|
| Fichiers `.java` | 296 | 294 |
| Ressources | 1 132 | 1 115 |

---

## 0. Comment lire ce document

Un `diff` brut des arborescences donne **21 fichiers Forge sans équivalent NeoForge**.
Ce chiffre est trompeur :

- **4 sont remplacés par un équivalent 1.21 meilleur** → §1, à ne surtout pas « re-porter » ;
- **17 manquent réellement** → §2, c'est le vrai travail.

S'y ajoutent des écarts *à l'intérieur* de fichiers présents des deux côtés (§3), invisibles
d'un diff d'arborescence — et c'est là qu'ont été trouvés la moitié des bugs du réacteur.

> **Le piège récurrent, vérifié deux fois.** Un fichier absent n'est pas le seul symptôme d'une
> feature manquante. À deux reprises, la classe qui « manquait » n'était qu'un **point
> d'enregistrement** : le reste de la feature existait déjà côté NeoForge, complet et correct,
> mais **n'était référencé nulle part** — donc entièrement mort en jeu.
> **Avant de porter quoi que ce soit, chercher les classes NeoForge jamais référencées.**

---

## 1. Volontairement non portés — l'équivalent 1.21 est meilleur

**Ne pas porter ces fichiers.** Les recréer serait une régression.

| Fichier Forge | Remplacé côté NeoForge par |
|---|---|
| `content/radiation/capability/IRadiationCapability` | **Data attachments NeoForge** — `RadiationCapability` expose `AttachmentType<RadiationCapability> RADIATION`, lu via `entity.getData(RADIATION)` |
| `content/radiation/capability/RadiationProvider` | idem — la plomberie `ICapabilityProvider` de Forge n'a pas d'équivalent, ni de raison d'exister |
| `foundation/advancement/CriterionTriggerBase` | **`SimpleCriterionTrigger` vanilla** — `SimpleCreateNuclearTrigger` en hérite directement |
| `foundation/utility/SimplexNoise` | **`net.minecraft.world.level.levelgen.synth.SimplexNoise`** — `Maths` utilise déjà celui de vanilla |

---

## 2. Les 17 fichiers réellement manquants

Classés par feature, du plus au moins impactant.

---

### 2.1 Poudre de neige (Snow Powder) — 4 fichiers

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

### 2.2 Vache irradiée + abstraction des animaux — 5 fichiers

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

### 2.3 Worldgen piloté par la config — 2 fichiers

`infrastructure/worldgen/CNPlacementModifiers` · `infrastructure/worldgen/ConfigPlacementFilter`

**Ce qui manque en jeu :** un filtre de placement qui lit la config avant de générer un minerai.
Sans lui, **les options de `CWorldGen` ne sont probablement pas respectées** : désactiver un
minerai dans la config n'a aucun effet.

**Points d'accroche :**
- `CreateNuclear.java` → `CNPlacementModifiers.register(modEventBus)`
- `CNPlacedFeatures` (lignes 51 et 60 côté Forge) → `ConfigPlacementFilter.INSTANCE` dans la
  liste des modificateurs de placement

**Difficulté : faible à moyenne.** Le `PlacementModifierType` se déclare de la même façon en
1.21, mais l'enregistrement passe par un `DeferredRegister` NeoForge.

⚠️ **À vérifier avant de porter** : NeoForge n'a peut-être aucun filtre, ou en a un autre. Un
`runData` sera nécessaire, et les features placées changeront — donc **monde de test neuf**.

---

### 2.4 Datagen de recettes — 2 fichiers

`foundation/data/recipe/CNDeployingRecipeGen` · `foundation/data/recipe/CNProcessingRecipeGen`

**Ce qui manque en jeu :** des recettes qui existent côté Forge et pas côté NeoForge —
déployeur, et une base commune de recettes de traitement.

**Difficulté : faible**, mais **impact direct sur la progression du joueur** : des objets
peuvent être tout simplement non craftables. À vérifier en jeu, JEI en main, en comparant les
deux versions côte à côte.

---

### 2.5 Compat Alex's Caves — 2 fichiers

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

### 2.6 Mixins client — 2 fichiers

`foundation/mixin/client/CameraAccessor` · `foundation/mixin/client/GameRendererMixin`

**Ce qui manque en jeu :** un effet visuel, très probablement lié au rendu de la radiation ou
de l'explosion (accès à la caméra + hook sur le `GameRenderer`).

NeoForge a à la place `AntiRadiationArmorTextureMixin`, qui n'existe pas côté Forge : les deux
versions ont donc **divergé sur les mixins client**. Comparer les deux `createnuclear.mixins.json`
avant de porter.

**Difficulté : moyenne.** Les mixins sont sensibles aux versions et cassent silencieusement.
À porter avec un test visuel explicite.

---

## 3. Écarts internes aux fichiers partagés — non audités

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

## 4. Vérifications en jeu en attente

Ce n'est pas du portage, mais c'est du travail restant. **Aucun de ces points n'est couvert par
les gametests.**

| À vérifier | Attendu | Pourquoi c'est en attente |
|---|---|---|
| Comportement de la radiation | seuils, décroissance, persistance à la mort identiques à Forge | `RadiationCapability` diverge de 186 lignes. Le mécanisme (data attachments) est correct, mais rien ne dit que le comportement l'est |
| Tooltip d'une barre d'uranium/graphite | cinq lignes de stats, type en vert (FUEL) ou cyan | livré sans test en jeu |
| `raw_uranium_block` en inventaire | la radiation monte de 27 par item | livré sans test en jeu |
| Tuyau ouvert Create crachant de l'uranium | irradie les entités de la zone | livré sans test en jeu ; la feature était morte avant, donc jamais observée |

---

## 5. Ordre de travail suggéré

| # | Chantier | Fichiers | Difficulté | Pourquoi ce rang |
|---|---|---|---|---|
| 1 | **Datagen recettes (§2.4)** | 2 | faible | Peut bloquer la progression du joueur |
| 2 | **Worldgen config (§2.3)** | 2 | faible/moy. | Silencieux mais fausse la génération |
| 3 | **Compat Alex's Caves (§2.5)** | 2 | faible | Isolé, sans risque |
| 4 | **Snow Powder (§2.1)** | 4 | moyenne | Feature complète, API recettes à traduire |
| 5 | **Abstraction animaux + vache (§2.2)** | 5 | moy./élevée | Refactor de 3 entités qui marchent |
| 6 | **Mixins client (§2.6)** | 2 | moyenne | Sensible, à faire avec test visuel |
| 7 | **Audit `CNBlocks` / `CNItems` (§3)** | — | continu | À faire au fil des symptômes, pas d'un bloc |

---

## 6. Méthode de portage — ce qui a marché sur le réacteur

À reprendre pour chaque chantier ci-dessus.

1. **Chercher d'abord les classes NeoForge jamais référencées.** Voir l'encadré du §0 : deux
   features sur deux se sont révélées présentes mais mortes, faute d'un point d'enregistrement.
   Un `grep -rl <NomDeClasse> src` qui ne renvoie que le fichier lui-même est un signal fort.
2. **Porter à l'identique par défaut.** Copier le fichier Forge, ne changer que ce que l'API
   1.21 impose. Sur le réacteur, ~35 fichiers sur 50 sont restés **identiques à l'octet**.
3. **Vérifier chaque fichier par un `diff`** contre son original Forge, et **justifier chaque
   ligne qui diffère**. C'est ce qui a permis de garder les deux versions synchronisables.
4. **Compiler après chaque lot**, pas à la fin.
5. **Un commit par lot**, poussé avant d'enchaîner.
6. **Documenter toute divergence assumée** dans le commit, pour qu'une future synchro ne la
   « corrige » pas par erreur.
7. **Tester en jeu.** Tout le portage du réacteur compilait et passait les gametests avant que
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
| `@Mod.EventBusSubscriber(bus = Bus.FORGE)` | `@EventBusSubscriber` — le bus GAME est le défaut, et l'attribut `bus` est déprécié |
| `data/<ns>/structures/` | `data/<ns>/structure/` *(singulier)* |
| capabilities `getCapability(...)` | `level.getCapability(Capabilities.X.BLOCK, pos, ctx)` |
| `ForgeCatnipServices.FLUID_RENDERER` | `CatnipServices.FLUID_RENDERER` — typé `<?>`, **cast requis** |

### Deux pièges qui ne cassent pas la compilation

Ce sont les plus coûteux : le code compile, ne lève rien, et se comporte mal.

1. **`stack.get(DataComponents.CUSTOM_DATA).copyTag()` renvoie une copie.** Traduire
   `stack.getOrCreateTag().putX(...)` littéralement produit une écriture **silencieusement
   perdue**. Passer par un composant typé et `stack.set(...)`.

2. **Un codec persistant qui peut refuser une valeur fait planter la sauvegarde du monde**,
   pas l'écriture — donc très loin de la cause. **Valider les invariants en amont, pas dans le
   codec.** Rencontré deux fois :
   - `ExtraCodecs.POSITIVE_FLOAT` sur la chaleur, qui refusait `0.0`, valeur normale d'un
     réacteur à l'arrêt ;
   - `ItemStack.CODEC` sur un motif de réacteur, qui **refuse la stack vide** alors que le motif
     fait 57 slots presque jamais tous remplis. **Réflexe 1.21 : pour un `ItemStack` qui peut
     être vide, c'est `ItemStack.OPTIONAL_CODEC` / `OPTIONAL_STREAM_CODEC`, jamais les variantes
     strictes.**

   > Ce second cas ne s'était pas vu aux gametests parce qu'ils **ne rechargent pas le monde** :
   > l'erreur ne sortait que dans le log de `runGameTestServer`, à l'arrêt du serveur, pendant
   > que « All tests passed » s'affichait juste au-dessus. **Lire le log, pas seulement le
   > verdict.**

### Noms qui masquent une classe vanilla

Trois classes du mod portent volontairement le nom d'une classe vanilla, pour rester alignées
sur Forge : `ArmorMaterials` (`net.minecraft.world.item.ArmorMaterials`), `CatLieOnBedGoal` et
`CatSitOnBlockGoal` (`net.minecraft.world.entity.ai.goal.*`). Les fichiers concernés importent
le package vanilla en wildcard — **sans danger**, parce qu'en Java une classe du même package
l'emporte sur un import à la demande, et qu'aucun de ces fichiers n'utilise la classe vanilla
homonyme. **Mais si l'un d'eux a un jour besoin de la version vanilla, il faudra la qualifier
complètement** — un `import` simple ne suffira pas.

---

## 7. Vérifier l'état à tout moment

```bash
# Fichiers Forge sans équivalent NeoForge — doit en lister 21
cd ~/Documents/Ynov/Ydays
diff <(cd CreateNuclearForge/src && find . -name '*.java' | sort) \
     <(cd CreateNuclearNeoForge/src && find . -name '*.java' | sort)

# Écart interne d'un fichier partagé
diff CreateNuclearForge/src/main/java/.../X.java \
     CreateNuclearNeoForge/src/main/java/.../X.java

# Classes NeoForge jamais référencées (le piège du §0)
cd CreateNuclearNeoForge && grep -rl '<NomDeClasse>' src --include=*.java

# Non-régression du réacteur — doit sortir en 0 des deux côtés
cd CreateNuclearNeoForge && ./gradlew runGameTestServer   # 31 tests
cd CreateNuclearForge   && ./gradlew runGameTestServer    # 28 tests
```
