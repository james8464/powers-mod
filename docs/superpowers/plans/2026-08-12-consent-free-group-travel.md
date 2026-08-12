# Consent-Free Group Travel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Make every supported teleport route carry nearby living entities without consent, honour exact entered coordinates, and safely include persistent Shadow companions.

**Architecture:** A shared `TravelCohort` service owns bounded capture, delayed eligibility, exact-offset movement, and Shadow-aware teleportation. `SafeDestinationResolver` gains an exact-coordinate route that retains policy checks but omits environmental landing safety. Mindscape crystals use the same cohort and a bounded non-player return tracker while player returns continue through vulnerable body sessions.

**Tech Stack:** Java 25, Fabric API/Minecraft 26.2, JUnit 5, Fabric GameTest, Gradle.

## Global Constraints

- Radius is exactly 2 blocks and total cohort size is at most 16.
- Teleport consent is never queried for these routes.
- Only the caster pays energy/cooldown.
- Entered coordinates are not moved or rejected for collision, fluid, floor, or hazard safety.
- Ordinary realm confinement remains enforced; Shadow has unrestricted dimensional authority.
- Crystal/item recipes deliberately left absent remain untouched.

---

### Task 1: Shared cohort and exact-coordinate policy

**Files:**
- Create: `src/main/java/com/powers/power/travel/TravelCohort.java`
- Create: `src/test/java/com/powers/power/travel/TravelCohortRulesTest.java`
- Modify: `src/main/java/com/powers/power/travel/SafeDestinationResolver.java`
- Modify: `src/test/java/com/powers/power/travel/SafeDestinationResolverTest.java`
- Modify: `src/main/java/com/powers/companion/PrivateCompanionManager.java`

**Interfaces:**
- Produces: `TravelCohort.capture(ServerLevel, ServerPlayer, LivingEntity)`, `TravelCohort.move(...)`, and `SafeDestinationResolver.validateExact(...)`.
- `TravelCohort` stores `LivingEntity` plus relative `Vec3`, revalidates against the caster origin, and delegates Shadow replacement rebinding to `PrivateCompanionManager.travelBody(...)`.

- [x] **Step 1: Write failing pure tests** for radius boundary, cap, dead/proxy exclusion policy, exact validation selection, and Shadow authority.
- [x] **Step 2: Run focused tests and verify RED** with missing shared interfaces.
- [x] **Step 3: Implement the minimum shared cohort and exact-route interfaces.**
- [x] **Step 4: Run focused tests and source compilation until GREEN.**
- [x] **Step 5: Commit** the independently working travel foundation.

### Task 2: Time Shift and artifact route integration

**Files:**
- Modify: `src/main/java/com/powers/power/AbilityActivationService.java`
- Modify: `src/main/java/com/powers/power/abilities/TeleportAbility.java`
- Delete: `src/main/java/com/powers/power/abilities/TeleportCompanionMover.java`
- Test: `src/test/java/com/powers/power/abilities/TeleportGroupTravelContractTest.java`
- Test: `src/gametest/java/com/powers/gametest/PowersGameTests.java`

**Interfaces:**
- Consumes: `TravelCohort.capture/move` and `SafeDestinationResolver.validateExact`.
- Produces: consent-free direct and Shadow Sword routed teleport, exact coordinate arrival, and automatic nearby living-entity movement.

- [x] **Step 1: Add RED contract and GameTests** proving denied-consent players plus mobs still move and solid entered coordinates remain exact.
- [x] **Step 2: Remove teleport-only consent checks and replace both legacy companion paths with `TravelCohort`.**
- [x] **Step 3: Remove the safe-mark search and use the exact marked coordinate after policy validation.**
- [x] **Step 4: Run focused unit tests and all travel GameTests until GREEN.**
- [x] **Step 5: Commit** Time Shift integration.

### Task 3: Crystal group entry and return

**Files:**
- Create: `src/main/java/com/powers/power/travel/MindscapeMobReturnTracker.java`
- Create: `src/test/java/com/powers/power/travel/MindscapeMobReturnTrackerTest.java`
- Modify: `src/main/java/com/powers/power/crystals/MindscapeCrystalAbility.java`
- Modify: `src/main/java/com/powers/power/crystals/MiddleworldAbility.java`
- Modify: `src/main/java/com/powers/PowersServerLifecycle.java`
- Test: `src/gametest/java/com/powers/gametest/PowersGameTests.java`

**Interfaces:**
- Consumes: `TravelCohort` and existing `BodyProxyManager` return APIs.
- Produces: automatic nearby-player/mob/Shadow crystal entry, independent player bodies, tracked mob returns, and lifecycle cleanup.

- [x] **Step 1: Add RED unit/GameTests** for all living traveller types, per-player body sessions, return, Shadow, cap, and distance boundary.
- [x] **Step 2: Implement the bounded mob-origin tracker and server-stop cleanup.**
- [x] **Step 3: Replace aimed-player/crouch consent branches in all three realm crystals with one automatic cohort flow.**
- [x] **Step 4: Run crystal tests, travel GameTests, and dedicated-server boot until GREEN.**
- [x] **Step 5: Commit** crystal integration.

### Task 4: Documentation, audits, and release verification

**Files:**
- Modify: `README.md`
- Modify: generated gameplay documentation and exact source/asset audit manifests using repository generators.
- Test: existing documentation, resource, and quality suites.

**Interfaces:**
- Produces: user-facing rules that match verified runtime behaviour and a clean quality gate.

- [x] **Step 1: Update README travel, consent, mindscape-body, and Shadow sections.**
- [x] **Step 2: Regenerate all repository-owned documentation and exact audit manifests.**
- [x] **Step 3: Run clean build, full JUnit, all GameTests, resource/docs/audit checks, dedicated-server boot, and client startup smoke.**
- [x] **Step 4: Inspect the diff, commit, push `main`, verify GitHub CI, and confirm a clean synchronized tree.**
