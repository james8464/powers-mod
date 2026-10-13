# PWR-006 momentum flight verification

Status: implementation, focused tests, and filtered live proof complete; exact full gate and independent review pending.

## Implemented behavior

- Server velocity remains authoritative; client input supplies intent only.
- Horizontal acceleration and turns are bounded instead of snapping velocity each tick.
- Releasing horizontal input brakes toward zero without reversing; vertical momentum settles toward hover.
- Water uses lower horizontal, vertical, and acceleration caps with an explicit transition cue.
- A hysteretic sonic threshold emits one ring/boom on entry rather than every tick.
- Steady and low-energy audio have distinct pitches; the rainbow wake banks by a bounded tilt signal.
- Survival propulsion still resets only the owning connection's floating counter and never mutates creative-flight flags or the server-wide flight policy.

## Evidence

- `FlightRulesTest` covers bounded acceleration/turning, air braking, water caps, sonic transition, low-energy cues, finite values, and tilt bounds.
- `AttributeModifierIsolationTest` continues to enforce the no-creative-flight contract.
- Filtered Java 25 GameTest `flight_game_tests_pwr006momentum_flight_remains_server_owned_and_bounds_client_intent` passed 1/1 and exercised the live `FlightAbility` path with a survival mock player.
- The first two live diagnostics were retained as test-fixture corrections: the mock input was not neutral, and the mock's reported game mode was not guaranteed to equal the requested enum. No production gate was weakened; the final live test compares exact pre/post authority and bounded velocity while explicit no-input behavior remains pure and deterministic.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-pwr006-focused-green-final-20260909.log` | `4a9f6467635ca72e802b8b41cf451087bc1e49bc079b9f6ac2d1650fac750528` |
| `/tmp/powers-pwr006-exact-live-final-20260909.log` | `e7d67a264513c6525a3413c2819a3d797911af4959ca0e2cf8a7f80be4d3bfcc` |

## Pending acceptance

Run the unchanged checkout through the exact Java 25 full gate, retain raw outputs, and obtain a fresh independent READY review before integration. This branch is not READY and must not be merged or pushed meanwhile.
