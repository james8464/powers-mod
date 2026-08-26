# VFX-005 exact-build closure candidate

This bundle binds the VFX-005 implementation and every retained acceptance artifact to immutable
implementation commit `562f092a1393cb05485499fbbfc9b6782ae9b5cc`. The documentation-only
closure commit is intentionally recorded as `PENDING` in `build-metadata.json` until it exists.

## Verified coverage

- The literal unfiltered Java 25 `check --rerun-tasks` gate passed at the implementation commit:
  138/138 required GameTests, 1,730 JUnit tests, 179 Python tests, audits, documentation, and
  resource validation. `logs/final-check.log` is the complete sanitized transcript.
- The real integrated client emitted exactly 56 manifest rows and 56 1280x720 PNGs through the
  production successful-cast hook, compact payload, connection-owned manager, and depth-tested
  renderer. Coverage includes all 23 normal and all 23 reduced-motion profiles at 96 blocks,
  opposite-alignment representatives, near-body/crosshair safety, minimal particles, opaque-wall
  occlusion, resource reload, dimension reset, and reconnect.
- The dedicated verifier's 32 tests passed. The current gallery passed 253 pairwise monochrome-mask
  comparisons, with minimum normal/reduced, alignment, and lifecycle outline Jaccard all `1.0`,
  4,676 retained near-body identity pixels (`0.994048` retention), zero crosshair intrusion, and zero
  wall leakage. Exact metrics and per-PNG digests are in `capture-verification.json`.
- `scripts/audit_java_sources.py` and `git diff --check` exited 0. Every retained file except
  `SHA256SUMS` is listed in `evidence-inventory.txt` and checksum-bound.

## Commands

All Gradle commands used
`JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.

```text
./gradlew check --rerun-tasks --no-daemon --console=plain
./gradlew runClientGameTest -Pvfx005ClientOnly --rerun-tasks --no-daemon --console=plain
python3 -m unittest scripts.tests.test_verify_vfx005_captures
python3 scripts/verify_vfx005_captures.py --screenshots build/run/clientGameTest/screenshots --manifest build/run/clientGameTest/vfx005-manifest.jsonl --output /tmp/vfx005-capture-verification.json
python3 scripts/audit_java_sources.py
git diff --check
```

Logs replace the private worktree, home, and Gradle-cache prefixes. The retained gallery and
manifest contain no absolute filesystem paths. `independent-review.md` remains explicitly
`PENDING`; this bundle does not claim an independent `READY` verdict before that review occurs.
