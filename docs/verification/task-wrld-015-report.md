# WRLD-015 verification report

## Status

`WRLD-015` is implemented and verified on its isolated branch only. It is not
merged, pushed, or accepted as a `main` integration.

## Delivered behavior

- Three compact, loaded-only Middleworld outposts build a small library and a
  bounded amethyst road from the existing arrival point.
- Each loaded outpost receives one persistent, neutral, non-AI Archivist; the
  actor has no trading or travel authority.
- Dream weather is presentation-only and deliberately never changes vanilla
  precipitation.
- The manager touches at most 64 blocks per pulse, skips unloaded chunks, and
  creates no teleport hub.

## Focused proof

- `MiddleworldOutpostRulesTest` — 2 assertions groups passed after compiling
  the manager and bounded-road implementation.

## Deterministic branch gate

At branch state before commit, the literal Java 25 deterministic gate passed:

```text
test validatePowerResources auditJavaSources auditNonItemAssets
verifyVfxAssetAudit verifyMagicDocs verifyItemDocs verifyRankDocs
testPythonScripts compileGametestJava --rerun-tasks --no-daemon --console=plain
```

- Result: `BUILD SUCCESSFUL` in 4m 29s.
- JUnit: 1,838 tests across 425 raw XML suites, with zero failures.
- Python: 240 tests, `OK`.
- Raw log: `/tmp/powers-wrld015-deterministic-green-20260928-retry1.log`.
- SHA-256: `32c2b15c17a0f8544520316e90655470a1d343c57bcf216bf1ba1be4248b59b4`.

The preceding attempt stopped in Python because its temporary dependency
directory had disappeared. The rerun used a newly isolated temporary Python
dependency directory; no repository source or test was changed to address that
environmental failure.

## Integration boundary

This report is branch evidence, not a substitute for the protected-main gate,
fresh independent review, or the final release envelope.
