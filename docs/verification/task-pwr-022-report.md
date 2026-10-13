# PWR-022 Astral return indicator verification

Status: implementation, focused tests, and filtered live proof complete; exact full gate and independent review pending.

## Implemented behavior

- An active Astral Projection publishes an owner-only return cue once per second through the existing actionbar path.
- Same-realm cues contain only the body realm, an eight-way compass direction, and rounded horizontal distance.
- Cross-realm cues contain only the body realm; direction and distance are structurally redacted.
- The resolver consumes the projection's already-recorded immutable anchor. It performs no entity scan, destination search, chunk query, ticket request, or force-load.
- Display distance is bounded and no exact coordinates or player/body identity enter the cue value.

## Evidence

- `AstralProjectionAbilityTest` proves compass/distance derivation, vertical-position exclusion, and cross-realm redaction.
- Filtered Java 25 GameTest `powers_game_tests_astral_return_indicator_uses_only_the_owned_anchor_without_forcing_chunks` passed 1/1 through the live Astral activation path and proved return-cue derivation changes neither travel tickets nor body tickets.
- One live compile diagnostic was retained after the fixture initially queried an unavailable Minecraft API; the final fixture instead observes the mod's authoritative travel/body ticket registries. Production behavior was unchanged.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-pwr022-red-20260909.log` | `23e1b5f6ab0647859c0845a6f465d0955effdfa5e7168e522ea28e47007e166b` |
| `/tmp/powers-pwr022-focused-green-final-20260909.log` | `0930c6c2ffb3af2b04d9e85b8c3c51859192cd1bc7e8c6d8ef7caec88b32a536` |
| `/tmp/powers-pwr022-exact-live-final-20260909.log` | `c57fda2ccc3c229aaad9fef9b47f5bf2b61a51b2d326440a224724cc6692fa12` |

## Pending acceptance

Run the unchanged checkout through the exact Java 25 full gate, retain raw outputs, and obtain a fresh independent READY review before integration. This branch is not READY and must not be merged or pushed meanwhile.
