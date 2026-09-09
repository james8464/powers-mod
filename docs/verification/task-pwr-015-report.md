# PWR-015 Super Speed comfort verification

Status: implementation, focused tests, filtered live-server proof, and real-client proof complete; exact full gate and independent review pending.

## Implemented behavior

- Chronal Overdrive sends its owner a bounded start/stop lifecycle signal used only by local presentation. Disconnect and a finite client countdown clear stale state.
- The active camera clamps speed-induced FOV expansion to twelve degrees and multiplies that ceiling by Minecraft's vanilla FOV Effects slider.
- Minecraft's existing reduced-motion selector (Distortion Effects plus particle preference) removes the overdrive FOV change entirely. No client setting changes movement, timing, collision, energy, or any other authoritative gameplay value.
- View bobbing remains under Minecraft's vanilla control; POWERS adds no forced camera roll, shake, or tilt.
- Server wakes now retain every sharp bend or reversal, render moderate bends every second tick, and thin straight runs to every third tick. Sampling remains finite, movement-derived, and independent of render frame rate; stillness and teleport-sized discontinuities remain rejected.

## Evidence

- `SuperSpeedCameraRulesTest` proves inactive passthrough, bounded expansion, vanilla slider scaling, invalid-value fallback, and exact reduced-motion suppression.
- `SuperSpeedRulesTest` proves the bounded 1/2/3-tick curvature LOD alongside the existing wake caps and teleport rejection.
- Packet and source-boundary tests prove consistent finite lifecycle payloads, owner-local countdown cleanup, mixin registration, vanilla accessibility ownership, and the absence of movement-authority calls from client state.
- The filtered Java 25 live-server test exercises Super Speed through the production activation and cleanup pipeline.
- The dedicated real-client Java 25 test launches the actual renderer and mixin, observes sprint FOV presentation, then enables reduced motion and observes exact return to the configured base FOV while the local overdrive lifecycle remains active.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-pwr015-red-20260909.log` | `d3cc9f2933f06ba32a78722519e9e132e5cd610ed4df7a41cf6f4bac5e911cab` |
| `/tmp/powers-pwr015-focused-green-final3-20260909.log` | `e73f45fddfd733a643ba37d12fd09a35063818eae40b7a3586121d88953545d2` |
| `/tmp/powers-pwr015-live-final-20260909.log` | `efeb33316aab902183b6036e86cca18e833dbc0ba51058d4df0bb4d641fd4c85` |
| `/tmp/powers-pwr015-client-live-final-20260909.log` | `8ade9d45e94966ca5b1eefc4e315f094581a8988c8140e25eaa5d4a42d66ea80` |

## Pending acceptance

Run the unchanged checkout through the exact Java 25 full gate, retain raw outputs, and obtain a fresh independent READY review before integration. This branch is not READY and must not be merged or pushed meanwhile.
