# Frozen Avatars, Private Companion, and Omnipower Boss Design

**Date:** 2026-08-10  
**Status:** Approved

## Exact Frozen-Body Avatar

Mindscape travel, astral projection, vessel possession, and teleport selection use one `BodySnapshot` contract. It records:

- resolved game profile and skin properties;
- slim/classic model resolution through the vanilla player-skin service;
- model-part bitset, main arm, equipment, active hand and use-item state;
- body/head/yaw/pitch rotations;
- pose, crouch/swim/fall-fly/sleep state, bed orientation;
- swing progress, attack animation, limb position/speed, scale, and movement vector at departure;
- dimension, position, health bridge identity, session token, and capture tick.

The server-owned body proxy carries the immutable snapshot. The client renderer uses the vanilla player models and the resolved owner skin; it freezes all recorded render values rather than recomputing idle/walk animation. Missing or invalid skin data falls back to the owner's vanilla default skin without blocking return. Equipment is visual only and never a second inventory.

Snapshot packets are bounded, codec-validated, and sent only to clients tracking the proxy. The proxy remains damageable under the existing guarded health bridge. Logout, death, server stop, lost entity, dimension removal, or deadline returns/cleans the session exactly once. Return uses the bounded asynchronous travel loader and never synchronously generates a chunk.

## Owner-Private Shadow Companion

### Privacy model

The companion is not a normal tracked world entity. The server stores an authoritative `PrivateCompanionSession`, and only the owner receives spawn/update/dialogue/despawn payloads. The owner's client renders a player-shaped, dark-skinned companion with interpolation. Other clients receive no entity, profile, sound, particles, or dialogue packet and therefore cannot see, target, collide with, or interact with it.

### Activation and movement

- Requires a darkness-tagged owner carrying the Shadow Sword.
- Activates after two seconds of stable eligibility; despawns immediately on removal, tag loss, logout, death, projection start, or unsupported dimension transition.
- Follows at 2.5–5 blocks like a tamed companion, avoids the camera, and teleports privately behind the owner beyond 20 blocks.
- Has no collision, loot, inventory, combat damage, pressure-plate effect, chunk ticket, or server-side pathfinding entity.
- Server simulates a cheap bounded follow point at 5 Hz; client interpolates at render rate.
- Owner interaction uses an authenticated interact packet with session ID and server-checked distance/view ray. A dedicated key and sneak-use fallback open dialogue.

### Dialogue

The default dialogue engine is deterministic and offline. It selects concise lore lines from context: realm, health, energy, rank, nearby alignment blocks, current artifact action, recent death, boss proximity, and story milestones. A short conversation state remembers the last eight topics per owner and avoids immediate repetition.

An optional OpenAI-compatible provider may replace only text generation:

- off by default;
- API credential read from an environment-variable name in config, never stored, synchronized, or logged;
- one request per owner and four globally, 30-second per-owner limit, 2.5-second timeout, 256 output characters;
- sanitized text-only prompt containing fictional game state, no chat history from other players, IP, UUID, coordinates, or secrets;
- asynchronous HTTP; the server thread only enqueues and consumes completed results;
- deterministic lore fallback for timeout, error, refusal, invalid output, or disabled provider;
- provider output cannot issue commands, modify game state, select boss actions, or bypass moderation.

## Omnipower Boss

### Identity and spawning

The **First Vessel** is a real hostile entity rendered as a player/Steve-shaped ancient avatar. It uses an original dark-starlight skin, not a real player's identity. It spawns only through an operator command, spawn egg, or documented late-game ritual; it never naturally floods a dimension.

Base attributes: 5,000 health, 16 armour, 0.33 movement speed, 0.8 knockback resistance. On encounter start, max health scales by `1 + 0.55 * (eligiblePlayers - 1)`, capped at 4x. Scaling snapshots nearby eligible non-spectator players and does not shrink mid-fight.

### Complete power access

Every registered player-power action has an entity-safe boss adapter. Catalogue validation fails if a new player power lacks one of:

- a concrete boss executor;
- a documented equivalent action adapter;
- an explicit `NOT_APPLICABLE` reason for player-state-only UI/mindscape actions plus a tactically equivalent boss action.

The boss never invokes handlers that require `ServerPlayer` by unsafe casts. Crystal-only and grimoire-only actions are not implied by “every player power,” though selected spells may appear as unique boss mechanics.

### Tactical planner

At most once per 10 ticks, the server scores up to 24 currently legal candidates using immutable encounter facts:

- target range/line of sight/vertical separation;
- boss health phase and energy;
- clustered players/allies;
- incoming projectiles;
- crowd control and wards;
- target movement, cover, recent action history, and counterplay state.

The highest weighted candidate wins with a small seeded variation to avoid repetition. Each action has its own cooldown, energy, range, and phase gate plus a one-action global cadence. The planner cannot queue stale entity references; it stores UUIDs/IDs and re-resolves at execution. No action can monopolize the tick scheduler.

### Unique phases and abilities

| Phase | Trigger | Mechanics |
|---|---|---|
| Waking Vessel | 100–70% | Tests players with movement, beam, telekinesis, elemental, and defensive powers. |
| Broken Constellation | 70–35% | **Constellation Theft** temporarily mirrors one recently observed player power; **Sevenfold Step** creates bounded afterimages and repositions safely. |
| Crownless God | below 35% | **World-Suture** interrupts projectiles and changes local terrain only where policy permits; **Last Firmament** cosmic telegraph combines safe pull, burst, and ward fracture. |

At 50%, the boss gets one **Vessel Reconstitution** heal that is interruptible by amethyst or opposing aligned dominion. It never bypasses admin invulnerability rules, claims, world border, or protected block entities.

### Dialogue and rewards

Dialogue is visible to encounter participants and uses the same offline lore engine. Optional provider rules match the private companion but use public fictional encounter state only. Dialogue never delays decisions.

Defeat emits a cosmic but budgeted sequence, grants advancements and configurable loot, and removes all boss-owned fields, scheduled actions, afterimages, and forced states. No crystal or mythic-artifact recipe is introduced.

## Acceptance Tests

### Body avatar

- snapshot codec round-trip for every field and malformed-data rejection;
- correct resolved profile, classic/slim selection, parts, arm, equipment, pose, pitch/yaw/body/head rotation, use and frozen limb/swing frame;
- tracking privacy, damage bridge recursion guard, one-time cleanup, and asynchronous return-ticket release;
- visual smoke with two different player skins and multiple poses.

### Companion

- only the owner receives every lifecycle/dialogue packet;
- eligibility, delayed spawn, follow, private teleport, interaction auth, and every cleanup transition;
- no world entity, pathfinder, collision, chunk ticket, item, or other-client sound/FX;
- provider queue/rate/timeout/sanitization/fallback and server-thread non-blocking tests.

### Boss

- catalogue completeness for every player action;
- planner cases for range, low health, projectiles, clusters, wards, cover, recent repetition, and invalidated target;
- scaling, one-time heal, phase transitions, cleanup, spawn restrictions, damage caps, and protected terrain;
- dedicated-server encounter simulation and client player-model render smoke;
- 20- and 50-player planner/tick performance regression tests.

