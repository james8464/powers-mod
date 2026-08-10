# Arcane Crucible Implementation Plan

> Execute every rule and transaction test-first. Never accept client-authored result items or weapon data.

**Goal:** Add the two-input Arcane Crucible with safe alignment conversion, animated-star lightning binding, rune XP, cross-mod extension tags, complete assets, and race-proof transactions.

## Task 1: Weapon data, XP, and lightning math

**Tests:** add `CrucibleWeaponDataTest`, `CrucibleXpRulesTest`, `CrucibleLightningRulesTest` under `src/test/java/com/powers/forge/`.

1. Prove codec validation/clamping, threshold sequence, level cap 30, saturation/no overflow, energy formula, damage formula/caps, faction bonus, and zero cooldown.
2. Add `src/main/java/com/powers/forge/CrucibleWeaponData.java`, `CrucibleXpRules.java`, `CrucibleLightningRules.java`.
3. Register a persistent/network codec data component in `PowersDataComponents`.

## Task 2: Eligibility and transformation catalogue

**Tests:** add `CrucibleEligibilityTest` and `CrucibleTransformationCatalogueTest`.

1. Prove vanilla/mod weapon acceptance by tag, explicit API extension, alignment target filtering, mythic hard exclusions, and component-preservation rejection.
2. Add `CrucibleEligibility`, `CrucibleTransformation`, and data-reloadable `CrucibleTransformationCatalogue`.
3. Add `powers:arcane_crucible_base_weapons` tags and a small public registration API for compatibility callbacks.
4. Map existing non-mythic `PowersWeapons` to dark/light families without altering their current base behavior.

## Task 3: Atomic transaction engine

**Tests:** add `CrucibleTransactionTest` covering convert/bind/infuse, stale version, insufficient count, invalid component, full result destination, repeat packet, and rollback.

1. Add immutable `CrucibleChoice` and `CrucibleTransactionResult`.
2. Implement server-only `CrucibleTransactionEngine` with inventory version, mutation lock, copy-before-validate, one atomic commit, and exact consumption.
3. Preserve enchantments, name, lore, repair cost, safe third-party components, and proportional damage.
4. Make every failure side-effect-free and action-bar describable.

## Task 4: Block, block entity, menu, and packets

**Tests:** GameTests in `src/gametest/java/com/powers/gametest/ArcaneCrucibleGameTests.java`.

1. Register `ArcaneCrucibleBlock`, `ArcaneCrucibleBlockEntity`, menu type, block item, ticker, comparator, loot table, and translated name.
2. Implement two single-stack inputs, valid insertion rules, safe break drops, hopper constraints, viewers, and mutation lock.
3. Implement compact server choice synchronization and authenticated transmute button packet with distance/menu/block/version checks.
4. GameTest concurrent viewers, close/disconnect, block break, hopper, stale choice, full inventory/drop fallback, and no duplication.

## Task 5: Crucible screen and assets

**Tests:** extend resource validators and add layout math tests.

1. Build `ArcaneCrucibleScreen` from vanilla container widgets/nine-slice textures.
2. Reuse the existing brooding-forge block textures for off/on faces; add only necessary blockstate/model/UI sprites via `apply_patch` or the approved asset pipeline.
3. Preview server-provided target, catalyst effect, XP/level, lightning unlock, and concise validation reason.
4. Validate at GUI scales 1–4 and common aspect ratios.

## Task 6: Rune acquisition and ordinary recipes

**Tests:** loot/recipe/resource validator assertions.

1. Assign existing runes to common/uncommon/rare/ancient XP tags.
2. Add documented balanced crafting recipes and world loot injection for runes only.
3. Confirm no crystal or mythic artifact recipe is introduced.

## Task 7: Star-bound weapon lightning

**Tests:** integration/GameTests for targeting, energy, alignment, PvP/safe-zone/amethyst, repeat input, high-health mob damage, and semantic FX.

1. Add server use/input route that recognizes only valid star-bound component data.
2. Reuse lightning impact validation and source attribution, replace cooldown with same-tick nonce/rate protection, charge computed energy, apply capped level damage.
3. Use bolt/thunder as `MINIMAL` presentation with alignment tint only at impact.
4. Add tooltip lines for alignment, level/XP, energy, and lightning damage.

## Task 8: Crucible checkpoint

1. Run all forge tests/GameTests, full test, client compile, resource validators, and server boot.
2. Test a vanilla weapon and a tagged synthetic third-party weapon through all three stages.
3. Update README/API documentation and interaction catalogue.
4. Commit: `feat: add the arcane crucible`.

