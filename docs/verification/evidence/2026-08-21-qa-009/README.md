# QA-009 deterministic packet-fault evidence

Exact environment: Minecraft 26.2, Fabric API 0.156.0+26.2, Fabric Loader 0.19.3, Java 25. Seeded profiles target only `powers:*` custom play payloads; production is disabled by default.

## Exact matrix

All rows offered 1,000 packets with seed `630793`; convergence completed with queue depth 0 and duplicate side effects 0.

| Profile | Dropped | Duplicated | Delayed | Reordered/stale | Delivered | Max queue | Max age ticks |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| delay150 | 0 | 0 | 1,000 | 0 | 1,000 | 4 | 3 |
| delay300 | 0 | 0 | 1,000 | 0 | 1,000 | 7 | 6 |
| loss1 | 4 | 0 | 0 | 0 | 996 | 1 | 0 |
| loss5 | 41 | 0 | 0 | 0 | 959 | 1 | 0 |
| duplicate | 0 | 1,000 | 0 | 1,000 suppressed | 1,000 | 2 | 0 |
| reorder | 0 | 0 | 667 | 666 suppressed | 334 | 3 | 2 |

No row expired, overflowed, or cancelled under the ordinary matrix. Separate hard-bound tests prove one 32,768-envelope admission cap and one 4,096-operation tick cap shared by every global and entity-scoped session on the same server. A two-session 40,000-envelope fixture proves exact overflow accounting, fair progress for both sessions, and a combined—not per-session—4,096-operation tick maximum. Per-channel fairness, lifetime expiry, duplicate-copy overflow safety, and fail-closed cancellation are covered separately.

## Production convergence matrix

One required Fabric GameTest creates six simultaneous scoped players. Each row submits a real registered grimoire selection and a burst of real `PowerStatePayload` connection writes. Loss rows use 250 state writes so the seeded 1% and 5% profiles exercise actual loss without exceeding the 256-envelope per-direction cap; other rows use 100. The test polls at tick ten, performs the documented direct retry only if a final current-state packet was lost, and asserts the final authoritative selection and outbound connection write by tick twelve. These are polling upper bounds, not exact first-convergence times, and this server fixture makes no client/HUD claim.

| Profile | Observed by ticks ≤ | Final authority | Final outbound write | Offered | Drop | Dupe | Delay | Reorder/stale | Deliver | Expire | Max queue | Max age | Dupe effects |
| --- | ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| delay150 | 10 | spell 1 | power state 91 | 101 | 0 | 0 | 101 | 99 | 2 | 0 | 101 | 4 | 0 |
| delay300 | 10 | spell 1 | power state 91 | 101 | 0 | 0 | 101 | 99 | 2 | 0 | 101 | 7 | 0 |
| loss1 | 10 | spell 1 | power state 91 | 251 | 2 | 0 | 0 | 247 | 2 | 0 | 249 | 3 | 0 |
| loss5 | 10 | spell 1 | power state 91 | 251 | 10 | 0 | 0 | 239 | 2 | 0 | 241 | 3 | 0 |
| duplicate | 10 | spell 1 | power state 91 | 101 | 0 | 101 | 0 | 198 | 2 | 0 | 202 | 3 | 0 |
| reorder | 10 | spell 1 | power state 91 | 101 | 0 | 0 | 68 | 99 | 2 | 0 | 101 | 2 | 0 |

The exact counts above are from the isolated production-matrix run on 2026-08-21 and join to the scheduler matrix by profile ID. Each player has a distinct asserted UUID and independent entity-scoped session; per-player rate-limit state is forgotten before a retry. The required test additionally asserts that both loss profiles drop at least one real production envelope, duplicate injection occurs, delay/reorder profiles schedule delayed work, only an explicit loss may enter the safe-loss/retry state, and the post-retry selection is exactly index 1.

## Integrated-client convergence matrix

One real Fabric client runs the six named profiles sequentially. Each row uses a unique final energy marker and observes `ClientPowerState`, the actual client packet-handler/HUD mirror. The first poll occurs after ten ticks; therefore “observed by” is a truthful upper bound rather than a claim of the exact earlier arrival tick. A lost final current-state snapshot is retried once only after disabling the fault profile.

| Profile | Observed by ticks ≤ | Retry | Final HUD energy | Offered | Drop | Dupe | Delay | Reorder/stale | Deliver | Expire | Max queue | Max age | Dupe effects |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| delay150 | 10 | no | 811 | 110 | 0 | 0 | 110 | 106 | 1 | 0 | 102 | 4 | 0 |
| delay300 | 10 | no | 812 | 110 | 0 | 0 | 110 | 103 | 1 | 0 | 105 | 7 | 0 |
| loss1 | 10 | no | 813 | 260 | 1 | 0 | 0 | 248 | 10 | 0 | 249 | 4 | 0 |
| loss5 | 11 | yes | 814 | 260 | 13 | 0 | 0 | 237 | 9 | 0 | 237 | 4 | 0 |
| duplicate | 10 | no | 815 | 110 | 0 | 110 | 0 | 198 | 10 | 0 | 200 | 4 | 0 |
| reorder | 10 | no | 816 | 110 | 0 | 0 | 77 | 103 | 5 | 0 | 100 | 2 | 0 |

The profile ID joins every client row to the exact scheduler and server-production rows above. The same integrated run separately proves the real Shadow screen plus lossy semantic-FX renderer path.

## Production-path proof

- Registered `ServerboundCustomPayloadPacket` handlers: Shadow selection, wheel binding, cycle, commit, coordinate teleport, grimoire, crystal, locator nonce, Vessel input, and Vessel release.
- Real mock-player connection writes: body snapshots for two entity IDs; separate companion state/status and Vessel state; two concurrent Celestial cues; event audio; HUD power state; Magic, Beam, Shape, and semantic batch payloads.
- Authority: latest logical state owner converges; discrete duplicates execute once; locator duplicate/loss/retry/reconnect/expiry is bounded; stale Vessel input cannot replace the newest frame; delayed/disconnected release returns the owner; cosmetic faults do not mutate health.
- Lifecycle: connection generations are monotonic. Real disconnect, verified Nether transfer, death, and entity replacement cancel queued envelopes, invoke their fail-closed callback, and do not count a failed resolution as delivered. Operator reset invokes every queued recovery callback before clearing metrics.
- A real integrated Fabric client receives a delayed/reordered Shadow wheel and latest HUD state, then a deterministic lossy/delayed FX burst. The client handler, screen, HUD mirror, renderer queue, and server aggregate metrics all converge before the scoped session is cleared.
- Independent logical keys participate in the deterministic decision hash, idle stream metadata is released after its final envelope, and simultaneous scoped sessions cannot replace the optional global operator session. Foreign custom-payload namespaces bypass the seam without accounting.

## Commands

```text
./gradlew test --tests 'com.powers.testing.network.*' --tests 'com.powers.network.*Nonce*' --tests com.powers.network.VesselControlSequenceTest --no-daemon
./gradlew runGameTest -PgameTestFilter=powers-gametest:packet_fault_game_tests_six_profiles_converge_through_registered_production_boundaries --no-daemon
./gradlew runGameTest -PgameTestFilter=powers-gametest:packet_fault_game_tests_production_packet_boundaries_remain_authoritative_and_converge --no-daemon
./gradlew runClientGameTest --no-daemon
./gradlew runGameTest --no-daemon
```

All focused commands passed. One required sequential server fixture owns the complete registered-path scenario, including the 600-tick locator expiry; the second required fixture proves all six profiles and six entity scopes concurrently. The original QA-009 implementation commit is `e48610853134640a2fb0400ab63dc054999df2d9`. Literal broad-gate results and the changed-files inventory are recorded in the task report; the shared-server-budget follow-up is recorded in this evidence file and its commit handoff.
