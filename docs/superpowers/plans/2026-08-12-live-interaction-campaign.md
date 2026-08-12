# Live Interaction Campaign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add and run repeatable live coverage for thousands of multiplayer magic, physical collision, and Shadow interaction scenarios.

**Architecture:** Add one isolated Fabric GameTest class that uses production adapters and distinct embedded `ServerPlayer` connections. Keep exhaustive matrix mechanics separate from Shadow chat/lifecycle cases, then execute the existing client, server, soak, and full regression gates.

**Tech Stack:** Java 25, Minecraft Java 26.2, Fabric Loader/API, Fabric GameTest, Gradle/JUnit 5.

## Global Constraints

- Tests assert observable state and cleanup, not merely catalogue membership or a boolean activation result.
- Ordinary server protections and workload budgets remain enabled.
- No crystal or deliberately deferred recipe is added or changed.
- Existing `main` branch workflow is retained as explicitly requested by the project owner.

---

### Task 1: Exhaustive production cast-adapter matrix

**Files:**
- Create: `src/gametest/java/com/powers/gametest/MultiplayerInteractionGameTests.java`
- Modify: `src/gametest/resources/fabric.mod.json`

**Interfaces:**
- Consumes: `MagicRuntime.catalogue()`, `ServerMagicCasts.prepare`, `ServerMagicCasts.commit`, `MagicRuntime.removePresence`.
- Produces: a required live GameTest covering all 2,080 action pairs.

- [x] Create two embedded server players and assert their UUIDs differ and both are connected.
- [x] Register one owned presence per first action and prepare the second action through the production adapter.
- [x] Assert one reaction, correct owner isolation, allowed/blocking commit semantics, and literal reviewed outcome totals.
- [x] Remove every presence and reset player position/effects after every case.
- [x] Run `./gradlew runGameTest --no-daemon` and investigate any failure from its full server trace.

### Task 2: Exhaustive supported physical-collision matrix

**Files:**
- Modify: `src/gametest/java/com/powers/gametest/MultiplayerInteractionGameTests.java`

**Interfaces:**
- Consumes: `PhysicalMagicPresences.registerFixed`, `MagicRuntime.movePresence`, `PhysicalMagicPresences.collideNearby`.
- Produces: live coverage for every supported delivery-family pair.

- [x] Map canonical deliveries to projectile, beam, field, entity, or force-block handles.
- [x] Enumerate every unordered supported pair and move one handle into the other.
- [x] Assert exactly one resolution in the collision window and zero immediate duplicate resolutions.
- [x] Remove both handles and assert the independently derived scenario count.
- [x] Run the complete live GameTest suite.

### Task 3: Complete Shadow chat arsenal and multiplayer isolation

**Files:**
- Modify: `src/gametest/java/com/powers/gametest/MultiplayerInteractionGameTests.java`
- Modify only if a failing live test proves a defect: the smallest responsible Shadow production class.

**Interfaces:**
- Consumes: `PrivateCompanionManager.handleChat/tickPlayer/body`, `ShadowPowerCatalogue.actions`, `ShadowPowerRuntime.stop`, `ShadowCompanionStore`.
- Produces: chat-to-executor proof for all Shadow actions and isolated multi-owner lifecycle proof.

- [x] Manifest Shadow from chat for an eligible owner and assert its server body exists.
- [x] For each action, provide a live player or mob target, issue `shadow, use <id>`, and assert payment plus action-specific state/cleanup.
- [x] If a case fails, reproduce it alone, identify its root cause, add the focused regression expectation, then make the smallest fix.
- [x] Create multiple eligible owners, assert unique bodies, and exercise independent visibility, stance, memory, targeting, and death.
- [x] Stop toggles, global freeze, summons, sessions, and temporary targets after each case.
- [x] Run all live GameTests again.

### Task 4: Full runtime campaign and evidence

**Files:**
- Modify: `docs/verification/2026-08-12-live-interaction-campaign.md`
- Modify: `docs/quality/code-audit.md` only if production Java changes require audit regeneration.

**Interfaces:**
- Consumes: existing Gradle verification tasks and test launchers.
- Produces: exact commands, revision, scenario counts, failures/fixes, and evidence limitations.

- [x] Run the clean JUnit/resource/docs/visual/Python/synthetic-soak gate.
- [x] Run Fabric GameTests and record required test/scenario counts.
- [x] Run the real client GameTest/screenshot suite and inspect its exit/logs.
- [x] Boot isolated dedicated and integrated test servers, run live commands/diagnostics, and inspect clean shutdown logs.
- [x] Run the connected-player and synthetic 10/50/100-player campaign without weakening budgets.
- [ ] Regenerate required exact audits, commit cohesive changes, push `main`, and verify the remote CI result.
