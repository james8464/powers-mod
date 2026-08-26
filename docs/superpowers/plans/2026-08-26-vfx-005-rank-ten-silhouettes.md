# VFX-005 Rank-Ten Silhouettes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one bounded, server-authoritative, long-distance silhouette for each of the 23 rank-ten innate transformations.

**Architecture:** A pure shared catalogue defines immutable profile geometry and validation. The committed innate cast path emits one compact semantic payload through a capped observer service; a connection-scoped client manager expands it into one depth-tested batched world-render submission independent of particles and player poses.

**Tech Stack:** Java 25, Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Fabric Loom 1.17.19, JUnit 5, Fabric server/client GameTests, Python 3.

**Spec:** `docs/superpowers/specs/2026-08-26-vfx-005-rank-ten-silhouettes-design.md`

## Global Constraints

- The exact catalogue is the 23 IDs in `InnatePowerLevels.powerIds()`.
- Only successful `CastSource.INNATE` rank-ten activations emit. Selection, failure, artifact, spell, crystal, toggle-off, and sub-rank-ten paths emit nothing.
- Server work is capped at 32 offers/tick, one caster/power/tick, 384 blocks, same dimension, and no chunk tickets.
- Client state is connection/dimension scoped, capped at 64 events, and uses a 1..80 receipt-local lifetime (40 authored).
- Profiles contain at most 64 primitives and 256 vertices; all numeric inputs are finite.
- The depth-tested renderer is particle-independent and static under reduced motion.
- Artifact, Shadow, Herald, First Vessel, sound, and VFX-006 pose work remain out of scope.
- All gates use `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.

---

### Task 1: Pure catalogue, geometry, and client lifecycle

**Files:**
- Create: `src/main/java/com/powers/fx/RankTenSilhouetteProfile.java`
- Create: `src/main/java/com/powers/fx/RankTenSilhouetteGeometry.java`
- Create: `src/main/java/com/powers/fx/ClientRankTenSilhouetteState.java`
- Create: `src/test/java/com/powers/fx/RankTenSilhouetteProfileTest.java`
- Create: `src/test/java/com/powers/fx/RankTenSilhouetteGeometryTest.java`
- Create: `src/test/java/com/powers/fx/ClientRankTenSilhouetteStateTest.java`
- Modify: `docs/quality/code-audit.md`

**Interfaces:**
- Produces `RankTenSilhouetteProfile.forPower(String)`, `powerIds()`, `networkId()`, `fromNetworkId(int)`, `alignmentPalette(boolean)`, and immutable `primitives()`.
- Produces `RankTenSilhouetteGeometry.mesh(Profile,Event,Camera,boolean)` returning capped `Mesh(vertices,primitiveSignature)`.
- Produces `ClientRankTenSilhouetteState.empty(int,long,String)`, `receive(Wire,long,long,String)`, `tick()`, `reset(long,String)`, and `entries()`.

- [ ] **Step 1: Write failing catalogue/geometry tests.** Assert exact equality with `InnatePowerLevels.powerIds()`, count 23, 23 distinct monochrome primitive signatures, finite vertices, <=64 primitives, <=256 vertices, legal palettes, and normal/reduced outer-outline equality.

```java
assertEquals(InnatePowerLevels.powerIds(), RankTenSilhouetteProfile.powerIds());
assertEquals(23, RankTenSilhouetteProfile.powerIds().stream()
        .map(id -> RankTenSilhouetteProfile.forPower(id).orElseThrow().primitiveSignature())
        .distinct().count());
```

- [ ] **Step 2: Run RED.**

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.fx.RankTenSilhouette*' --rerun-tasks --no-daemon --console=plain
```

Expected: missing production types.

- [ ] **Step 3: Implement the exact 23-profile catalogue.** Use sealed `Segment`, `Ring`, and `Disc` primitives. Validate unique IDs/network IDs/signatures, finite coordinates, and caps at static construction. Canonicalise `size_morph` to `size_shift`; unknown IDs return empty.

- [ ] **Step 4: Write failing state tests.** Prove validation-before-mutation, capacity 64, replay idempotence, stale epoch/dimension rejection, exact receipt-local expiry, overflow saturation, disconnect/dimension reset, and reload preservation.

- [ ] **Step 5: Implement immutable client state, run focused GREEN, regenerate `docs/quality/code-audit.md`, and commit.**

```bash
python3 scripts/audit_java_sources.py
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.fx.RankTenSilhouette*' --tests com.powers.fx.ClientRankTenSilhouetteStateTest auditJavaSources --rerun-tasks --no-daemon --console=plain
git add src/main/java/com/powers/fx/RankTenSilhouetteProfile.java src/main/java/com/powers/fx/RankTenSilhouetteGeometry.java src/main/java/com/powers/fx/ClientRankTenSilhouetteState.java src/test/java/com/powers/fx/RankTenSilhouetteProfileTest.java src/test/java/com/powers/fx/RankTenSilhouetteGeometryTest.java src/test/java/com/powers/fx/ClientRankTenSilhouetteStateTest.java docs/quality/code-audit.md
git commit -m "feat(vfx): define rank-ten silhouettes"
```

---

### Task 2: Compact protocol and bounded server delivery

**Files:**
- Create: `src/main/java/com/powers/network/RankTenSilhouettePackets.java`
- Create: `src/main/java/com/powers/fx/RankTenSilhouetteService.java`
- Create: `src/test/java/com/powers/network/RankTenSilhouettePacketsTest.java`
- Create: `src/test/java/com/powers/fx/RankTenSilhouetteServiceTest.java`
- Modify: `src/main/java/com/powers/PowersMod.java`
- Modify: `docs/quality/code-audit.md`

**Interfaces:**
- Produces `RankTenSilhouettePackets.Payload(long eventId,int profileId,UUID caster,String dimension,double x,double y,double z,float yaw,float pitch,int alignmentId,int visualSeed,int lifetimeTicks)` and `initialize()`.
- Produces `RankTenSilhouetteService.afterSuccessfulInnateCast(ServerPlayer,String)`, `clear(MinecraftServer)`, and immutable diagnostics.

- [ ] **Step 1: Write failing codec/policy tests.** Round-trip every field; reject invalid strings, IDs, lifetime, UUID/session, and non-finite geometry. Prove same-dimension range selection, one-per-caster/profile/tick coalescing, 32/tick cap, unsupported observer cancellation, event-ID exhaustion, and zero tickets.

- [ ] **Step 2: Run RED.** Expect missing payload/service types.

- [ ] **Step 3: Implement validated clientbound codec and register it in `PowersMod`.** Dimension UTF-8 is bounded; constructor delegates to the pure wire contract.

- [ ] **Step 4: Implement server ownership.** Require canonical profile and effective rank >=10, inspect only `ServerLevel.players()`, use squared distance <=384^2, `canSend`, guarded live-session/dimension predicates, 32/tick cap, and fail-closed event exhaustion. Delivery failure never affects gameplay.

- [ ] **Step 5: Run focused GREEN/audit and commit.**

```bash
python3 scripts/audit_java_sources.py
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.network.RankTenSilhouettePacketsTest --tests com.powers.fx.RankTenSilhouetteServiceTest auditJavaSources --rerun-tasks --no-daemon --console=plain
git add src/main/java/com/powers/network/RankTenSilhouettePackets.java src/main/java/com/powers/fx/RankTenSilhouetteService.java src/test/java/com/powers/network/RankTenSilhouettePacketsTest.java src/test/java/com/powers/fx/RankTenSilhouetteServiceTest.java src/main/java/com/powers/PowersMod.java docs/quality/code-audit.md
git commit -m "feat(vfx): deliver rank-ten silhouettes"
```

---

### Task 3: Successful-cast integration and negative boundaries

**Files:**
- Modify: `src/main/java/com/powers/power/AbilityActivationService.java`
- Create: `src/test/java/com/powers/fx/RankTenSilhouetteIntegrationSourceTest.java`
- Create: `src/gametest/java/com/powers/fx/RankTenSilhouetteGameTests.java`
- Modify: `src/gametest/resources/fabric.mod.json`
- Modify: `docs/quality/code-audit.md`

**Interfaces:** Consumes `RankTenSilhouetteService.afterSuccessfulInnateCast`; produces exactly one hook after committed ordinary/input/toggle-on innate activation.

- [ ] **Step 1: Write failing source-boundary and GameTests.** Exercise all 23 production IDs at rank 10. Separately prove rank 9, artifact source, selection, cooldown, insufficient energy, failed execution, toggle-off, 33 offers, range/dimension, unsupported/stale session, exhaustion, and ticket invariants.

```java
assertEquals(InnatePowerLevels.powerIds(),
        RankTenSilhouetteService.diagnostics(server).acceptedProfiles());
```

- [ ] **Step 2: Run `runGameTest` and prove RED.**

- [ ] **Step 3: Add the hook only inside committed `cast(...)` and committed toggle-on branches.**

```java
if (source == CastSource.INNATE) {
    RankTenSilhouetteService.afterSuccessfulInnateCast(player, ability.id().getPath());
}
```

Input activation already uses `cast`; selection, toggle-off, artifact teleport, and failures stay outside.

- [ ] **Step 4: Run focused JUnit, all GameTests, audit, and commit.**

```bash
python3 scripts/audit_java_sources.py
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.fx.RankTenSilhouetteIntegrationSourceTest runGameTest auditJavaSources --rerun-tasks --no-daemon --console=plain
git add src/main/java/com/powers/power/AbilityActivationService.java src/test/java/com/powers/fx/RankTenSilhouetteIntegrationSourceTest.java src/gametest/java/com/powers/fx/RankTenSilhouetteGameTests.java src/gametest/resources/fabric.mod.json docs/quality/code-audit.md
git commit -m "test(vfx): prove rank-ten cast boundaries"
```

---

### Task 4: Client manager, renderer, and lifecycle wiring

**Files:**
- Create: `src/client/java/com/powers/client/fx/ClientRankTenSilhouetteManager.java`
- Create: `src/client/java/com/powers/client/fx/ClientRankTenSilhouetteRenderer.java`
- Modify: `src/client/java/com/powers/client/PowersClient.java`
- Create: `src/test/java/com/powers/fx/RankTenSilhouetteRendererSourceTest.java`
- Modify: `docs/quality/code-audit.md`

**Interfaces:** Manager produces `captureHandlerStamp`, `handle`, `tick`, `resetConnectionEpoch`, `entries`, and reload-preserving callbacks. Renderer produces `initialize`, `closeResources`, `recreateResources`, and test-visible `renderActualProfileMesh`.

- [ ] **Step 1: Write failing client-boundary tests.** Require handler stamp capture before enqueue, tick lifecycle, disconnect/dimension reset, reload preservation, `COLLECT_SUBMITS`, one capped custom-geometry batch, depth-tested `RenderTypes.debugQuads()`, and no particle/FOV/sound/model dependency.

- [ ] **Step 2: Run RED.** Expect missing client types and wiring assertions.

- [ ] **Step 3: Implement manager and renderer.** Mirror the proven scar ownership pattern without shared mutable state. Submit one nearest-first capped quad mesh relative to camera. Normal phase uses lifecycle+seed; reduced phase is zero and retains outline with lower fill alpha.

- [ ] **Step 4: Wire payload receiver, tick, disconnect, join, and synchronous resource reload in `PowersClient`.** Capture epoch/dimension before `client.execute`.

- [ ] **Step 5: Compile/test/audit and commit.**

```bash
python3 scripts/audit_java_sources.py
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.fx.RankTenSilhouetteRendererSourceTest compileClientJava compileGametestJava auditJavaSources --rerun-tasks --no-daemon --console=plain
git add src/client/java/com/powers/client/fx/ClientRankTenSilhouetteManager.java src/client/java/com/powers/client/fx/ClientRankTenSilhouetteRenderer.java src/client/java/com/powers/client/PowersClient.java src/test/java/com/powers/fx/RankTenSilhouetteRendererSourceTest.java docs/quality/code-audit.md
git commit -m "feat(vfx): render rank-ten silhouettes"
```

---

### Task 5: Real-client gallery and deterministic verifier

**Files:**
- Create: `src/gametest/java/com/powers/client/RankTenSilhouetteClientGameTests.java`
- Modify: `src/gametest/resources/fabric.mod.json`
- Modify: `build.gradle`
- Create: `scripts/verify_vfx005_captures.py`
- Create: `scripts/tests/test_verify_vfx005_captures.py`
- Modify: `docs/quality/code-audit.md`

**Interfaces:** Produces `runClientGameTest -Pvfx005ClientOnly`, JSONL manifest, and verifier CLI `--screenshots --manifest --output`.

- [ ] **Step 1: Write failing Python tests.** Reject missing rows, wrong dimensions, blank foreground, duplicate monochrome masks, reduced-outline mismatch, crosshair intrusion, and wall leakage; include one green synthetic fixture.

- [ ] **Step 2: Run RED:** `python3 -m unittest scripts.tests.test_verify_vfx005_captures` must fail because the verifier is absent.

- [ ] **Step 3: Implement verifier.** Require exact 1280x720 rows: 23 far normal, 23 far reduced, legal alignment variants, near safety, wall, minimal particles, reload, dimension, reconnect. Verify non-empty foreground, pairwise monochrome distinction, normal/reduced outline equivalence, clear crosshair ROI, and zero wall leakage; emit sorted JSON.

- [ ] **Step 4: Implement integrated-client fixture.** Use the production server hook, packet, manager, and renderer. Fix GUI scale/FOV/camera/window/weather/time/background. Record ID, alignment, distance, reduced mode, particles, reload revision, epoch, and image path; never call renderer helpers instead of the packet path.

- [ ] **Step 5: Run gallery/verifier/audit and commit.**

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runClientGameTest -Pvfx005ClientOnly --rerun-tasks --no-daemon --console=plain
python3 -m unittest scripts.tests.test_verify_vfx005_captures
python3 scripts/verify_vfx005_captures.py --screenshots build/run/clientGameTest/screenshots --manifest build/run/clientGameTest/vfx005-manifest.jsonl --output /tmp/vfx005-capture-verification.json
python3 scripts/audit_java_sources.py
git add src/gametest/java/com/powers/client/RankTenSilhouetteClientGameTests.java src/gametest/resources/fabric.mod.json build.gradle scripts/verify_vfx005_captures.py scripts/tests/test_verify_vfx005_captures.py docs/quality/code-audit.md
git commit -m "test(vfx): capture rank-ten silhouettes"
```

---

### Task 6: Exact evidence, independent review, closure, integration

**Files:**
- Create: `docs/verification/evidence/2026-08-26-vfx-005/{README.md,build-metadata.json,capture-verification.json,independent-review.md,SHA256SUMS}`
- Create: `docs/verification/evidence/2026-08-26-vfx-005/logs/*`
- Create: `docs/verification/evidence/2026-08-26-vfx-005/screenshots/*`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/planning/IMPROVEMENT_BACKLOG.md`
- Modify: `docs/superpowers/plans/2026-08-12-stages-1-8-completion.md`

- [ ] **Step 1: Run literal exact-implementation aggregate without filters.**

```bash
set -o pipefail
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon --console=plain 2>&1 | tee /tmp/vfx005-final-check.log
```

- [ ] **Step 2: Recapture the unchanged implementation SHA gallery and verifier output.** Retain transcripts, screenshots, manifest, versions, row/test counts, and verifier metrics.

- [ ] **Step 3: Package evidence.** Redact private paths, privacy-scan, checksum every file except the manifest, verify `shasum -a 256 -c SHA256SUMS`, and reject missing/extra files.

- [ ] **Step 4: Obtain independent READY review.** It must inspect catalogue completeness, actual committed-cast hook, negative boundaries, budgets, stale/unsupported handling, lifecycle, occlusion, reduced motion, pairwise distinction, exact SHA, checksums, and privacy. Resolve all P0/P1/P2 findings and repeat affected gates.

- [ ] **Step 5: Close only VFX-005.** Remove its backlog row, mark its completion row, update README/CHANGELOG truthfully, stage owned paths, and commit `docs(vfx): accept rank-ten silhouettes`.

- [ ] **Step 6: Run the literal full check on closure head, fast-forward main, push, prove `main == origin/main`, and prove every owned worktree clean.** Continue strict order with VFX-006.
