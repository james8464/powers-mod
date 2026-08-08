# Magic Quality, Interaction, Presentation, and Rank Design

**Date:** 2026-08-08

**Target:** Fabric, Minecraft Java Edition 26.2, Java 25

**Status:** Approved through the user's explicit authorization to make the best autonomous design decisions

## Purpose

Give POWERS a second production-quality pass across all Java source, gameplay interactions, progression, audiovisual presentation, and non-item assets. The completed system must be maintainable rather than a collection of special-case patches: every magical action has a typed definition, every possible pair of actions resolves deterministically, every rank choice changes play, and every important state is readable through original ancient-magic visuals and sound.

## Constraints

- Preserve Minecraft 26.2, Fabric Loader, Java 25, server authority, and private-multiplayer safety.
- Do not add recipes for crystals or deliberately unreleased progression artifacts.
- Keep mindscape and projection bodies vulnerable.
- Preserve existing player data and migrate rank data without loss.
- Use professional comments: document public contracts, ownership, invariants, threading, validation boundaries, and non-obvious algorithms. Do not narrate trivial statements.
- Create original assets and motifs; do not copy art, sound, spell text, or exact mechanics from other properties.
- Respect particle budgets, reduced-motion settings, safe zones, consent, amethyst, and protection policy.

## Approaches Considered

### Independent patches

Adding rank checks, particles, sounds, and collision rules inside every ability is initially quick but produces hundreds of duplicated branches. It cannot prove that a new power has considered every old power and makes future balance changes unsafe.

### Complete rewrite

Replacing the established mod with an entity-component or scripting framework offers a clean theoretical model but risks player-data loss, version regressions, and months of revalidation. It discards working server-safety code without a proportional benefit.

### Typed magic kernel with incremental migration — selected

Keep each power's authored behaviour but route casting, scaling, residues, interactions, and presentation through small typed services. Definitions are data-oriented and testable without a running client. Existing entry points migrate in coherent tranches, so every commit remains buildable and stored data stays compatible.

## 1. Code Quality and Documentation

### Package boundaries

The source tree gains explicit packages with one responsibility each:

- `com.powers.magic`: action identity, aspects, descriptors, contexts, scaling, interaction outcomes, and the resolver.
- `com.powers.magic.runtime`: active fields/residues, spatial indexing, lifecycle cleanup, and server orchestration.
- `com.powers.magic.fx`: audiovisual cue definitions and compact client-bound effect payloads.
- `com.powers.progression`: rank graph, typed perks, aggregation, migration, and player-facing titles.
- `com.powers.client.fx`: client effect choreography and reduced-motion rendering.
- `com.powers.client.screen`: textured, accessible teleport, locator, and rank-maze views.

Large bootstrap classes are decomposed only where responsibilities are demonstrably mixed:

- `PowersMod` retains initialization order and delegates tick, lifecycle, storm, and passive-effect work.
- `PowersPackets` retains payload registration and delegates validation/handling by request family.
- `PlayerPowers` retains the stable attachment facade and delegates rank, consent, energy, cooldown, and serialization operations to focused value/services.
- `TeleportAbility` delegates marking sessions, storm travel, and destination execution.

### Documentation standard

- Every public type has class-level Javadoc describing its role and ownership.
- Every public or protected method whose contract is not a standard override documents parameters, return meaning, failure behaviour, side effects, and thread assumptions where relevant.
- State managers document creation, mutation, expiry, disconnect, death, and shutdown cleanup.
- Packet handlers document which fields are untrusted and where they are revalidated.
- Comments explain why an invariant exists, never merely repeat the next statement.
- `package-info.java` describes each source package.
- A source-quality test rejects unfinished-work markers, debug output, undocumented public types, accidental wildcard imports, and newly oversized responsibility classes.
- Compilation retains `-Werror`, deprecation, and unchecked warnings; Javadocs are generated with doclint for the new public kernel.

## 2. Canonical Magic Catalogue

Every castable or suppressing action receives a stable `MagicActionId` and `MagicActionDefinition` containing:

- origin: innate power, crystal, spell, or amethyst;
- aspects: flame, frost, storm, force, motion, gravity, time, space, mind, soul, life, light, darkness, void, protection, concealment, creation, or suppression;
- delivery: instant, beam, projectile, field, aura, channel, toggle, travel, or projection;
- intent: harm, control, movement, defence, support, information, or world interaction;
- base potency, range, duration, energy, cooldown, residue duration, and interaction priority;
- audiovisual signature and accessibility cue;
- protection, consent, terrain, line-of-sight, and target policy.

The catalogue contains exactly:

- 27 innate powers;
- 13 distinct crystal abilities;
- 20 grimoire spells;
- three amethyst sources: carried/item suppression, tagged-block suppression, and powered-ward suppression.

That produces 63 action definitions and 2,016 unordered interaction pairs when same-action collisions are included. A generated report lists every one of the 2,016 pairs. A test fails if an action is missing, duplicated, or resolves to an undefined outcome.

## 3. Interaction Resolution

### Runtime model

`MagicInteractionResolver` is pure and deterministic. It consumes two definitions plus environmental context and produces an `InteractionResolution`:

- outcome: coexist, resonate, amplify, dampen, contest, cancel, reflect, shatter, transform, consume, or destabilize;
- potency/duration/range multipliers for each side;
- optional replacement aspect or status;
- whether a projectile is redirected, a field is shortened, travel is blocked, or a channel is interrupted;
- the audiovisual reaction cue.

Resolution order is explicit:

1. Exact action-pair rule.
2. Suppression/protection rule.
3. Opposed-aspect rule.
4. Same/aspected resonance rule.
5. Delivery/intent rule.
6. Safe default coexistence rule.

The final default is still an accounted result, not an omission. It produces a restrained harmonic weave cue only when two effects genuinely overlap, preventing noise from unrelated casts.

### Spatial and temporal collision detection

`ActiveMagicIndex` stores bounded `MagicPresence` records by dimension and chunk cell. A presence may be anchored to a position, entity, projectile, player, or field and always has an expiry tick. Nearby queries inspect only intersecting cells.

All three cast pipelines—innate powers, crystals, and spells—call the same coordinator:

1. Validate the original cast.
2. Build an immutable `MagicCastContext`.
3. Query active presences and amethyst sources.
4. Resolve and apply pre-cast adjustments or cancellation.
5. Execute the ability.
6. Publish a bounded presence/residue.
7. Emit one deduplicated reaction cue per resolved pair.

Instant actions leave a short residue, so sequential collisions such as frost striking fresh hellfire are still resolved. Persistent fields, beams, projectiles, toggles, projections, time effects, bodies, and wards update or unregister their presence through explicit owner tokens. Disconnect, death, dimension change, entity removal, and server stop clean every presence.

### Hand-authored high-impact reactions

Aspect rules account for every pair; the following families receive distinctive mechanics:

- Flame + frost becomes steam: both damage types soften, vision is briefly obscured, fire is extinguished, and a white-blue pressure ring expands.
- Flame + life becomes wildfire growth: hostile flame shortens while healing becomes an outward cleansing pulse.
- Flame + storm becomes plasma: lightning chains through burning targets with a louder violet-white discharge.
- Frost + motion/gravity becomes brittle momentum: strong impacts shatter ice for bonus stagger rather than indefinite control.
- Storm + water/frost gains conduction but a grounded ward or amethyst diverts the strike visibly into the earth.
- Light + darkness contests by potency and focus rank; ties form an eclipse field that dampens both and reveals concealed players.
- Void + light tears into a temporary rift that consumes projectiles but cannot bypass safe-zone or terrain policy.
- Time + time uses shared ownership and priority; equal stops resonate, opposing acceleration destabilizes duration, and no entity is permanently frozen.
- Time + motion stores and releases momentum instead of applying unsafe repeated teleports.
- Space/travel + dimensional anchor or anti-portal field cancels travel before energy/cooldown commit and draws visible chains to the blocking source.
- Mind/projection/possession + amethyst snaps or weakens the tether and returns the player safely; it never strands spectator state.
- Mind + concealment obscures locator information; insight and tracking contest it without leaking exact coordinates on failure.
- Soul link + damage divides only once through a recursion guard; purification can sever it, vitality transfer can resonate, and ward breaking cannot duplicate damage.
- Healing/life + hex/exhaustion purifies by contest rather than blindly overwriting unrelated vanilla effects.
- Forcefield/kinetic ward + projectile reflects eligible magical projectiles with ownership reassignment and a finite reflection count.
- Forcefield + beam absorbs shield integrity and visibly fractures; it never simulates invulnerability with maximum Resistance.
- Gravity/telekinesis + anchor changes forced movement into stagger/impact while preserving consent and protection rules.
- Invisibility/concealment + area damage leaves a temporary magical silhouette so stealth has counterplay.
- Creation/clones/summons + banishment removes only POWERS-owned ephemeral entities and emits a return seal.
- Size shift + collision-sensitive movement revalidates the full destination box and safely rolls back scale if no space exists.
- Major crystal + amethyst creates a tiered suppression contest: carried amethyst dampens, tagged blocks strongly resist, and powered wards can shatter or repel according to configured strength.

## 4. Audiovisual Language

### Shared choreography

Each action and interaction uses an `FxSequence` with anticipation, release, impact, and aftermath beats. A sequence references compact geometry instructions rather than sending every particle from the server. Palettes and shapes identify mechanics:

- flame: angular embers and expanding broken circles;
- frost: sixfold crystalline rings and falling shards;
- storm: forked lines, ion motes, and sharp white cores;
- time: clock arcs, suspended dust, and reverse spirals;
- space: nested portals and parallax star motes;
- mind/soul: tethers, double silhouettes, and heartbeat pulses;
- light: radial geometry, gold-white rays, and ascending motes;
- darkness/void: inward spirals, violet fractures, and eclipsed cores;
- life: root curves, leaf motes, and green-gold waves;
- amethyst: faceted hexagons, lavender cracks, and resonant chimes.

Pair reactions deterministically combine both palettes, glyph seeds, and sound intervals. Therefore all 2,016 pairs have a distinct, reproducible presentation without storing 2,016 hand-drawn animations.

### Performance and accessibility

- The server sends one compact effect event and never particle geometry loops to every client.
- Clients generate particles locally within distance, density, and reduced-motion limits.
- Effects are deduplicated by pair/cell/tick and retain the existing global server budget for server-owned particles.
- Important cues include shape and sound as well as colour.
- Reduced motion replaces spirals/flashes with static rings and restrained opacity.
- Screen overlays avoid opaque flashes and expose an intensity configuration.

### Sound bank

Original procedural sound assets provide low rune hums, crystal resonance, amethyst fracture, temporal suspension/release, rift opening/closing, soul tether, light chorus, dark whisper, ward impact, rank awakening, and interaction clash. Sounds are short Vorbis `.ogg` files registered under POWERS and assigned correct sound categories.

## 5. Non-Item Asset Pass

All existing non-item textures, block models, blockstates, animation metadata, language references, and icons are decoded, referenced, visually inspected through contact sheets, and validated.

New original assets include:

- 18×18 icons for Exhaustion and Amethyst Poisoning;
- a scalable HUD sprite family for normal, empty, darkness, amethyst, projection, cooldown, toggle, insufficient-energy, and interaction states;
- teleport screen frame, coordinate/player modes, destination runes, validation/error states, and focus treatment;
- celestial locator frame, soul rows, consent-obscured state, and selection treatment;
- a rank-maze screen with light/dark backgrounds, branch sigils, connection lines, locked/unlocked/focused node states, perk details, and keyboard/narration support;
- advancement backgrounds aligned with the new light and darkness progression art;
- interaction overlay glyphs, projection tether vignette, and reduced-motion variants;
- particle sprites for mote, shard, glyph, ribbon, spark, eclipse, root, and fracture motifs;
- custom sound assets and `sounds.json` registrations.

HUD and screen layout uses GUI sprites and nine-sliced/scalable regions where supported. Logic and geometry remain testable separately from rendering. Screens use translatable text, accessible narration, predictable tab order, Escape/cancel, input validation, and common GUI-scale/aspect-ratio bounds.

## 6. Power Improvements

All powers gain rank-aware values, consistent cast beats, a signature, interaction presence, and explicit counterplay. Unique improvements are:

- **Slow World:** a visible bounded time bubble, stored/released momentum, owner-safe overlap, and rank-scaled radius/duration.
- **Time Shift:** textured destination preview, reusable personal mark, safe companion consent, anchor chains, and higher-rank mark stability.
- **Shadow Step:** a validated behind-target step, fading afterimage, brief reveal on impact, and optional rank-gated second step.
- **Flight:** authored takeoff/landing bursts, controlled aerial dash, fall rescue, and turbulence near suppression fields.
- **Elemental Blast:** environment-aware flame/frost/storm selection and genuine reaction mechanics.
- **Starfall:** visible impact telegraphs, bounded staggered stars, ward interception, and rank-scaled count without entity spam.
- **Void Beam:** short charge, finite penetration, light/ward contests, and a lingering void scar that cannot damage terrain by default.
- **Fireball:** charge tiers, owner-safe projectile state, frost/ward collision, finite reflection, and controlled terrain policy.
- **Frost Nova:** an expanding wave, brittle status, steam reaction, and safe temporary surface frost.
- **Lightning Strike:** wet-target conduction, grounded ward diversion, finite chains, and clear sky-to-target telegraph.
- **Ground Slam:** fall-height charge, radial wave timing, brittle shatter, and protected-block-safe debris.
- **Speed Burst:** directional burst, afterimage trail, collision-safe impact wave, and motion-rank recovery.
- **Telekinesis:** sustained target tether, aim-based throw, magical-projectile interception, consent, and mass limits.
- **Energy Beam:** readable charge/overheat, shield integrity damage, finite piercing, and release recoil.
- **Super Speed:** eased acceleration, step-up/fall safety, wake particles, and time-field momentum interaction.
- **Breezy Bash:** aimed cone, projectile redirection, fall cushioning, and gravity interaction.
- **Cozy Campfire:** a visible sanctuary field that heals allies, cleans minor harmful residues, and weakens hostile frost without placing permanent fire.
- **Invisibility:** phased fade, configurable break-on-attack, detectable magical residue, and insight-rank counterplay.
- **Time Freeze:** bounded visible frontier, reference-counted ownership, contest rules, safe release, and immunity feedback.
- **Forcefield:** real finite shield integrity, directional hit fractures, projectile reflection limits, collapse backlash, and wardcraft scaling.
- **Gravity Displacement:** aim-selected pull/push/collapse modes, anchor conversion to stagger, and safe velocity caps.
- **Vessel Possession:** clear body/host tethers, host health and consent limits, suppression return, and no inventory/identity duplication.
- **Astral Projection:** readable vulnerable-body tether, boundary feedback, controlled phase interaction, and safe forced return.
- **Energy Drain:** breakable line-of-sight channel, visible energy transfer, exhaustion contest, and capped conversion.
- **Ice Manipulation:** targeted lance/wall/path modes, collision-safe temporary constructs, brittle synergy, and melting cleanup.
- **Plant and Healing Acceleration:** a bounded growth wave, ally-aware healing, corrupted-life reaction, and no block duplication.
- **Double Health:** owned attribute modifiers, visible heart sigil, proportional expiration, healing limits, and rank-scaled resilience rather than raw invulnerability.

Crystal abilities and all twenty spells receive the same descriptor, scaling, presence, interaction, FX, protection, and cleanup treatment.

## 7. Impactful Rank Maze

### Typed perks

Every one of the 56 rank nodes defines one or more typed perks. Perks aggregate from all unlocked nodes; the focused node grants a stronger focus effect. Modifiers use explicit caps:

- magnitude, range, duration, channel stability, shield integrity, healing, control strength, interaction priority;
- energy capacity/regeneration, cast cost, cooldown recovery;
- magical resistance, knockback resistance, movement, concealment, detection, and projection tether;
- spell, crystal, power-family, and aspect-specific affinity.

The seven branch identities are meaningful:

- Might improves impact, damage, shatter, and shield pressure.
- Motion improves movement, range, travel stability, and cooldown flow.
- Insight improves targeting, detection, interaction knowledge, and duration.
- Wardcraft improves shields, suppression resistance, channel stability, and ally defence.
- Communion improves healing, soul effects, projections, and cooperative resonance.
- Veil improves concealment, mind effects, escape, and residue decay.
- Dominion improves fields, crystal control, spell scale, and interaction priority.

Light perks favour efficiency, protection, cleansing, cooperation, and stable control. Darkness perks trade safety for stronger harm, drain, concealment, corruption, and risky interaction dominance. No path is mutually exclusive: players can weave across open parent nodes, while focus and caps prevent unrestricted stacking.

### Player impact

Rank changes are visible and tangible:

- energy capacity and regeneration use aggregated perks instead of only the old numeric ladder;
- abilities scale through a single `PowerScalingService` rather than ad hoc calls;
- focused branch supplies a restrained aura, nameplate glyph, HUD sigil, and rank-awakening sequence;
- threshold perks unlock mechanical variants such as an extra chain, reflection, second step, purification, or safer return;
- resistance never blocks vanilla damage categories accidentally;
- all attribute modifiers use stable POWERS-owned identifiers and are removed precisely on respec, tag change, death/respawn migration, or reload.

The rank screen explains exact bonuses and their caps. Commands remain as an accessible fallback.

## 8. Tests and Generated Evidence

### Pure tests

- catalogue count, ID uniqueness, descriptors, values, and action coverage;
- all 2,016 unordered pairs resolve and have a non-null cue;
- resolver priority, symmetry/asymmetry declarations, exact family reactions, and safe defaults;
- spatial-index insertion, movement, expiry, entity ownership, and cleanup;
- scaling aggregation, caps, focus bonus, light/dark differences, migration, and respec removal;
- each of 27 innate powers has a descriptor, signature, rank scaling, counterplay, and improvement declaration;
- screen geometry at common resolutions/scales and energy state selection;
- source documentation and banned-shortcut audit.

### Runtime tests

- dedicated-server registration and all action catalogues;
- representative collision families: fire/frost, light/dark, time/time, travel/anchor, mind/amethyst, projectile/ward, healing/hex, soul/purification, and size/collision;
- lifecycle cleanup on death, logout, dimension change, and stop;
- two overlapping high-particle encounters remain within budgets;
- data migration retains rank and power state.

### Asset evidence

- strict resource validation resolves every GUI, effect, particle, sound, model, texture, animation, and advancement reference;
- PNG dimensions/alpha and OGG Vorbis decoding are checked;
- contact sheets are rendered and visually inspected for every non-item asset;
- client resource reload emits no POWERS warnings;
- HUD/screens are captured at representative GUI scales and states when the development client can be automated.

Generated documentation includes:

- `docs/interactions/action-catalogue.md`;
- `docs/interactions/interaction-rules.md`;
- `docs/interactions/interaction-matrix.csv` with exactly 2,016 data rows;
- `docs/quality/code-audit.md` mapping every Java file to its audit result;
- `docs/quality/asset-audit.md` mapping every non-item asset to validation and visual-review evidence;
- updated README controls, rank perks, counterplay, visuals, and configuration.

## 9. Delivery Tranches

1. Source-quality rules, documentation, and responsibility refactors.
2. Canonical action catalogue and generated exhaustive matrix.
3. Runtime presence index and cast-pipeline integration.
4. Rank perk model, migration, scaling, attributes, and rank screen.
5. Shared effect protocol, client choreography, particle/sound assets.
6. HUD, teleport, locator, advancement, effect-icon, and non-item asset redesign.
7. Per-power improvement migrations and exceptional interaction mechanics.
8. Full resource/client/server/performance verification and documentation.

Each tranche begins with failing tests, ends with a focused commit on `main`, and keeps the worktree clean between commits.

## Completion Gate

Completion requires direct evidence for all user objectives:

- every tracked Java file appears in the code-audit manifest and all actionable findings are fixed;
- professional documentation/source checks pass without blanket suppressions;
- every tracked non-item asset appears in the asset-audit manifest and all references/rendering checks pass;
- exactly 63 actions and 2,016 same-or-cross-action pairs are enumerated and resolved;
- every pair has deterministic mechanics and an audiovisual cue, with exceptional families demonstrated by tests;
- all 27 innate powers, 13 crystal abilities, and 20 spells use descriptors, rank scaling, interaction presence, protection, and presentation;
- every rank node has a perk and rank changes affect powers and players safely;
- `./gradlew clean test build` passes, client resources reload cleanly, an isolated dedicated server starts/stops cleanly, generated docs match registries, and the worktree is committed and clean on the sole `main` branch.
