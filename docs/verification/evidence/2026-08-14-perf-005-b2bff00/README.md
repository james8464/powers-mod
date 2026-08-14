# PERF-005 semantic-FX coalescing evidence

Status: **accepted** on exact commit `b2bff008444fda536e0ca696ef3d6659f2508d59` (Minecraft 26.2, one real Fabric client).

## Result

The duplicate-heavy live capture attempted 64 identical semantic sustain packets for one connected observer. The client received one packet: 64 to 1 packets and 3,776 to 59 encoded payload-body bytes, a 98.438% reduction for both measures. These byte counts are the exact POWERS custom-payload codec bodies; they do not include Minecraft/Fabric framing, compression, or TCP overhead.

Coalescing is keyed by tick, observer, dimension, chunk, action, and semantic phase. It is not used for physical collision or lifecycle state, and capacity exhaustion fails open. The production collision GameTest registered distinct Energy and Void beam presences, collapsed 64 visual attempts to one delivery, then proved the moved physical beam resolved its collision exactly once. All 85 required GameTests passed on the same commit.

The client log contained the expected offline Mojang profile-certificate HTTP 401 continuation and no unexpected error lines. The server log contained no captured error lines.

## Commands

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home python3 -B scripts/fx_coalescing_capture.py
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runGameTest --no-daemon
```

## Evidence

- `fx-coalescing-report.json`: machine-readable client/capture result.
- `server-key-lines.log`: connected-client marker, authoritative traffic result, and full GameTest result, with each source log identified.
- `client-key-lines.log`: the marker observed by the connected Fabric client.
- `SHA256SUMS`: checksums for this evidence bundle's immutable result files.

The live capture is intentionally duplicate-heavy so the acceptance threshold is measurable. It proves the coalescer's packet/byte effect and the separate live collision test proves collision equivalence; it is not presented as a TCP packet trace or a general mass-combat latency benchmark.
