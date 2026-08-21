# VFX-011 Exact Visual Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace unsupported visual-quality claims with complete deterministic asset audits and exact-build Minecraft client render evidence.

**Architecture:** A test-classpath Java audit resolves every POWERS asset and generates deterministic frame/mip/composite sheets plus an immutable manifest. A client GameTest gallery renders the baked production models, HUD, bosses, entities, and screens; one reviewed evidence ledger binds every required view to exact screenshots and runtime settings.

**Tech Stack:** Java 25, JUnit 5, Fabric/Minecraft 26.2 client GameTests, `ImageIO`, Gson, Gradle verification tasks, Markdown/JSON evidence.

**Spec:** `.superpowers/sdd/2026-08-12-stages-1-8-completion/task-vfx-011-brief.md`

**Repair status (2026-08-21):** Tasks 1–5 implemented the audit surfaces, and the asset/two-client evidence remains valid. Independent review found the 971 gallery raw files and original client-emitted metadata were not retained, so every historical screenshot decision is pending and the contact pages are navigation-only. The agent/packager now require emitted raw digests/runtime settings and content-addressed raw retention. After QA-006 releases the host, rerun, explicitly review the retained raw set, and run the literal aggregate before closing. VFX-009 remains open.

## Global Constraints

- Minecraft/Fabric remains locked to the repository's 26.2 dependency set; add no rendering dependency.
- Existing registered IDs, item recipes, server authority, gameplay, collisions, and saves remain unchanged.
- Real client output is the rendering authority; headless output is structural/pixel evidence only.
- VFX-009 enhanced sky stays open and is not claimed by this unit.
- Observe RED before each implementation and preserve exact focused and aggregate logs.

---

### Task 1: Complete asset graph and structural contracts

**Files:**
- Create: `src/test/java/com/powers/client/visual/VfxAssetAudit.java`
- Create: `src/test/java/com/powers/client/visual/VfxAssetAuditTest.java`
- Modify: `build.gradle`
- Modify: `scripts/audit_non_item_assets.py`

**Interfaces:**
- Produces: `VfxAssetAudit.scan(Path root): AuditManifest`; `VfxAssetAudit.main(<root>, --check|--update)`; Gradle `verifyVfxAssetAudit` and `updateVfxAssetAudit`.
- `AuditManifest` records full SHA-256, category, dimensions/frame count, alpha statistics, resolved item/model/texture graph, geometry/UV result, display contexts, sheet page IDs, live-capture IDs, and explicit verdict/notes.

- [ ] Add fixture tests whose temporary assets fail for UV outside `[0,16]`, zero-area UV, `from/to` reversal, unresolved/cyclic `#texture` variables, malformed/non-finite display triples, unknown model references, and missing required spawn-egg contexts.
- [ ] Run `./gradlew test --tests 'com.powers.client.visual.VfxAssetAuditTest'` and record the missing-owner RED.
- [ ] Implement recursive item/model/texture resolution, finite/schema validation, complete asset classification, deterministic ordering, and explicit reviewed exception loading; do not use artistic magnitude thresholds.
- [ ] Run the focused test and `validatePowerResources`; require GREEN.
- [ ] Change the historical audit wording from automatic “reviewed/pass” to integrity-only language and add a regression test preventing auto-PASS claims.

### Task 2: Alpha-correct frame and mip audit

**Files:**
- Create: `src/test/java/com/powers/client/visual/VfxPixelAudit.java`
- Create: `src/test/java/com/powers/client/visual/VfxPixelAuditTest.java`
- Create: `docs/quality/vfx-011-reviewed-exceptions.json`

**Interfaces:**
- Produces: `VfxPixelAudit.inspect(BufferedImage, FrameLayout): PixelEvidence`; `buildMipChain` using premultiplied-alpha box filtering; deterministic light/dark/checker composite pages.
- Consumes animation layout from `AuditManifest`; returns per-frame/per-mip alpha and edge-contamination measurements.

- [ ] Add RED fixtures for transparent-edge colour contamination, cross-frame mip bleed, incorrect animation frame bounds, missing mip pages, and an explicit reviewed exception that is path- and digest-bound.
- [ ] Implement per-frame mip generation through 1×1, transparent-edge diagnostics, page generation that never crops frames, and digest-bound exception handling.
- [ ] Add ownership checks rejecting stale, missing, and extra generated pages and mismatched hashes.
- [ ] Run focused pixel tests twice and compare page/manifest digests for determinism.

### Task 3: Truthful HUD, screen, and historical golden contracts

**Files:**
- Modify: `src/test/java/com/powers/client/visual/VisualGoldenHarness.java`
- Modify: `src/test/java/com/powers/client/visual/HudCombinationMatrixTest.java`
- Create: `src/test/java/com/powers/client/visual/VfxCoverageContractTest.java`
- Modify: `docs/verification/goldens/manifest.json`
- Modify: `docs/verification/final-requirement-matrix.md`

**Interfaces:**
- Produces actual scaled logical dimensions via `HudCase.physicalWidth/guiScale`; every counted case has a unique serialized case key.
- `VfxCoverageContractTest` rejects raw textures or synthetic sky images cited as renderer proof.

- [ ] Add RED tests showing the existing GUI-scale loop produces duplicate geometry, reduced motion is never applied, raw screen backgrounds/skin crops/egg textures are not rendered proof, and the claimed 160/8,192 counts are stale or duplicated.
- [ ] Apply GUI scale to HUD layout inputs, prove distinct case keys and outputs, and enumerate five energy rows × 21 half-unit values plus vanilla heart/armour/air/mount/spectator combinations.
- [ ] Remove unsupported reduced-motion and synthetic Light-sky claims; retain static fallback only when a real client capture ID exists.
- [ ] Regenerate historical goldens only for claims they can truthfully support, run `verifyVisualGoldens`, and inspect changed pages.

### Task 4: Production renderer gallery for models, transforms, and UVs

**Files:**
- Create: `src/client/java/com/powers/client/acceptance/VfxGalleryClientAgent.java`
- Create: `src/gametest/java/com/powers/client/VfxGalleryClientGameTests.java`
- Create: `src/gametest/java/com/powers/gametest/VfxGalleryGameTests.java`
- Create: `src/gametest/resources/data/powers/test_environment/vfx_gallery.json`
- Modify: `src/gametest/resources/fabric.mod.json`

**Interfaces:**
- Server gallery publishes bounded page descriptors and stable camera/entity/item state.
- Client gallery records `captureId`, GUI scale, mip level, reduced-motion value, window size, camera, render family, and source keys before each screenshot.

- [ ] Add compile-RED client tests for missing gallery routes and coverage IDs.
- [ ] Render every spawn egg and every custom `display` model in all defined `ItemDisplayContext` values; render representative structural families for inherited defaults.
- [ ] Render actual front/back/left/right entity models and representative poses/equipment for Heralds, First Vessel, Radiant Sentinel, Darkness Creature/test actor, plus Shadow/Echo wide and slim owner skins with overlays.
- [ ] Capture normal/reduced-motion at mip levels 0–4 with fixed camera/time/weather and light/dark/checker gallery backgrounds; assert every manifest-required capture ID was emitted once.
- [ ] Run the focused server gallery and integrated client suite; inspect every page and record repairs instead of auto-accepting.

### Task 5: Actual HUD, boss-bar, and screen matrix

**Files:**
- Modify: `src/client/java/com/powers/client/acceptance/VfxGalleryClientAgent.java`
- Modify: `src/gametest/java/com/powers/client/VfxGalleryClientGameTests.java`
- Modify: `src/gametest/java/com/powers/gametest/VfxGalleryGameTests.java`

**Interfaces:**
- Produces real captures for GUI scales 1–4, narrow/wide windows, hover/selected/empty/error/long-text states, and normal/reduced-motion.
- Coverage IDs include `hud/<state>`, `boss/<entity>/<phase>`, and `screen/<surface>/<state>/<scale>`.

- [ ] Add RED coverage tests for every custom screen: Teleport, Locator, both Rank Mazes, Power Selection, Artifact Catalogue, Shadow Sword, Grimoire Index, Rainbow Convergence, Reservoir Transfer, Arcane Crucible, and advancement roots.
- [ ] Capture actual energy rows/half units, slots/cooldowns, Shadow and realm/astral indicators, extra hearts/armour/air/mount/spectator combinations, and reduced motion through the Minecraft HUD pipeline.
- [ ] Capture Light Herald, Dark Herald, and First Vessel boss bars at representative progress/phase/channel states including released colour/notch/darken/text behavior.
- [ ] Capture every screen at scales 1–4 with actual widgets, text, clipping, scrolling, tooltips, focus, and required state variants; assert no missing/duplicate IDs.
- [ ] Run and visually inspect the client matrix; repairs require a new focused capture and digest.

### Task 6: Durable evidence and release gates

**Files:**
- Create: `docs/quality/vfx-011-asset-audit.json`
- Create: `docs/verification/evidence/2026-08-21-vfx-011/README.md`
- Create: `docs/verification/evidence/2026-08-21-vfx-011/review-ledger.tsv`
- Create: `docs/verification/evidence/2026-08-21-vfx-011/SHA256SUMS`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/planning/IMPROVEMENT_BACKLOG.md`
- Modify: `docs/superpowers/plans/2026-08-12-stages-1-8-completion.md`

**Interfaces:**
- `review-ledger.tsv` maps every source/page/capture to PASS/REPAIRED/LIMITED with named notes; no blank or inferred verdict.
- `SHA256SUMS` binds sanitized options, logs, sheets, client captures, manifest, and ledger to the exact implementation commit/JAR.

- [ ] Run `updateVfxAssetAudit`, inspect all generated pages, populate explicit verdicts, then run `verifyVfxAssetAudit` to reject any stale/extra output.
- [ ] Record exact commit/JAR, MC/Fabric/Java/GPU, mods/resource packs, options, commands, resource/atlas logs, and all hashes; sanitize identities without dropping warning/error lines.
- [ ] Run focused JVM/Python/resource tests, gallery GameTests, integrated client captures, full required GameTests, and `./gradlew check --rerun-tasks --no-daemon`; record exact counts and exit status.
- [ ] Update README/changelog, check VFX-011 and remove its backlog row only after every required gate and ledger row is accepted; keep VFX-009 open.
- [ ] Stage only the VFX-011 files, run `git diff --cached --check`, commit cohesively, bind the evidence to the implementation SHA in a metadata successor, and request independent review.
