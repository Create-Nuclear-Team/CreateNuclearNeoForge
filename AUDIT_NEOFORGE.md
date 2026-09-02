# Audit de code — Migration Forge → NeoForge (CreateNuclearNeoForge)

Document de suivi vivant. **Ne contient que les points encore ouverts.**
Dernière re-vérification intégrale contre le code : **23/08/2026** (branche `V2-Audit`, commit `d2c7078`).
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
9. [Historique des corrections](#8-historique-des-corrections)

---

## 0. Bugs de logique

| # | Fichier:ligne | Problème | Priorité |
|---|---|---|---|

---

## 1. Dead Code

### 1.1 Classes inutilisées / entièrement mortes

| Fichier | Détail | Priorité |
|---|---|---|

### 1.2 Méthodes inutilisées

| Fichier:ligne | Détail | Priorité |
|---|---|---|

### 1.3 Champs inutilisés

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/controller/ReactorControllerBlockEntity.java:60,372` | `countCoolerRod` est assigné en `tick()` mais jamais relu ensuite (champ « write-only »). **Confirmé pré-existant côté Forge** : `countCoolerRod` y est aussi write-only sur cette classe (`triggerExplosion` ne prend que `countFuelRod`), ce n'est donc pas un artefact de migration. Vu la symétrie avec `countFuelRod` (qui, lui, alimente `triggerExplosion`), il a probablement été prévu pour atténuer l'explosion via les cooler rods mais n'a jamais été branché. **Point mis de côté** : à discuter plus tard (câbler dans `triggerExplosion`, ou supprimer avec `getConfiguredPatternCoolerRodCount()`) — aucune décision prise pour l'instant. | 🟡 |

### 1.5 Imports inutiles

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/uraniumOre/UraniumOreBlock.java:18-19` | `EnchantmentHelper`, `Enchantments` ne servent qu'au bloc XP commenté (l.93-96). | 🟢 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:8-14` | `import net.minecraft.core.*;` rend redondants les imports explicites `BlockPos`, `Direction`, `HolderLookup` ; imports totalement inutilisés en plus : `SimpleMultiBlockAislePatternBuilder`, `CatnipServices`, `ChatFormatting`. | 🟢 |
| `lib/multiblock/SimpleMultiBlockAislePatternBuilder.java:5-7` | `import lib.multiblock.impl.IMultiBlockPatternBuilder;` importé deux fois (lignes 5 et 7). | 🟢 |
| `api/ItemRodTypesValue.java:5,10`, `api/ReactorFluidTypesValue.java:5,10` | Imports inutilisés `HolderSet`, `Collections`. | 🟢 |
| `net/nuclearteam/createnuclear/CNSoundEvents.java:5` | Import `com.simibubi.create.AllSoundEvents` inutile (seuls les types imbriqués, importés séparément, sont utilisés). | 🟢 |
| `net/nuclearteam/createnuclear/CNParticleRegistry.java:4-5` | Imports `BlockParticleOption`/`ItemParticleOption` inutilisés. | 🟢 |
| `content/multiblock/bluePrintItem/ReactorBluePrintMenu.java:16`, `content/multiblock/output/ReactorOutput.java:35` | Imports wildcard `net.nuclearteam.createnuclear.*` masquant les dépendances réelles de la classe. | 🟢 |
| `content/multiblock/controller/snapshot/ReactorInputSnapshotBuilder.java:12` | Import `VirtualReactorInputsItem` devenu inutile après la suppression de la variable locale `virtualItems` (ex-champ mort §1.3, cf. §8) qui l'utilisait. | 🟢 |

### 1.6 Code commenté pouvant être supprimé

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/input/item/ReactorRodInputEntity.java:86-93` | Bloc commenté utilisant explicitement l'ancienne API de capacités Forge (`Capability<?>`, `ForgeCapabilities.ITEM_HANDLER`, `ResetableLazy<T>`) — reliquat direct jamais retiré après le passage aux capacités NeoForge. | 🟠 |
| `compat/jei/CreateNuclearJEI.java:145-148` | Bloc commenté pour générer des fluides de potion par `BottleType`, dupliqué juste après par le code actif qui ne gère que `REGULAR`. | 🟡 |
| `net/nuclearteam/createnuclear/CNFluids.java:114` | Ligne d'enregistrement `//.onRegister(ReactorFluidTypesValue.setReactorFluidTypeInfos(8196, 100))` commentée sur `LIQUID_NITROGEN`. | 🟢 |
| `content/uraniumOre/UraniumOreBlock.java:93-96` | Bloc XP entièrement commenté dans `spawnAfterBreak`. | 🟢 |
| `foundation/utility/RenderHelper.java:52` | `//graphics.pose().scale(coverage, coverage, 1f);` commenté : la branche `coverage != 1f` (l.48-57) devient strictement équivalente à la branche `coverage == 1f` (l.59-61), au `pushPose`/`translate` inutile près. | 🟢 |
| `content/multiblock/input/fluid/PlayerInteractReactorFluidInput.java:54-58` | Bloc commenté mort laissé dans le code actif (`//if (!fluidInItem.isEmpty()...)`). | 🟡 |
| `content/multiblock/input/fluid/PlayerInteractReactorFluidInput.java:66-68` | Bloc conditionnel `if (player.isCreative() && !onClient) { }` entièrement vide, sans effet. | 🟡 |
| `content/multiblock/controller/ReactorControllerBlock.java:105-107` | `if (!state.getValue(ASSEMBLED)) { }` : premier branchement vide sans effet (condition à inverser pour ne garder que le `else`). | 🟡 |
| `content/multiblock/controller/ReactorControllerBlock.java:134` | `//be.clearTimers(); // uncomment if the timer should reset when the reactor stops`. | 🟢 |
| `content/multiblock/controller/ReactorControllerBlock.java:191` | `//entity.removeIOAll();`. | 🟢 |
| `content/particles/NuclearMushroomCloudParticle.java:62,70` | `LOGGER.info("EXPLOSIOOOOOON")` (log de debug oublié) et ligne commentée `// playSound(CNSoundEvents.NUCLEAR_EXPLOSION_SHOCKWAVE...)`. | 🟡 / 🟢 |
| `foundation/events/overlay/HelmetOverlay.java:73` | `//Minecraft.getInstance().gui.renderItemHotbar(12f, graphics);`. | 🟢 |

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
| `content/multiblock/controller/manager/ReactorFrameDisplayManager.java:28-30` | Javadoc tronqué : *« On the client this reads the synced ; on the server it reads the aggregated. »* — les mots attendus après « synced » et « aggregated » manquent. | 🟡 |

### 2.4 Commentaires devenus obsolètes

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `net/nuclearteam/createnuclear/CNTags.java:32-46,52` | Méthodes `forgeTag`/`forgeBlockTag`/`forgeItemTag`/`forgeFluidTag` : nommage hérité de l'ère Forge alors qu'elles pointent en réalité vers `NEO_FORGE`, lui-même aliasé sur le namespace commun `"c"`. `FORGE("forge")` (l.52) est **confirmée inutilisée** : plus aucune référence dans le projet. | 🟠 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:195-197,233-237` | Le Javadoc *« Main constructor allowing dependency injection for testability and DIP compliance. »* est dupliqué : il documente en réalité `getMultiblockPos()` (l.195), alors que le vrai constructeur porte le même texte plus bas (l.233) — reliquat de copier-coller après refactorisation. | 🟠 |
| `content/multiblock/controller/snapshot/ReactorInputSnapshot.java:13-16` | Javadoc obsolète : décrit des champs `bigFuelItem`/`bigCoolerItem` qui n'existent plus dans le record actuel (`items`, `fluids`, `maxFluidCapacity`). | 🟠 |
| `api/ItemRodTypesValue.java:51-53` | Javadoc affirmant l'existence d'une valeur `MIXTE` dans `RodType.TypeRod` (« For MIXTE we keep the builder default... »), alors que cet enum ne définit que `FUEL`, `COOLER`, `NONE` (`api/multiblock/rods/RodType.java:312-324`) et que le `switch` associé lève une exception pour toute autre valeur. | 🟠 |
| `api/data/recipe/EnrichedRecipeGen.java:18` | Javadoc copié de Create : *« The base class for **Haunting** recipe generation »*, alors que la classe concerne les recettes « Enriched » (four à vent enrichissant), pas le système « Haunting » de Create. | 🟡 |

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
| `content/particles/SmallNuclearExplosionParticle.java:73-279` | 13 classes internes `*Factory` (`NukeFactory`, `MineFactory`, `UnderzealotFactory`, `RaygunFactory`, `BlueRaygunFactory`, `TremorzillaFactory`, `TremorzillaRetroFactory`, `TremorzillaTectonicFactory`, `AmberFactory`, `TotemFactory`, `PurpleWitchFactory`, `ConversionCrucibleFactory`, `FrostmintFactory`) structurellement identiques (même champ `spriteSet`, même constructeur, même corps de `createParticle`), ne différant que par des constantes passées en paramètre. | 🟠 |
| `content/multiblock/input/fluid/ReactorFluidInputEntity.java:168-227` | `fill()`, `drain(FluidStack,...)`, `drain(int,...)` de `FilteredFluidHandler` répètent 3× le même bloc « résoudre le contrôleur → si `ServerLevel` : `PersistentFluidLocks`, sinon : `FluidLockManager` ». | 🟠 |
| `content/multiblock/pattern/ReactorPattern.java:57-108` | `findController`, `findControllerPos(pos, level, first)` et `findControllerPos(pos, level)` dupliquent presque intégralement le même corps ; la seconde variante appelle même `isInReactorRange` deux fois de suite (l.75 et 84) sur le même résultat. | 🟠 |
| `content/multiblock/casing/ReactorCasing.java`, `content/multiblock/cooler/ReactorCooler.java`, `content/multiblock/frame/ReactorFrame.java`, `content/multiblock/output/ReactorOutput.java` | Les méthodes `onPlace`/`setPlacedBy`/`onRemove` reproduisent presque littéralement le même triptyque `super.xxx(...)` + résolution de pattern/contrôleur dans les 4 classes de blocs du multiblock. | 🟡 |
| `content/multiblock/input/item/ReactorRodInputGenerator.java`, `content/multiblock/input/fluid/ReactorFluidInputGenerator.java`, `content/multiblock/output/ReactorOutputGenerator.java` | Trois `SpecialBlockStateGen` quasi identiques (mêmes calculs `getXRotation`/`getYRotation`, même schéma de nom de modèle `..._vertical`), ne différant que par le bloc/modèle ciblé. | 🟡 |
| `content/redstone/displayLink/source/CoolerDisplaySource.java` et `FuelDisplaySource.java` | Même structure (`getLabelKey`, `getMax`, `getColor`, boucle de comptage sur `getDisplayState().items()`), ne différant que par le prédicat `RodType.TypeRodPredicate` et la couleur. La même boucle de comptage est dupliquée une 3ᵉ fois dans `ReactorSummaryDisplaySource.getReactorSummary` (l.164-172). | 🟡 |
| `content/kinetics/fan/processing/EnrichedRecipe.java` et `SnowPowderRecipe.java` | Classes quasi identiques (`matches`, `getMaxInputCount`, `getMaxOutputCount`), ne différant que par le `RecipeType` passé au constructeur parent. | 🟡 |
| `compat/jei/category/FanEnrichedCategory.java` et `FanSnowPowderCategory.java` | Même structure (`getBlockShadow`, `renderAttachedBlock`, `getTitle`), ne différant que par le bloc rendu et le titre. | 🟡 |
| `content/kinetics/fan/processing/CNFanProcessingTypes.java` | `EnrichedType` et `SnowPowderType` dupliquent presque intégralement `canProcess`/`process`/`spawnProcessingParticles`. | 🟡 |
| `infrastructure/worldgen/biome/CNDensityFunctions.java:22-31` | `Irradiated.EROSION` et `Irradiated.FINAL_DENSITY` enregistrées avec exactement la même expression `DensityFunctions.add(DensityFunctions.yClampedGradient(0, 90, 1, -1), BlendedNoise.createUnseeded(0.25, 0.375, 80.0, 160.0, 8.0))` dupliquée littéralement. | 🟡 |
| `net/nuclearteam/createnuclear/CNTags.java:72-300` | Les 5 enums imbriquées (`CNBlockTags`, `CNItemTags`, `CNFluidTags`, `CNEntityTags`, `CNRecipeSerializerTags`) dupliquent exactement la même mécanique de constructeurs en cascade et une méthode `init()` vide (~230 lignes de duplication structurelle, indépendante de Forge/NeoForge). | 🟡 |
| `net/nuclearteam/createnuclear/CNBlocks.java:383-569` | Les blocs de minerai (uranium, lead, thorium, nitrate, variantes deepslate) répètent un schéma quasi identique (`initialProperties`, `loot` avec `createSilkTouchDispatchTable`/`applyExplosionDecay`, mêmes tags). | 🟡 |
| `net/nuclearteam/createnuclear/CNItems.java:222-379` | Les 4 entrées d'armure anti-radiation (casque/plastron/jambières/bottes) répètent presque à l'identique le bloc de recette + boucle `for (Cloths cloth : Cloths.values())` générant les recettes de smithing par teinte (~150 lignes). | 🟡 |
| `lib/multiblock/SimpleMultiBlockPattern.java:31-40,42-53` | `matches(...)` et `matchesWithResult(...)` dupliquent presque intégralement la même boucle de résolution ; `matches` pourrait être réécrit comme `matchesWithResult(...) != null`. | 🟡 |
| `content/multiblock/bluePrintItem/ReactorBluePrintMenu.java:166-175` et `content/multiblock/input/item/ReactorRodInputMenu.java:113-125` | Deux implémentations différentes du même besoin (rediriger `ClickType.THROW` vers `PICKUP` pour certains slots), l'une par plage d'index, l'autre par tableau codé en dur. | 🟢 |
| `foundation/data/recipe/CNCrushingRecipeGen.java:84-88` et `CNWashingRecipeGen.java:36-40` | Même motif de surcharge `create(Supplier<ItemLike>, UnaryOperator<...>)` redirigeant vers `create(CreateNuclear.MOD_ID, ...)`, répété à l'identique dans deux générateurs distincts. | 🟢 |
| `net/nuclearteam/createnuclear/CNItems.java:61-219` | Le trio « ingot/nugget cru → recette de décompactage depuis storage block » répète le même schéma `ShapelessRecipeBuilder...requires(...).save(...)` pour 8 items. | 🟢 |
| `api/multiblock/fluid/ReactorFluidType.java:80-92` et `api/multiblock/rods/RodType.java:93-105` | `resolveReactorFluidType`/`resolveRodType` suivent le même patron à 3 étapes (lookup registre → fallback `*Value` → fallback registre `FALLBACK_*`), dupliqué terme à terme. | 🟢 |
| `content/multiblock/controller/manager/ReactorOutputManager.java:102-111` | Dans `rotateOutputs`, les branches `if`/`else` dupliquent `entity.updateSpeed = true; entity.updateGeneratedRotation();`. | 🟢 |
| `content/contraptions/irradiated/{cat,chicken,cow,wolf}/*` | Duplication de structure attendue pour un portage vanilla, mais incohérence notable : seul `IrradiatedChicken` implémente `IrradiatedAnimal` (conversion), alors qu'`AnimalUtil.blockTamingWip` est utilisé par Cat et Wolf, suggérant une fonctionnalité de conversion partiellement portée sur un seul des 4 animaux — à vérifier si voulu. | 🟡 |

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
| `content/kinetics/fan/processing/CNFanProcessingTypes.java:39-47` | `LEGACY_NAME_MAP` : shim de compatibilité de noms lié à d'anciennes sauvegardes/NBT pré-migration. `ofLegacyName`/`parseLegacy`, ses seuls lecteurs, ont été supprimés (aucun appelant) ; le champ et son bloc d'initialisation statique sont donc désormais eux aussi orphelins — soit câbler `LEGACY_NAME_MAP` là où les NBT legacy sont lus, soit le supprimer avec le champ. | 🟡 |
| `content/contraptions/irradiated/cat/IrradiatedCatRenderer.java:5`, `wolf/IrradiatedWolf.java:3`, `foundation/block/HorizontalDirectionalReactorBlock.java:3`, `MultiDirectionalReactorBlock.java:3` | Import `com.mojang.math.MethodsReturnNonnullByDefault` au lieu de `net.minecraft.MethodsReturnNonnullByDefault` (utilisé partout ailleurs) — incohérence probablement issue d'un auto-import IDE pendant le portage. | 🟡 |
| `foundation/data/recipe/CNCrushingRecipeGen.java:42-56` | Différences de contenu de recette apparues pendant le portage, à trancher (voulu ou régression) : (1) nouvelle recette `RAW_URANIUM_BLOCK` absente côté Forge ; (2) `RAW_THORIUM_BLOCK` : la sortie secondaire `0.75f ×AllItems.EXP_NUGGET` (Forge) a été remplacée par `0.5f ×CNItems.THORIUM_DUST×72` ; (3) `RAW_THORIUM_ITEM` : même changement, `0.75f×EXP_NUGGET` → `0.5f×THORIUM_DUST×8`. Les recettes de fer/or (l.61,68) ont bien gardé leur `EXP_NUGGET`, ce qui rend l'écart d'autant plus visible. | 🟡 |
| `content/compat/alexscave/AlexscaveCompat.java:14-66` | Compat entièrement gelée en code Forge commenté (`MobSpawn`, `NukeParam`, `UpdateACProxy`, plus les méthodes entièrement commentées `isRaycat`, `isTremorzilla`, `ACResConfig`, `ACDestroyable`, `GetACSounds`, `GetACConfig`) ; le commentaire de classe indique explicitement que le mod tiers n'a pas encore de version 1.21.1. Coquille vide en attente côté NeoForge. | 🟠 |
| `content/multiblock/frame/ReactorFrameRenderer.java:74-77` | `CatnipServices.FLUID_RENDERER` est déclaré `FluidRenderHelper<?>`, obligeant un cast non vérifié (`@SuppressWarnings("unchecked")`) vers `FluidRenderHelper<FluidStack>` de NeoForge : shim multiplateforme (Catnip/Create, Forge vs NeoForge) laissé tel quel. | 🟡 |

---

## 5. Nettoyage

Éléments à retirer une fois la migration complètement terminée et les points de la section 4 tranchés.

- Retirer `content/multiblock/input/item/ReactorRodInputEntity.java:86-93` (bloc de capacités Forge commenté).
- Renommer `forgeEventBus` → `neoForgeEventBus` dans `CreateNuclear.java:70`, retirer la ligne `DistExecutor` commentée (l.110), retirer `neoEventBus` inutilisé dans `CreateNuclearClient.java:20`.
- Une fois le nommage `CNTags.forgeXxxTag` validé comme voulu ou non, renommer en `commonXxxTag` ; supprimer dans tous les cas l'entrée `FORGE("forge")` (l.52), confirmée sans référence.
- Supprimer ou câbler `CNFanProcessingTypes.LEGACY_NAME_MAP` (`ofLegacyName`/`parseLegacy`, ses seuls lecteurs, ont déjà été supprimés — le champ est désormais orphelin).
- Retirer la branche `coverage != 1f` (l.39-48 de `RenderHelper.renderOverlay`) rendue équivalente au cas `coverage == 1f` par le `scale` commenté.
- Nettoyer les imports/blocs commentés listés en §1.5/§1.6 (`UraniumOreBlock`, `CreateNuclearJEI`, `CNFluids`, `ReactorInputSnapshotBuilder`).
- Supprimer `content/compat/alexscave/AlexscaveCompat.java` (ou le réécrire proprement) une fois qu'Alex's Caves publie une version 1.21.1 compatible et qu'une vraie intégration est décidée.
- Retirer le champ mort `countCoolerRod` de `ReactorControllerBlockEntity` (ou l'implémenter réellement) — attention : `@SuppressWarnings({"unused"})` sur la classe masque ce type de code mort, à retirer une fois le nettoyage fait pour que l'IDE le détecte à nouveau.
- Corriger l'incohérence de paquet `foundation/damageTypes/CNDamageSources.java` : le dossier est `damageTypes` mais le fichier déclare `package ...foundation.damagesTypes;` (avec un « s » superflu). Compile aujourd'hui car les deux importeurs (`RadiationEffect.java`, `CNFanProcessingTypes.java`) utilisent la même faute, mais cassera tout futur refactor IDE automatique.

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
- **`SmallNuclearExplosionParticle`** : factoriser les 13 classes `*Factory` en une seule fabrique paramétrée par couleur/lifetime/scale/fadeColor.
- **`ReactorPattern`** : réduire les 3 méthodes de scan (`findController`, `findControllerPos` ×2) à une seule méthode paramétrée par un visiteur.
- **Blocs multiblock (`ReactorCasing`/`ReactorCooler`/`ReactorFrame`/`ReactorOutput`)** : extraire un comportement par défaut commun pour `onPlace`/`setPlacedBy`/`onRemove`.
- **`CNTags`** : factoriser les 5 enums de tags autour d'une classe/interface générique commune (registre + construction de `ResourceLocation` + `alwaysDatagen`).
- **`CNBlocks`** : extraire une méthode factory générique `registerOre(name, rawItemEntry, dropRange, fortuneBonus, deepslate?)` pour les 8 blocs de minerai.
- **`CNItems`** : extraire une méthode commune pour la génération des 4 pièces d'armure anti-radiation (recette de craft + boucle de recettes de smithing par teinte).
- **`CoolerDisplaySource`/`FuelDisplaySource`** (+ boucle équivalente dans `ReactorSummaryDisplaySource`) : factoriser autour d'un comptage générique paramétré par le prédicat `TypeRodPredicate`.
- **`EnrichedRecipe`/`SnowPowderRecipe`** et **`FanEnrichedCategory`/`FanSnowPowderCategory`** : factoriser autour d'une base commune paramétrée par `RecipeType`/bloc affiché/titre.
- **`lib/multiblock`** : fusionner `matches`/`matchesWithResult` dans `SimpleMultiBlockPattern`, et simplifier/retirer l'abstraction `IPatternBuilder` jamais exploitée pour son but (seul `SimpleMultiBlockPattern::new` est jamais fourni comme builder).
- **`CNDensityFunctions`** : factoriser la fonction de densité dupliquée entre `EROSION` et `FINAL_DENSITY`, après confirmation que ce n'était pas un placeholder intentionnel pour deux fonctions distinctes à terme.
- **Faire passer la consommation de fluide par le système de timer `IConsumable`** (si repris un jour — `FluidConsumable.java`, resté à l'état de placeholder inachevé, a été supprimé le 30/08/2026 ; le cas `"fluid"` de `IConsumable.deserializeNBT` est commenté en attendant une éventuelle reprise, cf. §8). État confirmé au 30/08/2026 : `FluidConsumptionRateCalculator.tick()` → `ReactorInputFluidManagerI.extractFluids(...)` effectue une **vraie extraction** (`handler.drain(toExtract, FluidAction.EXECUTE)`, pas une simple vérification de présence), via un modèle de **taux continu accumulé** (efficacité du fluide / taille du réacteur / `heatService.getLiquidTimer()`, buffer fractionnaire), totalement séparé du `ConsumptionCycleManager` des rods (modèle **discret** : timer qui expire puis consomme un bloc fixe). Pour unifier sous `IConsumable`/`ConsumableTimer` :
  1. **Élargir le contrat `IConsumable`** — `consume()` ne reçoit aujourd'hui que `ReactorInputManagerI` (manager d'items) ; un `FluidConsumable` a besoin du `ReactorInputFluidManagerI` pour appeler `extractFluids(...)`, donc soit ajouter ce second manager à la signature (impacte aussi `ItemConsumable`), soit l'injecter directement au consumable à sa construction.
  2. **Créer un équivalent de `PatternReader` pour le fluide** — `PatternReader` ne lit que la grille d'items du blueprint ; il n'existe aucune lecture équivalente côté `ReactorFluidInputEntity`/`inputFluidManager` (type de fluide présent, capacité) pour construire un `FluidConsumable` avec les bons `fluidName`/`mbPerCycle`.
  3. **Reformuler `computeTimer`** — remplacer le modèle de taux continu de `FluidConsumptionRateCalculator` (efficacité/`liquidTimer`/taille) par une durée en ticks compatible avec `ConsumableTimer`, ou assumer une sémantique de consommation moins fine que le buffer fractionnaire actuel.
  4. **Câbler `consume()`** — appel réel à `inputFluidManager.extractFluids(level, mbPerCycle)` au lieu du `return false;` d'origine.
  5. **Décider du sort de `FluidConsumptionRateCalculator`** — le supprimer si le fluide passe par le timer, ou garder les deux en s'assurant qu'ils ne s'exécutent jamais sur le même tick (sinon double extraction).
  6. **Brancher l'appel** dans `ReactorControllerBlockEntity.tick()` — soit un second `cycleManager` dédié au fluide, soit étendre `ConsumptionCycleManager`/`PatternReader` pour produire indifféremment des `ItemConsumable` et des `FluidConsumable`.

  Points bloquants principaux : **2** (aucune lecture de pattern côté fluide aujourd'hui) et **3** (les deux modèles de consommation — discret vs continu — ne sont pas directement compatibles).

  À noter : `ReactorControllerBlockEntity.java:71` déclare `private double liquidLife;` juste à côté du champ `cycleManager` (l.70), ni lu ni écrit ailleurs dans le fichier. Vu son emplacement et son nom, c'est vraisemblablement un reliquat/placeholder posé en prévision de cette même intégration fluide (un accumulateur de durée de vie de fluide, pendant du `remainingTicks` de `ConsumableTimer`) plutôt qu'un oubli isolé — à traiter avec le reste de ce chantier plutôt qu'à supprimer isolément.

---

## 7. Tableau de priorités global

### 🔴 Critique


### 🟠 Important

- Débris de migration à finaliser : `CreateNuclear.forgeEventBus` + ligne `DistExecutor` commentée, `CNTags.forgeXxxTag`/`FORGE`, capacités Forge commentées dans `ReactorRodInputEntity`, notes d'incertitude non tranchées dans `ReactorFluidInputEntity`, compat `AlexscaveCompat` entièrement gelée en code Forge commenté.
- Commentaires français masquant une incertitude technique : `ReactorFluidInputEntity`.
- Incohérence de paquet `foundation/damageTypes` (dossier) vs `foundation.damagesTypes` (package déclaré) dans `CNDamageSources.java`.
- `RenderHelper.renderOverlay` : mise à l'échelle `coverage` désactivée par une ligne commentée, effet visuel perdu silencieusement.
- Duplications significatives : `getBlocksPosition(Level)` ×4 managers (+ shadowing), `HorizontalDirectionalReactorBlock`/`MultiDirectionalReactorBlock`, `CNItems` decompacting ×9, `RadiationCapability.computeItemRadiation(Player)`, 13 classes `*Factory` de `SmallNuclearExplosionParticle`, triple duplication de verrou fluide dans `ReactorFluidInputEntity`, triple duplication de scan dans `ReactorPattern`.
- Javadoc trompeur/obsolète : copier-coller mal placé dans `ReactorControllerBlockEntity` (constructeur), champs disparus documentés dans `ReactorInputSnapshot`, valeur d'enum `MIXTE` inexistante dans `ItemRodTypesValue`.

### 🟡 Moyen

- Duplications : `HorizontalDirectionalReactorBlock`/`MultiDirectionalReactorBlock` (déjà listé ci-dessus), blocs multiblock `onPlace`/`onRemove`, générateurs `SpecialBlockStateGen` ×3, `CoolerDisplaySource`/`FuelDisplaySource`/`ReactorSummaryDisplaySource`, `EnrichedRecipe`/`SnowPowderRecipe` + catégories JEI, `CNTags` (5 enums ~230 lignes), `CNBlocks` (blocs de minerai), `CNItems` (armures anti-radiation), `CNDensityFunctions` (expression dupliquée), `matches`/`matchesWithResult` dans `SimpleMultiBlockPattern`.
- Dead code : champ orphelin `CNFanProcessingTypes.LEGACY_NAME_MAP`, `countCoolerRod` write-only, abstraction `IPatternBuilder` jamais exploitée.
- Javadoc mal placée après `@Override` dans `ReactorInputFluidManager` (6 méthodes) ; Javadoc française mal formée `ReactorAlarmManagerI` ; Javadoc tronqué `ReactorFrameDisplayManager`; Javadoc copié de Create dans `EnrichedRecipeGen`.
- Blocs commentés secondaires : `CreateNuclearJEI` (`BottleType`), `PlayerInteractReactorFluidInput` (×2), `ReactorControllerBlock` (branche vide + 2 lignes commentées).
- Imports `com.mojang.math.MethodsReturnNonnullByDefault` (4 fichiers) ; `CreateNuclearClient.neoEventBus` ; incertitude `ReactorFluidInput.java:91` ; shim `FluidRenderHelper<?>` cast non vérifié dans `ReactorFrameRenderer`.
- Divergences de recettes `CNCrushingRecipeGen` à trancher.
- Incohérence de portage entre les 4 animaux irradiés (seul `IrradiatedChicken` implémente `IrradiatedAnimal`).
- Log de debug oublié `"EXPLOSIOOOOOON"` dans `NuclearMushroomCloudParticle`.

### 🟢 Faible

- Imports et blocs commentés isolés (`UraniumOreBlock`, `CNFluids`, `RenderHelper`, imports dupliqués/inutilisés dans `lib/multiblock`, `ItemRodTypesValue`/`ReactorFluidTypesValue`, `CNSoundEvents`, `CNParticleRegistry`, imports wildcard).
- Commentaires français restants sans impact joueur (`ReactorInputManager`, `ReactorAlarmManager:47`, display sources, `CNDisplaySources`, `CNPonderIndex`, `ReactorBluePrintItemScreen`, `ReactorControllerBlockEntity:90`, `RadiationCapability.radiation_desactive`) et chemins de sons `"reacteur/..."`.
- Javadoc mal placée (`MultiblockHelpers`, `CNRodTypes`), commentaires paraphrasant le code (`RadiationEffect`), note `@goshante` dans `CreateNuclearJEI`.
- Duplications mineures : NBT des managers, verrous de fluide, builders `RodType`/`ReactorFluidType`, `isFood` poulet/loup, textures `WOLF_LOCATION`/`WOLF_TAME_LOCATION`, `rotateOutputs` if/else, générateurs de recettes `create(...)` (Crushing/Washing), trio ingot/nugget `CNItems`, `resolveReactorFluidType`/`resolveRodType`, menus `clicked()` (BluePrint/RodInput).

---

## 8. Historique des corrections

Points listés dans une version antérieure de cet audit, corrigés depuis et retirés des sections ci-dessus.

| Ex-# | Fichier:ligne | Problème (tel qu'audité) | Correction | Date |
|---|---|---|---|---|
| B3 | `content/enriching/campfire/EnrichingCampfireBlock.java:118-120` | `protected MapCodec<? extends BaseEntityBlock> codec()` renvoyait `return null;` au lieu du `CODEC` construit juste au-dessus (l.30-35) via `RecordCodecBuilder` : le codec du bloc était donc systématiquement `null` en jeu. | Remplacé `return null;` par `return CODEC;`. | 30/08/2026 |
| B2 | `content/radiation/capability/RadiationCapability.java:210-212` | Dans `applyEffects`, la branche `else if (< radiationLevel3)` et la branche `else` finale renvoyaient toutes deux `amplifierLevel2.get()` : redondantes, l'amplificateur plafonnait au niveau 2 faute de palier au-delà de `radiationLevel3`. | Ajout d'un 4e palier : `CRadiation.java` expose désormais `amplifierLevel3` (config `amplifier_level_3`, "Effect amplifier for Radiation IV"), et la branche `else` (l.212) l'utilise au lieu de dupliquer `amplifierLevel2`. | 30/08/2026 |
| B4 | `foundation/data/recipe/CNStandardRecipeGen.java:305-315` | Dans `viaShapeless(...)`, `RecipeOutput conditionalOutput = recipeOutput.withConditions(...)` (l.313) était calculé puis jamais utilisé : `b.save(...)` (l.315) sauvegardait via `recipeOutput` et non `conditionalOutput`, ignorant silencieusement les `recipeConditions` (`whenModLoaded`/`whenModMissing`). | Remplacé `b.save(recipeOutput, ...)` par `b.save(conditionalOutput, ...)`, alignant `viaShapeless` sur le motif déjà correct de `CNStandardRecipeGen.java:419-420`. | 30/08/2026 |
| B5 | `content/contraptions/irradiated/wolf/IrradiatedWolfModel.java:138-145` | `headParts()` et `bodyParts()` renvoyaient `null` au lieu d'une `Iterable<ModelPart>` vide : risque de `NullPointerException` si ces hooks d'`AgeableListModel` sont itérés par le moteur de rendu vanilla (mise à l'échelle des bébés). | `headParts()` renvoie désormais `List.of(head)` et `bodyParts()` renvoie `List.of(body, mane, leg1, leg2, leg3, leg4, tail)`, conformément aux parties rendues dans `renderToBuffer`. | 30/08/2026 |
| B6 | `foundation/ponder/CNPonderIndex.java:18,21` (et 24,27) | `t1` et `ioPlacement` étaient enregistrés sous le même id `reactor/reactor_t1_ponder`, avec une suspicion que le second storyboard écrase le premier dans le registre Ponder. **Testé en jeu : les deux storyboards s'affichent bien séparément**, l'écrasement redouté n'a pas lieu (l'API Ponder ne se comporte donc pas comme supposé) — mais l'id partagé restait ambigu. | `ioPlacement` enregistré sous son propre id `reactor/reactor_io_ponder` (sur les deux composants `REACTOR_CONTROLLER` et `REACTOR_BLUEPRINT`), pour lever l'ambiguïté même si aucune régression fonctionnelle n'était constatée. | 30/08/2026 |
| — | `foundation/utility/Maths.java` | Fichier entier issu d'une décompilation FernFlower (source originale perdue), dont seuls `smin` et `sampleNoise3D(float,float,float,float)` avaient un appelant réel (`NuclearExplosionEntity.java:157-158`, `NuclearMushroomCloudParticle.java:115`). Tout le reste (`sampleNoise2D`, `buildShape`, `walkValue`, `approachRotation`, `getGroundBelowPosition`, `readVec3`, `writeVec3`, `approachDegreesNoWrap`, `canyonStep`, `getBiomesWithinAtY`, `sampleNoise3D(int,int,int,float)`, `HORIZONTAL_DIRECTIONS`, `NOT_UP_DIRECTIONS`, `HALF_SQRT_3`, `QUARTER_PI`) était du code mort décompilé sans source récupérable. | Fichier réécrit pour ne garder que `smin` et `sampleNoise3D(float,float,float,float)` ; aucun appelant restant dans le dépôt ne référence les méthodes/champs supprimés (vérifié par recherche globale). | 30/08/2026 |
| — | `net/nuclearteam/createnuclear/CNPackets.java` | Enum sans aucune constante (corps vide) ; aucune classe du projet n'implémente `BasePacketPayload`. `register()` et la boucle `for (CNPackets packet : CNPackets.values())` sont donc des no-op complets. | **Décision du mainteneur : conservé tel quel**, sans correction ni suppression — le fichier reste un no-op assumé, prévu pour centraliser de futurs payloads réseau plutôt qu'à retirer maintenant. | 30/08/2026 |
| — | `content/explosion/CNBasicModelPart.java` | Toute la machinerie de construction de cube (classes internes `ModelBox`, `PositionTextureVertex`, `TexturedQuad`, 7 surcharges `addBox`, ainsi que `doRender`/`getRandomCube`/`copyModelAngles`/`getModelAngleCopy` et le constructeur privé sans arguments qui en dépendaient) n'avait aucun appelant réel : seul `CNAdvancedModelBox` est jamais instancié dans le dépôt, et il définit ses propres surcharges `addBox`/`render` en s'appuyant sur `CNTabulaModelRenderUtils`, sans jamais appeler celles de la classe de base. | Fichier réécrit pour ne garder que les membres réellement hérités et utilisés par `CNAdvancedModelBox`/`NuclearMushroomCloudModel` (constructeurs, `addChild`, `setTextureOffset`, `setRotationPoint`, `setTextureSize`, `render`/`translateRotate`) ; vérifié qu'aucun appelant du dépôt ne référence les classes/méthodes supprimées. | 30/08/2026 |
| — | `content/multiblock/controller/consumable/FluidConsumable.java` | Placeholder inachevé du plan de refactor `IConsumable`/`ConsumableTimer` (`m.md`) : jamais instancié hors de son propre `deserializeNBT`, `consume()` renvoyait `false` en dur, aucun code n'écrivait le tag `"type":"fluid"` correspondant. Le mécanisme réel de refroidissement passe par `FluidConsumptionRateCalculator`/`ReactorInputFluidManagerI.extractFluids(...)` (vraie extraction via `drain(..., FluidAction.EXECUTE)`, modèle de taux continu). | **Supprimé** par le mainteneur ; le cas `"fluid"` de `IConsumable.deserializeNBT` est commenté (pas retiré) en attendant une éventuelle reprise. Si le sujet est repris, la procédure pour brancher un futur `FluidConsumable` sur le système de timer est documentée en §6 (Refactorisations). | 30/08/2026 |
| — | `lib/multiblock/impl/IMultiBlockPattern.java:34-36` | Méthode par défaut `contruct(Level, BlockPos)` (faute de frappe pour `construct`) jamais appelée, doublon de `construct(level, pos, (a,b) -> true)`. | Supprimée (commit `3de660a`). | 02/09/2026 |
| — | `foundation/advancement/CNAdvancementBehaviour.java:108-112` | `tryAward(BlockGetter, BlockPos, CreateAdvancement)` sans appelant ; délèguait en plus vers `AdvancementBehaviour.TYPE` de Create au lieu de `CNAdvancementBehaviour.TYPE`. | Supprimée (commit `3de660a`). | 02/09/2026 |
| — | `foundation/utility/CreateNuclearLang.java:68-70` | `temporaryText(String)`, `@Deprecated`, aucun appelant. | Supprimée (méthode absente du fichier actuel ; le commit `3de660a` n'en documente que le nettoyage cosmétique, mais la méthode elle-même a disparu au plus tard à ce commit). | 02/09/2026 |
| — | `foundation/utility/TextUtils.java:45-75,77,116-119` | `renderMultilineDebugText`, `renderDebugText`, `translateWithFormatting`, `leftPad` : aucun appelant. | Supprimées avec les imports qui ne servaient qu'à elles (commit `3de660a`). | 02/09/2026 |
| — | `content/multiblock/alarm/ReactorAlarmEntity.java:21,98-100` | Champ public `controller` jamais lu, et méthode `setController(...)` jamais appelée. | Champ et méthode tous deux retirés du fichier. | 02/09/2026 |
| — | `content/kinetics/fan/processing/CNFanProcessingTypes.java:55-65` | `ofLegacyName(String)`/`parseLegacy(String)` : aucun appelant hors de `parseLegacy` qui appelait `ofLegacyName`. | Les deux méthodes ont été supprimées ; le champ `LEGACY_NAME_MAP` qu'elles lisaient reste en revanche présent et est désormais lui-même orphelin (cf. §4, mis à jour). | 02/09/2026 |
| — | `lib/multiblock/SimpleMultiBlockAislePatternBuilder.java:83-87` | `getDistanceController(char)` sans appelant ; réutilisait `Util.parseBlockPattern`, qui mute la liste `aisles` passée en paramètre — risque de double inversion si réactivée telle quelle. | Méthode supprimée. | 02/09/2026 |
| — | `content/multiblock/core/ReactorCoreEntity.java:11-23` | `tick()` ne faisait qu'un early-return conditionnel ; `countdownTicks`/`hasExploded` n'étaient jamais réellement pilotés — logique d'explosion du cœur inachevée. | L'override `tick()` et les deux champs ont été retirés ; `ReactorCoreEntity` ne fait plus qu'hériter du `tick()` de `ReactorCasingEntity` (aucune logique d'explosion propre pour l'instant, plutôt qu'une logique à moitié écrite). | 02/09/2026 |
| — | `foundation/advancement/CNAdvancement.java:54` | `public static final CreateNuclearAdvancement START = null,` — première entrée nulle de la déclaration groupée, jamais référencée en tant que valeur. | **Voulu, pas un bug** : `START` est un marqueur de bornage lisible pour repérer le début de la longue déclaration groupée. Un `END = null` symétrique a été ajouté en toute fin de la même déclaration (après `REACTOR_FRAME`) pour marquer la fin du groupe. | 02/09/2026 |
| — | `foundation/utility/RenderHelper.java:13-15,36-38` | `lastAlpha`, `lastCoverage`, `lastFirstPerson` : champs de « cache » assignés à chaque appel mais jamais relus. | Champs supprimés. | 02/09/2026 |
| — | `net/nuclearteam/createnuclear/CNRecipeTypes.java:45,61,71,76` | Champ `isProcessingRecipe` assigné à 3 endroits mais jamais lu. | Supprimé. | 02/09/2026 |
| — | `content/effects/VicinityEffect.java:22` | Paramètre constructeur `Consumer<Integer> timer` jamais stocké ni utilisé. | Paramètre supprimé du constructeur. | 02/09/2026 |
| — | `content/multiblock/IHeat.java:28,37-41` | Champ `intColor` et son constructeur `HeatLevel(int, int)` jamais utilisés. | Supprimés. | 02/09/2026 |
| — | `content/contraptions/irradiated/cat/IrradiatedCat.java:525-526` | Variable locale décompilée `var10006` dans `CatAvoidEntityGoal`, jamais utilisée. | Supprimée. | 02/09/2026 |
| — | `content/multiblock/controller/snapshot/ReactorInputSnapshotBuilder.java:41` | `VirtualReactorInputsItem virtualItems = inputManager.getInventory(level);` calculée puis jamais utilisée. | Variable supprimée ; l'import `VirtualReactorInputsItem` devenu orphelin par ce retrait est resté et a été ajouté en §1.5 (Imports inutiles). | 02/09/2026 |
| — | `content/multiblock/controller/ReactorControllerBlockEntity.java:69` | `private final ReactorPattern pattern = new ReactorPattern();` instancié à chaque bloc-entité mais jamais lu ni utilisé ailleurs dans la classe. | Champ (et son import `ReactorPattern`) supprimés. | 02/09/2026 |
| — | `content/multiblock/output/ReactorOutputEntity.java:67,77-79,90-92` | `outputPos` n'était jamais assigné ailleurs que dans `read()` (pas de setter, pas d'autre usage) : toujours `null` en pratique bien que lu/écrit en NBT. | Champ retiré, ainsi que sa lecture/écriture NBT dans `read()`/`write()`. | 02/09/2026 |
| — | `net/nuclearteam/createnuclear/api/multiblock/MultiBlockManagerBeta.java:16` | Constructeur vide `public MultiBlockManagerBeta() {}` redondant. | Supprimé. | 02/09/2026 |

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

## Notes de méthode

Chaque point ci-dessus a été **re-vérifié ligne à ligne** contre l'état actuel de la branche `V2-Audit` (commit `d2c7078`) le 23/08/2026 : existence du fichier, présence effective du code incriminé, et recherche d'appelants pour tout ce qui est annoncé comme mort. Les points de l'audit initial dont le code a disparu ou été corrigé ont été retirés du document. Aucun point n'est extrapolé au-delà du code effectivement lu ; les usages Forge fonctionnels et non signalés comme temporaires ne sont pas remontés comme des problèmes.

La passe du 23/08/2026 (après-midi) a complété le document par une seconde lecture intégrale des 300 fichiers de `src/main/java` (répartie par sous-système : Multiblock/Réacteur, Contenu hors multiblock, Fondations/Infrastructure/API, Registres racine), avec vérification directe (lecture de fichier + recherche d'appelants) de chaque nouveau point avant intégration, en particulier les points 🔴/🟠 nouvellement ajoutés (B3 à B6, `Maths.java`, `FluidConsumable`, champs morts de `ReactorControllerBlockEntity`, incohérence de paquet `damageTypes`/`damagesTypes`).
