# VFX-006 synchronized casting-pose evidence

This bundle binds the real integrated-client gallery to immutable production implementation commit
`5492b2798f65fa73766d433362a914fd41c2a701`.

## Accepted coverage

- The real Minecraft 26.2 client produced 55 decoded 1280x720 PNGs and 55 client-emitted manifest
  rows: six scoped styles by four poses in normal and reduced-motion modes, plus real delayed-packet
  latency, late tracking, moving locomotion, interruption, expiry, entity-ID reuse, and a real
  disconnect/reconnect lifecycle.
- The strict schema-2 verifier binds every image digest, entity ID and UUID, sequence, authoritative
  start, actual receipt/capture tick, duration, pose/style/hand, reduced-motion state, active state,
  progress, ten resolved joint angles, and real locomotion distance/speed. It rejects missing/extra
  coverage, stale identity, bad lifecycle state, synthetic or absent movement,
  malformed or non-1280x720 PNGs, angle overflow, path leakage, and checksum drift.
- Targeted inspection of the original frames checked held-item and locomotion compatibility,
  silhouette clipping, pose-family readability, normal/reduced distinction, and lifecycle reset.
  The first capture exposed poses outside the readable hold; a witnessed failing acceptance bound
  was added before recapture. The accepted frames show each actor completely framed with no held-item
  conflict, all four pose families remain identifiable, reduced motion is static/lower amplitude,
  and all clearing scenarios return to the vanilla base pose. The locomotion frame records 0.84
  blocks of server-driven displacement and 0.462 observed client walk speed without direct animation
  mutation.
- Observed maxima remain below the authored limits. The server/client ledgers retain their 256/128
  entry caps, a hard 64-attempt server tick budget split into 56 start attempts plus eight reserved
  terminal attempts, exact-dimension tracker delivery revalidated at dispatch, UUID/sequence binding,
  and bounded five-tick future-skew retention without rendering early. Explicit terminal packets clear
  interruption state even when the start lane is saturated.
- `logs/final-check.log` proves the literal unfiltered Java 25 aggregate passed on verification head
  `e6fa35e92d6ea7cd3f7aeb9eafd4d920b30ee08e`: 153/153 required GameTests, 1,773/1,773 JUnit tests,
  and 195/195 Python tests, plus Java/source/assets/docs audits, access-widener validation, and
  resource validation.

## Commands

All Gradle commands used
`JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.

```text
./gradlew runClientGameTest -Pvfx006ClientOnly -Pvfx006ImplementationSha=5492b2798f65fa73766d433362a914fd41c2a701 --rerun-tasks --no-daemon --console=plain
python3 scripts/verify_vfx006_gallery.py --root docs/verification/evidence/2026-08-28-vfx-006
python3 -B -m unittest scripts.tests.test_verify_vfx006_gallery scripts.tests.test_package_vfx006_evidence
./gradlew check --rerun-tasks --no-daemon --console=plain
python3 scripts/package_vfx006_evidence.py --root docs/verification/evidence/2026-08-28-vfx-006
python3 scripts/package_vfx006_evidence.py --root docs/verification/evidence/2026-08-28-vfx-006 --verify
```

Logs replace private home/worktree prefixes. `SHA256SUMS`, `evidence-inventory.txt`, and
`archive-inventory.txt` deterministically bind every retained payload file.
