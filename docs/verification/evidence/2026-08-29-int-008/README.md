# INT-008 temporal lease evidence

This pending package binds live dedicated-server acceptance to exact implementation commit
`4e5fed0a92374140af8f28cc42773d2aeb6a9267`. The retained JSONL rows are copied verbatim from the
successful unfiltered GameTest process; they are not reconstructed from expected values.

## Coverage

- Administrator-owned freeze remains authoritative and rejects POWERS lease acquisition.
- An external same-value write supersedes the lease and is not undone by POWERS cleanup.
- Crystal time stop records an exact 1,200-control-tick deadline.
- Selected world-clock managers do not advance or mutate energy while vanilla simulation is frozen.
- A real Darkness projectile remains stationary during freeze and resumes after the server-end thaw probe.
- Matching-owner lifecycle cleanup releases only its active lease.
- The unchanged unfiltered suite passed all 161 required GameTests. The aggregate closure gate and
  independent review remain pending, so `build-metadata.json` intentionally reports `PENDING`.

## Reproduction

```text
JAVA_HOME=<java25> ./gradlew runGameTest -Pint008ImplementationSha=4e5fed0a92374140af8f28cc42773d2aeb6a9267 --rerun-tasks --no-daemon --console=plain
python3 -m unittest discover -s scripts/tests -p test_*.py
python3 scripts/verify_int008_temporal.py docs/verification/evidence/2026-08-29-int-008
python3 scripts/package_int008_evidence.py docs/verification/evidence/2026-08-29-int-008 --output <archive.tar.gz>
```

All retained text is UTF-8/LF, privacy-scanned, inventory-bound, and checksum-bound. Acceptance also
requires two independent package generations to be byte-identical.
