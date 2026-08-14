# PERF-012 shared-perception evidence

Status: **accepted** on production commit `3baa5da2770c286c81b2502aa165673f73961ee4`.

## Guarantees

- Shadow, Darkness Creatures, Radiant Sentinels, and guardian alignment fields share immutable per-level, spatial-cell, server-tick observations.
- Each consumer retains an explicit inspection cap. A solitary guardian field inspects at most 16 bodies; separate cells retain independent caps.
- Cached observations expose only capability-neutral values and UUIDs. Live entities are re-resolved before attackability, line of sight, damage, healing, or targeting changes.
- A truncated capture is reused only for identical bounds. A contained query may reuse a different outer capture only when that outer capture was exhaustive.
- Alignment and spherical/lane eligibility run before the result limit. Allied crowds, vertical-cylinder distractors, and off-lane allies cannot starve a valid target or friendly-fire warning.
- Valid guardian targets remain stable; dead, newly allied, non-attackable, out-of-range, or unseen-expired targets clear before reacquisition.
- Attribute-less living entities are valid immutable observations. Server stop clears all caches and `/powers diagnose` exposes bounded aggregate work.
- Shadow preserves its former 24-block horizontal and 12-block vertical fallback range. Unknown firing lanes beyond 96 blocks on either horizontal axis or 48 blocks vertically fail closed by removing ranged offense before tactical planning, including at four energy.

## Performance proof

The live benchmark places Shadow, both guardian alignments, an attribute-less armour stand, and 24 targets at separated same-chunk positions. It moves them between four independent production planning cadences and compares the exact former query shapes/caps with the current production consumers on the same scene.

```text
queries=24
cacheHits=12
current entity inspections=348
identical legacy entity inspections=540
reduction=35.56%
```

This exceeds the selected programme's 30% acceptance threshold without relying on co-located actors or repeated same-tick field pulses.

## Verification

The implementation followed observed RED tests for the missing immutable snapshot API, runtime consumer integration, stop cleanup/diagnostics, saturated cross-bounds reuse, exact firing lanes, invalid target cleanup, a realistic 30% gate, and long/low-energy lane safety. Production commit `3baa5da2770c286c81b2502aa165673f73961ee4` then passed:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew test validatePowerResources compileClientJava compileGametestJava \
  verifyMagicDocs verifyItemDocs verifyRankDocs auditJavaSources \
  auditNonItemAssets testPythonScripts --rerun-tasks --no-daemon

JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew runGameTest --no-daemon
```

Results: 1,426/1,426 JVM tests, 27/27 Python tests, and 95/95 required Fabric GameTests passed. Resource validation, exact Java-source audit, non-item asset audit, client/GameTest compilation, and all generated magic/item/rank document checks passed. An independent read-only review completed with no remaining P1/P2 findings after its seven correctness/performance findings were reproduced or addressed.

`game-test-key-lines.log` records the accepted live measurement and suite count. `SHA256SUMS` authenticates that extracted log; this README is excluded because it contains the checksum reference.
