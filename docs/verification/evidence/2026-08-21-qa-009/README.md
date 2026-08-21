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

No row expired, overflowed, or cancelled under the ordinary matrix. Separate hard-bound tests prove the 32,768 global queue cap, 4,096 global work-per-tick cap, per-channel fairness, lifetime expiry, duplicate-copy overflow safety, and fail-closed cancellation accounting.

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
./gradlew runGameTest -PgameTestFilter=powers-gametest:packet_fault_game_tests_production_packet_boundaries_remain_authoritative_and_converge --no-daemon
./gradlew runClientGameTest --no-daemon
./gradlew runGameTest --no-daemon
```

All focused commands passed. One required sequential server fixture owns the complete registered-path scenario, including the 600-tick locator expiry, while entity-scoped sessions allow unrelated parallel GameTests to coexist. The ordinary batch passed all 115 required tests. The final broad gates and exact commit are recorded in the task report.
