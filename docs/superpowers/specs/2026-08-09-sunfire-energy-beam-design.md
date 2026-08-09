# Sunfire Energy Beam design

## Goal

Replace Energy Beam's one-frame ray with a live-aim, server-owned sunfire channel whose logic, rank identity, environmental transformations, counters, lifecycle, and presentation match its Flame/Force beam catalogue identity without duplicating Void Beam.

## Runtime contract

- One paid cast opens an eight-tick focus and then fires on four server damage beats over a 40-tick total lifetime. The caster's authoritative eye position and look direction are resolved anew on every beat.
- A short hidden Slowness refresh telegraphs commitment without permanently altering attributes. Death, disconnect, dimension change, amethyst suppression, power loss, or time freeze interrupts the channel safely.
- Each beat stops at the nearest ordinary block, living body, ritual ward, or sampled water boundary. No terrain is changed and no chunk is loaded.
- Consecutive hits on the same body raise damage from 1.00x through 1.15x to a capped 1.30x and extend fire duration. Changing target resets the streak.
- Water before the terminal point transforms that beat into a radius-3 scalding steam pulse at 65% base damage, with no ignition, at most eight protected victims, and bounded outward motion.
- Amethyst, Pure Light, Darkness, Sanctuary, Kinetic Ward, personal forcefields, safe zones, and ordinary matter each stop or transform the beam with distinct output. Forcefields consume integrity through the normal damage bridge.
- Empowered Impact triggers at most one non-griefing solar flare after the third consecutive body hit, damaging at most eight valid nearby entities.
- Ancient Mastery forks a successful primary hit into at most two line-of-sight secondary targets within five blocks at 45% damage. Splits never chain, penetrate walls, bypass protection, or ignite through amethyst/wards.

## Presentation

- Focus: tightening orange-gold runes around the hands and eyes, rising glyph heat, beacon charge, and a low furnace pulse.
- Fire: a layered white-hot core, orange ribbon sheath, sparks, live endpoint corona, and escalating pitch on consecutive hits.
- Steam: dense pale pressure ring, cloud bloom, hiss-like extinguish layers, and force ripples.
- Counters: violet crystalline grounding, white-gold light prism, purple-black darkness absorption, green-gold sanctuary seal, cyan kinetic lattice, shield tessellation, and restrained safe-zone boundary.
- Mastery: two gold-white fork arcs; Empowered Impact closes into a compact solar disc and bass shockwave.

## Safety and performance

- At most one channel per owner, 64 channels server-wide, four damage beats, two mastered splits per beat, one flare, eight flare/steam targets, 128 water samples, and globally budgeted particles.
- Mutable state lives only on the server thread and clears on respawn, disconnect, and shutdown.
- All pure timing, escalation, target caps, finite math, and terminal selection are unit tested.
