# Living Realm Forces Design

## Goal

Turn darkness and pure light into persistent realm matter that spreads through ordinary terrain, makes darkness hazardous or restorative according to an entity's `darkness` tag, and annihilates both forces in a catastrophic but server-safe clash.

## Decisions

- Both blocks use vanilla random ticks, so `randomTickSpeed` remains the administrator's global pacing control. Each selected block makes a small, configurable number of face-adjacent spread attempts and never loads a chunk.
- Spread is enabled by default independently of generic power terrain damage, because spreading is the block's defining behaviour. It still respects safe zones and never replaces air, fluids, block entities, unbreakable blocks, amethyst counterplay, protected mindscape landmarks, or entries extended through `#powers:living_force_immune`. A source continues probing for opposed matter even when conversion is administratively paused, so a clash refused only by the active-wave cap can retry later.
- Loaded force blocks are indexed per server level and chunk. Chunk-load palette checks rebuild the index without scanning sections that cannot contain either force; placement, spread, removal, and chunk unload keep it current.
- Once per second, indexed darkness affects nearby loaded living entities. Entities without the `darkness` scoreboard tag receive Wither III for a refreshable duration. Tagged players instead receive a rank-scaled energy pulse; amethyst poisoning prevents that restoration. Safe zones prevent hostile Wither, but do not suppress a tagged player's restorative affinity.
- Face-adjacent opposite forces enqueue one deduplicated clash wave. It immediately inflicts distance-scaled magic damage and knockback outside safe zones, then scans a configurable 48-block sphere over several ticks and removes only darkness and pure-light blocks. Typed living-entity inspection aborts at 256 before deterministic distance ordering, so pathological crowds degrade blast coverage instead of server time. This is the gameplay equivalent of the requested power-100 fireball without a literal vanilla strength-100 terrain calculation, uncontrolled drops, forced chunk loads, or destruction of unrelated builds.
- The clash uses an expanding eclipse/fracture/rune sequence, layered light/dark/clash/explosion sounds, smoke, end rods, flashes, and visual-only lightning. Every successful spread draws a short source-to-target magic filament, conversion rune, force-colored burst, and restrained semantic sound within the global particle budget; aura pulses remain lower intensity than a clash.
- `darkness_block` and `pure_light_block` join the canonical magic catalogue under a realm origin. Their exact pair resolves to mutual annihilation, while every new pairing with powers, spells, crystals, and amethyst is generated into the exhaustive interaction matrix.

## Components and data flow

`LivingForceBlock` delegates all server decisions to `LivingForceManager`. The manager owns lifecycle hooks, indexing, spread validation, aura application, clash deduplication, and active waves. `LivingForceIndex` contains only spatial bookkeeping. `ForceClashWave` owns one bounded scan cursor. `LivingForceRules` contains pure affinity and blast maths used by both runtime code and JUnit tests.

Configuration lives in a sanitized nested `PowersConfig.LivingForces` record so old configuration files inherit safe defaults. No client packet or client-side world mutation is introduced.

## Failure and performance boundaries

- Missing old config keys use defaults; hostile numeric values are clamped.
- Stale index entries are verified against loaded world state and removed lazily.
- At most four non-overlapping clashes run in one level; overlapping requests coalesce.
- Scans skip unloaded chunks. Each clash processes at most the configured number of coordinates per tick.
- Particle emission continues through `PowerFx`, so the existing per-tick budget remains authoritative.
- Server shutdown clears indexes and active waves.

## Verification

JUnit covers affinity, opposition, damage falloff, spherical bounds, spatial indexing, configuration defaults/clamps, catalogue counts, and the exact realm-force interaction. Resource validation covers the immune block tag. The final gate is Java source audit, generated interaction documentation, full `check`, clean `build`, and a dedicated-server startup/shutdown smoke test.
