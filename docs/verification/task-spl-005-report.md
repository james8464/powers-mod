# SPL-005 Cartographer enhancement verification

Date: 2026-09-09

Status: **STAGED — independent review pending; not integrated or pushed.**

## Scope

- Adds a copy-on-death, 32-entry discovered-site journal keyed by dimension and exact query.
- Reuses journaled destinations before invoking the bounded world search.
- Emits ten finite local breadcrumb pulses, each limited to four points within eight blocks of the player and performing no route chunk reads or loads.
- Gates realm landmarks before target lookup, preventing under-qualified players from probing whether a locked landmark exists.

## Test-first evidence

- Initial focused RED: `/tmp/powers-spl005-red-20260909.log` (expected missing Cartographer journal/breadcrumb contracts).
  - SHA-256: `3ff01ec9b4f44c8a9b1a2bad31c8965053391a8e9a23afb404d827386d50beaa`
- Final focused GREEN: `/tmp/powers-spl005-focused-green-final-20260909.log`
  - SHA-256: `7c81de4c86ec97dcfaac6cf531d95fd138800a0dc477a004821b9733d4d7088d`
  - Journal bounding/deduplication/codec, local breadcrumb geometry, progression visibility, and integration wiring all passed.
- Exact registered live packet proof: `/tmp/powers-spl005-cartographer-exact-final-20260909.log`
  - SHA-256: `3737c861a21307e9996e2fc61c1778e09932502c9bf05d2fabe82deee31e832f`
  - Exactly 1/1 required GameTest passed.
  - The real locator packet path persisted one live-registry biome discovery, owned exactly ten breadcrumb tasks, and reused the unchanged journal entry on the second query.

## Deterministic branch gate

Command used Java 25 and a disposable `/tmp` Python environment containing the repository's Pillow, NumPy, SciPy, and SoundFile verification dependencies.

- Log: `/tmp/powers-spl005-all-green-20260909.log`
- SHA-256: `87bb6a3b7fd085ee2059d6acc12b62f73cc928e8c9fbd25a9df1c50304020efa`
- 1,842 JUnit tests: 0 failures, 0 errors, 0 skipped (428 XML suites).
- 240 Python tests passed.
- `validatePowerResources`, `auditJavaSources`, `auditNonItemAssets`, `verifyVfxAssetAudit`, `verifyMagicDocs`, `verifyItemDocs`, `verifyRankDocs`, and `compileGametestJava` passed.
- `git diff --check` passed.

The generated Java, non-item asset, and VFX manifests were refreshed mechanically for the new production sources and localization key.

## Integration boundary

- Base commit: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.
- No broad/unfiltered main gate was run.
- No plan checkbox or backlog entry was closed.
- No merge, main mutation, or push was performed.
- A fresh independent review remains mandatory before this unit can be considered READY.
