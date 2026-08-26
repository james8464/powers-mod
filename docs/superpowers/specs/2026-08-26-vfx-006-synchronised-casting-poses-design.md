# VFX-006 Synchronised Casting Poses Design

## Purpose and scope

VFX-006 gives player-shaped magical entities short, readable casting poses whose client playback is
anchored to authoritative server action timing. The feature covers exactly these production entities:

- the revealed `ShadowCompanionEntity`;
- `RadiantSentinel` and `DarknessCreature` artifact guardians;
- both `RealmHerald` variants; and
- `FirstVessel`.

Actual players, Echo Clones, generic test actors, projectiles, and non-player-shaped mobs are excluded.
The task changes presentation only: it does not alter AI decisions, action cadence, damage, collision,
hitboxes, navigation, persistence, protection checks, or combat permissions. VFX-007 owns sound and
later MOB/SHD work owns new actions or behaviours.

## Chosen architecture

The server publishes one compact pose event when an in-scope entity commits a production magical
action. A shared contract defines the finite pose vocabulary, style vocabulary, hand, durations,
validation, sequence ordering, and client lifecycle. Delivery is limited to players already tracking
the entity. A tracking-start snapshot gives a newly observing client the current unexpired pose; no
global scan or chunk ticket is permitted.

The client stores at most one accepted pose per entity in a connection-epoch-scoped manager. Dedicated
POWERS render states and small model subclasses apply bounded arm, head, and torso rotations after
vanilla humanoid pose setup. Only POWERS renderers use those subclasses. The design adds no animation
library, keyframe asset format, vanilla-player mixin, or global humanoid renderer hook.

This is preferred over synced entity data because pose events are transient presentation state rather
than persisted entity identity, and over inferring poses from particles because packet loss, late
tracking, and client settings would make timing ambiguous.

## Pose and style vocabulary

The semantic pose vocabulary is deliberately small:

| Pose | Meaning | Normal authored duration |
|---|---|---:|
| `INVOKE` | gather power before an instantaneous or area action | 16 ticks |
| `PROJECT` | direct or release a projectile/bolt toward a target | 14 ticks |
| `CHANNEL` | sustain a beam, ritual, or recovery action | action-owned, 20–120 ticks |
| `RELEASE` | emphatic completion for exceptional area/phase actions | 20 ticks |

Style carries identity without multiplying controller types: `SHADOW`, `RADIANT`, `DARKNESS`,
`HERALD_LIGHT`, `HERALD_DARK`, and `FIRST_VESSEL`. Hand is `NONE`, `LEFT`, `RIGHT`, or `BOTH`.
The client model resolves pose + style + hand into exact bounded angles. Mirroring the active hand must
not change duration or sequencing.

Action mapping is explicit at production seams:

- guardians use `PROJECT` for committed lightning and fireball attacks;
- Heralds use `CHANNEL` for their committed 80-tick beam cadence;
- First Vessel deck and stolen-power casts map action kind to `INVOKE`, `PROJECT`, or `CHANNEL`;
- World Suture and Last Firmament use `RELEASE`;
- Reconstitution uses `CHANNEL`, and interruption or completion clears it immediately;
- Shadow actions use the same semantic mapping when their production executor commits an in-scope
  magical action; ordinary movement, melee, dialogue, hide, and dismiss do not emit poses.

Cancelled, dampened, protected, unaffordable, invalid, or otherwise uncommitted actions emit no pose.

## Wire contract and ordering

`CastingPosePackets.Payload` carries only:

- entity numeric ID and entity UUID;
- monotonic per-entity sequence number;
- pose, style, and hand network IDs;
- authoritative server game time at pose start; and
- duration ticks.

All enums use closed integer IDs. Duration is 1–120 ticks. Sequence is positive and monotonic for the
entity's current server lifetime and never wraps; exhaustion fails closed for later presentation
events. Entity ID is non-negative and UUID is non-zero. Payload construction and decoding reject
unknown IDs, invalid duration, invalid identity, and arithmetic overflow.

The server stores only current unexpired pose state for in-scope live entities. At most 256 live server
entries are retained; stale entries are removed on expiry, entity removal, level mismatch, or server
stop. One entity may start at most one pose per server tick, and the server may offer at most 64 pose
events per tick. Saturation drops presentation only and never changes the committed gameplay action.

## Server data flow

1. Existing AI/action code performs all target, legality, protection, and gameplay work.
2. Immediately after the action commits, it calls the pose service with entity, semantic pose, style,
   hand, and bounded duration.
3. The service validates scope and identity, advances the entity sequence, records the authoritative
   start time, and replaces that entity's previous pose.
4. It snapshots only `PlayerLookup.tracking(entity)` observers that are connected, current, and able to
   receive the payload, then uses the existing guarded-send boundary.
5. When tracking begins, the service sends the current pose only if it is still active; elapsed time is
   preserved through the original start game time.
6. Server tick cleanup removes expired or orphaned state without retaining players, levels, or entity
   objects beyond the live lookup boundary.

The service never scans all players, loads a chunk, adds a ticket, schedules delayed work, or retries a
failed presentation send.

## Client lifecycle and latency behaviour

The client manager is reset on join, disconnect, and world identity change. It accepts a payload only
when the current world resolves the numeric entity ID to the same UUID and the entity type is in scope.
It rejects stale or duplicate sequences, expired start times, starts more than five ticks in the
future, unknown enum IDs, and any handler stamp captured before the current connection/world epoch.

Animation age is `clientWorld.gameTime - startGameTime`, clamped to the validated duration. Therefore
latency advances a late packet to the correct point instead of replaying from frame zero. A tracking
snapshot uses the same start time and sequence. Entity ID reuse, dimension changes, reconnects, and
replayed packets cannot animate the wrong entity. Capacity is 128 client entries; admission of a new
entity at capacity evicts the oldest finishing entry, never a newer sequence for the same UUID.

Missing, late, malformed, or unsupported packets degrade to the ordinary vanilla pose. Rendering code
must remain safe when the entity vanishes between state extraction and submission.

## Rendering and accessibility

`CastingHumanoidRenderState` and `CastingAvatarRenderState` add only resolved pose/style/hand, normalized
progress, and reduced-motion state. `CastingHumanoidModel` and `CastingPlayerModel` call vanilla pose
setup first, then add clamped rotations to head, body, and arms. Maximum added rotation is 1.25 radians
for an arm, 0.35 radians for the body, and 0.25 radians for the head. Leg pose, held-item transforms,
swim/fall/fly pose, and hitbox remain vanilla-owned.

Normal mode uses deterministic ease-in/hold/ease-out interpolation with no looping beyond the
authoritative duration. `FxAccessibility.reducedMotion` selects a static, lower-amplitude readable pose:
no oscillation, repeated pumping, camera motion, FOV change, or flash. The reduced pose preserves hand,
direction, and action family. When a vanilla locomotion state makes the authored pose unsafe or
illegible, the renderer reduces amplitude rather than overriding locomotion.

## Verification and retained evidence

Pure JUnit tests must first fail, then prove:

- closed enum/network validation, duration bounds, and sequence monotonicity;
- exact scope admission and entity-ID/UUID binding;
- per-tick and capacity budgets, replacement, expiry, and snapshot remaining-time semantics;
- replay, entity-ID reuse, stale handler, reconnect, and world-change rejection;
- latency-derived progress and reduced-motion/static angle bounds; and
- complete action-kind-to-pose mapping for guardians, Heralds, Shadow, and First Vessel.

Fabric GameTests exercise production action seams: committed guardian projectiles, both Herald beams,
First Vessel deck/stolen/World Suture/Last Firmament/Reconstitution flows, protected cancellation,
tracking-only delivery, late tracking snapshots, expiry, and zero chunk tickets. Tests inspect semantic
delivery and authoritative timing rather than client rendering.

A real integrated-client acceptance gallery captures every in-scope entity/style across all four pose
families in normal and reduced-motion modes, plus latency, tracking-start, interruption, expiry,
disconnect/reconnect, and entity-ID-reuse scenarios. Deterministic metadata binds entity UUID, sequence,
pose, start time, duration, client receipt time, and image. Machine checks validate dimensions, manifest
coverage, bounded joint deltas, lifecycle, privacy, checksums, and archive contents. A targeted visual
review checks readability, held-item alignment, locomotion compatibility, and absence of clipping.

Acceptance is bound to an immutable implementation commit. The literal Java 25
`./gradlew check --rerun-tasks --no-daemon --console=plain` gate must pass on that implementation commit
and again on the final closure head before integration and push.
