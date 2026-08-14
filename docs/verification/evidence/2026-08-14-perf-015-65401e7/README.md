# PERF-015 measured semantic-FX transport evidence

Status: **accepted** on implementation commit `65401e7b3f021635d4c54615d57dd16ab0e0edd8`.

## Result

POWERS now decides whether to send a semantic-FX tail as individual packets or one batch by measuring the exact Minecraft 26.2 framed and compression-threshold-aware wire sizes. It batches only when `batch_wire_bytes < individual_wire_bytes`; there is no guessed size threshold. The clientbound PLAY custom-payload packet ID is derived from the active protocol registry and is asserted to be 24 for Minecraft 26.2.

At the normal 256-byte compression threshold, actual mixed Magic/Beam/Shape payloads produced:

| Nominal burst | Entries | Individual bytes | Batch bytes | Saved | Batch selected |
| ---: | ---: | ---: | ---: | ---: | :---: |
| 64 B | 1 | 99 | 110 | -11.111% | No |
| 128 B | 2 | 176 | 171 | 2.841% | Yes |
| 256 B | 4 | 353 | 157 | 55.524% | Yes |
| 512 B | 8 | 684 | 233 | 65.936% | Yes |
| 1 KiB | 15 | 1,270 | 367 | 71.102% | Yes |
| 2 KiB | 31 | 2,639 | 634 | 75.976% | Yes |
| 4 KiB | 61 | 5,179 | 1,131 | 78.162% | Yes |
| 8 KiB | 122 | 10,336 | 2,120 | 79.489% | Yes |

The complete deterministic profile also covered disabled compression and 128/512-byte thresholds. Every row round-tripped, and the production decision matched the measured smaller shape. The worst 8 KiB decision p95 was 328.416 microseconds and decode p95 was 9.750 microseconds.

## Ordering and lifecycle guarantees

- The first cue remains immediate; only its same-tick tail may be batched.
- A 128-entry tail cap preserves order by flushing before overflow; the 132-cue regression proves exact client order.
- Tick changes flush old work before a new lead. Connection or dimension epoch changes discard stale tails.
- Unsupported batching fails open to ordered individual packets.
- Physical collision and lifecycle state never enter this presentation transport.
- A real integrated Fabric client received ten distinct beam cues in exact server order through the production batch receiver.

## Verification

Environment: Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, OpenJDK 25.0.4, macOS on Apple M3 Pro.

```text
./gradlew perf015CompressionProfile --rerun-tasks --no-daemon
./gradlew test validatePowerResources verifyMagicDocs verifyItemDocs verifyRankDocs testPythonScripts auditJavaSources auditNonItemAssets --rerun-tasks --no-daemon
./gradlew runGameTest --rerun-tasks --no-daemon
./gradlew runClientGameTest --rerun-tasks --no-daemon
```

Results: 1,448 default-profile JVM tests and 1,392 compatibility-profile executions passed; 27 Python tests passed; all 100 required Fabric GameTests passed; resource, generated-document, exact-source, and non-item-asset checks passed. The live client suite completed successfully. Expected offline-development authentication and unavailable anisotropic-filter option messages were not POWERS failures.

The profile models exact Minecraft framed/compressed custom-payload bytes, not outer TCP/IP overhead. The integrated-client proof covers production delivery and ordering. Independent read-only review ended with no remaining actionable P1/P2 findings after overflow ordering, lifecycle epochs, exact payload measurement, and protocol packet-ID derivation were corrected.
