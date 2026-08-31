# INT-008 temporal lease evidence

This PASS package binds dedicated-server acceptance to clean implementation
`100efb814308a70aff228a877c2f9aca5cb9e550`, with immutable base `98b181671b1514a3695ccb8f1ba1985092bce3dd`.
The six schema-2 JSONL rows are copied byte-for-byte from the successful unfiltered
production GameTest transcript, not reconstructed from expected values.

## Retained results

- Exact-SHA capture: all 167 required GameTests passed.
- Literal full check: all 167 GameTests, 1,836 JUnit tests across
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
A further isolated real Void Scar test covers a mature world clock ahead of
control time, shared-index lifetime boundaries, and owner cleanup.

## Reproduction

```text
JAVA_HOME=<java25> ./gradlew runGameTest -Pint008ImplementationSha=100efb814308a70aff228a877c2f9aca5cb9e550 --rerun-tasks --no-daemon --console=plain
JAVA_HOME=<java25> ./gradlew check --rerun-tasks --no-daemon --console=plain
python3 scripts/verify_int008_temporal.py docs/verification/evidence/2026-08-29-int-008
python3 scripts/package_int008_evidence.py docs/verification/evidence/2026-08-29-int-008 --output <archive.tar.gz>
```

Fresh independent review `int008_review7` accepted repaired implementation
`aaff0b0f88312b66d232f6e1f4ef0741a8690928` and evidence head `9b245015` with
no findings. Non-evidence closure documents were then reconciled without gameplay
changes. This new capture binds those documents to the final clean SHA;
evidence head `5fa0de1a5944a3e3f31589014db32ea8c3ba3dc6` subsequently passed
its unchanged literal full gate and fresh independent `int008_final_review8`
review with no findings. Its gate and all 424 byte-preserved XML suites are
retained separately under `logs/final-head-5fa0de1a/`; the original clean-capture
evidence is unchanged. PASS records captured-source and reviewed-head acceptance,
not integration or verification of a later receipt commit. The resulting
evidence-only closure head still requires its own full gate and fresh review;
main integration remains outstanding in [the closure ledger](closure.md).
Inventories include deletions and digest both base and implementation blobs;
only evidence-package commits may follow this capture.
