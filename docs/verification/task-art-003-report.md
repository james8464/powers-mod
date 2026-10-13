# ART-003 relic attunement verification

Date: 2026-09-09

Status: **STAGED — independent review pending; not integrated or pushed.**

## Scope

- Gives every imported ring and amulet a visible named school and exact numerical tooltip.
- Warding grants Resistance II and Verdancy grants Regeneration I only at or below half health.
- Channeling restores two energy per second only while using an item.
- Recovery restores one or two energy per second only at or below 25% energy.
- Resolves duplicate and weaker relics to one strongest profile per school, then retains the global six-energy-per-second passive relic cap.

## Test-first evidence

- Initial focused RED: `/tmp/powers-art003-red-20260909.log`
  - SHA-256: `2fef20d73a1641513eb65fb83377b008f63f7c0674bd34a3314e409d2b74834f`.
  - The new school, profile, strongest-per-school, and situational energy contracts failed compilation before production support existed.
- Final focused GREEN: `/tmp/powers-art003-focused-green-20260909.log`
  - SHA-256: `331853571f0ac3c2c98dec8180888c2d3ced30cb1d55ec58f5ed4564ed150a18`.
  - All imported-artifact profile and boundary tests plus common/client/GameTest compilation passed.
- Exact registered live proof: `/tmp/powers-art003-live-20260909.log`
  - SHA-256: `fa96ccdcd68fd6ffb6558da67fa086882d9499d36898ec097474fa2a6a45f2ac`.
  - Exactly 2/2 required GameTests passed.
  - Duplicate/weaker Recovery rings produced only the strongest two-energy result.
  - Warding and Verdancy applied their exact bounded wounded effects, while duplicate Channeling amulets restored exactly two energy.

## Deterministic branch gate

Java 25 and a disposable `/tmp` Python dependency overlay were used.

- Log: `/tmp/powers-art003-deterministic-green-20260909.log`
- SHA-256: `fa1808f0b171cbcd280fd6c40472df2959d3b9fcf2e5cdda722fbbdcd728cdc8`.
- 1,840 JUnit tests: 0 failures, 0 errors, 0 skipped (424 XML suites).
- 240 Python tests passed.
- Resource validation, exact Java/non-item/VFX audits, item/magic/rank documentation checks, and all common/client/GameTest compilation passed.
- `git diff --check` passed after evidence reconciliation.

## Integration boundary

- Base commit: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.
- No broad/unfiltered main gate was run.
- No plan checkbox or backlog entry was closed.
- No merge, protected-main mutation, or push was performed.
- A fresh independent review remains mandatory before this unit can be considered READY.
