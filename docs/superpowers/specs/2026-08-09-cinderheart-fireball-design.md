# Cinderheart Fireball design

## Purpose

Preserve Fireball's distinctive summon-and-punch identity while removing uncapped entity spam, stale delayed callbacks, unsafe spawn fallback, mutable-owner damage attribution, uncontrolled lifetime, and visually silent counters.

## Chosen contract

- One server-owned Cinderheart may exist per original caster, with a global cap of 64. A blocked spawn refunds the cast instead of placing an orb inside terrain.
- The first cast opens a hovering tier-one heart. Recasting while it still hovers pays again and advances one tier; ordinary players cap at tier three and Ancient Mastery unlocks tier four. Every tier has a distinct nested seal, sound, impact radius, damage multiplier, ignition duration, and target cap.
- The first valid attack-deflection launches the orb. Later attack, forcefield, or external ownership transfers are finite: two reflections normally, one additional with Reflective Ward, and one additional with Ancient Mastery. A refused reflection produces a sealing fracture and cannot create an infinite volley.
- Original owner, current player controller, dimension, finite expiry, tier, rank variants, scaling, position history, and reflection count live in bounded server-thread state. Hover and launched lifetimes use exclusive server tick boundaries. Death, disconnect, respawn, dimension change, power loss, amethyst suppression, time lock, missing worlds, or server stop extinguish the owned entity exactly once.
- Impacts never invoke vanilla explosion terrain damage. The runtime applies nearest-first, capped power damage and ignition using a projectile-aware POWERS damage source. Current controller receives attribution; the current controller is immune to their own detonation, but an original caster can be harmed after a legitimate reflection.
- Safe zones, tagged amethyst, Sanctuary/Kinetic Ward fields, personal forcefields, water, ice/snow, invalid controllers, and protected bodies are explicit terminals. Forcefields consume finite integrity and may perform their existing one-shot ranked reflection. Water or frost transforms the heart into a reduced steam pressure pulse with no ignition. Every terminal has unique geometry, colour, particles, and sound.
- Empowered Impact adds a consent-safe, collision-checked pressure corona. Terrain mutation is off by default; when the server explicitly enables it, only a small capped set of valid adjacent air cells may receive ordinary fire, still respecting safe zones and block-entity policy. No blocks are destroyed.
- Hover pulses, tier changes, launch, measured flight trails, reflection handoffs, refused reflections, ward/amethyst/frost/water terminals, steam, ordinary impact, interruption, and expiry all use dedicated budgeted Cinderheart choreography.

## Bounds

- 64 active hearts globally; one per original caster.
- Hover lifetime 240 ticks, extended by charging but capped at 360 ticks from creation; launched lifetime 120 ticks.
- Tier 1–3 normally, tier 4 with Ancient Mastery.
- At most 12 impact bodies normally or 16 with Ancient Mastery.
- At most 2–4 post-launch reflections depending on rank.
- At most 24 trail samples and 8 terrain scorch attempts per impact.
- No chunk loading, delayed entity callbacks, vanilla explosion terrain damage, chain impacts, or ownershipless harm.

## Verification

Pure rules test tier progression, exclusive expiry, lifetime extension, reflection caps, radius/damage/ignition scaling, finite falloff, trail admission, target caps, and terminal priority. The full suite, source/resource/generated audits, Java 25 clean build, and dedicated six-dimension server smoke remain release gates.
