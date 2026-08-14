# NET-011 compatibility evidence

This directory contains privacy-safe evidence for the exact Minecraft 26.2 artifacts in the compatibility manifest. Third-party JARs, full logs, worlds, screenshots, player identity, UUIDs, IP addresses, and voice data are deliberately excluded.

- `runtime-results.tsv` is the machine-readable pass/limited/fail ledger.
- `runtime-markers.log` preserves selected exact, non-private log lines with source filenames and original line numbers.
- Full ignored logs remain locally under `build/compatibility-runs/*`; client copies are tied to their isolated receipts and unique process usernames as documented in the matrix.
- The two full-stack 113/114 logs and focused 1/1 reproduction are frozen under ignored `build/compatibility-runs/net011-log-snapshots/`; filenames and SHA-256 values are pinned in `runtime-results.tsv` so mutable `latest.log` is never the final reference.

The evidence supports only the bounded claims in [`compatibility-matrix.md`](../../compatibility-matrix.md). In particular, it does not claim a ClaimMod adapter, nested-container artifact ownership, microphone/audio success, visually graded renderer output, or the not-yet-implemented enhanced Light Realm sky.
