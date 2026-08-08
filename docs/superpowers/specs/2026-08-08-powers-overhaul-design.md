# Powers Mod Stabilization and Expansion Design

**Date:** 2026-08-08  
**Target:** Fabric, Minecraft Java Edition 26.2, Java 25  
**Status:** Approved architecture; detailed design derived from the user's continuous-implementation authorization

## Purpose

Turn POWERS into a stable, multiplayer-safe, lore-driven server mod. Preserve all existing authored content, deliberately leave the custom crystals and selected artifacts without recipes, repair every issue from the full audit, and expand the mod with mindscape realms, vulnerable body proxies, branching rank progression, grimoire spells, counterplay, ancient-magic presentation, and a cohesive HUD.

## Non-Negotiable Constraints

- Do not add recipes for crystals or other custom progression artifacts that intentionally lack them.
- Players remain vulnerable while their consciousness is in a mindscape, astral projection, vessel possession, or teleport-marking state.
- Damage to an abandoned body transfers to its owner; death or invalid state ends the projection safely.
- Existing working-tree changes belong to the user and must be preserved.
- All client-provided actions are untrusted and are revalidated on the server.
- Destructive powers, forced movement, particles, and realm access are configurable for a private multiplayer server.
- Visual design uses original ancient-cosmic magic motifs rather than copying assets, names, dialogue, or exact mechanics from another property.

## Delivery Structure

The work is divided into independently verifiable layers:

1. Stabilization and automated tests.
2. Shared safe ability infrastructure.
3. Vulnerable body proxies and mindscape travel.
4. Branching rank maze and grimoire spells.
5. Counterplay and server configuration.
6. Light/Dark Realm content and assets.
7. HUD and effects redesign.
8. Performance, compatibility, documentation, and release verification.

Each layer must compile and pass its tests before later layers depend on it.

## 1. Stabilization

### Build and resources

- Replace removed 26.2 APIs and pin Loom to the resolved stable 1.17.19 release.
- Correct the macOS JDK launcher and expand resource validation to `.mcmeta`, identifiers, referenced assets, advancement graphs, recipes, loot tables, tags, PNG decoding, and duplicate translation keys.
- Exclude filesystem metadata and remove obsolete unused assets only when their lack of references is proven.
- Replace full vanilla loot-table files with Fabric loot modification callbacks.

### Correctness fixes

- Restore vanilla food consumption semantics while substituting only nutrition/effects for darkness-tagged players.
- Replace shared raycasting with true nearest-hit selection and squared entity range.
- Use the shared raycaster for Light and Dark Crystal targeting.
- Correct HUD texture identifiers and rendering coordinates.
- Correct Cozy Campfire duration, Energy Drain rounding/synchronization, plant targeting, Ice Manipulation scaled endpoints, and complete player collision checks.
- Make skill damage/range scaling explicit and consistent for every applicable ability.
- Replace signed-chat cancellation with supported display/decorating behaviour.

### State ownership

- Persist cooldown deadlines, anchors, active paths, and projection metadata using player attachments or persistent state.
- Every effect, attribute modifier, flight flag, invisibility flag, and game-mode change has an owner token and restores only state owned by POWERS.
- Rerolls turn off retained and removed toggle instances correctly without deleting unrelated potion, beacon, command, or other-mod effects.
- Self-reroll is disabled by default and controlled by server configuration.

## 2. Safe Ability Infrastructure

### Cast pipeline

All regular, crystal, item, and grimoire abilities use a server-side `CastContext` and `CastPolicy` pipeline:

1. Resolve the registered ability from the player's actual slot or held item.
2. Validate player state, cooldown, energy, rank, amethyst, body/projection state, and dimension.
3. Validate target, line-of-sight, range, consent, safe-zone policy, world border, loaded-chunk policy, collision, fluids, and ward/anchor rules.
4. Reserve energy and cooldown.
5. Execute without accepting client authority over identity, item, dimension, or destination.
6. Commit on success or refund cleanly on failure.
7. Synchronize only changed state.

Packets contain requests, not trusted outcomes. Repeated malformed or unauthorized requests are rate-limited and logged.

### Damage

- Register a POWERS damage type/tag for magical ability damage.
- Amethyst blocks only tagged power damage.
- Vanilla magic, freezing, commands, void, administrative damage, and other mods retain their normal behaviour.
- Indirect projectiles retain caster attribution and use rank scaling where intended.

### Movement and block interaction

- A shared safe-destination resolver uses the complete entity collision box, real world border, build-height exclusivity, fluid/hazard policy, loaded-chunk limits, wards, anchors, safe zones, and realm gates.
- Forced movement of another player requires consent unless the server configuration explicitly enables hostile forced movement outside safe zones.
- Companion travel is opt-in and each companion is validated independently.
- Destructive powers use Fabric events and server policy. Block entities are protected by default. Optional terrain damage produces drops and honours claim/protection callbacks.

## 3. Vulnerable Body Proxy and Mindscape System

### Core model

`ProjectionManager` owns one `ProjectionSession` per player. A session records:

- projection type;
- body dimension, position, rotation, pose, equipment/skin identity, health and owner UUID;
- consciousness dimension/entity state;
- start time, maximum duration, return policy and owner token.

The proxy is a custom player-shaped entity rendered with the owner's skin and equipment. It is immobile, has an ancient frozen/sleeping pose and emits state-specific restrained particles. It is not a fake network player and cannot be used to duplicate inventory, scoreboard state, advancements, chat identity, or permissions.

### Vulnerability

- The proxy has an owner-bound health bridge. Valid damage is transferred once to the real player using a guarded damage path that cannot recurse.
- Body death kills or returns the player according to vanilla death rules; logout, server shutdown, dimension unload, proxy loss, or mod error restores the owner at the body position.
- Healing and effects target the real player unless explicitly marked body-compatible.
- The proxy remains attackable, targetable and ward-protectable, providing counterplay without inventory duplication.

### Uses

- Light and Dark Realm travel moves consciousness into a mindscape while the proxy remains in the origin world.
- Astral Projection uses a spectral consciousness form and vulnerable body.
- Vessel Possession leaves a body while the caster controls a validated living host; host death or protection rejection returns the caster.
- Time Shift marking leaves the proxy at the origin, limits scouting radius/time, and performs the final cast through the standard destination validator.
- Only one projection session can exist per player. Starting another cleanly refuses rather than nesting sessions.

## 4. Light and Dark Mindscapes

### Shared principles

- They feel like subjective, unstable spaces rather than ordinary dimensions.
- Terrain is finite around the consciousness, visually masked by fog, particles, sound and distant silhouettes.
- Each visit is seeded by the traveler and current progression, allowing controlled variation without uncontrolled world generation.
- A visible tether/rune periodically reminds the player that a vulnerable body remains behind.

### Light Realm

- Theme: memory, revelation, vows, pale-gold ruins, glass-like stone, suspended arches, luminous roots and choral resonance.
- Content: Echo Shrines, memory motes, non-hostile Witnesses, trial chambers and a Beacon of Names that exposes branching rank choices.
- Hazard: overexposure reveals the traveler's location and drains energy; shadowed sanctuaries provide recovery.
- Assets: original rune stone, luminous glass, memory-root and shrine block textures/models plus ambient particle palette.

### Dark Realm

- Theme: fear, buried desire, sacrifice, violet-black monoliths, impossible stairways, inverted reflections and whispering void growth.
- Content: Veil Altars, bound echoes, hostile Hollowed, temptation trials and corruption bargains.
- Hazard: instability accumulates while separated from the body, producing false paths and stronger enemies.
- Assets: original monolith, veil glass, void-root and altar textures/models plus smoke, glyph and distortion effects.

Neither realm requires new crystal recipes. Access continues through existing narrative items/abilities and server-controlled story progression.

## 5. Branching Rank Maze

- Replace each linear rank list with a directed advancement graph containing junctions and reconvergence points.
- Players may unlock multiple adjacent branches over time, but each choice has explicit prerequisites and cannot silently erase partial progress.
- Titles are assembled from attained nodes and the currently displayed focus, providing many combinations without hard-locking a player into one class.
- Initial branches emphasize Might, Motion, Insight, Wardcraft, Communion, Veil and Dominion; darkness has mirrored but not identical branches.
- A respec is an expensive server-configurable ritual rather than a free command.
- Existing rank progress migrates to the closest equivalent completed nodes without loss.
- Advancement JSON remains the visible journal; authoritative graph state is persisted separately so hiding a path never revokes earned criteria.

## 6. Grimoire Spell System

Every imported grimoire becomes a registered spellbook with one or more server-authoritative spells. The system uses existing items and does not add recipes.

Spell families draw inspiration from occult television tone while using original names and implementation:

- **Celestial Grimoire:** `Soul Compass` locator, authenticated held-item cast, obscured by wards and configurable consent.
- **Deep Grimoire:** `Dimensional Anchor`, binding sigil and temporary anti-portal field.
- **Blood Grimoire:** vitality transfer, tracking mark and costly ward-breaking ritual; never bypasses safe zones.
- **Nature Grimoire:** purification circle, root binding and sanctuary growth.
- **Nether Grimoire:** infernal seal, banishment circle and controlled hellfire with griefing disabled by default.
- **Witch Grimoire:** hex, counterspell and concealment veil.
- **Storm/Arcane variants:** weather sigil, kinetic ward, dispel and ritual amplification as appropriate to registered book names.

Spells use casting time, visible runes, interruption, reagents or energy, cooldowns and server-side validation. Strong rituals telegraph their area and allow opponents to break focus, leave the circle, deploy amethyst, counterspell, or attack the vulnerable caster/body.

## 7. Counterplay and Configuration

Configuration defaults favour persistent-server safety:

- terrain destruction off;
- block-entity destruction always off unless explicitly enabled;
- hostile forced movement on only outside safe zones, with opt-out/consent mode available;
- projection bodies vulnerable;
- ward strength, radius and upkeep configurable;
- particle intensity per client and capped per server tick;
- cooldowns persist through relog;
- dimension and spell allowlists;
- friendly-fire and companion-consent rules;
- safe-zone dimension/radius definitions;
- locator obfuscation/consent and rate limits;
- admin bypass permission level configurable and logged.

Further counterplay includes interruptible casts, line-of-sight breaks, ward occlusion, anchor dispelling, exhaustion after major rituals, instability in mindscapes, detectable magical signatures, and counterspell timing windows.

## 8. HUD and Ancient-Magic Presentation

### HUD

- Replace the three bottom-right rectangles with three compact radial/arched power slots framing a central energy sigil.
- Show icon, key binding, cooldown sweep, toggle state and insufficient-energy feedback without covering the hotbar or playfield.
- Energy has four authored visual states: normal, depleted, amethyst-poisoned and darkness-touched.
- A fifth projection/tether state appears only during mindscape or astral travel.
- Layout scales with GUI scale, screen aspect and accessibility settings; text alternatives and reduced-motion mode remain available.
- Rendering uses GUI sprites and correct 26.2 APIs, not raw OpenGL.

### Effects

- Centralize particle motifs: circles, glyph chains, spirals, seals, tethers, fractures and realm palettes.
- Batch/cap server-originated particles and render repeatable decorative geometry client-side from compact effect packets.
- Strong magic follows anticipation, release and aftermath beats with sound and light cues.
- Ancient magic remains readable in combat: each Force and countermeasure has a distinct palette and shape language.

## 9. Performance and Lifecycle

- Replace global entity scans with bounded spatial queries or explicitly tracked sets.
- Time manipulation affects supported entities in a configured radius and uses reference-counted freeze ownership. It cannot leave `noAI`/`noGravity` behind.
- World/block time is described accurately; unsupported full-world freezing is not simulated through per-tick teleports.
- Delayed work uses deadline queues and persistent cleanup records where entities can survive restart.
- Particle budgets, storm entity limits and per-player packet rate limits are enforced.
- Amethyst proximity uses movement/chunk-aware caching and invalidation rather than repeated full-cube scans.
- State cleanup runs on death, disconnect, respawn, dimension change, server stop and mod reload paths.

## 10. Testing and Acceptance

### Automated

- Pure unit tests for ray distance, energy arithmetic, progression graph, consent/config rules and safe-destination decisions.
- Fabric GameTests for food consumption, custom damage, protected blocks, teleport gates, projection body damage, relog persistence, overlapping freezes, cooldowns, anchors and spell authentication.
- Resource validator for JSON/`.mcmeta`, duplicate keys/translations, models, textures, item definitions, advancements, tags and PNG decoding.
- Dedicated-server startup smoke test proving no client classes load server-side.

### Manual

- Two-client tests for PvP consent, body damage, possession, locator privacy, chat compatibility and projection return.
- Realm traversal at multiple GUI/render distances and reduced-particle settings.
- HUD screenshots at common resolutions/GUI scales and with every energy state.
- Profiling with simultaneous wards, beams, storms, projections and time effects.

### Completion gate

Completion requires:

- clean `./gradlew build`;
- all automated tests pass;
- client and dedicated server start successfully;
- all audit findings are mapped to a verified fix or an explicitly preserved intentional design choice;
- no custom crystal/artifact recipes are introduced;
- no missing resource references or corrupt assets;
- current documentation matches actual behaviour and configuration.

## Failure Handling

Any invalid or interrupted cross-dimensional/session operation returns the player to the recorded body position and restores only POWERS-owned state. Missing dimensions, proxies or entities produce a safe return rather than a stranded spectator. Invalid packets never spend resources. Persistent-state migrations retain the previous data until the new representation has been written successfully.

## Out of Scope

- Copying proprietary assets, exact spell names, dialogue, characters or storylines from Rainbow Quest, Supernatural, The Vampire Diaries, or another mod.
- Adding recipes for deliberately unreleased crystals or artifacts.
- Claiming compatibility with arbitrary claim mods without an installed/API-detected integration; the internal protection API remains available for adapters.
