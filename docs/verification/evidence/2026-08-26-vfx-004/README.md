# VFX-004 accepted evidence

This bundle closes VFX-004 against immutable implementation commit
`7bde6695fe7b09bab416bf863bb98d356277205a`, recorded in `build-metadata.json`.

## Accepted coverage

- Production requests perform no world, block-entity, or protection inspection before entering the
  fair bounded queue. At most 64 selected requests per tick independently prove support/origin loaded,
  read fresh state, resolve the live owner and protection decision, classify material, and activate.
- Snapshot-row loss retries only that row. A focused regression and a real integrated client prove
  convergence through 150/300 ms delay, duplication, reordering, deterministic 1%/5% loss, and a real
  Overworld to Nether to Overworld lifecycle boundary.
- Production-path fixtures also prove unsupported capability and false session rejection, request and
  delivery overflow, 64 exact-generation authoritative REMOVE receipts with zero server/client
  survivors, movement from beyond 256 blocks into observation range, same-server service-session
  replacement, and generation exhaustion followed by a distinct-server restart;
  the retained fault and restart transcripts bind those integrated-client results.
- The real renderer gallery contains all 30 impact-by-material combinations at 1280x720, plus a
  post-resource-reload frame and front/opaque-wall views. `capture-verification.json` records 5,921
  saturated scar pixels in front and zero behind the wall.
- The retained `logs/final-check.log` proves the literal Java 25
  `./gradlew check --rerun-tasks --no-daemon --console=plain` gate passes without filtering: 134
  required GameTests, the complete JUnit suite, 147 Python tests, audits, docs, and resources.

## Commands

```text
./gradlew test --tests com.powers.fx.VisualScarDeliveryRulesTest --rerun-tasks --no-daemon --console=plain
./gradlew runClientGameTest -Pvfx004FaultClientOnly --rerun-tasks --no-daemon --console=plain
./gradlew runClientGameTest -Pvfx004RestartClientOnly --rerun-tasks --no-daemon --console=plain
./gradlew runClientGameTest -Pvfx004ClientOnly --rerun-tasks --no-daemon --console=plain
python3 -m unittest scripts.tests.test_verify_vfx004_captures
python3 scripts/verify_vfx004_captures.py --screenshots build/run/clientGameTest/screenshots --output capture-verification.json
./gradlew check --rerun-tasks --no-daemon --console=plain
```

All Gradle commands use `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.
Logs are privacy-sanitized, and `SHA256SUMS` binds every retained evidence file except itself.
