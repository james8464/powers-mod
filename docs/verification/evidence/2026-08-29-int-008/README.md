# INT-008 temporal lease evidence

Historical reviewed capture: subsequent non-evidence closure documentation requires
a new clean-SHA capture before this package can support final acceptance. See
[the closure ledger](closure.md). PENDING is not final integration approval.

This PENDING package binds dedicated-server acceptance to clean implementation
`051d105f52b845f8f399995211050b627d03f113`, with immutable base `98b181671b1514a3695ccb8f1ba1985092bce3dd`.
The six schema-2 JSONL rows are copied byte-for-byte from the successful unfiltered
production GameTest transcript, not reconstructed from expected values.

## Retained results

- Exact-SHA capture: all 166 required GameTests passed.
- Literal full check: all 166 GameTests, 1,836 JUnit tests across
  424 raw XML suites, and 240 Python tests passed.
- Ordered preflight/postflight receipts verify the actual clean checkout around
  the full check. The subsequent receipt binds every retained raw JUnit XML file
  by sorted filename, exact total, and SHA-256 inventory digest.
- Only private home/repository path prefixes in transcripts were redacted;
  JSONL rows and raw JUnit XML were preserved byte-for-byte.

## Live cases

Administrator preservation; external same-value supersession; measured 1,200
control-tick Crystal expiry with parked world time; seeded channel, field,
Celestial, realm-energy and Herald cadence parking under owned/external freeze;
real projectile pause/resume; and source-matched disconnect, death, dampening,
Shadow loss and shutdown cleanup. The suite also exercises API presence expiry
against a mature parked world's authoritative game time, divergent-clock beam
queries against world-owned fields, and innate/routed toggle cleanup on external
supersession. The projectile fixture retains its matching owned token until thaw.
Three further isolated tests cover hovering/launched Cinderheart lifetime under
external and both owned-freeze cases, parked overdue expiry with active lifecycle
cleanup, executor-started indefinite Shadow freeze with finite Flight expiry, and
body-scoped marker retirement after explicit stop, supersession, and body loss.

## Reproduction

```text
JAVA_HOME=<java25> ./gradlew runGameTest -Pint008ImplementationSha=051d105f52b845f8f399995211050b627d03f113 --rerun-tasks --no-daemon --console=plain
JAVA_HOME=<java25> ./gradlew check --rerun-tasks --no-daemon --console=plain
python3 scripts/verify_int008_temporal.py docs/verification/evidence/2026-08-29-int-008
python3 scripts/package_int008_evidence.py docs/verification/evidence/2026-08-29-int-008 --output <archive.tar.gz>
```

Independent READY review and integration gates remain outstanding. This package
does not claim closure. Inventories include deletions and digest both base and
implementation blobs; only evidence-package commits may follow capture.
