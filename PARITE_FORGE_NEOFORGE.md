# Parité Forge → NeoForge — ce qu'il reste à porter

Ce document liste tout ce qui sépare encore `CreateNuclearNeoForge` de `CreateNuclearForge`
en termes de **features**, et comment porter chaque bloc.

**Ce fichier ne contient que du travail restant.** Ce qui est fait en sort — l'historique
est dans les commits.

État au **16 août 2026**, branche `V2`, après le portage de la compat Alex's Caves,
des mixins client, de plusieurs refactors (RadiationCapability en attachment, overlay HUD,
routage des dégâts), du correctif de la fuite de namespace `create` (§3.1), et du commit
`7d16431` (« add the IrradiatedAnimal abstraction, wire it into the chicken, and align the cat
with vanilla Cat ») qui livre le dernier chantier de parité listé ici — voir §2.1.
Pour le domaine réacteur, déjà porté et validé, voir [`PORTAGE_REACTEUR.md`](PORTAGE_REACTEUR.md).
Pour le sous-dossier `content/multiblock/controller`, audité ligne à ligne, voir §7.

| | Forge | NeoForge |
|---|---|---|
| Fichiers `.java` | 296 | 300 |
| Ressources | 1 132 | 1 132 |

---

## 0. Comment lire ce document

Un `diff` brut des arborescences donne **6 fichiers Forge sans équivalent NeoForge**
(15 le 7 août ; 11 le 14 août ; 7 le 15 août — la compat Alex's Caves, les mixins client, la
vache irradiée, `AnimalUtil` et enfin `IrradiatedAnimal` ont été portés depuis). Ce chiffre est
trompeur :

- **3 sont remplacés par un équivalent 1.21 meilleur** → §1.1, à ne surtout pas « re-porter » ;
- **3 existent sous un autre nom ou dans un autre package** → §1.2, idem ;
- **0 manque réellement** → §2, le dernier chantier (`IrradiatedAnimal`) a été livré le 16 août.

S'y ajoutent des écarts *à l'intérieur* de fichiers présents des deux côtés (§3), invisibles
d'un diff d'arborescence — et c'est là qu'ont été trouvés la moitié des bugs du réacteur.

> **Le piège récurrent, vérifié deux fois.** Un fichier absent n'est pas le seul symptôme d'une
> feature manquante. À deux reprises, la classe qui « manquait » n'était qu'un **point
> d'enregistrement** : le reste de la feature existait déjà côté NeoForge, complet et correct,
> mais **n'était référencé nulle part** — donc entièrement mort en jeu.
> **Avant de porter quoi que ce soit, chercher les classes NeoForge jamais référencées.**

---

## 1. À ne pas porter — l'équivalent existe déjà

**Ne pas recréer ces fichiers.** Le `diff` d'arborescence du §8 les listera toujours.

### 1.1 Volontairement non portés — l'équivalent 1.21 est meilleur

| Fichier Forge | Remplacé côté NeoForge par |
|---|---|
| `content/radiation/capability/IRadiationCapability` | **Data attachments NeoForge** — `RadiationCapability` expose `AttachmentType<RadiationCapability> RADIATION`, lu via `entity.getData(RADIATION)` |
| `content/radiation/capability/RadiationProvider` | idem — la plomberie `ICapabilityProvider` de Forge n'a pas d'équivalent, ni de raison d'exister |
| `foundation/utility/SimplexNoise` | **`net.minecraft.world.level.levelgen.synth.SimplexNoise`** — `Maths` utilise déjà celui de vanilla |

> `foundation/advancement/CriterionTriggerBase` est sorti de cette liste : le fichier existe
> désormais des deux côtés avec un contenu identique (commit `5d69829`, remplacement du
> `SimpleCriterionTrigger` vanilla par une base trigger custom, suivant l'évolution de Create).

### 1.2 Présents sous un autre nom ou dans un autre package

| Fichier Forge | Équivalent NeoForge | Pourquoi |
|---|---|---|
| `foundation/data/recipe/CNProcessingRecipeGen` | `foundation/data/recipe/CNRecipeProvider` | En 1.21 la classe n'hérite plus de `ProcessingRecipeGen` mais de `RecipeProvider`, et agrège les générateurs. Même rôle, nom aligné sur son parent |
| `foundation/events/overlay/IrradiatedOverlayRendererVision` | `foundation/events/overlay/RadiationOverlay` | Même rôle (overlay HUD de radiation), réécrit pour hériter d'une nouvelle base `EasingHudOverlay`. Porté le 14 août (commit `fa48da6`, « restore the radiation vision overlay ») |
| `content/multiblock/bluePrintItem/ReactorBluePrintItemPacket` | `ReactorBluePrintData` + `PatternData` | Le paquet réseau Forge (`SimplePacketBase`) est remplacé par deux records à `Codec`/`StreamCodec`, cohérent avec l'architecture data-component déjà validée en §7 pour `controller/` — mais ce sous-dossier `bluePrintItem` lui-même n'a pas été audité ligne à ligne |

> ✅ **Résolu depuis le 7 août.** `SnowPowderRecipeGen` n'est plus une divergence de package :
> le commit `41f6176` (« move EnrichedRecipeGen/SnowPowderRecipeGen from foundation to the api
> package ») l'a ramené dans `api/data/recipe`, exactement au même chemin que Forge. Le sort
> exact de l'ancien vestige `api/data/recipe/EnrichedRecipeGen` (mentionné comme mort dans la
> version précédente de ce doc) n'a **pas été revérifié** dans cet audit — à confirmer avant de
> le considérer réglé.

---

## 2. Statut — plus aucun chantier de parité ouvert à ce niveau

Les quatre chantiers listés ici le 7 août sont **désormais tous portés** : la compat Alex's
Caves et les mixins client (tous les deux dans le commit `41f6176` du 8 août —
`compat/Mods.java`, `compat/alexscave/AlexscaveCompat.java`, `CameraAccessor.java`,
`GameRendererMixin.java` existent des deux côtés, correctement enregistrés dans
`createnuclear.neoforge.mixins.json`), la vache irradiée (commit `5350638` du 15 août), et
l'abstraction `IrradiatedAnimal` (commit `7d16431` du 16 août, détail en §2.1). Cette section
n'a plus vocation à lister de travail restant ; elle garde son historique pour référence.

---

### 2.1 `IrradiatedAnimal` — livrée le 16 août (commit `7d16431`)

**✅ Fait.** `content/contraptions/irradiated/IrradiatedAnimal.java` existe désormais côté
NeoForge, porté depuis Forge quasi à l'identique — seuls changements imposés par l'API 1.21 :
`ForgeEventFactory.onLivingConvert` → `EventHooks.onLivingConvert`, et l'appel à `finalizeSpawn`
adapté à sa nouvelle signature à 4 arguments (`ServerLevelAccessor` au lieu de `ServerLevel`,
plus de paramètre `dataTag`). L'interface expose la même mécanique de conversion animal
irradié → vanilla que Forge (mêmes effets `DAMAGE_BOOST`/`CONFUSION`, même `EntityEvent
.ZOMBIE_CONVERTING`, même son `SOUND_ZOMBIE_CONVERTED`) — c'est le pendant de la guérison
zombie-villageois vanilla.

`IrradiatedChicken` l'implémente, exactement comme côté Forge (`getNormalVariant()` →
`EntityType.CHICKEN`, `readFromVanilla`/`writeToVanilla` synchronisent `isChickenJockey`, un
`DATA_CONVERTING_ID` synchronisé porte l'état de conversion). Au passage, `IrradiatedChicken` a
aussi rattrapé le reste de son retard sur Forge : ponte d'œufs (`eggTime`), support chicken-jockey
complet (XP bonus, règle de despawn, sauvegarde), qui manquaient côté NeoForge. `CreateNuclear
.init(...)` enregistre `EntityType.CHICKEN → CNEntityType.IRRADIATED_CHICKEN` dans
`VANILLA_TO_IRRADIATED`, comme Forge.

**Piège 1.21 rencontré en portant `getExperienceReward()` :** cette méthode est désormais `final`
sur `LivingEntity` (`getExperienceReward(ServerLevel, Entity)`) — impossible à surcharger. Le hook
overridable équivalent est `protected int getBaseExperienceReward()`. Idem pour `positionRider` :
le décalage manuel par `getMyRidingOffset()` que faisait Forge n'a plus d'équivalent, la classe
vanilla `Chicken` de cette version ne fait plus que repositionner `yBodyRot` — copié tel quel côté
NeoForge plutôt que de réinventer un offset.

**Portée assumée, alignée sur Forge et non étendue :** seul `IrradiatedChicken` implémente
`IrradiatedAnimal`, exactement comme côté Forge — `Chat`/`Loup`/`Vache` n'implémentent pas
l'interface là-bas non plus. Un refactor qui irait plus loin (faire hériter chat/loup/vache d'une
base commune) serait une **amélioration NeoForge au-delà de la parité Forge**, pas un chantier de
portage : à ne considérer que si le besoin apparaît, pas pour « finir » ce document.

**Effet de bord découvert pendant ce chantier — bug de régression, pas un écart Forge/NeoForge :**
en réécrivant `IrradiatedCat` pour suivre de plus près le `Cat` vanilla (suppression des 3 classes
de goal dédiées `IrradiatedCatAvoidEntityGoal`/`RelaxOnOwnerGoal`/`TemptGoal`, remplacées par des
classes imbriquées calquées sur vanilla), `defineSynchedData()` avait régressé vers l'ancienne
signature sans paramètre (`protected void defineSynchedData()` au lieu de
`defineSynchedData(SynchedEntityData.Builder builder)`), plus valide en 1.21 — l'override
n'en était plus un, `IS_LYING`/`RELAX_STATE_ONE` et tout ce qu'apporte `super` n'étaient jamais
enregistrés dans le builder, d'où un crash immédiat au spawn (`IllegalStateException: ... has not
defined synched data value 19`). Corrigé dans le même commit en repassant par
`builder.define(...)` + `super.defineSynchedData(builder)`, sur le modèle de `IrradiatedWolf`.
**À garder en tête pour toute future entité :** un override qui compile sans erreur (signature
différente = simple surcharge, pas d'erreur du compilateur) peut quand même être un override
*raté* silencieusement — vérifier que la signature correspond bien à celle de la classe parente
en 1.21, pas à une version antérieure copiée-collée.

---

### 2.2 Nouveautés NeoForge non documentées, à surveiller

Des fichiers sans équivalent Forge sont apparus depuis le 7 août sans être suivis ici. La
plupart sont vivants et légitimes (infrastructure data attachments/components, goals IA du chat
irradié) ; ceux ci-dessous méritent une action :

> ✅ **`IrradiatedWoldCollarLayer.java` retirée** (commit `4918bf7`, « remove the orphaned
> IrradiatedWoldCollarLayer render layer »). Jamais ajoutée via `addLayer(...)` à
> `IrradiatedWolfRenderer` (`render()` était un no-op vide) — tentative de feature (collier coloré
> du loup irradié apprivoisé) commencée et jamais branchée, n'existait pas côté Forge. Supprimée
> plutôt que terminée.

`CNAttachmentTypes`, `CNDataComponents`, les goals IA du chat (`IrradiatedCatAvoidEntityGoal`,
`IrradiatedCatRelaxOnOwnerGoal`, `IrradiatedCatTemptGoal`), `FluidLockManager`,
`EasingHudOverlay`, `CNDamageTypeTagsProvider` : tous référencés et vivants, rien à signaler.

> ✅ **`CommentEventClients.java` fusionnée dans `CNClientEvent` puis supprimée.** N'était pas du
> code mort — `@EventBusSubscriber` auto-découverte par NeoForge, elle portait l'enregistrement
> des layers de modèle d'entités (`CNEntityType.registerModelLayer`, essentiel pour l'affichage
> de chat/poulet/loup/vache irradiés). Le problème était le découpage et le nom (coquille
> probable de « CommonEventClients », sans équivalent Forge ni convention du projet) : côté Forge,
> cet appel vit directement dans `CNClientEvent.registerLayers`, aux côtés du reste de
> l'enregistrement client. La méthode `registerLayers` a été rapatriée dans `CNClientEvent`
> (alignement avec Forge), et le fichier à part supprimé.

> ✅ **`ReactorGaugeOverrides.java` a disparu du code depuis la dernière mise à jour** — le
> fichier orphelin signalé précédemment a été retiré (reste seulement mentionné dans ce doc et
> dans `PORTAGE_REACTEUR.md`/`AUDIT_NEOFORGE.md`). Pas encore vérifié si la feature du gauge de
> cadre a été branchée ailleurs ou simplement abandonnée — à confirmer si le besoin ressurgit.
>
> ✅ **`logoFile` corrigé.** `neoforge.mods.toml` (`logoFile = "icon.png"`) pointait vers un
> fichier qui vivait dans `src/main/resources/META-INF/icon.png` — NeoForge résout `logoFile`
> relativement à la racine du jar (comme `assets/`, `data/`), pas au dossier `META-INF/` qui
> contient le toml lui-même. Déplacé vers `src/main/resources/icon.png` (racine des resources,
> sibling de `META-INF/`), la convention des templates NeoForge/Forge MDK.
> **Piège séparé rencontré en vérifiant le fix :** `bin/main` (sortie de compilation IntelliJ,
> distincte de `build/` géré par Gradle) peut rester figée sur une ancienne copie de
> `neoforge.mods.toml`/des resources si la run configuration charge cette sortie au lieu de
> `build/resources/main`. Un `Rebuild Project` (ou passer « Build and run using » sur Gradle dans
> les réglages Gradle d'IntelliJ) est nécessaire pour voir tout changement de resources se
> refléter en jeu — sans lien avec le portage Forge/NeoForge, à garder en tête pour tout futur
> « ça ne marche pas alors que le fichier est bon ».
>
> ✅ **`CNShapelessRecipeGen.java` supprimé** (commit `93975b3`), plutôt que rebranché. Sa
> registration dans `CreateNuclearDatagen` était déjà commentée et le générateur était mort code
> depuis le début — pas de recettes shapeless (cloth, etc.) perdues puisqu'elles n'étaient jamais
> produites. Sorti de cette liste et de l'ordre de travail (§5).

---

## 3. Écarts internes aux fichiers partagés — non audités

Ces fichiers existent des deux côtés mais divergent fortement. **Un gros diff n'est pas une
preuve de feature manquante** : l'API 1.21 en explique une grande part. Aucun n'a été audité
ligne à ligne, contrairement au domaine réacteur.

| Fichier | Lignes divergentes (7 août) | Lignes divergentes (14 août) | Lignes divergentes (16 août) |
|---|---|---|---|
| `CNBlocks` | 701 | 707 | non revérifié |
| `CNItems` | 457 | 472 | non revérifié |
| `foundation/advancement/CNAdvancement` | 394 | 416 | non revérifié |
| `content/contraptions/irradiated/cat/IrradiatedCat` | 384 | 380 | 40 |
| `foundation/data/recipe/CNStandardRecipeGen` | 395 | 394 | non revérifié |
| `compat/jei/CreateNuclearJEI` | 344 | 333 | non revérifié |
| `content/contraptions/irradiated/wolf/IrradiatedWolf` | 333 | 333 | 333 |
| `content/contraptions/irradiated/chicken/IrradiatedChicken` | — | — | 111 *(nouveau — n'était pas suivi ici avant le portage du 16 août)* |
| `CNCreativeModeTabs` | 249 | 249 | non revérifié |
| `content/radiation/capability/RadiationCapability` | 186 | 199 | non revérifié |

`IrradiatedCat` chute de 380 à 40 lignes divergentes : la réécriture du 16 août aligne l'IA du
chat sur `Cat` vanilla plutôt que de la dupliquer dans des classes de goal maison (§2.1), donc le
fichier **se rapproche** de Forge au lieu de s'en éloigner — l'inverse de la tendance habituelle
de ce tableau. `IrradiatedWolf` reste stable à 333, comme prévu (ni touché ni concerné par ce
chantier). Seules les trois lignes concernées par le travail du 16 août ont été revérifiées ;
les autres restent au dernier relevé du 14 août — dérives modestes (+6 à +22 lignes), cohérentes
avec les commits récents (`RadiationCapability` refactorée en attachment sérialisable/
synchronisable le 9 août, routage des dégâts le 8 août). Rien d'alarmant, mais aucun de ces
fichiers n'est audité ligne à ligne — la mise en garde ci-dessous reste entière.

> **Ce n'est pas théorique.** Sur les six bugs trouvés en testant le réacteur en jeu, **deux
> étaient cachés dans `CNBlocks`** : la capacité de stress de la sortie à `10240` au lieu de
> `64000` (SU divisé par 6,25) et quatre `addLayer` manquants — dont `reinforced_glass` qui
> rendait donc opaque. Aucun des deux n'était visible dans un diff d'arborescence.
>
> **Méthode qui a marché :** ne pas lire le diff en entier, mais partir d'un symptôme observé en
> jeu et remonter. C'est plus rapide et ça ne trouve que des bugs réels.
>
> ✅ **Capacité de stress de `REACTOR_OUTPUT` — revérifiée le 16 août, déjà corrigée.**
> `BlockStressValues.CAPACITIES.register(block, () -> 64000.0)` est identique des deux côtés
> (`CNBlocks.java` ligne 183 Forge / 191 NeoForge), et un `grep -rn "10240"` sur les deux dépôts ne
> renvoie plus rien. Réglé avant cette session, sans commit dédié retrouvé — probablement corrigé
> dans le même lot que le reste du portage réacteur.

### 3.3 Audit ligne à ligne — `CNBlocks` / `CNItems` (16 août 2026)

Audit complet demandé pour clore la ligne « continu » du §5. Les deux fichiers ont été lus
intégralement des deux côtés (623↔690 lignes pour `CNBlocks`, 436↔452 pour `CNItems`) et comparés
bloc par bloc / item par item, plutôt que jugés sur la taille du diff brut.

**Parité de la liste (aucune entrée manquante ou en trop) :**
- `CNBlocks` : 27 `.block("...")` de chaque côté, mêmes 27 noms.
- `CNItems` : 29 `.item(...)` de chaque côté (28 nommés + 1 anonyme), mêmes noms.

**Faux positif éliminé — les `addLayer` ne manquent pas, ils ont changé de mécanisme.** Six blocs
(`REACTOR_FRAME`, `REACTOR_ROD_INPUT`, `REACTOR_FLUID_INPUT`, `REINFORCED_GLASS`,
`ENRICHING_FIRE`, `ENRICHING_CAMPFIRE`) ont un `.addLayer(() -> RenderType::X)` côté Forge, absent
côté NeoForge. Vérifié sur les modèles JSON générés (`rod_input.json`, les 4 `frame_*.json`,
`fluid_input*.json`, `block.json`/`block_off.json` du campfire, `enriching/fire/*.json`) : chacun
déclare désormais `"render_type": "cutout_mipped"` (ou `"cutout"`/défini via `.renderType(...)`
dans le blockstate builder pour `REINFORCED_GLASS`) **directement dans le modèle**, le mécanisme
1.21 qui remplace l'ancien layer Java. **Ceci referme définitivement le doute laissé en §3 sur
les « 4 `addLayer` manquants »** — aucun des deux mécanismes de rendu n'est cassé, c'est un
remplacement correctement fait, pas un oubli.

**Bug réel trouvé et corrigé — rendement de minerai divergent sur uranium/plomb.** Comparé aux
JSON de loot table générés (`data/createnuclear/loot_table/blocks/*.json`), 4 minerais avaient
perdu leur fonction `set_count` de base :

| Bloc | Forge (`set_count` avant bonus fortune) | NeoForge avant correctif |
|---|---|---|
| `uranium_ore` / `deepslate_uranium_ore` | `uniform(3.0–4.0)` | **absent** — juste `ore_drops` (comme le fer vanilla, ~1 item) |
| `lead_ore` / `deepslate_lead_ore` | `uniform(2.0–4.0)` | **absent** |
| `thorium_ore` / `deepslate_thorium_ore` | `uniform(2.0–5.0)` | présent, conforme (contrôle) |
| `nitrate_ore` / `deepslate_nitrate_ore` | pas de `set_count` côté Forge non plus | conforme (contrôle) |

Cause probable : lors du portage de `Enchantments.BLOCK_FORTUNE` (champ statique Forge/1.20) vers
le `HolderLookup`-based `Enchantments.FORTUNE` de 1.21, `URANIUM_ORE`/`LEAD_ORE` (et leurs variantes
deepslate) sont passés à l'aide `ApplyBonusCount.addOreBonusCount(...)` (le helper vanilla standard,
sans `set_count`) au lieu de garder l'appel à `SetItemCountFunction.setCount(UniformGenerator
.between(...))` que `THORIUM_ORE` a conservé correctement. Conséquence en jeu : ces 4 minerais
donnaient nettement moins de matière première brute que Forge (pas de plancher garanti 2–4/3–4,
juste le comportement fer/or vanilla).

**Corrigé** dans `CNBlocks.java` : réintroduction du `.apply(SetItemCountFunction.setCount(
UniformGenerator.between(...)))` sur les 4 loot tables concernées, avant le `ApplyBonusCount`
existant. Recompilé (`./gradlew compileJava`) et revérifié via `./gradlew runData` : les 4 JSON
générés portent maintenant `"function": "minecraft:set_count"` avec les bornes de Forge.
**Non testé en jeu** (récolte réelle avec/sans fortune) — à ajouter au §4.

**Point mineur non corrigé, à surveiller :** le nom de `formula` sous `apply_bonus` diverge de
Forge sur tous les minerais concernés (`ore_drops` côté NeoForge contre `uniform_bonus_count`
côté Forge, y compris pour `thorium_ore` où le `set_count` de base, lui, est identique) — cet
écart vient du choix d'helper Java (`addOreBonusCount` vs `addUniformBonusCount`) et pourrait
légèrement changer la courbe du bonus fortune sans toucher au plancher garanti. Pas retouché ici
faute de certitude sur l'intention exacte ; à revoir si un joueur signale un rendement fortune
anormal sur ces minerais.

**Autres écarts relevés, aucun n'est un bug :**
- `CNBlocks`/`CNItems` NeoForge appellent chacun `CreateNuclear.REGISTRATE.setCreativeTab(
  CNCreativeModeTabs.MAIN)` dans un bloc `static {}` en tête de fichier, en plus de l'appel déjà
  présent dans `CNPaletteBlocks` (le seul endroit qui l'appelle côté Forge). Redondant mais
  inoffensif — fixe le même onglet, garantit juste l'ordre d'init sans dépendre du chargement de
  `CNPaletteBlocks` en premier.
- `ANTI_RADIATION_HELMETS` porte un tag `CNItemTags.ANTI_RADIATION_HELMET` absent côté Forge et
  absent des 3 autres pièces d'armure ; vérifié utilisé dans `foundation/events/overlay/
  HelmetOverlay.java` (détection du casque porté pour l'overlay de vision) — vivant, pas mort code.
- Divergences de typage Java (`ItemEntry<? extends Item>` généré en un seul bloc côté Forge,
  scindé en deux blocs `ItemEntry<RadiationItem>`/`ItemEntry<Item>` côté NeoForge),
  `.transform(setColorComponent(Cloths.DEFAULT))` et le `if (cloth == Cloths.DEFAULT) continue`
  dans les recettes smithing des armures — cohérent avec le passage NBT mutable → DataComponents
  déjà documenté en §6, pas une divergence de comportement en jeu.

**Conclusion : l'audit ligne à ligne de `CNBlocks`/`CNItems` est fait pour cette passe, un bug réel
trouvé et corrigé.** La ligne « continu » du §5 reste néanmoins en place par nature (voir sa
justification) — un futur symptôme en jeu peut toujours révéler autre chose, mais aucun résidu
connu ne traîne sur ces deux fichiers à ce jour.

### 3.1 Recettes — diff des JSON générés

Le diff des `.java` cache mal les recettes manquantes ; celui des JSON générés les montre
directement. Commande dans le §8.

**Corrigé** (symptôme signalé : le concentré d'azote refroidi ne servait à rien) — toute la
chaîne de l'azote était morte, **cassée aux deux bouts** :

| Étape | Recette | État avant |
|---|---|---|
| nitrate → concentré d'azote | `smelting` + `blasting` | absente de `CNStandardRecipeGen` |
| concentré → concentré refroidi | `snow_powder` | absente — c'est le §2.1, porté depuis |
| concentré refroidi + glace → azote liquide | `mixing` | absente de `CNMixingRecipeGen` |

Seul le fluide `LIQUID_NITROGEN` était enregistré — donc obtenable uniquement en créatif.

**Reste à traiter — revérifié le 14 août, aucune de ces trois lignes n'a bougé depuis le 7 août :**

| Recette Forge | Absente côté NeoForge |
|---|---|
| `mixing/thorium_fluid` | oui — toujours absente. Un `compacting/thorium_fluid_to_thorium_ingot.json` existe désormais (sens fluide → lingot), mais rien ne produit le fluide lui-même : `CNFluids.THORIUM` reste inobtenable hors créatif |
| `crafting/thorium_block_from_compacting` · `crafting/thorium_ingot_from_compacting` | oui — `THORIUM_COMPACTING` manque toujours dans `CNStandardRecipeGen` |
| `mechanical_crafting/reactor_alarm` | oui |

> ✅ **Corrigé.** Commit `93975b3` (« fix(recipe-gen): stop crushing/washing recipe generators
> from leaking into the create namespace »). Cause : `ProcessingRecipeGen.create(() -> ingrédient,
> …)`, l'overload à 2 arguments de Create, hardcode `Create.ID` comme namespace par défaut au lieu
> du namespace du générateur — un choix différent de Forge, où l'ancienne surcharge prenait le
> namespace du générateur. `CNCrushingRecipeGen` (6 recettes) et `CNWashingRecipeGen` (1 recette,
> via `moddedCrushedOreCustom`) appelaient cet overload sans namespace explicite, ce qui générait
> les JSON sous `data/create/recipe/` et **écrasait des recettes du jar de Create**
> (`crushing/raw_copper`, `crushing/raw_zinc`, `crushing/raw_uranium_block`,
> `splashing/crushed_raw_lead`).
>
> **Correctif retenu :** plutôt que de passer `CreateNuclear.MOD_ID` à chaque appel, override de
> la seule surcharge fautive dans chacun des deux générateurs :
> ```java
> @Override
> protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<B> transform) {
>     return create(CreateNuclear.MOD_ID, singleIngredient, transform);
> }
> ```
> Aucun appel existant à changer, et ça sécurise aussi les futures recettes ajoutées via ce
> pattern ainsi que les helpers hérités de Create (`WashingRecipeGen.convert`/`crushedOre`) qui
> passent par le même overload en interne.
>
> Vérifié : `find .../data/create -name '*.json'` ressort vide (hors tags) après régénération du
> datagen ; les 7 JSON concernés sont réapparus sous `data/createnuclear/recipe/`.
>
> **Audité au passage, les 10 autres générateurs de recettes du projet sont sains** — chacun
> passe par le namespace du mod pour une raison différente (surcharge par nom, `ResourceLocation`
> explicite via `CreateNuclear.asResource(...)`, ou classe de base custom qui ne reproduit pas le
> hardcode de Create). Seule note en passant, sans lien avec ce bug : `CNStandardRecipeGen.
> createSpecial()` (~ligne 127) hardcode `Create.asResource(...)`, mais la méthode n'est jamais
> appelée nulle part dans le repo — code mort, aucun impact en jeu.

### 3.2 Icônes d'inventaire des armures anti-radiation colorées — divergence assumée avec Forge

✅ **Corrigé le 15 août.** Symptôme : au lancement, `ModelManager` loggait `Missing textures in
model createnuclear:default_anti_radiation_{helmet,chestplate,leggings,boots}#inventory` pour
les 16 couleurs, alors que les fichiers PNG existaient bien sur le disque, aux bonnes dimensions
(96×96, cohérent avec le `texture_size` déclaré dans les modèles Blockbench `item/<slot>/item`).
Un `Rebuild Project` complet n'y changeait rien — **ce n'était pas le piège `bin/main` du §2.2**,
malgré la ressemblance de symptôme (« le fichier est bon mais rien ne change en jeu »).

**Divergence avec Forge, volontaire mais insuffisamment comprise au moment du portage.**
`CNBuilderTransformers.coloredArmorModel` (ligne ~50) fait pointer la texture des 16 modèles
colorés (`item/colored/<couleur>_anti_radiation_<slot>.json`) directement vers
`createnuclear:models/armor/<couleur>_anti_radiation_suit` — la même texture que celle utilisée
pour la couche d'armure portée sur le corps (via `AntiRadiationArmorTextureMixin` /
`ClothTagHelper`). Forge, lui, **duplique** ces 16 textures dans un second dossier,
`textures/item/armors/<couleur>_anti_radiation_suit.png`, et c'est ce chemin dupliqué que pointe
son propre `CNBuilderTransformers`. Ce n'était pas une maladresse côté Forge : c'était nécessaire,
pour la raison ci-dessous — non documentée jusqu'ici, d'où sa redécouverte à la dure ici.

**Cause réelle :** l'atlas `minecraft:blocks` (`textures/atlas/blocks.png`, celui qui fournit les
UV de tout modèle d'item 3D type Blockbench, par opposition aux icônes plates `item/generated`)
n'inclut par défaut que certains dossiers comme sources de sprites (`textures/block/`,
`textures/item/`, plus quelques ajouts `single` par mod). `textures/models/armor/` n'en fait
**jamais partie** côté vanilla — ce dossier est réservé aux textures de couche d'armure, chargées
par liaison GL directe (`HumanoidArmorLayer`), jamais par lookup d'atlas. D'où le warning : le
fichier existe, mais n'est simplement jamais stitché dans l'atlas que ces modèles 3D interrogent
pour leurs UV.

**Correctif retenu ici (différent de Forge — sans dupliquer les textures) :** ajout de
`src/main/resources/assets/minecraft/atlases/blocks.json`, qui déclare `models/armor` comme
source `directory` supplémentaire de l'atlas `minecraft:blocks` :
```json
{ "type": "directory", "source": "models/armor", "prefix": "models/armor/" }
```
Les sources de plusieurs packs de ressources pour un même atlas **s'additionnent**, elles ne
s'écrasent pas — donc sans risque pour les entrées `single` déjà présentes dans ce fichier
(sprites de fluides `thorium`/`uranium`/`nitrogen`). **Piège rencontré en l'écrivant, à surveiller
pour toute future entrée dans ce même fichier :** une première tentative avait imbriqué l'entrée
dans un objet sans son propre champ `"type"` (`{ "sources": [ { "type": "directory", ... } ] }`
au lieu de `{ "type": "directory", ... }` directement dans le tableau `"sources"` racine) — JSON
valide mais source non reconnue par le désérialiseur, qui a fait échouer le chargement de tout le
fichier et régresser au passage les sprites de fluides déjà fonctionnels.

**Bug distinct trouvé au passage, corrigé dans le même lot :** les 4 modèles Blockbench de base
(`models/item/default_anti_radiation_{helmet,chestplate,leggings,boots}/item.json`) déclaraient
une texture par défaut (`layer0`/`particle`/`"14"` selon la pièce) pointant vers
`createnuclear:item/default_anti_radiation_suit` — un fichier qui n'a **jamais existé** dans ce
projet, ni côté Forge où le même chemin orphelin est présent à l'identique. Resté invisible côté
Forge (et côté NeoForge avant ce fix) parce que ces 4 modèles de base ne sont jamais bakés seuls :
ils ne servent que de `parent` aux 16 `overrides` colorés, qui redéfinissent systématiquement
leurs propres clés de texture — donc sans effet observable, mais une référence morte à nettoyer
si elle ressurgit un jour côté Forge. Corrigé en pointant vers
`createnuclear:models/armor/default_anti_radiation_suit`, qui existe réellement.

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
| Ventilateur derrière de la neige poudreuse | le concentré d'azote devient du concentré d'azote refroidi ; particules flocon/éternuement ; entités ralenties | §2.1 porté sans test en jeu. Vérifier aussi la catégorie JEI « Cryogenic fan » |
| `enable_world_gen = false` dans la config commune | **monde neuf** sans uranium, plomb, thorium, nitrate ni strates | filtre de placement porté sans test en jeu. Ne se voit que sur un monde généré après coup |
| `enable_world_gen = true` (défaut) | les cinq features génèrent comme avant | le filtre est désormais `createnuclear:config_filter`, plus celui de Create — non-régression à confirmer |
| Chaîne de l'azote de bout en bout | nitrate → *(four)* → concentré → *(ventilateur + neige poudreuse)* → concentré refroidi → *(mixeur + glace)* → 100 mB d'azote liquide | §3.1 corrigé sans test en jeu |
| Minage `uranium_ore`/`deepslate_uranium_ore`/`lead_ore`/`deepslate_lead_ore` | drop 3–4 (uranium) ou 2–4 (plomb) matière brute, plus bonus fortune | §3.3 corrigé sans test en jeu — vérifié uniquement sur le JSON de loot table généré |

> **Divergence assumée sur la clé de config.** `CWorldGen` exposait `disableWorldGen` (défaut
> `false`) — un copier-coller littéral de la classe homonyme de Create, **jamais lu par personne**.
> Il est remplacé par `enable_world_gen` (défaut `true`), la clé de Forge. Un `createnuclear-common.toml`
> existant perdra donc son ancienne entrée : sans effet, puisqu'elle n'en avait aucun.

---

## 5. Ordre de travail suggéré

| # | Chantier | Fichiers | Difficulté | Pourquoi ce rang |
|---|---|---|---|---|
| 1 | **Audit `CNBlocks` / `CNItems` (§3)** | — | continu | À faire au fil des symptômes, pas d'un bloc |

> ✅ Audit ligne à ligne complet de `CNBlocks`/`CNItems` (§3.3) — fait le 16 août : parité de
> liste confirmée (27 blocs, 29 items), les 6 `addLayer` « manquants » se sont révélés être un
> remplacement correct par `render_type` en JSON (pas un bug), et un vrai bug de rendement de
> minerai (uranium/plomb sans `set_count` de base) trouvé et corrigé. La ligne reste « continu »
> par nature (voir §3.3), mais cette passe-ci est close.
>
> ✅ Abstraction animaux — `IrradiatedAnimal` (§2.1) — commit `7d16431` du 16 août : interface
> portée, `IrradiatedChicken` l'implémente (comme côté Forge), poulet rattrapé sur ponte d'œufs/
> chicken-jockey, `IrradiatedCat` réécrit pour suivre `Cat` vanilla. Dernier chantier de ce
> document, sorti de cette liste — plus rien à porter au niveau features.
>
> ✅ Namespace `create` des recettes crushing/washing (§3.1) — corrigé, commit `93975b3`, sorti de
> cette liste.
>
> ✅ `CNShapelessRecipeGen` (§2.2) — supprimé plutôt que rebranché (dead code depuis le début),
> commit `93975b3`, sorti de cette liste.
>
> ✅ `logoFile` (§2.2) — image déplacée de `META-INF/icon.png` vers `icon.png` (racine des
> resources), seul emplacement que NeoForge résout réellement pour cette clé.
>
> ✅ Vache irradiée + `AnimalUtil` (§2.1) — commit `5350638`.
>
> ✅ `IrradiatedWoldCollarLayer` (§2.2) — supprimée plutôt que branchée (dead code depuis le
> début), commit `4918bf7`, sorti de cette liste.
>
> ✅ Icônes d'inventaire des armures anti-radiation colorées (§3.2) — atlas `minecraft:blocks`
> manquait `models/armor` comme source de sprites ; corrigé le 15 août sans dupliquer les
> textures côté Forge, détail complet en §3.2.

Les tests en jeu du §4 passent avant tout ça.

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
| `ProcessingRecipe<XWrapper>` + `RecipeWrapper`/`ItemStackHandler` | `StandardProcessingRecipe<SingleRecipeInput>` — plus de classe wrapper à écrire, `new SingleRecipeInput(stack)` suffit |
| `Codec.unit(...)` pour un `PlacementModifierType` | `MapCodec.unit(...)` — `PlacementModifierType#codec()` renvoie un `MapCodec` |
| `CatnipServices.REGISTRIES.getKeyOrThrow(...)` | `RegisteredObjectsHelper.getKeyOrThrow(...)` |

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

## 7. Audit ciblé — `content/multiblock/controller` (14 août 2026)

Audit ligne à ligne demandé spécifiquement sur ce sous-dossier du domaine réacteur, malgré le
statut « porté et validé » de [`PORTAGE_REACTEUR.md`](PORTAGE_REACTEUR.md), pour vérifier que
ce statut tient toujours après les commits récents (« clean up dead imports/logic... after
RodType alignment », etc.).

### 7.1 Parité des fichiers

Les deux arborescences contiennent **exactement les 41 mêmes fichiers `.java`**, mêmes noms,
mêmes sous-dossiers (`consumable/`, `display/`, `manager/`, `service/`, `snapshot/`). Aucun
fichier Forge orphelin, aucun ajout NeoForge sans équivalent. Aucune classe du dossier n'a un
compte de référence à 0 ailleurs dans `src` — pas de piège du §0 ici.

### 7.2 Divergences internes

21 fichiers sur 41 diffèrent contre l'original Forge ; 20 sont identiques à l'octet. Les 21
divergences se répartissent en :

- **API 1.21 correctement anticipée** (la majorité) : data components à la place du NBT mutable
  (`CNDataComponents.HEAT`, `ReactorBluePrintData`), `read/write(..., HolderLookup.Provider, ...)`,
  `ItemStack.parse`/`saveOptional`, `BuiltInRegistries` à la place de `ForgeRegistries`,
  `level.getCapability(Capabilities.ItemHandler.BLOCK, ...)`, `useItemOn`/`ItemInteractionResult`.
  Plusieurs fichiers (`ReactorControllerBlockEntity`, `ReactorHeatUpdateCoordinator`,
  `IReactorHeatUpdateCoordinator`, `PatternReader`) portent même des Javadoc **nouvelles**
  expliquant explicitement pourquoi (copie défensive des `ItemStack` sous NeoForge notamment).
- **Cosmétique** : commentaires traduits en français (`ReactorInputManager`,
  `ReactorAlarmManager` et leurs interfaces), espaces blancs.
- `manager/ReactorInputFluidManager.java` (~ligne 123) : alloue un `new VirtualReactorInputFluid()`
  au lieu de réutiliser l'instance déjà créée quand `handlers` est vide. Micro-inefficacité sans
  impact fonctionnel, pas un bug.

**Conclusion sur ce sous-dossier : le statut « porté et validé » tient.** Aucune divergence
interne trouvée n'est une régression de logique métier.

### 7.3 Bug trouvé le 14 août, corrigé depuis — hors dossier `controller` mais qui l'impactait directement

En remontant l'usage de `rodType.ratio()` (appelé dans `ReactorHeatUpdateCoordinator`,
`calculateHeatBalance`) jusqu'à sa définition dans `api/multiblock/rods/RodType.java`, un bug
latent avait été repéré : `Builder.ratio` valait `null` par défaut au lieu de `() -> 1` comme
promis par la Javadoc de `build()`, exposant tout `RodType` construit sans `.ratio(...)`
explicite à une `NullPointerException` au premier calcul de heat balance.

> ✅ **Corrigé.** Commit `970503b` (« fix(rods): default RodType.Builder.ratio to 1 to prevent
> an NPE on unset rods ») — `Builder.ratio` vaut désormais `private Supplier<Integer> ratio =
> () -> 1;` (ligne 141), conforme à la Javadoc et au comportement Forge. Rien à faire de plus
> sur ce point.

### 7.4 Point pré-existant relevé au passage (ni régression ni lié au portage)

`ReactorControllerBlock.java` (~ligne 140 NeoForge, ~130 Forge) : `state.setValue(ASSEMBLED, false)`
— `setValue` renvoie un `BlockState` immuable qui n'est ni réassigné ni appliqué via
`level.setBlock(...)`. Code mort **identique des deux côtés**, donc pas une régression du
portage, mais probablement un bug fonctionnel réel (le flag `ASSEMBLED` ne semble jamais remis à
`false` par ce chemin). À vérifier en jeu séparément — hors périmètre de ce document qui ne
traite que des écarts Forge/NeoForge.

---

## 8. Vérifier l'état à tout moment

```bash
# Fichiers Forge sans équivalent NeoForge — doit en lister 6 (3 du §1.1, 3 du §1.2, 0 du §2)
cd ~/Documents/Ynov/Ydays
diff <(cd CreateNuclearForge/src && find . -name '*.java' | sort) \
     <(cd CreateNuclearNeoForge/src && find . -name '*.java' | sort)

# Recettes présentes côté Forge et absentes côté NeoForge (§3.1)
# Les préfixes diffèrent : `recipes/` en 1.20.1, `recipe/` en 1.21
diff <(cd CreateNuclearForge/src/generated/resources/data/createnuclear/recipes && find . -name '*.json' | sort) \
     <(cd CreateNuclearNeoForge/src/generated/resources/data/createnuclear/recipe && find . -name '*.json' | sort)

# Recettes qui fuient dans le namespace de Create — doit être vide hors tags
find CreateNuclearNeoForge/src/generated/resources/data/create -name '*.json'

# Écart interne d'un fichier partagé
diff CreateNuclearForge/src/main/java/.../X.java \
     CreateNuclearNeoForge/src/main/java/.../X.java

# Classes NeoForge jamais référencées (le piège du §0)
cd CreateNuclearNeoForge && grep -rl '<NomDeClasse>' src --include=*.java

# Non-régression du réacteur — doit sortir en 0 des deux côtés
cd CreateNuclearNeoForge && ./gradlew runGameTestServer   # 31 tests
cd CreateNuclearForge   && ./gradlew runGameTestServer    # 28 tests
```
