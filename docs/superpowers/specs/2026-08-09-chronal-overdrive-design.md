# Chronal Overdrive Super Speed design

## Goal

Replace Super Speed's unowned potion bundle with a server-owned chronal overdrive that preserves unrelated effects, ends immediately on legitimate counters, communicates extreme motion continuously, and gives motion, might, veil, and dominion ranks bounded mechanical consequences.

## Runtime contract

- One cast creates at most one owned overdrive per player for a rank-scaled baseline of 160 ticks. A POWERS-owned transient movement-speed modifier replaces the broad Speed effect; fall distance is neutralized while valid and a short Slow Falling release protects the final landing.
- The runtime removes only its own modifier. Death, disconnect, respawn, dimension change, power loss, amethyst suppression, or time freeze interrupts it without touching potion effects or attributes belonging to vanilla, commands, or another mod.
- Water grounds the speed bonus to 35% and changes the wake into a readable hydroplane state instead of silently preserving full land speed.
- The server records finite previous positions and emits a bounded trail only when actual movement occurred. Teleport-sized discontinuities break the trail rather than drawing a line across hidden space.
- A horizontal collision produces a rate-limited impact ceremony. Motion's Second Step may perform one collision-checked backward chronal rebound per cast; Might's Empowered Impact may release one consent-safe, non-damaging eight-target pressure wave.
- Veil's Afterimage periodically clears this runner from the target memory of at most eight nearby hostile mobs that can currently see them; it does not grant invisibility or conceal protected information.
- Dominion's Ancient Mastery curves at most sixteen hostile projectiles once per cast under a speed cap, without reflecting them, stealing ownership, or acting inside safe zones.

## Presentation

- Opening: nested cyan-white ankle runes, an accelerating spiral, electric sparks, and a rising time-suspension chord.
- Wake: two-layer ribbon afterimages tied to measured motion, with sparse glyph footfalls and rank-coloured accents.
- Water: low pale-blue pressure fans, bubble-like cloud rings, and a lowered rushing tone.
- Collision: a flattened fracture rune and stopped-time flash; Second Step folds the wake backward through a gold-cyan gate.
- Empowered pressure, veiled memory slips, mastered projectile bends, amethyst interruption, time locks, and natural completion each receive distinct bounded ceremonies.

## Safety and performance

- At most 64 active overdrives, one state per owner, 24 trail samples, eight impact targets, eight veil targets, sixteen mastered projectiles, and globally budgeted particles.
- All modifiers use stable POWERS identifiers and reconcile every tick, preventing stale speed after lifecycle transitions.
- Timing, finite modifiers, trail admission, water grounding, rebound, pressure impulse, projectile curvature, rank caps, and lifecycle are pure-tested.
