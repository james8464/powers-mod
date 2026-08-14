# PERF-006 allocation acceptance — 2026-08-14

The same 25,600-operation mass-combat workload was captured before and after the
geometry/payload hot-path change on Java 25.0.4. Each operation transforms 48 semantic
geometry points and resolves one semantic beam for 64 observers.

| Measurement | Baseline `ba573a0` | Accepted `9d2d31a` | Change |
| --- | ---: | ---: | ---: |
| Allocated bytes | 194,247,544 | 2,448,720 | -98.739% |
| Allocated bytes/operation | 7,587.795 | 95.653 | -98.739% |
| p99 nanoseconds/operation | 8,458 | 3,292 | -61.078% |
| Retained geometry entries | 16 | 16 | 0 |
| Retained payload entries | 1,024 | 0 | -1,024 |

This passes the locked gates of at least 20% lower allocation, bounded retained memory,
and no p99 regression above 5%. The optimized path reuses one primitive transform buffer
and at most two immutable observer variants per event; it does not pool lifecycle or
collision state.

`baseline-ba573a0.jfr` contains 120 allocation samples and zero `jdk.DataLoss` events.
`optimized-9d2d31a.jfr` contains 57 allocation samples and zero `jdk.DataLoss` events.
The production-entrypoint fixture and the complete suite both ran on the accepted source:
all 86 required Fabric GameTests passed. `comparison.json` records the machine-readable
gate result, and `SHA256SUMS` authenticates every evidence file except itself.

Commands:

```text
./gradlew perf006AllocationProfile -PpowersPerf006Label=baseline-ba573a0 --no-daemon
./gradlew perf006AllocationProfile -PpowersPerf006Label=optimized-9d2d31a --no-daemon
./gradlew runGameTest --no-daemon
<JAVA_HOME>/bin/jfr summary <recording>.jfr
```

