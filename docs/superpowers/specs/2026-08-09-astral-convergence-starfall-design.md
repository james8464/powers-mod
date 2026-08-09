# Astral Convergence Starfall design

## Goal

Replace Starfall's same-tick random bolt loop with a finite, readable celestial storm whose warning, impacts, protections, rank paths, and cleanup are all server-owned and visually distinct.

## Contract

- One convergence may belong to each caster, with a global cap of 32. State stores identifiers, immutable snapshots, bounded hit history, and positions—never delayed tasks or retained entities.
- A 20-tick omen telegraphs the complete storm footprint. Eight deterministic golden-angle strikes then descend six ticks apart; Might adds two, Dominion adds two and an eventual crown strike. The storm finishes shortly after its final authored beat.
- Motion may track the initially aimed living target, at a capped speed and only inside a finite leash. Without Motion, or after the target becomes invalid, the field remains at its last legal centre.
- Each impact is resolved from sky to surface. Roofs catch the strike; unloaded space is never queried or loaded. Water converts it into a wider conductive pulse. Tagged amethyst, Darkness, safe zones, Sanctuary, and Kinetic Ward ground or consume it. Pure Light resonates without terrain mutation.
- Damage uses radial falloff, deterministic nearest-first target order, a per-strike target cap, a per-body repeat interval, and a per-storm hit cap. Forcefields consume integrity through the ordinary power-damage bridge. Successful Insight strikes reveal power invisibility and briefly outline the body. Communion authors one bounded mirrored echo on every third regular strike. Might and Dominion add consent-safe shock pressure.
- No strike changes blocks, starts fire, or uses damaging vanilla lightning. Visual-only lightning is supplemental presentation.
- Death, disconnect, respawn, dimension change, power loss, amethyst suppression, time freeze, expiry, unload, and shutdown remove the storm exactly once.

## Presentation

`StarfallFx` owns opening astrolabe, omen clock, pre-impact constellation, regular strike, water conduction, mirrored echo, Pure-Light resonance, every protected terminal, revelation, crown, and collapse ceremonies. Gold, indigo, white and cyan separate authored phases; particles remain inside the global server budget.

## Verification

Pure tests prove strike counts and timing, deterministic bounded offsets, phase boundaries, falloff, repeat limits, tracking, counter priority, pressure, and lifecycle. Release gates remain source/asset audits, generated interaction documents, resource validation, full Java 25 build, and six-dimension dedicated-server smoke.
