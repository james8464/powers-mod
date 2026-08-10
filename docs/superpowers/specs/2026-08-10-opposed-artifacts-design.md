# Opposed Artifacts Design

**Date:** 2026-08-10  
**Status:** Approved

## Shared Rules

The Shadow Sword and Heavenly Partisan are mythic artifacts. Both:

- carry a registered, hidden `ArtifactIdentity` data component;
- ignore durability loss through both an unbreakable component and defensive item logic;
- survive fire, lava, ordinary explosions, item-entity aging, anvils, grindstones, enchanting, and the Arcane Crucible;
- expose right-click cast and sneak-right-click radial/list menu controls;
- may route all registered innate and crystal actions through an alignment-themed presentation layer;
- validate every cast through the original action's server rules and artifact-specific authorization;
- use darkness or light energy rather than bypassing energy;
- emit only one or two subtle, budgeted inventory particles every ten ticks;
- store menu selection server-side and synchronize only on change;
- have no crafting recipe unless a future story update explicitly adds one.

Artifact routing does not grant rank scaling to crystal powers or spells. Routed player powers use the wielder's applicable rank. Max darkness rank removes all Shadow Sword action cooldowns, including unique artifact actions, but never their costs, activation times, telegraphs, state caps, or validation.

## Shadow Sword

### Authorization and curse

Only a darkness-tagged player can use it. A non-dark carrier receives hidden-particle Blindness and Wither, cannot select or cast actions, and triggers a rate-limited lightning arrival of darkness guardians. Guardians target the unauthorised carrier and never the authorised owner. Dropping or removing the sword ends new curse applications; existing short effects expire naturally.

At maximum darkness rank, the wielder receives **Abyssal Apotheosis**:

- no artifact-action cooldowns;
- 90% faster darkness-energy refill while carried;
- reduced but non-zero ultimate costs;
- enhanced dark palette and a crown/sigil cue that remains readable but not constant screen clutter.

### Unique darkness actions

| Action | Unlock | Cost | Effect and counterplay |
|---|---:|---:|---|
| Call the Hollowed | 1 | 18 | Summons up to four darkness guardians; owner and global caps apply. |
| Blight Ground | 2 | 20 | Converts a capped disc beneath the wielder to darkness blocks through queued spread work. Claims and block entities are protected. |
| Umbral Step | 3 | 12 | Short safe teleport through a shadow path; wards, anchors, border, and collision apply. |
| Night Chain | 4 | 25 | Tethers one living target, pulling and suppressing movement while line of sight remains. |
| Eclipse Wave | 5 | 32 | Wide cone of high damage and projectile erasure; clearly telegraphed. |
| Abyss Gate | 6 | 40 | Temporary paired dark portals within loaded safe destinations; capped one pair per owner. |
| Devour Light | 7 | 35 | Removes nearby ordinary light sources and light magic, converts a small legal area, and restores energy. Never destroys block entities. |
| Black Decree | 8 | 55 | Marks one hostile living target for five seconds, then deals 22% max-health magic damage plus rank damage, capped at 400 against players and 2,000 against non-players. Breaking line of sight, entering a ward, amethyst suppression, or killing the caster cancels it. |
| Event Horizon | 8 | 60 | Eight-second singularity that pulls hostiles, consumes projectiles, and pulses capped damage. One per owner, four server-wide. |
| Deathless Night | 9 | 80 | Arms a five-minute single-use death ward. On lethal damage it leaves one health, grants two seconds of invulnerability, restores 35% health and 40 energy, and consumes itself. Void/admin damage bypasses it. |
| Legion of the Eclipse | 10 | 100 | Cosmic telegraph followed by two elite darkness guardians and a temporary darkness dominion. One legion per owner. |

No action deletes arbitrary entities, bypasses invulnerability/claims, or performs unbounded block edits. The immense power comes from high damage, strong control, repeatability at rank ten, and coordinated effects rather than unsafe server operations.

## Heavenly Partisan

### Authorization and judgement

Only a non-dark player can use it. A darkness-tagged carrier receives hidden-particle Glowing and Smite-like periodic radiant damage, cannot use the menu/actions, and triggers Radiant Sentinels in a lightning-and-gold arrival. Sentinels attack the darkness-tagged carrier and darkness creatures while protecting valid light-aligned wielders.

At maximum normal rank, the wielder receives **Empyrean Ascendance**:

- artifact cooldowns are reduced by 60%, not removed;
- 75% faster normal-energy refill while carried;
- enhanced gold-white presentation;
- stronger healing and ally protection.

The asymmetry is deliberate: darkness is the dangerous, cooldown-free route; light is safer, defensive, and group-oriented.

### Unique light actions

| Action | Unlock | Cost | Effect and counterplay |
|---|---:|---:|---|
| Call the Radiant | 1 | 18 | Summons up to four Radiant Sentinels with owner/global caps. |
| Consecrate Ground | 2 | 20 | Converts a capped disc beneath the wielder to pure-light blocks through queued work. |
| Dawnstride | 3 | 12 | Safe beam-step teleport; normal travel wards and collision apply. |
| Covenant Chain | 4 | 25 | Links an ally for damage sharing/healing or binds a hostile darkness creature. |
| Daybreak Wave | 5 | 32 | Wide cone that damages hostiles, purifies removable debuffs, and heals allies at reduced strength. |
| Heaven Gate | 6 | 40 | Temporary paired radiant portals at legal destinations; one pair per owner. |
| Banish Darkness | 7 | 35 | Purifies darkness blocks and magic within a capped area, restoring energy for successful conversions. Adjacent opposed blocks retain their established catastrophic reaction. |
| Divine Decree | 8 | 55 | Five-second marked judgement: 18% hostile max-health radiant damage, capped as Black Decree, or a strong heal/absorption if aimed at an ally. Wards, LOS, amethyst, or caster death cancel it. |
| Solar Firmament | 8 | 60 | Eight-second field that pushes hostiles/projectiles away, heals allies, and suppresses darkness regeneration. One per owner, four server-wide. |
| Second Dawn | 9 | 80 | Arms a five-minute single-use death ward with the same hard bypasses; restores 45% health and grants absorption instead of energy. |
| Host of Heaven | 10 | 100 | Cosmic telegraph followed by two elite sentinels, cleansing pulses, and a temporary radiant dominion. One host per owner. |

## Guardians

Darkness Guardians and Radiant Sentinels are real bounded mobs, not fake players. They use the project’s player-shaped renderer and alignment skin/model. Both have:

- owner UUID and alignment persisted;
- target predicates that cannot invert friendly/hostile factions;
- despawn/lifetime rules and dimension cleanup;
- lightning, fireball, melee, and one alignment field ability with internal tactical cooldowns;
- hidden potion particles and semantic local FX;
- no item duplication, inventory access, portal travel, breeding, or natural Overworld spawning.

The Shadow Sword summon command/action produces darkness guardians. The Partisan produces sentinels. Unauthorised-carrier guardians use a separate short lifetime and do not count as owned allies.

## Menus and Presentation

The shared artifact menu groups actions into **Routed Powers**, **Crystal Powers**, and **Artifact Dominion**. Each row shows icon, action name, energy cost, rank gate, cooldown state, and a short server-supplied description. Locked actions remain visible with the reason. The client sends an action identifier; the server verifies it exists in the held artifact's catalogue.

Dark presentation recolours routed powers with violet-black smoke, eclipse runes, reversed chimes, and low thunder. Light presentation uses pale gold, white sparks, solar glyphs, bells, and clean thunder. Recolouring never changes target or damage rules silently.

## Acceptance Tests

- durability cannot change through attacks, mining, fire, explosion, anvil, or Crucible;
- every catalogue action rejects the wrong alignment and accepts a legal owner;
- rank-ten darkness has zero cooldown with energy still charged;
- guardian target matrices cover owner, allied tag, opposed tag, mob, and unauthorised carrier;
- inventory aura stays inside actual per-viewer FX budgets;
- every unique action has success, insufficient energy, rank gate, ward/amethyst/safe-zone, cleanup, and concurrency-cap tests;
- generated README and interaction documents contain both aligned variants.

