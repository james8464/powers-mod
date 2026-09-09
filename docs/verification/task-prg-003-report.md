# PRG-003 boss contribution verification

Date: 2026-09-09

Status: **STAGED — independent review pending; not integrated or pushed.**

## Scope

- Shares Herald and First Vessel boss-deed credit with meaningful nearby contributors.
- Qualifies actual damage, actual healing of an active participant, or applied control at explicit thresholds.
- Requires recent activity plus live, non-spectator, same-level, 96-block proximity at encounter resolution.
- Orders qualified UUIDs deterministically, caps awards at 24, and rejects late or duplicate resolution.
- Preserves the finishing player's existing power-kill credit without granting it to other contributors.
- Bounds active/resolved encounter state and clears it on server stop.

## Test-first evidence

- Initial focused RED: `/tmp/powers-prg003-red-20260909.log`
  - SHA-256: `87e6ac1874c7ba45805d37607276f477454ccd0b75dc05fcb686ef2d7bb50dbe`.
  - Missing contribution rules and ledger classes failed compilation as expected.
- Initial live RED: `/tmp/powers-prg003-live-red-20260909.log`
  - SHA-256: `907aea776e5ff4f5ce5cf38a2b62066ce5ab9c053ffd8bcde55972a24de9b83b`.
  - The registered Herald contribution acceptance failed compilation before the production manager and read-only deed diagnostic existed.
- Support integration diagnostic failures are retained in `/tmp/powers-prg003-gametest-20260909-attempt1.log` and `/tmp/powers-prg003-gametest-20260909-attempt2.log`.
  - SHA-256: `63bdf339850d9d3fd139b11d08bd919307f9e967a24cc95f242913170380ab2b` and `250bbbd58fd14ba001a9d61469210b1779a8c7c82fc6cc060f7eb05992b39d9a`.
  - Both showed that the fixture had not established a real healing delta. It now sets damaged health explicitly before exercising the unchanged bounded Life Bloom companion scan.
- Final focused GREEN: `/tmp/powers-prg003-focused-green-final-20260909.log`
  - SHA-256: `331853571f0ac3c2c98dec8180888c2d3ced30cb1d55ec58f5ed4564ed150a18`.
  - Contribution rules/ledger, Light quest, Plant Healing, and Thunderclap rules plus all common/client/GameTest compilation passed.
- Final exact registered live proof: `/tmp/powers-prg003-exact-live-20260909.log`
  - SHA-256: `1cf85011bb387716af033b25dceb69d90018acb894d92a83364acf49050093fc`.
  - Exactly 2/2 required GameTests passed.
  - Herald damage, Life Bloom support, control, idle exclusion, and duplicate suppression passed through production hooks.
  - First Vessel contributor, finisher, preserved finisher-only power-kill credit, and duplicate suppression passed through production damage/death hooks.
- Existing Plant Healing boundary regression: `/tmp/powers-prg003-plant-regression-green-20260909.log`
  - SHA-256: `6706ab5d33c141a1eb089a35f6478922b34f56d90a8f91950cccf02adf17c6df`.
  - Exactly 1/1 required GameTest passed after an exploratory player-scan rewrite was rejected and reverted.

## Deterministic branch gate

Java 25 and a disposable `/tmp` Python dependency overlay were used.

- Log: `/tmp/powers-prg003-deterministic-green-20260909.log`
- SHA-256: `26d5b1a4c4b3a6f623eda0bae866a3146859e2f99a07068a12d279849dcaf1e5`.
- 1,840 JUnit tests: 0 failures, 0 errors, 0 skipped (426 XML suites).
- 240 Python tests passed.
- Resource validation, exact Java/non-item/VFX audits, item/magic/rank documentation checks, and all common/client/GameTest compilation passed.
- `git diff --check` passed after report reconciliation.

An exploratory unfiltered branch GameTest attempt was not acceptance: the temporary
Plant Healing scan rewrite failed its existing inclusive-boundary test and the
unchanged Living Force cadence test also failed. The scan rewrite was reverted and
the exact Plant regression passed; no unfiltered branch GameTest pass is claimed.

## Integration boundary

- Base commit: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.
- No broad/unfiltered main gate was run.
- No plan checkbox or backlog entry was closed.
- No merge, protected-main mutation, or push was performed.
- A fresh independent review remains mandatory before this unit can be considered READY.
