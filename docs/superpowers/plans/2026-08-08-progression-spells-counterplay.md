# Branching Progression, Grimoires, and Counterplay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace linear ranks with a persistent maze of choices, give every grimoire original spells, and expose fair counterplay/configuration.

**Architecture:** A data-driven rank graph and spell registry provide authoritative server rules while advancements act as the journal. Rituals use a shared channel state with visible interruption windows and protection-policy checks.

**Tech Stack:** Java 25, Fabric API, codecs/JSON resources, player attachments, advancements and custom networking.

## Global Constraints

- Existing completed rank progress migrates without loss.
- Players may branch and reconverge; choices are not permanent classes.
- No new recipes for deliberately unreleased custom items.
- Locator and forced movement honour consent, wards and safe zones.
- Spell names, lore and assets are original.

---

### Task 1: Data-driven rank graph

**Files:**
- Create: `src/main/java/com/powers/progression/RankNode.java`
- Create: `src/main/java/com/powers/progression/RankGraph.java`
- Create: `src/main/java/com/powers/progression/RankProgress.java`
- Create: `src/main/resources/data/powers/ranks/light.json`
- Create: `src/main/resources/data/powers/ranks/darkness.json`
- Create: `src/test/java/com/powers/progression/RankGraphTest.java`

**Interfaces:**
- Produces: graph validation, unlock checks, completed-node persistence and title composition inputs.

- [ ] Write tests for missing parents, cycles, branching, reconvergence, multiple unlockable neighbours and non-destructive hidden paths.
- [ ] Confirm RED with missing graph classes.
- [ ] Implement immutable graph loading and reject invalid reloads atomically.
- [ ] Define Might, Motion, Insight, Wardcraft, Communion, Veil and Dominion branches with ten depth bands and multiple reconnections.
- [ ] Run tests and commit graph code/data.

### Task 2: Progress migration, titles, and advancement journal

**Files:**
- Modify: `src/main/java/com/powers/player/SkillSystem.java`
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`
- Modify/create: `src/main/resources/data/powers/advancement/**`
- Create: `src/test/java/com/powers/progression/RankMigrationTest.java`

**Interfaces:**
- Consumes: `RankGraph` and existing legacy level/path.
- Produces: migrated node set, focus node and composed title.

- [ ] Write migration tests mapping every legacy light/dark rank 0–10 to completed depth nodes with no downgrade.
- [ ] Persist graph node IDs independently of advancement visibility.
- [ ] Stop revoking advancement criteria when focus changes; award journal entries from authoritative progress.
- [ ] Compose titles from depth plus focused branch, yielding distinct names at each junction.
- [ ] Add a costly configurable respec ritual/command that preserves earned universal nodes.
- [ ] Run tests and commit progression migration.

### Task 3: Spell registry and authenticated grimoire casting

**Files:**
- Create: `src/main/java/com/powers/spell/Spell.java`
- Create: `src/main/java/com/powers/spell/SpellRegistry.java`
- Create: `src/main/java/com/powers/spell/GrimoireDefinition.java`
- Modify: `src/main/java/com/powers/item/GrimoireItem.java`
- Modify: `src/main/java/com/powers/item/CelestialGrimoireItem.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Create: `src/test/java/com/powers/spell/SpellRegistryTest.java`

**Interfaces:**
- Produces: held-item-authenticated spell lookup and cast request containing only grimoire/spell selection plus target input.

- [ ] Write registry tests requiring every registered grimoire item to have at least one spell and unique spell IDs.
- [ ] Confirm RED for currently generic books.
- [ ] Register all imported grimoire variants by exact full item identifier.
- [ ] Validate held stack, selected spell, nonce, cooldown, rank, energy and rate limit server-side.
- [ ] Run tests and commit registry/network foundations.

### Task 4: Ritual channel and counterspell framework

**Files:**
- Create: `src/main/java/com/powers/spell/RitualSession.java`
- Create: `src/main/java/com/powers/spell/RitualManager.java`
- Create: `src/main/java/com/powers/spell/CounterspellService.java`
- Create: `src/test/java/com/powers/spell/RitualManagerTest.java`

**Interfaces:**
- Produces: interruptible channel lifecycle and one-use counterspell windows.

- [ ] Write tests for complete, move interruption, damage interruption, item swap, logout, counterspell, invalid target and refund policy.
- [ ] Confirm RED with absent manager.
- [ ] Implement server-tick deadlines and compact visual events for anticipation/release/aftermath.
- [ ] Let amethyst, line-of-sight loss, safe-zone entry and successful counterspell interrupt eligible rituals.
- [ ] Run tests and commit ritual framework.

### Task 5: Grimoire spell families

**Files:**
- Create classes under `src/main/java/com/powers/spell/spells/`
- Modify: `src/main/java/com/powers/power/abilities/DimensionalAnchorAbility.java`
- Modify translations in `src/main/resources/assets/powers/lang/en_us.json`

**Interfaces:**
- Produces: Soul Compass, Dimensional Anchor, Binding Sigil, Anti-Portal Field, Vitality Transfer, Tracking Mark, Ward-Breaking Ritual, Purification Circle, Root Binding, Sanctuary Growth, Infernal Seal, Banishment Circle, Controlled Hellfire, Hex, Counterspell, Concealment Veil, Weather Sigil, Kinetic Ward, Dispel and Ritual Amplification.

- [ ] For each spell, add a focused policy test for cost, cooldown, valid target, counterplay and cleanup.
- [ ] Move Dimensional Anchor out of the random power registry and bind it to the Deep Grimoire.
- [ ] Implement spells using the shared cast/ritual/protection systems; terrain fire and growth obey configuration.
- [ ] Add original translatable names, descriptions and failure messages.
- [ ] Run the spell suite after each family and commit by grimoire family.

### Task 6: Counterplay commands, signatures, and acceptance

**Files:**
- Modify: `src/main/java/com/powers/command/PowerCommand.java`
- Create: `src/main/java/com/powers/spell/MagicalSignature.java`
- Create: `docs/verification/progression-spells-audit.md`

**Interfaces:**
- Produces: consent inspection, magical-signature detection, admin diagnostics and evidence matrix.

- [ ] Add player-facing consent/status commands and op diagnostics for active rituals, anchors, projections and cooldowns.
- [ ] Emit bounded detectable signatures for major casts so counterplay is observable.
- [ ] Test two-player locator privacy, hostile movement, ward interruption, counterspell timing and respec migration.
- [ ] Record all commands/results and commit after acceptance passes.
