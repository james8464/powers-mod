# VFX-006 Synchronised Casting Poses Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bounded server-authoritative casting poses for Shadow, artifact guardians, both Heralds, and First Vessel, with latency-correct client playback and no heavy animation dependency.

**Architecture:** A pure shared contract and ledger validate compact semantic pose events. A server service emits only to current tracking players and snapshots active state at tracking start; a connection-epoch client manager binds entity ID to UUID and derives playback age from authoritative game time. POWERS-only renderer state/model subclasses add clamped joint rotations after vanilla humanoid setup.

**Tech Stack:** Java 25, Minecraft 26.2 official names, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Loom 1.17.19, JUnit 6, Fabric GameTest, existing integrated-client acceptance harness.

**Spec:** `docs/superpowers/specs/2026-08-26-vfx-006-synchronised-casting-poses-design.md`

## Global Constraints

- In scope: `ShadowCompanionEntity`, `RadiantSentinel`, `DarknessCreature`, both `RealmHerald` types, and `FirstVessel` only.
- Exclude actual players, `EchoClone`, `PowerTestActor`, projectiles, and non-player-shaped mobs.
- Presentation only: do not change AI, action cadence, damage, collision, hitboxes, navigation, persistence, protection, or permissions.
- Pose IDs are `INVOKE`, `PROJECT`, `CHANNEL`, and `RELEASE`; durations are 1–120 ticks.
- Server cap: 256 active entries, one start per entity/tick, 64 offered events/tick, no chunk tickets or global player scan.
- Client cap: 128 entries with connection/world epoch, entity-ID/UUID, sequence, and five-tick future-skew protection.
- Normal added joint rotations stay within arm 1.25 rad, body 0.35 rad, head 0.25 rad; reduced motion is static and lower amplitude.
- No animation library, keyframe asset format, vanilla-player mixin, or global humanoid renderer hook.
- Every behavior change follows a witnessed RED → GREEN cycle.
- Final acceptance requires the literal `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon --console=plain` command on implementation and closure heads.

---

### Task 1: Shared pose contract, mapping, and compact payload

**Files:**
- Create: `src/main/java/com/powers/animation/CastingPose.java`
- Create: `src/main/java/com/powers/animation/CastingStyle.java`
- Create: `src/main/java/com/powers/animation/CastingHand.java`
- Create: `src/main/java/com/powers/animation/CastingPoseEvent.java`
- Create: `src/main/java/com/powers/animation/CastingPoseRules.java`
- Create: `src/main/java/com/powers/network/CastingPosePackets.java`
- Create: `src/test/java/com/powers/animation/CastingPoseContractTest.java`
- Create: `src/test/java/com/powers/network/CastingPosePacketsTest.java`
- Modify: `src/main/java/com/powers/PowersMod.java`

**Interfaces:**
- Produces: `CastingPose.fromNetworkId(int)`, `CastingStyle.fromNetworkId(int)`, `CastingHand.fromNetworkId(int)` returning `Optional`.
- Produces: `CastingPoseEvent(int entityId, UUID entityUuid, long sequence, CastingPose pose, CastingStyle style, CastingHand hand, long startGameTime, int durationTicks)`.
- Produces: `CastingPoseRules.progress(long worldGameTime, CastingPoseEvent event)` and `CastingPoseRules.active(long, CastingPoseEvent)`.
- Produces: `CastingPosePackets.Payload` with `event()` conversion and clientbound codec registration.

- [x] **Step 1: Write a reflection-backed failing contract test so missing production types fail as an assertion, not a compiler error.**

```java
@Test
void invalidDurationCannotConstructPoseEvent() throws Exception {
    Class<?> type;
    try {
        type = Class.forName("com.powers.animation.CastingPoseEvent");
    } catch (ClassNotFoundException missing) {
        fail("CastingPoseEvent is not implemented");
        return;
    }
    Constructor<?> constructor = type.getConstructors()[0];
    assertThrows(InvocationTargetException.class, () -> constructor.newInstance(
            7, UUID.fromString("11111111-1111-1111-1111-111111111111"), 1L,
            enumValue("com.powers.animation.CastingPose", "PROJECT"),
            enumValue("com.powers.animation.CastingStyle", "RADIANT"),
            enumValue("com.powers.animation.CastingHand", "RIGHT"), 100L, 0));
}
```

- [x] **Step 2: Run the test and witness RED.**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.animation.CastingPoseContractTest --rerun-tasks --no-daemon --console=plain`

Expected: assertion failure `CastingPoseEvent is not implemented`.

- [x] **Step 3: Implement the closed enums, validated event, and pure time rules.**

```java
public enum CastingPose {
    INVOKE(0), PROJECT(1), CHANNEL(2), RELEASE(3);
    private final int networkId;
    public int networkId() { return networkId; }
    public static Optional<CastingPose> fromNetworkId(int id) {
        return Arrays.stream(values()).filter(value -> value.networkId == id).findFirst();
    }
}

public record CastingPoseEvent(int entityId, UUID entityUuid, long sequence,
        CastingPose pose, CastingStyle style, CastingHand hand,
        long startGameTime, int durationTicks) {
    public CastingPoseEvent {
        if (entityId < 0 || entityUuid == null || entityUuid.equals(new UUID(0L, 0L)))
            throw new IllegalArgumentException("entity identity");
        if (sequence <= 0 || pose == null || style == null || hand == null)
            throw new IllegalArgumentException("pose identity");
        if (startGameTime < 0 || durationTicks < 1 || durationTicks > 120)
            throw new IllegalArgumentException("pose timing");
    }
}

public static double progress(long gameTime, CastingPoseEvent event) {
    if (gameTime <= event.startGameTime()) return 0.0;
    return Math.clamp((gameTime - event.startGameTime()) / (double) event.durationTicks(), 0.0, 1.0);
}
```

- [x] **Step 4: Add direct table tests for all enum IDs, zero UUID, sequence, start-time/duration bounds, expiry, and latency-derived literal progress.**

```java
@Test
void lateReceiptUsesAuthoritativeStartInsteadOfRestarting() {
    var event = event(20L, 20);
    assertEquals(0.5, CastingPoseRules.progress(30L, event));
    assertTrue(CastingPoseRules.active(39L, event));
    assertFalse(CastingPoseRules.active(40L, event));
}
```

- [x] **Step 5: Run contract tests GREEN, then write payload codec/constructor tests RED and implement `CastingPosePackets.Payload`.**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.animation.*' --tests com.powers.network.CastingPosePacketsTest --rerun-tasks --no-daemon --console=plain`

Payload encoding order: entity ID, UUID, sequence, pose ID, style ID, hand ID, start game time, duration. Decode unknown IDs by throwing `IllegalArgumentException`; register once from `PowersMod.onInitialize()`.

- [x] **Step 6: Commit the shared contract.**

```bash
git add src/main/java/com/powers/animation src/main/java/com/powers/network/CastingPosePackets.java \
  src/main/java/com/powers/PowersMod.java src/test/java/com/powers/animation \
  src/test/java/com/powers/network/CastingPosePacketsTest.java
git commit -m "feat(vfx): define synchronized casting pose contract"
```

### Task 2: Bounded server ledger, tracking delivery, and lifecycle

**Files:**
- Create: `src/main/java/com/powers/animation/CastingPoseLedger.java`
- Create: `src/main/java/com/powers/animation/CastingPoseService.java`
- Create: `src/test/java/com/powers/animation/CastingPoseLedgerTest.java`
- Create: `src/test/java/com/powers/animation/CastingPoseServiceTest.java`
- Modify: `src/main/java/com/powers/PowersServerLifecycle.java`

**Interfaces:**
- Consumes: `CastingPoseEvent`, `CastingPosePackets.Payload`.
- Produces: `CastingPoseLedger.offer(Key, Request, long tick)`, `snapshot(UUID,long)`, `tick(long, Predicate<UUID>)`, and immutable `Metrics`.
- Produces: `CastingPoseService.start(LivingEntity, CastingPose, CastingStyle, CastingHand, int)`, `clear(LivingEntity)`, `current(UUID)`, `trackingStarted(Entity, ServerPlayer)`, `tick(MinecraftServer)`, `clearAll()`.

- [x] **Step 1: Write failing ledger tests for monotonic replacement, same-tick coalescing, 64/tick and 256-entry caps, expiry, exhaustion, and snapshot age preservation.**

```java
@Test
void sameEntitySameTickReplacesWithoutConsumingTwoOffers() {
    var ledger = new CastingPoseLedger();
    assertTrue(ledger.offer(key(1), request(CastingPose.INVOKE), 50L).accepted());
    assertTrue(ledger.offer(key(1), request(CastingPose.PROJECT), 50L).accepted());
    assertEquals(CastingPose.PROJECT, ledger.snapshot(UUID_ONE, 50L).orElseThrow().pose());
    assertEquals(1, ledger.metrics().offeredThisTick());
}
```

- [x] **Step 2: Run ledger tests RED, implement a pure UUID-keyed ledger with primitive identity snapshots, and run GREEN.**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.animation.CastingPoseLedgerTest --rerun-tasks --no-daemon --console=plain`

`offer` increments per-entity sequence with `Math.addExact`; overflow rejects without wrap. Capacity saturation evicts only expired entries; otherwise reject presentation.

- [x] **Step 3: Write service boundary tests RED using a narrow `RuntimeAccess` fake that records tracking observers and guarded sends.**

```java
assertEquals(List.of(OBSERVER_A), runtime.sentTo());
assertEquals(0L, runtime.chunkTicketsRequested());
assertFalse(runtime.allPlayersScanned());
```

- [x] **Step 4: Implement service delivery through `PlayerLookup.tracking(entity)`, `ServerPlayNetworking.canSend`, and `PowersPlayNetworking.sendGuarded`; register `EntityTrackingEvents.START_TRACKING`.**

```java
for (ServerPlayer observer : PlayerLookup.tracking(entity)) {
    if (ServerPlayNetworking.canSend(observer, CastingPosePackets.Payload.TYPE)) {
        PowersPlayNetworking.sendGuarded(observer, payload,
                current -> current.level() == entity.level() && !current.hasDisconnected(),
                () -> { }, failure -> { });
    }
}
```

- [x] **Step 5: Wire server tick and stop cleanup and run service + lifecycle tests GREEN.**

Add `CastingPoseService.tick(server)` after authoritative action systems and `CastingPoseService.clearAll()` in `onServerStopped`.

- [x] **Step 6: Commit the bounded server runtime.**

```bash
git add src/main/java/com/powers/animation/CastingPoseLedger.java \
  src/main/java/com/powers/animation/CastingPoseService.java \
  src/main/java/com/powers/PowersServerLifecycle.java \
  src/test/java/com/powers/animation/CastingPoseLedgerTest.java \
  src/test/java/com/powers/animation/CastingPoseServiceTest.java
git commit -m "feat(vfx): deliver bounded tracked casting poses"
```

### Task 3: Production action seams and semantic mapping

**Files:**
- Create: `src/main/java/com/powers/animation/CastingPoseMapping.java`
- Create: `src/test/java/com/powers/animation/CastingPoseMappingTest.java`
- Modify: `src/main/java/com/powers/entity/AbstractPlayerLikeMob.java`
- Modify: `src/main/java/com/powers/entity/RealmHerald.java`
- Modify: `src/main/java/com/powers/entity/FirstVessel.java`
- Modify: `src/main/java/com/powers/companion/combat/ShadowPowerExecutor.java`
- Modify: existing focused GameTests or create `src/gametest/java/com/powers/gametest/CastingPoseGameTests.java`

**Interfaces:**
- Consumes: `FirstVesselPowerAction.Kind`, `ShadowPowerExecutor.Handler`.
- Produces: `CastingPoseMapping.forFirstVessel(Kind)`, `forShadow(Handler)`, `style(LivingEntity)`, `hand(String actionId)`, and `duration(ShadowPowerExecutor.Handler)`.
- Production seams call `CastingPoseService.start` only after the gameplay action commits.

- [x] **Step 1: Write mapping tables RED with literal expectations for every First Vessel kind and every supported Shadow handler.**

```java
@ParameterizedTest
@CsvSource({"PROJECTILE,PROJECT", "BEAM,CHANNEL", "AREA,INVOKE", "RECOVERY,CHANNEL"})
void firstVesselKindsMapToStablePoseFamilies(FirstVesselPowerAction.Kind kind, CastingPose want) {
    assertEquals(want, CastingPoseMapping.forFirstVessel(kind));
}
```

- [x] **Step 2: Implement mapping and run unit tests GREEN.**

Use `PROJECT` for guardian lightning/fireball and Shadow `PROJECTILE`; `CHANNEL` for beams/recovery; `INVOKE` for area/control/defense/summon/terrain/apotheosis; `RELEASE` only for explicit exceptional First Vessel flows.

- [x] **Step 3: Write production-seam GameTests RED by clearing pose metrics, forcing each existing action path, and asserting the resulting semantic event; assert protected/countered actions emit none.**

```java
helper.runAfterDelay(2, () -> {
    CastingPoseEvent event = CastingPoseService.current(guardian.getUUID()).orElseThrow();
    helper.assertTrue(event.pose() == CastingPose.PROJECT, "guardian commit must project");
    helper.succeed();
});
```

- [x] **Step 4: Add hooks after successful guardian, Herald, First Vessel, and Shadow commits; add explicit clear calls for Reconstitution interruption/completion.**

```java
boolean success = /* existing action */;
if (success) CastingPoseService.start(shadow, CastingPoseMapping.forShadow(handler),
        CastingStyle.SHADOW, CastingPoseMapping.hand(action.id()),
        CastingPoseMapping.duration(handler));
```

Do not emit before target/protection/dampening checks or from movement, melee, hide, dismiss, or dialogue.

- [x] **Step 5: Run targeted GameTests and unit tests GREEN.**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runGameTest --rerun-tasks --no-daemon --console=plain`

- [x] **Step 6: Commit production seams.**

```bash
git add src/main/java/com/powers/animation/CastingPoseMapping.java \
  src/main/java/com/powers/entity/AbstractPlayerLikeMob.java \
  src/main/java/com/powers/entity/RealmHerald.java src/main/java/com/powers/entity/FirstVessel.java \
  src/main/java/com/powers/companion/combat/ShadowPowerExecutor.java \
  src/test/java/com/powers/animation/CastingPoseMappingTest.java \
  src/gametest/java/com/powers/gametest/CastingPoseGameTests.java
git commit -m "feat(vfx): emit poses from magical entity actions"
```

### Task 4: Client epoch manager and latency-correct pose resolution

**Files:**
- Create: `src/main/java/com/powers/animation/ClientCastingPoseState.java`
- Create: `src/client/java/com/powers/client/animation/ClientCastingPoseManager.java`
- Create: `src/test/java/com/powers/animation/ClientCastingPoseStateTest.java`
- Modify: `src/client/java/com/powers/client/PowersClient.java`

**Interfaces:**
- Produces: pure `ClientCastingPoseState.accept(Wire, HandlerStamp, WorldIdentity, EntityIdentity, long)` and `resolve(UUID,long)`.
- Produces: `ClientCastingPoseManager.captureHandlerStamp(Minecraft)`, `handle(Payload, HandlerStamp)`, `resolve(Entity)`, `resetConnectionEpoch()`, and `tick(Minecraft)`.

- [x] **Step 1: Write client-state tests RED for sequence replay, entity-ID reuse, UUID mismatch, five-tick future skew, expired receipt, stale handler/world, capacity eviction, and latency progress.**

```java
@Test
void reusedNumericIdCannotAnimateDifferentUuid() {
    assertFalse(state.accept(wire(12, UUID_ONE, 2L), stamp(1), world(1),
            entity(12, UUID_TWO), 50L));
    assertTrue(state.entries().isEmpty());
}
```

- [x] **Step 2: Implement the pure state machine and run GREEN.**

Keep immutable value records only; capacity 128 evicts the entry with the earliest `start + duration`, except never replace a same-UUID newer sequence with an older one.

- [x] **Step 3: Write manager boundary tests RED, then implement world/entity lookup, handler stamps, join/disconnect reset, and client tick expiry.**

```java
ClientPlayNetworking.registerGlobalReceiver(CastingPosePackets.Payload.TYPE,
        (payload, context) -> {
            var stamp = ClientCastingPoseManager.captureHandlerStamp(context.client());
            context.client().execute(() -> ClientCastingPoseManager.handle(payload, stamp));
        });
```

- [x] **Step 4: Run client-state tests and `compileClientJava` GREEN.**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.animation.ClientCastingPoseStateTest compileClientJava --rerun-tasks --no-daemon --console=plain`

- [x] **Step 5: Commit the client lifecycle.**

```bash
git add src/main/java/com/powers/animation/ClientCastingPoseState.java \
  src/client/java/com/powers/client/animation/ClientCastingPoseManager.java \
  src/client/java/com/powers/client/PowersClient.java \
  src/test/java/com/powers/animation/ClientCastingPoseStateTest.java
git commit -m "feat(vfx): resolve latency-correct client casting poses"
```

### Task 5: POWERS-only pose models, render states, and accessibility

**Files:**
- Create: `src/main/java/com/powers/animation/CastingPoseAngles.java`
- Create: `src/test/java/com/powers/animation/CastingPoseAnglesTest.java`
- Create: `src/client/java/com/powers/client/animation/CastingHumanoidRenderState.java`
- Create: `src/client/java/com/powers/client/animation/CastingAvatarRenderState.java`
- Create: `src/client/java/com/powers/client/animation/CastingHumanoidModel.java`
- Create: `src/client/java/com/powers/client/animation/CastingPlayerModel.java`
- Modify: `src/client/java/com/powers/client/PlayerLikeMobRenderer.java`
- Modify: `src/client/java/com/powers/client/ShadowCompanionRenderer.java`

**Interfaces:**
- Produces: `CastingPoseAngles.resolve(CastingPose, CastingStyle, CastingHand, double progress, boolean reducedMotion)` returning literal head/body/left-arm/right-arm deltas.
- Render states copy resolved semantic state from `ClientCastingPoseManager`.
- Models call `super.setupAnim(state)` then add the resolved clamped deltas.

- [x] **Step 1: Write angle tests RED for each pose family, handed mirroring, ease boundaries, hard angle caps, and reduced-motion static/lower amplitude.**

```java
@Test
void reducedMotionPreservesDirectionButRemovesTemporalOscillation() {
    var early = CastingPoseAngles.resolve(CastingPose.PROJECT, CastingStyle.RADIANT,
            CastingHand.RIGHT, 0.2, true);
    var late = CastingPoseAngles.resolve(CastingPose.PROJECT, CastingStyle.RADIANT,
            CastingHand.RIGHT, 0.8, true);
    assertEquals(early, late);
    assertTrue(Math.abs(early.rightArmX()) < 1.25);
    assertTrue(Math.abs(early.rightArmX()) > Math.abs(early.leftArmX()));
}
```

- [x] **Step 2: Implement pure angle resolution and run GREEN.**

Use deterministic ease-in/hold/ease-out `amplitude`: 0–0.2 smoothstep in, 0.2–0.75 hold, 0.75–1 smoothstep out. Reduced motion uses constant 0.55 amplitude while active.

- [x] **Step 3: Implement typed render states/models and compile RED/GREEN against the exact 26.2 renderer API.**

```java
@Override
public void setupAnim(CastingHumanoidRenderState state) {
    super.setupAnim(state);
    var delta = state.castingAngles;
    head.xRot += delta.headX();
    body.yRot += delta.bodyY();
    leftArm.xRot += delta.leftArmX();
    rightArm.xRot += delta.rightArmX();
}
```

- [x] **Step 4: Replace models/states only in `PlayerLikeMobRenderer` and `ShadowCompanionRenderer`; leave test actor and Echo Clone unanimated by resolving scope to empty.**

During extraction, query the manager, compute accessibility through `FxAccessibility.reducedMotion(Minecraft.getInstance())`, and store a zero-delta fallback when absent.

- [x] **Step 5: Run angle tests, `compileClientJava`, and renderer-focused tests GREEN.**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.animation.CastingPoseAnglesTest compileClientJava --rerun-tasks --no-daemon --console=plain`

- [x] **Step 6: Commit renderer integration.**

```bash
git add src/main/java/com/powers/animation/CastingPoseAngles.java \
  src/client/java/com/powers/client/animation src/client/java/com/powers/client/PlayerLikeMobRenderer.java \
  src/client/java/com/powers/client/ShadowCompanionRenderer.java \
  src/test/java/com/powers/animation/CastingPoseAnglesTest.java
git commit -m "feat(vfx): render accessible casting poses"
```

### Task 6: Integrated acceptance, deterministic evidence, documentation, and closure

**Files:**
- Create: `src/gametest/java/com/powers/gametest/CastingPoseClientAcceptance.java`
- Modify: `src/gametest/java/com/powers/gametest/PowersClientGameTests.java`
- Create: `scripts/verify_vfx006_gallery.py`
- Create: `scripts/package_vfx006_evidence.py`
- Create: `scripts/tests/test_verify_vfx006_gallery.py`
- Create: `scripts/tests/test_package_vfx006_evidence.py`
- Create: `docs/verification/evidence/2026-08-28-vfx-006/README.md` plus generated manifest/checksums/report/archive inventory.
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/planning/IMPROVEMENT_BACKLOG.md`
- Modify: `docs/superpowers/plans/2026-08-12-stages-1-8-completion.md`
- Modify: this plan checklist as tasks complete.

**Interfaces:**
- Acceptance metadata fields: implementation SHA, entity type, entity UUID, sequence, pose, style, hand, authoritative start tick, duration, receipt tick, reduced-motion flag, scenario, image path, SHA-256.
- Verifier rejects missing scope/pose/style/mode/scenario coverage, non-1280×720 images, identity mismatch, out-of-bounds angles, lifecycle mismatch, privacy leaks, checksum drift, or undeclared archive members.

- [x] **Step 1: Write Python verifier/package tests RED using temporary literal fixtures with one missing pose, one bad dimension, one stale checksum, and one path-privacy leak.**

```python
def test_gallery_rejects_missing_release_pose(self):
    result = verify_fixture(self.fixture_without("RELEASE"))
    self.assertIn("missing pose coverage: RELEASE", result.errors)
```

- [x] **Step 2: Implement deterministic verifier and packager, then run Python tests GREEN.**

Run: `python3 -B -m unittest scripts.tests.test_verify_vfx006_gallery scripts.tests.test_package_vfx006_evidence`

- [x] **Step 3: Add `CastingPoseClientAcceptance.run(context, singleplayer)` scenarios for all six styles × four poses in normal/reduced modes plus latency, late tracking, interruption, expiry, reconnect, and entity-ID reuse; call it from the existing `PowersClientGameTests.runTest` entrypoint.**

Reuse the existing acceptance client capture transport and run-directory isolation. Each screenshot is 1280×720 and the scenario writes its semantic metadata before capture.

- [x] **Step 4: Run the real integrated-client gallery and retain only deterministic owned evidence.**

Run the repository's existing client GameTest command with a fresh ignored run directory, then invoke:

```bash
python3 scripts/verify_vfx006_gallery.py --root docs/verification/evidence/2026-08-28-vfx-006
python3 scripts/package_vfx006_evidence.py --root docs/verification/evidence/2026-08-28-vfx-006
```

- [x] **Step 5: Run a targeted visual review and fix any clipping/readability defect through a new failing angle or acceptance test before recapture.**

Review held items, locomotion compatibility, normal/reduced distinction, pose-family readability, and lifecycle resets. Record findings and disposition in the evidence README.

- [x] **Step 6: Run the literal full gate on the immutable implementation head and bind evidence to that SHA.**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon --console=plain`

- [x] **Step 7: Update truthful closure docs, checksums, archive inventory, and plan/backlog state; commit closure.**

```bash
git add README.md CHANGELOG.md docs/planning/IMPROVEMENT_BACKLOG.md \
  docs/superpowers/plans/2026-08-12-stages-1-8-completion.md \
  docs/superpowers/plans/2026-08-28-vfx-006-synchronised-casting-poses.md \
  docs/verification/evidence/2026-08-28-vfx-006 scripts/verify_vfx006_gallery.py \
  scripts/package_vfx006_evidence.py scripts/tests/test_verify_vfx006_gallery.py \
  scripts/tests/test_package_vfx006_evidence.py src/gametest/java/com/powers/gametest
git commit -m "docs(vfx): accept synchronized casting poses"
```

- [ ] **Step 8: Run the literal full gate again on closure head, perform independent review, fast-forward `main`, verify merged head again, push, and prove every POWERS worktree clean.**

Exact final checks:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon --console=plain
git status --porcelain=v1
git rev-parse main
git rev-parse origin/main
git worktree list --porcelain
```

Continue strict Stage 1–8 order with VFX-007 only after VFX-006 is integrated, pushed, and clean.
