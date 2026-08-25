# Selected Stages 1–8 Completion Plan

> Execute directly on `main`, one accepted work unit at a time. Each behavioural unit requires observed RED, focused GREEN, a production-entrypoint GameTest, affected regression proof, documentation/evidence, backlog removal, and a cohesive commit.

## Scope and order

The selected programme contains 72 backlog rows. Work proceeds strictly from Stage 1 through Stage 8; Stage 9/P3 work is excluded. `QA-001` opens in Stage 1 and closes only after the final Stage 8 release-candidate commit.

The existing server-authoritative action, protection, persistence, spatial-index, semantic-FX, and generated-document boundaries remain authoritative. New work extends those owners rather than creating parallel state.

Locked invariants:

- Preserve deferred crystal/story-item recipes and stable registered IDs.
- Preserve the ten-symbol vanilla-style energy HUD directly above hunger.
- Preserve server-authoritative casting, realm confinement, vulnerable frozen bodies, artifact-only scaling policy, and rank-10 Shadow Sword cooldown removal.
- Preserve completed ranks and raw progression during migrations.
- Excluded work does not authorise removal of already-working behaviour.
- Never treat harness existence, a hash audit, or an unasserted command as gameplay proof.

## Preflight recovery

- [x] Preserve the original dirty worktree and QA evidence in `.git/codex-backups/20260813-stage-preflight/`.
- [x] Remove 80,496 mechanically inserted generic comments while retaining intentional Ward/QA changes.
- [x] Stop the orphaned development server cleanly.
- [x] Fix Ward-Breaking so a suppressed powered amethyst ward is not simultaneously treated as natural amethyst.
- [x] Prove the Ward fix fails when reverted and passes when restored.
- [x] Split oversized mixed responsibilities from `PlayerPowers` and `CelestialRuinManager`.
- [x] Prevent opt-in connected profiling metadata from leaking into ordinary GameTests.
- [x] Reset only the generated GameTest world before each run.
- [x] Pass 1,381 deterministic tests and all generated/resource audits.
- [x] Pass all 76 ordinary live GameTests from a fresh world.

## Selected work ledger

### Stage 1 — release evidence and immediate stabilisation

- [ ] `QA-001`: exact-build signed release envelope; close last.
- [x] `PERF-001`: rerun 10/50/100-player 36,000-tick and 1,800-second profiles; distinguish real clients from embedded server actors.
- [x] `QA-005`: complete every generated manual acceptance row on one exact build with four Fabric clients where relevant.
- [x] `QA-006`: complete a restart/reconnect soak for 24 uninterrupted hours; any failure restarts acceptance after repair.
- [x] `PRG-001`: gather ten Light and ten Darkness human-cadence samples and publish median/p90 for all thresholds.
- [x] `PERF-005`: observer/dimension/chunk/action/phase visual coalescing, with at least 25% fewer packets/bytes and unchanged collisions.
- [x] `PERF-006`: JFR-directed geometry/payload allocation reduction, at least 20% lower allocation and no greater than 5% p99 regression.

Stage 2 may start only after every row above except the deliberately open `QA-001` envelope is accepted.

### Stage 2 — correctness, performance, networking, and infrastructure

- [x] `COR-020`: delayed work stores stable identity/deadline/cancellation ownership, never stale entity/level references.
- [x] `PERF-012`: shared read-only per-level/chunk/tick perception snapshots; at least 30% fewer mixed-AI inspections.
- [x] `PERF-014`: compact long-lived summon persistence and exactly-once derived-index rebuild.
- [x] `PERF-016`: catastrophic load crossed the starvation gate; fair dimension/protection-policy scheduling now bounds validation and transformation work.
- [x] `PERF-015`: exact framed/compressed 64 B–8 KiB measurements now choose ordered semantic-FX batching only when it is smaller.
- [x] `PERF-013`: recognisable near/mid/far semantic-FX LOD without simplifying event geometry or identity.
- [x] `NET-007`: deterministic global → world → dimension policy resolution with absolute protection denial and diagnosed source.
- [x] `NET-009`: versioned `com.powers.api.v1` integration API plus compiled example/compatibility mod.
- [x] `NET-010`: atomic revisioned action reload, captured active casts, stale menu rejection, and alias migration.
- [x] `NET-011`: pinned 26.2 compatibility matrix for Sodium, Lithium, Simple Voice Chat, ClaimMod, and Inventory Extended/CompactStorage.
- [x] `QA-009`: testing-only mod-packet delay/loss/duplication/reorder injector and convergence proof.
- [x] `QA-010`: live hostile-environment fixtures for claims, borders, ceilings, void, fluids, mounts/passengers, portals, and a synthetic dimension.
- [x] `QA-016`: intent/invariant comments and stronger source-quality rejection of noise, stale TODOs, misleading claims, undocumented public contracts, and mixed-responsibility classes.

### Stage 3 — UI and magical presentation

- [x] `PERF-010` + `UX-004`: one fixed-widget virtual scrolling catalogue with search, filters, favourites, recents, direct binding, stable revisions, narration, and a 10,000-action fixture.
- [x] `VFX-011`: accepted exact-build successor retains all 971 raw gallery PNGs and original emitted metadata, binds 2,080 explicit review decisions, and passes the finalized-head literal aggregate (131 GameTests, 1,680 JUnit tests, and 143 Python tests); historical provisional evidence remains preserved.
- [x] `VFX-009`: dedicated ancient-white Light Realm sky renderer with Sodium-safe boundary and static fallback; full implementation/evidence commit `e78bf8f01fc79d6e05838c083685d348c214502c` binds digest-bound Java 25 Fabric and Sodium upward-camera normal/reduced/fallback galleries covering distances, reload, and rain-command observation. Normal and reduced silhouettes are visibly distinct from the fallback; the no-skylight client observed `weather=clear` after the successful rain command, so this does not assert rendered rain. The Sodium JAR remains external to Git.
- [ ] `VFX-004`: protected, no-drop, reversible material-aware scar service under hard budgets.
- [ ] `VFX-005`: unique long-distance silhouettes for every rank-10 transformation.
- [ ] `VFX-006`: synchronised bounded casting poses for player-like magical entities without a heavy animation dependency.
- [ ] `VFX-007`: authored near/mid/far sound layers, obstruction falloff, subtitles, mixing, and reduced-tinnitus support.

### Stage 4 — cross-system interactions

- [ ] `INT-008`: lease-based temporal ownership over vanilla tick freeze; preserve external freeze and distinguish frozen/control clocks.
- [ ] `INT-009`: one authoritative mind-session owner and complete death/return/confinement matrix.
- [ ] `INT-010`: data-driven allegiance across players, Shadow, artifacts, forces, bosses, food, amethyst, and parties.
- [ ] `INT-011`: forcefield → Soul Link → health damage ordering with overkill sacrifice and numerical debug accounting.
- [ ] `INT-007`: finite thermal transition table reused by every fire/ice/water/snow/plant/realm interaction.
- [ ] `INT-012`: bounded Dimensional Anchor stabilisation for local bodies and legal existing portals/gates only.
- [ ] `INT-014`: readable, optional Herald/First Vessel reactions to opposing artifacts and complementary casts.

### Stage 5 — selected innate-power improvements

- [ ] `PWR-004`: safe vanilla-scale transitions, collision preview/fallback, mount rejection, reach/drain HUD, and mass knockback.
- [ ] `PWR-006`: server-authoritative momentum flight, braking, water transition, sonic presentation, tilt, and latency proof without creative flags.
- [ ] `PWR-011`: destructive directional Thunderclap at every rank under protection and terrain budgets.
- [ ] `PWR-014`: discrete authoritative Energy Beam damage with interpolated visuals and temporary scorching.
- [ ] `PWR-015`: client-only comfort FOV/camera controls and reduced-motion wake LOD.
- [ ] `PWR-022`: Astral body direction/dimension/distance indicator only; never force-load.
- [ ] `PWR-023`: interruptible visible Energy Drain tether with boss-percent and absolute conversion caps.
- [ ] `PWR-024`: safe Ice melt/bridge/brittle-armour lifecycle through the shared thermal table.

### Stage 6 — selected spells and crystals

- [ ] `SPL-004`: loaded-state Augury forecasts with explicit uncertainty.
- [ ] `SPL-005`: temporary Cartographer breadcrumbs and a persistent discovered-site journal without forced search loading.
- [ ] `SPL-007`: bounded 6,000-block Celestial Ruin atmosphere, flash/ringing, fallout, damage falloff, and distant scars.
- [ ] `SPL-009`: bounded recent Blood Reading facts without hidden/private data.
- [ ] `SPL-011`: energy-only cleanse/link-sever/corruption-relief modes; Amethyst Poisoning remains non-cleansable.
- [ ] `CRY-003`: persistent per-player crystal-mode discovery and legacy migration.
- [ ] `CRY-006`: atomically preflighted datapack blueprints plus the approved entity-/loot-stripped vanilla templates.
- [ ] `CRY-007`: Yellow Size Shift reuses the common safe-scale service and exact mount rejection.

### Stage 7 — progression, artifacts, and Shadow

- [ ] `PRG-003`: deterministic once-per-encounter boss contribution credit for meaningful damage/support/control.
- [ ] `PRG-004`: all 56 nodes have an executable named numerical mechanic and generated profile proof.
- [ ] `PRG-009`: runestones stack to 64, have no cooldown, and are consumed only on a successful energy restore.
- [ ] `ART-003`: bounded strongest-per-school ring/amulet attunements.
- [ ] `ART-006`: exactly one villager heart drop; alignment-specific eating; strongest-heart-only passive; distinct relic presentation.
- [ ] `ART-014`: Light/Dark models and animations for all six named conversion weapons and every item transform.
- [ ] `ART-016`: tag-first food affinity; unknown third-party food is neutral.
- [ ] `ART-020`: only top-level authorised ownership powers mythics; nested/unknown containers fail closed.
- [ ] `SHD-011`: generated 100% Shadow knowledge coverage with source links.
- [ ] `SHD-013`: bounded persistent three-phase Darkness agenda that never lies about mechanics or safety.
- [ ] `SHD-008`: inspectable combat roles with absolute safety/energy/amethyst/work-budget rules.
- [ ] `SHD-009`: capped owner-local explicit feedback learning with reset and immutable safety rules.
- [ ] `SHD-010`: at most 16 named owner places; loaded-only navigation and no forced chunks.
- [ ] `SHD-014`: throttled private/global warnings for intrusion, danger, and combat state.
- [ ] `SHD-016`: default-off redacted localhost Ollama dialogue research; deterministic code remains sole action authority.

### Stage 8 — selected realm and boss expansion

- [ ] `WRLD-008` + `MOB-006`: persistent three-phase Herald Courts with reversible hazards, capped summons, scaling, loot, and unconditional recovery exits.
- [ ] `WRLD-015`: bounded Middleworld libraries, roads, dream weather, and one lightweight neutral Archivist per loaded outpost; no teleport hub.
- [ ] `MOB-007`: accessible wind-up for every lethal First Vessel action and phase/channel boss-bar text.
- [ ] `MOB-014`: generated every-action response matrix against both Heralds and First Vessel, plus batched live execution.
- [ ] `MOB-015`: configurable player-like testing actors while reserving truly player-only flows for connected clients.

## Decisions ledger

These rows are intentionally excluded, not accidentally omitted:

- Stage 1: `VFX-003`.
- Stage 2: `COR-018`, `PERF-011`, `PERF-017`, `QA-015`.
- Stage 3: `UX-007`, `UX-008`, `UX-009`, `VFX-010`.
- Stage 4: `INT-006`, `INT-013`.
- Stage 5: `PWR-005`, `PWR-007`, `PWR-008`, `PWR-012`, `PWR-013`, `PWR-016`, `PWR-017`, `PWR-021`, `PWR-025`.
- Stage 6: `SPL-003`, `SPL-012`, `SPL-013`, `SPL-014`, `CRY-004`, `CRY-005`, `CRY-008`, `CRY-010`, `CRY-011`, `CRY-014`.
- Stage 7: `PRG-005`, `PRG-006`, `PRG-008`, `PRG-010`, `ART-007`, `ART-009`, `ART-012`, `ART-013`, `ART-015`, `SHD-006`, `SHD-007`, `SHD-020`.
- Stage 8: `WRLD-003`–`WRLD-007`, `WRLD-009`–`WRLD-014`, `MOB-003`, `MOB-004`, `MOB-005`, `MOB-008`. `WRLD-006` is explicitly declined because a persistent NPC schedule subsystem is disproportionate.
- Stage 9/P3: all rows.

Interpretations fixed by the owner:

- Nonexistent `PERF-103` means `PERF-013`.
- `PWR-022` means the Astral return indicator only.
- `PRG-009` is replaced by consumable single-use runestones.
- `PERF-012`, `NET-011`, `PWR-004`, `PWR-015`, and `INT-014` are in scope.
- `PERF-016` uses the measurement gate above; a below-threshold result closes it as measured-and-declined.

## Per-unit protocol

1. Re-read the authoritative backlog row and inspect its production state.
2. Add the smallest deterministic failing test and observe the expected RED.
3. Implement through the current authoritative owner.
4. Run focused GREEN and affected deterministic suites.
5. Exercise the production entrypoint in an isolated Fabric GameTest, then run the complete live suite.
6. Use actual clients for UI, rendering, sound, networking, multiplayer, and manual behaviour.
7. Update README, catalogues, migration notes, changelog, interaction docs, audit manifests, and exact evidence.
8. Remove the backlog row only after its acceptance proof exists.
9. Commit the cohesive unit on `main`; push only after its stage gate is green.

## Final acceptance

- [ ] Regenerate the complete `QA-005` checklist on the final commit.
- [ ] Rerun final 10/50/100-player 30-minute profiles and the complete 24-hour restart soak.
- [ ] Pass `./gradlew clean check pitest verifyScreenshots verifyVisualGoldens saveMigrationCorpus syntheticSoak --rerun-tasks --no-daemon`.
- [ ] Pass complete Fabric server/client GameTests, dedicated-server reload/save/restart, compatibility, packet-fault, and four-client campaign gates.
- [ ] Verify asset, sound, resource, documentation, migration, source-quality, and exact-audit manifests.
- [ ] Build the final JAR/report, publish GitHub Actions provenance with `actions/attest@v4`, and verify it using `gh attestation verify`.
- [ ] Confirm only `main` exists, the worktree is clean, local/remote SHAs match, and GitHub Actions is green.
- [ ] Remove `QA-001` only after every statement above is proven on the same final commit.
