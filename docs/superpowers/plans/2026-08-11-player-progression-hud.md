# Player, Progression, and HUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Deliver the final innate roster, explicit rank behavior, clean status presentation, continuous energy rules, readable progression UI, first-join guide, and testing controls.

**Architecture:** Keep registration and activation server-authoritative. Replace passive/perk composition with a small authored innate-level profile consumed at each ability boundary; keep spell/crystal cast contexts unranked. Keep HUD placement pure and testable, with rendering limited to client source.

**Tech Stack:** Fabric API 0.156.0+26.2, Minecraft 26.2, Java 25, JUnit 6, Fabric GameTest.

## Global Constraints

- Exactly 23 innate powers remain after removing five requested powers.
- No automatic innate passive or aura particle remains.
- Crystals and grimoires never use innate rank scaling.
- Energy renders as ten vanilla-style symbols directly above hunger.
- All behavior changes follow RED → GREEN → affected-suite verification.

---

### Task 1: Roster and passive migration

**Files:** `Power.java`, `PowerRegistry.java`, `PowersMod.java`, `PlayerPowers.java`, removed ability classes, `PowerRegistryTest.java`, `PlayerPowersTest.java`, language/docs.

**Interfaces:** Produce `Power(id,name,description,color,ability,affinity)` and `PowerRegistry.reconcile(List<String>, PowerAffinity)` with deterministic safe replacements.

- [x] Add tests asserting 23 unique powers, removed IDs resolve to null, every registration has no passive collection, and legacy slot IDs migrate deterministically.
- [x] Run `./gradlew test --tests com.powers.power.PowerRegistryTest --tests com.powers.player.PlayerPowersTest` and confirm failures name the old roster/passive contract.
- [x] Remove `PassiveEffect`, passive registration/imports, `refreshPassives`, `tickAuras`, obsolete ability classes, catalogue/docs/lang references, and clear only legacy POWERS-owned ambient effects once during data migration.
- [x] Re-run focused tests and `./gradlew compileJava compileClientJava validatePowerResources`.
- [x] Commit as `refactor: remove innate passives and retired powers`.

### Task 2: Toggle drain, invisibility, healing, and messages

**Files:** `PowerEnergy.java`, toggle abilities/managers, `InvisibilityToggleAbility.java`, `PlantHealingAbility.java`, `PowerMessages.java`, `PowerEnergyTest.java`, ability tests/GameTests.

**Interfaces:** Produce `PowerEnergy.ongoingCost(ServerPlayer, Ability)` where every active indefinite ability returns a positive per-second cost; use hidden amplifier-255 infinite Invisibility owned by the toggle.

- [x] Add table-driven tests proving every toggle has upkeep, Time Freeze is the largest innate drain, testing overrides bypass only enabled constraints, invisibility add/clear ownership, zero healing cooldown, and a two-block inclusive heal radius.
- [x] Run the focused tests and verify the zero-upkeep/old invisibility/range cases fail.
- [x] Centralize upkeep payment and forced toggle shutdown; replace routine chat with silence or actionbar failures; implement hidden Invisibility and crouch healing.
- [x] Run focused tests plus `./gradlew test --tests '*PowerMessagesTest' --tests '*PowerStatusEffectsTest'`.
- [x] Commit as `fix: make active powers explicit and continuously draining`.

### Task 3: Explicit innate level profiles

**Files:** create `InnatePowerLevel.java`, `InnatePowerLevels.java`; modify `PowerScalingService.java`, all 23 innate abilities, rank JSON/loader; tests.

**Interfaces:** `InnatePowerLevels.forPower(String id, int level)` returns literal authored `damage`, `range`, `duration`, `destruction`, `capacity`, and `variant` values for levels 0-10; unknown/invalid levels clamp safely.

- [x] Add literal table tests covering all 253 power-level combinations and source-context tests proving `SPELL`, `CRYSTAL`, and `ARTIFACT` never inherit an innate profile.
- [x] Run tests and confirm they fail because generic percentage scaling is still used.
- [x] Implement the authored table and update each ability to consume only relevant fields (for example Fireball explosion/destruction tiers, Teleport range, Forcefield durability, possession duration/rank gate, beam width/damage).
- [x] Delete generic rank-perk mechanical composition while retaining title/lore branch state and save compatibility aliases.
- [x] Run all progression, cast-context, ability, catalogue, and interaction tests.
- [x] Commit as `feat: author every innate power level`.

### Task 4: Rank trees, HUD, and first-join guide

**Files:** `RankMazeLayout.java`, `RankMazeScreen.java`, light/dark rank JSON, `HudLayout.java`, `EnergyHudRenderer.java`, energy textures, new `PlayerGuide.java`; client/unit/GameTests.

**Interfaces:** `RankMazeLayout.arrange` produces non-overlapping rooted-tree bands with readable node widths; `HudLayout` anchors ten symbols to hunger and offsets for conditional vanilla rows.

- [x] Add layout tests for rooted edges, no overlaps, full title width, alignment artwork, ten-symbol x coordinates, correct half-fill orientation, and extra-heart/mount/air offsets.
- [x] Run focused tests and capture expected failures from the lattice and half-symbol orientation.
- [x] Replace lattice rendering and generic perk text; expose awakening requirements and level effects; correct texture paths/fills; update energy textures without changing vanilla-aligned structure.
- [x] Give each first-time player one authored written book using a persistent received flag; include controls and progression basics.
- [x] Run client compilation, resource validation, focused tests, and client screenshot smoke at supported GUI scales.
- [x] Commit as `feat: clarify progression and vanilla-align the HUD`.

### Task 5: Operator testing controls and diagnostics integration

**Files:** existing testing command/state classes, `PowersCommands.java`, `PowersDiagnoseCommand.java`, tests and README.

**Interfaces:** `/powers testing energy <on|off>`, `/powers testing cooldowns <on|off>`, `/powers testing reset`; per-player persistent state; diagnose reports it.

- [x] Extend command tests for permissions, independent flags, reset, logout persistence policy, and actionbar confirmation.
- [x] Run focused tests; implement only missing paths and connect central payment/cooldown gates.
- [x] Run command tests and a dedicated-server command smoke.
- [x] Commit as `test: complete operator gameplay overrides`.
