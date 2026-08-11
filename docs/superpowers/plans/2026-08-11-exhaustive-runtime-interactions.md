# Exhaustive Runtime Interactions Implementation Plan

> **Execution:** Use `superpowers:executing-plans`, test-driven development, systematic debugging, Minecraft modding, and Minecraft testing. The user explicitly approved autonomous decisions and no review pause is required.

**Goal:** Prove and enforce all action collisions and lifecycle interactions, including physical beam clashes, mind/body death, realm travel, toggle ownership, and killable revealed Shadow.

**Tech stack:** Java 25, Minecraft Java 26.2, Fabric Loader/API, Gradle/Loom, JUnit 6, Fabric GameTest.

## Task 1: Collision rule and physical ray geometry

- [x] Add a failing exact-rule test for Energy Beam × Void Beam.
- [x] Add failing pure segment-intersection and bounded-index tests.
- [x] Implement the exact canonical interaction and fixed-cap recent-ray index.
- [x] Publish Energy Beam and Void Beam segments and emit a no-grief pressure blast, two caster lightning omens, and short ringing.
- [x] Run focused unit tests and source compilation.

## Task 2: Travel and detached-mind lifecycle policy

- [x] Add failing exhaustive form × event and travel-kind tests.
- [x] Add the pure lifecycle decision table and internal fatal-soul return route.
- [x] Intercept fatal detached-avatar and physical-proxy damage, return to the physical body, then kill exactly once.
- [x] Preserve ordinary progression-gated body return and same-mindscape travel.
- [x] Run focused unit tests and realm/body GameTests.

## Task 3: Possessed-vessel death and divine wrath

- [x] Add failing tests distinguishing target death from expiry/unload/cancellation.
- [x] Add a typed session-end reason and pure wrath values.
- [x] Return the controller alive on vessel death, then apply hidden debuffs, bounded energy loss, nonlethal damage, divine visuals, and short ringing.
- [x] Run focused tests and a live control-session GameTest.

## Task 4: Toggle ownership reconciliation

- [x] Add failing policy tests for innate death and artifact item/authorisation loss.
- [x] Centralise reconciliation without removing unrelated effects or ability state.
- [x] Ensure death, respawn, disconnect, power loss, artifact drop, and energy exhaustion converge on the same off transition.
- [x] Run focused tests and artifact-loss/death GameTests.

## Task 5: Killable revealed Shadow

- [x] Add failing visibility/body lifecycle tests.
- [x] Spawn one equipment-free, profile-matched mannequin only while revealed.
- [x] Suppress duplicate client apparitions, follow/teleport at the bounded cadence, and process death/dismissal.
- [x] Preserve player-keyed attempt/knowledge memories across Shadow death and resummon.
- [x] Run focused tests, compile client/common sources, and add a live death/resummon GameTest.

## Task 6: Exhaustive catalogue and documentation

- [x] Generate lifecycle interaction documentation from production policy.
- [x] Verify all 2,080 action pairs and every lifecycle combination have non-empty decisions/presentation.
- [x] Update README, interaction rules, changelog, diagnostics, migration notes, and verification scope.
- [x] Regenerate magic docs, item docs, Java audit, and non-item asset audit.

## Task 7: Final verification and Git

- [x] Run focused mutation-relevant tests after final source change.
- [x] Run clean full build, all JUnit, all GameTests, resource/doc/audit validation, dedicated-server boot, client resource boot, and 10/50/100 soak.
- [x] Confirm clean diff checks and no unexpected logs.
- [x] Commit subsystem changes intentionally, push `codex/powers-finalisation`, and finish with a clean synchronized worktree.
