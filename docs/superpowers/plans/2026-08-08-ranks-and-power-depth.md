# Rank Perks and Power Depth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make all 56 rank nodes mechanically meaningful and deepen every innate, crystal, and spell ability through one capped scaling and feature system.

**Architecture:** Rank JSON declares typed perks; a pure aggregator creates a capped `RankProfile`; `PowerScalingService` translates the profile and action aspects into cast values and unlocked variants. Ability implementations consume scaled contexts and never query branch-string literals directly.

**Tech Stack:** Java 25, Gson resource loading, Fabric attachments, Minecraft attributes/effects, JUnit 6.

## Global Constraints

- Preserve all existing player attachment IDs and migrate existing rank-node sets without loss.
- Players may unlock multiple paths; focused nodes strengthen identity without permanently excluding another path.
- Do not add crystal/progression-artifact recipes.
- Attribute modifiers use stable POWERS identifiers and precise ownership cleanup.
- Every ability respects consent, protection, amethyst, interaction, and performance policy.
- Every commit remains buildable on the sole `main` branch.

---

### Task 1: Typed Rank Perks and Capped Aggregation

**Files:**
- Create: `src/main/java/com/powers/progression/RankPerkType.java`
- Create: `src/main/java/com/powers/progression/RankPerk.java`
- Create: `src/main/java/com/powers/progression/RankProfile.java`
- Create: `src/main/java/com/powers/progression/RankProfileService.java`
- Modify: `src/main/java/com/powers/progression/RankNode.java`
- Modify: `src/main/java/com/powers/progression/RankGraphRegistry.java`
- Modify: `src/main/resources/data/powers/ranks/light.json`
- Modify: `src/main/resources/data/powers/ranks/darkness.json`
- Test: `src/test/java/com/powers/progression/RankProfileServiceTest.java`

**Interfaces:**
- Produces: `RankProfileService.profile(RankGraph, RankProgress)` and immutable capped perk values.

- [x] **Step 1: Write failing coverage, focus, cap, and identity tests**

```java
@Test
void everyNodeHasAtLeastOneMechanicalPerk() {
    Stream.of(RankGraphRegistry.light(), RankGraphRegistry.darkness())
            .flatMap(graph -> graph.nodes().stream())
            .forEach(node -> assertFalse(node.perks().isEmpty(), node.id()));
}

@Test
void focusStrengthensPerksWithoutRemovingUnlockedPaths() {
    RankProfile profile = service.profile(graph, progress(Set.of("might_1", "motion_1"), "might_1"));
    assertTrue(profile.value(RankPerkType.POWER_DAMAGE) > profile.value(RankPerkType.MOVEMENT));
    assertTrue(profile.value(RankPerkType.MOVEMENT) > 0);
}
```

- [x] **Step 2: Run and verify missing typed perk model**

Run: `./gradlew test --tests com.powers.progression.RankProfileServiceTest`

Expected: compilation fails on `RankPerkType`.

- [x] **Step 3: Implement perk parsing and explicit caps**

```java
public record RankPerk(RankPerkType type, double amount, String actionOrAspect) {
    public RankPerk {
        Objects.requireNonNull(type);
        actionOrAspect = actionOrAspect == null ? "" : actionOrAspect;
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Non-finite rank perk");
    }
}
```

Cap total damage/healing/control at +40%, range/duration at +35%, energy at +50%, regeneration at +40%, cost/cooldown reduction at 25%, and resistance at 20%. Focus multiplies the focused node's numeric perks by 1.5 before caps.

- [x] **Step 4: Give all 56 nodes distinct perks and run tests**

Use Might/Motion/Insight/Wardcraft/Communion/Veil/Dominion identities from the design. Light favours stable protection/efficiency; darkness favours harm/drain/concealment with explicit backlash multipliers.

Run: `./gradlew test --tests 'com.powers.progression.*'`

Expected: every node parses, existing migration tests pass, and caps hold.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/powers/progression src/main/resources/data/powers/ranks src/test/java/com/powers/progression
git commit -m "feat: give every rank node a capped mechanical perk"
```

### Task 2: Unified Power Scaling and Player Attributes

**Files:**
- Create: `src/main/java/com/powers/progression/ScaledMagicValues.java`
- Create: `src/main/java/com/powers/progression/PowerScalingService.java`
- Create: `src/main/java/com/powers/progression/RankAttributeManager.java`
- Modify: `src/main/java/com/powers/player/SkillSystem.java`
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`
- Modify: `src/main/java/com/powers/power/Ability.java`
- Modify: `src/main/java/com/powers/power/PowerEnergy.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Test: `src/test/java/com/powers/progression/PowerScalingServiceTest.java`
- Test: `src/test/java/com/powers/progression/RankAttributeManagerTest.java`

**Interfaces:**
- Consumes: `MagicActionDefinition`, `RankProfile`, and existing numeric level.
- Produces: `scale(ServerPlayer, MagicActionDefinition)` and owned attribute reconciliation.

- [x] **Step 1: Write failing scale and cleanup tests**

```java
@Test
void motionFocusImprovesMovementActionButNotUnrelatedHealing() {
    ScaledMagicValues step = service.scale(motionPlayer, action("shadow_step"));
    ScaledMagicValues healing = service.scale(motionPlayer, action("plant_healing_acceleration"));
    double stepRatio = step.range() / action("shadow_step").baseRange();
    double healingRatio = healing.range() / action("plant_healing_acceleration").baseRange();
    assertTrue(stepRatio > healingRatio);
}

@Test
void reconciliationRemovesOnlyPowersOwnedModifiers() {
    manager.reconcile(player, profile);
    manager.clear(player);
    assertFalse(player.attributes().containsModifier(POWERS_RANK_HEALTH));
    assertTrue(player.attributes().containsModifier(foreignModifier));
}
```

- [x] **Step 2: Run and verify missing scaler**

Run: `./gradlew test --tests 'com.powers.progression.PowerScalingServiceTest' --tests 'com.powers.progression.RankAttributeManagerTest'`

Expected: compilation fails on `ScaledMagicValues`.

- [x] **Step 3: Implement one scaling formula and stable modifier ownership**

```java
public record ScaledMagicValues(int potency, double range, int durationTicks,
        int energyCost, int cooldownTicks, int interactionPriority,
        Set<String> unlockedVariants, double backlashMultiplier) {}
```

Combine the legacy depth baseline with branch perks once. Clamp every output to its documented cap and minimum safe value. Register stable modifier IDs under `powers:rank_*`; reconcile only on rank/focus/tag changes.

- [x] **Step 4: Replace `SkillSystem.damage/range` and energy-capacity ad hoc calls**

Keep compatibility methods temporarily delegating to `PowerScalingService`, migrate call sites, then remove only when `rg 'SkillSystem\.(damage|range)' src` returns no production use.

- [x] **Step 5: Run progression, energy, HUD-state, and full tests**

Run: `./gradlew test --tests 'com.powers.progression.*' --tests 'com.powers.power.*' --tests 'com.powers.hud.*' && ./gradlew test`

Expected: all pass; foreign attributes/effects remain untouched.

- [x] **Step 6: Commit**

```bash
git add src/main/java/com/powers/progression src/main/java/com/powers/player src/main/java/com/powers/power src/main/java/com/powers/network/PowersPackets.java src/test/java/com/powers/progression
git commit -m "feat: scale magic and player traits through rank profiles"
```

### Task 3: Movement, Time, and Mind Power Depth

**Files:**
- Modify: `SlowWorldAbility.java`, `TeleportAbility.java`, `ShadowStepAbility.java`, `FlightAbility.java`, `SpeedBurstAbility.java`, `SuperSpeedAbility.java`, `TimeFreezeToggleAbility.java`, `VesselPossessionAbility.java`, `AstralProjectionAbility.java` under `src/main/java/com/powers/power/abilities/`
- Modify: `src/main/java/com/powers/power/state/EntityFreezeController.java`
- Modify: `src/main/java/com/powers/mind/BodyProxyManager.java`
- Create: `src/test/java/com/powers/power/PowerImprovementCoverageTest.java`

**Interfaces:**
- Consumes: scaled values, variants, runtime presence, effect cue service.
- Produces: nine improved abilities with safe lifecycle and interaction registration.

- [x] **Step 1: Write failing per-power improvement coverage**

```java
@ParameterizedTest
@ValueSource(strings = {"slow_world","time_shift","shadow_step","flight","speed_burst",
        "super_speed","time_freeze","vessel_possession","astral_projection"})
void movementTimeAndMindActionsDeclareCounterplayScalingAndSignature(String id) {
    assertCompleteImprovement(id);
}
```

- [x] **Step 2: Run and record missing improvement declarations**

Run: `./gradlew test --tests com.powers.power.PowerImprovementCoverageTest`

Expected: failure for every action not yet migrated.

- [x] **Step 3: Implement the nine design-listed variants**

Use `ScaledMagicValues` for numbers, `MagicRuntime` for bubble/tether/residue presence, full collision validation for movement, owner tokens for flags/game modes, and bounded momentum storage. Flight/toggle cleanup must restore only POWERS-owned state.

- [x] **Step 4: Add focused lifecycle and interaction tests**

Test owner-overlapping time effects, anchor-blocked marking, suppression return, body damage return, second-step rank gate, acceleration caps, and disconnect cleanup.

- [x] **Step 5: Run tests and commit**

Run: `./gradlew test --tests 'com.powers.power.*' --tests 'com.powers.mind.*' --tests 'com.powers.magic.*'`

```bash
git add src/main/java/com/powers/power/abilities src/main/java/com/powers/power/state src/main/java/com/powers/mind src/test/java/com/powers/power
git commit -m "feat: deepen movement time and mind powers"
```

### Task 4: Offensive Elemental and Force Power Depth

**Files:**
- Modify the following under `src/main/java/com/powers/power/abilities/`: `ElementalBlastAbility.java`, `StarfallAbility.java`, `VoidBeamAbility.java`, `FireballAbility.java`, `FrostNovaAbility.java`, `LightningStrikeAbility.java`, `GroundSlamAbility.java`, `TelekinesisAbility.java`, `EnergyBeamAbility.java`, `BreezyBashAbility.java`, `GravityDisplacementAbility.java`, `EnergyDrainAbility.java`, `IceManipulationAbility.java`
- Modify: `src/main/java/com/powers/mixin/LargeFireballMixin.java`
- Test: `src/test/java/com/powers/power/ExceptionalReactionTest.java`

**Interfaces:**
- Consumes: action descriptors, scaler, resolver, runtime, protection policy.
- Produces: thirteen improved abilities and actual exceptional collision mechanics.

- [x] **Step 1: Write failing reaction-mechanics tests**

```java
@Test void frostTurnsFireResidueIntoSteamAndExtinguishesOwnedFire() { assertSteamReaction(); }
@Test void wardReflectionChangesOwnerOnceAndStopsAtReflectionCap() { assertFiniteReflection(); }
@Test void anchorTurnsGravityMovementIntoBoundedStagger() { assertAnchorStagger(); }
@Test void lightningConductsThroughWetTargetsWithFiniteChainCount() { assertFiniteConduction(); }
```

- [x] **Step 2: Run and verify mechanics are not yet connected**

Run: `./gradlew test --tests com.powers.power.ExceptionalReactionTest`

Expected: assertions fail on current one-off behaviours.

- [x] **Step 3: Implement each design-listed improvement and interaction hook**

Every projectile carries owner/action/reflection-count metadata; beams consume ward integrity; frost applies POWERS-owned brittle residue; telekinesis intercepts eligible projectiles; gravity uses capped velocity; drain breaks on line-of-sight/distance; ice constructs expire without drops or duplication.

- [x] **Step 4: Prove terrain, consent, protection, and budgets remain enforced**

Run: `./gradlew test --tests 'com.powers.power.*' --tests 'com.powers.protection.*' --tests 'com.powers.fx.*'`

Expected: all reaction and safety tests pass.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/powers/power/abilities src/main/java/com/powers/mixin/LargeFireballMixin.java src/test/java/com/powers/power
git commit -m "feat: deepen offensive elemental and force powers"
```

### Task 5: Defensive, Support, and Stealth Power Depth

**Files:**
- Modify under `src/main/java/com/powers/power/abilities/`: `ForcefieldAbility.java`, `CozyCampfireAbility.java`, `InvisibilityToggleAbility.java`, `PlantHealingAbility.java`, `DoubleHealthAbility.java`
- Create: `src/main/java/com/powers/power/state/MagicShieldManager.java`
- Test: `src/test/java/com/powers/power/state/MagicShieldManagerTest.java`

**Interfaces:**
- Produces: finite shield integrity and five improved support/stealth abilities.

- [x] **Step 1: Write failing finite-shield and owned-modifier tests**

```java
@Test void shieldConsumesIntegrityAndCollapsesWithoutResistanceFive() { assertFiniteShield(); }
@Test void doubleHealthExpirationPreservesHealthRatioAndForeignModifiers() { assertOwnedHealth(); }
@Test void attackingBreaksPowerInvisibilityAndLeavesDetectableResidue() { assertStealthCounterplay(); }
```

- [x] **Step 2: Run and verify current potion-based behaviour fails the tests**

Run: `./gradlew test --tests 'com.powers.power.state.MagicShieldManagerTest'`

Expected: compilation fails because shield integrity does not exist.

- [x] **Step 3: Implement shield state and support improvements**

Track shield owner, integrity, expiry, reflection count, and fracture stage. Cozy Campfire registers a sanctuary presence; Plant Healing checks natural/growable blocks and never duplicates drops; Double Health owns one attribute modifier and clamps expiration; invisibility uses owned state and residue.

- [x] **Step 4: Run support, effect ownership, and full tests**

Run: `./gradlew test --tests 'com.powers.power.*' --tests 'com.powers.player.*' && ./gradlew test`

Expected: all pass without maximum Resistance or foreign-effect removal.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/powers/power/abilities src/main/java/com/powers/power/state src/test/java/com/powers/power
git commit -m "feat: deepen defensive support and stealth powers"
```

### Task 6: Crystal and Spell Depth

**Files:**
- Modify: all Java files under `src/main/java/com/powers/power/crystals/`
- Modify: all Java files under `src/main/java/com/powers/spell/`
- Test: `src/test/java/com/powers/power/crystals/CrystalImprovementCoverageTest.java`
- Test: `src/test/java/com/powers/spell/SpellImprovementCoverageTest.java`

**Interfaces:**
- Consumes: the same descriptor/scaling/runtime/interaction APIs as innate powers.
- Produces: complete coverage for 13 crystal actions and 20 spells.

- [x] **Step 1: Write failing origin-wide coverage tests**

```java
@Test void everyCrystalActionUsesScalingPresenceCounterplayAndCue() { assertOriginComplete(MagicOrigin.CRYSTAL, 13); }
@Test void everySpellUsesScalingPresenceCounterplayAndCue() { assertOriginComplete(MagicOrigin.SPELL, 20); }
```

- [x] **Step 2: Run and record every unmigrated action**

Run: `./gradlew test --tests 'com.powers.power.crystals.*' --tests 'com.powers.spell.*'`

Expected: new coverage assertions list unmigrated action IDs.

- [x] **Step 3: Migrate crystals and spells**

Scale exact effects, fields, channels, cooldowns, costs, and interaction priority. Mode selection remains free; underlying cooldowns remain swap-proof; suppression/anchor checks run before commit; temporary entities remain ephemeral; channel interruption refunds exactly the documented amount.

- [x] **Step 4: Add exceptional family tests and run all tests**

Cover light/dark contest, crystal/amethyst tiers, soul/purification, creation/banishment, travel/anchor, healing/hex, ritual amplification caps, and counterspell ownership.

Run: `./gradlew test`

Expected: every one of 60 castable actions plus three amethyst actions satisfies catalogue coverage.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/powers/power/crystals src/main/java/com/powers/spell src/test/java/com/powers/power/crystals src/test/java/com/powers/spell
git commit -m "feat: deepen every crystal and grimoire action"
```
