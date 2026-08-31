# INT-008 Temporal Leases and Explicit Clocks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give POWERS source-scoped ownership of vanilla tick freeze and make every selected temporal subsystem use an explicit control or frozen/world clock.

**Architecture:** Typed clock values prevent accidental deadline mixing. A pure lease state machine and schema-2 journal sit behind the existing `GlobalTimeStopManager` API; external tick-manager writes supersede POWERS without being undone. World-owned managers gate mutation on vanilla freeze state while lease lifecycle work continues on the control clock.

**Tech Stack:** Java 25, Minecraft 26.2, Fabric Loader/API and mixins, Fabric GameTests, JUnit 5, Python 3 evidence tooling, Gradle 9.5.1.

**Spec:** `docs/superpowers/specs/2026-08-29-int-008-temporal-leases-design.md`

**Current acceptance:** NOT READY. The 2026-08-31 independent review found
additional gameplay and verifier gaps; see [the repair report](../../verification/task-int-008-report.md).
Earlier checked capture/package steps describe historical PENDING evidence only.
The repaired implementation requires fresh 163-GameTest capture, aggregate receipts,
and independent review before closure.

## Global Constraints

- Preserve all existing authored durations, costs, damage, protection, and gameplay outcomes.
- Control time owns leases, drain, reconciliation, and Time Stop presentation.
- Frozen/world time owns projectiles, entities, channels, fields, Celestial Ruin, and realm cycles.
- External freeze always wins and must never be released by POWERS.
- Keep one bounded lease record per server; add no per-entity freeze ledger.
- Preserve the existing public `GlobalTimeStopManager` entry points.
- Use observed RED/GREEN TDD for every behavior change.

---

### Task 1: Typed clock vocabulary

**Files:**
- Create: `src/main/java/com/powers/time/ControlTick.java`
- Create: `src/main/java/com/powers/time/WorldTick.java`
- Create: `src/main/java/com/powers/time/TemporalClocks.java`
- Create: `src/main/java/com/powers/time/package-info.java`
- Create: `src/test/java/com/powers/time/TemporalClocksTest.java`

**Interfaces:**
- Produces: `ControlTick.at(long)`, `WorldTick.at(long)`, deadline/elapsed helpers, `TemporalClocks.control(MinecraftServer)`, `world(ServerLevel)`, and `worldAdvances(MinecraftServer)`.

- [x] Write tests for negative rejection, saturating deadlines, elapsed/remaining boundaries, and external/owned vanilla freeze equivalence.
- [x] Run `./gradlew test --tests 'com.powers.time.*' --no-daemon --console=plain` and observe RED from missing types.
- [x] Implement the minimal immutable clock types and Minecraft adapter.
- [x] Rerun focused tests and `auditJavaSources`; expect green.
- [x] Commit and push Task 1.

### Task 2: Pure source-scoped lease state machine and schema-2 journal

**Files:**
- Create: `src/main/java/com/powers/power/state/TimeStopLease.java`
- Create: `src/main/java/com/powers/power/state/TimeStopLeaseSource.java`
- Create: `src/main/java/com/powers/power/state/TimeStopLeaseRules.java`
- Modify: `src/main/java/com/powers/power/state/TimeStopSavedData.java`
- Create: `src/test/java/com/powers/power/state/TimeStopLeaseRulesTest.java`
- Modify: `src/test/java/com/powers/power/state/TimeStopSavedDataTest.java`

**Interfaces:**
- Consumes: typed `ControlTick` from Task 1.
- Produces: immutable acquire/reconcile/release decisions and schema-2 lease snapshots.

- [x] Write RED tests for acquisition refusal, three sources, exact identity release, external supersession, deadlines, token saturation, and schema-1 migration.
- [x] Run the focused state tests and observe the intended failures.
- [x] Implement the smallest pure records/rules and journal codec changes.
- [x] Rerun state tests plus serialization/resource audits; expect green.
- [x] Commit and push Task 2.

### Task 3: Production lease manager and tick-manager boundary

**Files:**
- Modify: `src/main/java/com/powers/power/state/GlobalTimeStopManager.java`
- Modify: `src/main/java/com/powers/mixin/ServerTickRateManagerMixin.java`
- Modify: `src/main/java/com/powers/power/abilities/TimeFreezeToggleAbility.java`
- Modify: `src/main/java/com/powers/power/crystals/ChronoStopAbility.java`
- Modify: `src/main/java/com/powers/companion/combat/ShadowPowerExecutor.java`
- Modify: `src/main/java/com/powers/companion/combat/ShadowPowerRuntime.java`
- Modify: `src/test/java/com/powers/power/state/GlobalTimeStopRulesTest.java`
- Create: `src/test/java/com/powers/power/state/GlobalTimeStopBoundaryTest.java`

**Interfaces:**
- Consumes: Task 2 lease decisions.
- Produces: token-bound internal writes, source-aware stop paths, control-clock expiry/drain/snapshot, and transactional persistence.

- [x] Write RED boundary tests proving mismatched stops and external writes cannot unfreeze or retain authority.
- [x] Run focused tests and observe RED.
- [x] Replace manager-owned mutable `Stop` with leases; bind mixin writes to the active token.
- [x] Migrate innate/crystal/Shadow entry points without changing their public contracts.
- [x] Rerun focused power/state/companion tests and source audit; expect green.
- [x] Commit and push Task 3.

### Task 4: Frozen/world-clock subsystem migration

**Files:**
- Modify: `src/main/java/com/powers/spell/SpellCastingManager.java`
- Modify: `src/main/java/com/powers/spell/SpellFieldManager.java`
- Modify: `src/main/java/com/powers/spell/SpellFieldTiming.java`
- Modify: `src/main/java/com/powers/spell/CelestialRuinManager.java`
- Modify: `src/main/java/com/powers/spell/CelestialRuinRules.java`
- Modify: `src/main/java/com/powers/realm/RealmEventManager.java`
- Modify: `src/main/java/com/powers/realm/RealmHeraldManager.java`
- Modify: `src/test/java/com/powers/spell/ChannelRulesTest.java`
- Modify: `src/test/java/com/powers/spell/SpellFieldTimingTest.java`
- Modify: `src/test/java/com/powers/spell/CelestialRuinRulesTest.java`
- Modify: `src/test/java/com/powers/realm/RealmEventRulesTest.java`

**Interfaces:**
- Consumes: `TemporalClocks.world` and `worldAdvances`.
- Produces: no world-owned mutation during any vanilla freeze; unchanged behavior when advancing.

- [x] Add RED tests that classify each selected subsystem's clock and prevent repeated modulo side effects at parked world time.
- [x] Observe focused failures before production edits.
- [x] Route all selected managers through the explicit clock adapter and delete ownership-only freeze predicates.
- [x] Rerun spell/realm/time tests and source audit; expect green.
- [x] Commit and push Task 4.

### Task 5: Live temporal GameTests

**Files:**
- Create: `src/gametest/java/com/powers/gametest/TemporalOwnershipGameTests.java`
- Create: `src/gametest/resources/data/powers/test_environment/temporal_ownership_isolated.json`
- Modify: `src/gametest/resources/fabric.mod.json`
- Create or modify: `src/test/java/com/powers/quality/TemporalSourceBoundaryTest.java`

**Interfaces:**
- Produces: live proof for admin preservation, external supersession, expiry, projectile pause/resume, world-owned pause, and lifecycle cleanup.

- [x] Register RED GameTests and source-boundary assertions before changing any remaining production behavior.
- [x] Run focused JUnit and the unfiltered GameTest task; retain expected RED evidence.
- [x] Add only the minimal test hooks needed for deterministic observation; no test-only production policy.
- [x] Rerun all GameTests until 155 plus the new temporal cases pass without a lag invalidation.
- [x] Commit and push Task 5.

### Task 6: Exact-SHA evidence and independent review

**Files:**
- Create: `scripts/verify_int008_temporal.py`
- Create: `scripts/package_int008_evidence.py`
- Create: `src/test/python/test_verify_int008_temporal.py`
- Create: `src/test/python/test_package_int008_evidence.py`
- Create: `docs/verification/evidence/2026-08-29-int-008/**`
- Create: `docs/verification/task-int-008-report.md`

**Interfaces:**
- Produces: deterministic exact-SHA evidence package, strict verifier, checksums, privacy report, and review bundle.

- [x] Write verifier/package tests first and observe RED.
- [x] Implement strict schema, sorted inventory, deterministic archive, checksum recomputation, and privacy rejection.
- [x] Capture real dedicated-server/GameTest evidence for the clean implementation SHA; do not synthesize runtime rows.
- [x] Verify/package twice and prove byte-identical archives.
- [ ] Run focused gates and request independent code/evidence review; resolve every finding through TDD.
- [ ] Commit and push accepted implementation evidence.

### Task 7: Closure, full gates, integration, and hygiene

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/planning/IMPROVEMENT_BACKLOG.md`
- Modify: `docs/superpowers/plans/2026-08-12-stages-1-8-completion.md`
- Modify: `docs/superpowers/plans/2026-08-29-int-008-temporal-leases.md`
- Modify: `docs/verification/evidence/2026-08-29-int-008/**`

- [ ] Run focused verifier/package/checksum/privacy gates.
- [ ] Run literal `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon --console=plain` on implementation head.
- [ ] Obtain independent READY review before closure claims.
- [ ] Reconcile task/plan/backlog/docs and commit/push closure.
- [ ] Rerun the literal gate and final READY review on closure head.
- [ ] Fast-forward merge to `main`, rerun the literal gate, and push `origin/main` only when green.
- [ ] Prove `main == origin/main`, every POWERS worktree clean, then continue strict order with `INT-009`.
