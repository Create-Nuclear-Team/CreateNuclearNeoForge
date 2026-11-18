# Villageois irradié — Document de conception

> Document généré à partir d'une session de conception. Rien n'est encore implémenté : ce fichier
> décrit l'architecture retenue, les mécanismes Minecraft utilisés et les contraintes identifiées
> avant d'écrire le code. Les points marqués **à vérifier** doivent être confirmés dans les sources
> décompilées de Minecraft 1.21.1 avant implémentation.

---

## Objectif

Un villageois exposé aux radiations se **transforme petit à petit** :

- il accumule une **dose** de radiation ;
- au-delà de certains seuils, il passe des **stades** de mutation ;
- à chaque stade, son **visuel** change et il propose de **nouveaux trades** avec des items du mod.

Contexte technique : Minecraft 1.21.1, NeoForge 21.1.x. Le mod dispose déjà de
`RadiationCapability` (attachment `CNAttachmentTypes.RADIATION`, synchronisé client),
`RadiationEffect` et du modèle de conversion `IrradiatedAnimal`.

---

## 1. Rappels sur les villageois vanilla

| Élément | Fonctionnement |
|---|---|
| `VillagerData` | Biome d'origine, métier, niveau (1 à 5) |
| `MerchantOffers` | Liste des trades **de ce villageois précis**, sauvegardée dans son NBT |
| `VillagerTrades` | Pools de trades **en Java** (pas de JSON en 1.21.1), par métier et par niveau. À chaque niveau, 2 offres sont tirées du pool. |
| `VillagerTradesEvent` (NeoForge) | Ajoute des offres au pool d'un **métier**, pas à un villageois individuel |
| `Brain` | IA complexe : travail, POI, lit, cloche, panique, ragots |
| `VillagerRenderer` | Couches empilées : peau de base, puis vêtements (type et métier). Extensible via `EntityRenderersEvent.AddLayers`. |
| Zombie villageois guéri | Conversion d'entité qui recopie manuellement métier, offres, XP et ragots. C'est le modèle suivi par `IrradiatedAnimal`. |

---

## 2. Architecture retenue

### Comparaison

| | **A. Villageois vanilla + données attachées** ✅ | B. Nouvelle entité `IrradiatedVillager` |
|---|---|---|
| Principe | Le `Villager` vanilla reçoit un attachment (dose, stade). Une couche de rendu change le visuel. Les trades sont ajoutés à ses `MerchantOffers`. | Conversion vers une sous-classe de `Villager`, comme `IrradiatedAnimal` |
| Progression par stades | Naturelle | Il faut quand même des stades dans l'entité |
| IA, POI, lit, reproduction, raids, golems | Fonctionnent nativement | Risques : le `Brain` et les POI sont liés au type d'entité |
| Compatibilité avec d'autres mods | Le villageois reste un `minecraft:villager` | Beaucoup de mods ciblent `EntityType.VILLAGER` |
| Cohérence avec l'existant | Nouveau modèle | Même logique qu'`IrradiatedAnimal` |
| Quantité de code | Faible | Élevée : renderer, modèle, copie complète des données |

**Choix : option A.** Une transformation progressive correspond à une donnée qui évolue sur une
entité existante, et on évite de toucher au `Brain` du villageois.

Si un stade final « mutant », non commerçant et réellement différent, est voulu plus tard, une
conversion de type B pourra être ajoutée **uniquement pour ce dernier stade**.

### Vue d'ensemble

```
tickRadiation(villager)
   └─► totalRadiation > 0 ──► VILLAGER_MUTATION.dose += totalRadiation
                                   │
                                   └─► dose ≥ seuil[stade+1] ──► stade++
                                          ├─ son + particules
                                          ├─ sync client ──► RenderLayer (visuel du stade)
                                          └─ injection des trades du stade dans MerchantOffers
```

---

## 3. Composants

### 3.1 Attachment `VILLAGER_MUTATION`

`RadiationCapability` calcule une radiation **instantanée** et ne cumule rien. Il faut un
attachment dédié :

| Champ | Type | Rôle |
|---|---|---|
| `dose` | `double` | Dose cumulée |
| `stage` | `int` | Stade de mutation courant (0 = sain) |

- Enregistré dans `CNAttachmentTypes`, avec `CODEC` (persistance) et `.sync(STREAM_CODEC)`, sur le
  même modèle que `RADIATION`. Le client a besoin du stade pour le rendu.
- Alimenté dans `RadiationCapability.tickRadiation` : si l'entité est un `Villager`, on ajoute
  `totalRadiation` à `dose` (éventuellement avec un multiplicateur configurable).
- On ne crée l'attachment qu'à la première exposition, pour ne pas en ajouter un à tous les
  villageois du monde (même principe que le garde `canBeIrradiated` avant `getData`).

### 3.2 Stades

Exemple de découpage (seuils dans `CNCServer`) :

| Stade | Nom | Visuel indicatif |
|---|---|---|
| 0 | Sain | Vanilla |
| 1 | Contaminé | Teint pâle et verdâtre, quelques taches |
| 2 | Muté | Excroissances, nez déformé, mains abîmées |
| 3 | Irradié | Yeux luminescents (émissif), peau craquelée |
| 4 | Stade final | Mutation complète, particules irradiées permanentes |

Le passage de stade est géré côté serveur uniquement. Il joue un son (`ZOMBIE_CONVERTING` ou un son
dédié) et émet les particules `IRRADIATED_PARTICLES`.

### 3.3 Visuel : `IrradiatedVillagerLayer`

- Une `RenderLayer<Villager, VillagerModel<Villager>>` ajoutée à `VillagerRenderer` via
  `EntityRenderersEvent.AddLayers`.
- Elle lit le stade dans l'attachment synchronisé et choisit la texture correspondante.
- **Contrainte :** la couche est dessinée **par-dessus** les vêtements. Les textures doivent donc
  être transparentes, sauf sur les zones mutées (visage, mains, excroissances).
- Les yeux luminescents passent par une seconde couche en `RenderType.eyes(...)` (comme
  l'araignée).
- Optionnel : des cubes supplémentaires sur le modèle pour les excroissances. C'est plus coûteux, à
  ne faire qu'après une première version en textures.

Textures prévues :

```
assets/createnuclear/textures/entity/villager/irradiated/
├── stage_1.png
├── stage_2.png
├── stage_3.png
├── stage_4.png
└── eyes.png          ← couche émissive (stades 3 et 4)
```

### 3.4 Trades : `IrradiatedVillagerTrades`

- Un registre Java : `stade → (métier → liste de VillagerTrades.ItemListing)`, avec un pool
  `générique` utilisé quand le métier n'a pas de pool dédié.
- À chaque passage de stade, on tire N offres du pool et on les ajoute avec
  `villager.getOffers().add(...)`.
- Les offres ajoutées sont sauvegardées par vanilla dans le NBT du villageois, il n'y a rien à
  persister en plus.

Exemples d'orientation par métier :

| Métier | Idée de trades irradiés |
|---|---|
| Forgeron (armures) | Pièces d'armure anti-radiation |
| Cartographe | Carte vers une ancienne base nucléaire (lien avec `BIOME_ANCIENNE_BASE_NUCLEAIRE.md`) |
| Bibliothécaire | `REACTOR_BLUEPRINT` |
| Clerc | Iode et objets de protection, `REINFORCED_GLASS_BOTTLE` |
| Générique | Achat de matériaux radioactifs (uranium, plomb) contre émeraudes |

Monnaie : l'émeraude, ou un objet du mod (plomb, uranium…) pour ancrer ces trades dans la
progression. **À décider.**

---

## 4. Contraintes et points d'attention

### 4.1 ⚠️ Le villageois meurt avant de muter

`RadiationEffect` inflige des dégâts. Un villageois a 20 PV et ne régénère pas naturellement.
Options :

1. **Aucun dégât de radiation pour les villageois**, qui accumulent seulement de la dose.
2. **Dégâts réduits et plafonnés**, jamais mortels.
3. **Dégâts normaux** : c'est au joueur de doser l'exposition (gameplay de « cuisson » risqué).

Le tag `CNEntityTags.IRRADIATED_IMMUNE` **ne convient pas** : il coupe tout le calcul dans
`canBeIrradiated`, donc la dose ne serait jamais accumulée. Il faut un cas spécifique dans
`RadiationEffect#applyEffectTick`.

### 4.2 Effacement des trades par vanilla

Un villageois sans XP qui perd son poste de travail perd son métier, et ses offres sont
réinitialisées (**à vérifier** dans `Villager#setVillagerData` et la logique de perte de métier).

Parade : le stade étant dans notre attachment, on **réinjecte** les offres manquantes de façon
idempotente (à la montée de stade, au chargement, au changement de métier). `MerchantOffer` n'a pas
d'identifiant : la comparaison se fait sur les items (coût et résultat).

### 4.3 Cas particuliers

| Cas | Question |
|---|---|
| Sans métier | Vanilla refuse l'échange si la liste d'offres est vide (**à vérifier** dans `Villager#mobInteract`). Un sans-emploi irradié peut-il commercer ? |
| Idiot (nitwit) | Peut-il muter ? Peut-il commercer une fois irradié ? |
| Bébé | Un bébé peut-il muter ? Les enfants de parents irradiés naissent-ils irradiés ? |
| Montée de niveau | Les offres vanilla continuent de s'ajouter normalement, sans conflit attendu |
| Villageois errant | Inclus ou non ? (`WanderingTrader` n'est pas un `Villager`) |

### 4.4 Compatibilité

L'option A garde un `minecraft:villager` standard. Les mods qui manipulent les villageois (Easy
Villagers, mods de trades) devraient fonctionner, mais les mods de rendu de villageois peuvent
masquer notre couche.

---

## 5. Décisions de gameplay à prendre

1. **Comment irradier un villageois ?** Par le biome, par des objets radioactifs à proximité, par la
   contagion, ou par un objet dédié (seringue ou fiole) qui donne plus de contrôle au joueur.
2. **Dégâts** : option 1, 2 ou 3 de la section 4.1.
3. **Trades** : génériques par stade, ou par stade et par métier ? Quelle monnaie ?
4. **Réversibilité** : la décontamination fait-elle baisser le stade ? Le villageois garde-t-il ses
   trades irradiés ? (Équivalent de la guérison du zombie villageois.)
5. **Contagion** : un villageois irradié contamine-t-il ses voisins, avec un risque d'épidémie
   dans le village ?
6. **Relations** : les golems l'attaquent-ils ? Les prix changent-ils (réduction « de
   reconnaissance » comme après une guérison) ?
7. **Lien avec `ancient_fallout`** : des villageois déjà irradiés, à un stade avancé, peuvent-ils
   apparaître dans les ruines de l'ancienne base ?

---

## 6. Fichiers à produire

```
src/main/java/net/nuclearteam/createnuclear/
├── CNAttachmentTypes.java                         ← + VILLAGER_MUTATION
├── content/villager/
│   ├── VillagerMutation.java                      ← attachment (dose, stade, codecs)
│   ├── VillagerMutationHandler.java               ← accumulation, passage de stade, injection des trades
│   ├── IrradiatedVillagerTrades.java              ← pools de trades par stade / métier
│   └── client/
│       ├── IrradiatedVillagerLayer.java           ← textures par stade
│       └── IrradiatedVillagerEyesLayer.java       ← couche émissive
├── content/radiation/capability/RadiationCapability.java   ← appel au handler dans tickRadiation
├── content/radiation/RadiationEffect.java                  ← cas spécifique des dégâts villageois
└── infrastructure/config/CNCServer.java                    ← seuils de dose, activation

src/main/resources/assets/createnuclear/textures/entity/villager/irradiated/*.png
```

Plus les clés de langue (sous-titres de sons, messages éventuels) et un son de mutation dédié si
souhaité (`CNSoundEvents`).

---

## 7. Tests

| Test | Méthode |
|---|---|
| Accumulation de dose | Placer un villageois dans `irradiated_land`, lire l'attachment (étendre `RadiationInfoCommand`) |
| Passage de stade | Commande de debug pour forcer un stade, vérifier son, particules et visuel |
| Trades | Ouvrir l'interface d'échange à chaque stade |
| Survie | Exposition longue : vérifier que le villageois ne meurt pas (selon l'option 4.1 retenue) |
| Effacement vanilla | Retirer le poste de travail d'un villageois sans XP, vérifier la réinjection des offres |
| Persistance | Quitter et recharger le monde : stade, visuel et trades conservés |
| Multijoueur | Vérifier que le visuel est synchronisé pour tous les joueurs proches |

Une GameTest est possible pour l'accumulation et le passage de stade, sur le modèle des tests
existants (`empty_platform`).

---

## 8. Plan d'implémentation

1. Attachment `VILLAGER_MUTATION` et accumulation de dose dans `tickRadiation`.
2. Gestion des dégâts selon l'option retenue.
3. Passage de stade (son, particules) et commande de debug.
4. Couches de rendu avec des textures provisoires.
5. Registre de trades et injection, réinjection idempotente.
6. Config serveur (seuils, activation).
7. Textures définitives et équilibrage des trades.
