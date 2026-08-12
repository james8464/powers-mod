# Stages 1–8 Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` and complete one acceptance row at a time. Every behavioral change follows RED → GREEN → focused live Minecraft proof → affected suite → commit.

**Goal:** Fully implement and prove stages 1–8 of the ordered `docs/planning/IMPROVEMENT_BACKLOG.md` delivery programme while leaving every Stage 9/P3 expansion untouched.

**Architecture:** Preserve the existing server-authoritative action, protection, persistence, spatial-index, semantic-FX, and generated-document boundaries. Add the smallest focused rule/runtime surface needed for each acceptance condition; compose related duplicate rows into one implementation only when they share the same authoritative state owner (notably `PERF-010`/`UX-004`).

**Tech stack:** Minecraft Java Edition 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25, Gradle/Fabric Loom, JUnit 6, Fabric GameTest, deterministic screenshot contracts, JFR, Python validation harnesses.

## Global constraints

- Do not implement Stage 9/P3 rows.
- Do not add recipes for crystals or other deliberately deferred story items.
- Keep `main` as the sole branch and preserve save-compatible registrations/aliases.
- No rank scaling for spells/crystals; artifacts use only their explicit policies.
- Realm confinement, protection precedence, frozen-body vulnerability, ten-symbol energy HUD, and rank-10 Shadow Sword cooldown removal remain locked.
- Every completion claim must satisfy the row's own acceptance condition; harness presence is not execution evidence.
- Automated embedded connections are valid for repeatable multiplayer behavior, but human/visual acceptance rows require actual client observation and recorded evidence.
- Update README/generated catalogues, migration notes, interaction documentation, manual ledger, and audit manifests in the same cohesive commit.

## Execution ledger

### Stage 1 — release evidence and immediate stabilisation

- [ ] `QA-001`: prepare the signed release envelope; close it only after every other Stage 1 row and the final repository gate passes.
- [ ] `PERF-001`: run 10/50/100 connected-player profiles for 30 minutes each; publish JFR, p95/p99, limits, revision, and environment.
- [ ] `QA-005`: execute and sign every generated manual action/item/entity/screen/command row against one exact build.
- [ ] `QA-006`: run and inspect the full 24-hour forced-restart soak.
- [ ] `PRG-001`: gather/publish real multiplayer quest telemetry for all 20 alignment/level rows and apply justified, migrated threshold changes if evidence requires them.
- [ ] `PERF-005`: capture live before/after packet count and encoded bytes with collision equivalence.
- [ ] `PERF-006`: capture live before/after allocation profiles and prove materially lower young-generation churn.
- [ ] `VFX-003`: record/review Lightning and Fireball first-person captures at every rank and correct any aim-obscuring presentation.

### Stage 2 — correctness, performance, networking, and test infrastructure

- [ ] `COR-020`, `COR-018`.
- [ ] `PERF-011`, `PERF-012`, `PERF-014`, `PERF-016`, `PERF-017`, `PERF-015`, `PERF-013`.
- [ ] `NET-007`, `NET-010`, `NET-011`, `NET-009`.
- [ ] `QA-009`, `QA-010`, `QA-016`, `QA-015`.

### Stage 3 — UI, accessibility, visuals, and audio

- [ ] `PERF-010` and `UX-004` as one virtualised catalogue implementation.
- [ ] `UX-007`, `UX-008`, `UX-009`.
- [ ] `VFX-011`, `VFX-009`, `VFX-004`, `VFX-005`, `VFX-006`, `VFX-007`, `VFX-010`.

### Stage 4 — cross-system interactions and counterplay

- [ ] `INT-008`, `INT-009`, `INT-010`, `INT-011`, `INT-006`, `INT-007`, `INT-012`, `INT-013`, `INT-014`.

### Stage 5 — innate-power depth

- [ ] `PWR-004`, `PWR-005`, `PWR-006`, `PWR-007`, `PWR-008`, `PWR-011`, `PWR-012`, `PWR-013`, `PWR-014`, `PWR-015`, `PWR-016`, `PWR-017`, `PWR-021`, `PWR-022`, `PWR-023`, `PWR-024`, `PWR-025`.

### Stage 6 — grimoire and crystal depth

- [ ] `SPL-003`, `SPL-004`, `SPL-005`, `SPL-007`, `SPL-009`, `SPL-011`, `SPL-012`, `SPL-013`, `SPL-014`.
- [ ] `CRY-003`, `CRY-004`, `CRY-005`, `CRY-006`, `CRY-007`, `CRY-008`, `CRY-010`, `CRY-011`, `CRY-014`.

### Stage 7 — progression, artifacts, Crucible, and Shadow

- [ ] `PRG-003`, `PRG-004`, `PRG-005`, `PRG-006`, `PRG-008`, `PRG-009`, `PRG-010`.
- [ ] `ART-003`, `ART-006`, `ART-007`, `ART-009`, `ART-012`, `ART-013`, `ART-014`, `ART-015`, `ART-016`, `ART-020`.
- [ ] `SHD-011`, `SHD-013`, `SHD-008`, `SHD-009`, `SHD-010`, `SHD-006`, `SHD-007`, `SHD-014`, `SHD-020`, `SHD-016`.

### Stage 8 — realms, factions, structures, mobs, and bosses

- [ ] `WRLD-003`, `WRLD-004`, `WRLD-005`, `WRLD-006`, `WRLD-007`, `WRLD-008`, `WRLD-009`, `WRLD-010`, `WRLD-011`, `WRLD-012`, `WRLD-013`, `WRLD-014`, `WRLD-015`.
- [ ] `MOB-003`, `MOB-004`, `MOB-005`, `MOB-006`, `MOB-007`, `MOB-008`, `MOB-014`, `MOB-015`.

## Per-row execution protocol

1. Re-read the authoritative backlog row and inspect production/runtime evidence.
2. Write the smallest failing unit/contract test for each pure invariant and run it to the expected RED.
3. Implement the smallest cohesive production change; do not introduce parallel state owners.
4. Run focused GREEN tests and affected regression suites.
5. Add or extend a Fabric GameTest that reaches the real server-authoritative entrypoint; run it alone, then in the full live suite.
6. For client/visual work, run the actual development client, interact through the GUI, capture/review evidence, then run visual/resource contracts.
7. For profiling/long-duration rows, execute the specified wall-clock run and inspect every generated report; do not replace duration with simulated ticks.
8. Update documentation and exact audit manifests, run `git diff --check`, and commit the accepted row/cohesive pair.
9. Remove the row from `IMPROVEMENT_BACKLOG.md` only after its acceptance evidence exists and is linked from the acceptance ledger.
10. Move to the next row only after fresh proof is green.

## Final acceptance

- [ ] Reconcile every Stage 1–8 ID against code, live evidence, documentation, and backlog removal.
- [ ] Run `./gradlew clean check pitest verifyScreenshots verifyVisualGoldens saveMigrationCorpus syntheticSoak --rerun-tasks --no-daemon`.
- [ ] Run the complete Fabric GameTest and client GameTest suites.
- [ ] Boot a dedicated server, reload resources/datapacks, save, stop, and inspect logs.
- [ ] Run the final connected-player performance scenarios and restart-soak leak check on the release commit.
- [ ] Complete the signed manual playthrough on the release commit.
- [ ] Confirm only `main` exists, the worktree is clean, local/remote commit IDs match, and GitHub Actions is green.
