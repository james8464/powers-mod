# Universal Arcane Cast Ceremonies Design

**Date:** 2026-08-08

**Target:** Fabric, Minecraft Java Edition 26.2, Java 25

**Status:** Approved through the user's standing authorization to make autonomous design decisions

## Purpose

Give every successful innate power, crystal ability, and grimoire spell a readable ancient-magic cast ceremony without replacing its bespoke impact effects. The presentation must remain server-authoritative, compact on the network, deterministic for observers, bounded for performance, and accessible at reduced screen-effect settings.

## Approaches Considered

### Rewrite every ability independently

This offers maximum bespoke detail, but repeats timing, networking, sound selection, budgets, and accessibility logic across 60-plus actions. Coverage would be difficult to prove and future powers would regress easily.

### Add one generic effect in the cast coordinator

This guarantees coverage cheaply, but identical rings and sounds would erase the catalogue's flame, time, soul, light, darkness, and suppression identities.

### Signature-driven quality floor plus bespoke impacts — selected

The central successful-cast boundary emits one compact semantic event derived from the canonical action definition. The client expands that signature into a typed four-beat ceremony. Existing ability-specific particles, projectiles, fields, and collision reactions remain the authored impact layer.

## Event Model

`MagicFxEvent` gains an explicit `CAST` or `INTERACTION` kind. The kind is transmitted as a bounded network identifier and participates in transport deduplication. A cast event contains only the existing semantic fields: identity, motif, sound profile, position, two RGB colours, glyph seed, and intensity. It never carries particle arrays.

Only `ServerMagicCasts.commit` emits cast events. Consequently failed, blocked, refunded, or validation-rejected attempts remain silent, while every successfully committed innate, crystal, and spell action is covered. The event ID combines owner, action ID, completed game time, and committed presence identity so simultaneous players and repeated casts remain distinct.

Persistent realm matter and passive amethyst sources do not manufacture cast ceremonies because they do not cross the player-cast commit boundary. Their existing ambient and collision effects remain separate.

## Presentation Profile

A pure `MagicCastPresentation` derives display intensity and one authored sound cue from a `MagicActionDefinition`.

Sound priority is mechanical rather than based on unsafe arbitrary strings:

1. suppression uses amethyst fracture or ward impact;
2. time uses temporal suspension;
3. space/travel uses rift opening;
4. soul or mind uses soul tether;
5. light uses light chorus;
6. darkness or void uses dark whisper;
7. protection, force, or storm uses ward impact;
8. crystal-origin actions use crystal resonance;
9. other actions use the rune hum.

Intensity is clamped to 1–5 and reflects origin, potency, and delivery. Innate utility casts remain restrained; spells and crystals read more strongly; high-potency fields, projections, beams, and travel receive an additional beat without exceeding the hard client budget.

## Four-Beat Choreography

A pure `FxChoreography` maps event kind and age to an optional immutable frame containing beat, optional motif override, particle-budget scale, geometry scale, and velocity scale.

Cast choreography:

- tick 0, anticipation: a compact ground glyph/ring gathers beneath the caster;
- tick 3, release: the action's signature motif opens around the body;
- tick 7, impact: the signature expands to its largest readable form;
- tick 13, aftermath: a restrained spiral or glyph dissipates;
- tick 17: the event expires.

Interaction choreography retains slightly slower, larger collision timing so overlapping forces remain distinguishable from an ordinary cast.

The existing implementation incorrectly applies beat scale only to particle count. Rendering will also multiply local geometry and velocity by the frame values, making the four beats visibly change size and motion. Reduced motion replaces moving motifs with static sigils, clamps geometry expansion and velocity, and continues to honour the vanilla screen-effect scale. Zero effect scale emits no particles.

## Audio and Server Compatibility

The server plays exactly one registered authored cue at the completed cast position. This lets nearby observers hear the action even if their client disables particles. Semantic client events provide the richer geometry; the existing global server-owned effects remain responsible for gameplay impacts and compatibility cues. Sound volume and pitch are bounded by intensity and never use client-provided identifiers.

## Performance and Safety

- At most 32 semantic events may wait on one client and 256 event IDs are remembered for deduplication.
- Every frame uses `FxGeometry.MAX_POINTS`, distance falloff, intensity, accessibility scale, and a hard 128-block observer radius.
- Geometry remains deterministic and finite for every motif and seed.
- No new ticking entity, chunk load, per-player server particle loop, screen flash, or persistent state is introduced.
- Packet decoding rejects unknown event-kind IDs rather than silently changing semantics.
- Cast emission occurs on the logical server after successful execution and commit.

## Verification

Pure tests cover event-kind validation, all catalogue presentation profiles, timing frames, reduced-motion frame clamping, compact wire estimates, finite scaled geometry, and transport deduplication. The Java source audit, full Gradle `clean check build`, and an isolated dedicated-server startup/shutdown smoke test form the release gate.
