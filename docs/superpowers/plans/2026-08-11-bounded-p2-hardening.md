# Bounded P2 hardening implementation plan

> Execute directly on `main` under the owner's explicit single-branch instruction. Every behavior change follows RED → GREEN → affected suite → commit.

**Goal:** Close the selected P2 hardening entries in the approved design without expanding save schemas or introducing unfinished feature surfaces.

**Architecture:** Add small pure rule/format records at system boundaries, then route existing Minecraft adapters through them. Keep effects server-authoritative, bounded, tag-extensible, and observable through redacted diagnostics.

**Tech stack:** Java 25, Fabric/Minecraft 26.2, Gradle/JUnit 5, Python resource generators, GitHub Actions.

---

## Task 1: Cooldown presentation (COR-017)

**Files:** new `src/main/java/com/powers/cooldown/CooldownPresentation.java`, new focused test, existing HUD/catalogue/grimoire/Shadow/diagnostic formatting call sites.

1. Add failing boundary tests for zero, one tick, exact seconds, fractional seconds, and tenths.
2. Implement one ceiling-seconds and one tenths formatter.
3. Replace duplicate player-facing arithmetic; retain raw ticks in protocol/state.
4. Run focused tests and source compilation; commit.

## Task 2: Fireball release and lightning conductance (PWR-009, PWR-010)

**Files:** existing ability/rule/impact resolver classes, new conductor tags, focused rule/resolver tests.

1. Add failing tests for crouch-use release state/velocity and exact ownership retention.
2. Implement explicit release while preserving normal use-to-charge and bounded reflection.
3. Add failing conductor classification/node-cap tests for wet, copper/rod contact, armour, and non-conductive content.
4. Implement tag-backed runtime classification and medium-specific bounded chain metadata without harmful vanilla bolt damage.
5. Run focused and full power-ability suites; commit.

## Task 3: Time Freeze forecast (PWR-018)

**Files:** `TimeFreezeDrainRules`, `TimeFreezeToggleAbility`, relevant messages/tests.

1. Add failing forecast/low-TPS threshold tests.
2. Implement authoritative safe-seconds forecast and activation warning.
3. Keep refusal tied only to explicit operator policy; run focused tests and commit.

## Task 4: Consent and operator audit (ART-010, NET-005)

**Files:** new bounded audit event/log classes, `ConsentOverrideRuntime`, recovery/travel/testing/catastrophic command and ritual hooks, tests.

1. Add failing tests for sanitised structured events, bounded aggregation, and target-facing override details.
2. Implement logger plus aggregate counters; never weaken safe-zone/policy checks.
3. Hook all required privileged actions and test their pure event construction.
4. Run protection/command/ritual suites; commit.

## Task 5: Diagnostics export and config validation (NET-006, NET-008)

**Files:** `PowerDiagnosticsCommand`, new export schema/writer, `PowersConfigLoader`, new validation report, tests.

1. Add failing schema redaction, atomic-write, clamp/default-delta, and bounded-report tests.
2. Implement `/powers diagnose export` to a narrow world-owned directory.
3. Retain raw-versus-sanitised config deltas, expose their count/revision in reload and diagnostics.
4. Run configuration/command/diagnostics suites; commit.

## Task 6: Wisdom Fruit acquisition (ART-017)

**Files:** existing additive loot injection catalogue/resources, item documentation generator, loot tests.

1. Add a failing catalogue test proving at least one concrete survival source.
2. Add a low-rate Archivist/realm-themed source using existing additive loot infrastructure.
3. Regenerate item/acquisition documentation and prove no accidental unobtainable entries.
4. Run loot/resource suites; commit.

## Task 7: Verification hardening (QA-011, QA-013, QA-014, QA-018)

**Files:** CI workflow, Gradle tasks, README contract test, documentation generators, resource validator/tests.

1. Add README registry-count/local-link contract tests.
2. Add generator `--check` modes and a CI dirty-tree gate.
3. Add deterministic recipe/loot/tag local-reference and cycle validation fixtures.
4. Add a separately attributed synthetic performance job with existing budgets.
5. Run focused validation, full unit suite, and commit.

## Task 8: Evidence, tidy, and release

**Files:** backlog, acceptance ledger, changelog/README only where behavior changed, generated manifests/catalogues.

1. Remove only P2 rows whose acceptance conditions are actually proven; keep the eight real-evidence P0/P1 rows open.
2. Remove obsolete imports/files discovered by compiler/static checks; regenerate exact manifests after all source movement.
3. Run clean full build, unit/resource/doc validation, GameTests, dedicated-server boot, visual suite, and synthetic soak.
4. Confirm one local/remote branch, clean tree, push `main`, and verify local/remote HEAD equality.
