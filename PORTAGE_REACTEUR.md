# Portage du réacteur — Forge → NeoForge

Comparaison `CreateNuclearForge` (branche `V2`) → `CreateNuclearNeoForge` (branche `V2-Reacteur`).
Périmètre : domaine réacteur uniquement (`content/multiblock/**` + dépendances directes).

> ## ✅ Portage terminé et validé en jeu
>
> Les 8 lots sont faits. Le réacteur s'assemble, produit, chauffe, alarme, fond et explose.
> Validé en jeu par mathi le 6 août 2026 : assemblage, blueprint, fluide, production SU.
>
> **~50 fichiers portés**, dont la majorité identiques à l'octet à l'original Forge.
> `ReactorControllerBlockEntity` : 868 lignes monolithiques → 424 lignes déléguantes,
> divergence vs Forge ramenée de **864 lignes à 76** (toutes des adaptations 1.21).
>
> Gametests : `./gradlew runGameTestServer` → **29 tests, tous verts** (`EXIT=0`).
> Utilisable en CI bloquante.
>
> Ce qui reste hors périmètre est inventorié en **§9**.

---

## 1. Constat central

**Le réacteur NeoForge n'est pas une version « réorganisée » du réacteur Forge : c'est une génération antérieure du code.**

C'est le point le plus important du rapport, parce qu'il change la nature du travail. Un portage classique consiste à traduire l'API. Ici, il faut d'abord **remplacer la logique**, puis traduire l'API.

| | Forge | NeoForge |
|---|---|---|
| `ReactorControllerBlockEntity` | 440 lignes, logique déléguée | **735 lignes monolithiques** |
| Packages `service/`, `reactorLogic/`, `consumable/`, `snapshot/`, `display/` | 31 fichiers | **0 — inexistants** |
| Modèle thermique | équilibre 6:1 fuel/cooler, surchauffe progressive, fluide caloporteur | somme `baseUraniumHeat`/`baseGraphiteHeat` codée en dur |
| Fusion du cœur | compte à rebours 300 ticks + notifications + explosion dimensionnée | **absente** |
| Alarmes | pilotées par coordinateur + advancement | **jamais déclenchées** |
| Consommation des barres | cycles par type de barre, persistés en NBT | timers `tmpUraniumTimer`/`tmpGraphiteTimer` codés en dur |
| Son de fonctionnement | boucle pilotée par blockstate `ACTIVE` | **absent** |
| Tooltip goggles | rendu depuis un snapshot synchronisé | lit `fuelItem`/`coolerItem` locaux |

Concrètement, le `tick()` NeoForge fait encore : lire l'inventaire d'un unique `ReactorRodInput`, décrémenter deux timers, calculer une chaleur par voisinage uranium/graphite, et faire tourner la sortie. Il n'y a ni surchauffe, ni fusion, ni explosion, ni consommation de fluide.

**Conséquence sur la méthode :** ne pas essayer de « compléter » le BE NeoForge existant. Il faut le remplacer par la version Forge et porter les 31 fichiers de logique en dessous. Les ~300 lignes actuelles de calcul (`calculateHeat`, `calculateProgress`, `updateTimers`, `getStructureBounds`, `convertePattern`, `formattedPattern`, `rotate`) sont du code mort une fois le portage fait.

---

## 2. Ce qui existe déjà côté NeoForge (bonne nouvelle)

Toute l'infrastructure dont dépendent les services Forge est **déjà présente** :

`RodType` · `ReactorFluidType` · `BigFluidStack` · `NotifyUtil` · `CreateNuclearLang` · `BiomeIrradiationService` · `CNBiomes` · `NuclearExplosionEntity` · `CNAdvancement` · `CNAdvancementBehaviour` · `CNConfigs` · `ReactorBluePrintItem.getItemStorage()` · `CNSoundEvents.REACTOR_RUNNING`

Les 5 managers (`ReactorInputManager`, `ReactorOutputManager`, `ReactorInputFluidManager`, `ReactorAlarmManager`, `AbstractReactorIOManager`) sont eux aussi déjà portés et à jour.

Le portage ne part donc pas de zéro : il porte sur la couche logique, pas sur les fondations.

---

## 3. Divergences d'API bloquantes — à traiter EN PREMIER

Ces cinq points doivent être réglés avant de porter le moindre service, sinon rien ne compile.

### 3.1 `RodType` — modèle différent

| Forge | NeoForge |
|---|---|
| `Holder<Item> item` (une barre par type) | `HolderSet<Item> items` (plusieurs barres par type) |
| `baseRodHeat()` → `Supplier<Integer>`, appelé `.get()` | `baseRodHeat()` → `int` direct |
| `proximityRodHeat()` → `Supplier<Float>` | `proximityRodHeat()` → `float` direct |
| `rodTimer()` → `Supplier<Integer>` | `rodTimer()` → `int` direct |
| **`ratio()` présent** | **`ratio()` absent** |
| `TypeRodPredicate.isFuel(RodType)` / `isCooled(RodType)` | uniquement `IS_FUEL` / `IS_COOLED` sur `ItemStack` |

**À faire :** ajouter `ratio()` à `RodType` NeoForge (+ builder + codec + config), ajouter les surcharges `isFuel(RodType)` / `isCooled(RodType)`. Dans les services portés, supprimer les `.get()` sur `baseRodHeat`/`proximityRodHeat`/`rodTimer`.

Le `HolderSet` au lieu du `Holder` n'est pas un problème : les services n'utilisent que `resolveRodType(Item, Level)`, dont la signature est identique des deux côtés.

### 3.2 `IHeat.HeatLevel` — pas de notion de taille de réacteur

| Forge | NeoForge |
|---|---|
| `of(int heat, int reactorSize)` | `of(int heat)` |
| `isNotDanger(int heat, int reactorSize)` | **absent** |
| `getFormattedHeatText(int heat, int reactorSize)` | `getFormattedHeatText(int heat)` |
| `getFormattedItemText(ItemStack, Boolean, Level)` | `getFormattedItemText(ItemStack, Boolean)` |

Côté Forge, les seuils de danger dépendent de la taille du réacteur (5×5 / 7×7 / 9×9). C'est ce qui rend un 9×9 viable à haute chaleur. Côté NeoForge, le seuil est unique.

**À faire :** porter la version taille-consciente de `IHeat.HeatLevel`, qui dépend du point suivant.

### 3.3 `CReactorHeat` — fichier de config absent

```java
public final ConfigInt size5Danger = i(256,  0, 8192, ...);
public final ConfigInt size7Danger = i(1024, 0, 8192, ...);
public final ConfigInt size9Danger = i(4096, 0, 8192, ...);
```

**À faire :** créer `infrastructure/config/CReactorHeat.java` et le déclarer dans `CNCServer`/`CNConfigs`.

### 3.4 `CRods` — config incomplète

Forge expose 12 valeurs (uranium / graphite / **thorium**, avec `*HeatRatio` et `*ProximityBonus`). NeoForge n'en expose que 3 (`uraniumRodLifetime`, `graphiteRodLifetime`, `maxHeat`) et **ne connaît pas le thorium**.

**À faire :** aligner `CRods` sur la version Forge. Prérequis du `ratio()` de 3.1.

### 3.5 `ReactorControllerBlock` — propriété `ACTIVE` absente

Forge déclare `ASSEMBLED` **et** `ACTIVE`. NeoForge n'a que `ASSEMBLED`.

`ACTIVE` est recalculée chaque tick serveur et synchronisée automatiquement au client ; c'est elle qui pilote la boucle sonore de fonctionnement. Sans elle, `ReactorRunningSoundInstance` n'a rien à observer.

**À faire :** ajouter `ACTIVE` au blockstate + au datagen des blockstates JSON.

---

## 4. Fichiers à créer — par ordre de portage

### Lot 0 — Prérequis (rien ne compile sans) ✅ FAIT
| Fichier | Note |
|---|---|
| `infrastructure/config/CReactorHeat.java` | création |
| `infrastructure/config/CRods.java` | **extension** (thorium + ratios + proximité) |
| `api/multiblock/rods/RodType.java` | **modification** (valeurs en `Supplier`, `ratio()`, prédicats `RodType`) |
| `content/multiblock/IHeat.java` | **modification** (seuils par taille) |
| `content/multiblock/controller/ReactorControllerBlock.java` | **modification** (`ACTIVE`) |
| `content/multiblock/controller/ReactorControllerGenerator.java` | **modification** (textures off/standby/on) |
| `CNItems.java` | **modification** — voir ci-dessous |

> **Trouvé en cours de route :** seul `THORIUM_ROD` avait un `RodType` enregistré côté
> NeoForge. `URANIUM_ROD` et `GRAPHITE_ROD` n'en avaient aucun, donc `resolveRodType()`
> retombait sur `FALLBACK` (type `NONE`), que `DefaultHeatCalculator` ignore : le modèle
> thermique aurait calculé **zéro chaleur en permanence**, sans erreur visible. Les trois
> barres sont désormais enregistrées et adossées à la config.
>
> ⚠️ Le blockstate du contrôleur passe de 8 à 16 variants (ajout d'`ACTIVE`) :
> `runData` doit être relancé, sinon le bloc affiche un modèle manquant en jeu.

### Lot 1 — Modèle thermique (`reactorLogic/`, 7 fichiers, ~217 lignes) ✅ FAIT
`IHeatCalculator` · `DefaultHeatCalculator` · `IOverheatController` · `DefaultOverheatController` · `HeatBalance` · `EquilibriumState` · `HeatManager`

Cœur du modèle : équilibre 6:1 fuel/cooler, surchauffe qui s'accélère (`overFlowLimiter` décrémenté), pénalité de fluide insuffisant ou de dépassement du `maxHeat` du fluide.

> **Correction d'ordre :** `display/ReactorDisplayState` est une dépendance dure de
> `DefaultHeatCalculator` (paramètre de `computeHeat`). Il a donc été porté avec le lot 1
> au lieu du lot 4. Ses méthodes NBT prennent un `HolderLookup.Provider` en 1.21, parce que
> `BigFluidStack.write()/read()` l'exigent côté NeoForge.

### Lot 2 — Lecture de pattern & consommation (`consumable/`, 6 fichiers, ~317 lignes) ✅ FAIT
`PatternReader` · `IConsumable` · `ItemConsumable` · `FluidConsumable` · `ConsumableTimer` · `ConsumptionCycleManager`

`PatternReader` est utilisé par le lot 1 et le lot 3 — à porter tôt.

> **Divergence corrigée dans `ReactorBluePrintMenu` :** côté Forge, `pattern` est la grille
> brute du joueur et `patternAll` la **même grille normalisée** — slots vides *et* items
> non-barres remplacés par du `GLASS_PANE`. Côté NeoForge les deux champs pointaient sur
> **le même tableau**, et les items non-barres y étaient conservés.
>
> Sans correction, un item quelconque déposé dans la grille aurait été compté par
> `PatternReader` comme une exigence du pattern. `ReactorHeatUpdateCoordinator.updateHeatOnly`
> exige que *chaque* entrée du pattern soit disponible dans les inputs : un item non-barre
> n'étant jamais fourni, la chaleur serait restée bloquée à 0 sans erreur visible.
>
> `saveData` construit désormais deux tableaux distincts. `getItemStorage()` continue de lire
> `pattern()` (brut), comme la version Forge qui lit l'élément NBT `pattern`.

#### Lot 2bis — Migration NBT → Data Components du blueprint ✅ FAIT (9 août 2026)

Le lot 2 ci-dessus portait la lecture du pattern ; ce qui suit va plus loin et retravaille le
**stockage** du blueprint lui-même (`bluePrintItem/`), au-delà d'un simple portage d'API.

**Point de départ.** À l'ouverture de ce chantier, le blueprint NeoForge était dans un état
intermédiaire : `ReactorBluePrintData`/`PatternData` (les vrais Data Components) coexistaient
avec des restes NBT jamais nettoyés après leur introduction — un composant
`CNDataComponents.PATTERN` qui ne faisait que réemballer une `CompoundTag` (jamais écrit,
toujours lu vide), un paquet réseau (`ReactorBluePrintItemPacket`) hérité du portage Forge dont
**tous les champs étaient write-only** côté client (`heat`, compteurs, `progress`,
`sendUpdate` : aucun n'était relu par un rendu ou une logique quelconque), et quatre Data
Components orphelins (`URANIUM_TIME`, `GRAPHITE_TIME`, `COUNT_GRAPHITE_ROD`,
`COUNT_URANIUM_ROD`) enregistrés mais jamais lus ni écrits.

**Ce qui a été fait :**
- Suppression complète du paquet réseau et de son enregistrement dans `CNPackets` : ce menu
  n'a structurellement besoin d'aucun paquet, dans aucun sens. C'est un menu à un seul joueur
  possible (item tenu en main), l'édition du pattern passe déjà par la synchronisation de slots
  vanilla, et `saveData()` (`MenuBase#removed`, dans Create) écrit directement sur le
  `contentHolder` serveur sans aller-retour réseau.
- Suppression de `CNDataComponents.PATTERN` et des quatre composants orphelins.
- `ReactorBluePrintData` réduit à `(int countCooledRod, int countFuelRod, PatternData[] pattern)` :
  `patternAll` n'est plus persisté ni synchronisé, il est dérivé à la demande
  (`patternAll(Level)`) depuis `pattern`, ce qui élimine par construction le risque de
  divergence entre les deux vues qui avait motivé la correction du lot 2. `graphiteTime`/
  `uraniumTime` sont retirés aussi : ce n'étaient que des constantes de config recopiées sur
  chaque blueprint, jamais lues nulle part.
- Ajout d'un sentinel `ReactorBluePrintData.EMPTY`, qui remplace sept réimplémentations
  indépendantes de « ce blueprint est-il configuré ? » (`configuredPattern.get(...) == null`,
  dupliqué dans `ReactorControllerBlockEntity` ×4, `ReactorControllerBlock`,
  `ReactorHeatUpdateCoordinator`, `ConsumptionCycleManager`) par un seul point de vérité,
  comparé par identité (`== EMPTY`), sur le même principe que `ItemStack.EMPTY`.

**Bug trouvé et corrigé, propre à NeoForge :** un blueprint contenant des barres de thorium
perdait silencieusement ces barres à la réouverture du menu (confirmé par un dump NBT
`/setblock` montrant le pattern correctement sauvegardé, mais absent une fois le menu rouvert).
Cause : `THORIUM_ROD` est enregistré `.fuelRodType()` côté `RodType` (`CNItems.java`), mais son
`.tag(...)` n'incluait pas `CNItemTags.FUEL.tag` — contrairement à `URANIUM_ROD`/`GRAPHITE_ROD`,
qui ont bien leur tag. `ReactorBluePrintMenu#initAndReadInventory` filtrait la réaffichage sur
ce tag plutôt que sur `RodType`, donc le thorium passait la sauvegarde mais échouait
silencieusement à la relecture. Corrigé à deux niveaux : le tag manquant ajouté dans
`CNItems.java`, et le filtre de réaffichage réécrit pour utiliser `RodType`/`TypeRodPredicate`
partout (comptage, `pattern`, `patternAll`, réaffichage), la même source de vérité que
`saveData` utilisait déjà pour `patternAll`.

**Bug de config trouvé au passage, sans lien avec les Data Components :** `CNCCommon` déclarait
sa propre instance de `CRods` (`Type.COMMON`), en plus de celle déjà présente dans `CNCServer`
(`Type.SERVER`) — deux fichiers de config distincts pour la même donnée conceptuelle. Absent
côté Forge, où `CRods` ne vit que dans le config serveur. `ReactorBluePrintMenu` lisait la
copie `COMMON`, `RodType`/`CNItems` la copie `SERVER` : un admin modifiant l'une sans l'autre
aurait désynchronisé silencieusement la durée de vie affichée sur le blueprint de celle
réellement utilisée par le calcul de chaleur. Le champ dupliqué a été retiré de `CNCCommon`,
`ReactorBluePrintMenu` lit désormais `CNConfigs.server().rods`, comme le reste du code.

> **Différence assumée entre Forge et NeoForge, à ne pas « corriger » lors d'une synchro.**
> Forge stocke le blueprint en NBT brut sur l'`ItemStack` (`getOrCreateTag().put("pattern", ...)`).
> NeoForge le stocke en Data Component typé (`ReactorBluePrintData`, `Codec` + `StreamCodec`).
> Ce n'est pas un écart à réduire : NBT muté en place sur un `ItemStack` casse silencieusement
> en 1.21 (voir §5.1, `copyTag()` renvoie une copie défensive), et les Data Components sont la
> direction que 1.21 impose pour ce genre de donnée structurée. Une synchro Forge → NeoForge sur
> `bluePrintItem/` ne doit **jamais** réintroduire de `CompoundTag`/`getOrCreateTag()` ; elle doit
> traduire chaque nouveau champ NBT Forge en champ du record `ReactorBluePrintData`, avec son
> propre `Codec`/`StreamCodec`, sur le modèle de `PatternData`.
>
> Différence secondaire assumée : le paquet réseau `ReactorBluePrintItemPacket` que Forge
> utilise n'a pas d'équivalent côté NeoForge, par choix, pas par oubli — voir le lot 2bis
> ci-dessus. S'il réapparaît un jour côté Forge avec un vrai usage (affichage de statistiques),
> ne pas le traduire tel quel : passer par la synchronisation de menu vanilla
> (`ContainerData`/`DataSlot`) plutôt que par un paquet personnalisé.

**Ce qu'il reste, honnêtement :** aucun bug connu à ce stade sur `bluePrintItem/` après cette
passe. Le point réellement ouvert est la **couverture de test** : les 29 gametests ne couvrent
pas le cas qui a produit le bug du thorium (un `RodType` enregistré sans le tag `CNItemTags`
correspondant). Ajouter un cas de ce genre permettrait à une future régression similaire
d'échouer un test plutôt que de n'être trouvée qu'en jeu.

### Lot 3 — Services (`service/`, 12 fichiers sur 14) ✅ FAIT
`IHeatService` / `DefaultHeatService` · `IExplosionService` / `ReactorMeltdownExecutor` · `IReactorMeltdownMonitor` / `ReactorMeltdownMonitor` · `IReactorAlarmCoordinator` / `ReactorAlarmCoordinator` · `IReactorHeatUpdateCoordinator` / `ReactorHeatUpdateCoordinator` · `IFluidConsumptionRateCalculator` / `FluidConsumptionRateCalculator`

> **Le piège de §5.1 est neutralisé.** `CNDataComponents.HEAT` (`DataComponentType<Float>`)
> **existait déjà** côté NeoForge — inutile d'en créer un. `ReactorHeatUpdateCoordinator`
> écrit désormais via `configuredPattern.set(CNDataComponents.HEAT, (float) heat)`, dans une
> méthode `writeHeat()` dédiée, au lieu de muter `getOrCreateTag()`. Le test de pattern vide
> passe par `isEmptyPattern()`, qui vérifie l'absence de `ReactorBluePrintData`.
>
> Hors javadoc, ce sont les **seules** différences avec l'original Forge : 9 des 10 autres
> fichiers sont identiques à l'octet.

> **Reporté au lot 6 :** `IPersistenceService` et `DefaultPersistenceService`. Ils appellent
> des accesseurs que le `ReactorControllerBlockEntity` NeoForge actuel n'a pas encore
> (`setMultiblockFacing(Direction)` vs `String`, `getMultiblockPos()` en `BoundingBox` vs
> `int[]`, `serializeInventory`, `get/setDisplayState`). Les porter maintenant obligerait à
> réécrire le BE, c'est-à-dire à faire le lot 6 — ils partiront donc avec lui.

### Lot 4 — Affichage & snapshot (6 fichiers, ~450 lignes) ✅ FAIT
`snapshot/ReactorInputSnapshot` · `snapshot/ReactorInputSnapshotBuilder` · `display/ReactorDisplayState` (porté au lot 1) · `display/ReactorGoggleTooltipRenderer` · `manager/ReactorFrameDisplayManagerI` · `manager/ReactorFrameDisplayManager`

`ReactorFrameDisplayManager` remplace les champs `frameFluidCache*` / `frameColumn*` actuellement inlinés dans le BE NeoForge — c'est une extraction, la logique existe déjà et est équivalente. Le BE ne sera débarrassé de ses copies qu'au lot 6.

> Aucune surprise sur ce lot : `ReactorInputSnapshot` est identique à l'octet, les trois
> autres ne diffèrent que par les imports `minecraftforge` → `neoforged`, plus
> `FluidStack.getDisplayName()` → `getHoverName()` dans le renderer.
>
> Le javadoc d'exemple de `ReactorGoggleTooltipRenderer` a été corrigé au passage : côté
> Forge il montrait un appel à `render()` avec 4 arguments alors que la méthode en prend 5.

### Lot 5 — Divers contrôleur (2 fichiers, ~100 lignes) ✅ FAIT
`ReactorRunningSoundInstance` · `ReactorDebugDiagnostics`

> Les deux fichiers sont **identiques à l'octet** — aucune adaptation d'API. `AbstractTickableSoundInstance`
> n'a pas bougé entre 1.20.1 et 1.21, et `ReactorAlarmSoundInstance` (déjà porté) sert de témoin.
>
> En revanche les 8 clés `createnuclear.reactor.debug.*` manquaient dans
> `lang/default/reactor.json` — ajoutées. Les 9 clés `notification.reactor.*` dont dépend le
> `ReactorMeltdownMonitor` du lot 3, elles, existaient déjà. `runData` est nécessaire pour
> répercuter les nouvelles clés dans `en_us.json`.

### Lot 6 — Réécriture du block entity ✅ FAIT (lot 7 inclus)
`ReactorControllerBlockEntity` passe de **868 lignes monolithiques à 424 lignes** qui délèguent
aux 33 fichiers des lots 0-5. Divergence vs Forge : **864 lignes → 76**, toutes des adaptations
1.21 (`HolderLookup.Provider`, DataComponents, imports `neoforged`).

Fichiers portés avec : `ReactorControllerBlock` · `ReactorAssembler` · `CNMultiblock` ·
`ReactorPattern` · `MultiblockHelpers` · `ReactorOutput` · `ReactorOutputEntity` ·
`ReactorOutputManager(I)` · `ReactorFrame` · `ReactorFrameRenderer` · `ReactorCasing` ·
`ReactorCooler` · `ReactorCoreEntity` · `ReactorRodInput` · `IPersistenceService` /
`DefaultPersistenceService` · `IMultiblockController` (lot 7).

> **Features réellement absentes de NeoForge, découvertes en câblant :**
>
> 1. **Seul le réacteur 5×5 existait.** `CNMultiblock` n'enregistrait qu'un multiblock, avec
>    entrée et sortie à positions fixes. Forge en enregistre trois (5×5 / 7×7 / 9×9) où les I/O
>    se placent librement sur l'enveloppe. Les 7×7 et 9×9 étaient donc **impossibles à assembler**.
> 2. **`ReactorFrameEntity` n'était jamais enregistré.** La classe et son renderer existaient,
>    mais sans `BlockEntityEntry` dans `CNBlockEntityTypes` : le bloc frame n'avait aucune block
>    entity, donc le rendu du fluide dans les vitres ne pouvait pas fonctionner. Ajouté.
> 3. **La capacité de fluide était multipliée par le nombre d'entrées.** `applyReactorTierCapacity`
>    donnait à *chaque* entrée la capacité totale du réacteur ; 4 entrées = 4× la capacité prévue.
>    Remplacé par `applyCapacity(int)`, l'assembleur répartissant le total comme sur Forge.
> 4. **`ReactorOutputEntity` utilisait un `KineticScrollValueBehaviour`** (vitesse réglable à la
>    molette) au lieu du `generatedSpeed` persisté piloté par le manager. Remplacé, ce qui
>    supprime aussi l'interaction `DIR` sur `ReactorOutput` que Forge avait retirée.
>
> **Piège évité :** Forge déclare `int heat = ...` dans `tick()` en variable **locale masquant le
> champ** — donc morte. Une lecture rapide pousse à en faire une affectation du champ, ce qui
> changerait la valeur de `previousHeat` passée au calcul. La sémantique Forge est conservée.

**Divergences résiduelles assumées** (à ne pas « corriger » lors d'une synchro) :
`CNShapes.REACTOR_INPUT` et `CNBlockEntityTypes.REACTOR_INPUT` gardent leur nom NeoForge
(Forge : `REACTOR_ROD_INPUT`) — les renommer changerait l'identifiant de registre
`createnuclear:reactor_input` et casserait les mondes existants.

### Lot 9 — Corrections issues du test en jeu ✅ FAIT

Six défauts trouvés en jouant, tous corrigés. Cinq étaient des divergences NeoForge
préexistantes, un était une erreur introduite par le portage.

| Symptôme en jeu | Cause | Origine |
|---|---|---|
| Crash à la sauvegarde du monde | `CNDataComponents.HEAT` en `ExtraCodecs.POSITIVE_FLOAT`, qui refuse `0.0` — or un réacteur à l'arrêt écrit 0 à chaque tick | **introduit au lot 3** |
| Fluide quasi noir dans les vitres | `reactor_frame` sans `.noOcclusion()` : bloc traité comme opaque plein, la lumière ne passe plus | préexistant |
| idem | renderer passant un `FluidState` au lieu du `FluidStack`, ce qui perd la teinte | introduit au lot 6 |
| 2 slots dans le rod input | `super(2)` avec uranium/graphite figés, au lieu d'un slot acceptant toute barre via `RodType` | préexistant |
| Explosion malgré un pattern stable | azote liquide **commenté** dans `CNReactorFluidTypes` → pas de `ReactorFluidType` → `efficiency = -1` → `fluidMalus` permanent → surchauffe infinie | préexistant |
| 81 920 SU au lieu de 512 000 | capacité de stress du `REACTOR_OUTPUT` à `10240.0` au lieu de `64000.0` (rapport 6,25 ✓) | préexistant |

> **Leçon sur les codecs :** un codec **persistant** qui peut refuser une valeur échoue au moment
> de la **sauvegarde**, très loin de l'écriture fautive, et emporte le block entity avec lui.
> Les invariants de domaine se valident en amont, pas dans le codec.
> Le test `heatComponent_atZero_survivesSerialization` verrouille ce cas.

**Trouvé en vérifiant :** Forge a **5 appels à `addLayer`**, NeoForge n'en avait qu'**un**.
Quatre blocs avaient perdu leur couche de rendu — dont `reinforced_glass` en `translucent`,
qui rendait donc **opaque**. Tous rétablis.

---

## 5. Pièges de traduction 1.20.1 Forge → 1.21 NeoForge

### 5.1 ⚠️ Le piège majeur : `getConfiguredPatternTag()` renvoie une copie

Côté Forge, la chaleur est **écrite dans le tag de l'ItemStack** :

```java
configuredPattern.getOrCreateTag().putDouble("heat", heat);   // mute en place
```

Côté NeoForge, l'implémentation actuelle est :

```java
public CompoundTag getConfiguredPatternTag() {
    return this.configuredPattern
        .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        .copyTag();          // ⚠️ COPIE
}
```

`copyTag()` renvoie une copie défensive. **Tout `put*` dessus est silencieusement perdu.** Traduire `ReactorHeatUpdateCoordinator.updateHeatOnly()` / `calculateAndWriteHeat()` littéralement produirait un réacteur dont la chaleur reste bloquée à 0, sans aucune erreur de compilation ni exception à l'exécution.

Deux options :

- **A — pansement** : après modification, réécrire le composant
  `stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))`.
  Rapide, mais conserve la sémantique « NBT brut » que 1.21 cherche à supprimer.
- **B — propre (recommandé)** : introduire un `ReactorHeatData` (ou étendre `ReactorBluePrintData`, qui existe déjà comme record côté NeoForge) et remplacer les accès `tag.getDouble("heat")` par un accès typé.

Option B est plus de travail mais s'aligne sur la direction déjà prise par le repo NeoForge, qui utilise `CNDataComponents` / `ReactorBluePrintData` / `PatternData`. Mélanger les deux approches est le vrai risque : `PatternReader` (Forge) lit `tag.getCompound("patternAll")`, alors que NeoForge stocke déjà le pattern dans `ReactorBluePrintData.patternAll()` sous forme de `PatternData[]`.

**→ `PatternReader` ne doit pas être traduit ligne à ligne. Il doit être réécrit pour lire `ReactorBluePrintData`.**

### 5.2 Correspondances mécaniques

| Forge 1.20.1 | NeoForge 1.21 |
|---|---|
| `net.minecraftforge.items.IItemHandler` | `net.neoforged.neoforge.items.IItemHandler` |
| `net.minecraftforge.fluids.FluidStack` | `net.neoforged.neoforge.fluids.FluidStack` |
| `net.minecraftforge.api.distmarker.OnlyIn` | `net.neoforged.api.distmarker.OnlyIn` |
| `ForgeRegistries.ITEMS.getKey(item)` | `BuiltInRegistries.ITEM.getKey(item)` |
| `ForgeRegistries.ITEMS.getValue(rl)` | `BuiltInRegistries.ITEM.get(rl)` |
| `new ResourceLocation(s)` | `ResourceLocation.parse(s)` |
| `read/write(CompoundTag, boolean)` | `read/write(CompoundTag, HolderLookup.Provider, boolean)` |
| `handler.deserializeNBT(tag)` | `handler.deserializeNBT(provider, tag)` |
| `ItemStack.of(tag)` | `ItemStack.parse(provider, tag)` → `Optional` |
| `stack.serializeNBT()` | `stack.saveOptional(provider)` |
| `CODEC.encodeStart(..).getOrThrow(false, X::new)` | `CODEC.encodeStart(..).getOrThrow()` |

`ReactorDisplayState` et `DefaultPersistenceService` concentrent la majorité de ces changements, car ils sérialisent tout.

### 5.3 `FluidStack` n'est plus nullable de la même façon

En 1.21, `FluidStack` porte des `DataComponents` et `FluidStack.EMPTY` se compare avec `isEmpty()`, jamais avec `==`. Le code Forge de `ReactorFrameDisplayManager` fait `fluid.isEmpty()` — correct — mais `stack.copy()` conserve désormais les composants, ce qui est le comportement voulu.

---

## 6. Volumétrie

| Lot | Fichiers | Lignes (Forge) | Difficulté |
|---|---|---|---|
| 0 — Prérequis | 5 (2 créés, 3 modifiés) | ~150 | moyenne |
| 1 — reactorLogic | 7 | 217 | faible (logique pure, peu d'API MC) |
| 2 — consumable | 6 | 317 | **élevée** (`PatternReader` à réécrire) |
| 3 — service | 14 | 536 | moyenne (persistence = élevée) |
| 4 — display/snapshot | 6 | 450 | **élevée** (sérialisation) |
| 5 — divers | 2 | 100 | faible |
| 6 — BlockEntity | 1 | 440 | **élevée** |
| 7 — api multiblock | 1 | ~30 | faible |
| **Total** | **42** | **~2 240** | |

Les lots 1, 3 et 5 sont essentiellement de la traduction d'imports. Les lots 2, 4 et 6 demandent de vraies décisions d'architecture (DataComponents vs NBT).

---

## 7. Recommandation d'ordre d'exécution

1. **Lot 0** — sans lui rien ne compile.
2. **Décision DataComponents vs NBT** (§5.1) — elle conditionne les lots 2, 4 et 6. À trancher avant d'écrire une ligne du lot 2.
3. **Lots 1 → 5** dans l'ordre, en compilant après chaque lot.
4. **Lot 6** en dernier : le BE ne compile qu'une fois toutes ses dépendances présentes.
5. **Lot 7** au choix, indépendant.

Les deux gametests Forge (`DefaultHeatCalculatorGameTest`, `ReactorInputFluidManagerGameTest`) sont à porter avec les lots 1 et 3 respectivement : ils valident précisément la logique la plus risquée.

### Lot 8 — Gametests ✅ FAIT

`./gradlew runGameTestServer` — **29 tests, tous verts** (`EXIT=0`).

Les 4 tests du modèle thermique passent, dont `threeByThreeDiamond` croisé avec le calculateur
du wiki communautaire. C'est la première validation à l'exécution du portage.

> À l'écriture du lot, la suite comptait 31 tests dont 2 échouant volontairement : les marqueurs
> `*_expectedContract` du bug `extractFluids`, qui échouait identiquement sur Forge. Ce bug est
> corrigé depuis dans les deux repos (§9.4), les marqueurs sont donc passés au vert et les deux
> tests qui figeaient le comportement bogué ont été supprimés.

**Adaptations du portage :**
- `loadPattern` écrit un `ReactorBluePrintData` typé (57 `PatternData`) au lieu du tag NBT `pattern`.
- Capabilities via `level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null)`.
- `ForgeRegistries.FLUIDS` → `BuiltInRegistries.FLUID`, annotations `net.neoforged.neoforge.gametest`.
- La structure va dans `data/createnuclear/structure/` (**singulier** en 1.21, `structures` en 1.20.1).

**Bug NeoForge corrigé au passage** (absent de Forge) : `ReactorInputFluidManager` lisait
`getFluidInTank(handler.getTanks())` — un index hors bornes — dans `getInventory()` **et**
`extractFluids()`. Corrigé en `getFluidInTank(0)`. Un `LOGGER.warn` de debug oublié dans
`getBlocksPosition()`, qui spammait à chaque appel, a aussi été retiré.

---

## 8. Hors périmètre de ce rapport

Le reste du mod présente aussi des écarts. Inventaire complet en §9.

---

## 9. Ce qui n'est PAS porté de Forge vers NeoForge

> 📄 **Cette section est reprise et approfondie dans [`PARITE_FORGE_NEOFORGE.md`](PARITE_FORGE_NEOFORGE.md)**,
> qui détaille pour chaque feature manquante ses points d'accroche, sa difficulté et l'ordre de
> travail conseillé. Le présent document reste centré sur le réacteur.

État au 6 août 2026, branche `V2-Reacteur`. Le comptage brut donne **42 fichiers Forge sans
équivalent NeoForge**, mais 6 sont en réalité présents sous un autre nom — le vrai reste est
**36 fichiers**, plus des écarts internes sur des fichiers partagés.

### 9.1 Faux positifs — présents mais renommés ou déplacés

Ne pas les reporter comme manquants lors d'une synchro :

| Forge | NeoForge |
|---|---|
| `content/multiblock/rod/CNRodTypes` | `content/rod/CNRodTypes` |
| `foundation/damageTypes/CNDamageSources` | `foundation/damages**T**ypes/CNDamageSources` |
| `content/equipment/armor/ArmorMaterials` | `CNArmorMaterials` |
| `infrastructure/config/CExplode` | `CExplose` *(faute de frappe côté Neo)* |
| `input/fluid/PlayerInteract**R**eactorFluidInput` | `PlayerInteract**e**ReactorFluidInput` *(faute de frappe)* |
| `irradiated/cat/CatLieOnBedGoal`, `CatSitOnBlockGoal` | `IrradiatedCatLieOnBedGoal`, `IrradiatedCatSitOnBlockGoal` |

### 9.2 Features entièrement absentes

**① Jauges DisplayLink du réacteur — 9 fichiers** ← *le manque le plus visible en jeu*

`CNDisplaySources` · `AbstractReactorStatDisplaySource` · `HeatDisplaySource` ·
`FuelDisplaySource` · `CoolerDisplaySource` · `LiquidLevelDisplaySource` ·
`ReactorSizeDisplaySource` · `ReactorDisplayConstants` · `ReactorGaugeRenderer`

NeoForge n'a que `ReactorSummary` + `ReactorSummaryDisplaySource`. Côté Forge, `CNBlocks`
attache en plus au contrôleur `.transform(displaySource(CNDisplaySources.HEAT))`,
`LIQUID_LEVEL`, etc. — **impossible de brancher un Display Link sur une statistique précise
du réacteur** côté NeoForge. Le code réacteur nécessaire (`ReactorDisplayState`,
`getMultiblockSize`, `getInputFluidManager`) est désormais en place, donc c'est un portage
direct sans prérequis.
*NeoForge a en plus `ReactorGaugeOverrides`, qui n'existe pas côté Forge.*

**② Poudre de neige (Snow Powder) — 4 fichiers**

`SnowPowderRecipe` · `SnowPowderRecipeGen` · `CNSnowPowderRecipeGen` · `FanSnowPowderCategory` (JEI)

Recette de ventilateur complète, avec sa catégorie JEI. Rien de tout ça côté NeoForge.

**③ Vache irradiée + abstraction des animaux — 5 fichiers**

`IrradiatedCow` · `IrradiatedCowModel` · `IrradiatedCowRenderer` · `IrradiatedAnimal` · `AnimalUtil`

NeoForge a le chat, le loup et le poulet, mais **pas la vache**. Il lui manque aussi la classe
de base `IrradiatedAnimal` et l'utilitaire `AnimalUtil` : ses animaux dupliquent donc la
logique que Forge a factorisée. C'est le dossier le plus divergent hors réacteur
(`IrradiatedCat` 384 lignes d'écart, `IrradiatedWolf` 333).

**④ Worldgen piloté par config — 2 fichiers**

`CNPlacementModifiers` · `ConfigPlacementFilter`

Filtre de placement lisant la config : sans lui, les options de génération de minerai
(`CWorldGen`) ne sont probablement pas respectées.

**⑤ Datagen de recettes — 2 fichiers**

`CNDeployingRecipeGen` · `CNProcessingRecipeGen` — des recettes existent donc côté Forge
et pas côté NeoForge.

**⑥ Radiation — 2 fichiers**

`IRadiationCapability` · `RadiationProvider`

Attendu : NeoForge 1.21 remplace les capabilities Forge par les *data attachments*.
`RadiationCapability` existe et diverge de 186 lignes. **À vérifier fonctionnellement**
plutôt qu'à porter à l'identique.

**⑦ Compat externe — 2 fichiers**

`Mods` · `AlexscaveCompat` — intégration Alex's Caves absente.

**⑧ Mixins client — 2 fichiers**

`CameraAccessor` · `GameRendererMixin` — effet visuel (probablement lié au shader de radiation).
NeoForge a à la place `AntiRadiationArmorTextureMixin`, qui n'existe pas côté Forge.

**⑨ Divers — 6 fichiers**

`CNOpenPipeEffectHandlers` (effets des tuyaux ouverts) · `UraniumOreItem` ·
`CriterionTriggerBase` (base des advancements custom) · `RodsTooltipHandler` +
`RodsStats` (tooltips détaillés des barres) · `SimplexNoise`

**⑩ Extracteur d'irradiation de biome — 1 fichier**

`BiomeIrradationExtractorItem`. ⚠️ **Déjà porté en amont** : les 4 commits d'`origin/V2` que
cette branche n'a pas encore intégrés l'ajoutent sous le nom `BiomeIrradiationExtractorItem`.
Il apparaîtra automatiquement au merge.

### 9.3 Portés mais non audités

Ces fichiers existent des deux côtés mais divergent fortement. Un gros diff n'est **pas**
une preuve de feature manquante — l'API 1.21 en explique une grande part. Aucun n'a été
audité ligne à ligne, contrairement au domaine réacteur.

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

Le lot 9 a montré que ces écarts cachent de vrais bugs : la capacité de stress et les
`addLayer` manquants étaient tous les deux dans `CNBlocks`.

### 9.4 Bug `extractFluids` ✅ CORRIGÉ dans les deux versions

`ReactorInputFluidManager.extractFluids` ne décrémentait jamais `fluidNeeded` entre les
handlers : le montant demandé était traité comme un quota **par entrée** au lieu d'un total.
Un réacteur demandant 10 unités avec deux entrées de 10 les vidait toutes les deux, soit 20
retirées. Plus le joueur posait d'entrées, plus le caloporteur disparaissait vite.

Un garde `if (toExtract > 1)` avalait aussi les demandes d'exactement 1 unité — précisément
ce que demande `FluidConsumptionRateCalculator` en bas de la courbe de consommation.

Corrigé à l'identique dans les deux repos (implémentations byte-identical). Les deux tests
`*_expectedContract` passent, les deux tests qui figeaient le comportement bogué sont
supprimés, et le javadoc — qui promettait « true si le montant complet a été extrait » alors
que le code renvoyait true sur toute extraction partielle — documente désormais le vrai
contrat.

**`runGameTestServer` sort maintenant en code 0 des deux côtés** (Forge 28 tests,
NeoForge 29) : il peut servir de garde-fou CI.

### 9.5 Ordre suggéré pour la suite

1. ~~**Merger `origin/V2`**~~ ✅ fait — un seul conflit, l'import prévu dans `CNItems`.
2. **Jauges DisplayLink** — le plus visible, sans prérequis, le réacteur expose déjà tout.
3. ~~**Corriger `extractFluids`**~~ ✅ fait dans les deux repos.
4. **Worldgen config** — silencieux mais fausse la génération de minerai.
5. **Vache irradiée + `IrradiatedAnimal`/`AnimalUtil`** — permet aussi de dédupliquer chat/loup/poulet.
6. Snow Powder, datagen manquant, compat Alex's Caves, mixins client.
7. **Auditer `CNBlocks` et `CNItems`** en dernier, mais ne pas l'oublier : c'est là qu'étaient
   cachés deux des six bugs du lot 9.