# VFX-007 layered audio evidence

This package retains production client audit decisions, subtitle screenshots, and source-file metrics from the exact implementation SHA. No microphone recording is used as source-faithful proof; decoded asset metrics and the production mixer audit are the authoritative evidence.

## Accepted coverage

- Exact implementation commit `1e04d2c2534120a234ebba2d04229a6f44a240d1` ran with one dedicated
  Minecraft 26.2 server and two real clients. The capture retains 77 schema-1 rows: all 48 cue/layer
  combinations in open space, all 16 cues behind an opaque wall, and a nine-event burst proving four
  admissions and five bounded drops.
- Ordinary and reduced-tinnitus Celestial ringing retain authoritative event origins and use the same
  positional distance/obstruction path. Reload, reconnect, and dimension transitions each demonstrate
  a fresh admitted event after client-state reset.
- Sixteen checksum-bound screenshots cover every localized subtitle. Decoded metrics bind all 51
  deterministic mono 44.1 kHz OGG assets, including descending near/mid/far RMS, restrained peaks,
  softened far spectral centroids, and the reduced Celestial warning band.
- Payload construction, exact-dimension recipients, capability gating, client coalescing, group/global
  limits, global headroom, missing-resource fallback, and the no-gameplay-mutation boundary have unit,
  source, and live GameTest coverage. Independent final implementation/evidence review returned READY
  with no P0, P1, or P2 findings.
- The literal unfiltered Java 25 aggregate passed on verification commit
  `fe79079965f89d9d147a664740bc11d6073aa254`: 155/155 required GameTests, 1,810/1,810 JUnit tests,
  and 218/218 Python tests, plus source, resource, access-widener, documentation, and VFX asset audits.

## Commands

```text
JAVA_HOME=<java25> python3 scripts/vfx007_audio_capture.py
python3 scripts/verify_vfx007_audio.py docs/verification/evidence/2026-08-28-vfx-007
python3 -m unittest scripts.tests.test_verify_vfx007_audio scripts.tests.test_package_vfx007_evidence scripts.tests.test_validate_layered_audio -v
JAVA_HOME=<java25> ./gradlew check --rerun-tasks --no-daemon --console=plain
python3 scripts/package_vfx007_evidence.py docs/verification/evidence/2026-08-28-vfx-007 --output <archive.tar.gz>
```

All retained text is UTF-8/LF, privacy-scanned, inventory-bound, and checksum-bound. Two independent
package generations must have identical bytes before acceptance.
