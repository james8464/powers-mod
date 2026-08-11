# Practical Spells, Artifacts, and Shadow Diagnostics Design

## Objective

Replace the unfocused combat-heavy grimoire catalogue with a smaller practical ritual system, give every retained imported artifact a concrete role, eliminate the parallel essence economy, and let Shadow explain failed magical actions from authoritative server state. Preserve registered identifiers and saved worlds even when gameplay content is retired.

## Chosen approach

Use a deliberately small, fixed spell catalogue rather than rebalancing all existing attacks or adding a free-form programmable system.

- Rebalancing the current catalogue would preserve quantity but not solve the overlap with innate combat powers.
- A programmable ritual language would be flexible but would greatly expand validation, UI, migration, and server-abuse risk.
- The selected catalogue keeps the strongest practical rituals, gives each grimoire a distinct purpose, and makes every outcome explainable and testable.

Spells and artifacts continue to use the existing player energy value. There is no essence balance, essence inventory, or second resource bar.

## Spell catalogue

### Celestial Grimoire

The Celestial school covers discovery, prediction, and world-scale intervention.

1. **Soul Compass** locates players, uniquely named creatures, and valid marked targets through the existing locator interface. Consent still applies unless the caster carries the consent-override artifact.
2. **Augury** reports the current moon phase, weather transition, relevant Light/Dark force pressure, and the next scheduled magical world event when one is knowable. It reports uncertainty rather than inventing unavailable information.
3. **Cartographer's Star** searches the server's registered structures, biomes, shrines, libraries, and POWERS landmarks. It returns a direction, distance, dimension, and registry identifier without teleporting the caster or forcing an unbounded chunk search.
4. **Celestial Ruin** remains the delayed catastrophic cleansing ritual with its existing persistent event and workload budgets.

### Deep Grimoire

The Deep school contains exactly one spell:

1. **Dimensional Anchor** prevents magical displacement and dimensional travel for its configured duration. It retains the existing field, counterplay, visuals, and interaction rules.

Waystone Binding, Threshold Gate, Quarantine Seal, Binding Sigil, Anti-Portal Field, and Kinetic Ward are not part of the resulting spell catalogue.

### Blight Grimoire

The Blight school is practical forbidden knowledge rather than another damage school.

1. **Blood Reading** reports the targeted entity's health, armour, active effects, alignment, relevant immunities, and known magical vulnerabilities. Player privacy/consent applies unless overridden by the designated artifact. Information the server cannot prove is omitted.
2. **Grave Recall** reports the caster's most recent death coordinates and dimension. It never searches the world, restores items, loads the death chunk, or exposes another player's death. If no retained death record exists, it says so clearly.

There is no Essence Distillation spell or essence capture mechanic.

### Wild Grimoire

The Wild school covers restoration and protection.

1. **Purification Circle** removes configured harmful effects, hostile anchors, and possession/soul-link conditions from entities in its bounded area while respecting effect ownership and interaction policy.
2. **Verdant Tending** grows eligible crops, saplings, and plants, restores suitable soil, hydrates farmland, and extinguishes fire within a capped area and per-tick block budget.
3. **Hearth Sanctuary** applies the same sacrificial forcefield contract as the innate Forcefield power to every living entity within three blocks of the cast centre. Each protected entity receives an independent bounded durability pool. A hit that exceeds remaining durability destroys that entity's field but deals no overflow damage from that hit. The spell does not inherit innate rank scaling.

### Infernal Grimoire

The Infernal school and its spells are removed from active gameplay, acquisition, menus, generated spell documentation, and creative exposure. The registered grimoire and spell identifiers remain internal, hidden compatibility aliases so existing worlds do not lose or corrupt item stacks. A legacy Infernal Grimoire cannot cast and explains that the school is dormant when an old stack is used.

### Abyssal Grimoire

The Abyssal school contains exactly two spells:

1. **Ward-Breaking Ritual** suppresses eligible amethyst and magical wards for a bounded duration.
2. **Dispel** removes an eligible nearby hostile field or effect through the existing ownership and priority rules.

Counterspell, Ritual Amplification, and Unbinding are not part of the resulting catalogue.

## Compatibility and migration

Registered item identifiers are never hard-deleted from a released namespace. Retired spell selections migrate deterministically:

- Celestial Tracking Mark and Weather Sigil map to Augury; invalid Celestial selections fall back to Soul Compass.
- Every retired Deep selection maps to Dimensional Anchor.
- Blight utility/curse selections map to Blood Reading, with any grave/death lineage mapping to Grave Recall.
- Wild Root Binding maps to Verdant Tending and Sanctuary Growth maps to Hearth Sanctuary.
- Retired Abyssal counter/boost selections map to Dispel, while ward-breaking lineage remains Ward-Breaking Ritual.
- Infernal selections become dormant and do not silently grant access to another school.

Saved cooldown entries for retired spells are ignored and removed lazily. Menu indices are reconciled on load and then saved in canonical form. Migration is idempotent.

## Consent-override artifact

The **Empyrean Jewel** is the automatic consent-override artifact. Carrying one anywhere in the player's inventory allows all otherwise-valid consent-gated player interactions:

- teleporting to or moving another player;
- locating or observing another player;
- bringing a companion through travel;
- dreamwalking;
- possession;
- other forced movement routed through the central protection policy.

The jewel overrides consent only. Server safe zones, administrative restrictions, invalid targets, rank locks, cooldowns, energy requirements, realm confinement, and amethyst mechanics remain independent checks.

An override adds a single documented energy surcharge to the original action rather than introducing charges or essence. The surcharge is calculated and charged atomically with the action; insufficient energy prevents the override without partially applying it. Testing-mode energy bypass applies consistently. The affected player receives a concise action-bar warning, an ancient magical sound, and a bounded visible mark identifying that consent was overridden. Duplicate jewels do not stack or reduce the surcharge.

All consent decisions continue to pass through `PowerProtection`; individual powers may not implement private bypasses.

## Artifact purposes

Retained imported artifacts receive distinct, documented behavior:

- **Soulstones, Mini Soulstones, and the Soul Matrix** become energy reservoirs. They store only the existing magic-energy unit and automatically provide energy when an action would otherwise fail. Capacity and transfer limits prevent inventory size from multiplying per-tick work.
- **Rings and amulets** receive individual attunements such as regeneration, channel stability, ward durability, Wild-area efficiency, or corruption resistance. Their effects are capped and do not stack without limit.
- **Ritual Dagger** converts a bounded amount of health into ordinary magic energy for the current ritual, with lethal sacrifice and repeated-use protections.
- **Heart relics** specialise in vitality, nature restoration, magical recovery, or necromantic resistance without creating a new resource.
- **Philosopher's Stone** keeps controlled, recipe-driven transmutation with explicit inputs, energy costs, and block-protection checks.
- **Lodestone, Oddstone, and Miniportal** bind and use the existing travel-anchor system; they do not restore the retired Deep portal spells.
- **Flute** commands, recalls, calms, or retargets eligible allied magical creatures.
- **Bloodstone** records the owner's latest death imprint and improves Grave Recall retention; it stores no essence.
- **Malignember** acts as an energy-efficient focus for fire and destructive magic rather than fuel in a separate inventory.
- **Stars and celestial jewels** focus Augury and Cartographer's Star, narrowing or extending valid searches within hard server budgets.
- **Bowls, small pots, and dripping orbs** become ritual containers that reduce the energy cost of compatible non-combat spells.
- **Coins and bullion** become Archivist and realm-faction offerings/currency.
- **Fossils, figurines, salts, pages, pearls, dusts, and stones** receive explicit archaeology, Arcane Crucible, spell-focus, ward, or lore functions.

Items whose visuals or identifiers are redundant after this pass are removed from acquisition, creative display, and normal documentation but retained as hidden save-safe aliases. Existing items whose names contain “essence” are renamed in display text and repurposed as arcane dusts or energy-related materials; their registry identifiers remain stable for old saves. Intentionally recipe-less crystals and user-deferred recipes remain untouched.

Every retained visible gameplay item must have a catalogue row stating behavior, acquisition, energy interaction, and save/migration status. Generic descriptions such as “lore relic” or “crucible catalyst” are not sufficient proof of purpose.

## Shadow action awareness

Shadow observes authoritative gameplay attempts, not the player's screen. A bounded `MagicAttemptJournal` records structured outcomes for the owner:

- action identifier and cast source;
- success, interruption, or typed failure reason;
- target type and relevant registry identifiers;
- dimension and bounded contextual facts;
- energy required/available, remaining cooldown, rank/alignment lock, and missing reagent or item;
- consent override, safe-zone, amethyst, realm, targeting, range, and line-of-sight decisions;
- channel interruption and server-work-budget outcomes.

Central gateways record these facts once: spell casting, innate/crystal/artifact dispatch, targeting, travel, consent protection, cooldown/energy payment, and channel completion. Individual powers do not compose natural-language explanations.

The journal is session-bounded, limited to a small ring buffer, expires stale entries, and is cleared on disconnect. It performs no periodic world scan. A question such as “Shadow, why didn't that work?” resolves the newest relevant failed attempt; naming an ability resolves the newest matching attempt. The deterministic offline answer states the exact cause and a valid corrective action using translated text and live numeric values.

Optional remote AI receives only a redacted structured diagnosis and may improve phrasing. It cannot replace, contradict, or invent the authoritative cause, recipes, permissions, coordinates, or progression requirements. Shadow can offer one restrained hint after repeated identical failures, protected by a per-player cooldown. Existing global reveal/hide and chat visibility rules remain unchanged.

## Performance and correctness boundaries

- Structure/biome searches use Minecraft's bounded locator facilities and never synchronously fan out across arbitrary chunks.
- Verdant Tending and forcefield application use hard block/entity caps and staggered visual packets.
- Artifact inventory checks are cached or performed at action boundaries rather than scanning every slot for every ticked subsystem.
- Energy-reservoir transfers are atomic and cannot duplicate energy through death, dimension change, stacking, or save migration.
- The consent override cannot bypass safe zones or create a partially paid action.
- Shadow journals store identifiers and bounded facts rather than entity references or forced chunks.

## Verification

Implementation follows test-driven development and requires:

1. deterministic catalogue and migration tests for every retained and retired spell;
2. GameTests for every retained spell's success, failure, ownership, area, and environmental behavior;
3. consent tests for every gated interaction with no jewel, a carried jewel, insufficient energy, safe zones, and testing bypasses;
4. energy-reservoir duplication, stacking, death, reload, and transfer tests;
5. artifact-purpose and acquisition coverage for every visible custom item;
6. Shadow journal tests for exact failure classification, matching, expiry, privacy redaction, and offline answers;
7. dedicated-server boot, client/resource validation, generated documentation, and bounded multiplayer soak tests;
8. a clean full build and clean synchronized Git worktree before completion.

README and generated documentation will describe only behavior proven by the corresponding tests. Removed gameplay is documented as a save-safe migration rather than claiming that historical registry identifiers disappeared.
