# Magic Presentation, Assets, and Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver original compact magic effects, sounds, textured HUD/screens/rank view, verified non-item assets, and final runtime/release evidence.

**Architecture:** The server sends compact semantic effect events; clients choreograph bounded local geometry using registered sprites and sounds. UI logic is separated into pure layout/state models and texture-backed renderers so accessibility and resolution behaviour are testable.

**Tech Stack:** Fabric client networking/rendering, Minecraft 26.2 GUI sprites/particles/sounds, PNG, Vorbis OGG, JUnit 6, strict Python resource validation.

## Global Constraints

- Create original ancient-cosmic assets; do not copy other mods or television/YouTube properties.
- Exclude new item art from this pass while retaining all current item references.
- Keep critical cues distinguishable by shape and sound, not colour alone.
- Respect reduced motion, effect intensity, distance culling, and server/client particle budgets.
- Keep screens translatable, narrated, keyboard-operable, scalable, and server-authoritative.
- Add no crystal/progression-artifact recipes.
- Every commit remains buildable on the sole `main` branch.

---

### Task 1: Semantic Effect Protocol and Sound Registry

**Files:**
- Create: `src/main/java/com/powers/magic/fx/FxBeat.java`
- Create: `src/main/java/com/powers/magic/fx/FxMotif.java`
- Create: `src/main/java/com/powers/magic/fx/FxSequence.java`
- Create: `src/main/java/com/powers/magic/fx/MagicFxEvent.java`
- Create: `src/main/java/com/powers/magic/fx/MagicFxService.java`
- Create: `src/main/java/com/powers/magic/fx/package-info.java`
- Create: `src/main/java/com/powers/PowersSounds.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `src/main/java/com/powers/fx/PowerFx.java`
- Create: `src/main/resources/assets/powers/sounds.json`
- Create: short `.ogg` files under `src/main/resources/assets/powers/sounds/magic/`
- Test: `src/test/java/com/powers/magic/fx/MagicFxServiceTest.java`

**Interfaces:**
- Consumes: `InteractionCue`, cast positions, dimensions, actor/target IDs.
- Produces: `MagicFxService.emitCast`, `emitInteraction`, and a bounded `MagicFxPayload`.

- [x] **Step 1: Write failing phase, deduplication, and compactness tests**

```java
@Test void majorSequenceHasAnticipationReleaseImpactAndAftermath() { assertEquals(EnumSet.allOf(FxBeat.class), beats(major)); }
@Test void duplicatePairCellTickProducesOneEvent() { assertEquals(1, sink.sizeAfterDuplicate()); }
@Test void payloadContainsSemanticsNotParticleArrays() { assertTrue(encodedSize(event) < 160); }
```

- [x] **Step 2: Run and verify missing FX protocol**

Run: `./gradlew test --tests com.powers.magic.fx.MagicFxServiceTest`

Expected: compilation fails on `MagicFxService`.

- [x] **Step 3: Implement typed sequences and original sounds**

Register at least: `rune_hum`, `crystal_resonate`, `amethyst_fracture`, `time_suspend`, `time_release`, `rift_open`, `rift_close`, `soul_tether`, `light_chorus`, `dark_whisper`, `ward_impact`, `rank_awaken`, and `interaction_clash`. Encode mono Vorbis at a modest bitrate and normalize peaks to avoid clipping.

- [x] **Step 4: Route legacy `PowerFx` calls through semantic helpers where an action definition exists**

Keep `PowerFx` as the bounded server fallback for vanilla particles; do not send geometry lists over the network.

- [x] **Step 5: Validate sounds, run tests, and commit**

Run: `ffprobe` each OGG, `python3 scripts/validate_resources.py --root src/main/resources`, and `./gradlew test`.

```bash
git add src/main/java/com/powers/magic/fx src/main/java/com/powers/PowersSounds.java src/main/java/com/powers/network/PowersPackets.java src/main/java/com/powers/fx/PowerFx.java src/main/resources/assets/powers/sounds.json src/main/resources/assets/powers/sounds src/test/java/com/powers/magic/fx
git commit -m "feat: add semantic magic effects and original sound bank"
```

### Task 2: Client Effect Choreography and Particle Assets

**Files:**
- Create: `src/client/java/com/powers/client/fx/ClientMagicFx.java`
- Create: `src/main/java/com/powers/fx/FxGeometry.java`
- Create: `src/client/java/com/powers/client/fx/FxAccessibility.java`
- Create: `src/client/java/com/powers/client/fx/package-info.java`
- Create: particle registration/factories under `src/client/java/com/powers/client/fx/particle/`
- Create: particle definitions under `src/main/resources/assets/powers/particles/`
- Create: particle textures under `src/main/resources/assets/powers/textures/particle/`
- Modify: `src/client/java/com/powers/client/PowersClient.java`
- Test: `src/test/java/com/powers/fx/FxGeometryTest.java`

**Interfaces:**
- Consumes: compact `MagicFxPayload`.
- Produces: deterministic ring, spiral, tether, fork, shard, glyph, root, eclipse, and fracture point sets.

- [x] **Step 1: Write failing geometry/accessibility/budget tests**

```java
@Test void everyMotifProducesFiniteBoundedPoints() { assertAllMotifsFiniteAndUnder(96); }
@Test void reducedMotionReplacesSpiralsWithStaticRings() { assertEquals(FxMotif.RING, reduced(SPIRAL).motif()); }
@Test void distanceAndIntensityNeverExceedClientBudget() { assertTrue(points(farLowIntensity) <= 12); }
```

- [x] **Step 2: Run and verify missing client geometry**

Run: `./gradlew test --tests com.powers.fx.FxGeometryTest`

Expected: compilation fails on `FxGeometry`.

- [x] **Step 3: Implement deterministic geometry and original particle sprites**

Use the action/cue glyph seed so the same pair reproduces the same shape. Eight sprites are `mote`, `shard`, `glyph`, `ribbon`, `spark`, `eclipse`, `root`, and `fracture`; use transparent edges and nearest-neighbour pixel treatment.

- [x] **Step 4: Register client receiver/factories and verify no server class loads client code**

Run: `./gradlew test && ./gradlew runServer --args='nogui --port 0'` and stop after `Done`.

Expected: server starts without client-class linkage and effect unit tests pass.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/powers/fx/FxGeometry.java src/client/java/com/powers/client/fx src/client/java/com/powers/client/PowersClient.java src/main/resources/assets/powers/particles src/main/resources/assets/powers/textures/particle src/test/java/com/powers/fx
git commit -m "feat: choreograph bounded client magic effects"
```

### Task 3: Effect Icons and Texture-Backed HUD

**Files:**
- Create: `src/main/resources/assets/powers/textures/mob_effect/exhaustion.png`
- Create: `src/main/resources/assets/powers/textures/mob_effect/amethyst_poisoning.png`
- Create: GUI sprites under `src/main/resources/assets/powers/textures/gui/sprites/hud/`
- Create: `src/client/java/com/powers/client/HudLayout.java`
- Modify: `src/client/java/com/powers/client/EnergyHudRenderer.java`
- Modify: `src/client/java/com/powers/client/PowerHudRenderer.java`
- Modify: `src/client/java/com/powers/client/ClientPowerState.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Test: `src/test/java/com/powers/hud/HudLayoutTest.java`

**Interfaces:**
- Consumes: energy/capacity, cooldowns, toggle, rank focus, interaction cue, GUI dimensions/scale.
- Produces: texture-backed normal/empty/darkness/amethyst/projection/interaction layouts.

- [x] **Step 1: Write failing bounds and state-coverage tests**

```java
@ParameterizedTest @MethodSource("commonScreens")
void hudNeverLeavesSafeBounds(int width, int height, int scale) { assertInside(layout(width, height, scale)); }
@Test void everyEnergyModeAndFeedbackStateHasASprite() { assertAllHudSpritesResolve(); }
@Test void cooldownSweepIsMonotonicAndClamped() { assertMonotonicSweep(); }
```

- [x] **Step 2: Run and verify current primitive HUD lacks sprite coverage**

Run: `./gradlew test --tests 'com.powers.hud.*'`

Expected: new sprite-resolution assertions fail.

- [x] **Step 3: Create icons/sprites and pure layout model**

The effect icons are distinct at 18×18: Exhaustion uses an inward-draining indigo spiral; Amethyst Poisoning uses a fractured lavender hexagon. HUD sprites provide frames, fill masks, cooldown runes, toggle flare, error fracture, tether, and interaction crest.

- [x] **Step 4: Replace primitive diamonds/rectangles with GUI sprite rendering**

Retain translatable text alternatives and draw only minimal fallback geometry when a resource reload is in progress. Animate through tick-derived sprite/state selection, not per-frame allocation.

- [x] **Step 5: Run HUD/resource tests and commit**

Run: `./gradlew test --tests 'com.powers.hud.*' && python3 scripts/validate_resources.py --root src/main/resources`

```bash
git add src/main/resources/assets/powers/textures/mob_effect src/main/resources/assets/powers/textures/gui/sprites/hud src/client/java/com/powers/client src/main/java/com/powers/network/PowersPackets.java src/test/java/com/powers/hud
git commit -m "feat: replace the HUD with authored arcane sprites"
```

### Task 4: Teleport, Locator, and Rank-Maze Screens

**Files:**
- Create: `src/client/java/com/powers/client/screen/ArcaneScreenTheme.java`
- Create: `src/client/java/com/powers/client/screen/RankMazeScreen.java`
- Create: `src/client/java/com/powers/client/screen/RankMazeLayout.java`
- Modify: `src/client/java/com/powers/client/screen/TeleportInputScreen.java`
- Modify: `src/client/java/com/powers/client/screen/CelestialLocatorScreen.java`
- Modify: `src/client/java/com/powers/client/PowersClient.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Create: screen sprites under `src/main/resources/assets/powers/textures/gui/sprites/screen/`
- Create: backgrounds under `src/main/resources/assets/powers/textures/gui/advancements/backgrounds/`
- Modify: `src/main/resources/data/powers/advancement/skill_root.json`
- Modify: `src/main/resources/data/powers/advancement/darkness_root.json`
- Modify: `src/main/resources/assets/powers/lang/en_us.json`
- Test: `src/test/java/com/powers/progression/RankMazeLayoutTest.java`

**Interfaces:**
- Consumes: synchronized rank graph/profile, consent-obscured locator rows, teleport modes/dimensions.
- Produces: scalable textured views, keyboard focus, narration, and request-only packets.

- [x] **Step 1: Write failing graph-layout and input-state tests**

```java
@Test void allTwentyEightNodesFitWithoutOverlapAtMinimumSupportedSize() { assertNoOverlap(layout(320, 240)); }
@Test void connectionsOnlyJoinDeclaredParents() { assertEquals(graphEdges, layoutEdges); }
@Test void invalidTeleportInputCannotEmitAPacket() { assertEquals(0, payloadSink.size()); }
@Test void locatorObscuresDeniedTargetsWithoutLeakingCoordinates() { assertObscuredRow(); }
```

- [x] **Step 2: Run and verify missing rank view/theme**

Run: `./gradlew test --tests 'com.powers.progression.RankMazeLayoutTest'`

Expected: compilation fails on `RankMazeLayout`.

- [x] **Step 3: Implement pure maze layout and synchronized rank payload**

Lay nodes by depth band, deterministic branch lane, and collision relaxation with a fixed iteration bound. Client data contains node IDs/titles/perk summaries/status only; unlock/focus requests are revalidated through the existing command/service authority.

- [x] **Step 4: Redesign all three screens and advancement backgrounds**

Use shared nine-slice panels, original branch sigils, focus rings, error banners, and dimension runes. Add a rebindable rank-maze key, narration for every node/row/control, tab order, Escape/cancel, and translatable labels.

- [x] **Step 5: Run screen, networking, resource tests and commit**

Run: `./gradlew test --tests 'com.powers.progression.*' --tests 'com.powers.network.*' && python3 scripts/validate_resources.py --root src/main/resources`

```bash
git add src/client/java/com/powers/client/screen src/client/java/com/powers/client/PowersClient.java src/main/java/com/powers/network/PowersPackets.java src/main/resources/assets/powers/textures/gui src/main/resources/data/powers/advancement src/main/resources/assets/powers/lang/en_us.json src/test/java/com/powers/progression
git commit -m "feat: build textured teleport locator and rank views"
```

### Task 5: Complete Non-Item Asset Audit

**Files:**
- Modify: `scripts/validate_resources.py`
- Create: `scripts/audit_non_item_assets.py`
- Create: `docs/quality/asset-audit.md`
- Create: generated contact sheets under `build/asset-audit/` (ignored build evidence)
- Modify: every non-item resource with a proven finding
- Test: `src/test/java/com/powers/BuildBaselineTest.java`

**Interfaces:**
- Consumes: tracked resources excluding `assets/powers/items`, `models/item`, and `textures/item`.
- Produces: exhaustive manifest with decode/reference/dimension/alpha/visual-review results.

- [x] **Step 1: Extend the failing baseline expectations**

```java
@Test void everyTrackedNonItemAssetHasAnAuditRow() { assertEquals(trackedNonItems(), auditedNonItems()); }
@Test void everyCustomSoundAndImageDecodesAndIsReferenced() { assertTrue(assetAudit().broken().isEmpty()); }
```

- [x] **Step 2: Run audit and capture exact missing/broken/stale assets**

Run: `python3 scripts/audit_non_item_assets.py --check`

Expected: failure until the manifest and new asset coverage are complete.

- [x] **Step 3: Generate contact sheets and inspect every group**

Groups are block textures/models, effect icons, HUD, screens, advancement backgrounds, particles, mod icon, and animations. Record `pass`, `fixed`, or `intentional` with a concrete reason; no row may remain `unreviewed`.

- [x] **Step 4: Fix every proven asset issue and enforce the manifest in `check`**

Validate PNG signatures/dimensions/alpha, JSON references, animation frames/frametime, OGG Vorbis streams, translation coverage, model parents, blockstate variants, particle definitions, and advancement backgrounds.

- [x] **Step 5: Run audit/resource/build checks and commit**

Run: `python3 scripts/audit_non_item_assets.py --check && python3 scripts/validate_resources.py --root src/main/resources && ./gradlew check`

```bash
git add scripts docs/quality/asset-audit.md src/main/resources src/test/java/com/powers/BuildBaselineTest.java build.gradle
git commit -m "fix: verify every non-item asset"
```

### Task 6: Final Documentation and Release Verification

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Create: `docs/verification/2026-08-08-magic-quality-release.md`
- Modify: all three plans to check completed steps accurately

**Interfaces:**
- Consumes: current source/asset audits, generated interaction documents, tests, client/server logs, built JAR.
- Produces: requirement-by-requirement completion evidence and clean release artifact.

- [x] **Step 1: Run a fresh clean automated gate**

Run: `./gradlew clean test build`

Expected: `BUILD SUCCESSFUL`, all tests pass, magic docs and both audits have no drift, and resource validation passes.

- [x] **Step 2: Run isolated dedicated-server smoke**

Run with a new `mktemp -d` universe and `--port 0`; require the log to show POWERS version, 27 innate powers, 63 actions, 2,016 interactions, both 28-node rank graphs, all custom dimensions, `Done`, clean `stop`, and exit 0.

- [x] **Step 3: Run client resource/render smoke and inspect POWERS diagnostics**

Require successful resource reload with no missing model/texture/particle/sound, invalid animation, shader linkage, or POWERS rendering exception. Capture representative HUD/screen states when the Java game window is automatable; otherwise record the exact automated geometry/resource evidence without claiming screenshots.

- [x] **Step 4: Verify artifact, recipes, branch, and worktree**

Require one local branch named `main`, empty `git status --porcelain`, no crystal recipes, JAR forbidden-entry inspection pass, and record SHA-256/entry count.

- [x] **Step 5: Update documentation from actual behaviour and commit**

```bash
git add README.md CHANGELOG.md docs
git commit -m "docs: verify the complete magic quality release"
```

- [x] **Step 6: Recheck the final committed state**

Run: `git diff --check`, `git status --porcelain`, branch-count assertion, artifact hash assertion, and any check affected by documentation generation.

Expected: no output from status/diff checks and exactly one `main` branch.
