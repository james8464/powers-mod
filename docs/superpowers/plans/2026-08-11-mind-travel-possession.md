# Mind, Possession, and Travel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make remote-body control, mindscapes, crystals, Middleworld, and all teleport paths safe, consistent, and testable.

**Architecture:** One server-owned `MindInteractionSession` coordinates participant locks, body proxy, target control, return, and cleanup. One `TeleportStorm` owns origin/destination tickets, midpoint transfer, effects, and safe-arrival resolution for every travel source.

**Tech Stack:** Fabric server events/networking, Minecraft 26.2 server levels/tickets, Java 25, JUnit 6, Fabric GameTest.

## Global Constraints

- Frozen physical bodies remain vulnerable; mind avatars cannot take damage or die.
- Every remote participant is power-locked for the session.
- Player-controlled realm departure never bypasses requirements.
- Travel tickets are bounded, asynchronous, and always released.

---

### Task 1: Participant locks and controllable vessel sessions

**Files:** create `ParticipantPowerLock.java`, `ControlledVesselSession.java`; modify possession, dreamwalking, activation gates, input payloads, tests/GameTests.

**Interfaces:** `ParticipantPowerLock.acquire(UUID session, Collection<UUID>)`, `isLocked(UUID)`, `release(UUID)`; session duration maximum 600 ticks.

- [ ] Add failing tests for both-side lock, nested refusal, disconnect cleanup, 30-second ceiling, higher-rank refusal, amethyst refusal, mob support, attack/inventory forwarding, and no-power forwarding.
- [ ] Implement server-authenticated movement/look/hotbar/attack forwarding with bounded deltas and target authority; freeze the target’s own input while possessed.
- [ ] Reuse the core for Dreamwalking; run focused tests and live player/mob/test-actor GameTests.
- [ ] Commit as `feat: implement authoritative vessel control`.

### Task 2: Body and mind safety

**Files:** `BodyProxyManager.java`, `PowerCombatEvents.java`, realm/mind session managers, tests/GameTests.

**Interfaces:** fatal proxy damage calls `returnBeforeFatalDamage(owner, source, amount)` once; mind-avatar damage is cancelled before health mutation.

- [ ] Add failing tests for avatar immunity, nonfatal proxy mirroring, fatal return-before-death, logout, missing dimension, duplicate cleanup, and vulnerable teleport-selection bodies.
- [ ] Implement one cleanup owner and bounded retry/locked holding state with administrator diagnostics.
- [ ] Run all body-proxy, realm-confinement, combat, and GameTests.
- [ ] Commit as `fix: make mind sessions death-safe`.

### Task 3: Shared five-second teleport storm

**Files:** create `TeleportStorm.java`, `TeleportRequest.java`; modify `TravelChunkLoader`, `SafeDestinationResolver`, `TeleportAbility`, packets/screens, tests.

**Interfaces:** `TeleportStorm.begin(ServerPlayer, TravelTarget, TravelSource)` creates a 100-tick lifecycle, transfers on tick 50, and rejects a second active request.

- [ ] Add failing tests for midpoint transfer, dual lightning, body lifecycle, unloaded chunks, nearest non-suffocating resolution, border/protection rejection, cancellation cleanup, unique-name ambiguity, and menu realm visibility.
- [ ] Implement menu with only self coordinates and near unique name; rename every user-facing Time Shift string/ID migration to Teleport; hide Middleworld.
- [ ] Route both ordinary and near-entity teleport through the storm and test distant destinations live.
- [ ] Commit as `feat: unify safe teleport storms`.

### Task 4: Light/Dark crystals and realm return

**Files:** `MindscapeCrystalAbility.java`, realm session/confinement/travel policy, beam FX/packets, tests/GameTests.

**Interfaces:** normal use raycasts a living target; crouch use selects caster plus players within two blocks; fixed realm destinations are created safe before transfer.

- [ ] Add failing tests for target beam, crouch group, player proxy, mob NoAI restore, crystal activation, fixed destination, return storm, invalid affinity, and every forbidden/allowed departure source.
- [ ] Implement shared mind-session transport and strict `PLAYER_RETURN` versus `ADMIN_RECOVERY` policy.
- [ ] Run realm policy, crystal, proxy, chunk loader, and live GameTests.
- [ ] Commit as `fix: complete mindscape crystal travel`.

### Task 5: Middleworld round trip

**Files:** `CrystalAbilityCatalog.java`, `MiddleworldAbility.java`, player persisted origin data, tests/GameTests.

**Interfaces:** Indigo exposes only `middleworld`; first activation stores dimension/position and enters a stable destination through `TeleportStorm`; second activation returns to the exact resolved origin.

- [ ] Add failing catalogue and round-trip persistence tests including restart, occupied origin fallback, and abandoned session cleanup.
- [ ] Remove Portal Rift, implement persisted origin and shared storm, then run focused and live tests.
- [ ] Commit as `fix: make indigo a reliable Middleworld passage`.
