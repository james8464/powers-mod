# Practical Spells, Artifacts, and Shadow Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the active grimoire roster with twelve practical spells, make retained imported artifacts purposeful using only existing magic energy, and give Shadow authoritative explanations for failed magical actions.

**Architecture:** Keep registry IDs save-safe while separating catalogue migration, spell execution, consent policy, artifact energy storage, and diagnostic history into focused server-owned services. Pure rules receive JUnit coverage; entity, world, networking, and damage behavior receive Fabric GameTests. All cast sources continue through the existing server-authoritative magic runtime.

**Tech Stack:** Java 25, Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API, Gradle/Loom, JUnit 6, Fabric GameTest, Mojang mappings.

## Global Constraints

- Deep contains only Dimensional Anchor.
- Blight contains Blood Reading and Grave Recall; Grave Recall reports only the caster's latest death coordinates and dimension.
- Wild Hearth Sanctuary grants the existing sacrificial forcefield contract to living entities within three blocks.
- Infernal is absent from active gameplay but historical identifiers remain hidden save-safe aliases.
- Abyssal contains only Ward-Breaking Ritual and Dispel.
- No essence balance, essence inventory, essence spell, or second resource bar may remain.
- The Empyrean Jewel overrides every consent-gated action, but not safe zones or administrative/server policy.
- Crystals and intentionally deferred recipes remain untouched.
- Spells never inherit innate rank scaling.
- Status effects created by this work use `PowerStatusEffects.hidden` and authored particles rather than vanilla effect particles.

---

### Task 1: Canonical Spell Catalogue and Save Migration

**Files:**
- Create: `src/main/java/com/powers/spell/SpellSelectionMigration.java`
- Modify: `src/main/java/com/powers/spell/SpellEffect.java`
- Modify: `src/main/java/com/powers/spell/SpellRegistry.java`
- Modify: `src/main/java/com/powers/spell/SpellCastingManager.java`
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`
- Test: `src/test/java/com/powers/spell/SpellSelectionMigrationTest.java`
- Test: `src/test/java/com/powers/spell/SpellRegistryTest.java`

**Interfaces:**
- Produces: `SpellSelectionMigration.canonicalIndex(String grimoireKey, int savedIndex)`.
- Produces: `SpellRegistry.isDormantTexture(String texture)` and twelve active spell definitions.
- Produces: `PlayerPowersData.rawSelectedSpell(String)` and `setSelectedSpell(String, int)`.

- [ ] **Step 1: Write failing catalogue and migration tests**

```java
assertEquals(12, registry.definitions().stream().mapToInt(g -> g.spells().size()).sum());
assertEquals(List.of("dimensional_anchor"), ids(registry.forTexture("book_grimoire_deep")));
assertTrue(registry.isDormantTexture("book_grimoire_infernal"));
assertEquals(1, SpellSelectionMigration.canonicalIndex("book_grimoire_abyssal", 3));
```

- [ ] **Step 2: Run `./gradlew test --tests 'com.powers.spell.SpellRegistryTest' --tests 'com.powers.spell.SpellSelectionMigrationTest'` and confirm the assertions fail against the 21-spell roster.**

- [ ] **Step 3: Replace retired enum values and build the exact active roster**

```java
public enum SpellEffect {
    SOUL_COMPASS, AUGURY, CARTOGRAPHERS_STAR, CELESTIAL_RUIN,
    DIMENSIONAL_ANCHOR, BLOOD_READING, GRAVE_RECALL,
    PURIFICATION_CIRCLE, VERDANT_TENDING, HEARTH_SANCTUARY,
    WARD_BREAKING_RITUAL, DISPEL
}
```

Use the approved order in each book so cycling is stable. Exclude Infernal from `definitions()` and mark the canonical and recolour-overlay textures dormant.

- [ ] **Step 4: Reconcile raw saved indices once at selection time**

```java
int raw = data.rawSelectedSpell(grimoire.key());
int canonical = SpellSelectionMigration.canonicalIndex(grimoire.key(), raw);
if (canonical != raw) data.setSelectedSpell(grimoire.key(), canonical);
return grimoire.spells().get(Math.floorMod(canonical, grimoire.spells().size()));
```

- [ ] **Step 5: Make use of a dormant Infernal book return a translated action-bar explanation without payment or cooldown.**

- [ ] **Step 6: Run the focused tests and `./gradlew compileJava`; expect both to pass.**

- [ ] **Step 7: Commit `feat: replace grimoires with practical spell catalogue`.**

### Task 2: Persistent Last-Death Record

**Files:**
- Create: `src/main/java/com/powers/player/LastDeathRecord.java`
- Modify: `src/main/java/com/powers/player/PlayerPowerAttachments.java`
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`
- Modify: `src/main/java/com/powers/PowerCombatEvents.java`
- Test: `src/test/java/com/powers/player/LastDeathRecordTest.java`
- Test: `src/gametest/java/com/powers/PowersGameTests.java`

**Interfaces:**
- Produces: `LastDeathRecord(String dimension, int x, int y, int z)` with a persistent `Codec`.
- Produces: `PlayerPowersData.lastDeath()` and `recordDeath(ServerPlayer)`.

- [ ] **Step 1: Write a failing codec round-trip test and a GameTest asserting a killed test player records floored block coordinates and dimension.**

- [ ] **Step 2: Run the focused JUnit test and confirm `LastDeathRecord` is missing.**

- [ ] **Step 3: Add the copy-on-death attachment and record it in `AFTER_DEATH` before respawn state is replaced.**

```java
public void recordDeath(ServerPlayer player) {
    BlockPos pos = player.blockPosition();
    target.setAttached(LAST_DEATH, new LastDeathRecord(
            player.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ()));
}
```

- [ ] **Step 4: Run the focused JUnit test, then the named GameTest; expect both to pass.**

- [ ] **Step 5: Commit `feat: persist player death locations for grave recall`.**

### Task 3: Practical Spell Rules and Execution

**Files:**
- Create: `src/main/java/com/powers/spell/AuguryReport.java`
- Create: `src/main/java/com/powers/spell/BloodReadingReport.java`
- Create: `src/main/java/com/powers/spell/VerdantTendingRules.java`
- Modify: `src/main/java/com/powers/spell/SpellEffects.java`
- Modify: `src/main/java/com/powers/spell/SpellCastValues.java`
- Modify: `src/main/java/com/powers/realm/RealmEventRules.java`
- Test: `src/test/java/com/powers/spell/AuguryReportTest.java`
- Test: `src/test/java/com/powers/spell/BloodReadingReportTest.java`
- Test: `src/test/java/com/powers/spell/VerdantTendingRulesTest.java`
- Test: `src/gametest/java/com/powers/PowersGameTests.java`

**Interfaces:**
- Produces: `AuguryReport.create(ServerLevel, BlockPos)` returning translated facts only.
- Produces: `BloodReadingReport.create(LivingEntity)` returning bounded health/armour/effect facts.
- Produces: `VerdantTendingRules.action(BlockState)` and `MAX_BLOCKS_PER_CAST = 192`.
- Consumes: `PlayerPowersData.lastDeath()` from Task 2.

- [ ] **Step 1: Add failing pure tests for moon/weather/event timing, bounded blood facts, and every supported vegetation/soil/fire transformation.**

- [ ] **Step 2: Run the three focused test classes and confirm missing production types.**

- [ ] **Step 3: Implement Augury, Blood Reading, Grave Recall, Purification, and Verdant Tending in the spell switch.**

```java
case AUGURY -> augury(caster);
case BLOOD_READING -> bloodReading(caster, target);
case GRAVE_RECALL -> graveRecall(caster);
case VERDANT_TENDING -> verdantTending(caster, values);
```

Blood Reading requires a living target and routes player privacy through `PowerProtection.mayLocate`. Grave Recall sends exactly one dimension line and one coordinate line. Verdant Tending traverses a deterministic radius order, checks `PowerProtection.mayAffectBlock`, and stops after 192 inspected positions or the configured changed-block cap.

- [ ] **Step 4: Add bounded dust/rune/sound presentations for each successful ritual and cancellation feedback for empty death records or invalid targets.**

- [ ] **Step 5: Add GameTests for success and failure paths, including no last death, protected blocks, and Blood Reading on a player-like test actor.**

- [ ] **Step 6: Run focused JUnit/GameTests and `./gradlew compileJava`; expect all to pass.**

- [ ] **Step 7: Commit `feat: implement practical grimoire rituals`.**

### Task 4: Cartographer's Star Search Flow

**Files:**
- Create: `src/main/java/com/powers/spell/CartographerQuery.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `src/main/java/com/powers/network/LocatorSpellPackets.java`
- Modify: `src/client/java/com/powers/client/screen/CelestialLocatorScreen.java`
- Modify: `src/client/java/com/powers/client/PowersClient.java`
- Test: `src/test/java/com/powers/spell/CartographerQueryTest.java`
- Test: `src/test/java/com/powers/network/CastNonceTrackerTest.java`
- Test: `src/gametest/java/com/powers/PowersGameTests.java`

**Interfaces:**
- Produces: `enum CelestialSearchMode { ENTITY, WORLD }` carried in the open-screen payload.
- Produces: `CartographerQuery.parse(String)` accepting `structure <namespace:id>`, `biome <namespace:id>`, and `landmark <name>`.

- [ ] **Step 1: Write failing parser tests for valid namespaced IDs, missing modes, excessive length, and invalid syntax.**

- [ ] **Step 2: Run the parser test and confirm failure because the parser does not exist.**

- [ ] **Step 3: Extend the nonce-bound locator screen with a mode-specific title, placeholder, and help text; retain keyboard, narration, and packet length limits.**

- [ ] **Step 4: Resolve only loaded registry entries through Minecraft's bounded locate APIs. Reject unknown IDs and report scan failure without charging; commit spell energy/cooldown only when a valid search begins.**

- [ ] **Step 5: Return direction, distance, dimension, coordinates, and registry ID. Do not teleport or keep chunks loaded.**

- [ ] **Step 6: Run parser/network tests, the world-search GameTest, and `./gradlew compileClientJava`; expect all to pass.**

- [ ] **Step 7: Commit `feat: add bounded cartographers star search`.**

### Task 5: Hearth Sanctuary Forcefields

**Files:**
- Create: `src/main/java/com/powers/spell/HearthSanctuaryRules.java`
- Modify: `src/main/java/com/powers/spell/SpellEffects.java`
- Modify: `src/main/java/com/powers/power/abilities/ForcefieldAbility.java`
- Test: `src/test/java/com/powers/spell/HearthSanctuaryRulesTest.java`
- Test: `src/gametest/java/com/powers/PowersGameTests.java`

**Interfaces:**
- Produces: `ForcefieldAbility.raiseSpellWard(ServerLevel, LivingEntity, float)` using non-reflective, unranked integrity.
- Produces: `HearthSanctuaryRules.withinRadius(double distanceSquared)` with a strict three-block radius and a capped 32-entity candidate set.

- [ ] **Step 1: Write failing radius, integrity, no-overflow, and independent-shield tests.**

- [ ] **Step 2: Run the focused tests and confirm the spell API is missing.**

- [ ] **Step 3: Extract the existing shield raise operation and add the unranked spell entry point without exposing mutable shield state.**

- [ ] **Step 4: Apply an independent shield to each living, non-spectating entity within three blocks, including the caster, with ancient green-gold/cyan ward visuals.**

- [ ] **Step 5: Add a GameTest where an oversized hit shatters one target's field, blocks the entire hit, and leaves another target's field intact.**

- [ ] **Step 6: Run focused tests and commit `feat: turn hearth sanctuary into a shared forcefield ritual`.**

### Task 6: Full Consent Override via Empyrean Jewel

**Files:**
- Create: `src/main/java/com/powers/protection/ConsentKind.java`
- Create: `src/main/java/com/powers/protection/ConsentOverrideRules.java`
- Create: `src/main/java/com/powers/protection/ConsentOverrideRuntime.java`
- Modify: `src/main/java/com/powers/protection/ConsentProtectionRules.java`
- Modify: `src/main/java/com/powers/protection/PowerProtection.java`
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`
- Modify: `src/main/java/com/powers/ImportedPackItems.java`
- Test: `src/test/java/com/powers/protection/ConsentOverrideRulesTest.java`
- Test: `src/gametest/java/com/powers/PowersGameTests.java`

**Interfaces:**
- Produces: `ConsentOverrideRuntime.authorize(ServerPlayer caster, ServerPlayer target, ConsentKind kind, boolean ordinaryConsent)`.
- Produces: one fixed `OVERRIDE_ENERGY_SURCHARGE = 40` charged at most once per caster/target/kind/server tick.
- Consumes: the registered `artifact_emperyeanjewel` item (display name corrected to Empyrean Jewel).

- [ ] **Step 1: Write failing policy tests proving self-access, safe-zone precedence, ordinary consent, all five consent categories, insufficient energy, duplicate-jewel non-stacking, and per-tick deduplication.**

- [ ] **Step 2: Run the tests and confirm the override service is absent.**

- [ ] **Step 3: Route TELEPORT, LOCATOR, COMPANION, DREAMWALK, POSSESSION, and forced-player movement through the service after safe-zone checks.**

```java
if (ordinaryConsent) return true;
return ConsentOverrideRuntime.authorize(caster, target, kind, false);
```

- [ ] **Step 4: Consume exactly 40 existing energy, synchronize the HUD, emit a bounded violet-gold mark and sound, notify the target, and record a diagnostic outcome.**

- [ ] **Step 5: Add live tests for every consent method, safe-zone refusal, insufficient energy, and testing-mode energy bypass.**

- [ ] **Step 6: Run focused tests and commit `feat: let empyrean jewel override player consent`.**

### Task 7: Energy-Only Artifact Roles

**Files:**
- Create: `src/main/java/com/powers/item/ArtifactRole.java`
- Create: `src/main/java/com/powers/item/ArtifactRoleCatalogue.java`
- Create: `src/main/java/com/powers/item/ArtifactEnergyReservoir.java`
- Modify: `src/main/java/com/powers/PowersDataComponents.java`
- Modify: `src/main/java/com/powers/item/ImportedArtifactKind.java`
- Modify: `src/main/java/com/powers/item/ImportedArtifactRules.java`
- Modify: `src/main/java/com/powers/item/ImportedArtifactItem.java`
- Modify: `src/main/java/com/powers/item/ImportedArtifactRuntime.java`
- Modify: `src/main/java/com/powers/player/PlayerEnergyStorage.java`
- Modify: `src/main/java/com/powers/ImportedItemRules.java`
- Modify: `src/main/java/com/powers/PowersCreativeTab.java`
- Test: `src/test/java/com/powers/item/ArtifactRoleCatalogueTest.java`
- Test: `src/test/java/com/powers/item/ArtifactEnergyReservoirTest.java`
- Test: `src/test/java/com/powers/item/ImportedArtifactRulesTest.java`
- Test: `src/gametest/java/com/powers/PowersGameTests.java`

**Interfaces:**
- Produces: persistent, synchronized `PowersDataComponents.STORED_ENERGY` with a non-negative bounded codec.
- Produces: `ArtifactRoleCatalogue.role(String texture)` for every visible non-food imported item.
- Produces: `ArtifactEnergyReservoir.payShortfall(ServerPlayer, int)` with deterministic inventory order and atomic stored-energy writes.

- [ ] **Step 1: Write failing catalogue completeness and reservoir debit tests, including stacked/duplicate items, death/reload codec round trips, insufficient aggregate energy, and no partial debit.**

- [ ] **Step 2: Run focused tests and confirm the new role/reservoir APIs are missing.**

- [ ] **Step 3: Rename `SOUL_VESSEL` to `ENERGY_RESERVOIR`; remove drain-and-damage behavior; give soulstone sizes and Soul Matrix explicit energy capacities.**

- [ ] **Step 4: Let `PlayerEnergyStorage.consume` compute a shortfall and atomically draw it from carried reservoirs before refusing the action. Prevent recursive energy payment.**

- [ ] **Step 5: Implement distinct ring/amulet caps, Ritual Dagger health-to-energy conversion, heart specialisations, Bloodstone death retention, Malignember destructive-magic efficiency, celestial focuses, ritual containers, flute control, travel relics, and existing Philosopher's Stone transmutation.**

- [ ] **Step 6: Reclassify `magic_essence_*` as arcane energy dust in display text and roles. Hide redundant asset layers, duplicate trilobite, dormant Infernal, and generic no-role items from normal creative/acquisition while preserving registrations.**

- [ ] **Step 7: Add GameTests for reservoir payment, each active artifact family, and save-safe hidden aliases.**

- [ ] **Step 8: Run focused tests and commit `feat: give imported artifacts energy based roles`.**

### Task 8: Shadow's Authoritative Attempt Journal

**Files:**
- Create: `src/main/java/com/powers/knowledge/MagicFailureReason.java`
- Create: `src/main/java/com/powers/knowledge/MagicAttempt.java`
- Create: `src/main/java/com/powers/knowledge/MagicAttemptJournal.java`
- Create: `src/main/java/com/powers/knowledge/MagicDiagnosticAnswer.java`
- Modify: `src/main/java/com/powers/knowledge/KnowledgeQuery.java`
- Modify: `src/main/java/com/powers/knowledge/KnowledgeService.java`
- Modify: `src/main/java/com/powers/knowledge/BoundedKnowledgeProvider.java`
- Modify: `src/main/java/com/powers/spell/SpellCastingManager.java`
- Modify: `src/main/java/com/powers/power/MagicUseGate.java`
- Modify: `src/main/java/com/powers/magic/runtime/ServerMagicCasts.java`
- Modify: `src/main/java/com/powers/protection/PowerProtection.java`
- Modify: `src/main/java/com/powers/companion/PrivateCompanionManager.java`
- Modify: `src/main/java/com/powers/PowersServerLifecycle.java`
- Test: `src/test/java/com/powers/knowledge/MagicAttemptJournalTest.java`
- Test: `src/test/java/com/powers/knowledge/MagicDiagnosticAnswerTest.java`
- Test: `src/test/java/com/powers/knowledge/BoundedKnowledgeProviderTest.java`

**Interfaces:**
- Produces: a 16-entry, five-minute `MagicAttemptJournal` keyed by player UUID.
- Produces: `MagicAttemptJournal.record(UUID, MagicAttempt)`, `latestFailure(UUID, String query, long tick)`, `forget(UUID)`, and `clear()`.
- Produces: typed reasons including `NO_TARGET`, `INSUFFICIENT_ENERGY`, `COOLDOWN`, `AMETHYST`, `SAFE_ZONE`, `CONSENT`, `RANK_LOCK`, `ALIGNMENT_LOCK`, `WRONG_DIMENSION`, `OUT_OF_RANGE`, `BLOCKED_LINE_OF_SIGHT`, `CHANNEL_INTERRUPTED`, and `SERVER_BUDGET`.

- [ ] **Step 1: Write failing tests for capacity eviction, expiry, named-action matching, exact numeric context, privacy redaction, and deterministic “why didn't that work?” answers.**

- [ ] **Step 2: Run the focused tests and confirm the journal types are absent.**

- [ ] **Step 3: Implement the bounded server-thread journal with immutable string/number facts only; never retain entity references, chat history, or chunk coordinates for remote use.**

- [ ] **Step 4: Record failures at central spell, energy/cooldown, magic-gate, target, travel, and consent boundaries. Record a success when a prepared cast commits.**

- [ ] **Step 5: Resolve diagnostic questions before curated lore/recipes. Include the redacted diagnosis in `KnowledgeQuery`, and make the remote provider preserve the authoritative cause verbatim while only improving surrounding phrasing.**

- [ ] **Step 6: Clear journal state on disconnect and server stop; add one rate-limited private hint after three identical failures.**

- [ ] **Step 7: Run focused tests and commit `feat: let shadow explain failed magic attempts`.**

### Task 9: Resources, Documentation, and Full Acceptance

**Files:**
- Modify: `src/main/resources/assets/powers/lang/en_us.json`
- Modify: `src/main/resources/data/powers/knowledge_entries/grimoires.json`
- Modify: `src/main/resources/data/powers/knowledge_entries/relics.json`
- Modify: `README.md`
- Modify: `docs/MAGIC_ACTIONS.md`
- Modify: `docs/MAGIC_INTERACTIONS.md`
- Modify: `docs/ITEM_CATALOGUE.md`
- Modify: `CHANGELOG.md`
- Modify: `src/main/java/com/powers/testing/GameplayAcceptanceCatalogue.java`
- Modify: `scripts/java_source_audit.json`
- Modify: `scripts/non_item_asset_audit.json`
- Test: relevant resource, documentation, source-quality, and gameplay-acceptance tests.

**Interfaces:**
- Consumes: every production behavior from Tasks 1–8.
- Produces: exact user-facing descriptions and current audit manifests.

- [ ] **Step 1: Add failing documentation/catalogue assertions for twelve active spells, dormant Infernal compatibility, every visible artifact role, energy-only terminology, consent override, and Shadow diagnostics.**

- [ ] **Step 2: Run the focused documentation/resource tests and confirm stale content fails.**

- [ ] **Step 3: Update translations, knowledge entries, README, item catalogue, migration notes, changelog, and generated magic documents; remove active claims for retired spells and essence.**

- [ ] **Step 4: Regenerate Java/non-item manifests only after source and assets stop changing.**

- [ ] **Step 5: Run fresh verification:**

```bash
./gradlew clean test validatePowerResources verifyMagicDocs auditJavaSources auditNonItemAssets
./gradlew runGameTest
./test.sh server
./test.sh client
./test.sh soak
git diff --check
git status --short --branch
```

Expected: zero JUnit failures, zero GameTest failures, successful dedicated/client startup without POWERS crash telemetry, bounded 10/50/100-player soak metrics, valid resources/manifests, and no uncommitted files after the final commit.

- [ ] **Step 6: Commit `docs: publish practical magic and artifact reference`, push `codex/powers-finalisation`, and verify local/remote commit equality.**
