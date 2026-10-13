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

## Candidate repair — 2026-10-13

Review found that the original five-block lookup could spawn another Archivist
after the first was moved away. The production spawn boundary now consults an
overworld SavedData identity per outpost. Loaded tagged villagers adopt their
identity; chunk unloading and dimension transfer retain it; destruction releases
only the matching owner. No gameplay chunks are force-loaded by this repair.

Observed regression: `displacedArchivistIsNotDuplicated` failed on tick 30 with
“Moving an Archivist outside the library spawned a duplicate”. The preceding
attempt failed during fixture setup and is not treated as behavioral RED.

Focused final run passes three actual spawn/lifecycle GameTests: displacement,
unload retention, and exactly-one replacement after destruction. The fixture
holds the origin chunk explicitly; the unload case invokes the real entity
removal lifecycle, not a dedicated-server disk reload. Two unit tests verify
SavedData codec round-trip, ownership conflict protection, and independent sites.
The whole JUnit run passes 1,984 tests in 474 suites, with zero skipped, failed,
or errored tests. Source audit and whitespace validation pass.

Raw logs and all JUnit XML are retained in the common Git directory under
`codex-backups/stages-candidate-rjj18l/archivist-20261013/`.

| Evidence | SHA-256 |
| --- | --- |
| `powers-archivist-displacement-red-20261013-2.log` | `5207d521a923a0ddd36eba52eb7c72700eee4ab84c59d0b22c8ddfcf180472d9` |
| `powers-archivist-lifecycle-green-20261013.log` | `0cf6e513636f04b29701735c8ab8120a2447d484e77fccf769102f96f1b26fe9` |

This is focused repair evidence, not completed WRLD-015 acceptance: full candidate
check, client presentation, dedicated-server save/restart, and legacy saves with
already duplicated or displaced unloaded NPCs still need verification.
