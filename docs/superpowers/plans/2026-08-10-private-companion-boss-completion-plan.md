# Private Companion, Omnipower Boss, and Completion Plan

> Execute behavior test-first. External text generation is optional and may never block, identify players, or control gameplay.

**Goal:** Add the owner-private Shadow companion and tactical First Vessel boss, then prove every present and historical requirement and leave a committed clean tree.

## Task 1: Private companion state and routing

**Tests:** add `PrivateCompanionRulesTest`, `PrivateCompanionRoutingTest`, `PrivateCompanionFollowRulesTest`.

1. Prove eligibility, delayed activation, immediate cleanup, 5 Hz following, preferred offset, >20 block private teleport, interaction distance/view/session checks, and owner-only recipient set.
2. Add `PrivateCompanionSession`, `PrivateCompanionRules`, and `PrivateCompanionManager` storing no world entity.
3. Register spawn/update/interact/dialogue/despawn payloads; server always derives the single owner recipient.
4. Integrate one bounded tick call and all logout/death/dimension/projection cleanup hooks.

## Task 2: Private player-shaped renderer and dialogue UI

**Tests:** payload codec tests, resource/layout validators, and client smoke checklist.

1. Add client `PrivateCompanionView`, renderer, interpolation, ray pick, and compact dialogue panel using the original dark companion skin.
2. Render no collision/shadow/nameplate and play all companion sound/FX only from owner payloads.
3. Add a keybind and sneak-use fallback; reject stale session interaction.
4. Add translations and accessibility/reduced-motion behavior.

## Task 3: Deterministic lore engine

**Tests:** add `LoreDialogueEngineTest` for every context and non-repetition.

1. Add `LoreDialogueContext`, `LoreDialogueEngine`, and bounded eight-topic history.
2. Supply concise original lines for realm, health, energy, rank, alignment blocks, artifact selection, death, boss proximity, and milestones.
3. Use the same engine for boss participant dialogue with different voice tables.

## Task 4: Optional bounded AI text provider

**Tests:** add `DialogueProviderTest` using a local fake HTTP client for disabled, queue full, rate limit, timeout, malformed/refused/oversize output, secret redaction, async completion, and fallback.

1. Add provider config: disabled default, endpoint/model, credential environment-variable name, timeout/caps.
2. Implement `DialogueProvider` interface, offline provider, and optional OpenAI-compatible HTTP provider on a bounded executor.
3. Sanitize prompts to fictional non-identifying state and strip/control-limit returned text.
4. Ensure no credential/prompt leaks to logs/packets and no provider result mutates gameplay.
5. Document setup using current official provider guidance if the optional implementation targets a concrete API.

## Task 5: First Vessel entity and render resources

**Tests:** add `FirstVesselRulesTest`, entity/resource tests, and GameTests.

1. Register `FirstVessel`, attributes, spawn egg, operator command, late-game ritual hook, loot/advancement, original skin/model, sounds, and translations.
2. Implement encounter participant snapshot and health scaling formula/cap.
3. Prohibit natural spawn and clean every owned state on death/removal/stop.

## Task 6: Complete power adapter catalogue

**Tests:** add `BossPowerCatalogueTest` that compares directly with all player-power action IDs and fails for missing undocumented adapters.

1. Add `BossPowerAdapter` and `BossPowerCatalogue`.
2. Implement entity-safe adapters or explicit equivalent actions for every player power; never cast a mob to `ServerPlayer`.
3. Reuse pure impact/rules services and normal protection/damage attribution wherever possible.
4. Generate adapter coverage documentation.

## Task 7: Tactical planner and unique phases

**Tests:** add `BossTacticalPlannerTest` for melee/range/vertical/cluster/projectile/low-health/ward/cover/repetition/invalid-target scenarios.

1. Add immutable `BossEncounterFacts`, candidate scorer, seeded anti-repeat variation, max-24 candidate bound, and 10-tick decision cadence.
2. Enforce per-action cooldown/energy/phase plus one-action global cadence; store IDs, not entity references, in delays.
3. Implement Waking Vessel, Broken Constellation, Crownless God transitions.
4. Implement Constellation Theft, Sevenfold Step, interruptible Vessel Reconstitution, World-Suture, and Last Firmament with semantic `COSMIC` FX and safe bounded terrain work.
5. Add participant dialogue that cannot delay planning.

## Task 8: Multiplayer and lifecycle proof

**Tests:** GameTests plus deterministic 20/50-player simulations.

1. Prove companion payload privacy with multiple players, boss scaling/targeting, phase cleanup, scheduler caps, FX recipient caps, and no retained player/entity references.
2. Exercise high-health/armour synthetic modded bosses as targets for developed player/artifact/forge powers.
3. Run performance regression thresholds and inspect profiler summaries for global scans, packet bursts, and queue growth.

## Task 9: Historical requirement completion matrix

1. Build `docs/verification/final-requirement-matrix.md` mapping every user-request sentence across all prior prompts to implementation files, tests, documentation, and status.
2. Regenerate action roster, every pairwise interaction/collision, effect significance, rank impact, recipes/intentional omissions, assets, and source manifests.
3. Add a test that rejects any incomplete/unknown matrix status, missing registered action documentation, missing boss adapter, or missing interaction pair.
4. Update README fully: install, controls, HUD (ten vanilla-aligned energy symbols above hunger), powers, ranks, paths, spells, realms, confinement, proxies, artifacts, guardians, companion, boss, Crucible, runes, counterplay, config, permissions, commands, compatibility API, performance limits, and troubleshooting.

## Task 10: Final verification and git state

1. Run Java 25 `./gradlew clean check validatePowerResources runGameTestServer`.
2. Boot dedicated server, load/save every custom dimension, execute automated smoke commands, stop cleanly.
3. Boot client and complete visual checklist for HUD at multiple GUI scales, all screens, classic/slim bodies, companion privacy, boss model/phases, realm skies, and significance tiers.
4. Run asset/source audits, `git diff --check`, search for direct visible-effect constructors, synchronous travel loads, unbounded collections/scans, TODO/TBD/placeholders, and undocumented registrations.
5. Commit remaining generated docs/audits as `docs: prove final powers completion`.
6. Require `git status --short` empty and report exact commit hashes and verification evidence.

