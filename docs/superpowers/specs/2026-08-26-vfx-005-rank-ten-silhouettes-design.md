# VFX-005 Rank-Ten Silhouettes Design

## Purpose and scope

VFX-005 gives every rank-ten innate power a unique, presentation-only silhouette that remains
recognisable to another player at long distance without chat, HUD, subtitles, or power-name text.
The authoritative catalogue is the 23 IDs returned by `InnatePowerLevels.powerIds()`:

| Power ID | Rank-ten identity | Silhouette family |
|---|---|---|
| `size_shift` | World Titan | tall stepped body-frame with shoulder pylons |
| `time_shift` | World Stride | opposed hourglass gates |
| `flight` | Heavenbreaker | swept double-wing chevron |
| `starfall` | Firmament Fall | crowned descending comet |
| `void_beam` | World Rend | split eclipse with a central cut |
| `fireball` | Black Sun | dark solar disc with asymmetric flares |
| `lightning_strike` | Thunder God | forked crown and vertical bolt |
| `thunderclap` | World Clap | paired palms and concentric fracture |
| `speed_burst` | Living Comet | forward comet wedge with two wakes |
| `telekinesis` | World Hand | five-ray hand enclosing an orbit |
| `energy_beam` | Daystar Lance | long spearhead through a sun ring |
| `super_speed` | Time Runner | broken clock-ring with tangent streaks |
| `breezy_bash` | Tempest Sovereign | cyclone trident |
| `invisibility` | Perfect Absence | interrupted negative-space body outline |
| `time_freeze` | Eternal Instant | square clock cage and stopped hands |
| `forcefield` | Absolute Aegis | hexagonal shield with a central boss |
| `gravity_displacement` | Singularity Court | offset triple orbit around a void core |
| `vessel_possession` | Sovereign Mind | mirrored heads joined by a control chain |
| `astral_projection` | Unbound Spirit | offset double-body halo |
| `energy_drain` | Endless Hunger | open crescent maw with inward tethers |
| `ice_manipulation` | Winter Crown | six-point snow crown and hanging icicles |
| `plant_healing_acceleration` | Worldspring | branching world-tree canopy and roots |
| `double_health` | Immortal Heart | double heart chamber and pulse rings |

This task excludes artifact apotheosis, Shadow, Herald, First Vessel, and player-like casting poses.
Those remain owned by their later ART, SHD, MOB, and VFX-006 work units. VFX-005 changes no
mechanics, player model, hitbox, collision, light, world state, loot, or persistence.

## Architecture

The server emits one compact semantic event only after a successful rank-ten innate cast. The event
contains a monotonic event ID, catalogue profile ID, caster UUID, dimension, position, facing, legal
alignment, visual seed, and lifetime. It contains no expanded vertices. A pure shared contract owns
catalogue completeness, validation, palette selection, finite primitive descriptions, reduced-motion
projection, and hard limits.

The client owns a connection-epoch-scoped semantic manager and a dedicated world renderer. It expands
the profile into thin camera-stable ribbons and filled accents around the caster position, using depth
testing so terrain occludes the silhouette. Geometry is power-specific; alignment is a secondary edge
language only: Radiant uses warm white/gold outer accents, Darkness uses violet/black-magenta accents.
Changing alignment must never make two different powers share a silhouette.

The renderer is independent of particle settings and VFX-006 poses. Normal motion may use bounded
phase progression; reduced motion freezes that phase and lowers alpha without changing the identifying
outline. Resource reload recreates renderer resources while preserving valid semantic events.
Dimension change and disconnect clear all events. A stale handler from an earlier connection or
dimension cannot mutate current state.

## Runtime data flow

1. The existing authoritative cast path resolves ownership, rank, legality, cost, and gameplay result.
2. Only after success, the rank-ten presentation hook canonicalises aliases such as `size_morph` to
   `size_shift` and rejects non-innate, unknown, or sub-rank-ten actions.
3. The server selects already-connected observers in the caster's current dimension within 384 blocks.
   Distance filtering reads player positions only and creates no chunk tickets.
4. A guarded clientbound send binds the event to the observer's live connection/session predicate.
5. The client validates the payload before mutation, deduplicates exact event IDs, and inserts it into
   the bounded semantic manager.
6. The world renderer resolves the immutable profile, applies the observer-facing transform and
   accessibility mode, then draws a finite primitive list with depth testing.
7. Receipt-local lifecycle expiry removes the event even if its source entity disappears. Explicit
   connection/dimension resets clear it sooner.

Failed, cancelled, protected, unaffordable, stale, or otherwise uncommitted casts emit no silhouette.

## Budgets and validation

- Exactly 23 catalogue profiles; aliases never create a twenty-fourth identity.
- At most 64 active silhouettes per client connection.
- At most 32 silhouette events offered by one server tick and at most one event per caster/power/tick.
- Observer radius: 384 blocks, using squared-distance comparison in the current dimension.
- Wire lifetime clamps to 1..80 ticks; authored lifetime is 40 ticks.
- At most 64 primitives and 256 expanded vertices per profile.
- Every coordinate, facing component, scale, and phase is finite; scale clamps to 0.25..8.0.
- Unknown profile/alignment IDs, invalid UUID/session stamps, stale dimensions, non-finite values, and
  invalid lifetimes are rejected before mutation.
- Event ID exhaustion fails closed for new presentation events without affecting the cast.
- Rendering work is proportional only to the capped active-event and primitive counts.

These are presentation budgets, not permission to weaken the existing global FX budgets or packet
fault predicates.

## Visual and accessibility contract

At a 96-block observer distance, every power must be identifiable by outline alone after images are
normalised to monochrome. Pairwise masks must remain distinct; palette alone cannot satisfy the gate.
At 8 blocks, the silhouette must not obscure the crosshair or caster body. Behind a solid opaque wall,
no silhouette pixels may remain visible. Minimal particle settings must not remove the silhouette.

Reduced motion retains the same outer outline, uses a static phase, eliminates rotation/pulsing, and
reduces fill opacity. The silhouette cannot flash the full screen, change FOV, shake the camera, or
play sound; VFX-007 owns sound layering.

## Verification and retained evidence

Pure JUnit tests prove exact catalogue equality with `InnatePowerLevels.powerIds()`, all 23 unique
normalised primitive signatures, legal palette handling, aliasing, reduced-motion outline stability,
finite geometry, caps, lifecycle, replay, and stale-session rejection.

Server GameTests prove that all 23 successful rank-ten innate casts traverse the production hook,
sub-rank-ten and failed casts emit nothing, same-tick coalescing and global work caps hold, observer
range/dimension filtering creates no tickets, and unsupported/stale observers retain no queued work.

A real integrated-client gallery retains deterministic 1280x720 captures for all 23 profiles at 96
blocks in their legal alignment, representative opposite-alignment variants, all 23 reduced-motion
profiles, near/crosshair safety, opaque-wall occlusion, minimal particles, resource reload, dimension
reset, and reconnect. Machine verification checks the capture manifest, exact dimensions, non-empty
foreground, pairwise monochrome-mask distinction, normal/reduced outline equivalence, near-camera
exclusion, and zero wall leakage. A targeted human/agent review confirms recognisability and catches
visual defects that pixel statistics cannot establish.

Acceptance evidence is privacy-sanitised, checksum-bound, and tied to an immutable implementation
commit. The literal Java 25 `./gradlew check --rerun-tasks --no-daemon --console=plain` gate must pass
on that implementation commit and again on the final closure head before integration and push.
