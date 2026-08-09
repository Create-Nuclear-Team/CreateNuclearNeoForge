# Audit de code — Migration Forge → NeoForge (CreateNuclearNeoForge)

Document de suivi vivant. À mettre à jour au fur et à mesure que les points listés sont traités.
Périmètre : intégralité de `src/main/java` (207 fichiers, ~19 500 lignes), à l'exclusion des ressources/datagen JSON.
Rappel du cadre : le mod est **en cours de migration** de Forge vers NeoForge 1.21.1. Les usages de patterns Forge qui fonctionnent correctement et ne sont pas explicitement temporaires ne sont **pas** listés comme problème. Les nouvelles fonctionnalités propres à NeoForge 1.21.1 non encore adoptées ne sont **pas** considérées comme un manque.

Légende priorité : 🔴 Critique · 🟠 Important · 🟡 Moyen · 🟢 Faible

---

## Sommaire

1. [Bugs fonctionnels critiques découverts pendant l'audit](#0-bugs-fonctionnels-critiques-découverts-pendant-laudit)
2. [Dead Code](#1-dead-code)
3. [Commentaires et Javadocs](#2-commentaires-et-javadocs)
4. [Duplications](#3-duplications)
5. [Migration Forge → NeoForge](#4-migration-forge--neoforge)
6. [Nettoyage](#5-nettoyage)
7. [Refactorisations](#6-refactorisations)
8. [Tableau de priorités global](#7-tableau-de-priorités-global)

---

## 0. Bugs de logique indépendants de la migration

Le mod étant encore en migration, une bonne partie du code manquant/commenté relevé dans ce rapport (ex. `ReactorAlarmManager`, `ReactorSummaryDisplaySource`, cf. §4) reflète simplement un travail inachevé et n'a **pas** été classée ici — ces cas sont traités dans la section [4. Migration Forge → NeoForge](#4-migration-forge--neoforge) et [1. Dead Code](#1-dead-code) comme du code à terminer, pas comme des anomalies isolées.

En revanche, les points ci-dessous sont de **vraies erreurs de logique** (inversion de paramètres, copier-coller fautif, condition inversée) qui ne s'expliquent pas par du code manquant côté migration : ils resteraient des bugs même une fois la migration terminée, et méritent donc un traitement indépendant du planning de migration.

| # | Fichier:ligne | Problème | Priorité |
|---|---|---|---|
| B1 | `content/multiblock/bluePrintItem/ReactorBluePrintItemScreen.java:56-65,71-73` | Les comptes de barres uranium/graphite sont inversés et un `+3` magique est ajouté lors de l'appel à `sendValueUpdate`, puis `sendValueUpdate` renvoie `countCooledRod` deux fois au lieu de `countFuelRod` pour le dernier paramètre : le comptage des barres envoyé au serveur est corrompu. | 🔴 |
| B2 | `content/radiation/capability/RadiationCapability.java:220-223` | Dans `applyEffects`, la branche `radiationLevel3` et la branche `else` utilisent toutes deux `amplifierLevel2.get()` (copier-coller) : il manque un `amplifierLevel3`, l'amplificateur d'effet plafonne au niveau 2 quel que soit le niveau réel de radiation. | 🟠 |
| B3 | `content/multiblock/controller/manager/ReactorInputFluidManager.java:129-130,148-149` | `int tank = h.getTanks(); h.getFluidInTank(tank);` utilise le *nombre* de tanks comme *index* (valides : `0..getTanks()-1`) → accès systématiquement hors bornes. | 🟠 |
| B4 | `lib/multiblock/manager/MultiBlockCache.java:22-24` | `isCached()` a une sémantique inversée : `return cachedResult == null` renvoie `true` quand **rien n'est en cache**. (Classe par ailleurs non utilisée, cf. §1.) | 🟡 |
| B5 | `content/multiblock/controller/ReactorControllerBlockEntity.java:489-491` | `isEmptyConfiguredPattern()` retourne `!configuredPattern.isEmpty()` — le nom est l'inverse exact du comportement. Fonctionne car l'appelant (`tick()`) compense, mais piège de maintenance. | 🟠 |

### Code manquant/inachevé lié à un travail en cours (à titre indicatif, non compté comme bug)

Ces trois cas ont une logique entièrement commentée ou un comportement neutralisé, mais rien n'indique que ce soit lié à la migration Forge→NeoForge spécifiquement (plutôt une fonctionnalité en cours de développement) — ils sont donc listés dans les sections thématiques appropriées plutôt qu'ici : `ReactorAlarmManager.clearInvalid()/getBlocksPosition()` (§1.6, purge/récupération des alarmes non finalisée), `ReactorSummaryDisplaySource` (§1.6, alimentation du résumé du réacteur non finalisée — provoque une exception tant que ce n'est pas terminé), `IrradiatedCatRelaxOnOwnerGoal.canUse()` (§1, comportement du chat jamais terminé).

---

## 1. Dead Code

### 1.1 Classes inutilisées

| Fichier | Détail | Priorité |
|---|---|---|
| `lib/multiblock/manager/MultiBlockManager.java` | Jamais référencée hors du fichier ; remplacée de facto par `MultiBlockManagerBeta`. | 🟠 |
| `lib/multiblock/manager/RegisteredMultiBlockPattern.java` | Utilisée uniquement par `MultiBlockManager`, donc morte par transitivité. | 🟡 |
| `lib/multiblock/manager/MultiBlockCache.java` | Jamais instanciée nulle part (voir aussi bug B7). | 🟠 |
| `lib/multiblock/impl/IBetterPattern.java` | Jamais utilisée ; stub cassé (`matches` → toujours `false`, `matchesWithResult` → toujours `null`, `construct` → no-op). | 🟠 |
| `foundation/block/EventTriggerBlock.java` | Seul point d'enregistrement (`CNBlocks.java:636-640`) commenté ; classe jamais instanciée en jeu. | 🟡 |
| `content/contraptions/irradiated/wolf/IrradiatedWoldCollarLayer.java` | Jamais instanciée/enregistrée ; `render()` a un corps vide ; nom mal orthographié (« Wold » au lieu de « Wolf »). | 🟠 |
| `foundation/mixin/client/RadiationHeartMixing.java` | Classe entière encapsulée dans un commentaire bloc, non listée dans `createnuclear.neoforge.mixins.json` (qui ne référence que `BaseFireBlockMixin`) : totalement inactive. | 🟠 |
| `foundation/events/possible code` | Fichier **sans extension `.java`**, donc jamais compilé mais versionné dans le repo. Contient du code Forge legacy (`net.minecraftforge.*`) et un `HudOverlayRegistry` fictif basé sur `ServiceLoader`. Brouillon oublié. | 🔴 |
| `content/multiblock/pattern/ReactorPattern.java` (`VerifyPattern5x5/7x7/9x9`, l.34-92) | Jamais référencées ; le vrai mécanisme passe par `CNMultiblock.REGISTRATE_MULTIBLOCK`. Ancienne implémentation abandonnée. | 🟡 |

### 1.2 Méthodes inutilisées

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/controller/ReactorControllerBlockEntity.java:618-635` | `convertePattern(CompoundTag)` : variable `list` non utilisée, retourne toujours `null`, jamais appelée. | 🟡 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:637-654` | `FindController(char)` privée jamais appelée ; duplique le pattern statique de `CNMultiblock`. | 🟡 |
| `content/multiblock/core/ReactorCoreEntity.java:51-68` | Idem : `FindController(char)` jamais appelée. | 🟡 |
| `content/multiblock/input/item/ReactorInputEntity.java:73-90` | Idem : `FindController(char)` jamais appelée. | 🟡 |
| `content/multiblock/ReactorAssembler.java:132-136` | `getPlayersInRadius(...)` annotée `@Deprecated`, jamais appelée. | 🟢 |
| `content/multiblock/alarm/ReactorAlarmEntity.java:98-100` | `setController(...)` jamais appelée. | 🟢 |
| `foundation/utility/CreateNuclearLang.java:67-70` | `temporaryText(String)`, `@Deprecated`, aucun appelant. | 🟢 |
| `foundation/utility/TextUtils.java:45-75,116-119` | `renderMultilineDebugText`, `renderDebugText`, `translateWithFormatting`, `leftPad` : aucun appelant dans le code. | 🟢 |
| `net/nuclearteam/createnuclear/CNDamageTypes.java:10-16` | Méthode privée `key(String)` jamais appelée (`bootstrap()` vide). | 🟢 |

### 1.3 Champs inutilisés

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/controller/ReactorControllerBlockEntity.java:60` | `public boolean test = true;` — champ de debug jamais lu ailleurs. | 🟡 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:70-71` | `public State powered = State.OFF;` assigné mais jamais lu ailleurs. | 🟢 |
| `content/multiblock/alarm/ReactorAlarmEntity.java:21` | Champ public `controller` jamais lu (hors `setController` lui-même mort). | 🟢 |
| `content/multiblock/IHeat.java:33-37` | Champ `intColor` et constructeur associé jamais utilisés (tous les enum values utilisent le constructeur `ChatFormatting`). | 🟢 |
| `net/nuclearteam/createnuclear/CNRecipeTypes.java:43,59,69,74` | Champ `isProcessingRecipe` assigné mais jamais lu. | 🟡 |
| `net/nuclearteam/createnuclear/CNAdvancement.java:46` | `public static final CreateNuclearAdvancement START = null,` jamais utilisée ailleurs. | 🟢 |
| `content/effects/VicinityEffect.java:22-27` | Paramètre constructeur `Consumer<Integertimer` jamais stocké ni utilisé. | 🟡 |

### 1.4 Constantes inutilisées

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/output/ReactorOutput.java:45,54,111` | `SPEED` (IntegerProperty) déclarée mais son ajout au state-builder est commenté à 3 endroits. | 🟢 |
| `content/multiblock/fluid/CNReactorFluidTypes.java:45-50` | Enregistrement du fluide `nitrogen` entièrement commenté. | 🟢 |
| `net/nuclearteam/createnuclear/infrastructure/config/CNCServer.java:17` | `Comments.explode` défini mais jamais utilisé côté serveur. | 🟢 |

### 1.5 Imports inutiles

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/logistics/BigFluidStack.java:4` | `net.minecraft.core.registries.BuiltInRegistries` inutilisé. | 🟢 |
| `content/redstone/displayLink/source/ReactorSummaryDisplaySource.java:13,17,25` | `Item`, `TypeRodPredicate`, `Map` ne servent plus qu'au code mort du bug B3. | 🟡 |
| `content/uraniumOre/UraniumOreBlock.java:18-19` | `EnchantmentHelper`, `Enchantments` ne servent qu'au bloc XP commenté (l.93-96). | 🟢 |
| `content/radiation/CNRadiationValues.java:5` | Import lié à l'enregistrement de radiation de biome commenté, jamais réactivé. | 🟢 |
| `lib/multiblock/SimpleMultiBlockAislePatternBuilder.java:5,7` / `SimpleMultiBlockPatternBuilder.java:6,7` | Import dupliqué (deux fois la même ligne) de `IMultiBlockPatternBuilder`. | 🟢 |
| `infrastructure/data/CreateNuclearDatagen.java:20` | Import de `CNShapelessRecipeGen`, dont l'instanciation est commentée l.51 (classe morte, cf. §3). | 🟢 |

### 1.6 Code commenté pouvant être supprimé

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `net/nuclearteam/createnuclear/CNBlocks.java:636-640` | Bloc entier commenté (`TEST_EVENT_TRIGGER_BLOCK`). | 🟡 |
| `net/nuclearteam/createnuclear/CNFluids.java:114` | Ligne d'enregistrement `onRegister(...)` commentée sur `LIQUID_NITROGEN`. | 🟢 |
| `content/multiblock/controller/manager/ReactorAlarmManager.java:9,51-53,65-67` | Import et logique commentés — directement liés au bug B1. | 🔴 |
| `content/multiblock/ReactorAssembler.java:78,107-108` | `reactorAlarmBlock` et bloc `addAlarm` commentés dans `findAndRegisterSpecialBlocks` — les alarmes ne sont jamais reliées lors d'un réassemblage complet. | 🟠 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:165-171` | Bloc `deserializeInventory`/`serializeInventory` commenté. | 🟢 |
| `content/multiblock/input/item/ReactorInput.java:69-80` | Bloc `setPlacedBy` commenté avec note « may be useless » jamais nettoyée. | 🟡 |
| `content/multiblock/frame/ReactorGaugeOverrides.java:11-14,19` | Bloc de log de debug et `.predicate(...)` commentés. | 🟢 |
| `content/multiblock/bluePrintItem/ReactorBluePrintMenu.java:160-164` | Bloc `LOGGER.warn(...)` commenté. | 🟢 |
| `content/multiblock/output/ReactorOutputEntity.java:137` | `//convertToDirection(...)` commenté. | 🟢 |
| `content/radiation/CNRadiationValues.java:14-17` | Bloc d'enregistrement de radiation de biome commenté. | 🟢 |
| `content/uraniumOre/UraniumOreBlock.java:93-96` | Bloc XP entièrement commenté dans `spawnAfterBreak`. | 🟢 |
| `compat/jei/CreateNuclearJEI.java:132-139` | Bloc commenté (avec note personnelle) pour générer des fluides de potion par `BottleType`, dupliqué juste après par le code actif qui ne gère que `REGULAR`. | 🟡 |
| `foundation/advancement/CNAdvancement.java:239-261,319-340` | Deux blocs entiers d'avancements commentés (`ANTI_RADIATION_ARMOR`, `AVOIDING_CANCER`, `DYE_ANTI_RADIATION_ARMOR`, `REACTOR_ROD_INPUT`, `REACTOR_FLUID_INPUT`, `REACTOR_OUTPUT`). | 🟢 |
| `foundation/events/HudRenderer.java:15` | `//new RadiationOverlay(),` commenté — `RadiationOverlay` n'est donc jamais affiché alors que `HelmetOverlay.java:67` continue d'appeler `RadiationOverlay.setCoverage(...)` sans effet visuel. | 🟠 |
| `foundation/events/overlay/EventTextOverlay.java:40` | `return timer 0 && false;` : l'overlay est **toujours inactif**, quel que soit le minuteur (bug/fonctionnalité désactivée non documentée). | 🟠 |
| `foundation/utility/RenderHelper.java:46-59` | `//graphics.pose().scale(coverage, coverage, 1f);` commenté : la branche `coverage != 1f` devient strictement identique à la branche `coverage == 1f`. | 🟢 |
| `api/multiblock/rods/RodType.java:218-223,238,252,266` / `api/multiblock/fluid/ReactorFluidType.java:199,209` | Commentaires de code mort type `//CNConfigs.server().rods.maxHeat.get();` juxtaposés à des valeurs en dur — intention de lecture config jamais implémentée. | 🟠 |
| `content/multiblock/bluePrintItem/ReactorBluePrintItem.java:60-74` | Branches `if (!shiftDown)` / `else if (shiftDown)` au corps strictement identique — le test n'a aucun effet. | 🟢 |

### 1.7 Fichiers non-code oubliés dans l'arborescence source

| Fichier | Détail | Priorité |
|---|---|---|
| `content/multiblock/bluePrintItem/test.txt` | Dump de debug de `ReactorBluePrintData` laissé à côté du code source Java. | 🟡 |
| `foundation/events/possible code` | Voir §1.1 — scratch file sans extension `.java`, contenant du code Forge legacy jamais intégré. | 🔴 |

---

## 2. Commentaires et Javadocs

### 2.1 Commentaires/Javadocs en français (à traduire ou signaler dans le style du projet, qui est très majoritairement en anglais)

| Fichier:ligne | Extrait | Priorité |
|---|---|---|
| `infrastructure/config/CNCCommon.java:18` | `static String explode = "Explose: pas d'idée";` — texte visible en jeu comme description de config. | 🟠 |
| `content/multiblock/controller/manager/ReactorAlarmManager.java:47,50,64` | *« On ne supprime pas si le chunk est juste déchargé »*, *« Si le bloc n'existe plus... on marque pour suppression »*, *« On vérifie que l'entité est bien chargée... »* — masquent en plus la logique cassée (bug B1). | 🟠 |
| `content/multiblock/controller/manager/ReactorAlarmManagerI.java:9-10` | Javadoc en français et mal formée (astérisque en trop). | 🟡 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:332,363` | *« permet de savoir si le réacteur est formé ou pas »* ; *« (Si les methode read et write ne sont pas implémenté... les items... auront disparu !) »*. | 🟢 |
| `content/multiblock/controller/manager/ReactorInputManager.java:138,141,143,146,154` | *« On récupère le nom de l'item »*, *« Comparaison intelligente : on ignore la casse et les underscores »*, etc. | 🟢 |
| `content/multiblock/input/fluid/ReactorFluidInputEntity.java:29,54-56,67,85,95` | Javadoc entièrement en français, plus deux commentaires d'incertitude technique liés à la migration (*« Pensez à passer registries si requis par la v1.20+... »*). | 🟠 |
| `content/multiblock/input/fluid/ReactorFluidInput.java:91` | *« Convertit le vieux InteractionResult en ItemInteractionResult si nécessaire pour NeoForge »*. | 🟡 |
| `content/multiblock/casing/ReactorCasing.java:78` | *« En attendant le controller pour verifier le pattern »* — suggère du code temporaire non résolu. | 🟡 |
| `foundation/mixin/client/RadiationHeartMixing.java:27,30,32-33` | Commentaires en français à l'intérieur du bloc commenté (classe morte, cf. §1). | 🟢 |
| `content/logistics/BigFluidStack.java:35,42,48` | *« Utilisation de la méthode de sauvegarde moderne... »*, *« Utilisation du codec/parseur moderne... »*, *« Utilisation du StreamCodec natif de NeoForge... »* — paraphrasent le code sans apporter d'information. | 🟠 |
| `api/multiblock/rods/RodType.java:218-223` | *« Lazy config resolution... »* mélangé à du code mort commenté en anglais/français incohérent. | 🟢 |

### 2.2 Commentaires peu explicites ou ambigus

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/controller/ReactorControllerBlockEntity.java:489-491` | Nom de méthode trompeur `isEmptyConfiguredPattern()` (voir bug B8). | 🟠 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:110-112` | Marqueur de section `/* FORGE ARGUMENTS PART */` — trompeur pour du code désormais NeoForge. | 🟢 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:154-158` | Commentaires décrivant une architecture DIP (`IHeatService`/`IPersistenceService`) jamais implémentée — aspiration non concrétisée, trompeuse pour le lecteur. | 🟢 |
| `content/multiblock/controller/ReactorControllerBlock.java:147` | *« this is the Function that verifies if the pattern is correct (as a test, we added the energy output) »* — suggère du code de test laissé en prod. | 🟡 |
| `foundation/block/EventTriggerBlock.java:33` | Log de debug oublié : `LOGGER.warn("hum EventTriggerBlock ? {}", packet);` — niveau `warn` inapproprié, ton de debug. | 🟢 |
| `foundation/events/overlay/EventTextOverlay.java:47` | Idem : `LOGGER.warn("hum EventTextOverlay: {}", timer);`, combiné au bug de désactivation permanente. | 🟢 |
| `compat/jei/CreateNuclearJEI.java:132-134` | Commentaire attribué nominativement à un contributeur (« `@goshante:` ») au-dessus d'un bloc mort. | 🟢 |

### 2.3 Javadocs incomplètes ou non standard

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/controller/manager/ReactorInputFluidManager.java` (7 méthodes, ex. l.29-33,44-47,60-63) | Javadoc placée **après** `@Override` au lieu d'avant — non reconnue par l'outillage Javadoc standard. | 🟡 |
| `content/multiblock/MultiblockHelpers.java:54-57` | Javadoc placée **à l'intérieur** du corps de `getControllerForPart` au lieu d'être au-dessus de la signature. | 🟢 |
| `content/multiblock/controller/manager/ReactorOutputManager.java:44-69` | Javadoc de 26 lignes contenant un exemple de code complet référençant des champs publics (`outEntity.heat = speed;`) — bien trop volumineuse pour un Javadoc de méthode. | 🟡 |
| `content/rod/CNRodTypes.java:14-35` | Javadoc utile mais mal placée (documente `RodType.Builder` en général, pas la méthode `bootstrap()` sur laquelle elle est apposée). | 🟢 |
| `content/radiation/RadiationEffect.java:21-73` | Commentaires inline très verbeux, répétant littéralement le code (`// Reduces movement speed by 20%` juste au-dessus de la ligne qui le fait). | 🟢 |

### 2.4 Commentaires devenus obsolètes

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `foundation/advancement/CNAdvancementBehaviour.java:81-83` | Commentaire français documentant un `@Override` volontairement retiré en attendant une vérification de signature Create/NeoForge 1.21 — dette explicite non résolue (cf. §4). | 🟠 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:685-743,745-752` | `getStructureBounds(...)`/`applyOffset(...)` marquées `@Deprecated` mais toujours utilisées activement en production (`ReactorAssembler.java:52`) — annotation trompeuse. | 🟡 |
| `net/nuclearteam/createnuclear/CNTags.java:32-46,52` | Méthodes `forgeTag`/`forgeBlockTag`/`forgeItemTag`/`forgeFluidTag` et `FORGE("forge")` — nommage hérité de l'ère Forge alors qu'elles pointent en réalité vers le namespace commun NeoForge `"c"` ; `FORGE("forge")` semble inutilisée (seul `NEO_FORGE` sert). | 🟠 |

---

## 3. Duplications

| Fichier(s) | Détail | Priorité |
|---|---|---|
| `foundation/data/recipe/CNShapelessRecipeGen.java` vs `CNStandardRecipeGen.java` | ~350 lignes quasi identiques (Marker, enterFolder, create x3, createSpecial, blastCrushedMetal, recycleGlass*, blastFurnaceRecipe*, metalCompacting, conversionCycle, clearData, classes internes `GeneratedRecipeBuilder`/`GeneratedCookingRecipeBuilder`/shims). `CNShapelessRecipeGen` n'est jamais instanciée (commentée dans `CreateNuclearDatagen.java:51`) : à supprimer ou fusionner. | 🔴 |
| `content/multiblock/casing/ReactorCasing.java`, `cooler/ReactorCooler.java`, `frame/ReactorFrame.java`, `input/item/ReactorInput.java`, `output/ReactorOutput.java` | La méthode `FindController(...)` (recherche brute-force par triple boucle) est copiée-collée dans **cinq classes non liées par héritage**, alors qu'un mécanisme O(1) existe déjà (`MultiblockHelpers`/`ReactorPattern.findControllerPos`, utilisé par `ReactorAlarm`/`ReactorFluidInput`). Deux systèmes de recherche du contrôleur cohabitent. | 🟠 |
| `content/multiblock/controller/manager/ReactorInputManager.java`, `ReactorOutputManager.java`, `ReactorInputFluidManager.java` | `getBlocksPosition(Level)` réimplémente 3 fois le même filtre `instanceof XxxEntity`, avec une variable locale `positions` qui masque le champ protégé homonyme d'`AbstractReactorIOManager`. | 🟡 |
| `content/multiblock/controller/manager/ReactorInputManager.java:31-52`, `ReactorInputFluidManager.java:34-58`, `ReactorAlarmManager.java:17-39` | Sérialisation NBT (`read`/`write`, triplet x/y/z) identique répétée dans 3 managers. | 🟢 |
| `content/multiblock/bluePrintItem/ReactorBluePrintMenu.java:54-64,108-118,128-138` | Le même tableau `int[][] positions` (57 entrées) dupliqué **littéralement trois fois** dans la même classe. | 🟡 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:656-683` vs `content/multiblock/controller/ReactorControllerBlock.java:177-195` | `rotate(...)`/`Rotate(...)` dupliquent la même logique `speed`/`heat`/`updateSpeed`/`updateGeneratedRotation`. | 🟡 |
| `content/multiblock/input/fluid/FluidLockManager.java` vs `PersistentFluidLocks.java` | Logique de verrouillage de fluide dupliquée entre version mémoire et version persistante ; le chemin mémoire n'est utilisé qu'en secours, quasi jamais atteint côté serveur. | 🟢 |
| `foundation/block/HorizontalDirectionalReactorBlock.java` vs `MultiDirectionalReactorBlock.java` | Structure `rotate`/`mirror` identique, seule la propriété (`HORIZONTAL_FACING` vs `FACING`) diffère. | 🟡 |
| `foundation/events/overlay/RadiationOverlay.java` vs `IrradiatedOverlayRendererVision.java` | Deux implémentations parallèles du même overlay « vision irradiée » (même texture, même logique de fondu). `RadiationOverlay` suit le pattern `HudOverlay` mais est désactivé (§1.6) ; `IrradiatedOverlayRendererVision` est la version réellement enregistrée (`CNClientEvent.java:19`). Deux tentatives successives non unifiées. | 🟠 |
| `net/nuclearteam/createnuclear/CNItems.java` | Motif de recette « `_from_decompacting` » répété ~9 fois de façon quasi identique. | 🟡 |
| `lib/multiblock/SimpleMultiBlockAislePatternBuilder.java` vs `SimpleMultiBlockPatternBuilder.java` | Logique de construction largement dupliquée (`lookup`/`predicateHashMap`, `blockProvider`, validation `'*'`, message d'erreur identique). | 🟡 |
| `api/multiblock/rods/RodType.java` (Builder) vs `api/multiblock/fluid/ReactorFluidType.java` (Builder) | Deux builders structurellement identiques (drapeaux `xxxSet`, liste `missing`, `useConfig`). | 🟢 |
| `content/equipment/armor/AntiRadiationArmorItem.java:162-176` | `isArmored(ItemStack)` et `isArmored2(ItemStack)` vérifient la même chose avec deux implémentations différentes ; `isArmored2` semble sans appelant dans le périmètre audité. | 🟠 |
| `content/radiation/capability/RadiationCapability.java:148-178` | `computeItemRadiation(Player)` réimplémente manuellement la logique déjà factorisée dans `getStackRadiation(ItemStack, LivingEntity)`. | 🟠 |
| Trois animaux irradiés (`IrradiatedCat`, `IrradiatedChicken`, `IrradiatedWolf`) | `tryToTame(Player)` quasi identique entre `IrradiatedCat`/`IrradiatedWolf` ; patterns `isFood`/tags FUEL identiques dans les trois classes ; goals `AvoidEntityGoal` dupliqués (fichier séparé côté chat, classe interne côté loup). | 🟡 |
| `content/contraptions/irradiated/wolf/IrradiatedWolfRenderer.java:16-17` | `WOLF_LOCATION` et `WOLF_TAME_LOCATION` pointent vers exactement la même texture — variable redondante. | 🟢 |
| `net/nuclearteam/createnuclear/CNTags.java` (5 enums) | Méthode privée `init() {}` vide dupliquée à l'identique dans les 5 enums de tags (limite du langage, pas de vraie remédiation possible). | 🟢 |

---

## 4. Migration Forge → NeoForge

Rappel : uniquement les éléments clairement transitoires/résiduels de la migration technique. Le fonctionnement correct actuel n'est pas remis en cause en soi.

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `net/nuclearteam/createnuclear/CreateNuclear.java:64,93` | `IEventBus forgeEventBus = NeoForge.EVENT_BUS;` — variable nommée d'après l'ancienne API alors qu'elle référence le bus NeoForge ; utilisée telle quelle plus loin. | 🟠 |
| `net/nuclearteam/createnuclear/CreateNuclear.java:98` | `//DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` — `DistExecutor` est une API Forge, remplacée en NeoForge par `@Mod(dist = Dist.CLIENT)` (déjà utilisée correctement dans `CreateNuclearClient.java:11`). Ligne morte à supprimer. | 🟠 |
| `net/nuclearteam/createnuclear/CreateNuclearClient.java:20` | `IEventBus neoEventBus = NeoForge.EVENT_BUS;` déclarée mais jamais utilisée — vestige d'un ancien câblage d'événements client. | 🟡 |
| `net/nuclearteam/createnuclear/CNTags.java:32-52` | `forgeTag`/`forgeBlockTag`/`forgeItemTag`/`forgeFluidTag` et enum `FORGE("forge")` : nommage hérité de Forge pour un mécanisme qui pointe désormais vers le namespace commun NeoForge `"c"`. Fonctionne correctement mais induit en erreur (utilisé des dizaines de fois dans `CNBlocks.java`, `CNItems.java`, `CreateNuclearRegistrateTags.java`). | 🟠 |
| `content/multiblock/input/item/ReactorInputEntity.java:109-118` | Bloc commenté utilisant explicitement l'ancienne API de capacités Forge (`Capability<?, `ForgeCapabilities.ITEM_HANDLER`, `ResetableLazy<T) — reliquat direct jamais retiré après le passage aux capacités NeoForge (`Capabilities.ItemHandler.BLOCK` utilisé ailleurs dans le même package). | 🟠 |
| `content/multiblock/input/fluid/ReactorFluidInputEntity.java:85,95` | Commentaires d'incertitude explicites sur la bonne API post-migration (*« Pensez à passer registries si requis par la v1.20+... »*, *« Pareil ici selon l'implémentation de SmartFluidTank »*) — notes-à-soi-même jamais nettoyées. | 🟠 |
| `content/multiblock/input/fluid/ReactorFluidInput.java:91` | *« Convertit le vieux InteractionResult en ItemInteractionResult si nécessaire pour NeoForge »* — le « si nécessaire » signale une incertitude non tranchée. | 🟡 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:110-112` | Marqueur de section `/* FORGE ARGUMENTS PART */`, obsolète et trompeur. | 🟢 |
| `foundation/advancement/CNAdvancementBehaviour.java:81-92` | `write`/`read` sans `@Override`, avec commentaire explicite indiquant que la signature attendue par `BlockEntityBehaviour` (Create, migré vers NeoForge 1.21) n'a jamais été vérifiée. Dette de migration explicite et non résolue. | 🟠 |
| `foundation/mixin/client/RadiationHeartMixing.java` | Mixin ciblant probablement une signature Forge ayant changé sous 1.20.2+/NeoForge, désactivé en bloc en attendant une réécriture jamais faite. | 🟠 |
| `foundation/data/recipe/CNShapelessRecipeGen.java` | Quasi-clone de `CNStandardRecipeGen`, jamais instancié — suggère une classe issue d'une itération précédente du portage, dupliquée par erreur puis jamais retirée. | 🟡 |
| `foundation/events/possible code` | Scratch file contenant explicitement du code `net.minecraftforge.*` — brouillon de portage jamais finalisé ni supprimé. | 🔴 |
| `content/effects/IodineEffect.java:14` | Cast douteux `(Holder<...Attribute(Object)CNAttributes.IRRADIATED_RESISTANCE` — double-cast via `Object`, typique d'un contournement de typage non résolu proprement pendant le portage. | 🟠 |
| `content/contraptions/irradiated/cat/IrradiatedCatRenderer.java:5`, `content/contraptions/irradiated/wolf/IrradiatedWolf.java:3` | Import `com.mojang.math.MethodsReturnNonnullByDefault` au lieu de `net.minecraft.MethodsReturnNonnullByDefault` (utilisé partout ailleurs) — incohérence probablement issue d'un auto-import IDE pendant le portage. | 🟡 |
| `content/kinetics/fan/processing/CNFanProcessingTypes.java:36-62` | `LEGACY_NAME_MAP`/`ofLegacyName`/`parseLegacy` : shim de compatibilité de noms probablement lié à d'anciennes sauvegardes/NBT pré-migration — à confirmer si toujours nécessaire. | 🟡 |
| `infrastructure/config/CNCCommon.java:7`, `CNCServer.java:6` | `CRods` imbriqué à la fois dans la config `Common` et `Server` — à vérifier si voulu ou résidu de réorganisation de config pendant la migration (source de confusion avec deux sections « Rods » dans deux fichiers config). | 🟡 |

### Parties restant à migrer / terminer (logique commentée, non spécifiquement liée à l'API Forge mais bloquant tant que le portage n'est pas achevé)

| Fichier:ligne | Détail | Priorité |
| --- | --- | --- |
| `content/multiblock/controller/manager/ReactorAlarmManager.java:46-70` | `clearInvalid()` et `getBlocksPosition(Level)` ont leur logique interne entièrement commentée : les alarmes détruites en jeu ne sont jamais purgées, et la récupération des positions renvoie toujours une liste vide. À terminer avant de considérer le système d'alarme comme fonctionnel. | 🟠 |
| `content/redstone/displayLink/source/ReactorSummaryDisplaySource.java:162-170,189-192` | Le code qui alimente `fuel`, `cooler`, `fluid`, `heat` du `ReactorSummary.Builder` est commenté, alors que `ReactorSummary.Builder.build()` lève une `IllegalStateException` si l'un de ces champs est `null` → `getReactorSummary()` plante systématiquement tant que ce n'est pas terminé. | 🟠 |
| `content/multiblock/ReactorAssembler.java:78,107-108` | `reactorAlarmBlock` et bloc `addAlarm` commentés dans `findAndRegisterSpecialBlocks` — les alarmes ne sont jamais reliées lors d'un réassemblage complet, cohérent avec `ReactorAlarmManager` encore inachevé. | 🟡 |
| `content/contraptions/irradiated/cat/IrradiatedCatRelaxOnOwnerGoal.java:41-58` | `canUse()` calcule `blockpos`/`blockstate` sans les utiliser et retourne toujours `false` : le comportement « chat se blottit contre le propriétaire endormi » n'a jamais été terminé. | 🟡 |

---

## 5. Nettoyage

Éléments à retirer une fois la migration complètement terminée et les points de la section 4 tranchés.

- Supprimer `foundation/events/possible code` (scratch file Forge).
- Supprimer ou finaliser `foundation/mixin/client/RadiationHeartMixing.java`.
- Supprimer `foundation/block/EventTriggerBlock.java` (ou décommenter son enregistrement dans `CNBlocks.java:636-640` si le bloc de test doit être conservé).
- Supprimer ou fusionner `foundation/data/recipe/CNShapelessRecipeGen.java` avec `CNStandardRecipeGen.java`.
- Trancher entre `RadiationOverlay` et `IrradiatedOverlayRendererVision`, supprimer l'implémentation non retenue, et réactiver/retirer `HudRenderer.java:15`.
- Retirer `CreateNuclearLang.temporaryText`, `TextUtils.renderMultilineDebugText/renderDebugText/translateWithFormatting/leftPad` (aucun appelant).
- Retirer les champs de « cache » inertes de `RenderHelper.java:13-15`.
- Nettoyer les blocs d'avancements commentés dans `CNAdvancement.java`.
- Supprimer `lib/multiblock/manager/MultiBlockManager.java`, `RegisteredMultiBlockPattern.java`, `MultiBlockCache.java`, `lib/multiblock/impl/IBetterPattern.java` une fois confirmé que `MultiBlockManagerBeta` les remplace intégralement.
- Supprimer `content/multiblock/bluePrintItem/test.txt`.
- Supprimer `content/contraptions/irradiated/wolf/IrradiatedWoldCollarLayer.java` si le calque n'est réellement jamais utilisé.
- Nettoyer les imports/blocs commentés listés en §1.5/§1.6 (`CNRadiationValues.java`, `UraniumOreBlock.java`, `CreateNuclearJEI.java`, `BigFluidStack.java`, `ReactorSummaryDisplaySource.java`).
- Renommer `forgeEventBus` → `neoForgeEventBus` dans `CreateNuclear.java`, retirer `neoEventBus` inutilisé dans `CreateNuclearClient.java`, retirer la ligne `DistExecutor` commentée.
- Une fois le nommage `CNTags.forgeXxxTag`/`FORGE` validé comme voulu ou non, renommer en `commonXxxTag` ou supprimer l'entrée `FORGE` inutilisée.
- Retirer `content/multiblock/input/item/ReactorInputEntity.java:109-118` (bloc de capacités Forge commenté).

---

## 6. Refactorisations

Uniquement des refactors pertinents **après** la fin de la migration — pas liés à la dette de migration elle-même, et n'impliquant pas l'adoption de nouvelles fonctionnalités NeoForge 1.21.1.

- **Recherche du contrôleur multiblock** : remplacer les cinq implémentations dupliquées de `FindController` (`ReactorCasing`, `ReactorCooler`, `ReactorFrame`, `ReactorInput`, `ReactorOutput`) par l'appel unique à `MultiblockHelpers`/`ReactorPattern.findControllerPos` déjà utilisé par les blocs plus récents.
- **`AbstractReactorIOManager`** : ajouter une méthode générique `filterByType(Level, Class<T` pour factoriser les trois implémentations quasi identiques de `getBlocksPosition(Level)` (Input/Output/InputFluid).
- **`ReactorControllerBlockEntity`** (classe « god object » de ~870 lignes) : extraire l'assemblage, le calcul de chaleur, la gestion NBT, la gestion des managers I/O et la rotation en services distincts — les commentaires DIP avortés (l.154-158) montrent que l'intention existait déjà.
- **`ReactorBluePrintMenu`** : extraire le tableau `positions` dupliqué trois fois en une constante statique unique.
- **`CNItems.java`** : factoriser le motif de recette « `_from_decompacting` » répété ~9 fois en une méthode utilitaire.
- **`SimpleMultiBlockAislePatternBuilder`/`SimpleMultiBlockPatternBuilder`** : extraire un tronc commun (classe abstraite) pour la logique de construction partagée.
- **`RodType.Builder`/`ReactorFluidType.Builder`** : factoriser le squelette de validation par booléens `xxxSet` partagé par les deux builders.
- **`HorizontalDirectionalReactorBlock`/`MultiDirectionalReactorBlock`** : fusionner en une classe abstraite générique paramétrée par le `DirectionProperty`.
- **`CNRecipeProvider`** : unifier les deux mécanismes d'enregistrement de générateurs de recettes qui coexistent (liste interne `GENERATORS` vs appels directs dans `CreateNuclearDatagen`).
- **Animaux irradiés** (`IrradiatedCat`/`IrradiatedChicken`/`IrradiatedWolf`) : factoriser `tryToTame`, `isFood`/tags FUEL et les goals d'évitement dans une classe/interface utilitaire partagée pour « TamableAnimal irradié ».
- **`AntiRadiationArmorItem.isArmored`/`isArmored2`** : fusionner en une seule API une fois confirmé qu'aucun appelant externe ne dépend de la variante conservée.
- **`RadiationCapability.computeItemRadiation`** : faire systématiquement appel à `getStackRadiation` au lieu de réimplémenter la boucle main-hand/armure/offhand.
- **`CNArmorMaterials.durabilityForType`** : extraire le tableau `BASE_DURABILITY` recréé à chaque appel en constante statique.
- **`ReactorControllerBlock`** (`Verify`, `Rotate`, `FindController`) : envisager d'aligner le nommage PascalCase de ces méthodes publiques sur la convention camelCase Java standard.

---

## 7. Tableau de priorités global

### 🔴 Critique

| Fichier | Sujet |
| --- | --- |
| `content/multiblock/bluePrintItem/ReactorBluePrintItemScreen.java` | Comptage de barres uranium/graphite corrompu — vraie erreur de logique, indépendante de la migration (bug B1) |
| `foundation/data/recipe/CNShapelessRecipeGen.java` / `CNStandardRecipeGen.java` | ~350 lignes dupliquées, classe morte |
| `foundation/events/possible code` | Scratch file Forge non compilé, oublié dans le repo |

### 🟠 Important

- Bugs de logique indépendants de la migration : `RadiationCapability.applyEffects` (bug B2, copier-coller), `ReactorInputFluidManager` (bug B3, index hors bornes), `ReactorControllerBlockEntity.isEmptyConfiguredPattern` (bug B5, nom inversé).
- Parties restant à migrer/terminer (code manquant, cf. §4) : purge/récupération des alarmes (`ReactorAlarmManager`), alimentation du résumé du réacteur (`ReactorSummaryDisplaySource`), liaison des alarmes lors du réassemblage (`ReactorAssembler`) — ces trois-là bloquent la fonctionnalité tant que le portage n'est pas achevé, mais ne sont pas des anomalies isolées.
- Classes mortes/désactivées à trancher : `MultiBlockManager`/`MultiBlockCache`/`IBetterPattern`, `IrradiatedWoldCollarLayer`, `RadiationHeartMixing`, `RadiationOverlay` vs `IrradiatedOverlayRendererVision`, `HudRenderer` (overlay radiation jamais affiché), `EventTextOverlay.isActive()` toujours `false`.
- Débris de migration à finaliser : `CreateNuclear.forgeEventBus`/`DistExecutor` commenté, `CNTags.forgeXxxTag`/`FORGE`, capacités Forge commentées dans `ReactorInputEntity`, `CNAdvancementBehaviour.write/read` sans `@Override`, cast douteux `IodineEffect`, notes d'incertitude dans `ReactorFluidInputEntity`.
- Duplication `FindController` (5 classes), `AntiRadiationArmorItem.isArmored/isArmored2`, `RadiationCapability.computeItemRadiation`.
- Commentaires français impactant l'expérience joueur ou masquant un bug : `CNCCommon.explode`, `ReactorAlarmManager`, `ReactorFluidInputEntity`, `BigFluidStack`.
- `RodType`/`ReactorFluidType` : intention de lecture config jamais implémentée (code mort + doc trompeuse).

### 🟡 Moyen

- Duplications diverses (managers I/O, `ReactorBluePrintMenu.positions`, `HorizontalDirectionalReactorBlock`/`MultiDirectionalReactorBlock`, `CNItems` decompacting, builders `SimpleMultiBlock*`, animaux irradiés).
- Javadoc mal placée (`ReactorInputFluidManager`, `MultiblockHelpers`, `ReactorOutputManager`).
- `CNRecipeTypes.isProcessingRecipe`, `VicinityEffect.timer`, blocs commentés secondaires (`ReactorAssembler` alarmes, `ReactorInput.setPlacedBy`, `CreateNuclearJEI`).
- Fichier de dump `test.txt`, imports morts liés à du code commenté.
- `CNCCommon`/`CNCServer` : section `CRods` potentiellement dupliquée entre deux fichiers de config.
- `CNFanProcessingTypes` : shim de noms legacy à confirmer.

### 🟢 Faible

- Champs/méthodes/constantes isolés sans impact (`IHeat.intColor`, `ReactorOutput.SPEED`, `CNDamageTypes.key`, `TextUtils`/`CreateNuclearLang` méthodes mortes, etc.).
- Imports inutilisés isolés.
- Commentaires verbeux ou informels sans risque (`RadiationEffect`, logs `"hum ..."`, marqueur `FORGE ARGUMENTS PART`).
- Petites incohérences de nommage/texture (`WOLF_LOCATION`/`WOLF_TAME_LOCATION`, `CNSoundEvents` chemins en français `"reacteur/..."`).
- Refactors cosmétiques (nommage PascalCase de `ReactorControllerBlock`, constante `BASE_DURABILITY`).

---

## Notes de méthode

Cet audit a été réalisé par lecture intégrale des 207 fichiers Java du module (`src/main/java`), répartis en quatre zones (bibliothèque multiblock/API/infrastructure/registries, contenu multiblock du réacteur, autres sous-systèmes de contenu + compat JEI, et package `foundation`). Chaque point est ancré sur un fichier et une ligne précis dans l'état du dépôt à la date de cet audit (branche `V2`). Aucun point n'a été extrapolé au-delà du code effectivement lu ; les usages Forge fonctionnels et non signalés comme temporaires n'ont pas été remontés comme des problèmes.

---

## Prompt d'origin

```md
Réalise un audit complet du code de la version NeoForge et consigne le résultat dans un fichier Markdown directement dans le projet.

Contexte :

* Le projet est actuellement en cours de migration de la V2 Forge vers NeoForge.
* Cette migration n'est pas terminée.
* L'objectif est d'identifier les points restant à traiter avant d'entamer une véritable modernisation vers les fonctionnalités propres à NeoForge 1.21.1.

Le rapport doit au minimum contenir les sections suivantes :

## 1. Dead Code

* Classes inutilisées.
* Méthodes inutilisées.
* Champs inutilisés.
* Constantes inutilisées.
* Imports inutiles.
* Code commenté pouvant être supprimé.
* Code devenu inaccessible ou obsolète à la suite de la migration.

## 2. Commentaires et Javadocs

* Tous les commentaires et Javadocs rédigés en français.
* Les commentaires peu explicites ou ambiguës.
* Les Javadocs incomplètes ou ne respectant pas les standards Java.
* Les commentaires devenus obsolètes.

## 3. Duplications

* Logique dupliquée.
* Méthodes très similaires.
* Blocs de code répétitifs.
* Possibilités de mutualisation.
* Duplication entre Forge et NeoForge.

## 4. Migration Forge → NeoForge

Identifier tout le code correspondant uniquement à une migration technique de Forge vers NeoForge, notamment :

* API encore héritées de Forge.
* Adaptations temporaires.
* Compatibilités provisoires.
* TODO liés à la migration.
* Parties restant à migrer.
* Code pouvant être simplifié une fois la migration terminée.

Cette section ne doit pas prendre en compte les nouvelles fonctionnalités spécifiques à NeoForge 1.21.1.

## 5. Nettoyage

Lister tout ce qui pourra être supprimé une fois la migration complètement terminée.

## 6. Refactorisations

Identifier les refactorisations pertinentes uniquement après la fin de la migration.

## 7. Priorités

Classer chaque élément selon son niveau de priorité :

* 🔴 Critique
* 🟠 Important
* 🟡 Moyen
* 🟢 Faible

Contraintes :

* Analyse l'intégralité du code NeoForge.
* Ignore les nouvelles fonctionnalités propres à NeoForge 1.21.1.
* Ne considère pas comme problème le fait qu'une fonctionnalité Forge n'ait pas encore été remplacée par son équivalent NeoForge moderne si elle fonctionne correctement pendant la migration.
* Base toutes les conclusions uniquement sur le code réellement présent.
* N'invente aucun problème.
* Crée directement le fichier Markdown dans le projet.
* Utilise une structure claire, cohérente et facilement maintenable afin qu'il puisse servir de document de suivi pendant toute la migration.
```
