# Abyssal Void Beam Design

## Purpose

Void Beam must become a readable, frightening signature power rather than an instant first-target damage call. It will remain a one-button innate ability, but the server will own a short telegraphed charge, resolve a finite penetrating release at the player's current aim, and leave a bounded void scar that participates in the existing magic-interaction system without changing terrain.

This tranche completes the promises already recorded in `2026-08-08-magic-quality-interactions-design.md`: short charge, finite penetration, light and ward contests, and a lingering non-griefing scar.

## Considered approaches

1. **Cosmetic polish around the current instant ray.** This is inexpensive, but it leaves no reaction window, penetration, spatial aftermath, or meaningful counterplay.
2. **Server-owned charged hitscan with a managed scar.** This gives opponents a visible warning, preserves accurate low-latency beam behavior, reuses existing lifecycle and collision systems, and creates no persistent entity spam. This is the selected approach.
3. **Custom projectile plus client-controlled channel/release packets.** This could expose manual timing, but it adds packet validation, prediction mismatch, projectile persistence, and a second input protocol without improving the intended one-button power enough to justify them.

## Cast transaction and charge

- A successful key press commits the normal energy cost, collision transaction, and cooldown immediately, then opens a 12-tick server-owned charge.
- The charge is not a second payment and cannot bypass suppression, magic-collision blocking, or the ordinary cooldown gate.
- The caster may move and turn during those 12 ticks. Release uses the authoritative server position and look direction at the final tick, creating an aimable but clearly telegraphed attack.
- The release is cancelled if the caster dies, disconnects, changes dimension, loses Void Beam, or becomes amethyst-dampened. A committed interrupted charge is not refunded; opponents therefore gain real counterplay without duplicating transaction state.
- Charge state is runtime-only, one entry per owner, and cleared on respawn, disconnect, and server shutdown.

## Ray and penetration

- The release ray begins at the caster's eyes and uses a 48-block base range, scaled and capped by the existing rank profile.
- The nearest solid block or hostile spell-field intercept establishes the terminal distance. No target behind that distance is affected.
- Candidate living targets are intersected against the actual ray, sorted nearest-first, deduplicated, and capped. The base limit is three; Empowered Impact adds one, and Ancient Mastery adds one, for a hard maximum of five.
- Damage falls to 72% for the second target and 52% for every later target. Invalid numeric values resolve to zero rather than emitting malformed damage.
- A target protected by amethyst, a sanctuary, a safe zone, or an active personal forcefield stops the ray and receives an explicit counter-impact ceremony. A normal permitted target is damaged and withered; Wither is applied only if server damage succeeds.
- Kinetic Ward and Sanctuary spell fields owned by another player intercept the ray at the first line/sphere entry. Ordinary blocks stop the ray, while tagged amethyst, powered Amethyst Ward, and Pure Light receive distinct collision ceremonies.
- Void Beam never breaks or replaces a block.

## Rank consequences

- All damage, range, duration, cost, cooldown, and scar dimensions continue through the canonical `PowerScalingService` profile and any already-resolved interaction multipliers.
- Empowered Impact increases penetration and makes release/impact geometry heavier.
- Ancient Mastery increases penetration, scar duration, and the number of stable scar glyph layers while respecting hard effect budgets.
- Dark Resurgence deepens the Wither tier and scar pulse while retaining amethyst and light as hard counters.
- These variants are snapshotted when the charge starts so administrative rank changes during the 12-tick telegraph cannot alter a committed cast unpredictably.

## Void scar

- A release that reaches ordinary matter, its penetration cap, or empty maximum range creates one spherical scar at the terminal point. Pure Light, amethyst, sanctuary, kinetic-ward, forcefield, and safe-zone counters do not create a scar.
- The base scar lasts 80 ticks with a 2.75-block radius. Rank scaling and Ancient Mastery may extend it, but radius, duration, total scars, pulse targets, and audiovisual work all have hard bounds.
- Every 10 ticks, the scar deals a small owner-attributed power-damage pulse and refreshes brief Wither on up to 16 permitted non-owner living entities. Amethyst, safe zones, sanctuaries, and forcefields retain their normal protections.
- Every 5 ticks, the scar renders alternating black-violet rings, an inward spiral, fracture motes, and a low dark whisper. Release and expiry have separate ceremonies.
- The scar registers a `void_beam` magic presence at its real impact point. Light magic cast near it can therefore produce the catalogue's projectile-consuming star-rift/eclipsing reactions instead of colliding only with residue at the caster.
- Scars are runtime-only, do not load chunks, do not mutate terrain, and are removed when their owner disconnects/respawns or the server stops.

## Presentation

- Charge: nested eclipse runes contract around the caster's eye/hand, dark ribbons climb inward, and pitch rises across four readable beats.
- Release: layered eclipse and ribbon beams trace the complete path, with fracture sparks at every penetrated body and a bass sonic tear at the terminal point.
- Counters are semantically distinct: white-gold inversion for Pure Light, violet crystalline fracture for amethyst, cyan tessellation for wards, and green-gold sealing for Sanctuary.
- Scar: counter-rotating rings, an asymmetric inward spiral, periodic soul/eclipsing motes, and a final reverse-portal collapse. Existing custom particles and authored sounds are reused so no unverified asset dependency is introduced.
- All calls continue through `PowerFx` and its shared server particle budget. Geometry uses colour plus different shapes, so the result remains readable without relying on hue alone.

## Safety and lifecycle invariants

- The client never selects action identity, charge completion, aim result, target order, damage, scar location, or rank variant.
- One owner cannot have more than one charge. The global scar cap is 128 and each pulse considers at most 16 living targets.
- Entity scans use only the finite ray envelope or scar AABB and never enumerate a whole level.
- Dimension identity is captured at charge start and must still match at release.
- Cleanup hooks cover respawn, disconnect, power loss, death, unloaded scar centres, and shutdown.
- Failed or countered release damage never bypasses safe zones, sanctuary, amethyst, or personal shields.

## Verification

- Pure tests cover charge timing boundaries, penetration limits, damage falloff, ray/sphere ward entry, target ordering, and scar pulse cadence/bounds.
- Runtime integration compiles on dedicated-server source sets and existing collision/resource validation remains green.
- Java and non-item asset audits are regenerated and checked after implementation.
- Final verification is `clean check build`, followed by a dedicated Minecraft 26.2 server startup and graceful multi-dimension shutdown smoke test.
