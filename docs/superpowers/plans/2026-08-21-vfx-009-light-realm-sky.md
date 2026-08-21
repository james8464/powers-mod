# VFX-009 Light Realm Sky Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render a distinctive ancient-white Light Realm sky through a narrow Sodium-safe vanilla boundary while retaining an unconditional static-white fallback.

**Architecture:** Pure immutable rules select none, static, normal enhanced, or reduced enhanced profiles. The client mixin keeps vanilla's white extracted state, stores a per-frame profile on its `SkyRenderer` instance, and delegates only additive untextured geometry to a lifecycle-owned GPU renderer; any unavailable/failing enhancement leaves the white disc untouched.

**Tech Stack:** Java 25, Minecraft 26.2, Fabric Loader/API, Sponge Mixin, Mojang render pipelines/GPU buffers, JUnit 5, Fabric client GameTests.

**Spec:** `.superpowers/sdd/2026-08-12-stages-1-8-completion/task-vfx-009-brief.md`

## Global Constraints

- Direct main at base `ac4ffb9ce00c2bd5db7b65f3d1157fd6917d45a8`; no push.
- No gameplay, server-authority, networking, save, travel, dimension-resource, fog-distance, or resource-pack changes.
- No Sodium imports/reflection, custom/core shaders, sky textures, optional rendering dependency, or outer `LevelRenderer` replacement.
- Keep `VFX-011` open and do not alter its accepted evidence except an independently required review correction.
- Real client output is the only visual authority; headless geometry tests remain structural evidence.
- Do not run expensive client/full GameTest gates while the protected QA-006 soak is active.

---

### Task 1: Pure mode and accessibility contract

**Files:**
- Create: `src/main/java/com/powers/visual/LightRealmSkyProfile.java`
- Create: `src/main/java/com/powers/visual/LightRealmSkyRules.java`
- Create: `src/test/java/com/powers/visual/LightRealmSkyRulesTest.java`

**Interfaces:**
- Produces: `LightRealmSkyRules.resolve(boolean lightRealm, boolean reducedMotion, boolean enhancedAvailable, double gameTime)`.
- Produces: immutable `LightRealmSkyProfile` with `Mode`, white base, bounded immutable layers, animation values, and explicit dependency flags.

- [ ] Write failing tests for `NONE`, exact `STATIC_WHITE`, normal enhanced, reduced enhanced, finite malformed time, immutable/bounded layers, and absence of textures/custom shaders.
- [ ] Run `./gradlew test --tests com.powers.visual.LightRealmSkyRulesTest --rerun-tasks --no-daemon` and retain the expected missing-contract RED.
- [ ] Implement the smallest pure records/rules satisfying the tests.
- [ ] Rerun the focused test and affected source audit GREEN.
- [ ] Commit the independently reviewable pure contract.

### Task 2: Narrow client render boundary and lifecycle

**Files:**
- Create: `src/client/java/com/powers/client/realm/LightRealmSkyRenderer.java`
- Create: `src/client/java/com/powers/client/realm/LightRealmSkyClientState.java`
- Modify: `src/client/java/com/powers/mixin/LightRealmSkyMixin.java`
- Create: `src/test/java/com/powers/client/visual/LightRealmSkyBoundaryTest.java`

**Interfaces:**
- Consumes: `LightRealmSkyProfile` from Task 1 and `FxAccessibility.reducedMotion(Minecraft)`.
- Produces: `LightRealmSkyRenderer.tryRender(profile)` and idempotent `close()`; one renderer/profile field per vanilla `SkyRenderer` instance.

- [ ] Write source/reachability tests requiring extraction, additive post-disc render, close cleanup, fail-closed circuit breaker, and static fallback while rejecting `LevelRenderer`, Sodium, texture, shader-resource, server, and per-frame mesh allocation coupling.
- [ ] Run focused tests and `compileClientJava` to observe RED.
- [ ] Build bounded untextured ring/radial meshes once, draw through a built-in translucent position-colour pipeline (not opaque `RenderPipelines.SKY`), and release buffers on `SkyRenderer.close()`.
- [ ] Preserve the current white state mutation before selecting/storing the immutable frame profile; log and circuit-break once on enhanced failure.
- [ ] Run focused tests, `compileJava`, `compileClientJava`, `compileGametestJava`, source audit, and resource validation GREEN.
- [ ] Commit the renderer boundary.

### Task 3: Real-client gallery and fallback proof

**Files:**
- Create: `src/gametest/java/com/powers/client/LightRealmSkyClientGameTests.java`
- Modify: `src/gametest/resources/fabric.mod.json`
- Create: `docs/verification/evidence/<date>-vfx-009/` bounded captures/metadata after execution

**Interfaces:**
- Consumes: production Light Crystal travel or an existing production-safe realm fixture and the renderer diagnostics seam.
- Produces: exact normal/reduced/fallback capture IDs and renderer-mode metadata.

- [ ] After QA-006 releases the host, write the client GameTest coverage contract RED for normal/reduced/fallback, render distances, weather state, and resource reload.
- [ ] Implement only test-fixture orchestration; do not add a shipping 10k/visual payload or fabricate renderer state.
- [ ] Run the isolated Java 25 client gallery, visually inspect full-resolution frames, and repair actual defects test-first.
- [ ] Repeat normal/reduced/fallback on the pinned Sodium 26.2 client with exact mod/options hashes.
- [ ] Record manual digest-bound verdicts; reject missing, stale, blank, or inferred decisions.
- [ ] Commit the integrated proof only after review.

### Task 4: Documentation, broad gates, and acceptance

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/verification/compatibility-matrix.md`
- Modify: `docs/planning/IMPROVEMENT_BACKLOG.md`
- Modify: `docs/superpowers/plans/2026-08-12-stages-1-8-completion.md`
- Modify: exact asset/source manifests generated by repository tasks

- [ ] Run focused tests, client compilation, resource/asset/source audits, complete client GameTests, ordinary GameTests, and literal Java 25 `check --rerun-tasks --no-daemon` after the soak.
- [ ] Verify dedicated-server boot/class loading is unchanged and no client/GPU class enters the main/server execution path.
- [ ] Document exact fallback/Sodium/resource-pack/reduced-motion results and limitations without calling structural tests visual proof.
- [ ] Remove VFX-009 only after every live matrix row and digest-bound review is accepted; leave VFX-011 governed by its independent pending aggregate.
- [ ] Stage exact VFX-009 files, run cached diff/privacy checks, commit cohesively, and bind evidence to the exact implementation SHA in a metadata successor.
