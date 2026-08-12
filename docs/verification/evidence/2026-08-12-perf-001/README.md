# PERF-001 connected-player profile

Status: **accepted** on commit `6fb69910dc5ea387e9f921314dbdf016301f4fc7`.

## Acceptance budget

- Exactly 10, 50, and 100 players must remain in the authoritative server player list.
- Each population must run for 36,000 logical ticks and at least 1,800 wall-clock seconds.
- The fixed workload must attempt and successfully execute 18,000 server-authoritative casts per phase.
- p95 server work time must be below 50 ms and p99 below 100 ms.
- The JFR must span 1,800 seconds and report zero `jdk.DataLoss` events.
- The GameTest must finish successfully with no warning, error, exception, or failed assertion.

All conditions passed.

## Environment

- POWERS 1.0.2; Minecraft Java Edition 26.2.
- Fabric Loader 0.19.3; Fabric API 0.156.0+26.2.
- OpenJDK 25.0.4, 64-bit Server VM.
- macOS 26.6.1 (Darwin 25.6.0).
- Apple M3 Pro, 11 cores (5 performance and 6 efficiency), 18 GB RAM.

## Command

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew runGameTest -PpowersConnectedProfile --rerun-tasks --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL in 1h 30m 15s`; one required live GameTest passed.

## Workload

The opt-in harness connects embedded server players and cycles one authenticated
Lightning Strike, Energy Beam, or Thunderclap cast every two logical ticks.
Players use the ordinary `AbilityActivationService`, magic-interaction kernel,
physical ability runtimes, visual budgets, bounded entity queries, and lifecycle
cleanup. Testing overrides remove energy and cooldown limits only; they do not
bypass activation, collisions, effects, or cleanup. A cumulative deadline paces
the normally unthrottled headless GameTest at 20 TPS while recorded tick samples
measure server work and exclude the deliberate pacing wait.

| Players | Ticks | Wall seconds | Casts | p95 ms | p99 ms | Peak particles/tick | Peak inspections/tick |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 10 | 36,000 | 1800.001 | 18,000/18,000 | 7.231 | 8.825 | 512 | 30 |
| 50 | 36,000 | 1800.006 | 18,000/18,000 | 5.851 | 8.503 | 512 | 106 |
| 100 | 36,000 | 1800.010 | 18,000/18,000 | 6.581 | 9.327 | 512 | 127 |

## Trace integrity

| Players | JFR duration | Allocation samples | Data-loss events | SHA-256 |
| ---: | ---: | ---: | ---: | --- |
| 10 | 1,800 s | 0 | 0 | `1095312cdb3149a8e6ee9f3558a1407ca9925fdb9a64ebe1385082af204a6f49` |
| 50 | 1,800 s | 361,296 | 0 | `02e07a913234f890572e13cc3422b147a28cdc1340dd16ebbbcfb422408ee7ea` |
| 100 | 1,800 s | 252,859 | 0 | `fa8f0bf67f9ab4352dd5591ca75f3c43e6daba230ef792f209d689a1e64cec3b` |

The first JFR contains tick, CPU, GC, and monitor-wait events but no allocation
samples; this does not invalidate PERF-001, but that trace must not be used as
PERF-006 allocation evidence.

Embedded GameTest players use the real server player list but do not traverse a
TCP client connection, so the packet counter is zero. Network coalescing remains
open under PERF-005 and requires a separate before/after real-client capture.

