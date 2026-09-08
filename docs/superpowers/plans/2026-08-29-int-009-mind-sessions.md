# INT-009 Authoritative Mind Sessions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every mind-affecting runtime one authoritative participant owner, precise nested-session refusal, and lifecycle-matrix-backed fatal cleanup.

**Architecture:** `MindSessionManager` atomically owns typed sessions and every participant UUID; body proxies either own a one-player session or attach to a control/view session acquired by its caller. Existing ability, protection, payment, body-return, realm-confinement, and Shadow managers keep their focused world effects but acquire/release participant authority only through this manager. `MagicLifecycleRules` expands its generated exhaustive matrix for Soul Compass and both Soul Link roles.

**Tech Stack:** Java 25, Fabric 26.2, JUnit 6, Fabric GameTest, Gradle 9.

**Spec:** `docs/planning/IMPROVEMENT_BACKLOG.md` row `INT-009` and `docs/superpowers/plans/2026-08-12-stages-1-8-completion.md`

## Global Constraints

- Preserve server-authoritative casting, protection, consent, payment, body vulnerability, body return, and realm confinement.
- Reject an overlapping session atomically before gameplay side effects and identify the conflicting participant and existing session kind.
- Keep Shadow's travelling body physical while treating Shadow as a participant when a mind effect targets it.
- Do not alter INT-008 evidence, stage checkboxes, or the INT-009 backlog row.
- Run focused tests only with conservative workers; do not run the full GameTest suite in this lane.

---

### Task 1: Authoritative admission and lifecycle policy

**Files:**
- Create: `src/main/java/com/powers/mind/MindSessionManager.java`
- Create: `src/test/java/com/powers/mind/MindSessionManagerTest.java`
- Modify: `src/main/java/com/powers/magic/runtime/MagicLifecycleRules.java`
- Modify: `src/test/java/com/powers/magic/runtime/MagicLifecycleRulesTest.java`

**Interfaces:**
- Produces: typed `Kind`, `Role`, `Participant`, `Admission`, `Session`, atomic `acquire`, participant removal, release, lookup, and lifecycle-form mapping.

- [x] **Step 1: Write deterministic admission and fatal-matrix tests.**
- [x] **Step 2: Run the two focused test classes and record the expected missing-authority RED.**
- [x] **Step 3: Implement the smallest synchronized owner and expanded lifecycle decisions.**
- [x] **Step 4: Run the two focused test classes to GREEN.**

### Task 2: Integrate all production entrypoints

**Files:**
- Modify: `src/main/java/com/powers/mind/BodyProxyManager.java`
- Delete: `src/main/java/com/powers/mind/ParticipantPowerLock.java`
- Modify: `src/main/java/com/powers/power/MagicUseGate.java`
- Modify: `src/main/java/com/powers/power/abilities/VesselPossessionAbility.java`
- Modify: `src/main/java/com/powers/power/crystals/DreamwalkingAbility.java`
- Modify: `src/main/java/com/powers/power/crystals/SoulLinkAbility.java`
- Modify: `src/main/java/com/powers/network/LocatorSpellPackets.java`
- Modify: `src/main/java/com/powers/companion/PrivateCompanionManager.java`
- Modify: `src/main/java/com/powers/power/PowerAbilityRuntime.java`
- Delete: `src/test/java/com/powers/mind/ParticipantPowerLockTest.java`

**Interfaces:**
- Consumes: `MindSessionManager.acquire`, `preflight`, `release`, `removeParticipant`, and `activeFor`.
- Produces: one admission/release path for realm, marking, Astral, possession, controlled Dreamwalking, Soul Compass, Soul Link, and targeted Shadow bodies.

- [x] **Step 1: Add a focused production-entrypoint GameTest that overlaps two real session routes and asserts the precise refusal and unchanged first session.**
- [x] **Step 2: Run only that GameTest and record RED.**
- [x] **Step 3: Replace every participant-lock/body-only admission with the central owner, retaining current safety checks and rollback.**
- [x] **Step 4: Run the focused GameTest and affected body/control/crystal tests to GREEN.**

### Task 3: Generated contract and evidence

**Files:**
- Modify: `src/main/java/com/powers/magic/MagicDocumentation.java`
- Modify: `docs/interactions/lifecycle-matrix.csv`
- Modify: `docs/interactions/interaction-rules.md`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Create: `docs/verification/task-int-009-report.md`

**Interfaces:**
- Produces: generated lifecycle evidence and an exact command/result report bound to this unit.

- [x] **Step 1: Regenerate the lifecycle matrix and update count-bearing prose.**
- [x] **Step 2: Run focused unit, documentation, resource, source-audit, and selected GameTest checks.**
- [x] **Step 3: Record RED/GREEN evidence and known constraints in the task report.**
- [x] **Step 4: Review the diff, commit the cohesive unit, and report the commit SHA without merging or pushing.**
