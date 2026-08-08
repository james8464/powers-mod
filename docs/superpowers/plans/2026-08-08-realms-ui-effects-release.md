# Mindscape Realms, HUD, Effects, Assets, and Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the Light and Dark Realms into authored mindscapes, redesign the HUD, intensify ancient-magic effects, and produce a verified release.

**Architecture:** Realm content is built from original registered blocks/entities/structures and finite session-seeded encounters. The HUD and repeatable magic geometry are client-rendered from compact synchronized state/effect events under configurable budgets.

**Tech Stack:** Java 25, Fabric API, Minecraft 26.2 resource/data packs, JSON block/item models, PNG textures, entity renderers and GUI sprites.

## Global Constraints

- Do not add recipes for intentionally unreleased crystals or artifacts.
- Generate only original assets and lore.
- Use Blaze3D/Fabric-supported rendering; no raw OpenGL.
- Support normal, depleted, amethyst, darkness and projection HUD states.
- Respect reduced motion and server/client particle budgets.

---

### Task 1: Realm block and item foundation

**Files:**
- Modify: `src/main/java/com/powers/PowersBlocks.java`
- Modify: `src/main/java/com/powers/PowersItems.java`
- Create block classes under `src/main/java/com/powers/realm/block/`
- Create blockstates/models/items/textures under `src/main/resources/assets/powers/`
- Create loot tables under `src/main/resources/data/powers/loot_table/blocks/`

**Interfaces:**
- Produces: Light Rune Stone, Luminous Glass, Memory Root, Echo Shrine, Dark Monolith, Veil Glass, Void Root and Veil Altar.

- [ ] Register all eight blocks with distinct sound/light/strength behaviour and no survival recipes.
- [ ] Create original 16× textures with matching item/block models and blockstates.
- [ ] Add self-drop loot tables where lore allows; altar/shrine cores require configured ritual interaction.
- [ ] Run asset/model validation and client model bake smoke test.
- [ ] Commit each realm's block set separately.

### Task 2: Finite mindscape layout and structures

**Files:**
- Create: `src/main/java/com/powers/realm/MindscapeLayout.java`
- Create: `src/main/java/com/powers/realm/MindscapeBuilder.java`
- Add structure templates under `src/main/resources/data/powers/structure/`
- Modify dimension/biome JSON under `src/main/resources/data/powers/`
- Create: `src/test/java/com/powers/realm/MindscapeLayoutTest.java`

**Interfaces:**
- Produces: deterministic finite layout from owner UUID, projection type and progression depth.

- [ ] Write deterministic tests for bounded radius, guaranteed spawn platform, shrine/altar reachability and no overlapping critical rooms.
- [ ] Confirm RED before layout implementation.
- [ ] Implement pure seeded layout followed by chunk-safe placement of arches, roots, monoliths, stairs and trial chambers.
- [ ] Give Middleworld its own biome identity rather than reusing Light Realm.
- [ ] Run tests/resource validation and commit layout/data.

### Task 3: Realm encounters and lore journal

**Files:**
- Create entity classes under `src/main/java/com/powers/realm/entity/`
- Create renderers/models under `src/client/java/com/powers/client/render/`
- Create: `src/main/java/com/powers/realm/RealmEncounterManager.java`
- Create translations/lore resources under `src/main/resources/assets/powers/lang/` and `data/powers/`

**Interfaces:**
- Produces: Light Witness, Memory Mote, Bound Echo and Hollowed encounters plus Beacon of Names and Veil bargains.

- [ ] Register bounded-lifetime encounter entities that cannot leak into ordinary dimensions.
- [ ] Implement original silhouettes/models, palettes and readable combat telegraphs.
- [ ] Connect trial outcomes to rank-maze choices without granting unreleased items or recipes.
- [ ] Add journal entries through advancements/messages rather than unskippable dialogue.
- [ ] Run realm session cleanup tests and commit encounters/lore.

### Task 4: Compact magic effect protocol

**Files:**
- Refactor: `src/main/java/com/powers/fx/PowerFx.java`
- Create: `src/main/java/com/powers/fx/MagicEffectEvent.java`
- Create: `src/client/java/com/powers/client/fx/MagicEffectRenderer.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Create: `src/test/java/com/powers/fx/MagicEffectBudgetTest.java`

**Interfaces:**
- Produces: effect events for circle, glyph chain, spiral, seal, tether, fracture, storm and realm pulse.

- [ ] Write budget tests proving invalid counts clamp and per-player/server limits cannot be exceeded.
- [ ] Confirm RED with no budget service.
- [ ] Send compact origin/palette/seed/duration events and generate repeatable geometry client-side.
- [ ] Implement anticipation, release and aftermath timing for major abilities/spells.
- [ ] Add reduced-motion substitutions and distance culling.
- [ ] Profile packet/entity counts and commit effect protocol.

### Task 5: HUD redesign

**Files:**
- Replace: `src/client/java/com/powers/client/EnergyHudRenderer.java`
- Replace: `src/client/java/com/powers/client/PowerHudRenderer.java`
- Modify: `src/client/java/com/powers/client/ClientPowerState.java`
- Add GUI sprites under `src/main/resources/assets/powers/textures/gui/sprites/hud/`
- Create: `src/test/java/com/powers/client/HudLayoutMathTest.java`

**Interfaces:**
- Produces: resolution-independent central sigil and three arched/radial slot descriptors.

- [ ] Write pure layout tests for 16:9, 16:10, 4:3 and minimum supported GUI sizes, asserting no hotbar/playfield overlap.
- [ ] Confirm RED before layout helper exists.
- [ ] Implement normal, empty, amethyst, darkness and projection/tether energy skins.
- [ ] Add power icon, key label, cooldown sweep, toggle state and insufficient-energy pulse for each slot.
- [ ] Use correct 26.2 sprite/blit APIs and accessibility text/reduced motion.
- [ ] Capture screenshots for every state/scale and commit HUD code/assets.

### Task 6: Resource validation and packaging

**Files:**
- Create: `scripts/validate_resources.py`
- Modify: `build.gradle`
- Modify: `src/main/resources/fabric.mod.json`
- Modify: `.gitignore`

**Interfaces:**
- Produces: deterministic validator invoked by `check`.

- [ ] Validate JSON and `.mcmeta` duplicate keys, identifiers, translations, item definitions, models, textures, blockstates, advancements, tags, loot references, PNG decoding and orphan allowlist.
- [ ] Fail on `.DS_Store`, stale typo keys, missing assets or unregistered content.
- [ ] Add mod contact/source/issues metadata and exact supported Minecraft/Fabric dependency bounds.
- [ ] Verify the packaged JAR contains no cache/test/world files.
- [ ] Commit validation/metadata.

### Task 7: Full release verification

**Files:**
- Modify: `README.md`
- Create: `CHANGELOG.md`
- Create: `docs/verification/final-release-audit.md`

**Interfaces:**
- Produces: final requirement-to-evidence matrix and releasable JAR.

- [ ] Reconcile every design/spec/audit item to code, resource, automated test and manual evidence.
- [ ] Confirm no crystal/artifact recipes were added by diffing the recipe manifest.
- [ ] Run clean build, all tests/GameTests, resource validation, dedicated-server startup and client smoke session.
- [ ] Run two-client projection, consent, spell, rank, realm and HUD scenarios.
- [ ] Profile simultaneous wards, storms, projections and time effects under configured budgets.
- [ ] Update README/CHANGELOG only from verified behaviour.
- [ ] Build the final JAR, inspect its contents and record hashes/commands/results.
- [ ] Commit release documentation only when no requirement remains unproven.
