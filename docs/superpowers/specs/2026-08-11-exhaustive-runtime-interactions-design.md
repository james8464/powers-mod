# Exhaustive Runtime Interactions Design

## Objective

Make magical interactions complete at runtime, not only complete in generated documentation. Every registered magical action pair must retain deterministic mechanics and presentation, while every detached-mind, toggle, artifact, travel, death, and Shadow lifecycle edge must resolve through one explicit state policy.

## Chosen architecture

Use two orthogonal exhaustive systems:

1. The existing 64-action `MagicInteractionResolver` remains the canonical action-by-action matrix. Its 2,080 unordered pairs define scaling, cancellation, transformation, and audiovisual identity.
2. A new lifecycle policy enumerates magical form, ownership source, and termination event. Runtime owners consume that policy at their existing server-authoritative boundaries instead of inventing local cleanup rules.

Moving rays need a physical collision adapter because a cast residue at the caster is insufficient to prove that two beams crossed later. A fixed-cap recent-segment index registers live beam segments, tests only same-dimension recent candidates, deduplicates caster/action pairs, and emits the canonical interaction at the actual intersection point.

## Runtime collision contract

- Sunfire Energy Beam and Void Beam publish their authoritative ray segments.
- Distinct owners' segments may collide; a caster's own repeated segment cannot collide with itself.
- Segment history lasts only a few ticks and has hard per-dimension and per-tick caps.
- The exact Energy Beam × Void Beam rule is `DESTABILIZE` with an `annihilating_beam_clash` motif.
- A physical collision creates a bounded no-grief pressure blast, damages eligible nearby living entities, strikes one visual-only lightning bolt at each caster, and plays a short celestial ringing cue.
- Safe zones, amethyst, forcefields, body proxies, and standard power-damage policy remain authoritative for affected entities.
- Other physical ray pairs use their canonical resolver cue and multipliers; unsupported or stale action IDs are rejected.

## Travel contract

- Any player-controlled route starting in Light Realm or Dark Realm may travel within the same current mindscape.
- Power, crystal, projection, companion, and artifact routes may not leave the current mindscape.
- `PLAYER_RETURN` remains the explicit real-body route and continues to obey the established realm departure progression requirements.
- `ADMIN_RECOVERY` is operator-only.
- `FATAL_SOUL_RETURN` is an internal-only route used solely to place fatal detached-mind damage at the vulnerable physical body before vanilla death and respawn. It cannot be selected through commands, packets, powers, crystals, or artifacts.

## Toggle and ownership contract

- Death or respawn deactivates every innate and artifact-routed toggle before the replacement player continues.
- An artifact-routed toggle is reconciled every active player tick. Dropping, losing, or becoming unauthorised for the owning artifact invokes the ability's off transition and clears the saved toggle key.
- Disconnect, server stop, power loss, suppression, expiry, and dimension invalidation clear their runtime-only sessions exactly once.
- Cleanup must preserve unrelated potion effects and unrelated mods' flight/invisibility states.

## Detached-mind death contract

The forms are physical, realm avatar, astral avatar, teleport marker, possessed vessel controller, and dreamwalk controller.

- Fatal damage to the vulnerable physical proxy returns the owner to the proxy and kills the owner there, so ordinary respawn and inventory/drop rules occur in the physical world.
- Fatal damage to a realm or astral avatar performs the same internal soul return, kills the physical player, and invokes ordinary respawn.
- A possessed or dreamwalk vessel dying ends only the control session. The controller returns alive, regains the recorded game mode, and receives Wrath from the Gods: nonlethal power damage, a bounded energy loss, particle-hidden weakness/slowness/darkness, a divine rune/storm presentation, and a short ringing cue.
- Expiry, target unload, consent loss, safe-zone change, or ordinary cancellation returns the controller without wrath.
- Re-entrant fatal callbacks are guarded so one hit cannot double-return or double-kill.

## Shadow mortality contract

- Hidden Shadow remains an owner-only collisionless client apparition.
- Revealed Shadow is represented by one server-authoritative, skin-matched vanilla mannequin with no equipment or armour. It follows and teleports using the existing bounded companion cadence and is visible to all players globally under the established reveal rule.
- The revealed mannequin is damageable and killable, has no drops, owns no chunks, and cannot attack.
- Its death dismisses the current Shadow session and produces a bounded darkness-collapse ceremony.
- A later sword interaction or `shadow, reveal yourself` summons a fresh body. `MagicAttemptJournal` and knowledge-provider history are keyed to the player, not the body, so all previous memories survive.
- Hidden/revealed transitions never render two Shadow bodies to the same viewer.

## Exhaustive proof

- The generated action catalogue and 2,080-pair CSV remain build-verified.
- A generated lifecycle CSV lists every form × event decision and is verified against the production policy.
- Pure tests enumerate every action pair, every lifecycle combination, every travel kind across same/outside mindscape routes, every ray geometry boundary, and every Shadow visibility/death transition.
- Live GameTests cover the physical effects that pure tests cannot prove: beam clash, real-body fatal return, possessed-vessel wrath, artifact loss cleanup, and revealed Shadow death/resummon.
- Runtime diagnostics expose recent ray segments, collision budget use, detached bodies, and revealed Shadow bodies without scanning all entities.

## Performance boundaries

- No ordinary tick scans every entity or every magical action pair.
- Ray segments are partitioned by dimension, expire after a few ticks, and use hard per-dimension/per-tick caps, so pathological casts reduce collision selection instead of growing server work.
- Collision effects inspect a fixed maximum number of nearby living entities.
- Shadow adds a real entity only while globally revealed; hidden sessions remain client-local and ticket-free.
- Lifecycle evaluation is constant-time per active session or owned toggle.

## Documentation and compatibility

README and interaction rules explain physical ray collisions and lifecycle behavior. No registry identifiers or save formats are removed. New internal travel/lifecycle enums are not persisted. Existing Shadow memory stores and magic-attempt history remain unchanged.
