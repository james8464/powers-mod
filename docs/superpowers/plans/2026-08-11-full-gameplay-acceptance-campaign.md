# Full Gameplay Acceptance Campaign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:dispatching-parallel-agents` for independent audit lanes, `minecraft-codex-skills:minecraft-testing` for runtime proof, `computer-use:computer-use` for interactive client acceptance, and `superpowers:systematic-debugging` for every failure. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exercise every finite registered gameplay identity and every canonical interaction/lifecycle rule, fix reproducible defects, and publish evidence that distinguishes automated logic, live Minecraft behavior, visual inspection, and untested environmental permutations.

**Architecture:** The live registries and generated catalogues define the finite scope. Pure rules cover exhaustive combinations; Fabric GameTests cover server-authoritative world behavior; a controlled dev client covers HUD, screens, input, camera, sound, and visual readability. Evidence is recorded per identity instead of treating registry resolution as behavioral success.

**Tech Stack:** Java 25, Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Gradle/Loom, JUnit, Fabric GameTest, Python validation scripts, Codex Computer Use.

## Global Constraints

- Test the pushed `main` build and preserve deliberately recipe-less crystals/items.
- Never claim literal infinity: “every interaction” means every registered pair, lifecycle row, supported target class, travel route, and documented finite state; world seeds/mod combinations remain sampled environmental coverage.
- Reproduce every failure before editing, add the narrowest regression test, then rerun the affected and full gates.
- Keep temporary parallel worktrees detached so `main` remains the only branch.
- Do not overwrite the user's ordinary saves; use isolated Gradle run directories and generated test worlds.
- Record screenshots/logs only from the tested build SHA.

---

### Task 1: Freeze scope and create the evidence register

**Files:**
- Modify: `src/main/java/com/powers/testing/GameplayAcceptanceCatalogue.java`
- Modify: `src/test/java/com/powers/testing/GameplayAcceptanceCatalogueTest.java`
- Modify: `src/test/java/com/powers/testing/ManualAcceptanceChecklistReport.java`
- Generate: `docs/verification/manual-acceptance-checklist.md`
- Create: `docs/verification/2026-08-11-full-gameplay-acceptance.md`

**Interfaces:**
- Consumes: `PowerRegistry`, `SpellRegistry`, `CrystalAbilityCatalog`, `ArtifactActionCatalogue`, item catalogue, action/lifecycle matrices.
- Produces: one row per finite identity with `REGISTRY`, `UNIT`, `GAMETEST`, `CLIENT`, or `PENDING` proof and an evidence identifier.

- [ ] Add a failing catalogue test proving registry resolution alone cannot emit `AUTOMATED PASS` for behavior.
- [ ] Implement proof-strength/result separation and regenerate the checklist.
- [ ] Record build SHA, versions, world/run directory, and exact commands in the campaign report.
- [ ] Verify every live registry identity appears exactly once and every evidence target exists.

### Task 2: Baseline, static integrity, and exhaustive pure matrices

**Files:**
- Inspect: `docs/interactions/interaction-matrix.csv`
- Inspect: `docs/interactions/lifecycle-matrix.csv`
- Inspect: `docs/quality/code-audit.md`
- Inspect: `docs/quality/asset-audit.md`
- Modify only on reproduced defect: corresponding production/test file.

**Interfaces:**
- Consumes: 64-action `MagicActionCatalogue`, `MagicInteractionResolver`, lifecycle decision tables, save migration corpus.
- Produces: exact counts and zero-failure logs for all action pairs, lifecycle rows, serialization, packets, resources, and audits.

- [ ] Run `./gradlew clean check verifyScreenshots saveMigrationCorpus syntheticSoak --no-daemon` from Java 25.
- [ ] Run Python fixture suites, resource validation, generated-doc checks, source audit, and non-item asset audit.
- [ ] Assert 2,080 unordered action pairs and all lifecycle combinations have symmetric, non-empty, budgeted resolutions.
- [ ] Run packet fuzzing and malformed/foreign target fixtures.

### Task 3: Live innate, spell, crystal, and artifact behavior

**Files:**
- Modify: `src/gametest/java/com/powers/gametest/PowersGameTests.java`
- Modify or create focused files under: `src/gametest/java/com/powers/gametest/`
- Modify tests/production only for reproduced gaps.

**Interfaces:**
- Consumes: testing override, `PowerTestActor`, canonical action catalogues, server-owned cast contexts.
- Produces: behavioral GameTest evidence for all 23 innate powers, 12 spells, 11 crystal actions, three Shadow Sword uniques, Partisan dominions, and artifact-routed actions.

- [ ] Build a data-driven probe list whose expected result is specific to damage, terrain, toggle, travel, control, summon, field, information, or support actions.
- [ ] Verify every action starts, charges correct energy, applies its authored effect, observes cooldown/source scaling policy, and cleans up on death/drop/logout/restart.
- [ ] Exercise player, player-compatible actor, ordinary mob, boss, projectile, block, empty-space, protected, amethyst, Light, and Darkness targets where supported.
- [ ] Run all GameTests and attach exact per-action evidence to the register.

### Task 4: Realms, forces, mind/body, progression, and persistence

**Files:**
- Inspect/modify on defect: `src/main/java/com/powers/power/travel/`
- Inspect/modify on defect: `src/main/java/com/powers/mind/`
- Inspect/modify on defect: `src/main/java/com/powers/force/`
- Inspect/modify on defect: `src/main/java/com/powers/progression/`
- Test: relevant unit and GameTest files.

**Interfaces:**
- Consumes: every travel kind, realm confinement, body proxy, Light/Dark spread, amethyst, ranks, quests, energy, Time Stop, Celestial Ruin.
- Produces: live evidence for permitted/forbidden travel, vulnerable bodies, fatal return, spread/clash/aura, rank scaling, persistence, and cleanup.

- [ ] Exercise same-realm, cross-realm, body-return, admin-recovery, unloaded-chunk, logout, death, missing-destination, and restart cases.
- [ ] Exercise Light/Dark spread, aura tags, amethyst suppression/crystallisation, block clash, force events, and catastrophic-spell persistence.
- [ ] Exercise rank 0/5/10 values, quest refresh/prefixes, alignment exclusivity, energy variants, reservoirs, poisoning, and toggles.
- [ ] Restart the dedicated server twice and prove zero leaked tickets, proxies, fields, freezes, summons, or pending catastrophes.

### Task 5: Items, entities, structures, loot, and acquisition

**Files:**
- Inspect: `docs/gameplay/item-catalogue.md`
- Inspect/modify on defect: item, loot, entity, world-generation, Crucible, recipe, and structure sources.
- Test: item executable audit, loot distribution, GameTests, resource checks.

**Interfaces:**
- Consumes: every registered item/entity/block/menu/loot injection and documented acquisition state.
- Produces: evidence that each visible item has a purpose, executable behavior, model, translation, legal acquisition/deferred status, and save-safe legacy handling.

- [ ] Validate every item model/texture/translation/creative visibility and intentionally hidden alias.
- [ ] Exercise artifacts, generic weapons, Grimoires, runestones, reservoirs, Shadow Sword, Partisan, spawn eggs, blocks, Crucible, and consumables.
- [ ] Spawn and fight every custom entity; verify targeting, factions, powers, drops, persistence, model/UV, spawn egg, and cleanup.
- [ ] Generate loot-distribution evidence and ensure no deliberately deferred recipe is introduced.

### Task 6: Interactive client UI, input, audio, and visual acceptance

**Files:**
- Inspect/modify on defect: `src/client/java/com/powers/client/`
- Evidence: `docs/verification/evidence/2026-08-11-full-gameplay-acceptance/`
- Update: `docs/verification/2026-08-11-full-gameplay-acceptance.md`

**Interfaces:**
- Consumes: isolated dev client, testing commands, GUI-scale/accessibility settings, controlled test world.
- Produces: screenshots and observed results for every custom screen/HUD state plus representative near/mid/far magic visuals and sounds.

- [ ] Launch the exact build, create an isolated creative acceptance world, and enable testing overrides.
- [ ] Inspect energy symbols above hunger across GUI scales, health rows, mounts, air, poisoning, Darkness, empty/full states, and HUD rail.
- [ ] Inspect teleport, rank maze, Shadow Sword wheel/library, Rainbow selector, Grimoires, Crucible, advancement tracks, diagnostics, and first-join guide with mouse/keyboard/narration/reduced motion.
- [ ] Cast representative low/high-rank powers at near/mid/far range; inspect particles, scars, flashes, tinnitus, subtitles, camera/FOV comfort, models, and sky/realm presentation.
- [ ] Exercise Shadow chat visibility, conversation, failure explanation, following, combat, conjuration, death, and memory restoration.

### Task 7: Multiplayer, performance, compatibility, and hostile environments

**Files:**
- Inspect/modify on defect: performance/index/network/protection/config/diagnostic sources and tests.
- Update: campaign report.

**Interfaces:**
- Consumes: 10/50/100-player synthetic scenarios, dedicated server, packet fuzzing, protection adapters, permission fallback, borders/claims/void/fluid/mount cases.
- Produces: budget evidence and explicit compatibility caveats.

- [ ] Run 10/50/100-player workloads and record tick work, scans, packets, particles, forced chunks, and memory estimates.
- [ ] Exercise simultaneous fields/casts, collision storms, entity caps, Celestial Ruin loading, Light/Dark spread, Shadow AI, and reconnect churn.
- [ ] Exercise permission nodes, operator audits, consent overrides, safe-zone precedence, malformed config reload, foreign projectiles/effects, and absent optional providers.
- [ ] Sample border, low ceiling, void, fluids, mounts/passengers, portals, and non-vanilla dimension identifiers.

### Task 8: Defect closure and release handoff

**Files:**
- Modify: all files implicated by reproduced failures.
- Update: `README.md`, `CHANGELOG.md`, generated docs/audits, and campaign report.

**Interfaces:**
- Consumes: all failures and evidence from Tasks 1–7.
- Produces: fixed regressions, clean main branch, pushed commit, and an honest residual-risk list.

- [ ] For each defect, record reproduction, failing test, root cause, fix, focused pass, and full regression pass.
- [ ] Rerun the clean build, all JUnit/GameTests, visual/migration/load lanes, client smoke, and dedicated restart soak after the final source change.
- [ ] Regenerate documentation/audits, verify a clean worktree, commit intentionally, push `main`, and wait for GitHub Actions.
- [ ] Report finite coverage counts, failures fixed, unresolved external/manual constraints, and paths to evidence.
