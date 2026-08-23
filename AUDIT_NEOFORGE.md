# Audit de code — Migration Forge → NeoForge (CreateNuclearNeoForge)

Document de suivi vivant. **Ne contient que les points encore ouverts.**
Dernière re-vérification intégrale contre le code : **23/08/2026** (branche `V2`).
Tout point corrigé depuis l'audit initial a été retiré du fichier — l'historique complet reste disponible dans `git log`.

Périmètre : `src/main/java` (292 fichiers), à l'exclusion des ressources/datagen JSON.
Rappel du cadre : le mod est **en cours de migration** de Forge vers NeoForge 1.21.1. Les usages de patterns Forge qui fonctionnent correctement et ne sont pas explicitement temporaires ne sont **pas** listés comme problème. Les nouvelles fonctionnalités propres à NeoForge 1.21.1 non encore adoptées ne sont **pas** considérées comme un manque.

Légende priorité : 🔴 Critique · 🟠 Important · 🟡 Moyen · 🟢 Faible

---

## Sommaire

1. [Bugs de logique](#0-bugs-de-logique)
2. [Dead Code](#1-dead-code)
3. [Commentaires et Javadocs](#2-commentaires-et-javadocs)
4. [Duplications](#3-duplications)
5. [Migration Forge → NeoForge](#4-migration-forge--neoforge)
6. [Nettoyage](#5-nettoyage)
7. [Refactorisations](#6-refactorisations)
8. [Tableau de priorités global](#7-tableau-de-priorités-global)

---

## 0. Bugs de logique

| # | Fichier:ligne | Problème | Priorité |
|---|---|---|---|
| B2 | `content/radiation/capability/RadiationCapability.java:210-212` | Dans `applyEffects`, la branche `< radiationLevel3` et la branche `else` renvoient toutes deux `amplifierLevel2.get()`. Conséquence : le seuil de config `radiation_level_3` n'a **aucun effet** (les deux dernières tranches sont identiques) et l'amplificateur plafonne au niveau 2. À trancher : soit ajouter un `amplifierLevel3` dans `CRadiation.java` (qui n'expose aujourd'hui que `amplifierLevel0/1/2`), soit supprimer `radiationLevel3` devenu inutile. | 🟠 |

---

## 1. Dead Code

### 1.2 Méthodes inutilisées

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/kinetics/fan/processing/CNFanProcessingTypes.java:55-65` | `ofLegacyName(String)` / `parseLegacy(String)` : **aucun appelant** hors de `parseLegacy` qui appelle `ofLegacyName`. Le shim de noms legacy (cf. §4) est donc entièrement mort. | 🟡 |
| `content/multiblock/alarm/ReactorAlarmEntity.java:98-100` | `setController(...)` jamais appelée (les seuls `setController` appelés en jeu sont ceux de `ReactorCasingEntity`/`ReactorFrameEntity`, qui prennent un `BlockPos`). | 🟢 |
| `foundation/utility/CreateNuclearLang.java:68-70` | `temporaryText(String)`, `@Deprecated`, aucun appelant. | 🟢 |
| `foundation/utility/TextUtils.java:45-75,77,116-119` | `renderMultilineDebugText`, `renderDebugText`, `translateWithFormatting`, `leftPad` : aucun appelant dans le code (`renderDebugText` n'est appelée que par `renderMultilineDebugText`, elle-même morte). | 🟢 |

### 1.3 Champs inutilisés

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `foundation/utility/RenderHelper.java:13-15,36-38` | `lastAlpha`, `lastCoverage`, `lastFirstPerson` : champs de « cache » assignés à chaque appel mais **jamais relus** (la comparaison qui les justifiait a été retirée, il ne reste que le commentaire `// Skip rendering if parameters unchanged` l.34). | 🟡 |
| `net/nuclearteam/createnuclear/CNRecipeTypes.java:45,61,71,76` | Champ `isProcessingRecipe` assigné aux 3 endroits mais jamais lu. | 🟡 |
| `content/effects/VicinityEffect.java:22` | Paramètre constructeur `Consumer<Integer> timer` jamais stocké ni utilisé (tous les appelants passent `timer -> {}`, ex. `RadiationEffect.java:24`). | 🟡 |
| `content/multiblock/IHeat.java:28,37-41` | Champ `intColor` et son constructeur `HeatLevel(int, int)` jamais utilisés (les 5 valeurs de l'enum passent toutes par le constructeur `ChatFormatting`), et aucun getter ne l'expose. | 🟢 |
| `content/multiblock/alarm/ReactorAlarmEntity.java:21` | Champ public `controller` jamais lu (hors `setController` lui-même mort). | 🟢 |
| `foundation/advancement/CNAdvancement.java:54` | `public static final CreateNuclearAdvancement START = null,` — première entrée nulle de la déclaration groupée, jamais référencée. | 🟢 |
| `content/contraptions/irradiated/cat/IrradiatedCat.java:525-526` | Dans `CatAvoidEntityGoal` : `Predicate<Entity> var10006 = EntitySelector.NO_CREATIVE_OR_SPECTATOR; Objects.requireNonNull(var10006);` — variable locale décompilée, jamais utilisée ensuite (le prédicat n'est transmis à aucun `super(...)`). | 🟢 |

### 1.5 Imports inutiles

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/uraniumOre/UraniumOreBlock.java:18-19` | `EnchantmentHelper`, `Enchantments` ne servent qu'au bloc XP commenté (l.93-96). | 🟢 |

### 1.6 Code commenté pouvant être supprimé

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/input/item/ReactorRodInputEntity.java:86-93` | Bloc commenté utilisant explicitement l'ancienne API de capacités Forge (`Capability<?>`, `ForgeCapabilities.ITEM_HANDLER`, `ResetableLazy<T>`) — reliquat direct jamais retiré après le passage aux capacités NeoForge. | 🟠 |
| `compat/jei/CreateNuclearJEI.java:145-148` | Bloc commenté pour générer des fluides de potion par `BottleType`, dupliqué juste après par le code actif qui ne gère que `REGULAR`. | 🟡 |
| `net/nuclearteam/createnuclear/CNFluids.java:114` | Ligne d'enregistrement `//.onRegister(ReactorFluidTypesValue.setReactorFluidTypeInfos(8196, 100))` commentée sur `LIQUID_NITROGEN`. | 🟢 |
| `content/uraniumOre/UraniumOreBlock.java:93-96` | Bloc XP entièrement commenté dans `spawnAfterBreak`. | 🟢 |
| `foundation/utility/RenderHelper.java:52` | `//graphics.pose().scale(coverage, coverage, 1f);` commenté : la branche `coverage != 1f` (l.48-57) devient strictement équivalente à la branche `coverage == 1f` (l.59-61), au `pushPose`/`translate` inutile près. | 🟢 |

---

## 2. Commentaires et Javadocs

### 2.1 Commentaires/Javadocs en français

Le style du projet est très majoritairement en anglais.

| Fichier:ligne | Extrait | Priorité |
|---|---|---|
| `content/multiblock/input/fluid/ReactorFluidInputEntity.java:55,91,101` | Javadoc « Capacité du tank en fonction de la taille du réacteur (tier) », plus deux notes d'incertitude technique liées à la migration (« Pensez à passer registries si requis par la v1.20+... », « Pareil ici selon l'implémentation de SmartFluidTank ») — cf. §4. | 🟠 |
| `content/multiblock/controller/manager/ReactorAlarmManagerI.java:9-10` | Javadoc en français **et mal formée** (`/** * Retourne une copie immuable...`, astérisque en trop sur la première ligne). | 🟡 |
| `content/multiblock/input/fluid/ReactorFluidInput.java:91` | « Convertit le vieux InteractionResult en ItemInteractionResult si nécessaire pour NeoForge » — cf. §4. | 🟡 |
| `content/multiblock/controller/manager/ReactorInputManager.java:138,143,146,154,158` | « On récupère le nom de l'item », « On tente d'extraire 1 unité », « Si l'extraction a réussi... », « On n'a pas trouvé l'item demandé », Javadoc « Helper pour comparer "GraphiteRod" avec "graphite_rod" ». | 🟢 |
| `content/redstone/displayLink/source/ReactorSummaryDisplaySource.java:157-159` | Commentaire de 3 lignes « Divergence assumee vs Forge, qui lit... ». | 🟢 |
| `content/redstone/displayLink/source/HeatDisplaySource.java:28-29` | « En 1.21 la chaleur vit dans le data component... relire le tag NBT de la stack renvoie une copie defensive ». | 🟢 |
| `net/nuclearteam/createnuclear/CNDisplaySources.java:20-24` | Javadoc « Divergence assumee vs Forge : en 1.21 Registrate type ses entrees... ». | 🟢 |
| `content/multiblock/controller/manager/ReactorAlarmManager.java:47` | « On ne supprime pas si le chunk est juste déchargé » (dernier commentaire français restant du fichier). | 🟢 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:90` | « les pos sont [xMin, xMax, yMin, yMax, zMin, zMax] ». | 🟢 |
| `content/multiblock/bluePrintItem/ReactorBluePrintItemScreen.java:45` | `//ici pour le titre`. | 🟢 |
| `foundation/ponder/CNPonderIndex.java:16` | « Reactor - Storyboards pour chaque taille ». | 🟢 |
| `net/nuclearteam/createnuclear/CNSoundEvents.java:40,45,50,85,90` | Chemins de ressources en français : `create("reacteur/activation")`, `"reacteur/running"`, `"reacteur/shut_off"`, `"reacteur/assemble_deassemble/..."`. Impacte l'arborescence des assets, donc plus coûteux à renommer. | 🟢 |

### 2.2 Commentaires peu explicites ou ambigus

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `compat/jei/CreateNuclearJEI.java:141-143` | Commentaire attribué nominativement à un contributeur (« `@goshante:` ») au-dessus du bloc mort l.145-148. | 🟢 |

### 2.3 Javadocs incomplètes ou non standard

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/controller/manager/ReactorInputFluidManager.java:30,45,61,86,100,119` | Javadoc placée **après** `@Override` au lieu d'avant, sur 6 méthodes — non reconnue par l'outillage Javadoc standard. C'est le **seul fichier du projet** encore concerné. | 🟡 |
| `content/multiblock/MultiblockHelpers.java:42-45` | Javadoc placée **à l'intérieur** du corps de `getControllerForPart` (l.41) au lieu d'être au-dessus de la signature. | 🟢 |
| `content/multiblock/rod/CNRodTypes.java:12-33` | Javadoc utile mais mal placée : elle documente la classe et `RodType.Builder` en général, alors qu'elle est apposée sur la méthode `bootstrap()`. | 🟢 |
| `content/radiation/RadiationEffect.java:26,34,42` | Commentaires inline répétant littéralement le code (`// Reduces movement speed by 20%` juste au-dessus de la ligne qui applique `-0.2D`). | 🟢 |

### 2.4 Commentaires devenus obsolètes

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `net/nuclearteam/createnuclear/CNTags.java:32-46,52` | Méthodes `forgeTag`/`forgeBlockTag`/`forgeItemTag`/`forgeFluidTag` : nommage hérité de l'ère Forge alors qu'elles pointent en réalité vers `NEO_FORGE`, lui-même aliasé sur le namespace commun `"c"`. `FORGE("forge")` (l.52) est **confirmée inutilisée** : plus aucune référence dans le projet. | 🟠 |

---

## 3. Duplications

| Fichier(s) | Détail | Priorité |
|---|---|---|
| `.../manager/ReactorInputManager.java:187`, `ReactorOutputManager.java:69`, `ReactorInputFluidManager.java:90`, `ReactorAlarmManager.java:58` | `getBlocksPosition(Level)` réimplémente **4 fois** le même filtre `instanceof XxxEntity`. Trois d'entre elles déclarent en plus une variable locale `positions` qui **masque le champ protégé homonyme** d'`AbstractReactorIOManager`. | 🟡 |
| `foundation/block/HorizontalDirectionalReactorBlock.java` vs `MultiDirectionalReactorBlock.java` | Structure `rotate`/`mirror` identique (≈30 lignes chacune), seule la propriété (`HORIZONTAL_FACING` vs `FACING`) diffère. | 🟡 |
| `net/nuclearteam/createnuclear/CNItems.java` | Motif de recette « `_from_decompacting` » répété **9 fois** de façon quasi identique. | 🟡 |
| `content/radiation/capability/RadiationCapability.java:137-151` | `computeItemRadiation(Player)` réimplémente inline la logique déjà factorisée dans `getStackRadiation(ItemStack, LivingEntity)` (l.153-158), que la surcharge `computeItemRadiation(LivingEntity)` utilise pourtant correctement. | 🟡 |
| `.../manager/ReactorInputManager.java:31-45`, `ReactorInputFluidManager.java:48-62`, `ReactorAlarmManager.java:17-39` | Sérialisation NBT (`read`/`write`, triplet `x`/`y`/`z`) identique répétée dans 3 managers. `ReactorOutputManager` utilise un 4e format (`BlockPos.asLong` sous la clé `"p"`) : les formats ne sont même pas homogènes entre managers. | 🟢 |
| `content/multiblock/input/fluid/FluidLockManager.java` vs `PersistentFluidLocks.java` | Logique de verrouillage de fluide dupliquée entre version mémoire et version persistante. `ReactorFluidInputEntity` appelle **les deux côte à côte** (l.174-221 : `PersistentFluidLocks.get(...).tryLock(...)` immédiatement suivi de `FluidLockManager.tryLock(...)`), ce qui maintient deux sources de vérité en parallèle. | 🟢 |
| `api/multiblock/rods/RodType.java` (Builder, l.293-306) vs `api/multiblock/fluid/ReactorFluidType.java` (Builder, l.168-177) | Même squelette de validation (liste `missing` + `IllegalStateException` nommant les champs manquants), avec deux mécanismes de détection différents (`== null` côté rods, drapeaux `xxxSet` côté fluides). | 🟢 |
| `content/contraptions/irradiated/chicken/IrradiatedChicken.java:139-141` vs `wolf/IrradiatedWolf.java:350-352` | `isFood(ItemStack)` strictement identique (`stack.is(CNTags.CNItemTags.FUEL.tag)`) alors qu'`AnimalUtil.isFood(...)` existe déjà et est utilisé par `IrradiatedCow`. | 🟢 |
| `content/contraptions/irradiated/wolf/IrradiatedWolfRenderer.java:16-17` | `WOLF_LOCATION` et `WOLF_TAME_LOCATION` pointent vers exactement la même texture (`textures/entity/irradiated_wolf.png`) — le branchement l.43-45 est donc sans effet visuel. | 🟢 |

---

## 4. Migration Forge → NeoForge

Rappel : uniquement les éléments clairement transitoires/résiduels de la migration technique. Le fonctionnement correct actuel n'est pas remis en cause en soi.

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `net/nuclearteam/createnuclear/CreateNuclear.java:70,106` | `IEventBus forgeEventBus = NeoForge.EVENT_BUS;` — variable nommée d'après l'ancienne API alors qu'elle référence le bus NeoForge ; utilisée telle quelle l.106. | 🟠 |
| `net/nuclearteam/createnuclear/CreateNuclear.java:110` | `//DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` — `DistExecutor` est une API Forge, remplacée en NeoForge par `@Mod(dist = Dist.CLIENT)` (déjà utilisée correctement dans `CreateNuclearClient.java`). Ligne morte à supprimer. | 🟠 |
| `net/nuclearteam/createnuclear/CNTags.java:32-52` | `forgeTag`/`forgeBlockTag`/`forgeItemTag`/`forgeFluidTag` et enum `FORGE("forge")` : nommage hérité de Forge pour un mécanisme qui pointe désormais vers le namespace commun `"c"`. Fonctionne correctement mais induit en erreur (utilisé des dizaines de fois dans `CNBlocks.java`, `CNItems.java`, `CreateNuclearRegistrateTags.java`). | 🟠 |
| `content/multiblock/input/item/ReactorRodInputEntity.java:86-93` | Bloc commenté sur l'ancienne API de capacités Forge — reliquat direct jamais retiré après le passage aux capacités NeoForge (`Capabilities.ItemHandler.BLOCK` utilisé ailleurs dans le même package). | 🟠 |
| `content/multiblock/input/fluid/ReactorFluidInputEntity.java:91,101` | Commentaires d'incertitude explicites sur la bonne API post-migration (« Pensez à passer registries si requis par la v1.20+... », « Pareil ici selon l'implémentation de SmartFluidTank ») — notes-à-soi-même jamais tranchées. | 🟠 |
| `content/multiblock/input/fluid/ReactorFluidInput.java:91` | « Convertit le vieux InteractionResult en ItemInteractionResult si nécessaire pour NeoForge » — le « si nécessaire » signale une incertitude non tranchée. | 🟡 |
| `net/nuclearteam/createnuclear/CreateNuclearClient.java:20` | `IEventBus neoEventBus = NeoForge.EVENT_BUS;` déclarée mais jamais utilisée — vestige d'un ancien câblage d'événements client. | 🟡 |
| `content/kinetics/fan/processing/CNFanProcessingTypes.java:36-65` | `LEGACY_NAME_MAP`/`ofLegacyName`/`parseLegacy` : shim de compatibilité de noms lié à d'anciennes sauvegardes/NBT pré-migration. **Vérification faite : aucun appelant** — soit le câbler là où les NBT legacy sont lus, soit le supprimer. | 🟡 |
| `content/contraptions/irradiated/cat/IrradiatedCatRenderer.java:5`, `wolf/IrradiatedWolf.java:3`, `foundation/block/HorizontalDirectionalReactorBlock.java:3`, `MultiDirectionalReactorBlock.java:3` | Import `com.mojang.math.MethodsReturnNonnullByDefault` au lieu de `net.minecraft.MethodsReturnNonnullByDefault` (utilisé partout ailleurs) — incohérence probablement issue d'un auto-import IDE pendant le portage. | 🟡 |
| `foundation/data/recipe/CNCrushingRecipeGen.java:42-56` | Différences de contenu de recette apparues pendant le portage, à trancher (voulu ou régression) : (1) nouvelle recette `RAW_URANIUM_BLOCK` absente côté Forge ; (2) `RAW_THORIUM_BLOCK` : la sortie secondaire `0.75f ×AllItems.EXP_NUGGET` (Forge) a été remplacée par `0.5f ×CNItems.THORIUM_DUST×72` ; (3) `RAW_THORIUM_ITEM` : même changement, `0.75f×EXP_NUGGET` → `0.5f×THORIUM_DUST×8`. Les recettes de fer/or (l.61,68) ont bien gardé leur `EXP_NUGGET`, ce qui rend l'écart d'autant plus visible. | 🟡 |

---

## 5. Nettoyage

Éléments à retirer une fois la migration complètement terminée et les points de la section 4 tranchés.

- Retirer `content/multiblock/input/item/ReactorRodInputEntity.java:86-93` (bloc de capacités Forge commenté).
- Renommer `forgeEventBus` → `neoForgeEventBus` dans `CreateNuclear.java:70`, retirer la ligne `DistExecutor` commentée (l.110), retirer `neoEventBus` inutilisé dans `CreateNuclearClient.java:20`.
- Une fois le nommage `CNTags.forgeXxxTag` validé comme voulu ou non, renommer en `commonXxxTag` ; supprimer dans tous les cas l'entrée `FORGE("forge")` (l.52), confirmée sans référence.
- Supprimer ou câbler `CNFanProcessingTypes.LEGACY_NAME_MAP`/`ofLegacyName`/`parseLegacy`.
- Retirer `CreateNuclearLang.temporaryText`, `TextUtils.renderMultilineDebugText`/`renderDebugText`/`translateWithFormatting`/`leftPad` (aucun appelant).
- Retirer les champs de « cache » inertes de `RenderHelper.java:13-15` (et le commentaire l.34 devenu faux), ainsi que la branche `coverage != 1f` (l.48-57) rendue équivalente par le `scale` commenté.
- Retirer `ReactorAlarmEntity.controller` (l.21) et `setController` (l.98-100), tous deux morts.
- Retirer `IHeat.intColor` et son constructeur `HeatLevel(int, int)`, `CNRecipeTypes.isProcessingRecipe`, `CNAdvancement.START`, le paramètre `Consumer<Integer> timer` de `VicinityEffect`.
- Retirer la variable locale décompilée `var10006` dans `IrradiatedCat.CatAvoidEntityGoal` (l.525-526).
- Nettoyer les imports/blocs commentés listés en §1.5/§1.6 (`UraniumOreBlock`, `CreateNuclearJEI`, `CNFluids`).

---

## 6. Refactorisations

Uniquement des refactors pertinents **après** la fin de la migration — pas liés à la dette de migration elle-même, et n'impliquant pas l'adoption de nouvelles fonctionnalités NeoForge 1.21.1.

- **`AbstractReactorIOManager`** : ajouter une méthode générique `filterByType(Level, Class<T>)` pour factoriser les quatre implémentations quasi identiques de `getBlocksPosition(Level)` (Input / Output / InputFluid / Alarm), et supprimer au passage les variables locales `positions` qui masquent le champ protégé.
- **Sérialisation des managers** : remonter le `read`/`write` de positions dans `AbstractReactorIOManager` et unifier le format (aujourd'hui triplet `x`/`y`/`z` dans 3 managers, `BlockPos.asLong` dans `ReactorOutputManager`).
- **`CNItems.java`** : factoriser le motif de recette « `_from_decompacting` » répété 9 fois en une méthode utilitaire.
- **`HorizontalDirectionalReactorBlock`/`MultiDirectionalReactorBlock`** : fusionner en une classe abstraite générique paramétrée par le `DirectionProperty`.
- **`RodType.Builder`/`ReactorFluidType.Builder`** : factoriser le squelette de validation partagé et homogénéiser la détection des champs manquants (`== null` vs drapeaux `xxxSet`).
- **`RadiationCapability.computeItemRadiation(Player)`** : faire appel à `getStackRadiation` comme le fait déjà la surcharge `LivingEntity`.
- **Verrous de fluide** : ne garder qu'un seul chemin entre `FluidLockManager` (mémoire) et `PersistentFluidLocks` (persistant), aujourd'hui appelés en parallèle dans `ReactorFluidInputEntity`.
- **`CNRecipeProvider`** : unifier les deux mécanismes d'enregistrement de générateurs de recettes qui coexistent (liste interne `GENERATORS` dans `CNRecipeProvider.java:18-37` vs `addProvider` directs dans `CreateNuclearDatagen.java:46-52`).
- **Animaux irradiés** : faire passer `IrradiatedChicken.isFood`/`IrradiatedWolf.isFood` par `AnimalUtil.isFood`, comme `IrradiatedCow`.
- **`CNArmorMaterials.durabilityForType`** : extraire le tableau `BASE_DURABILITY` (l.72) recréé à chaque appel en constante statique.

---

## 7. Tableau de priorités global

### 🔴 Critique

*Aucun point critique restant.*

### 🟠 Important

- Bug de logique : `RadiationCapability.applyEffects` (B2) — `radiation_level_3` sans effet, amplificateur plafonné au niveau 2.
- Débris de migration à finaliser : `CreateNuclear.forgeEventBus` + ligne `DistExecutor` commentée, `CNTags.forgeXxxTag`/`FORGE`, capacités Forge commentées dans `ReactorRodInputEntity`, notes d'incertitude non tranchées dans `ReactorFluidInputEntity`.
- Commentaires français masquant une incertitude technique : `ReactorFluidInputEntity`.

### 🟡 Moyen

- Duplications : `getBlocksPosition(Level)` ×4 managers (+ shadowing), `HorizontalDirectionalReactorBlock`/`MultiDirectionalReactorBlock`, `CNItems` decompacting ×9, `RadiationCapability.computeItemRadiation(Player)`.
- Dead code : shim legacy `CNFanProcessingTypes` sans appelant, champs de cache inertes de `RenderHelper`, `CNRecipeTypes.isProcessingRecipe`, `VicinityEffect.timer`.
- Javadoc mal placée après `@Override` dans `ReactorInputFluidManager` (6 méthodes) ; Javadoc française mal formée `ReactorAlarmManagerI`.
- Blocs commentés secondaires : `CreateNuclearJEI` (`BottleType`).
- Imports `com.mojang.math.MethodsReturnNonnullByDefault` (4 fichiers) ; `CreateNuclearClient.neoEventBus` ; incertitude `ReactorFluidInput.java:91`.
- Divergences de recettes `CNCrushingRecipeGen` à trancher.

### 🟢 Faible

- Champs/méthodes isolés sans impact (`IHeat.intColor`, `ReactorAlarmEntity.controller`/`setController`, `CNAdvancement.START`, méthodes mortes de `TextUtils`/`CreateNuclearLang`, `var10006` dans `IrradiatedCat`).
- Imports et blocs commentés isolés (`UraniumOreBlock`, `CNFluids`, `RenderHelper`).
- Commentaires français restants sans impact joueur (`ReactorInputManager`, `ReactorAlarmManager:47`, display sources, `CNDisplaySources`, `CNPonderIndex`, `ReactorBluePrintItemScreen`, `ReactorControllerBlockEntity:90`) et chemins de sons `"reacteur/..."`.
- Javadoc mal placée (`MultiblockHelpers`, `CNRodTypes`), commentaires paraphrasant le code (`RadiationEffect`), note `@goshante` dans `CreateNuclearJEI`.
- Duplications mineures : NBT des managers, verrous de fluide, builders `RodType`/`ReactorFluidType`, `isFood` poulet/loup, textures `WOLF_LOCATION`/`WOLF_TAME_LOCATION`.

---

## Notes de méthode

Chaque point ci-dessus a été **re-vérifié ligne à ligne** contre l'état actuel de la branche `V2` le 23/08/2026 : existence du fichier, présence effective du code incriminé, et recherche d'appelants pour tout ce qui est annoncé comme mort. Les points de l'audit initial dont le code a disparu ou été corrigé ont été retirés du document. Aucun point n'est extrapolé au-delà du code effectivement lu ; les usages Forge fonctionnels et non signalés comme temporaires ne sont pas remontés comme des problèmes.
