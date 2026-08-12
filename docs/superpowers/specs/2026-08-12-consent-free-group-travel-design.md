# Consent-Free Group Travel Design

## Outcome

Time Shift, its Shadow Sword route, and the Light, Dark, and Middleworld crystals carry a bounded cohort of nearby living players and mobs. Travel never asks those nearby travellers for consent. The caster remains the only entity charged energy or cooldown.

## Cohort contract

- Capture living entities within 2 blocks of the caster, not the remote subject.
- Include the caster/principal traveller and at most 15 additional entities (16 total).
- Include players, ordinary living mobs, player-like test actors, and Shadow.
- Exclude dead/removed entities and vulnerable body-proxy mannequins.
- Re-check life, origin dimension, and 2-block range at the delayed commit.
- Preserve each companion's offset from the caster at the destination.
- A blocked companion is skipped without cancelling the caster's journey.

## Destination contract

Entered coordinates are exact. Finite-coordinate, Minecraft build-limit, chunk-loading, realm-confinement, amethyst, dimensional-anchor, anti-portal, safe-zone, and platform portal checks remain authoritative; floor support, fluids, suffocation/collision, and landing-space searches do not alter or reject entered coordinates.

The Light/Dark/Middleworld fixed realm arrivals retain their generated realm arrival point because no coordinates were entered. Ordinary players cannot use group travel to escape a mindscape: they must return to their recorded vulnerable body and meet the realm's exit requirements.

## Mindscape lifecycle

- Every captured player entering a mindscape receives an independent vulnerable `REALM` body session.
- Captured non-player living entities record a bounded, server-memory origin and can return when a nearby crystal return is triggered. Stale/dead records are discarded.
- Returning with a crystal attempts the caster's normal body return, nearby player body returns, and nearby tracked-mob returns.
- Shadow never receives a proxy or ordinary mob-origin record. Its real companion body moves with the group.

## Shadow authority

Shadow may cross into or out of any dimension, including the Dark Realm, without realm-exit, consent, ward, safe-zone, anti-portal, anchor, or landing-safety checks. Coordinates must still be finite and the target chunk must be available. Cross-dimensional movement is performed through `PrivateCompanionManager` so the persistent session is rebound to the replacement entity returned by Minecraft teleportation.

## Performance and failure boundaries

- One bounded local entity query per cast; no global entity scan.
- Hard cohort cap of 16.
- Existing bounded asynchronous chunk tickets remain in use.
- A principal-player failure rolls back newly started body sessions. Companion failure never duplicates or deletes an entity.
- All transient mob-origin and cohort state is cleared on server shutdown.

