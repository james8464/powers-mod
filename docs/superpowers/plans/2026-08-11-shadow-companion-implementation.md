# Shadow Companion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove routine mindscape gamemode coercion and deliver a persistent, conversational, task-capable, magically equivalent Shadow companion with all 23 innate and three Shadow Sword powers, bounded adaptive combat tactics, and negligible client/server overhead.

**Architecture:** A real `ShadowCompanionEntity` owns movement, damage, skin identity, and world position; owner attachments retain memories and learned state across manifestation death. Deterministic parsers, policies, utility scoring, and a bounded contextual learner produce typed tasks/actions that a server-only controller validates. Existing client apparition packets render the same hidden server entity privately, while revealed state uses normal entity tracking and a player-skin renderer.

**Tech Stack:** Java 25, Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API/Loom, Mojang mappings, JUnit 6, Fabric GameTest, codecs/data attachments, vanilla goals/navigation, existing POWERS magic/runtime/FX systems.

## Global Constraints

- Work only in the existing linked worktree `/Users/james/Developer/Minecraft mods/POWERS/.worktrees/powers-finalisation` on `codex/powers-finalisation`.
- Use `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home` for Gradle.
- Follow RED → observed failure → GREEN → affected suite → commit for every task.
- Ordinary Light/Dark mindscape travel never changes gamemode; emergency confinement remains separate.
- Shadow uses the owner's skin and a player model but owns no inventory, equipment, armour, or fake-player connection.
- Exactly 23 canonical innate actions plus `call_hollowed`, `blight_ground`, and `nightfall_dominion` are executable; no crystal action is executable.
- Dark Crystal item manifestation is the only crystal exception and requires a full 1,850-energy, 1,200-tick interruptible rite.
- Adaptive learning changes only legal action preference within ±25%; exploration is capped at 5% and disabled in unsafe/high-load contexts.
- No routine operation scales with all world entities/chunks, all players, all memories, or an unbounded registry.
- All status effects hide vanilla particles and use POWERS semantic FX.
- Intentionally deferred crystal/custom-item recipes remain absent.

---

### Task 1: Retire Mindscape Gamemode Coercion Safely

**Files:**
- Create: `src/main/java/com/powers/realm/LegacyRealmGamemodeRules.java`
- Create: `src/test/java/com/powers/realm/LegacyRealmGamemodeRulesTest.java`
- Modify: `src/main/java/com/powers/PlayerPowerTicker.java`
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`
- Modify: `src/main/java/com/powers/PowersServerLifecycle.java`
- Modify: `src/gametest/java/com/powers/gametest/PowersGameTests.java`

**Interfaces:**
- Produces: `LegacyRealmGamemodeRules.Decision decide(String storedMode, GameType current)`.
- Produces: `PlayerPowersData.legacyPreviousGameModeName()` and `clearLegacyPreviousGameMode()`; the old writer is removed.

- [ ] **Step 1: Write the failing pure rules test**

```java
@Test
void legacyAdventureSnapshotRestoresOnceWithoutOverridingLegitimateModes() {
    assertEquals(GameType.SURVIVAL,
        LegacyRealmGamemodeRules.decide("survival", GameType.ADVENTURE).restore());
    assertNull(LegacyRealmGamemodeRules.decide("creative", GameType.CREATIVE).restore());
    assertTrue(LegacyRealmGamemodeRules.decide("broken", GameType.ADVENTURE).clearSnapshot());
}
```

- [ ] **Step 2: Run `./gradlew test --tests com.powers.realm.LegacyRealmGamemodeRulesTest --no-daemon` and confirm compilation/test failure because the rules type is absent.**

- [ ] **Step 3: Implement the pure migration decision**

```java
public record Decision(GameType restore, boolean clearSnapshot) {}

public static Decision decide(String storedMode, GameType current) {
    GameType stored = parse(storedMode);
    return new Decision(current == GameType.ADVENTURE ? stored : null,
        storedMode != null && !storedMode.isBlank());
}
```

- [ ] **Step 4: Replace `enforceRealmGamemode` with one-time `migrateLegacyRealmGamemode`, invoke it at join/tick, remove all routine `setGameMode` and previous-mode writes, and retain only read/clear compatibility accessors.**

- [ ] **Step 5: Add GameTests that carry Survival, Creative, Adventure, and Spectator through Light/Dark entry/return unchanged and restore one synthetic legacy Adventure snapshot exactly once.**

- [ ] **Step 6: Run the focused unit test, `./gradlew compileJava compileClientJava`, and the relevant GameTest class.**

- [ ] **Step 7: Commit with `fix: preserve gamemode through mindscapes`.**

### Task 2: Add Persistent Shadow Data and the Real Entity

**Files:**
- Create: `src/main/java/com/powers/companion/ShadowStance.java`
- Create: `src/main/java/com/powers/companion/ShadowCompanionData.java`
- Create: `src/main/java/com/powers/companion/ShadowCompanionStore.java`
- Create: `src/main/java/com/powers/companion/ShadowCompanionRules.java`
- Create: `src/main/java/com/powers/companion/ShadowCompanionEntity.java`
- Create: `src/test/java/com/powers/companion/ShadowCompanionDataTest.java`
- Create: `src/test/java/com/powers/companion/ShadowCompanionRulesTest.java`
- Modify: `src/main/java/com/powers/PowersEntities.java`
- Modify: `src/main/java/com/powers/PowersBootstrap.java`
- Create: `src/client/java/com/powers/client/ShadowCompanionRenderer.java`
- Modify: `src/client/java/com/powers/client/PowersClient.java`

**Interfaces:**
- Produces: `ShadowCompanionData DEFAULT`, `withEnergy`, `withStance`, `withRevealed`, `withBodyId`, and a bounded `CODEC`.
- Produces: `ShadowCompanionStore.get(ServerPlayer)`, `set`, `update`, `clearBody`, and `initialize`.
- Produces: `ShadowCompanionEntity.configure(ServerPlayer, ShadowCompanionData)`, `ownerId()`, `ownerProfile()`, `revealed()`, `setRevealed(boolean)`, `energy()`, `setEnergy(int)`.

- [ ] **Step 1: Write failing data tests for the 1,850 clamp, default `FOLLOW`, reveal/body identity round trip, and codec rejection/sanitization of oversized collections.**

```java
@Test
void persistentStateClampsEnergyAndHasNoRuntimeReferences() {
    var data = ShadowCompanionData.defaults().withEnergy(9_999);
    assertEquals(1_850, data.energy());
    assertEquals(ShadowStance.FOLLOW, data.stance());
    assertTrue(ShadowCompanionData.CODEC != null);
}
```

- [ ] **Step 2: Run the focused test and observe failure because the data/store types do not exist.**

- [ ] **Step 3: Implement immutable data and a persistent copy-on-death player attachment; initialize it before entity/world use.**

- [ ] **Step 4: Write the failing entity-rule test for follow distance, hidden collision/damage policy, safe teleport threshold, and death recall energy.**

- [ ] **Step 5: Register `powers:shadow_companion` as `MobCategory.CREATURE`, attributes (100 health, 12 armour, 16 attack, 0.32 movement, 48 follow range), and a no-equipment `ShadowCompanionEntity` with synchronized owner profile/reveal state and owner/body NBT.**

- [ ] **Step 6: Implement only cheap goals: float, owner-follow controller, look, and immediate hazard avoidance. Leave high-level combat to Task 8.**

- [ ] **Step 7: Implement `ShadowCompanionRenderer` from the proven `EchoCloneRenderer` wide/slim player-model pattern; never add an item-in-hand/armour layer. Register it client-side.**

- [ ] **Step 8: Run focused tests, client/common compilation, and the entity resource audit.**

- [ ] **Step 9: Commit with `feat: add persistent player-like Shadow entity`.**

### Task 3: Replace Mannequin Sessions with One Authoritative Body

**Files:**
- Create: `src/main/java/com/powers/companion/ShadowManifestationRules.java`
- Create: `src/test/java/com/powers/companion/ShadowManifestationRulesTest.java`
- Modify: `src/main/java/com/powers/companion/PrivateCompanionManager.java`
- Modify: `src/main/java/com/powers/companion/PrivateCompanionRules.java`
- Modify: `src/main/java/com/powers/network/CompanionPackets.java`
- Modify: `src/client/java/com/powers/client/PrivateCompanionClient.java`
- Modify: `src/client/java/com/powers/client/ShadowRemotePlayer.java`
- Modify: `src/main/java/com/powers/PowerCombatEvents.java`
- Modify: `src/main/java/com/powers/PowersServerLifecycle.java`
- Modify: `src/gametest/java/com/powers/gametest/PowersGameTests.java`

**Interfaces:**
- Produces: `PrivateCompanionManager.body(UUID)`, `manifest`, `dismiss`, `setRevealed`, `reconcileLoadedBody`, `afterDeath`, `diagnostics`.
- Preserves: `handleChat`, `interact`, `activeSessionCount`, `activeRevealedBodyCount`, `revealedBodyId`, and visibility packet compatibility.

- [ ] **Step 1: Write failing rules tests proving hidden/revealed are the same body UUID, hidden is private/collisionless, reveal does not heal/cleanse, duplicate bodies reconcile, and source loss releases every runtime handle.**

- [ ] **Step 2: Run the focused tests and observe the expected missing-policy failures.**

- [ ] **Step 3: Replace `Session.body Mannequin` and `BODY_OWNERS` with the real entity and owner/body indexes. Manifest once, change the entity's reveal state in place, and send apparition state only to the owner while hidden.**

- [ ] **Step 4: Make hidden bodies invisible, invulnerable to external targeting, non-pushable, and collisionless while continuing environmental energy/suppression state; revealing restores ordinary mortality without restoring health/effects.**

- [ ] **Step 5: Reconcile entity load/unload, owner join/logout/death, source sword/alignment loss, dimension failure, server stop, and duplicate/orphan bodies idempotently. Death persists memory but clears body/task/toggles and sets recall energy to 25%.**

- [ ] **Step 6: Update the live Shadow GameTest to assert `ShadowCompanionEntity` instead of `Mannequin`, same UUID across hide/reveal, owner skin profile, empty hands/armour, mortality only while revealed, and remembered recall.**

- [ ] **Step 7: Run focused tests, compilation, and GameTests; commit with `feat: make Shadow a single authoritative companion`.**

### Task 4: Structured Conversation, Memory, and Typed Tasks

**Files:**
- Replace: `src/main/java/com/powers/companion/ShadowChatIntent.java`
- Create: `src/main/java/com/powers/companion/ShadowRequest.java`
- Create: `src/main/java/com/powers/companion/ShadowRequestParser.java`
- Create: `src/main/java/com/powers/companion/ShadowConversationMemory.java`
- Create: `src/main/java/com/powers/companion/ShadowTask.java`
- Create: `src/main/java/com/powers/companion/ShadowTaskController.java`
- Create: `src/main/java/com/powers/companion/ShadowDialogueEngine.java`
- Modify: `src/main/java/com/powers/companion/LoreDialogueEngine.java`
- Modify: `src/main/java/com/powers/companion/PrivateCompanionManager.java`
- Modify: `src/main/java/com/powers/knowledge/KnowledgeService.java`
- Modify: `src/main/java/com/powers/companion/BoundedDialogueProvider.java`
- Create: `src/test/java/com/powers/companion/ShadowRequestParserTest.java`
- Create: `src/test/java/com/powers/companion/ShadowConversationMemoryTest.java`
- Create: `src/test/java/com/powers/companion/ShadowTaskControllerTest.java`
- Create: `src/test/java/com/powers/companion/ShadowDialogueEngineTest.java`

**Interfaces:**
- Produces: `ShadowRequestParser.parse(String, ShadowConversationMemory, ShadowNameResolver)`.
- Produces: `ShadowRequest.Kind` covering summon/dismiss/reveal/hide/follow/stay/guard/stop/attack/defend/use/stop-power/get/conjure/scout/diagnose/converse.
- Produces: `ShadowTaskController.submit`, `tick`, `cancel`, `active`, and typed `ShadowTask.Result`.

- [ ] **Step 1: Write failing parser tests for every intent family, polite/case variants, counts, translated/registry power names, ambiguity, and recent pronoun resolution.**

```java
assertEquals(ShadowRequest.Kind.GET_ITEM,
    parser.parse("shadow, please bring me 16 minecraft:torch", memory, names).kind());
assertEquals(ShadowRequest.Kind.RANGE_PREFERENCE,
    parser.parse("shadow, fight that boss from farther away", memory, names).kind());
```

- [ ] **Step 2: Observe the focused RED, then implement the side-effect-free bounded parser and registry/name resolver.**

- [ ] **Step 3: Write failing memory tests for 24-turn cap, compact referents, summary bounds, private-data redaction, relationship/influence bounds, persistence, and death survival.**

- [ ] **Step 4: Implement immutable bounded memory and persist it inside `ShadowCompanionData`.**

- [ ] **Step 5: Write failing task tests for one foreground task, stop/cancel, exact failure reason, timeout, reservation release, and save-safe summary without entity references.**

- [ ] **Step 6: Implement task controller and deterministic dialogue: exact mechanics stay truthful; Darkness/dependence bias affects advice only; initiative is event-driven with a 3-minute minimum.**

- [ ] **Step 7: Restrict optional remote dialogue to redacted prose input/output; action plans are always local and cannot be returned by the provider. Preserve hidden owner-only and revealed global speech.**

- [ ] **Step 8: Run focused suites and existing knowledge/attempt-journal tests; commit with `feat: add contextual Shadow conversation and tasks`.**

### Task 5: Retrieval and Darkness Conjuration

**Files:**
- Create: `src/main/java/com/powers/companion/ShadowConjurationTier.java`
- Create: `src/main/java/com/powers/companion/ShadowConjurationFacts.java`
- Create: `src/main/java/com/powers/companion/ShadowConjurationRules.java`
- Create: `src/main/java/com/powers/companion/ShadowConjurationManager.java`
- Create: `src/main/java/com/powers/companion/ShadowItemRetrieval.java`
- Create: `src/test/java/com/powers/companion/ShadowConjurationRulesTest.java`
- Create: `src/test/java/com/powers/companion/ShadowConjurationManagerTest.java`
- Create: `src/test/java/com/powers/companion/ShadowItemRetrievalTest.java`
- Create: `src/main/resources/data/powers/tags/item/shadow_conjuration_forbidden.json`
- Create: `src/main/resources/data/powers/tags/item/shadow_conjuration_uncommon.json`
- Create: `src/main/resources/data/powers/tags/item/shadow_conjuration_rare.json`
- Create: `src/main/resources/data/powers/tags/item/shadow_conjuration_mythic.json`
- Create: `src/main/resources/data/powers/tags/item/shadow_conjuration_allowed_external.json`
- Modify: `src/main/resources/assets/powers/lang/en_us.json`

**Interfaces:**
- Produces: `ShadowConjurationRules.evaluate(ShadowConjurationFacts)` returning `Decision(allowed, tier, boundedCount, cost, reason)`.
- Produces: `ShadowConjurationManager.begin`, `tick`, `interrupt`, and atomic `Reservation`.
- Produces: `ShadowItemRetrieval.find(ServerLevel, Vec3, Item, int, UUID)` capped to 32 blocks/64 candidates/200 ticks.

- [ ] **Step 1: Write failing policy tests for plain-stack sanitization, one-stack cap, 4/12/40/250 energy tiers, third-party opt-in, all crystals denied except Dark Crystal rite, artifacts/admin/spawn eggs denied, and testing bypass preserving policy.**

- [ ] **Step 2: Observe RED, implement tag keys and pure cost/count/policy logic, and populate explicit forbidden/tier tags without adding recipes.**

- [ ] **Step 3: Write failing runtime tests for reservation/refund, inventory insertion/remainder drop, nearby legitimate item first, owned-drop rejection, bounded scan, full-energy Dark Crystal precondition, 1,200-tick channel, duplicate prevention, and every interruption.**

- [ ] **Step 4: Implement retrieval with `BoundedEntityCandidates`/chunk-local queries and direct owner delivery; Shadow never owns an item inventory.**

- [ ] **Step 5: Implement ordinary manifestation and the Dark Crystal rite with escalating semantic FX, one active rite per owner, atomic completion, and no other task/cast during the rite.**

- [ ] **Step 6: Run focused tests and `validatePowerResources`; commit with `feat: let Shadow retrieve and conjure bounded items`.**

### Task 6: Player-Like Magic Participation and Darkness Energy

**Files:**
- Create: `src/main/java/com/powers/magic/participant/MagicParticipant.java`
- Create: `src/main/java/com/powers/magic/participant/MagicParticipants.java`
- Create: `src/main/java/com/powers/magic/participant/MagicConsentAuthority.java`
- Create: `src/main/java/com/powers/companion/ShadowMagicState.java`
- Create: `src/main/java/com/powers/companion/ShadowEnergyRules.java`
- Create: `src/test/java/com/powers/magic/participant/MagicParticipantsTest.java`
- Create: `src/test/java/com/powers/companion/ShadowEnergyRulesTest.java`
- Modify: `src/main/java/com/powers/entity/PlayerLikeTarget.java`
- Modify: `src/main/java/com/powers/entity/PowerTestActor.java`
- Modify: `src/main/java/com/powers/protection/PowerProtection.java`
- Modify: `src/main/java/com/powers/power/AmethystDampening.java`
- Modify: `src/main/java/com/powers/spell/SpellFieldManager.java`
- Modify: `src/main/java/com/powers/PowerCombatEvents.java`
- Modify: `src/main/java/com/powers/power/state/MagicShieldManager.java`

**Interfaces:**
- Produces: `MagicParticipants.resolve(LivingEntity)` with identity, alignment, energy access, owner consent authority, anchor/suppression state, and cleanup hooks.
- Produces: `ShadowEnergyRules.tick(EnergyFacts)` returning bounded refill/drain/action suppression.
- Changes: test actor keeps explicit `ALWAYS_ALLOW_TESTS`; Shadow delegates consent to owner and never auto-consents.

- [ ] **Step 1: Write failing tests proving ServerPlayer, test actor, and Shadow resolve to distinct participant policies and that Shadow never inherits test auto-consent.**

- [ ] **Step 2: Observe RED, implement participant adapters without modifying vanilla `ServerPlayer`, and route central consent/protection through them.**

- [ ] **Step 3: Write failing energy tests for 1,850 cap, 900/sec linked refill, per-tick clamp, Darkness boost, Pure Light suppression/harm, amethyst drain/action lock, energy transfer, and testing-mode behavior.**

- [ ] **Step 4: Implement `ShadowMagicState` over persistent data and pulse energy at 5/20-tick cadence, never every subsystem tick.**

- [ ] **Step 5: Generalize amethyst detection/effect, forcefield/shield, fields, anchors, drain/link, purification/dispel, Time Freeze participation, and lifecycle cleanup to `MagicParticipant` while preserving player behavior.**

- [ ] **Step 6: Run focused tests plus all protection, amethyst, shield, forcefield, spell-field, lifecycle, and interaction suites; commit with `feat: make Shadow a full magic participant`.**

### Task 7: Exact Shadow Power Manifest and Real Executors

**Files:**
- Create: `src/main/java/com/powers/companion/combat/ShadowPowerAction.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowPowerCatalogue.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowPowerExecutor.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowPowerRuntime.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowPowerFx.java`
- Create: `src/test/java/com/powers/companion/combat/ShadowPowerCatalogueTest.java`
- Create: `src/test/java/com/powers/companion/combat/ShadowPowerExecutorTest.java`
- Modify: `src/main/java/com/powers/boss/FirstVesselCombat.java`
- Modify: `src/main/java/com/powers/power/state/GlobalTimeStopManager.java`
- Modify: `src/main/java/com/powers/power/artifact/ArtifactGuardianSummons.java`
- Modify: `src/main/java/com/powers/power/abilities/CombatTerrainImpact.java`

**Interfaces:**
- Produces: `ShadowPowerCatalogue.actions()` derived from canonical `PowerRegistry` plus three uniques, and `requireComplete()`.
- Produces: `ShadowPowerExecutor.execute(ServerLevel, ShadowCompanionEntity, LivingEntity, ShadowPowerAction, ExecutionContext)` returning typed `ExecutionResult` and `MagicPresenceHandle` ownership.
- Produces: `ShadowPowerRuntime.tickToggles`, `stop`, `clearOwner`, and per-action workload reservations.

- [ ] **Step 1: Write the failing manifest test**

```java
@Test
void manifestMatchesEveryInnateAndExactlyThreeSwordUniquesWithoutCrystals() {
    var ids = ShadowPowerCatalogue.actions().stream().map(ShadowPowerAction::id).toList();
    assertEquals(PowerRegistry.getAll().stream().map(p -> p.id().getPath()).toList(),
        ids.subList(0, 23));
    assertEquals(List.of("call_hollowed", "blight_ground", "nightfall_dominion"),
        ids.subList(23, 26));
    assertTrue(ids.stream().noneMatch(CrystalAbilityCatalog.defaults().values()
        .stream().flatMap(List::stream).collect(toSet())::contains));
}
```

- [ ] **Step 2: Observe RED and implement immutable metadata for range mode, cost, intent, destructive class, toggle, and server-work class.**

- [ ] **Step 3: Write failing executor-family tests for mobility, projectile, beam, area, control, defense, recovery, toggle, possession/projection, summon, terrain spread, and apotheosis; assert every catalogue entry has a non-fallback handler.**

- [ ] **Step 4: Extract reusable entity-safe helpers from `FirstVesselCombat`; implement a named switch/executor for all 26 actions with max-Darkness strength, central protection, terrain budgets, actual lightning/projectiles/beams, hidden potion particles, and corrupted semantic FX.**

- [ ] **Step 5: Generalize Time Freeze ownership so a Shadow cast lets the owner act, freezes Shadow's body as the source manifestation, drains Shadow energy in the server-end manager, and releases on suppression/source loss/exhaustion/external clock mutation.**

- [ ] **Step 6: Implement companion-specific possession/projection return semantics, physical flight/navigation, forcefield sacrifice, toggle cleanup, and zero cooldown with action/work cadence instead.**

- [ ] **Step 7: Run focused executor tests and all power/interaction suites; commit with `feat: give Shadow the complete non-crystal arsenal`.**

### Task 8: Tactical Range Intelligence and Bounded Learning

**Files:**
- Create: `src/main/java/com/powers/companion/combat/ShadowEngagementMode.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowTargetArchetype.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowCombatFacts.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowCombatContext.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowTacticalPlanner.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowLearningState.java`
- Create: `src/main/java/com/powers/companion/combat/BoundedCombatLearner.java`
- Create: `src/main/java/com/powers/companion/combat/ShadowCombatController.java`
- Create: `src/test/java/com/powers/companion/combat/ShadowTacticalPlannerTest.java`
- Create: `src/test/java/com/powers/companion/combat/BoundedCombatLearnerTest.java`
- Create: `src/test/java/com/powers/companion/combat/ShadowCombatControllerTest.java`

**Interfaces:**
- Produces: `ShadowTacticalPlanner.choose(List<ShadowPowerAction>, ShadowCombatFacts, ShadowLearningState)` returning mode/action/movement/score/evaluated count.
- Produces: `BoundedCombatLearner.adjust`, `openCredit`, `completeCredit`, `reset`, `encode`, and `decode`.
- Produces: `ShadowCombatController.tick(ServerLevel, ShadowCompanionEntity, ServerPlayer, int)` at a staggered 10-tick cadence.

- [ ] **Step 1: Write failing planner tests showing CLOSE against fragile/ranged targets, FAR against dangerous melee/area bosses, SKIRMISH for mixed threats, RESCUE for owner danger, RECOVER for suppression/low energy, ally-safe firing lanes, and explicit legal order priority.**

- [ ] **Step 2: Observe RED and implement pure mode/action/movement scoring over at most 26 already-legal actions and capped target/projectile facts.**

- [ ] **Step 3: Write failing learner tests for reward `[-1,1]`, learned modifier ±25%, exploration ≤5%, unsafe exploration shutdown, 64 context/32 type LRU caps, saturated counts, exponential decay, deterministic tie-breaking, versioned persistence, and reset.**

- [ ] **Step 4: Implement the owner-local contextual learner with O(1) credit completion and no allocation inside the ordinary planner loop.**

- [ ] **Step 5: Write failing controller tests for five-second attribution, target-type adaptation, spoken range preference, no player-UUID persistence, action cadence, candidate caps, and server-budget refusal.**

- [ ] **Step 6: Integrate movement orbit/close/retreat/rescue/recover goals, short velocity intercepts, owner/ally firing-lane checks, and executor invocation. Persist learning through `ShadowCompanionData`.**

- [ ] **Step 7: Run focused tests and deterministic 10/50/100 companion planner soak; commit with `feat: add adaptive Shadow combat tactics`.**

### Task 9: Runtime Integration, Commands, and Diagnostics

**Files:**
- Modify: `src/main/java/com/powers/companion/PrivateCompanionManager.java`
- Modify: `src/main/java/com/powers/PlayerPowerTicker.java`
- Modify: `src/main/java/com/powers/PowersServerLifecycle.java`
- Modify: `src/main/java/com/powers/PowerCombatEvents.java`
- Modify: `src/main/java/com/powers/command/PowerCommand.java`
- Modify: `src/main/java/com/powers/command/PowerDiagnosticsCommand.java`
- Create: `src/main/java/com/powers/companion/ShadowDiagnostics.java`
- Create: `src/test/java/com/powers/companion/ShadowRuntimeLifecycleTest.java`
- Create: `src/test/java/com/powers/companion/ShadowDiagnosticsTest.java`
- Modify: `src/test/java/com/powers/diagnostics/RuntimeDiagnosticSnapshotTest.java`

**Interfaces:**
- Produces: `/powers shadow learning reset <player>` as an administrator-only reset.
- Produces: `ShadowDiagnostics` counts for bodies, visibility, tasks, energy, casts, conjures, candidates, contexts, profiles, credit windows, forced chunks, packets, particles, and leaks.

- [ ] **Step 1: Write failing lifecycle tests for owner/source death/loss/logout, entity death, task/toggle cleanup, possession/projection failure, dimension failure, server stop, and idempotent repeated cleanup.**

- [ ] **Step 2: Observe RED and integrate manager/task/combat/energy/conjuration ticks in stable order with staggered cadences; ensure Time Freeze reconciliation runs in the frozen server-end path.**

- [ ] **Step 3: Write failing diagnostics/reset tests, then add bounded non-private counters and the administrator reset branch. Never include conversation text.**

- [ ] **Step 4: Re-run all companion, lifecycle, command, diagnostics, power, realm, protection, interaction, and performance unit suites.**

- [ ] **Step 5: Commit with `feat: integrate Shadow lifecycle and diagnostics`.**

### Task 10: Live Proof, Resources, Documentation, and Release Verification

**Files:**
- Modify: `src/gametest/java/com/powers/gametest/PowersGameTests.java`
- Create: `src/test/java/com/powers/performance/ShadowCompanionSoakTest.java`
- Modify: `src/main/resources/assets/powers/lang/en_us.json`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/gameplay/POWER_CATALOGUE.md`
- Modify: `docs/interactions/POWER_INTERACTION_MATRIX.md`
- Modify: `docs/quality/PLAYTEST_ACCEPTANCE.md`
- Modify: `docs/verification/2026-08-08-magic-quality-release.md`
- Modify: exact Java/asset manifests through their repository scripts only after all source/assets settle.

**Interfaces:**
- Produces: live acceptance evidence for ordinary gamemode preservation, identity/visibility/death, conversation/tasks, conjuration, all 26 executors, magic interactions, tactics/learning, confinement, and cleanup.

- [ ] **Step 1: Add GameTests for each critical contract listed in the design, splitting async behavior into bounded delayed assertions and ensuring every test calls `helper.succeed()`.**

- [ ] **Step 2: Add the 10/50/100 Shadow synthetic soak and assert candidate, learning, forced-chunk, packet, particle, summon, projectile, and per-tick work caps.**

- [ ] **Step 3: Update translations and documentation with exact commands, behaviors, energy/costs, allowed/forbidden conjuration, all powers, tactics, learning/reset/privacy, migration, and limitations proven by tests.**

- [ ] **Step 4: Run `./gradlew test --rerun-tasks --no-daemon`, `./gradlew runGameTest --no-daemon`, dedicated-server boot, resource validation, magic-doc verification, and source/asset audits.**

- [ ] **Step 5: Launch `./test.sh client` and manually verify skin/model, hidden/revealed presentation, chat scope, navigation, close/far combat decisions, FX, item delivery/rite, amethyst/Light/Darkness reactions, and mindscape gamemode preservation. Record only observed results.**

- [ ] **Step 6: Regenerate magic docs and exact Java/non-item asset manifests using repository scripts, then run `./test.sh check` from a clean build.**

- [ ] **Step 7: Commit with `docs: verify advanced Shadow companion`, confirm a clean worktree, push `codex/powers-finalisation`, and report the exact automated/live evidence without claiming unperformed manual checks.**
