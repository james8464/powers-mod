# Performance, Proof, and Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove every advertised behavior, eliminate ordinary global scans, synchronize all documentation/assets, and produce a clean launchable branch.

**Architecture:** Lifecycle-owned chunk indexes and bounded rotating queues replace whole-world scans. A generated requirement matrix connects prompts to tests and manual evidence. Verification proceeds from focused deterministic tests to GameTests, server/client boots, and synthetic multiplayer workloads.

**Tech Stack:** Java 25/JUnit 6, Fabric GameTest, Gradle/Loom, Python resource auditors, Git.

## Global Constraints

- No ordinary tick performs work proportional to every registered world object.
- Diagnostics expose actual workload, ticket, packet, and particle state.
- Documentation claims only executed evidence.
- Completion requires a clean, committed, remote-synchronized worktree.

---

### Task 1: Spatial indexes and bounded work

**Files:** force/aura/ward/field/proxy/guardian/named-entity/presence managers, `ChunkSpatialIndex`, rotating queues, metrics/tests.

- [ ] Add adversarial tests for insertion/move/removal/dimension/logout cleanup, capped neighborhood queries, fair rotation, staggered pulses, and overflow refusal.
- [ ] Replace remaining global scans found by static/runtime profiling with shared lifecycle indexes and budgets; reduce proxy tickets to minimum footprints; pre-load Celestial area progressively.
- [ ] Run focused index/budget tests and profile instrumented 10/50/100-player synthetic loads.
- [ ] Commit as `perf: bound all ordinary magical world work`.

### Task 2: Diagnose command and interaction proof

**Files:** `/powers diagnose`, metrics, interaction rules/catalogue/generator, tests/docs.

- [ ] Add tests for permission, stable sections, counts, zero-state output, budget saturation, and sensitive-data exclusion.
- [ ] Report active fields, indexes, forced chunks, bodies, sessions, celestial events, scans, packets, particles, and testing overrides.
- [ ] Generate deterministic tests for every action pair and live GameTests for each physical collision family.
- [ ] Commit as `feat: expose bounded magic diagnostics`.

### Task 3: Gameplay and client proof expansion

**Files:** `PowersGameTests.java`, test structures, client layout/screenshot harness, soak harness/reports.

- [ ] Add live cases for every remaining innate, every spell, every crystal action, three Shadow uniques, curated Partisan routes, all travel mechanisms, body failures, named mobs, Time Stop ownership, spread/auras/clashes, artifact migration, Miniportal, and Shadow routing.
- [ ] Add deterministic client captures for radial/library, both rank panels, energy states/rows/scales, power rail, advancement backgrounds, teleport menu, entity skins, and Light sky.
- [ ] Run GameTests, client visual suite, and 10/50/100-player soak tests; record measured tick, scan, forced-chunk, packet, and particle budgets.
- [ ] Commit as `test: prove complete multiplayer magic behavior`.

### Task 4: Documentation and asset/code manifests

**Files:** README, changelog, interaction docs, requirement matrix, audit docs/manifests, migration notes.

- [ ] Map every sentence from every user prompt to implementation and evidence; mark subjective/manual checks honestly.
- [ ] Explain every remaining power, level transformation, spell, crystal, artifact route, item, acquisition state, realm, faction, control, counterplay, testing command, configuration, and limitation.
- [ ] Run generated magic docs and exact source/non-item asset manifest generation; validate JSON, PNG dimensions, models, translations, recipes, loot, tags, and advancements.
- [ ] Commit as `docs: publish verified POWERS reference`.

### Task 5: Clean release verification and launch

**Files:** verification report and Git state only unless evidence reveals a bug, in which case return to the owning RED/GREEN task.

- [ ] Run with the bundled Java 25 runtime: `./gradlew clean test validatePowerResources auditJavaSources auditNonItemAssets verifyMagicDocs build --no-daemon`.
- [ ] Run `./gradlew runGameTestServer`, dedicated `runServer` boot/clean stop, then `./gradlew runClient` for manual acceptance and leave the requested instance available.
- [ ] Re-run any affected focused/full suites after acceptance fixes; verify `git diff --check` and `git status --short` are clean.
- [ ] Commit final evidence, push `codex/powers-finalisation`, confirm it matches its remote, and provide the exact terminal launch command.
