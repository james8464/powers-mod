# POWERS Playtest Closure Design

**Date:** 2026-08-10  
**Target:** Fabric, Minecraft Java Edition 26.2, Java 25  
**Status:** Approved; the user delegated implementation decisions and requested uninterrupted execution

## Outcome

Turn the current feature-rich build into a coherent, testable multiplayer magic mod. Every historical prompt and the latest playtest report is a release requirement unless superseded here. Correctness comes first, then bounded performance, proof, and content expansion. Crystals and other deliberately deferred story items remain recipe-less.

## Canonical Player System

- Remove every automatically applied innate passive and the joining aura dust. Powers act only when selected, toggled, channelled, or explicitly triggered.
- Remove Cozy Campfire, Frost Nova, Elemental Blast, Ground Slam, and Shadow Step. Saved selections migrate deterministically to safe remaining powers.
- Every indefinite toggle drains energy while active. Time Freeze is the most expensive innate upkeep. Operator testing overrides may independently bypass energy, cooldowns, or both.
- Innate powers use explicit authored behavior for levels 0-10. Spells and crystals never inherit rank scaling. Artifact routes use their explicit alignment/apotheosis rules only.
- Rank screens use separate readable rooted trees for Light and Darkness, concise titles, alignment-specific artwork, clear awakening requirements, and no generic perk lattice or “Labyrinth of Names” heading.
- Energy is ten vanilla-style symbols directly above and aligned with hunger. Half-energy fills the correct side and the layout clears extra hearts, mount health, armour, air, and all GUI scales.
- A first-join written guide explains controls, energy, ranks, counterplay, realms, grimoires, crystals, artifacts, and testing commands without replacing the vanilla Knowledge Book.

## Targeting, Possession, and Travel

- Remote player/entity interactions share one participant lock. Neither caster nor target can use powers until the interaction ends.
- Vessel Possession and Dreamwalking control a valid living target’s movement, view, selected inventory action, and attacks for at most 30 seconds. They cannot cast powers. Higher-rank players and amethyst-poisoned targets reject possession. The original body remains vulnerable.
- Mindscape avatars cannot take damage or die. Fatal damage to a frozen physical body ends the session safely before ordinary death handling, preventing realm death loops.
- Teleport replaces the Time Shift name. The menu exposes only self coordinates and proximity to a uniquely named player/entity. Light/Dark options obey rank/alignment gates and Middleworld never appears.
- Every teleport uses one server-owned five-second storm: a skin-matched frozen body, lightning at origin and destination, transfer at the midpoint, bounded asynchronous destination loading, collision-resolving safe arrival, and cleanup on interruption.
- Light/Dark crystals fire a target beam normally and transport the caster plus nearby players while crouching. Bodies remain behind; mobs receive NoAI during their mind session. Return uses the shared storm. Crystals use fixed safe realm arrival sites.
- Indigo exposes only Middleworld, stores exact origin dimension/position, always enters at one stable location, and returns through the shared storm.
- Realm departure rules apply to every player-controlled path. Only explicit administrator recovery bypasses them.

## Combat and Magic

- Lightning has no cooldown, summons reliably through artifacts, and is non-destructive. Fireball block impacts never dereference a missing direct target.
- Plant and Healing Acceleration has no cooldown; crouch-use heals the caster and living allies within two blocks.
- Breezy Bash explodes at the victim’s landing point. Forcefield protects the caster and players within a two-block radius, has persistent durability, renders as a readable shell/ring, and completely absorbs the breaking hit even when overkilled.
- Destructive abilities author terrain damage at every player level and scale it by innate rank, subject to server policy and bounded work. This includes Fireball, Thunderclap, Breezy Bash, energy/void beams, Inferno, and other explicitly destructive actions.
- POWERS-authored status effects hide vanilla potion particles. Bespoke dust, ring, beam, rune, sound, and impact effects remain semantic, colourable, distance-culled, and budgeted.
- Spells are boss-useful and visually ritualistic. Circles are dense, coherent semantic packets expanded client-side. Routine success messages use no chat; exceptional failures use actionbar; rituals and catastrophic warnings may use chat/title.
- Celestial Ruin keeps a 60-second warning, with an unmistakable 100-block beam and loud pulses within 6,000 blocks. Detonation applies a 20-second whiteout/ringing/knockback sequence, a rapidly expanding terrain crater completed within that interval, high boss-relevant radial damage over 6,000 blocks, and many bounded fireball-100-style streaks. The event persists and progresses without the caster or ordinary player chunk loading.
- Darkness/Pure Light clashes create a genuine catastrophic explosion while their normal spread, harmful/beneficial auras, amethyst counterplay, protection policy, and work budgets remain intact.

## Crystals, Artifacts, Creatures, and Items

- Red Inferno uses thicker beams, fire, and rank-independent crystal destruction. Orange clones are unarmed player-model copies using the caster skin; Creativity Manifestation offers an allowlisted vanilla structure-placement workflow. Yellow supports the smallest safe scale through 10×. Green retains only Life Bloom. Blue Dreamwalking uses the shared possession core and Chrono Stop uses global Time Stop without energy until toggled off or one minute. Infected Rainbow is not a separate item; the normal crystal receives an infected visual only while held by Darkness.
- The Shadow Sword routes all remaining innate/crystal powers with corrupted presentation and exactly three unique actions: Call the Hollowed, Blight Ground, and Nightfall Dominion. Rank-10 Darkness removes its gameplay cooldowns while workload budgets remain. The Heavenly Partisan has a smaller curated light roster.
- Artifact selection uses an eight-favourite combat wheel and compact searchable icon library. Saved keys and favourites migrate safely. The Shadow Sword remains strictly stronger and broader than the Partisan.
- The First Vessel guarantees a Miniportal. Miniportals store two same-dimension coordinate teleports and become uncharged after use; a dropped amethyst shard recharges an uncharged device.
- Darkness creatures and the testing actor are player-compatible living targets. The test actor has stable username/identity and supports every applicable targeting path. Entity textures, UV alignment, and spawn-egg assets must pass visual/resource validation.
- Every registered artifact/item receives documented lore, purpose, acquisition state, and interaction. Intentionally unfinished recipes remain absent.

## Shadow Companion

- Remove the custom Knowledge Book UI, networking, loot progression, and remote-provider entry point. Vanilla Knowledge Book behavior returns unchanged.
- A player carrying the Shadow Sword receives a non-physical Shadow apparition. It uses a client-side player representation with the owner’s exact skin/model and no armour or held item.
- Shadow is owner-private by default. `shadow, reveal yourself` reveals its apparition to other players and makes subsequent Shadow replies global. `shadow, hide yourself` removes it from other clients and returns dialogue to owner-private. The owner always sees it.
- Any case-insensitive chat beginning `shadow,` is consumed by the server and never broadcast as the owner’s normal signed message. Reveal/hide are deterministic commands. Other questions use curated offline Minecraft/mod/lore knowledge first and an optional privacy-filtered, rate-limited remote provider second.
- When revealed, dialogue is server-wide; the apparition itself renders only in the owner’s current dimension. Shadow walks beside/behind its owner, chooses safe ground, and uses Darkness teleport effects when blocked, changing dimension, or too far away. It has no hitbox, combat authority, inventory, chunk tickets, or gameplay targetability.

## Performance and Proof

- No ordinary tick scans every force chunk, ward, field, named entity, proxy, guardian, or magical presence. Dimension/chunk indexes, capped rotating queues, staggered pulses, and cleanup ownership are mandatory.
- `/powers diagnose` reports active fields, indexes, forced chunks, proxies, celestial events, work/packet/particle budgets, and testing overrides.
- Pure rules receive strict RED/GREEN unit tests. Physical world behavior receives Fabric GameTests. UI/FX receive deterministic render-layout or client screenshot evidence. Critical travel, realms, combat, artifacts, and Shadow routing receive live acceptance checks.
- A requirement matrix maps every prompt statement to code, automated evidence, and any irreducibly manual observation. Documentation claims only evidence actually produced.
- Release requires clean unit/resource/document audits, a clean build, GameTests, dedicated-server boot, client launch, 10/50/100-player synthetic soak evidence, committed documentation/assets, a clean worktree, and synchronization with the tracked remote branch.

## Precedence

This specification supersedes older requirements that kept automatic passives, the removed powers, the separate Infected Rainbow Crystal, camera-only possession, the custom Knowledge Book, private-only Shadow replies, a large Partisan-exclusive catalogue, short Celestial Ruin presentation, or unscaled generic rank perks. Frozen physical bodies remain vulnerable; mind avatars remain immune.
