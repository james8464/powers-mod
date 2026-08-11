# Repository Hygiene and Live Acceptance Implementation Plan

> Execute in order with test-first changes and independently reviewable commits.

**Goal:** Make the completed POWERS mod smaller, clearer, reproducible to launch, and demonstrably stable under live Minecraft and multiplayer-style workloads.

**Architecture:** Keep `PowersMod` as a thin Fabric facade. Move registration, lifecycle, runtime cleanup, and player tick policy into focused package collaborators. Add source/resource reachability gates and a gameplay acceptance catalogue, then use JUnit, Fabric GameTests, dedicated-server boot, client bootstrap, and soak telemetry as complementary proof.

**Stack:** Java 25, Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Fabric Loom 1.17.19, Gradle 9.5.1, JUnit 6.

---

## Task 1: Lock the maintenance contracts

**Files:**
- Create: `src/test/java/com/powers/quality/ProductionReachabilityTest.java`
- Create: `src/test/java/com/powers/quality/BootstrapArchitectureTest.java`
- Modify: `src/test/java/com/powers/quality/SourceAudit.java`
- Modify: `src/test/java/com/powers/quality/SourceQualityTest.java`

1. Add a failing reachability test that reports top-level production types referenced only by their own source.
2. Add a failing architecture test requiring the Fabric entrypoint to delegate registration/lifecycle work and stay below its reviewed size.
3. Run only the new tests and record the expected orphan/entrypoint failures.
4. Extend the existing source audit only where the new tests expose false positives, keeping reflective Fabric entrypoints and mixins visible through resource references.

## Task 2: Repair the launch and GameTest harness

**Files:**
- Create: `src/test/java/com/powers/quality/LauncherContractTest.java`
- Modify: `test.sh`
- Modify: `build.gradle`
- Modify: `README.md`

1. Add failing tests for explicit Java override, fallback discovery, stable project-root execution, and supported modes.
2. Replace the missing Homebrew-only default with a Java 25 resolver.
3. Add `check`, `gametest`, `server`, `client`, and `soak` modes while preserving the original client/server interface.
4. Seed GameTest EULA/server properties in the task working directory before launch.
5. Run launcher tests, a clean check, and GameTests; reject missing-runtime or missing-properties error telemetry.

## Task 3: Remove proven dead code and stale planning clutter

**Files:**
- Delete only reachability-test-confirmed orphan production types and their orphan-only tests.
- Create: `docs/development/history.md`
- Delete: superseded files under `docs/superpowers/plans/` and `docs/superpowers/specs/`, retaining this current design and plan until completion.
- Modify: `README.md`

1. Confirm every candidate with production/resource/build references and Git history.
2. Delete confirmed orphans and rerun compilation plus reachability tests.
3. Summarize historical architectural decisions and point to Git history, then remove superseded internal planning files.
4. Verify no README or current verification document links to a removed file.

## Task 4: Extract focused bootstrap collaborators

**Files:**
- Create: `src/main/java/com/powers/PowersBootstrap.java`
- Create: `src/main/java/com/powers/PowersServerLifecycle.java`
- Create: `src/main/java/com/powers/PlayerPowerTicker.java`
- Create or modify focused lifecycle tests under `src/test/java/com/powers/`
- Modify: `src/main/java/com/powers/PowersMod.java`
- Modify: `src/main/java/com/powers/PlayerTickCoordinator.java`

1. Keep the architecture test red while introducing registration-only bootstrap orchestration.
2. Move join, respawn, disconnect, server stop, and server tick wiring without changing callback order.
3. Move per-player realm mode, toggle, energy, exhaustion, and backlash policy into `PlayerPowerTicker`.
4. Keep public scheduling/storm/id facades compatible, then make the architecture test green.
5. Run all player, realm, power, lifecycle, and GameTest suites.

## Task 5: Audit historical crashes and null/lifecycle boundaries

**Files:**
- Modify or create focused tests beside each affected subsystem.
- Modify production files only after reproducing a current failure.
- Create: `docs/verification/2026-08-11-maintenance-findings.md`

1. Parse every ignored crash report and unique error stack from development logs.
2. Match each stack to current source/history and classify fixed, environment-only, or reproducible.
3. For each reproducible defect, write the smallest failing unit/GameTest, trace the root cause, implement one fix, and run the affected suite.
4. Record why old fireball, asset, auth, and server-property errors are or are not current mod failures.

## Task 6: Complete the live gameplay acceptance catalogue

**Files:**
- Create: `src/main/java/com/powers/testing/GameplayAcceptanceCatalogue.java`
- Create: `src/test/java/com/powers/testing/GameplayAcceptanceCatalogueTest.java`
- Modify: `src/gametest/java/com/powers/gametest/PowersGameTests.java`
- Modify: `src/main/java/com/powers/command/TestingCommand.java`
- Modify: `README.md`

1. Add a failing catalogue test requiring every registered innate, spell, crystal, artifact route, realm, mob, and critical system family to have an acceptance identifier and proof type.
2. Implement immutable catalogue entries mapped to unit, live, visual/resource, or soak evidence.
3. Add missing live smoke tests for command tree, light-crystal travel, ordinary block impact fireball safety, actor targeting, and lifecycle cleanup.
4. Add an operator-only testing arena command that creates bounded targets/markers without persistent entities or forced chunks.
5. Run focused GameTests and verify arena cleanup.

## Task 7: Profile and harden tick work

**Files:**
- Modify: `src/test/java/com/powers/performance/SyntheticMultiplayerSoakTest.java`
- Modify: `src/main/java/com/powers/diagnostics/ServerRuntimeMetrics.java`
- Modify production managers only when a failing budget test demonstrates unbounded work.
- Modify: `docs/verification/2026-08-11-maintenance-findings.md`

1. Expand deterministic 10/50/100-player scenarios across forces, presences, wards, fields, proxies, guardians, named entities, packets, and particles.
2. Fail on super-linear or undocumented per-tick work.
3. Run live `/powers diagnose` snapshots before, during, and after a bounded stress arena.
4. Fix only measured leaks/scans, preserving current gameplay caps.

## Task 8: Resource, HUD, and client presentation QA

**Files:**
- Modify focused resource/HUD tests only where coverage is missing.
- Modify assets or render code only after a failing geometry/resource check or captured client defect.
- Modify: `docs/verification/2026-08-11-maintenance-findings.md`

1. Validate JSON, texture dimensions/alpha, model references, animation frames, translations, spawn eggs, entity skin geometry, rank panels, and advancement backgrounds.
2. Verify ten energy symbols align above hunger across GUI scales and conditional vanilla HUD rows through deterministic layout tests.
3. Launch the development client, load resources and atlases, inspect accessible UI where possible, and scan logs for POWERS-caused errors.
4. Record any environment limitation without converting it into a visual proof claim.

## Task 9: Final verification, documentation, and Git publication

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `README.md`
- Modify: `docs/quality/code-audit.md`
- Modify: `docs/quality/asset-audit.md` if assets changed.
- Modify: `docs/verification/2026-08-11-maintenance-findings.md`

1. Regenerate item/magic documentation and exact source/asset manifests.
2. Run a clean full build/check, all JUnit tests, all Fabric GameTests, dedicated-server boot/clean stop, client bootstrap, resource validation, test-layout audit with project-specific interpretation, and soak tests.
3. Confirm no current POWERS crash/error telemetry and no tracked/generated drift.
4. Review the full diff, ensure the worktree is clean after commits, push `codex/powers-finalisation`, and verify it matches the remote branch.
