# VFX-006 synchronized casting-pose evidence

This bundle binds the real integrated-client gallery to immutable production implementation commit
`49a8b81ad11d70992861d557709fb0a822f66210`. The literal unfiltered aggregate passed on
`d0270c013d4b84eeb952e38cdef101e95a3baa7e`; the only intervening tracked change isolates an
unrelated presentation-only GameTest actor in spectator mode while its health invariant is sampled.
Production casting-pose bytes are unchanged.

## Accepted coverage

- The real Minecraft 26.2 client produced 54 decoded 1280x720 PNGs and 54 client-emitted manifest
  rows: six scoped styles by four poses in normal and reduced-motion modes, plus latency, late
  tracking, interruption, expiry, entity-ID reuse, and a real disconnect/reconnect lifecycle.
- The strict verifier binds every image digest, entity ID and UUID, sequence, authoritative start,
  receipt/capture tick, duration, pose/style/hand, reduced-motion state, active state, progress, and
  ten resolved joint angles. It rejects missing/extra coverage, stale identity, bad lifecycle state,
  malformed or non-1280x720 PNGs, angle overflow, path leakage, and checksum drift.
- Targeted inspection of the original frames checked held-item and locomotion compatibility,
  silhouette clipping, pose-family readability, normal/reduced distinction, and lifecycle reset.
  The first capture exposed poses outside the readable hold; a witnessed failing acceptance bound
  was added before recapture. The accepted frames show no clipping or held-item conflict, all four
  pose families remain identifiable, reduced motion is static/lower amplitude, and all clearing
  scenarios return to the vanilla base pose.
- Observed maxima remain below the authored limits: arms `1.2272`, body `0.1872`, and head `0.1040`
  radians. The server/client ledgers retain their 256/128 entry caps, 64-offer server tick budget,
  current-tracker-only delivery, UUID/sequence binding, and five-tick future-skew rejection.
- `logs/final-check.log` proves the literal Java 25 aggregate passed without filtering: 145/145
  required GameTests, 1,760 JUnit tests, 191 Python tests, Java/source/assets/docs audits, access
  widener validation, and resource validation.

## Commands

All Gradle commands used
`JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.

```text
./gradlew runClientGameTest -Pvfx006ClientOnly -PpowersVfx006ImplementationSha=49a8b81ad11d70992861d557709fb0a822f66210 --rerun-tasks --no-daemon --console=plain
python3 scripts/verify_vfx006_gallery.py --root docs/verification/evidence/2026-08-28-vfx-006
python3 -B -m unittest scripts.tests.test_verify_vfx006_gallery scripts.tests.test_package_vfx006_evidence
./gradlew check --rerun-tasks --no-daemon --console=plain
python3 scripts/package_vfx006_evidence.py --root docs/verification/evidence/2026-08-28-vfx-006
python3 scripts/package_vfx006_evidence.py --root docs/verification/evidence/2026-08-28-vfx-006 --verify
```

Logs replace private home/worktree prefixes. `SHA256SUMS`, `evidence-inventory.txt`, and
`archive-inventory.txt` deterministically bind every retained payload file.
