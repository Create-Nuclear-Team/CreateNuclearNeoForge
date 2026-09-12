# Audit de code — Migration Forge → NeoForge (CreateNuclearNeoForge)

Document de suivi vivant. **Ne contient que les points encore ouverts.**
Dernière re-vérification intégrale contre le code : **23/08/2026** (branche `V2-Audit`, commit `d2c7078`), complétée par des **re-vérifications incrémentales** le 06/09/2026 (commit `5fa0cb7`, 11 commits) et le 11/09/2026 (commits `7c27b91`→`cd82e97`), puis par une **seconde passe intégrale le 11/09/2026** couvrant l'intégralité de `src/main/java` hors `content/multiblock` (déjà repassé en revue lors des passes précédentes) — répartie par sous-système (compat/foundation/infrastructure/gametest/impl/lib, contenu hors multiblock, registres racine/api), chaque fichier lu en entier avec recherche d'appelants pour toute affirmation de code mort.
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
| B8 | `content/contraptions/irradiated/cat/IrradiatedCat.java:283-292` | `finalizeSpawn(ServerLevelAccessor, DifficultyInstance, MobSpawnType, SpawnGroupData, CompoundTag)` déclare un 5ᵉ paramètre `CompoundTag dataTag` qui n'existe plus dans la signature réelle de `Mob#finalizeSpawn` en 1.21.1 (confirmé par comparaison avec `IrradiatedWolf.java:137`, qui utilise la bonne signature à 4 paramètres). Sans `@Override` et avec une signature qui ne correspond à aucune méthode parente, cette surcharge n'est **jamais appelée par le moteur de spawn** : la vérification de structure « chat noir » (`StructureTags.CATS_SPAWN_AS_BLACK` → `setPersistenceRequired()`) ne s'exécute donc jamais en jeu. Résidu de portage (signature d'une ancienne version de Minecraft/Forge jamais mise à jour). | 🟠 |
| B9 | `content/effects/VicinityEffect.java:20,44-49` | `cooldowns` (`HashMap<UUID, Long>`) accumule une entrée par entité jamais purgée (pas de retrait à la mort/déchargement de l'entité, pas de nettoyage périodique). `MobEffect` étant un singleton à durée de vie du serveur, cette map grossit sans limite sur un serveur longue durée. Pas de crash immédiat, mais fuite mémoire non bornée réelle. | 🟠 |

---

## 1. Dead Code

### 1.1 Classes inutilisées / entièrement mortes

| Fichier | Détail | Priorité |
|---|---|---|
| `infrastructure/config/CExplode.java` | Classe de configuration entière (`size`, `type`, `time`) jamais imbriquée dans `CNCServer`/`CNCClient`/`CNCCommon` ni enregistrée nulle part — confirmé sans aucune référence hors du fichier lui-même. | 🟡 |

### 1.2 Méthodes inutilisées

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `compat/jei/CreateNuclearJEI.java:158-208` | `consumeAllRecipes`, `consumeTypedRecipes`, `getTypedRecipes`, `getTypedRecipesExcluding`, `doInputsMatch`, `doOutputsMatch` n'ont aucun appelant hors d'eux-mêmes ; le champ statique `runtime` (l.67/212) est write-only. Masqué par `@SuppressWarnings("unused")` sur la classe. | 🟡 |
| `foundation/data/recipe/CNStandardRecipeGen.java` | `createSpecial` (l.122), `blastCrushedMetal` (l.131), `recycleGlass` (l.138), `recycleGlassPane` (l.146), `conversionCycle` (l.194), `clearData` (l.206), `whenModLoaded`/`whenModMissing` (l.270-276, donc `recipeConditions` n'est jamais peuplé), `viaNetheriteSmithing` (l.312), `inSmoker()`/`inSmoker(builder)` (l.380-388) : aucun appelant projet-large. Masqué par `@SuppressWarnings("unused")` sur la classe. | 🟡 |
| `content/equipment/armor/CNArmorMaterials.java:71-75` | `durabilityForType(Type)` sans aucun appelant (tous les appels réels passent par `setArmorDurability`) — distinct du point déjà tracké sur le tableau `BASE_DURABILITY` recréé à chaque appel. | 🟡 |
| `content/logistics/BigFluidStack.java:64-70,95-100` | `receive(RegistryFriendlyByteBuf)`, `comparator()`, `duplicateWrappers(List<BigFluidStack>)` sans appelant. | 🟡 |
| `foundation/damageTypes/CNDamageSources.java:28-36` | Surcharges privées `source(...)` à 3 et 4 arguments sans appelant (les deux méthodes publiques n'utilisent que la surcharge à 2 arguments). | 🟢 |
| `foundation/utility/CreateNuclearLang.java` | `blockName(BlockState)` (l.38), `fluidName(FluidStack)` (l.48), `text(String)` (l.61) : aucun appelant. | 🟢 |
| `foundation/utility/TextUtils.java:37-60` | `formatInt(int)` et `formatInt(int, String)` sans appelant. | 🟢 |
| `infrastructure/config/CNConfigs.java:38-40` | `byType(ModConfig.Type)` sans appelant. | 🟢 |
| `infrastructure/worldgen/biome/CNDensityFunctions.java:34-36` | `registerAndWrap(...)` (privée) sans appelant. | 🟢 |
| `infrastructure/worldgen/biome/PersistentIrradiatedZones.java:40-42` | `isInsideAnyZone(BlockPos)` sans appelant. | 🟢 |
| `infrastructure/worldgen/biome/surfacerule/IrradiatedSurfaceRules.java:66-68` | Surcharge `biome(TagKey<Biome>)` sans appelant (tous les usages réels passent par la surcharge vararg `ResourceKey...`) — le seul chemin qui instancierait `BiomeTagRule` n'est donc jamais emprunté. | 🟢 |
| `lib/multiblock/impl/IMultiBlockPattern.java:22-32` | Surcharges par défaut `matches(Level, BlockPos)`, `matchesWithResult(Level, BlockPos, Direction)`, `matchesWithResult(Level, BlockPos)` sans appelant. | 🟢 |
| `content/contraptions/irradiated/wolf/IrradiatedWolf.java:434-436` | `checkWolfSpawnRules(EntityType<Wolf>, ...)` sans appelant, paramètre `wolf` lui-même inutilisé dans le corps. | 🟢 |
| `content/decoration/palettes/PaletteBlockPattern.java:152-158` | `cubeBottomTop(String)` sans appelant (seules les fabriques `cubeAll`/`pillar`/`cubeColumn` sont câblées). | 🟢 |
| `content/explosion/CNAdvancedModelBox.java:125-127` | `getParent()` sans appelant (le champ `parent` est écrit via `setParent` mais jamais relu). | 🟢 |
| `content/explosion/CNBasicModelPart.java:36-38,40-47,65-67` | Constructeurs `CNBasicModelPart(CNBasicEntityModel, int, int)`/`CNBasicModelPart(int, int, int, int)` et la surcharge `render(...)` à 4 arguments : aucun appelant (seuls le constructeur à 1 argument et le `render(...)` à 8 arguments sont utilisés). | 🟢 |
| `content/explosion/CNAdvancedEntityModel.java:9,21-23` | Champ `movementScale` et son accesseur `getMovementScale()` : jamais réécrit hors de l'initialiseur, jamais relu ailleurs que dans l'accesseur lui-même. | 🟢 |
| `content/enriching/campfire/EnrichingCampfireBlock.java:79-81` | Constructeur `EnrichingCampfireBlock(int fireDamage, Properties property)` sans appelant (seul le constructeur à 3 arguments, utilisé par `CNBlocks.java:321`, est utilisé). | 🟢 |
| `foundation/data/recipe/CNDeployingRecipeGen.java:36-42`, `CNItemApplicationRecipeGen.java:23-29,31-37` | Chacune de ces classes déclare deux surcharges (`Ingredient` vs `Item`) au corps identique pour un même helper ; dans les deux fichiers, tous les appels réels ne résolvent qu'une seule des deux surcharges (l'autre — `Ingredient` pour `CNDeployingRecipeGen`, `Item` pour `CNItemApplicationRecipeGen` — n'est jamais invoquée). | 🟢 |

### 1.3 Champs inutilisés

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/controller/ReactorControllerBlockEntity.java:60,372` | `countCoolerRod` est assigné en `tick()` mais jamais relu ensuite (champ « write-only »). **Confirmé pré-existant côté Forge** : `countCoolerRod` y est aussi write-only sur cette classe (`triggerExplosion` ne prend que `countFuelRod`), ce n'est donc pas un artefact de migration. Vu la symétrie avec `countFuelRod` (qui, lui, alimente `triggerExplosion`), il a probablement été prévu pour atténuer l'explosion via les cooler rods mais n'a jamais été branché. **Point mis de côté** : à discuter plus tard (câbler dans `triggerExplosion`, ou supprimer avec `getConfiguredPatternCoolerRodCount()`) — aucune décision prise pour l'instant. | 🟡 |
| `content/decoration/palettes/PaletteBlockPattern.java:66-67` | Champ `private RenderType renderType;` (annoté `@OnlyIn(Dist.CLIENT)`) sans getter/setter, jamais assigné ni lu. | 🟢 |
| `content/decoration/palettes/CNPaletteStoneTypes.java:45-47` | `getVariant()` et le champ `variant` qu'il expose : jamais appelé nulle part (état write-only). | 🟢 |

### 1.4 Constantes inutilisées

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `foundation/gui/CNGuiTextures.java:15-16,31-33` | Constantes d'enum `REACTOR_CONTROLLER`/`REACTOR_CONTROLLER_PROGRESS` jamais référencées par leur nom, et le constructeur raccourci `(int startX, int startY)` (l.31-33) jamais utilisé par aucune entrée. | 🟢 |
| `infrastructure/config/CRods.java:33` | `Comments.maxFuelPerCooled` : constante de chaîne définie mais jamais passée à un appel `i()`/`f()`. | 🟢 |
| `compat/Mods.java:16` | `ALEXS_CAVE` jamais référencée hors de sa propre déclaration (le seul consommateur de `Mods` est `SableCompat`/`ReactorMeltdownExecutor`, qui utilisent `SABLE`). À rapprocher du point déjà tracké sur `AlexscaveCompat` (§4) : cette classe n'est même pas instanciée nulle part dans le projet — pas seulement « gelée », mais entièrement orpheline. | 🟢 |
| `content/decoration/palettes/PaletteBlockPattern.java:47` | `VANILLA_RANGE` jamais référencé nulle part, et strictement identique à `STANDARD_RANGE` (l.49), qui est le tableau réellement utilisé par `CNPaletteStoneTypes` — cf. duplication en §3. | 🟡 |

*(La constante `CNTags.NameSpace.FORGE`, listée ici précédemment, a été retirée dans l'arbre de travail local ; voir §8.)*

### 1.5 Imports inutiles

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `foundation/data/recipe/CNMaterialTags.java:10` | `import java.util.*;` (wildcard) dans un fichier neuf (ajouté par `cd82e97`, 11/09/2026) — incohérent avec le nettoyage déjà fait ailleurs dans le projet, qui a remplacé les imports wildcard par des imports explicites (cf. §8, `ReactorBluePrintMenu`/`ReactorOutput`). Pas un import inutile à proprement parler (`EnumMap`, `EnumSet`, `Arrays`, `Map`, `Set` sont bien utilisés), mais masque les dépendances réelles de la classe. | 🟢 |
| `CNClientProxy.java:4`, `CNCreativeModeTabs.java:9,16`, `CNDisplaySources.java:6`, `CNItems.java:4,18`, `CNRecipeTypes.java:14` | Imports wildcard (`com.mojang.blaze3d.vertex.*`, `fastutil.objects.*`, `net.minecraft.world.item.*`, `content.redstone.displayLink.source.*`, static `AntiRadiationArmorItem.*`, `net.minecraft.world.item.crafting.*`) masquant les dépendances réelles — même style d'incohérence que ci-dessus, dans des fichiers plus anciens cette fois. Aucun n'est un import inutilisé au sens strict. | 🟢 |
| `api/multiblock/fluid/ReactorFluidType.java:7,9,26` | Imports inutilisés `HolderSet`, `RegistryCodecs`, `Collectors` (aucun des trois symboles n'apparaît dans le corps du fichier). | 🟢 |
| `content/radiation/RadiationBucketItem.java:9` | `import java.util.function.Supplier;` inutilisé (le cast `Supplier` a lieu côté appelant, dans `CNFluids.java:67`, pas ici). | 🟢 |
| `content/redstone/displayLink/source/ReactorSummaryDisplaySource.java:11,15` | `import net.minecraft.core.component.DataComponents;` et `import net.minecraft.world.item.component.CustomData;` : reliquats de l'ancienne lecture de la chaleur par tag NBT (cf. commentaire de migration l.157-159), jamais retirés après le passage à `controller.getConfiguredPatternHeat()`. | 🟢 |
| `content/equipment/armor/AntiRadiationArmorClientExtensions.java:7,14` | Imports inutilisés `net.minecraft.world.entity.Entity` et `foundation/utility/ClothTagHelper`. | 🟢 |
| `content/equipment/armor/CNArmorMaterials.java:4` | Import inutilisé `NonNullBiConsumer`. | 🟢 |
| `foundation/advancement/CNAdvancementBehaviour.java:3-4` | Imports inutilisés `AdvancementBehaviour`, `CreateAdvancement` — reliquats de la méthode `tryAward` déjà supprimée (cf. §8). | 🟢 |
| `foundation/advancement/CreateNuclearAdvancement.java:3` | Import inutilisé `CreateAdvancement`. | 🟢 |
| `infrastructure/config/CBiomeRestore.java:5` | Import inutilisé `CNParticleTypes`. | 🟢 |
| `infrastructure/worldgen/biome/BiomeIrradiationService.java:10` | Import inutilisé `ServerPlayer`. | 🟢 |
| `api/radiation/IRadiationSource.java:4` | Import inutilisé `net.minecraft.world.entity.player.Player` (l'interface n'utilise que `LivingEntity`/`ItemStack`). | 🟢 |
| `content/contraptions/irradiated/chicken/IrradiatedChickenRenderer.java:5` | Import inutilisé `net.minecraft.client.renderer.MultiBufferSource`. | 🟢 |
| `content/logistics/BigFluidStack.java:5,9` | Imports inutilisés `NBTHelper`, `NbtUtils`. | 🟢 |

### 1.6 Code commenté pouvant être supprimé

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `foundation/utility/RenderHelper.java:39-51` | La branche `coverage != 1f` fait un `pushPose()`/`translate` qui s'annule exactement, puis blit à `(0,0)` — strictement identique à la branche `coverage == 1f` : le paramètre `coverage` de `renderOverlay` n'a plus aucun effet (l'appel `scale(...)` correspondant, autrefois commenté, a depuis été supprimé sans être remplacé). Seul appelant du projet (`HelmetOverlay.java:71`) passe toujours `coverage = 1f`, donc aucun effet visible aujourd'hui. **Mis de côté** : le mainteneur a choisi de ne rien changer pour l'instant (ni réimplémenter le scale, ni retirer le paramètre mort) — aucune décision prise. | 🟢 |
| `foundation/gui/CNGuiTextures.java:14` | Ancienne entrée d'enum `REACTOR_CONTROLLER("toolbox", 188, 171)` laissée en commentaire. | 🟢 |
| `content/contraptions/irradiated/cat/IrradiatedCatModel.java:32,64,66,77` | Champ `state` initialisé à `1` et jamais réassigné nulle part : les branches `state == 2` et `state == 3` de `setupAnim` sont du code mort inatteignable, signe d'une fonctionnalité d'état d'animation jamais câblée. | 🟡 |
| `net/nuclearteam/createnuclear/CNCreativeModeTabs.java:112-113` | `PackageStyles.STANDARD_BOXES.forEach(item -> { });` — lambda à corps vide, la boucle ne fait rien. | 🟢 |
| `content/enriching/campfire/EnrichingCampfireBlockEntity.java:27` | `i = state.getValue(EnrichingCampfireBlock.FACING).get2DDataValue();` : valeur calculée puis jamais utilisée (variable de boucle réutilisée puis abandonnée) — reliquat de code décompilé/porté sans effet. | 🟢 |
| `content/explosion/NuclearExplosionEntity.java:150` | `float itemDropModifier = 0.025F / Math.min(1, this.getSize());` calculée puis jamais utilisée dans `removeChunk(...)` — suggère une fonctionnalité de taux de drop d'item jamais branchée. | 🟡 |
| `net/nuclearteam/createnuclear/CNBlocks.java:390,415` | Dans les blocs `DEEPSLATE_URANIUM_ORE`/`DEEPSLATE_LEAD_ORE`, `HolderLookup.RegistryLookup<Enchantment> enchantmentRegistryLookup` est calculée puis jamais lue (le code appelle directement `lt.getRegistries().holderOrThrow(...)`) — incohérent avec les blocs jumeaux `URANIUM_ORE`/`LEAD_ORE` (l.465,490) qui, eux, réutilisent bien cette variable. | 🟢 |
| `infrastructure/ponder/CNCreateNuclearPonderTags.java:22-23` | Variable locale `itemHelper` calculée puis jamais utilisée. | 🟢 |
| `infrastructure/ponder/scenes/CNPonderReactorScenes.java:142-147` | `minX`/`maxX`/`minZ`/`maxZ`/`minY`/`maxY` calculées dans `showReactorStructure` puis jamais relues dans la méthode. | 🟢 |
| `content/contraptions/irradiated/wolf/IrradiatedWolf.java:137-141` | `finalizeSpawn` calcule `Holder<Biome> holder = level.getBiome(this.blockPosition());` puis ne l'utilise jamais. | 🟢 |
| `content/contraptions/irradiated/cat/IrradiatedCat.java:127-133` | `addAdditionalSaveData`/`readAdditionalSaveData` ne font qu'appeler `super(...)` sans logique additionnelle — contrairement aux versions poulet/loup qui persistent de vrais champs. | 🟢 |

*(Tous les autres blocs commentés précédemment listés ici — `CreateNuclearJEI`, `CNFluids`, `PlayerInteractReactorFluidInput` ×2, `ReactorControllerBlock` ×3, `NuclearMushroomCloudParticle`, `HelmetOverlay` — ont été nettoyés dans l'arbre de travail local ; voir §8.)*

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
| `content/multiblock/controller/manager/ReactorAlarmManager.java:48` | « On ne supprime pas si le chunk est juste déchargé » (dernier commentaire français restant du fichier). | 🟢 |
| `content/multiblock/controller/ReactorControllerBlockEntity.java:90` | « les pos sont [xMin, xMax, yMin, yMax, zMin, zMax] ». | 🟢 |
| `content/multiblock/bluePrintItem/ReactorBluePrintItemScreen.java:45` | `//ici pour le titre`. | 🟢 |
| `foundation/ponder/CNPonderIndex.java:16` | « Reactor - Storyboards pour chaque taille ». | 🟢 |
| `net/nuclearteam/createnuclear/CNSoundEvents.java:40,45,50,85,90` | Chemins de ressources en français : `create("reacteur/activation")`, `"reacteur/running"`, `"reacteur/shut_off"`, `"reacteur/assemble_deassemble/..."`. Impacte l'arborescence des assets, donc plus coûteux à renommer. | 🟢 |
| `infrastructure/config/CRods.java:14` | Commentaire mélangeant anglais et français : « the calcul will be... » (« calcul » au lieu de « calculation »). | 🟢 |

### 2.2 Commentaires peu explicites ou ambigus

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `foundation/events/CommentEvents.java` | Le nom de la classe ne correspond à rien de son contenu (elle enregistre des recettes de brassage, des capacités, des modificateurs d'attribut d'entité — rien à voir avec des « commentaires ») ; vraisemblablement une coquille pour `CommonEvents`. | 🟢 |
| `infrastructure/config/CExplode.java:8` | Commentaire « Duration before exploration » (coquille pour « explosion »). | 🟢 |

*(Le commentaire `@goshante` de `CreateNuclearJEI`, listé ici précédemment, a disparu avec le bloc mort qu'il annotait, supprimé dans l'arbre de travail local ; voir §8.)*

### 2.3 Javadocs incomplètes ou non standard

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/controller/manager/ReactorInputFluidManager.java:30,45,61,87,102,122` | Javadoc placée **après** `@Override` au lieu d'avant, sur 6 méthodes (`read`, `write`, `clearInvalid`, `getBlocksPosition`, `getFuildHandlers`, `getInventory`) — non reconnue par l'outillage Javadoc standard. C'est le **seul fichier du projet** encore concerné. | 🟡 |
| `content/multiblock/MultiblockHelpers.java:42-45` | Javadoc placée **à l'intérieur** du corps de `getControllerForPart` (l.41) au lieu d'être au-dessus de la signature. | 🟢 |
| `content/multiblock/rod/CNRodTypes.java:12-33` | Javadoc utile mais mal placée : elle documente la classe et `RodType.Builder` en général, alors qu'elle est apposée sur la méthode `bootstrap()`. | 🟢 |
| `content/radiation/RadiationEffect.java:26,34,42` | Commentaires inline répétant littéralement le code (`// Reduces movement speed by 20%` juste au-dessus de la ligne qui applique `-0.2D`). | 🟢 |
| `content/multiblock/controller/manager/ReactorFrameDisplayManager.java:29-31` | Javadoc tronqué : *« On the client this reads the synced ; on the server it reads the aggregated. »* — les mots attendus après « synced » et « aggregated » manquent. | 🟡 |
| `gametest/ReactorInputFluidManagerGameTest.java:49` | Javadoc de classe contenant `{@link Level}` sans import de `Level` dans le fichier — référence Javadoc non résolue. | 🟢 |

### 2.4 Commentaires devenus obsolètes

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/controller/ReactorControllerBlockEntity.java:183,221-224` | Le Javadoc *« Main constructor allowing dependency injection for testability and DIP compliance. »* est dupliqué : il documente en réalité `getMultiblockPos()` (l.183), alors que le vrai constructeur porte le même texte plus bas (l.221-224) — reliquat de copier-coller après refactorisation. | 🟠 |
| `content/multiblock/controller/service/FluidConsumptionRateCalculator.java:43-46` | Commentaire affirmant une « limitation connue » (« `fluidNeeded` n'est pas décrémenté à travers plusieurs handlers d'entrée suivis ») qui contredit l'implémentation réelle : `ReactorInputFluidManagerI.extractFluids(...)` décrémente bien `remaining` au fur et à mesure des handlers (cf. §6, déjà audité). Commentaire obsolète, jamais corrigé lors du dernier passage sur ce fichier (`4ebbd26`). | 🟢 |
| `content/multiblock/controller/snapshot/ReactorInputSnapshot.java:13-16` | Javadoc obsolète : décrit des champs `bigFuelItem`/`bigCoolerItem` qui n'existent plus dans le record actuel (`items`, `fluids`, `maxFluidCapacity`). | 🟠 |
| `api/ItemRodTypesValue.java:51-53` | Javadoc affirmant l'existence d'une valeur `MIXTE` dans `RodType.TypeRod` (« For MIXTE we keep the builder default... »), alors que cet enum ne définit que `FUEL`, `COOLER`, `NONE` (`api/multiblock/rods/RodType.java:312-324`) et que le `switch` associé lève une exception pour toute autre valeur. | 🟠 |
| `api/data/recipe/EnrichedRecipeGen.java:18` | Javadoc copié de Create : *« The base class for **Haunting** recipe generation »*, alors que la classe concerne les recettes « Enriched » (four à vent enrichissant), pas le système « Haunting » de Create. | 🟡 |
| `api/ItemRodTypesValue.java:67` | Le message d'exception de `setRodTypeInfos(int, int, int, RodType.TypeRod)` référence le nom pré-migration du projet (« ...CreateNuclearForge mod »). | 🟢 |
| `foundation/utility/InventoryHashUtil.java:48-51` | `@implNote` décrit l'algorithme comme utilisant `tag.hashCode()` (terminologie NBT), alors que l'implémentation réelle (l.91) utilise `stack.getComponents().hashCode()` — un patch de data components, pas du NBT. Description obsolète de l'algorithme qu'elle documente. | 🟡 |
| `foundation/utility/Maths.java:1-4` | Bandeau d'en-tête « Source code recreated from a .class file by IntelliJ IDEA (FernFlower decompiler) » obsolète : d'après §8, le fichier a déjà été réécrit pour ne garder que `smin`/`sampleNoise3D` — l'attribution au décompilateur ne correspond plus au contenu (désormais retouché à la main). | 🟢 |
| `content/contraptions/irradiated/cow/IrradiatedCow.java:44-45` | Commentaire « Define the base food of the animal (e.g., Wheat for Cows) » obsolète/trompeur : l'ingrédient réel de `FOOD_ITEMS` est `CNItems.YELLOWCAKE`, pas du blé. | 🟡 |

---

## 3. Duplications

| Fichier(s) | Détail | Priorité |
|---|---|---|
| `.../manager/ReactorInputManager.java:191`, `ReactorOutputManager.java:70`, `ReactorInputFluidManager.java:91`, `ReactorAlarmManager.java:59` | `getBlocksPosition(Level level, BlockPos controllerPos)` réimplémente **4 fois** le même filtre `instanceof XxxEntity` (résolution relative à `controllerPos.offset(offset)` incluse). | 🟡 |
| `content/redstone/displayLink/source/ReactorSizeDisplaySource.java:28,37` vs `ReactorSummaryDisplaySource.java:203` (`formatSize`) | Même calcul de palier (`size <= 5 ? small : size <= 7 ? medium : large`) et même motif de clé de traduction `"display_source.reactor.size." + key` dupliqués entre les deux classes. | 🟡 |
| `content/redstone/displayLink/source/ReactorSizeDisplaySource.java:16-41` | Réimplémente en ligne le switch valeur/pourcentage/jauge déjà centralisé dans `AbstractReactorStatDisplaySource.provideLine` (l.34-42) : la classe étend directement `NumericSingleLineDisplaySource` au lieu de la base abstraite déjà utilisée par les autres sources d'affichage. | 🟡 |
| `content/decoration/palettes/PaletteBlockPattern.java:47,49` | `VANILLA_RANGE` et `STANDARD_RANGE` déclarés avec un contenu strictement identique ; combiné au fait que `VANILLA_RANGE` n'a aucun appelant (§1.4), ceci ressemble à un doublon oublié plutôt qu'à une intention. | 🟡 |
| `foundation/data/recipe/CNDeployingRecipeGen.java` et `CNItemApplicationRecipeGen.java` | Chacune déclare 2 surcharges (`Ingredient`/`Item`) au corps identique pour un même helper — cf. §1.2, une des deux surcharges est de toute façon inatteignable dans chaque fichier. | 🟢 |
| `foundation/block/HorizontalDirectionalReactorBlock.java` vs `MultiDirectionalReactorBlock.java` | Structure `rotate`/`mirror` identique (≈30 lignes chacune), seule la propriété (`HORIZONTAL_FACING` vs `FACING`) diffère. | 🟡 |
| `net/nuclearteam/createnuclear/CNItems.java` | Motif de recette « `_from_decompacting` » répété **9 fois** de façon quasi identique. | 🟡 |
| `content/radiation/capability/RadiationCapability.java:137-151` | `computeItemRadiation(Player)` réimplémente inline la logique déjà factorisée dans `getStackRadiation(ItemStack, LivingEntity)` (l.153-158), que la surcharge `computeItemRadiation(LivingEntity)` utilise pourtant correctement. | 🟡 |
| `.../manager/ReactorInputManager.java:31-52`, `ReactorInputFluidManager.java:34-58`, `ReactorAlarmManager.java:18-39` | Sérialisation NBT (`read`/`write`, triplet `x`/`y`/`z`) identique répétée dans 3 managers. `ReactorOutputManager.java:22-42` utilise un 4e format (`BlockPos.asLong` sous la clé `"p"`) : les formats ne sont même pas homogènes entre managers. | 🟢 |
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
| `net/nuclearteam/createnuclear/CNBlocks.java:384-569` | Les blocs de minerai (uranium, lead, thorium, nitrate, variantes deepslate) répètent un schéma quasi identique (`initialProperties`, `loot` avec `createSilkTouchDispatchTable`/`applyExplosionDecay`, mêmes tags — désormais via `CNMaterialTags`, cf. §8). | 🟡 |
| `net/nuclearteam/createnuclear/CNItems.java:224-381` | Les 4 entrées d'armure anti-radiation (casque/plastron/jambières/bottes) répètent presque à l'identique le bloc de recette + boucle `for (Cloths cloth : Cloths.values())` générant les recettes de smithing par teinte (~150 lignes). | 🟡 |
| `lib/multiblock/SimpleMultiBlockPattern.java:31-40,42-53` | `matches(...)` et `matchesWithResult(...)` dupliquent presque intégralement la même boucle de résolution ; `matches` pourrait être réécrit comme `matchesWithResult(...) != null`. | 🟡 |
| `content/multiblock/bluePrintItem/ReactorBluePrintMenu.java:166-175` et `content/multiblock/input/item/ReactorRodInputMenu.java:113-125` | Deux implémentations différentes du même besoin (rediriger `ClickType.THROW` vers `PICKUP` pour certains slots), l'une par plage d'index, l'autre par tableau codé en dur. | 🟢 |
| `foundation/data/recipe/CNCrushingRecipeGen.java:84-88` et `CNWashingRecipeGen.java:36-40` | Même motif de surcharge `create(Supplier<ItemLike>, UnaryOperator<...>)` redirigeant vers `create(CreateNuclear.MOD_ID, ...)`, répété à l'identique dans deux générateurs distincts. | 🟢 |
| `net/nuclearteam/createnuclear/CNItems.java:61-221` | Le trio « ingot/nugget cru → recette de décompactage depuis storage block » répète le même schéma `ShapelessRecipeBuilder...requires(...).save(...)` pour 8 items. | 🟢 |
| `api/multiblock/fluid/ReactorFluidType.java:80-92` et `api/multiblock/rods/RodType.java:93-105` | `resolveReactorFluidType`/`resolveRodType` suivent le même patron à 3 étapes (lookup registre → fallback `*Value` → fallback registre `FALLBACK_*`), dupliqué terme à terme. | 🟢 |
| `content/multiblock/controller/manager/ReactorOutputManager.java:104-113` | Dans `rotateOutputs`, les branches `if`/`else` dupliquent `entity.updateSpeed = true; entity.updateGeneratedRotation();`. | 🟢 |
| `content/multiblock/controller/service/ReactorMeltdownExecutor.java:26-27` | `globalNotifyPos` et `globalExplosionPos` sont calculées avec exactement la même expression `SableCompat.toGlobal(level, explosionPos)` — deux variables distinctes portant la même valeur, utilisées respectivement par `NotifyUtil.sendTitle(...)` (l.30) et `BiomeIrradiationService.circularArea(...)` (l.47). | 🟢 |
| `content/contraptions/irradiated/{cat,chicken,cow,wolf}/*` | Duplication de structure attendue pour un portage vanilla entre les 4 animaux. | 🟢 |

---

## 4. Migration Forge → NeoForge

Rappel : uniquement les éléments clairement transitoires/résiduels de la migration technique. Le fonctionnement correct actuel n'est pas remis en cause en soi.

| Fichier:ligne | Détail | Priorité |
|---|---|---|
| `content/multiblock/input/fluid/ReactorFluidInputEntity.java:91,101` | Commentaires d'incertitude explicites sur la bonne API post-migration (« Pensez à passer registries si requis par la v1.20+... », « Pareil ici selon l'implémentation de SmartFluidTank ») — notes-à-soi-même jamais tranchées. | 🟠 |
| `content/multiblock/input/fluid/ReactorFluidInput.java:91` | « Convertit le vieux InteractionResult en ItemInteractionResult si nécessaire pour NeoForge » — le « si nécessaire » signale une incertitude non tranchée. | 🟡 |
| `net/nuclearteam/createnuclear/CreateNuclearClient.java:27` | `IEventBus neoEventBus = NeoForge.EVENT_BUS;` déclarée mais jamais utilisée — vestige d'un ancien câblage d'événements client. | 🟡 |
| `content/kinetics/fan/processing/CNFanProcessingTypes.java:39-47` | `LEGACY_NAME_MAP` : shim de compatibilité de noms lié à d'anciennes sauvegardes/NBT pré-migration. `ofLegacyName`/`parseLegacy`, ses seuls lecteurs, ont été supprimés (aucun appelant) ; le champ et son bloc d'initialisation statique sont donc désormais eux aussi orphelins — soit câbler `LEGACY_NAME_MAP` là où les NBT legacy sont lus, soit le supprimer avec le champ. | 🟡 |
| `content/contraptions/irradiated/cat/IrradiatedCatRenderer.java:5`, `wolf/IrradiatedWolf.java:3`, `foundation/block/HorizontalDirectionalReactorBlock.java:3`, `MultiDirectionalReactorBlock.java:3` | Import `com.mojang.math.MethodsReturnNonnullByDefault` au lieu de `net.minecraft.MethodsReturnNonnullByDefault` (utilisé partout ailleurs) — incohérence probablement issue d'un auto-import IDE pendant le portage. | 🟡 |
| `foundation/data/recipe/CNCrushingRecipeGen.java:42-56` | Différences de contenu de recette apparues pendant le portage, à trancher (voulu ou régression) : (1) nouvelle recette `RAW_URANIUM_BLOCK` absente côté Forge ; (2) `RAW_THORIUM_BLOCK` : la sortie secondaire `0.75f ×AllItems.EXP_NUGGET` (Forge) a été remplacée par `0.5f ×CNItems.THORIUM_DUST×72` ; (3) `RAW_THORIUM_ITEM` : même changement, `0.75f×EXP_NUGGET` → `0.5f×THORIUM_DUST×8`. Les recettes de fer/or (l.61,68) ont bien gardé leur `EXP_NUGGET`, ce qui rend l'écart d'autant plus visible. | 🟡 |
| `content/compat/alexscave/AlexscaveCompat.java:14-66` | Compat entièrement gelée en code Forge commenté (`MobSpawn`, `NukeParam`, `UpdateACProxy`, plus les méthodes entièrement commentées `isRaycat`, `isTremorzilla`, `ACResConfig`, `ACDestroyable`, `GetACSounds`, `GetACConfig`) ; le commentaire de classe indique explicitement que le mod tiers n'a pas encore de version 1.21.1. Coquille vide en attente côté NeoForge. **Confirmé** : la classe n'est même instanciée nulle part dans le projet (pas seulement « gelée » en interne, mais entièrement orpheline) ; `Mods.ALEXS_CAVE` (le seul point d'entrée logique pour l'activer un jour) est lui aussi sans référence — cf. §1.4. | 🟠 |
| `content/multiblock/frame/ReactorFrameRenderer.java:74-77` | `CatnipServices.FLUID_RENDERER` est déclaré `FluidRenderHelper<?>`, obligeant un cast non vérifié (`@SuppressWarnings("unchecked")`) vers `FluidRenderHelper<FluidStack>` de NeoForge : shim multiplateforme (Catnip/Create, Forge vs NeoForge) laissé tel quel. | 🟡 |

---

## 5. Nettoyage

Éléments à retirer une fois la migration complètement terminée et les points de la section 4 tranchés.

- Retirer `CreateNuclearClient.java:27` (`neoEventBus` inutilisé).
- Supprimer ou câbler `CNFanProcessingTypes.LEGACY_NAME_MAP` (`ofLegacyName`/`parseLegacy`, ses seuls lecteurs, ont déjà été supprimés — le champ est désormais orphelin).
- Supprimer `content/compat/alexscave/AlexscaveCompat.java` et l'entrée `Mods.ALEXS_CAVE` (ou les réécrire proprement) une fois qu'Alex's Caves publie une version 1.21.1 compatible et qu'une vraie intégration est décidée — les deux sont aujourd'hui totalement orphelins (§1.4).
- Retirer le champ mort `countCoolerRod` de `ReactorControllerBlockEntity` (ou l'implémenter réellement) — attention : `@SuppressWarnings({"unused"})` sur la classe masque ce type de code mort, à retirer une fois le nettoyage fait pour que l'IDE le détecte à nouveau. Le même masquage existe sur `CreateNuclearJEI`, `CNStandardRecipeGen` et `UraniumOreBlock` (`@SuppressWarnings("unused")` sur la classe entière) — à retirer une fois leurs méthodes/champs morts respectifs nettoyés (§1.2), pour que l'IDE redétecte tout code mort futur.
- Corriger l'incohérence de paquet `foundation/damageTypes/CNDamageSources.java` : le dossier est `damageTypes` mais le fichier déclare `package ...foundation.damagesTypes;` (avec un « s » superflu). Compile aujourd'hui car les deux importeurs (`RadiationEffect.java`, `CNFanProcessingTypes.java`) utilisent la même faute, mais cassera tout futur refactor IDE automatique.
- Supprimer les blocs de code mort listés en §1.2 une fois confirmés inutiles pour de bon : le cluster de méthodes mortes de `CreateNuclearJEI` et de `CNStandardRecipeGen`, `CExplode` (classe entière), `BigFluidStack` (3 méthodes), et les surcharges inatteignables listées.
- Corriger le message d'exception de `ItemRodTypesValue.setRodTypeInfos` qui référence encore le nom pré-migration du projet (« CreateNuclearForge mod »).

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
- **`ReactorSizeDisplaySource`** : le faire hériter de `AbstractReactorStatDisplaySource` comme les autres sources d'affichage, au lieu de réimplémenter en ligne le switch valeur/pourcentage/jauge déjà centralisé dans `provideLine`.
- **`content/explosion`** (`CNAdvancedModelBox`, `CNTabulaModelRenderUtils`) : renommer les paramètres décompilés à la MCP (`p_228300_1_`, `p_i225950_3_`, etc.) restés tels quels après le portage — purement cosmétique, aucun changement de comportement.
- **`CNBasicModelPart.java:76`** : renommer la variable locale qui masque le nom de sa propre classe (`CNBasicModelPart CNBasicModelPart = (CNBasicModelPart) var9.next();`) — correcte mais déroutante à la lecture.
- **`IrradiatedCatModel`** : décider du sort du champ `state` (jamais réassigné) — soit câbler un vrai état d'animation pour les branches `state == 2`/`state == 3` de `setupAnim`, soit les retirer avec le champ.

---

## 7. Tableau de priorités global

### 🔴 Critique


### 🟠 Important

- Bugs de logique (§0) : `IrradiatedCat.finalizeSpawn` n'override plus rien depuis un changement de signature Minecraft (la vérification « chat noir » ne s'exécute jamais) ; fuite mémoire non bornée dans `VicinityEffect.cooldowns` (jamais purgée).
- Débris de migration à finaliser : notes d'incertitude non tranchées dans `ReactorFluidInputEntity`, compat `AlexscaveCompat` entièrement gelée en code Forge commenté et confirmée totalement orpheline (avec `Mods.ALEXS_CAVE`).
- Commentaires français masquant une incertitude technique : `ReactorFluidInputEntity`.
- Incohérence de paquet `foundation/damageTypes` (dossier) vs `foundation.damagesTypes` (package déclaré) dans `CNDamageSources.java`.
- Duplications significatives : `getBlocksPosition(Level)` ×4 managers, `HorizontalDirectionalReactorBlock`/`MultiDirectionalReactorBlock`, `CNItems` decompacting ×9, `RadiationCapability.computeItemRadiation(Player)`, 13 classes `*Factory` de `SmallNuclearExplosionParticle`, triple duplication de verrou fluide dans `ReactorFluidInputEntity`, triple duplication de scan dans `ReactorPattern`.
- Javadoc trompeur/obsolète : copier-coller mal placé dans `ReactorControllerBlockEntity` (constructeur), champs disparus documentés dans `ReactorInputSnapshot`, valeur d'enum `MIXTE` inexistante dans `ItemRodTypesValue`.

### 🟡 Moyen

- Duplications : `HorizontalDirectionalReactorBlock`/`MultiDirectionalReactorBlock` (déjà listé ci-dessus), blocs multiblock `onPlace`/`onRemove`, générateurs `SpecialBlockStateGen` ×3, `CoolerDisplaySource`/`FuelDisplaySource`/`ReactorSummaryDisplaySource`, `ReactorSizeDisplaySource` (formatSize + switch non centralisé), `EnrichedRecipe`/`SnowPowderRecipe` + catégories JEI, `CNTags` (5 enums ~230 lignes), `CNBlocks` (blocs de minerai), `CNItems` (armures anti-radiation), `CNDensityFunctions` (expression dupliquée), `matches`/`matchesWithResult` dans `SimpleMultiBlockPattern`, `PaletteBlockPattern.VANILLA_RANGE`/`STANDARD_RANGE` (tableaux identiques).
- Dead code : champ orphelin `CNFanProcessingTypes.LEGACY_NAME_MAP`, `countCoolerRod` write-only, abstraction `IPatternBuilder` jamais exploitée, classe entière `CExplode` jamais référencée, clusters de méthodes mortes dans `CreateNuclearJEI` et `CNStandardRecipeGen`, `CNArmorMaterials.durabilityForType`, `BigFluidStack` (3 méthodes), branches inatteignables `state == 2`/`3` dans `IrradiatedCatModel`.
- Javadoc mal placée après `@Override` dans `ReactorInputFluidManager` (6 méthodes) ; Javadoc française mal formée `ReactorAlarmManagerI` ; Javadoc tronqué `ReactorFrameDisplayManager`; Javadoc copié de Create dans `EnrichedRecipeGen` ; commentaire obsolète dans `FluidConsumptionRateCalculator` contredisant l'implémentation réelle ; `@implNote` obsolète dans `InventoryHashUtil` (NBT vs data components) ; commentaire obsolète dans `IrradiatedCow` (blé vs Yellowcake).
- Imports `com.mojang.math.MethodsReturnNonnullByDefault` (4 fichiers) ; `CreateNuclearClient.neoEventBus` ; incertitude `ReactorFluidInput.java:91` ; shim `FluidRenderHelper<?>` cast non vérifié dans `ReactorFrameRenderer`.
- Divergences de recettes `CNCrushingRecipeGen` à trancher.
- Variable locale calculée puis jamais utilisée dans `NuclearExplosionEntity.removeChunk` (`itemDropModifier`) — suggère un taux de drop d'item jamais branché.

### 🟢 Faible

- Paramètre `coverage` mort dans `RenderHelper.renderOverlay` (mis de côté, pas d'action prévue).
- Commentaires français restants sans impact joueur (`ReactorInputManager`, `ReactorAlarmManager:47`, display sources, `CNDisplaySources`, `CNPonderIndex`, `ReactorBluePrintItemScreen`, `ReactorControllerBlockEntity:90`, `RadiationCapability.radiation_desactive`, `CRods`) et chemins de sons `"reacteur/..."`.
- Javadoc mal placée (`MultiblockHelpers`, `CNRodTypes`, `ReactorInputFluidManagerGameTest`), commentaires paraphrasant le code (`RadiationEffect`), bandeau décompilateur obsolète (`Maths.java`), noms de classe trompeurs (`CommentEvents`), coquilles (`CExplode` "exploration"), référence au nom pré-migration du projet dans `ItemRodTypesValue`.
- Duplications mineures : NBT des managers, verrous de fluide, builders `RodType`/`ReactorFluidType`, `isFood` poulet/loup, textures `WOLF_LOCATION`/`WOLF_TAME_LOCATION`, `rotateOutputs` if/else, variables `globalNotifyPos`/`globalExplosionPos` identiques dans `ReactorMeltdownExecutor`, générateurs de recettes `create(...)` (Crushing/Washing), trio ingot/nugget `CNItems`, `resolveReactorFluidType`/`resolveRodType`, menus `clicked()` (BluePrint/RodInput), surcharges jumelles dans `CNDeployingRecipeGen`/`CNItemApplicationRecipeGen`.
- Dead code mineur : nombreuses méthodes/constructeurs/imports sans appelant recensés en §1.2/§1.3/§1.4/§1.5 (constantes `CNGuiTextures`, champs `PaletteBlockPattern`/`CNPaletteStoneTypes`, variables locales diverses, imports inutilisés répartis sur une quinzaine de fichiers).
- Refactors cosmétiques : paramètres décompilés MCP non renommés (`CNAdvancedModelBox`, `CNTabulaModelRenderUtils`), variable masquant le nom de sa classe (`CNBasicModelPart`).

---

## 8. Historique des corrections

Points listés dans une version antérieure de cet audit, corrigés depuis et retirés des sections ci-dessus.

| Ex-# | Fichier:ligne | Problème (tel qu'audité) | Correction | Date |
|---|---|---|---|---|
| — | `api/ReactorFluidTypesValue.java:53,71`, `api/ItemRodTypesValue.java:58-74` | Signalées comme méthodes inutilisées (`setReactorFluidTypeInfos(...)` ×2 surcharges, `setRodTypeInfos(int, int, int, RodType.TypeRod)`) car aucun appelant n'existe dans le code de ce mod. | **Décision du mainteneur : conservées telles quelles, pas du code mort.** Ce sont des méthodes d'API publique (paquet `api/`), destinées à être appelées par d'autres mods tiers pour déclarer leurs propres types de fluide de réacteur / de rod — l'absence d'appelant interne est donc normale et voulue, pas un oubli. Seul le message d'exception obsolète de `ItemRodTypesValue.setRodTypeInfos` (référence au nom pré-migration « CreateNuclearForge mod », l.67) reste tracké, en §2.4 (commentaire obsolète), car ce point-là est indépendant du statut « API publique ». | 11/09/2026 |
| — | `foundation/utility/NotifyUtil.java:113-131` | Signalées comme méthodes inutilisées : les deux surcharges de `quickAlert` n'ont aucun appelant dans le projet. | **Décision du mainteneur : conservées telles quelles, pas du code mort**, même principe que ci-dessus bien que le fichier ne soit pas dans le paquet `api/` — utilitaire public prévu pour un usage futur/externe, l'absence d'appelant actuel n'est pas un oubli. | 12/09/2026 |
| — | `content/contraptions/irradiated/IrradiatedAnimal.java:64-89` (`getConversionProgress()`), et l'incohérence de portage notée entre les 4 animaux irradiés (seul `IrradiatedChicken` implémente `IrradiatedAnimal`, `AnimalUtil.blockTamingWip` utilisé par Cat et Wolf) | Signalées comme code mort / portage incohérent, faute d'appelant ou de symétrie entre les 4 classes d'animaux. | **Décision du mainteneur : conservées telles quelles, pas un problème.** C'est la base de la future fonctionnalité d'évolution dynamique des mobs irradiés documentée dans `PROTOTYPE_EVOLUTION_MOBS_IRRADIES.md` (mob irradié acquérant dynamiquement attributs/comportements/apparence des entités qu'il tue) — le portage n'est délibérément pas symétrique entre les 4 animaux tant que cette fonctionnalité n'est pas implémentée. | 12/09/2026 |
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
| — | `content/uraniumOre/UraniumOreBlock.java:18-19,93-96` | `EnchantmentHelper`/`Enchantments` ne servaient qu'au bloc XP commenté de `spawnAfterBreak`. | **Le bloc XP n'est plus commenté** : `spawnAfterBreak` est désormais actif (lookup silk touch + `popExperience`), donc les deux imports sont réellement utilisés — ce n'était pas un nettoyage d'import mais l'activation de la fonctionnalité elle-même. | 02/09/2026 |
| — | `content/multiblock/controller/ReactorControllerBlockEntity.java:8-14` | `import net.minecraft.core.*;` rendait redondants les imports explicites `BlockPos`/`Direction`/`HolderLookup` ; imports totalement inutilisés en plus : `SimpleMultiBlockAislePatternBuilder`, `CatnipServices`, `ChatFormatting`. | Wildcard et imports inutilisés retirés ; ne restent que les imports explicites nécessaires. | 02/09/2026 |
| — | `lib/multiblock/SimpleMultiBlockAislePatternBuilder.java:5-7` | `import lib.multiblock.impl.IMultiBlockPatternBuilder;` importé deux fois. | Doublon retiré. | 02/09/2026 |
| — | `api/ItemRodTypesValue.java:5,10`, `api/ReactorFluidTypesValue.java:5,10` | Imports inutilisés `HolderSet`, `Collections`. | Retirés. | 02/09/2026 |
| — | `net/nuclearteam/createnuclear/CNSoundEvents.java:5` | Import `com.simibubi.create.AllSoundEvents` inutile (seuls les types imbriqués, importés séparément, sont utilisés). | Retiré. | 02/09/2026 |
| — | `net/nuclearteam/createnuclear/CNParticleRegistry.java:4-5` | Imports `BlockParticleOption`/`ItemParticleOption` inutilisés. | Retirés. | 02/09/2026 |
| — | `content/multiblock/bluePrintItem/ReactorBluePrintMenu.java:16`, `content/multiblock/output/ReactorOutput.java:35` | Imports wildcard `net.nuclearteam.createnuclear.*` masquant les dépendances réelles de la classe. | Remplacés par des imports explicites dans les deux fichiers. | 02/09/2026 |
| — | `content/multiblock/controller/snapshot/ReactorInputSnapshotBuilder.java:12` | Import `VirtualReactorInputsItem` devenu inutile après la suppression de la variable locale `virtualItems`. | Retiré. | 02/09/2026 |
| — | `net/nuclearteam/createnuclear/CNTags.java:32-46` | `forgeTag`/`forgeBlockTag`/`forgeItemTag`/`forgeFluidTag` : nommage hérité de l'ère Forge pour un mécanisme qui pointe en réalité vers `NEO_FORGE`/`"c"`, prêtant à confusion (utilisé des dizaines de fois dans `CNBlocks.java`, `CNItems.java`, `CreateNuclearRegistrateTags.java`). | Renommées en `neoForgeTag`/`neoForgeBlockTag`/`neoForgeItemTag`/`neoForgeFluidTag`, cohérent avec leur usage réel. L'entrée d'enum `FORGE("forge")` elle-même n'a en revanche pas été retirée par ce commit — reste suivie en §1.4 (Constantes inutilisées). | 02/09/2026 |
| — | `content/multiblock/input/item/ReactorRodInputEntity.java:86-93` | Bloc commenté utilisant explicitement l'ancienne API de capacités Forge (`Capability<?>`, `ForgeCapabilities.ITEM_HANDLER`, `ResetableLazy<T>`) — reliquat direct jamais retiré après le passage aux capacités NeoForge. | Bloc commenté supprimé. | 02/09/2026 |
| — | `content/explosion/NuclearExplosionEntity.java:90-107` | La garde `if (!level().isClientSide)` ajoutée par `bf569b7` (05/09/2026) autour de la boucle dégâts/knockback, dans le but de corriger un double dégâts/knockback client-serveur, avait pour effet de bord de supprimer tout recul (knockback) en jeu. **Confirmé par le mainteneur** : avec la garde en place, plus aucun recul n'était appliqué. Retirée par `d312fed` (06/09/2026) — retrait volontaire pour restaurer le recul, pas une régression accidentelle. | Garde retirée intentionnellement. | 06/09/2026 |
| — | `net/nuclearteam/createnuclear/CNTags.java:52` | Constante d'enum `FORGE("forge")` sans aucune référence dans le projet, restée orpheline après le renommage `forgeXxxTag`→`neoForgeXxxTag`. | Entrée retirée. | `7c27b91`, 06/09/2026 |
| — | `content/multiblock/controller/ReactorControllerBlockEntity.java:360` | Log de debug `CreateNuclear.LOGGER.info("[MeltdownDebug] destroying structure, multiblockBounds={}", ...)` laissé en `info()` après l'ajout de la destruction du multiblock au meltdown. | Ligne supprimée. | `7c27b91`, 06/09/2026 |
| — | `compat/jei/CreateNuclearJEI.java:129-166` | `registerExtraIngredients(...)` contenait un bloc commenté (générer des fluides de potion par `BottleType`) dupliqué par le code actif juste après, plus un commentaire attribué nominativement à un contributeur (« `@goshante:` »). | Méthode entière supprimée (plus utilisée). | `7c27b91`, 06/09/2026 |
| — | `net/nuclearteam/createnuclear/CNFluids.java:114` | Ligne d'enregistrement `//.onRegister(ReactorFluidTypesValue.setReactorFluidTypeInfos(8196, 100))` commentée sur `LIQUID_NITROGEN`. | Ligne supprimée. | `7c27b91`, 06/09/2026 |
| — | `content/multiblock/input/fluid/PlayerInteractReactorFluidInput.java:54-58,66-68` | Bloc commenté mort (`//if (!fluidInItem.isEmpty()...)`) et bloc conditionnel entièrement vide (`if (player.isCreative() && !onClient) { }`). | Les deux blocs supprimés. | `7c27b91`, 06/09/2026 |
| — | `content/multiblock/controller/ReactorControllerBlock.java:105-107,134,194` | Premier branchement vide `if (!state.getValue(ASSEMBLED)) { }` (condition à inverser), plus deux lignes commentées (`//be.clearTimers();...`, `//entity.removeIOAll();`). | Branchement vide inversé (ne garde que le contenu utile), lignes commentées supprimées. | `7c27b91`, 06/09/2026 |
| — | `content/particles/NuclearMushroomCloudParticle.java:62,70` | `LOGGER.info("EXPLOSIOOOOOON")` (log de debug oublié) et ligne commentée `// playSound(CNSoundEvents.NUCLEAR_EXPLOSION_SHOCKWAVE...)`. | Log repassé en `LOGGER.debug(...)`, ligne commentée supprimée. | `7c27b91`, 06/09/2026 |
| — | `foundation/events/overlay/HelmetOverlay.java:73` | `//Minecraft.getInstance().gui.renderItemHotbar(12f, graphics);` (et son commentaire d'explication). | Lignes supprimées. | `7c27b91`, 06/09/2026 |

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

La passe du 06/09/2026 est une **re-vérification incrémentale** (pas une relecture intégrale) : elle porte uniquement sur les 11 commits intervenus entre `d2c7078` et `5fa0cb7` (renommage `CNTags.forgeXxxTag`, suppression du bloc de capacités Forge de `ReactorRodInputEntity`, refactor des managers vers des positions relatives au contrôleur, ajout de la compat Create Aeronautics/Sable, destruction du multiblock au meltdown). Chaque fichier touché par ces commits a été relu en entier pour confirmer les points désormais résolus (déplacés en §8), réajuster les numéros de ligne des points encore ouverts, et vérifier l'absence/présence de nouveaux problèmes (dead code, commentaires, duplication, résidus de migration) introduits par ces changements. Un point initialement suspecté comme régression (une garde `!isClientSide` ajoutée par un commit puis retirée par le suivant, dans `NuclearExplosionEntity`) s'est révélé être un retrait volontaire après vérification auprès du mainteneur (la garde supprimait tout recul/knockback en jeu) — consigné en §8, pas dans les bugs ouverts.
