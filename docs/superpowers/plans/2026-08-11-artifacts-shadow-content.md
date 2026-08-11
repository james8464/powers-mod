# Artifacts, Shadow, and Content Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Simplify artifact combat selection, finish crystal/item behavior, guarantee meaningful progression loot, and replace the Knowledge Book AI with the approved skin-matched Shadow companion and global reveal mode.

**Architecture:** Artifact catalogues are immutable server definitions with persistent eight-key loadouts. Shadow is server-authored session state rendered as client-local player representations, so privacy and global reveal require no physical mob, fake player, or chunk ticket. Curated knowledge remains a reusable server service; the custom Knowledge Book transport/UI is removed.

**Tech Stack:** Fabric networking/chat events, Minecraft profiles/player rendering/loot/item entities, Java 25, JUnit 6, Fabric GameTest.

## Global Constraints

- Shadow Sword has all remaining routed innate/crystal powers and exactly three unique actions.
- Heavenly Partisan has a smaller curated roster.
- Rank-10 Darkness artifact casts have no gameplay cooldown; workload budgets remain.
- Shadow uses the owner’s skin/model with no equipment and is owner-private until globally revealed.

---

### Task 1: Crystal roster and infected visual

**Files:** `CrystalAbilityCatalog.java`, `PowersItems.java`, item rendering/model predicates or component, crystal tests/resources/docs.

- [x] Add failing tests for Green/Indigo exact rosters, removed Infected Rainbow registration, owner-sensitive Rainbow appearance, Orange clone identity, structure allowlist, and Yellow scale bounds.
- [x] Remove the separate item save-safely through missing-item migration documentation; make normal Rainbow visual state derive from holder alignment without mutating stacks.
- [x] Replace wolf clones with unarmed skin-matched player-like clones and Creativity chamber with authenticated allowlisted structure placement.
- [x] Run focused tests, resource validation, and live crystal GameTests.
- [x] Commit as `feat: complete crystal identities`.

### Task 2: Artifact roster, migration, and combat UI

**Files:** `ArtifactActionCatalogue.java`, favourite/loadout rules/persistence, wheel/library screens/packets/HUD glyph component, tests.

- [x] Add failing tests for exactly three Shadow uniques, Partisan strict-subset roster, rank-10 cooldown bypass, legacy key/favourite migration, eight defaults, search/category snapshots, and server selection validation.
- [x] Remove surplus Partisan uniques, preserve their best visuals as routed presentation, and implement deterministic loadout migration.
- [x] Replace the flat paginated catalogue with responsive tabs/search/icon grid and persistent favourites; keep radial release/1-8/shift-scroll fast paths and accessibility states.
- [x] Run catalogue/menu/network tests and client visual smoke.
- [x] Commit as `feat: streamline mythic artifact combat`.

### Task 3: Miniportal and item purpose closure

**Files:** First Vessel loot, Miniportal item/state/recharge handler, item registries/lang/README, tests/GameTests.

**Interfaces:** charged device stores `charges=2`; each successful same-dimension coordinate trip decrements once; overlapping dropped amethyst shard restores two and consumes one shard.

- [x] Add failing tests for guaranteed boss drop, dimension rejection, two-use lifecycle, failed-trip non-consumption, dropped recharge, and component persistence.
- [x] Implement shared teleport-storm routing and dropped-item recharge with bounded nearby-item indexing.
- [x] Normalize every translation name and document purpose/acquisition/deferred state for every registered item family.
- [x] Run loot/item/travel GameTests and resource/lang audits.
- [x] Commit as `feat: finish miniportal and artifact purposes`.

### Task 4: Shadow chat, visibility, and player rendering

**Files:** replace private companion manager/rules/ghost/client/packets; add `ShadowSession.java`, `ShadowChatRouter.java`, `ShadowVisibility.java`, `ShadowClientAvatar.java`; tests/GameTests.

**Interfaces:** case-insensitive prefix `shadow,`; deterministic commands `reveal yourself` and `hide yourself`; hidden recipients `{owner}`; revealed dialogue recipients are all online players; avatar recipients are owner plus all clients tracking the owner’s current dimension.

- [x] Add failing tests for eligibility, prompt consumption, hidden/global recipients, reveal/hide transitions, owner-always-visible, join/dimension cleanup, response length/rate/concurrency, sanitized remote context, and safe doglike follow/teleport points.
- [x] Implement signed-chat allow hook that consumes the owner prompt, deterministic commands before AI, offline curated answer lookup, and optional bounded privacy-filtered provider.
- [x] Render client-local `RemotePlayer`-style avatars from owner `GameProfile`, clear all equipment layers, interpolate safe follow movement, and teleport with semantic Darkness FX.
- [x] Run companion/network/privacy tests plus two-client live acceptance checks.
- [x] Commit as `feat: reforge knowledge into the living Shadow`.

### Task 5: Remove custom Knowledge Book and finish lore/content

**Files:** remove Knowledge Book mixin/screen/packets/history/loot/advancement; retain and rename reusable curated knowledge index/provider; realm/item/lore data and README.

- [x] Add a GameTest proving vanilla Knowledge Book use is no longer intercepted and Shadow answers the former curated queries.
- [x] Remove custom UI/network/loot progression and registration; update mixin manifests safely.
- [x] Add documented realm structures, factions, hazards, bosses/events, artifact acquisition chains, and alignment interactions only where complete code/data and tests ship together.
- [x] Run resource/mixin/loot/advancement tests and dedicated-server boot.
- [x] Commit as `refactor: move arcane knowledge into Shadow`.
