# Opposed Artifacts Implementation Plan

> Execute test-first and preserve the complete existing Shadow Sword roster while migrating it behind the shared artifact framework.

**Goal:** Deliver genuinely indestructible Shadow Sword and Heavenly Partisan artifacts, full opposed action rosters, faction-correct guardians, menus, effects, and max-rank behavior.

## Task 1: Artifact rules and registered identity

**Tests:** add `ArtifactAlignmentTest`, `ArtifactAuthorizationRulesTest`, `ArtifactDurabilityTest` under `src/test/java/com/powers/item/artifact/`.

1. Prove dark/light authorization matrices, max-rank cooldown policy, energy still charged, and hard indestructibility.
2. Add `src/main/java/com/powers/item/artifact/ArtifactAlignment.java`, `ArtifactIdentity.java`, `ArtifactAuthorizationRules.java`, `ArtifactCooldownRules.java`.
3. Register codec-backed artifact identity component in `PowersItems` (or a dedicated `PowersDataComponents`).
4. Add shared `MythicArtifactItem`; migrate `ShadowSwordItem`; add `HeavenlyPartisanItem`.
5. Register Partisan specially in `PowersWeapons`, apply unbreakable/fire-resistant behavior to both, and block repair/grindstone/forge consumption paths.

## Task 2: Shared catalogue and routing

**Tests:** add `ArtifactActionCatalogueTest`, `ArtifactSelectionRulesTest`; migrate existing Shadow Sword catalogue tests.

1. Define `ArtifactActionDefinition` with ID, alignment, category, rank, energy, base cooldown, significance, and handler key.
2. Build a shared catalogue containing all registered routed innate/crystal actions and unique dark/light actions.
3. Reject duplicate/missing IDs and server-validate held item/alignment/rank/selection.
4. Adapt `ShadowSwordPowerManager/Runtime/Packets` into `ArtifactWeaponManager/Runtime/Packets` while keeping compatibility wrappers only where necessary.
5. Add corresponding player attachment state for Partisan selection.

## Task 3: Passive aura, energy, and unauthorised carrier rules

**Tests:** add `ArtifactInventoryAuraRulesTest` and GameTests for curse/judgement.

1. Implement one ten-tick inventory scan in the consolidated player coordinator.
2. Authorised dark: fast darkness regeneration; authorised light: fast normal regeneration; both emit one/two budgeted alignment particles.
3. Unauthorised Shadow Sword: hidden Blindness/Wither and rate-limited darkness guardians.
4. Unauthorised Partisan: hidden Glowing/radiant damage and rate-limited Radiant Sentinels.
5. Ensure effects stop reapplying after removal and all status effects use `PowerStatusEffects.hidden`.

## Task 4: Guardian faction abstraction and Radiant Sentinel

**Tests:** extend `PlayerLikeMobRulesTest`; add `GuardianFactionRulesTest`, resource tests, and GameTests.

1. Generalize owner/alignment/target predicates without breaking `DarknessCreature`.
2. Add `RadiantSentinel` entity, attributes, player-shaped renderer/skin resource, spawn egg, sounds, translations, loot, and registrations.
3. Give both factions bounded melee, lightning, fireball, and alignment-field tactical actions.
4. Enforce owner, four-normal/two-elite, server, lifetime, dimension, death, and logout caps.
5. Verify unauthorised-carrier guardians target the carrier but never valid owners/allies.

## Task 5: Darkness actions 8–11

**Tests:** one pure rules test and one GameTest per action.

1. Implement `BlackDecreeAbility`: mark lifecycle, percent health caps, LOS/ward/amethyst/caster-death cancellation.
2. Implement `EventHorizonAbility`: one-per-owner singularity, projectile consumption, bounded candidate query, pulses, cleanup.
3. Implement `DeathlessNightManager`: one stored death ward, five-minute expiry, legal lethal sources, health/energy restoration, exact once consumption.
4. Implement `LegionOfEclipseAbility`: cosmic telegraph, two elite guardians, temporary capped dominion.
5. Add dark palette FX/sounds/translations/action docs/interactions.

## Task 6: Full light action roster

**Tests:** add `LightArtifactActionRulesTest` plus focused tests/GameTests per stateful action.

1. Implement Call Radiant, Consecrate Ground, Dawnstride, Covenant Chain, Daybreak Wave, Heaven Gate, and Banish Darkness using safe shared primitives.
2. Implement Divine Decree, Solar Firmament, Second Dawn, and Host of Heaven with exact caps/counterplay from the design.
3. Use queued bounded block conversions and retain the existing opposed-block catastrophe.
4. Ensure normal rank only affects Partisan actions/routed player powers, never crystal actions.

## Task 7: Shared artifact screen

**Tests:** codec/selection tests and client layout/resource validators.

1. Add `ArtifactMenuPayload` and authenticated selection packet carrying stable action IDs.
2. Replace/extend `ShadowSwordScreen` with `ArtifactWeaponScreen`: categories, full descriptions, cost, gate, cooldown, icons, alignment skin, vanilla widgets, scaling/tooltips.
3. Add new icons only where needed; validate all PNG/model/translation references.
4. Right click casts; sneak-right-click opens the correct alignment menu.

## Task 8: Exhaustive artifact validation

1. Generate action definitions and all pairwise interactions for the expanded catalogue.
2. Test every action with wrong item/alignment/rank, insufficient energy, amethyst, ward, safe zone, cleanup, and concurrency cap.
3. Run full test/GameTest/client compile/resource validation and dedicated-server boot.
4. Update README artifact/guardian sections.
5. Commit: `feat: forge opposed mythic artifacts`.

