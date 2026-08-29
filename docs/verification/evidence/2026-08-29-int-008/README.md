# INT-008 temporal lease evidence

This pending package binds live dedicated-server acceptance to exact implementation commit
`7242e601fd651dc1e7b95215e2259c1699bc38a5`. The retained JSONL rows are copied verbatim from the
successful unfiltered GameTest process; they are not reconstructed from expected values.

## Coverage

- Administrator-owned freeze remains authoritative and rejects POWERS lease acquisition.
- An external same-value write supersedes the lease and is not undone by POWERS cleanup.
- Crystal time stop remains active through control tick 1,199 and releases at exactly tick 1,200
  while world time is parked.
- Seeded channel, field, Celestial, realm-energy, Herald cadence, and world-time state remains
  unchanged under both external and POWERS-owned vanilla freeze.
- A real Darkness projectile remains stationary during freeze and resumes after the server-end thaw probe.
- A mismatched innate stop preserves a crystal lease; real death, dampening, Shadow loss, shutdown,
  and Fabric disconnect lifecycle paths release their leases.
- The unchanged unfiltered suite passed all 161 required GameTests. The aggregate closure gate and
  its exact implementation head also passed 1,835 JUnit and 234 Python tests. Independent review
  remains pending, so `build-metadata.json` intentionally reports `PENDING`.

## Reproduction

```text
JAVA_HOME=<java25> ./gradlew runGameTest -Pint008ImplementationSha=7242e601fd651dc1e7b95215e2259c1699bc38a5 --rerun-tasks --no-daemon --console=plain
python3 -m unittest discover -s scripts/tests -p test_*.py
python3 scripts/verify_int008_temporal.py docs/verification/evidence/2026-08-29-int-008
python3 scripts/package_int008_evidence.py docs/verification/evidence/2026-08-29-int-008 --output <archive.tar.gz>
```

All retained text is UTF-8/LF, privacy-scanned, inventory-bound, and checksum-bound. Acceptance also
requires two independent package generations to be byte-identical.
