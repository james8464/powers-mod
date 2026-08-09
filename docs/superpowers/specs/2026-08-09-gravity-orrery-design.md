# Gravity Orrery design

## Goal

Replace Gravity Displacement's one-shot Levitation effect with a visible, server-owned control field that feels like an ancient gravitational ritual while remaining bounded and fair on a multiplayer server.

## Runtime contract

- A successful cast anchors one field at the caster's current position for five rank-scaled seconds. One owner may have only one field.
- The field scans a spherical radius every two ticks, considers nearest living entities first, and captures at most 16 targets. Empowered Impact and Ancient Mastery each add eight slots, capped at 32.
- Captured targets are steered toward deterministic orbital points with finite acceleration and a hard velocity cap. Their fall distance is cleared while held and Slow Falling protects every release.
- Safe zones or refused forced-movement consent, amethyst poisoning, active personal forcefields, hostile Sanctuary/Kinetic Ward fields, and time-freeze ownership all resist capture. Each counter has a readable but rate-limited visual response.
- The field ends if its duration expires, its caster dies/disconnects/changes dimension, or amethyst suppresses the caster. Cleanup is idempotent on respawn, disconnect, and server stop.
- Ordinary collapse releases targets outward without damage. Empowered Impact adds bounded magic damage and a stronger downward/outward impulse only where harm and movement policy still permit it.
- Ancient Mastery bends at most 24 hostile projectiles per pulse around the orrery rather than reflecting or taking ownership of them.

## Presentation

- Opening: three opposed violet/cyan rings, a rising gravitational helix, reverse-portal matter, and layered beacon/warden tones.
- Sustain: counter-rotating floor and suspended rings, orbit tethers, sparse falling matter, and a low periodic pulse under the global particle budget.
- Resistance: violet crystalline grounding for amethyst, cyan tessellation for personal wards, green-gold sealing for ritual wards, pale time fractures for frozen bodies, and a restrained blue boundary for policy protection.
- Collapse: rings contract into a flash and fracture burst; Empowered Impact adds a bass shockwave and downward streaks.

## Safety and performance

- No terrain changes, chunk loads, global entity scans, unbounded queues, or client-authoritative positions.
- All mutable state is server-thread-owned and cleared through every lifecycle boundary.
- Geometry helpers reject non-finite inputs and cap target count, projectile count, and velocity.
