# Tempest Breezy Bash design

## Goal

Replace Breezy Bash's unbounded delayed lambda with a server-owned two-stage wind rite: a protected radial launch, a readable eighteen-tick apex, and an authoritative downward verdict whose ownership, counters, rank effects, and lifecycle are explicit.

## Runtime contract

- A successful cast examines at most 96 nearest living candidates in a rank-scaled radius, captures at most 16 bodies, and commits only if at least one body is actually launched.
- Launch combines bounded outward drift with vertical lift. Body-volume collision checks prevent ceilings or walls from turning the impulse into clipping.
- Each captured body belongs to one gust owner until resolution. Overlapping casts cannot compete over velocity; a second gust receives a visible wind-resonance refusal.
- After eighteen ticks, every body is resolved independently. Death, unloading, dimension change, range escape, consent/safe-zone changes, amethyst, projection-body anchors, personal forcefields, Sanctuary/Kinetic Ward, and time locks all cancel that body's slam and apply a safe Slow Falling release.
- A caster death, disconnect, dimension change, power loss, amethyst suppression, or time freeze interrupts the whole rite and safely releases every captured body.
- Empowered Impact raises the target cap by eight, strengthens the downward verdict, and creates a non-griefing pressure burst at the shared eye of the storm.
- Ancient Mastery raises the cap by eight and curves at most sixteen hostile projectiles away from the eye without changing their owner or reflecting them back at a player.

## Presentation

- Opening: two counter-rotating sky-blue floor runes, a rising white spiral, leaf motes, and a bass gust.
- Capture: a cyan tether and tight ankle-to-crown helix around every launched body.
- Apex: sparse orbiting ribbons show ownership without obscuring aim.
- Slam: a vertical white-cyan fracture line, contracting rune, gust emitter, and descending impact note.
- Counters: privacy blue, amethyst violet, soul-body lavender, shield cyan, ritual green-gold, time white, terrain grey, and overlapping-wind teal each have distinct rings and sounds.
- Interruption: the central seal breaks outward while released bodies receive pale feather coronas.

## Safety and performance

- One active rite per owner, 64 rites server-wide, 96 scanned candidates, 32 captures, 16 mastered projectiles, and globally budgeted particles.
- No terrain damage, chunk loading, direct teleportation, stale scheduled entity references, or competing velocity writes.
- Pure caps, impulses, lifecycle, ownership, and counter decisions are unit tested.
