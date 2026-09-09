# PWR-014 Energy Beam verification

Status: implementation, focused tests, and filtered live proof complete; exact full gate and independent review pending.

## Implemented behavior

- The server-owned channel refreshes its live-aim ray and presentation on every firing server tick (20 Hz), independent of client frame rate.
- Authoritative damage remains limited to the four authored ten-tick beats after focus; the smoother presentation adds no damage opportunities.
- Every accepted direct body hit requests one temporary material-aware beam scorch on the supporting surface beneath the target.
- Scorch requests continue through `VisualScarService`, preserving protection, loaded-chunk, support, cap, expiry, persistence, collision, loot, and world-mutation rules.
- Existing water conversion, surface termination, escalating consecutive-hit burn, ranked flare, non-chaining forks, ward handling, and physical beam intersections are unchanged.

## Evidence

- `EnergyBeamRulesTest` proves exactly 32 firing visual frames and four damage beats across the finite channel.
- The filtered Java 25 GameTest enters the production activation pipeline and observes per-tick visual refresh, exact discrete damage cadence, one temporary scorch request per accepted hit, real health loss, an active visual decal, and an unchanged obsidian support block.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-pwr014-red-20260909.log` | `f0cb9335eea10092ce4ce0ed029c6d626eee0946170a5a5f518165d4e15e779f` |
| `/tmp/powers-pwr014-focused-green-final-20260909.log` | `e73f45fddfd733a643ba37d12fd09a35063818eae40b7a3586121d88953545d2` |
| `/tmp/powers-pwr014-live-20260909.log` | `6138e7e7b2bca099713cd64c0a83ac5b0e0b181ee4875dac1f2639473073fe34` |

## Pending acceptance

Run the unchanged checkout through the exact Java 25 full gate, retain raw outputs, and obtain a fresh independent READY review before integration. This branch is not READY and must not be merged or pushed meanwhile.
