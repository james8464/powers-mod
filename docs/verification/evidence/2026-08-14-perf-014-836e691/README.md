# PERF-014 compact-summon evidence

Status: **accepted** on production commit `836e691c68b4419f5c40db5c73cb56c95f70c37f`.

## Guarantees

- Minecraft's entity UUID remains the stable identity. POWERS writes only four custom keys: nullable owner `o`, task `t`, archetype `a`, and absolute game-time expiry `e`.
- Save size is fixed and excludes targets, paths, derived attributes, owner-cap membership, loaded-session membership, and every other cache.
- GUARD always has one owner and INVADE has none. Missing, unknown, or contradictory task/archetype data becomes an immediately expired record.
- Positive legacy remaining lifetime migrates to a bounded absolute deadline; zero expires immediately and `-1` preserves a natural realm creature.
- Absolute expiry continues while chunks are unloaded. An expired load is discarded before it can consume global or owner capacity.
- Each accepted entity load reconstructs derived membership once; duplicate lifecycle callbacks are idempotent. Loaded owner/tier changes atomically remove and rebuild membership without stale cap entries.
- The persisted archetype restores authoritative max health, armour, and attack damage without healing saved current health. Elite-to-normal changes clamp health safely.
- Unowned hostile invaders never enter the caster's owned tier index, so cap behavior is identical before and after reload.

## Verification

Development followed observed RED failures for the missing compact-record API, legacy zero-lifetime migration, expired unloaded capacity, loaded rebinds, owner/task invariants, owner-cap pollution, and live schema/attribute cases. Production commit `836e691c68b4419f5c40db5c73cb56c95f70c37f` then passed:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew test validatePowerResources verifyMagicDocs verifyItemDocs \
  verifyRankDocs testPythonScripts auditJavaSources auditNonItemAssets \
  --rerun-tasks --no-daemon

JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew runGameTest --rerun-tasks --no-daemon
```

Results: 1,431/1,431 JVM tests, 27/27 Python tests, and 100/100 required Fabric GameTests passed. Resource validation, generated magic/item/rank documentation, exact Java-source audit, and non-item asset audit passed. Focused live tests separately proved compact round-trip, legacy/malformed migration, unloaded expiry, loaded owner/tier rebinds, and unowned cap isolation.

An independent read-only acceptance review completed with no remaining actionable P1/P2 findings after tier restoration, malformed-archetype coverage, and unowned cap consistency were corrected.

`game-test-key-lines.log` records the final live-suite count and clean shutdown. `SHA256SUMS` authenticates that extracted log; this README is excluded because it contains the checksum reference.
