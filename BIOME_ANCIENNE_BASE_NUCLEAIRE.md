# Biome « Ancienne base nucléaire » — Document de conception

> Document généré à partir d'une session de conception. Rien n'est encore implémenté : ce fichier
> décrit l'architecture retenue, les mécanismes Minecraft utilisés et les contraintes identifiées
> avant d'écrire le code.

---

## Objectif

Aujourd'hui, le biome `createnuclear:irradiated_land` n'apparaît **que** lorsqu'un réacteur explose
(conversion à l'exécution via `BiomeIrradiationService`). On veut en plus un biome radioactif
**généré naturellement avec le monde** : une ancienne base nucléaire qui aurait explosé, comme si
un autre joueur était passé avant.

- Une zone irradiée avec son ambiance propre (couleurs, brouillard, particules, son, mobs, radiation).
- Un **cratère** au centre de la zone, avec les **ruines de la base**.
- **Plusieurs variantes** pour que deux bases ne se ressemblent jamais.

Contexte technique : Minecraft 1.21.1, NeoForge 21.1.x, worldgen enregistré par datagen
(`CNBiomes.bootstrapRegistries`, `GeneratedEntriesProvider`).

---

## Architecture retenue

Deux mécanismes complémentaires :

| Responsable | Rôle |
|---|---|
| **TerraBlender** (biome) | L'**environnement** : couleurs, brouillard, particules, son d'ambiance, surface, mobs irradiés, niveau de radiation. Il définit **où** la zone existe. |
| **Structure custom** (Java + jigsaw) | Le **cratère procédural** et les **ruines de la base**, placés à l'intérieur du biome. |

```
Monde ──► TerraBlender place le biome ancient_fallout (rare, plat, stérile)
              │
              └──► AncientReactorStructure (limitée au biome par tag)
                      1. vérifie que toute la zone du cratère est dans le biome
                      2. pièce « cratère » procédurale (Java)
                      3. JigsawPlacement → ruines .nbt (variantes pondérées + processors)
```

---

## 1. Le biome : TerraBlender

### Pourquoi une librairie ?

Depuis la 1.18, l'Overworld utilise une `MultiNoiseBiomeSource` : chaque biome occupe une zone d'un
espace climatique à 6 dimensions (température, humidité, continentalité, érosion, weirdness,
profondeur). Cette liste est figée dans le preset `minecraft:overworld`. NeoForge ne fournit
**aucun** moyen propre d'y ajouter un biome : les `BiomeModifiers` (déjà utilisés pour les
minerais, `CNBiomeModifiers`) modifient le contenu d'un biome existant, pas sa présence.

| Option | Principe | Avis |
|---|---|---|
| **TerraBlender** | Ajoute des « Regions » pondérées, copies de la carte climatique dans lesquelles on remplace des zones par nos biomes. Fournit aussi `SurfaceRuleManager`. | ✅ Standard (BoP, Regions Unexplored…), compatible avec les autres mods |
| Biolith | Même principe, plus léger | Viable, moins répandu |
| Surcharger le preset Overworld | Réécrire toute la liste des biomes | ❌ Incompatible avec tout autre mod de worldgen |

### Nouveau biome plutôt que réutiliser `irradiated_land`

Nouveau biome **`createnuclear:ancient_fallout`**, construit avec le même builder
`IrradiatedBiomes.irradiated(...)`, pour pouvoir :

- lui donner une **radiation propre** dans `RadiationRegistry` (explosion ancienne, donc moins
  violente qu'un meltdown récent) ;
- gérer la **décontamination** à part : `BiomeIrradiationMapping` / `CBiomeRestore` supposent un
  biome d'origine, ce qu'un biome naturel n'a pas ;
- réserver la structure à ce biome via un **tag**, sans qu'elle apparaisse dans les zones de meltdown ;
- y faire apparaître des **mobs irradiés** (actuellement `spawnBuilder` est vide dans `IrradiatedBiomes`).

### Paramètres climatiques visés

- **Rare** : faible poids de région et petite niche climatique.
- **Plat** : érosion élevée, pour que la base ne se retrouve pas à flanc de montagne.
- **Stérile** : pas d'arbres et très peu de végétation. Sinon les features de végétation, posées
  après les structures, viendraient s'incruster dans le cratère.

---

## 2. La structure

### 2.1 Type de structure custom : `AncientReactorStructure`

Par défaut, Minecraft vérifie le biome **uniquement au point de départ** d'une structure. Elle peut
donc naître au bord du biome, et le cratère déborde alors sur une forêt vanilla.

Solution : surcharger `findGenerationPoint` pour vérifier le biome sur un cercle autour du centre
(par exemple 8 points au rayon du cratère). Si un point n'est pas dans `ancient_fallout`, rien n'est
généré à cet endroit.

En complément, on règle un `spacing` serré dans le `structure_set` pour que la plupart des zones
contiennent une base. Les zones sans cratère restent crédibles : ce sont des retombées portées par
le vent.

### 2.2 Le cratère : pièce procédurale (Java)

Une `StructurePiece` calculée par code plutôt qu'un `.nbt` :

- cuvette de rayon et de profondeur aléatoires, bords irréguliers (bruit) ;
- remplacement des blocs de surface par des blocs brûlés, du verre fondu, des gravats ;
- peut dépasser 48 blocs de large (limite d'un `.nbt`) ;
- chaque cratère est unique.

`postProcess` est appelé **chunk par chunk** avec une bounding box : la pièce doit se découper
proprement pour éviter les cratères tronqués.

### 2.3 Les ruines : système Jigsaw (data-driven)

Après le cratère, la structure appelle `JigsawPlacement.addPieces(...)` pour poser les ruines au fond.

Trois niveaux de diversité qui se combinent :

1. **Template pool pondéré** : N bâtiments centraux différents, chacun avec un poids.
2. **Jigsaw blocks** : des connecteurs qui piochent au hasard des annexes (tour de refroidissement,
   bunker, débris…) ou une pièce `empty`. `size` (profondeur max 20) limite l'expansion.
3. **Processors** : dégradation aléatoire au placement (`stone_bricks` → `cracked` à X %,
   `block_rot`, remplacement par des gravats…). Deux instances du même `.nbt` ne sont jamais identiques.

S'y ajoutent la rotation et le miroir aléatoires, qui sont gratuits.

---

## 3. Fichiers à produire

```
data/createnuclear/
├── structure/ancient_reactor/                ← .nbt construits en jeu (structure blocks)
│   ├── core_a.nbt, core_b.nbt, core_c.nbt        variantes du bâtiment central
│   └── annex_bunker.nbt, cooling_tower_ruin.nbt, debris_1.nbt …
├── worldgen/
│   ├── biome/ancient_fallout.json             ← datagen (CNBiomes)
│   ├── structure/ancient_reactor.json         ← type custom, biomes, step
│   ├── structure_set/ancient_reactor.json     ← rareté : spacing / separation / salt
│   ├── template_pool/ancient_reactor/
│   │   ├── start.json                         ← variantes centrales pondérées
│   │   └── annexes.json                       ← pièces optionnelles
│   └── processor_list/ancient_decay.json      ← dégradation aléatoire
├── tags/worldgen/biome/has_structure/ancient_reactor.json
└── loot_table/chests/ancient_reactor.json
```

Côté Java :

- `CNBiomes` : nouvelle clé `ancient_fallout` et son bootstrap.
- Une `Region` TerraBlender et ses règles de surface (réutiliser `IrradiatedSurfaceRules` si possible).
- `AncientReactorStructure` (type de structure) et son `StructureType` enregistré.
- `CraterPiece` (pièce procédurale) et son `StructurePieceType` enregistré.
- Bootstraps datagen pour `Structure`, `StructureSet`, `TemplatePool`, `ProcessorList`.
- `RadiationRegistry` : valeur de radiation du biome.
- Dépendance TerraBlender dans `build.gradle` et `neoforge.mods.toml`.

Seuls les `.nbt` sont faits à la main. Le code peut être livré avec des `.nbt` provisoires que
l'équipe remplacera.

---

## 4. Contraintes et points d'attention

### Ordre de génération

```
biomes → bruit (terrain) → surface (règles du biome) → carvers → features (structures, puis végétation)
```

- Le cratère est creusé **après** la surface : ses parois montrent la pierre et la terre, ce qui est
  cohérent avec une explosion. C'est la pièce cratère qui pose les blocs brûlés.
- La végétation est posée **après** les structures : d'où l'importance d'un biome stérile.

### Techniques

- **⚠️ Block entities du mod dans les `.nbt`** : un contrôleur de réacteur ou des pièces de
  multibloc pourraient tenter de s'assembler, de tourner, voire déclencher
  `ReactorMeltdownExecutor` au chargement. Il faut des blocs **purement décoratifs** ou des block
  entities vérifiées « mortes ». Même vigilance pour les blocs cinétiques de Create.
- **Renommage de blocs** : si un ID change, les `.nbt` cassent silencieusement (le bloc devient de
  l'air). Il n'y a pas de DataFixer pour les mods.
- **Taille** : 48×48×48 par `.nbt`, `max_distance_from_center` ≤ 128, `size` ≤ 20.
- **Mondes existants** : biome et structure n'apparaissent que dans les **nouveaux chunks**, d'où
  des frontières nettes dans les mondes déjà explorés.
- **Compatibilité** : les mods qui remplacent le bruit vanilla (Terralith, Tectonic…) peuvent rendre
  le biome rare ou absent.

### Gameplay (à décider)

- Le niveau de radiation du biome et l'équipement nécessaire pour l'explorer.
- Le loot qui motive l'expédition (uranium, composants abîmés, plans…).
- La rareté du biome et la fréquence des bases.
- **La décontamination** : peut-on « soigner » ce biome, et vers quel biome revient-il ?

### Production

Le plus gros du travail est la **construction en jeu des variantes**, pas le code. Il faut viser au
moins 3 bâtiments centraux et 4 à 6 annexes.

---

## 5. Tests

| Commande | Usage |
|---|---|
| `/locate biome createnuclear:ancient_fallout` | Trouver le biome |
| `/locate structure createnuclear:ancient_reactor` | Trouver une base |
| `/place structure createnuclear:ancient_reactor` | Générer une base complète ici |
| `/place template createnuclear:ancient_reactor/core_a` | Tester une pièce `.nbt` isolée |
| `/place jigsaw createnuclear:ancient_reactor/start <cible> <profondeur>` | Tester l'assemblage jigsaw |

---

## 6. Plan d'implémentation

1. Ajouter TerraBlender et enregistrer le biome `ancient_fallout` (Region, règles de surface,
   radiation, mobs).
2. Créer `AncientReactorStructure` avec la vérification du biome sur le rayon.
3. Créer `CraterPiece` (cratère procédural).
4. Brancher le jigsaw : pools, processors, structure_set, tag de biome, loot table, avec des `.nbt`
   provisoires.
5. Remplacer les `.nbt` provisoires par les vraies variantes construites en jeu.
6. Équilibrage : rareté, radiation, loot, décontamination.
