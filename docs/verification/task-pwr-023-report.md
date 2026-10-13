# PWR-023 Energy Drain verification

Status: implementation, focused tests, and filtered live proof complete; exact full gate and independent review pending.

## Implemented behavior

- The server-owned two-second soul tether remains movement-, damage-, amethyst-, sanctuary-, ownership-, range-, and lifecycle-interruptible.
- A targeted player is told to hold crouch; eight consecutive server-observed ticks break the tether, while releasing crouch resets progress.
- The existing live tether brightens from violet to white-lavender as resistance approaches success, and both participants receive owner-local outcome feedback.
- Non-player pulse and completion damage retain their percentage scaling and hard damage caps.
- Non-player energy conversion now shares one immutable health-scaled ritual budget capped at 120 energy. Successful pulses spend that budget first, and completion receives only the remainder.
- Player-compatible targets still transfer only energy actually removed from their finite authoritative pool.

## Evidence

- `EnergyDrainRulesTest` proves the eight-tick continuous contest, reset behavior, stress-color endpoints, health-scaled conversion budget, and no-overrun grants alongside the retained boss damage caps.
- Filtered Java 25 GameTests passed 3/3 together: the player-energy-well regression, the live crouch counter, and the live Iron Golem conversion budget.
- The first combined diagnostic exposed fixture-global `clearAll()` interference. The fixtures were repaired to use owner-scoped cleanup and the unchanged three-test batch then passed together; no production gate was weakened.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-pwr023-red-20260909.log` | `7a637234310a5db113eb5cdf120128aa905932c021fd8a4a302cd508474a61c3` |
| `/tmp/powers-pwr023-focused-green-final-20260909.log` | `52827147b46bde9d49f8ee62d7b2687d709f8564ca3cfe9fe39cee023f1ac6d5` |
| `/tmp/powers-pwr023-live-combined-final-20260909.log` | `87bfbd3f21073d871176a6dc4eabd99de67b5bc7fe26ecd71ad83f7edb289a9a` |

## Pending acceptance

Run the unchanged checkout through the exact Java 25 full gate, retain raw outputs, and obtain a fresh independent READY review before integration. This branch is not READY and must not be merged or pushed meanwhile.
