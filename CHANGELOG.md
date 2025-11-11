# CHANGELOG

### ⚙️ QOL (Quality of Life)

- Updated item tags for uranium and coal dusts in `CNItems` and `CNMixingRecipeGen` to make recipes more flexible and tag-driven. ([commit `9a32735`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/9a32735))
- Reorganized uranium processing recipes for improved clarity and consistency. ([commit `2f7ae97`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/2f7ae97), [commit `7842a12`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/7842a12))
- Refactored armor and dust tags to simplify tag management and ease future recipe changes. ([commit `2f7ae97`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/2f7ae97))

### 🐛 Bug Fixes

- Merged fixes from `Fix-create608&craft` ([PR #9](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/pull/9)) addressing crafting and compatibility issues. ([commit `c8cdf6d`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/c8cdf6d))
- Fixed issues in recipe generation and recipe definitions introduced during refactors. ([commit `159f735`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/159f735), [commit `7842a12`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/7842a12))

### 🧱 Additions

- No major feature additions in this set; the changes focus on refactors, tag updates and recipe reorganizations to prepare for ongoing compatibility work. (commits [`9a32735`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/9a32735), [`2f7ae97`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/2f7ae97), [`159f735`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/159f735), [`7842a12`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/7842a12))

### 🧩 Technical / Codebase

- Refactored anti-radiation armor item classes and updated recipe generation to be more modular and maintainable. ([commit `159f735`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/159f735))
- Reorganized recipe/data generation sources to reduce duplication and improve localization support. ([commit `7842a12`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/7842a12))

### 🔖 Contributors

- [@Giovanniricotta2002](https://github.com/Giovanniricotta2002)
- [@Create-Nuclear-Team](https://github.com/Create-Nuclear-Team)
- [@MathisGredt](https://github.com/MathisGredt)
- Community contributors via pull requests [#1](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/issues/1)

### 🧠 Notes

- This batch merges fixes intended to address crafting crashes and recipe issues related to recent refactors (see PR #9).
- Commits included in this changelog:
  - [commit `c8cdf6d`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/c8cdf6d) Merge pull request #9 from Create-Nuclear-Team/Fix-create608&craft
  - [commit `9a32735`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/9a32735) Update item tags for uranium and coal dusts in CNItems and CNMixingRecipeGen
  - [commit `2f7ae97`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/2f7ae97) Refactor armor and dust tags, reorganize recipes for uranium processing
  - [commit `159f735`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/159f735) Refactor anti-radiation armor item classes and update recipe generation
  - [commit `7842a12`](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/7842a12) Update recipes and localization for uranium processing and crafting
