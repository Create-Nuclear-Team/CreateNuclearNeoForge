
# New version available on CurseForge & Modrinth:
- mod version: v2.0.0
- mod loader: NeoForge
- game version: 1.21.1

## Notes
- Version bump: **v2.0.0** (from v2.0.0)

## Additions
- [69a21afc](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/69a21afc68e9bb39d695a6f6d6024ff1903b832e) — Add a display name for the irradiated_land biome and add an optional Xaero's Minimap dependency (Giovanni)
- [627c9761](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/627c9761821385a9a8785df87f67987e0ef15727) — Add optional TFMG compat for the limesand-to-nitrate recipe (Giovanni)


## Technical / Codebase
- [11a22b60](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/11a22b601f052152eacdc33f2c42ced46de46498) — Reduce reactor sound range (Gio)
- [875f8915](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/875f8915ea083273454af7f545092b76af2ce42c) — Stop reactor outputs ticking rotation when no pattern is assembled (Giovanni)
- [eb519c68](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/eb519c686f4872cd179e6491da9e32462ca71035) — Fix anti-radiation suit model sharing state across equipment slots and add compat mod stubs (Giovanni)
- [c883059f](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/c883059f74b251657f8e8f0fb54aac0d9e86650b) — Make the reactor blueprint's keybind hint reflect the actual configured keys (Giovanni)
- [563e54fa](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/563e54fa68e0ecd3e0f89c4897e4b0659fd70791) — Refresh the generated-resources cache after a gradle runData re-run (Giovanni)
- [009afa21](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/009afa21deb4945cff14f95afe05ff0200cc6c11) — Avoid ConcurrentModificationException when positions changes during rotateOutputs (Giovanni)
- [7b9104f6](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/7b9104f6fffd18e1dc26143f5225815e95a641e6) — Stop reactor outputs registering phantom stress capacity on world reload (Giovanni)
- [79421ec6](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/79421ec6c9d3b1a427cb5505e9ae30d006495182) — Reject reactor blueprints with no rods in the controller (Gio)

# New version available on CurseForge & Modrinth:
- mod version: v2.0.0
- mod loader: NeoForge
- game version: 1.21.1

## Notes
- Version bump: **v2.0.0** (from 2.0.0-beta3)

## Additions
- [583383d5](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/583383d59277e44b1accc7e5c92359d20da83159) — Add optional Create Dragons Plus compat for the snow powder fan recipe (Giovanni)


## Technical / Codebase
- [871837ac](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/871837ac2fc55a2492aa82093f7cfe91d5598902) — Correct death message translation keys for radiation damage types (Giovanni)

# New version available on CurseForge & Modrinth:
- mod version: 2.0.0-beta3
- mod loader: NeoForge
- game version: 1.21.1

## Notes
- Version bump: **2.0.0-beta3** (from 2.0.0-beta2)

## Technical / Codebase
- [5406b513](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/5406b513bd8757d0920551a46719e4d9d8c7e946) — Use markdown headings for cliff-testers.toml sections (Giovanni)
- [5675765f](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/5675765f843fac1653459811400a1554d19f7217) — Unify SpecialBlockStateGen duplicates into ReactorIOGenerator (Giovanni)
- [d1a6ee86](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/d1a6ee86cfecbd8c4b39df1f68252af79cd75120) — Dedupe multiblock matching, display sources, armor recipes, and density functions (Giovanni)
- [331e9637](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/331e9637b217b30562bd2a3981c891f263bfe490) — Dedupe ore block registration, remove more dead code (Giovanni)
- [8b9ce285](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/8b9ce285dd8142f252b79bd16936e8a781eb33d3) — Allow fluid-input interaction outside creative mode (Giovanni)
- [4f44289f](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/4f44289f53fc5daffce1a21830c83a8e85635552) — Unify manager NBT read/write and dedupe global-pos variable (Giovanni)
- [fc94ef15](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/fc94ef15480beca5df1cb069013539dfa888a611) — Prune dead CNTags matches()/constructors, rename decompiled MCP params (Giovanni)
- [f65b477e](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/f65b477ea547ac865644c421ae1bedd6a7ed4b99) — Dedupe throw-to-pickup slot redirect, drop identical wolf texture (Giovanni)
- [eaa16d28](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/eaa16d287f2d8bd2349da5862c618a0eb3054afc) — Remove dead code in JEI, IrradiatedCat, and recipe gen; drop stale @SuppressWarnings (Giovanni)
- [2096a626](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/2096a626c70adb4304f13c8f1899b497579e1223) — Extract AbstractReactorMultiblockPart, RegistryTypeResolver, and drop remaining dead code (Giovanni)
- [a20efd9d](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/a20efd9d47066716c6b89146e817d900fdb221c9) — Cleanup follow-up on AbstractReactorMultiblockPart/RegistryTypeResolver extraction (Giovanni)

# New version available on CurseForge & Modrinth:
- mod version: 2.0.0-beta2
- mod loader: NeoForge
- game version: 1.21.1

## Notes
- Version bump: **2.0.0-beta2** (from 2.0.0-beta1)

## Technical / Codebase
- [bf569b71](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/bf569b7179e6c21f23f11a2ed779c0cca777c75a) — Resolve reactor explosion/notification positions out of Sable sub-levels, and fix client-side mushroom cloud duplication (Giovanni)
- [d312fed5](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/d312fed5d49c77b9db62df875660539d01ec5659) — Destroy the entire multiblock structure on meltdown, not just the controller block (Giovanni)
- [5fa0cb75](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/5fa0cb752bf56a2baf24f1b12a883ed8dc6ec3fa) — Make Create Aeronautics/Sable opt-in at runtime via a gradle.properties flag (Giovanni)
- [7c27b918](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/7c27b9186c91ace93ecc2aa9c467a105ad303d58) — Remove dead code confirmed by the incremental 06/09 audit pass and fix reactor blueprint interaction outside the assembled state (Giovanni)
- [bdc9dc51](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/bdc9dc519aa7d9b701a57192f7c84b4310deb74a) — Remove unused overlay.event_message tooltip key and delete the empty reactor alarm block model (Gio)
- [cd82e97c](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/cd82e97cec5abed3bbd87b284a85ad42be5a7c93) — Centralize per-material common tags in a new CNMaterialTags enum (Giovanni)
- [56307e94](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/56307e94429998e6350d76e8257ec7f7c9d27cb6) — Replace the wildcard java.util import in CNMaterialTags with explicit imports (Giovanni)
- [2b653df2](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/2b653df2048f74648a01a69b1e41752716099421) — Document a second full audit pass covering src/main/java outside content/multiblock (Giovanni)
- [c6aa3f48](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/c6aa3f48f6970941f2402150d8b983b259c65454) — Remove confirmed dead code from the second full audit pass and fix the damageTypes package name mismatch (Giovanni)
- [06124dda](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/06124ddab12c617071beaedd0e7b0b372e398997) — Remove the dead code confirmed by the second full audit pass, reflow indentation, and drop wildcard imports (Giovanni)
- [9dfb029e](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/9dfb029e22ad5f6878347f8c901470dfff47e24a) — Remove the remaining dead field/constant findings from PaletteBlockPattern and CRods (Giovanni)
- [0dd14b44](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/0dd14b4445eba4cbcfefc12d774326d5c22e7c04) — Translate the remaining French comments and Javadoc to English (Giovanni)
- [d0702dc0](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/d0702dc000ebc7b2f401ce605f4bc593f0b5d7d6) — Rename CommentEvents to CommonEvents, translate sound paths, and fix Javadoc placement (Giovanni)
- [35f0ff7c](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/35f0ff7c7f9a92abb7f872b0a7fdae7a5a70e539) — Extract DirectionalReactorBlock and deduplicate fluid-lock/decompacting-recipe logic (Giovanni)
- [aac141ad](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/aac141ad425ab967c05d5cccbaf61a7fc39ce90e) — Log the DirectionalReactorBlock, decompacting-recipe, and fluid-lock deduplication in AUDIT_NEOFORGE.md (Giovanni)
- [73c77250](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/73c77250b863324bbcfb7f4332c8e0edd3839b41) — Unify manager position storage/scanning, extract shared validators, and fix several stale Javadoc/comments (Giovanni)
- [42a7d619](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/42a7d6195aca59a3dff9f96c912d5f75bd3888a8) — Correct entity type in getBlocksPosition after manager merge (Giovanni)
- [c8819617](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/c8819617770b553e171bf3a10e14f391de04b6ae) — Unify ReactorPattern controller scan and prune dead code (Giovanni)
- [340b2588](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/340b2588b07db50e7073631a550b75ed93d823ee) — Align crushing recipe secondary outputs with Forge parity (Giovanni)
- [e468726d](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/e468726dfef3b315cb2346ea852d8785dbdbea84) — Drop the RAW_URANIUM_BLOCK crushing recipe (Giovanni)
- [1fe80c4b](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/1fe80c4bb819fd912efc118962d994c04d898157) — Log the CNCrushingRecipeGen recipe fix in AUDIT_NEOFORGE.md (Giovanni)
- [f8b80034](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/f8b80034ead85c844c0e3d9bef1fc5c5bdea11bf) — Add git-cliff configs for release and tester changelogs (Giovanni)

# New version available on CurseForge & Modrinth:
- mod version: 2.0.0-beta1
- mod loader: NeoForge
- game version: 1.21.1

## Notes
- Version bump: **2.0.0-beta1** (from v1.3.2-beta.1)

## Additions
- [71fdcdfe](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/71fdcdfeb8e8aabed183bed65239b79f8708c738) — Port lot 0 - prerequisites for the Forge reactor logic (mathi)
- [ce17de9a](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/ce17de9a8e5f246b9e269c70f408196fe6770868) — Port lot 1 - thermal model (reactorLogic) (mathi)
- [6b406773](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/6b4067730b3672b73657d299ea9e315a3bdf5df1) — Port lot 2 - pattern reading and rod consumption (mathi)
- [521d6cfa](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/521d6cfadb56a03ff870b07d0649f179450e8ae0) — Port lot 3 - reactor services (12 of 14) (mathi)
- [b85f2a04](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/b85f2a041053ecd5ee497d7db7ffe6071e17699c) — Port lot 4 - input snapshot and frame/goggle display (mathi)
- [6ab2da30](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/6ab2da30232298a1ded4bb0993bed863fc860cc9) — Port lot 5 - running sound and debug diagnostics (mathi)
- [49c7fb7c](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/49c7fb7c247daf7aff93f2da1b1fc74e2aa4aad8) — Port lot 6 - rewire the controller block entity (mathi)
- [3f99c878](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/3f99c8784a55c269a8ffd14bec17bbe545b2edcb) — Porter les jauges DisplayLink du reacteur (mathi)
- [489d7a17](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/489d7a1772eb5ca45f11ff879f3d5777d7eab86f) — Porter les 4 fichiers divers du chantier 2 (mathi)
- [7a1f4a02](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/7a1f4a02fbeb4eaeaa2a956f6720ddf9df2f18b3) — Add snow powder processing and related recipes (mathi)
- [34189d74](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/34189d7476a81703784da8133683a86767071bc0) — Route radiation damage through RadiationCapability's dose/contagion model (Giovanni)
- [5350638c](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/5350638cc829461591e27a604bcbe84f7b9141f2) — Add the irradiated cow, rename the helmet-detection tag, and extract shared AnimalUtil helpers (Giovanni)
- [e64e18b0](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/e64e18b0e88185d6444bfe4bae893e9f20457f43) — Add the missing thorium fluid/compacting recipes and the reactor alarm recipe (Giovanni)
- [7d164310](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/7d16431023564283446db47f0b68c06737728d3e) — Add the IrradiatedAnimal abstraction, wire it into the chicken, and align the cat with vanilla Cat (Giovanni)
- [b4a5d83f](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/b4a5d83f2de27a24c3be603926f48f729b0e4c42) — Re-enable 6 disabled advancements, register the anti-radiation helmet as a Create goggle, and audit 5 previously-unreviewed files (Giovanni)


## Technical / Codebase
- [a8213417](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/a8213417387aecc40be613e6d6f872a59d8f2400) — Port the two gametests, fix an out-of-bounds tank read (mathi)
- [56685b7a](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/56685b7ae01397ac7150b6796f87dd8cbe51d7b0) — Untrack the world generated by runGameTestServer (mathi)
- [1e58d7c8](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/1e58d7c855fec67caa16f75ad65f3caaa4431eba) — Stop the world save crashing on a heat of zero (mathi)
- [12a7fc0a](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/12a7fc0a50fc91b53932b97c64e2bbc134f3a162) — Dark frame fluid, two-slot rod input, unregistered nitrogen (mathi)
- [1eacc3ce](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/1eacc3ceed71c23078a44e0e543e56b31dcb25bf) — Restore the output stress capacity and the missing render layers (mathi)
- [a8f23b83](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/a8f23b830fb22f873c1f9211f95bb13c4ee1dd2b) — Close out the reactor port and inventory what is left (mathi)
- [90f02adf](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/90f02adf188783f9dd0fe24e34c51b1b6550d1e3) — Restore potion.png, deleted by accident in the merge (mathi)
- [b256ce41](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/b256ce41d28b9a4bc928de9220c1ec196042859f) — Stop extractFluids over-draining across fluid inputs (mathi)
- [8aacafa6](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/8aacafa6a278db69927f6062d30c4383382e19ad) — Record the extractFluids fix and the merge of origin/V2 (mathi)
- [0727862a](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/0727862a35a9ed1cb08bd65d04c65122a2bb1776) — Remove potion.png (mathi)
- [9e7aba01](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/9e7aba0140cb1731cc64eb41c284cb8b57edf537) — Add the Forge -> NeoForge parity roadmap (mathi)
- [a8d1f1b1](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/a8d1f1b18d0f0ba46116fe640d5e603cc9e40f6b) — Le controleur ne persistait plus des qu'il portait un blueprint (mathi)
- [4cd8a46b](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/4cd8a46b680cb65d6100578244371f342d5e9871) — Marquer le chantier 1 fait et consigner le bug de persistance (mathi)
- [b5534d2d](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/b5534d2d3842f091d4a418c37cea176b3c28e424) — Aligner les 8 noms de fichiers divergents sur Forge (mathi)
- [b86cbb01](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/b86cbb013f1876ee6097739aa71d28136b853ff5) — Pointer les liens de depot vers l'organisation Create-Nuclear-Team (mathi)
- [3b89c0b1](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/3b89c0b165a225b3011798aa94566f5dd7546e78) — Reduire la roadmap de parite au seul travail restant (mathi)
- [6e71c110](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/6e71c11071a0490c19a722b71281434b93c43c43) — Change RodType to hold a single item instead of a HolderSet (Giovanni)
- [4fba3eb4](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/4fba3eb4c8747ef64b0302969fb88eb54fbf6093) — Mirror RodType's single-item model onto ReactorFluidType (Giovanni)
- [c25ca35a](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/c25ca35a413f0cadc8eb334607e24e0912761c18) — Subscribe RadiationCapability's event bus on the default bus (Giovanni)
- [5d69829d](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/5d69829d929735548400df81cccc57f40ff9f4e8) — Replace SimpleCriterionTrigger with a custom base trigger implementation (Giovanni)
- [41f61767](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/41f617675f073ec1cb0e21c285d96be069a5f6db) — Move EnrichedRecipeGen/SnowPowderRecipeGen from foundation to the api package (Giovanni)
- [0c259fc0](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/0c259fc0005ea6eeb7c6159abe468bc4e4864e2c) — Classify reactor blueprint rods via RodType instead of item tags (Giovanni)
- [d2a92a49](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/d2a92a493998faef69d2964f31d90bce17fa0111) — Drop stored graphite/uranium timers and derive patternAll on demand (Giovanni)
- [3e87ed2d](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/3e87ed2dd7e12f881fa03fd6c559f7dcb7275558) — Back custom sound entries with DeferredHolder instead of an eagerly-created SoundEvent (Giovanni)
- [66bcc7e6](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/66bcc7e6a1816026fa27b70b4342d1fc663cd79d) — Switch RadiationCapability to a serializable+syncable attachment and clean up codebase-wide leftovers (Giovanni)
- [aa37ff5c](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/aa37ff5c3de7566ffe0edadc08b250ae56eaf1ec) — Update reactor/alarm/biome sound files and remove unused reactor-casing sounds (Giovanni)
- [d7a380ee](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/d7a380eeb439bc176803b4ac555b8df3ddee82ad) — Add architecture write-up for dynamic irradiated-mob trait evolution (Giovanni)
- [c0bed295](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/c0bed295d7a3adc34048c44f7aee12cffde52658) — Move ClothItemStack to ClothItem, switch biome extractor charge to a typed component (Giovanni)
- [fa48da6d](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/fa48da6d6377438371918ddd225f8ba2c65374cc) — Restore the radiation vision overlay and remove dead event-text/vision-overlay duplication (Giovanni)
- [b6e263ad](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/b6e263ad6a09a15bee5fb13d2e74ebe168906a22) — Fix indentation and remove stray blank line in ReactorControllerBlockEntity (Giovanni)
- [107bdc49](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/107bdc49dc234e9bfbce6a49122db19e322265d9) — Clean up dead imports/logic and update port tracking doc after RodType alignment (Giovanni)
- [b8ef440e](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/b8ef440ebe7019044823ce91aa9ff1c1f4c413df) — Update PORTAGE_REACTEUR.md gametest count and run/server.properties timestamp (Giovanni)
- [970503ba](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/970503ba0738b6b7c3a46d5c7165b369c8d11cb8) — Default RodType.Builder.ratio to 1 to prevent an NPE on unset rods (Giovanni)
- [9b78da79](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/9b78da796a78cf3e525b8427971c08f8ba810e9d) — Mark the RodType.ratio() NPE bug as fixed and correct stale icon.png claim (Giovanni)
- [93975b3a](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/93975b3a5099ffa5fdfb6007000862f2c315e822) — Stop crushing/washing recipe generators from leaking into the create namespace (Giovanni)
- [90168031](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/9016803147cb81353958ec2334f47dff35933672) — Mark the create-namespace recipe leak as fixed in PARITE_FORGE_NEOFORGE.md (Giovanni)
- [4566ef97](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/4566ef97a2130c303e3ca599630ccb6bf58e0c72) — Fix mod logo resolution and remove stray commented-out provider registration (Giovanni)
- [09ab367b](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/09ab367bf7ffc2426e0afcfd7ab6bcac284c8422) — Confirm the logoFile fix and note an IntelliJ stale-output pitfall (Giovanni)
- [a333f75e](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/a333f75ed515678d26ec1a8b14651a31cffcccb5) — Move anti-radiation armor texture into the blocks atlas and fix a stale particle path (Giovanni)
- [3ea62552](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/3ea625525c9f26ea3f06b6039a32c0b4aaa3e0d6) — Document the anti-radiation armor atlas fix and the dead default-texture reference (Giovanni)
- [4918bf77](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/4918bf774318ed76319170db97296007da7c22aa) — Remove the orphaned IrradiatedWoldCollarLayer render layer (Giovanni)
- [0fb35c0e](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/0fb35c0e4ec26376cda9f97f49af058cb874176e) — Remove the redundant CommentEventClients subscriber, fold layer registration into CNClientEvent (Giovanni)
- [72f4138d](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/72f4138dfd2c75148bcfa314d4b057ab134310e4) — Document the CommentEventClients merge into CNClientEvent (Giovanni)
- [6fe898db](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/6fe898dbd1eb7d48de58bb9dc86500dd7744a867) — Switch ore/storage-block tags to NeoForge's common `c` tag conventions (Giovanni)
- [d098d831](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/d098d8317d6fe8ca5848f8cf8943815dedbd8785) — Stop cloth-dyeing recipes from allowing a cloth to craft itself (Giovanni)
- [3fe96a68](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/3fe96a68bfb4858758bbcf7b6997c3b786ab0c7d) — Fix indentation on REINFORCED_GLASS_BOTTLE and IRRADIATION_BIOME_EXTRACTOR (Giovanni)
- [b10ce958](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/b10ce95821c5657b9b27358f0b66a8afcb1e0519) — Restore missing set_count on uranium/lead ore loot tables and complete the CNBlocks/CNItems parity audit (Giovanni)
- [87333150](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/873331502ab95eafa90a8c1d636363fc3582fccf) — Actually apply anti-radiation armor durability, switch Jade to Modrinth, and fix HUD depth-mask occlusion (Giovanni)
- [6103e5ba](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/6103e5ba7c1e34275610ae8c18aae3d6d4ef5493) — Mark the 6 re-enabled advancements as fixed and document the AVOIDING_CANCER trigger pitfall (Giovanni)
- [d9cfe0ea](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/d9cfe0eab10c2ad8d5d8931692a768a44868b2e4) — Align IrradiatedWolf with Forge's WIP-blocked taming and restore the missing panic goal (Giovanni)
- [151f2c07](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/151f2c07e37036fdd0174656d98b8ccbed4f12a1) — Confirm the dead getStandingEyeHeight override on IrradiatedCat/IrradiatedCow and defer its fix out of scope (Giovanni)
- [9f3ef5ba](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/9f3ef5baac07d5549ba1476e6b05736420f9e66c) — Align ore generation values with Forge and re-enable dormant armor/camera code paths (Giovanni)
- [c030e8d1](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/c030e8d119b17bf1458080428537d02099db5794) — Update repository configurations and improve dependency management (mathi)
- [f4a3a754](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/f4a3a75401082ca2a1c38b0396f997505cc53f2a) — Link the reactor blueprint's wiki chat message to its specific page (Giovanni)
- [d2c70789](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/d2c707893c12979ba9ae5bf9f8b940713764eb48) — Remove dead code and fix silk-touch enchantment lookup for uranium ore XP drops (Giovanni)
- [f42c87b6](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/f42c87b65552a30f5d06fdd4e17eac8b09c10493) — Expand AUDIT_NEOFORGE.md with a full second-pass review of src/main/java (Giovanni)
- [734fffb6](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/734fffb6a2900fee5c624302a694c052bdfd779e) — Resolve the critical/important bugs found in the 23 August audit (B2-B6, Maths.java, CNBasicModelPart) (Giovanni)
- [f8455bc6](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/f8455bc697e44a111059802f48d4d0426e58b72d) — Remove the unfinished FluidConsumable placeholder (Giovanni)
- [3de660ab](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/3de660ab60c97f2f7e242c730671a49b1620cd42) — Remove dead methods and unused imports flagged in the AUDIT_NEOFORGE.md dead-code audit (Giovanni)
- [19b90823](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/19b90823e68833bae8ce1cdad85011a747abd819) — Remove the remaining dead code confirmed by the AUDIT_NEOFORGE.md audit (Giovanni)
- [2bb25e2e](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/2bb25e2eacd2c9a782ad0fcaefe80f891dbdaec1) — Rename CNTags' forgeXxxTag helpers to neoForgeXxxTag and finish the leftover-import cleanup (Giovanni)
- [f487f929](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/f487f92991254eeadad0d49a6e9811775bbeb491) — Restore the missing reactor I/O placement ponder scene NBT (Giovanni)
- [4ebbd26a](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/4ebbd26a57c8127ecb8807d3be990d11ee6b2629) — Make the reactor multiblock work correctly under Create Aeronautics/Sable, and fix the radiation-cleaner bottle's missing charge icon (Giovanni)
- [6c75b059](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/6c75b05992fec7430e46349bac8cba4e4ed0814e) — Fix indentation in ReactorControllerBlock and remove a stray blank line in ReactorRodInput (Giovanni)


## V2
- [cfc42ed3](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/cfc42ed33613239b7efb97fc5946df4c98830f8b) — Updated id for desagne mob (Gio)


## V2-Reacteur
- [2fa14573](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/2fa14573ae812c2dd37301830a21abce73f69ff0) — Updated discution (Gio)

# New version available on CurseForge & Modrinth:
- mod version: v1.3.2-beta.1
- mod loader: NeoForge
- game version: 1.21.1

## Notes
- Version bump: **v1.3.2-beta.1** (from )

## main
- [470c062b](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/470c062bd79cf6ec6daeb647422939af2920d1cb) — Updated (Giovanni)
- [0f6b64c8](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/0f6b64c8cd091b892389a1130eee2ecdff9f92ec) — Added licence (Giovanni)
- [13c8aac6](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/13c8aac6f0a73b3df27b7257e9363e05b10c37de) — Updated (Giovanni)


---

*If you want to help us translate to other languages: https://crowdin.com/project/createnuclear*
*Some translations were verified/completed using AI, if you find any issues let us know on our Discord!*

# Links:
### Curseforge:
https://www.curseforge.com/minecraft/mc-mods/createnuclear

### Modrinth:
https://modrinth.com/mod/createnuclear
