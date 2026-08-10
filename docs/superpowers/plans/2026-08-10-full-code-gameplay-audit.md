# Full Code and Gameplay Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Audit every production Java and resource path, simplify fragile implementations without obscuring rules, fix reproducible gameplay defects, and leave fresh automated evidence for the shipped mod.

**Architecture:** Preserve the server-authoritative Fabric 26.2 design. Move reusable arithmetic, policy, and lifecycle decisions into small deterministic rule classes; keep Minecraft object access in thin runtime adapters; require all world scans, scheduled work, packets, and forced chunks to have explicit bounds and cleanup ownership.

**Tech Stack:** Java 25, Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Gradle/Loom 1.17.19, JUnit 6, Fabric GameTest, Python resource/source validators.

## Global constraints

- Preserve deliberately recipe-less crystals and deferred custom-item recipes.
- Preserve vulnerable frozen bodies and rank-10 Darkness Shadow Sword zero cooldowns.
- Keep spell/crystal/artifact scaling isolated from innate rank scaling.
- Keep the ten-symbol energy row aligned directly above vanilla hunger.
- Do not trade correctness for terseness: simplify ownership and duplication, not named domain rules.
- Every confirmed bug receives a failing focused test before its production fix.

---

### Task 1: Exact inventory and baseline

- [x] Enumerate every file under `src/main/java`, `src/client/java`, `src/main/resources`, and generated resource roots.
- [x] Confirm loader, Minecraft, Java, API, entrypoint, mixin, access-widener, and environment wiring.
- [x] Record largest classes, duplicate implementations, unsafe casts, synchronous chunk loads, global scans, unbounded loops, delayed entity captures, chat delivery, and direct status-effect construction.
- [x] Run the existing unit/resource/source/documentation baseline before edits.

### Task 2: Lifecycle, persistence, and authority

- [x] Audit login/logout/death/respawn/dimension-change/server-stop cleanup for player state, body proxies, companions, toggles, shields, fields, time stop, delayed work, and chunk tickets.
- [x] Audit saved-data/component schema migration and invalid/corrupt input handling.
- [x] Audit every travel path for confinement policy, unloaded destinations, async completion, duplicate activation, energy/cooldown transaction ownership, and administrative recovery separation.
- [x] Add deterministic lifecycle tests and fix only reproduced faults.

### Task 3: Complete gameplay-system audit

- [x] Trace every innate ability, spell, crystal, artifact action, passive, mob attack, force block, rank node, quest, item use, and command from registration to execution.
- [x] Verify target selection, friendly-fire/protection policy, rank/source scaling, energy/cooldown accounting, damage caps, terrain effects, effect visibility, sounds/particles, and cancel/refund behaviour.
- [x] Exercise cross-system collisions among Light, Darkness, amethyst, shields, time stop, projectiles, bodies, realms, artifacts, and Celestial Ruin.
- [x] Add focused unit/GameTests for every confirmed defect, then implement minimal fixes.

### Task 4: Client, networking, and bounded work

- [x] Audit every payload codec/receiver for validation, main-thread execution, authorization, rate limiting, replay resistance, and disconnect cleanup.
- [x] Audit HUD, input mixins, menus, realm sky, entity renderers, and accessibility paths for null state, GUI scaling, vanilla overlap, and stale client state.
- [x] Replace confirmed ordinary-tick global work, uncontrolled packet/particle fan-out, forced loading, or stale spatial indices with bounded ownership-aware work.
- [x] Add deterministic performance-budget and client-layout tests for each change.

### Task 5: Proof and documentation

- [x] Run affected unit tests after each fix and the complete JUnit suite after each subsystem.
- [x] Run Fabric GameTests, resource validation, generated documentation verification, exact Java/asset audits, and dedicated-server boot.
- [x] Update README, changelog, requirement matrix, interaction catalogue, source manifest, and asset manifest only to match proven behaviour.
- [x] Record any path that cannot be live-proven as an explicit acceptance limitation rather than calling it complete.

### Task 6: Final delivery

- [x] Run `./gradlew clean build --no-daemon` from a clean Gradle output directory.
- [x] Review the complete diff for unrelated changes, dead code, stale resources, and generated-file drift.
- [x] Commit the audit in independently reviewable units and report the verified JAR path and checksum.

## Final evidence

- Clean build: `./gradlew clean build --no-daemon` — successful, 2026-08-10.
- Automated proof: 511 JUnit tests and 16 live Fabric GameTests.
- Dedicated server: all vanilla dimensions plus Light Realm, Dark Realm, and Middleworld loaded, saved, and stopped cleanly.
- Runtime JAR: `build/libs/powers-1.0.0.jar`.
- SHA-256: `990a38758dfefc489f1f5cd94eff477ed588acf2a36aca7d27d88f5f0bb694b8`.
