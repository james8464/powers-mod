# PERF-001 connected-player profiles

Status: **accepted** on exact implementation commit
`9d69a923c9a8287068109a82f3c90128d1c0671f`.

## Acceptance budget

- 10, 50, and 100 authoritative server-player populations.
- Exactly 36,000 sampled ticks and at least 1,800 wall seconds per profile.
- Every attempted server-authoritative cast succeeds.
- p95 below 50 ms and p99 below 100 ms.
- A 1,800-second JFR with zero `jdk.DataLoss` events.
- A separate rendered/network scenario using ten real Fabric clients.

All conditions passed.

## Environment

- POWERS 1.0.2; Minecraft Java Edition 26.2.
- Fabric Loader 0.19.3; Fabric API 0.156.0+26.2.
- OpenJDK 25.0.4, 64-bit Server VM.
- macOS 26.6.1 build 25G76, arm64.
- Apple M3 Pro, 18 GiB RAM.

## Embedded connected-player campaigns

Each population ran from its own immutable `git archive` of the accepted commit:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew runGameTest -PpowersConnectedProfile \
  -PpowersProfilePopulation=<10|50|100> --no-daemon
```

These are server-list players backed by Fabric's embedded GameTest connection,
not TCP clients. They exercise `AbilityActivationService`, the interaction
kernel, destructive ability runtimes, visual budgets, bounded queries, and
cleanup. Testing mode removes energy/cooldown limits only. Each scenario
attempts one Lightning Strike, Energy Beam, or Thunderclap every two logical
ticks. Cumulative pacing holds the otherwise-unthrottled GameTest to 20 TPS.

| Players | Ticks | Wall seconds | Casts | p95 ms | p99 ms | Peak particles/tick | Peak inspections/tick |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 10 | 36,000 | 1800.006 | 18,000/18,000 | 6.633 | 8.116 | 512 | 30 |
| 50 | 36,000 | 1800.010 | 18,000/18,000 | 18.640 | 30.635 | 512 | 106 |
| 100 | 36,000 | 1800.001 | 18,000/18,000 | 10.349 | 13.121 | 512 | 124 |

Each concurrent process emitted one setup-only `Can't keep up` warning while
synchronously creating its complete player population. The warnings precede the
first pacing heartbeat and are retained in the logs. No warning, error,
exception, failed assertion, or process loss occurred after measurement began.
All three GameTests passed and shut down cleanly.

## Rendered/network campaign

Ten independent Minecraft clients (`Profile01` through `Profile10`) connected
over localhost to one Fabric dedicated server. Clients started ten seconds apart
against a pre-generated 9×9-chunk test area. Client 1 rendered at 854×480;
clients 2–10 rendered at 320×240. Each client loaded the checked-in evidence
script, received Thunderclap in slot 1, proved casts before measurement, then
continued sending one real activation packet every two seconds.

| Clients | Ticks | Wall seconds | Network casts | p95 ms | p99 ms | JFR data loss |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 10 | 36,000 | 1800.050 | 9,003/9,003 | 4.933 | 9.216 | 0 |

All ten clients remained connected throughout the profile. The accepted server
reported no lag, watchdog, crash, or disconnect during measurement. The client
logs retain expected failed Mojang/Realms authentication requests from Loom's
offline development accounts; these occur before each successful localhost
connection and are not local server or POWERS failures. The final diagnostic
export reports zero proxies, travel requests, celestial events, forced chunks,
spell fields, artifact fields, guardians, and force blocks. The world was
flushed and the server stopped cleanly after all clients disconnected.

The network run's generic runtime packet/particle peaks are zero because those
counters reset earlier in the tick than the profiler's end hook. They are not
used as packet-volume evidence; the server-authoritative activation counter,
client action logs, screenshots, server JSON, and JFR prove this workload.
`PERF-005` retains its separate before/after encoded-packet acceptance gate.

## Trace integrity

| Scenario | JFR duration | Allocation samples | Data-loss events | SHA-256 |
| --- | ---: | ---: | ---: | --- |
| Embedded 10 | 1,800 s | 0 | 0 | `7b60dec44136eae3cb97acc3fb32efd576f3b755a2132f1a10bd3362c7b6ab5b` |
| Embedded 50 | 1,800 s | 0 | 0 | `74ad3b2ad6042e2b7352a8ddb4db1b73627f98a86197f3d764fe008f8d7cfd5c` |
| Embedded 100 | 1,800 s | 0 | 0 | `e2ad120de57792b20faf41fdeb501191ffd70d0356791dd3a1344e5dde770d09` |
| Ten real clients | 1,800 s | 0 | 0 | `7c7a2554ef020329ef1c039ad0259399f7ba83bc38fb91c98965e43095b9afb7` |

These traces prove CPU, GC, monitor, duration, and data-loss requirements for
`PERF-001`. None contains allocation samples, so none is accepted as
`PERF-006` allocation evidence.

## Rejected attempts and historical evidence

- `rejected-4b2b3b2/simultaneous-startup` preserves the rejected attempt where
  ten simultaneous first-time clients triggered vanilla chunk-generation
  watchdog failure. The accepted run used staggered login and pre-generation.
- `rejected-4b2b3b2/short-wall-profile` preserves a 36,000-tick result whose
  wall duration was only 1799.656 seconds. It was rejected, the profiler was
  corrected test-first, and the full rendered campaign was repeated.
- `embedded-a5ac119` preserves the first accepted 10/50/100 historical reruns.
  The active acceptance uses `embedded-9d69a92` so every current result shares
  the corrected implementation commit.

`SHA256SUMS` covers every evidence file other than itself.
