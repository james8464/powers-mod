# Vulnerable Mindscape Projection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reusable vulnerable player-body proxy for mindscape travel, astral projection, possession, and teleport marking.

**Architecture:** A server-owned `ProjectionManager` maintains one session per player and a custom non-player proxy entity. The real player remains authoritative for inventory/health while damage to the proxy is forwarded once through a recursion guard.

**Tech Stack:** Java 25, Minecraft 26.2, Fabric API, synced entity data, Fabric networking and client entity rendering.

## Global Constraints

- Do not add recipes for intentionally unreleased items.
- The abandoned body is vulnerable and cannot duplicate inventory or identity.
- Any invalid lifecycle state returns the owner safely to the body.
- All transitions use the shared cast and destination policy.
- Assets and names are original.

---

### Task 1: Projection session state machine

**Files:**
- Create: `src/main/java/com/powers/projection/ProjectionType.java`
- Create: `src/main/java/com/powers/projection/ProjectionSession.java`
- Create: `src/main/java/com/powers/projection/ProjectionManager.java`
- Create: `src/test/java/com/powers/projection/ProjectionStateMachineTest.java`

**Interfaces:**
- Produces: `begin`, `returnToBody`, `onBodyDamaged`, `tick`, `onDisconnect`, `onDeath`, and `clearAll` operations.

- [ ] Write state-machine tests for one-session-only, normal return, expiry, body death, owner disconnect, proxy loss and server stop.
- [ ] Confirm RED with missing classes.
- [ ] Implement transitions as explicit enum states `STARTING`, `ACTIVE`, `RETURNING`, `CLOSED`; make close idempotent.
- [ ] Persist the body location/type/deadline before moving consciousness.
- [ ] Run tests and commit session classes.

### Task 2: Player-shaped body proxy entity and renderer

**Files:**
- Create: `src/main/java/com/powers/projection/ProjectionBodyEntity.java`
- Modify: `src/main/java/com/powers/PowersMod.java`
- Create: `src/client/java/com/powers/client/render/ProjectionBodyRenderer.java`
- Modify: `src/client/java/com/powers/client/PowersClient.java`
- Create: `src/main/resources/assets/powers/textures/entity/projection_rune.png`

**Interfaces:**
- Produces: registered proxy entity with owner UUID, projection type, pose and equipment snapshot; client renderer resolves owner skin securely.

- [ ] Register a non-saveable or explicitly lifecycle-managed living entity with no inventory pickup, AI, portal travel or player permissions.
- [ ] Synchronize only owner UUID, pose and projection palette.
- [ ] Render the standard player model/skin with frozen pose, translucent rune overlay and copied equipment visuals.
- [ ] Verify a dedicated server loads without client renderer classes.
- [ ] Test spawn/despawn and commit entity/renderer/assets.

### Task 3: Damage bridge and counterplay

**Files:**
- Modify: `src/main/java/com/powers/projection/ProjectionBodyEntity.java`
- Modify: `src/main/java/com/powers/projection/ProjectionManager.java`
- Create: `src/test/java/com/powers/projection/ProjectionDamageBridgeTest.java`

**Interfaces:**
- Produces: guarded one-way damage forwarding preserving attacker attribution.

- [ ] Write tests proving one proxy hit causes one owner hit, recursive callbacks are rejected, immunity frames remain bounded and lethal damage closes the session.
- [ ] Confirm RED before bridge implementation.
- [ ] Forward valid damage to the real player with a thread-local/session guard; never mirror owner damage back to the proxy.
- [ ] Apply ward/safe-zone/forcefield policy to body hits and emit a visible tether pulse on damage.
- [ ] Run tests and commit damage integration.

### Task 4: Realm mindscape travel integration

**Files:**
- Modify: `src/main/java/com/powers/power/crystals/LightCrystalAbility.java`
- Modify: `src/main/java/com/powers/power/crystals/DarkCrystalAbility.java`
- Modify: `src/main/java/com/powers/player/SkillSystem.java`
- Modify: `src/main/java/com/powers/projection/ProjectionManager.java`

**Interfaces:**
- Consumes: body sessions and safe destination resolver.
- Produces: `ProjectionType.LIGHT_MINDSCAPE` and `DARK_MINDSCAPE` travel with a visible tether and guaranteed return.

- [ ] Add integration tests for entry gate, body spawn, body damage, manual return, timeout, death and restart recovery.
- [ ] Replace direct realm teleport with projection begin/return operations.
- [ ] Persist a recovery marker before transition and clear it only after safe return completes.
- [ ] Ensure leaving a realm returns to body rather than requiring an exit recipe.
- [ ] Run tests and commit realm projection integration.

### Task 5: Astral projection, possession, and marking integration

**Files:**
- Modify: `src/main/java/com/powers/power/abilities/AstralProjectionAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/VesselPossessionAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/TeleportAbility.java`

**Interfaces:**
- Produces: projection-backed ability sessions with no stale game-mode restoration.

- [ ] Add tests for no double energy charge on manual return, possession host rejection/death, marking radius/timeout and safe final destination.
- [ ] Replace stored game-mode snapshots with owner-scoped projection state.
- [ ] Limit teleport scouting to configured radius around the target and keep the body at origin.
- [ ] Make possession control a bounded validated host relationship; never transfer inventory or permissions.
- [ ] Run tests and commit each ability integration separately.

### Task 6: Projection visuals and acceptance

**Files:**
- Modify: `src/main/java/com/powers/fx/PowerFx.java`
- Modify: `src/client/java/com/powers/client/ClientPowerState.java`
- Create: `docs/verification/projection-audit.md`

**Interfaces:**
- Produces: tether effect packet/state and reduced-motion alternative.

- [ ] Add compact effect events for tether idle, body damaged, return warning and severed session.
- [ ] Render client-side curves/runes under particle budgets instead of sending each particle from the server.
- [ ] Perform two-client tests for every projection type, body attack, relog and server restart.
- [ ] Record evidence and commit only after all projection acceptance cases pass.
