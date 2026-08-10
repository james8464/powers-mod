# Runtime Integrity and Frozen Avatar Implementation Plan

> Execute every task test-first. Run the named focused test and observe the intended failure before production edits; make it pass minimally; run the surrounding suite; commit at each checkpoint.

**Goal:** Make casting presentation significance-aware, bound all hot-path work, eliminate remaining synchronous travel loads, and render an exact frozen player snapshot.

**Architecture:** Add small pure policy/value types first. Runtime integrations consume them without duplicating limits. Player appearance becomes one immutable codec-backed `BodySnapshot` used by the proxy and client renderer.

**Stack:** Java 25, Fabric API 26.2, JUnit 5, Fabric GameTest, Mojang codecs/custom payloads.

## Task 1: Explicit action significance

**Tests:** modify `src/test/java/com/powers/magic/MagicActionCatalogueTest.java`, `MagicCastPresentationTest.java`, `MagicDocumentationTest.java`.

1. Add failing assertions that every action has explicit `MagicSignificance`, lightning/fireball are `MINIMAL`, spells are `RITUAL`, time freeze/celestial ruin are `COSMIC`, and `NONE` emits no generic sequence.
2. Add `src/main/java/com/powers/magic/MagicSignificance.java` and a non-null field to `MagicActionDefinition`.
3. Assign all catalogue entries explicitly and map significance to 0/1/2/4/6 beats in `MagicCastPresentation`/`FxChoreography`.
4. Make `ServerMagicCasts` avoid duplicate generic effects when the action declares bespoke satisfaction.
5. Include significance in generated action documentation and regenerate manifests.
6. Run `./gradlew test --tests 'com.powers.magic.*' --tests 'com.powers.magic.fx.*'`.

## Task 2: Actual-recipient particle and semantic FX budgets

**Tests:** extend `ParticleBudgetTest`, `MagicFxServiceTest`, and add `src/test/java/com/powers/fx/ViewerParticleBudgetTest.java`.

1. First prove one broadcast to N viewers costs N recipient-particles and per-viewer limits are independent.
2. Implement `ViewerParticleBudget` with server-tick reset, server cap 512, viewer cap 128, distance culling, and cosmic exemption only from ordinary range—not caps.
3. Route `PowerFx` send sites through recipient-aware claims; replace beam point packets with one semantic cue handled by `ClientMagicFx`.
4. Remove duplicate lightning/fireball foreground clouds while retaining bolt/projectile/thunder feedback.
5. Run focused FX tests and `compileClientJava`.

## Task 3: Bounded, exception-safe scheduling

**Tests:** extend `ScheduledTaskQueueTest`; add `src/test/java/com/powers/ServerMagicSchedulerTest.java`.

1. Write failing cases for capacity 8,192, per-tick 256 execution budget, stable spillover order, cancelled tokens, callback exception isolation, and 32-storm cap/one-per-owner replacement.
2. Add cancellation token and bounded queue behavior to `ScheduledTaskQueue`.
3. Make `ServerMagicScheduler` store owner/action IDs and resolve entities at execution instead of retaining entity objects.
4. Bound storms and catch/report callbacks through a rate-limited logger.
5. Add stop/disconnect/world-unload cleanup and run focused tests.

## Task 4: Remove hot-path global entity scans and consolidate ticks

**Tests:** extend `LivingForceIndexTest`; add `src/test/java/com/powers/ServerTickWorkBudgetTest.java` with deterministic 20/50-player simulations.

1. Prove aura candidate visits remain proportional to occupied nearby buckets rather than all entities.
2. Add nearby-entry iteration to `LivingForceIndex` and use it in `LivingForceManager.tickAuras`.
3. Introduce one `PlayerTickCoordinator` call from `PowersMod`; schedule passives, regen, state cleanup, and dirty sync by cadence in one player loop.
4. Preserve all existing lifecycle calls and verify sync sends only changed state.
5. Run focused performance tests, then `./gradlew test`.

## Task 5: Complete asynchronous travel migration

**Tests:** extend `TravelChunkLoaderTest` and add source-audit assertions forbidding synchronous `getChunk(` in ability/realm/body travel paths.

1. Write failing timeout, replacement, disconnect, invalid destination, and ticket-release tests for Middleworld, realm confinement, and body return use cases.
2. Route `MiddleworldAbility`, `RealmConfinementManager`, and `BodyProxyManager.returnToBody` through `TravelChunkLoader`.
3. Store one pending token per player, revalidate after load, release tickets on every outcome, and use action-bar failure text.
4. Run travel tests and dedicated-server boot.

## Task 6: Immutable full body snapshot

**Tests:** add `src/test/java/com/powers/mind/BodySnapshotTest.java` and extend `BodyProxyKindTest`.

1. Add failing codec round-trip/clamp/malformed tests for profile, model parts, arm, equipment descriptors, active hand/use ticks, rotations, pose, bed orientation, swing/limb frame, scale, velocity, position/dimension, and token.
2. Add `src/main/java/com/powers/mind/BodySnapshot.java` with a bounded stream codec and validated constructor.
3. Capture it exactly once in `BodyProxyManager` and persist the minimum return-safe subset in `MindBodyState`.
4. Preserve correct resolved player profile and vanilla default-skin fallback.

## Task 7: Frozen proxy renderer and payload

**Tests:** add packet codec tests and `src/gametest/java/com/powers/gametest/BodyProxyGameTests.java`.

1. Register snapshot spawn/sync payloads and prove only tracking clients receive bounded snapshots.
2. Add `src/client/java/com/powers/client/body/FrozenBodyRenderState.java` and `FrozenBodyRenderer.java`, reusing vanilla classic/slim player models and skin resolution.
3. Freeze recorded head/body/pitch, pose, limb, swing, hand-use, model parts, main arm, scale, and equipment; never derive animation from the stationary proxy.
4. GameTest vulnerability, recursion guard, equipment non-duplication, exact one-time cleanup, and async return.
5. Run `test`, `runGameTestServer`, `compileClientJava`, and a client visual-smoke checklist with two skins/poses.

## Task 8: Runtime checkpoint

1. Run `./gradlew clean check runGameTestServer validatePowerResources` with Java 25.
2. Run dedicated server start/stop and inspect errors/warnings.
3. Regenerate relevant audits/docs; run `git diff --check`.
4. Commit: `feat: bound magic runtime and preserve frozen avatars`.

