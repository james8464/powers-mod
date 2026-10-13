# SPL-009 Blood Reading enhancement verification

Date: 2026-09-09

Status: **STAGED — independent review pending; not integrated or pushed.**

## Scope

- Adds a server-owned 30-second damage/healing history capped at 512 entities and eight amount-only samples per entity.
- Reports a stable, worsening, recovering, or volatile trend plus categorical force-ward and amethyst states.
- Preserves the existing player-consent check before diagnosis and stores no attacker, source, equipment, coordinate, display-name, or other private fields.
- Removes history on death and clears all retained samples at server shutdown.

## Test-first evidence

- Initial focused RED: `/tmp/powers-spl009-red-20260909.log`
  - SHA-256: `ee9e985a2e421b424743042f8e2f17ab4b50d4a2391bf54d674e10e80ad4f280`
  - Failed for the deliberately missing trend rules, history service, and extended report contract.
- Final focused GREEN: `/tmp/powers-spl009-focused-green-final-20260909.log`
  - SHA-256: `719e307657b822342305b51779ccb95fe16055daca0d410857517a033b24886c`
  - Windowing, sample/entity bounds, finite totals, trend classification, report sanitization, consent ordering, and forbidden-field checks passed.
- Exact registered live proof: `/tmp/powers-spl009-exact-live-final-20260909.log`
  - SHA-256: `399a2e3ae3b6c03ad2d32d512dee89019cb88c003bf81ca00a01bd0094e3134e`
  - Exactly 1/1 required GameTest passed.
  - Real server damage and healing reached the bounded history, then Blood Reading returned worsening, intact-force, and amethyst-dampened categories.

## Deterministic branch gate

Java 25 and the disposable `/tmp` Python verification environment were used.

- Log: `/tmp/powers-spl009-deterministic-green-final-20260909.log`
- SHA-256: `bb9c5dfbcfaf46e8bbc93dc8d9210a3f9bc45f96d9f31ac732633d64332b0429`
- 1,841 JUnit tests: 0 failures, 0 errors, 0 skipped (426 XML suites).
- 240 Python tests passed.
- Resource validation, exact Java/non-item/VFX audits, item/magic/rank documentation checks, and all common/client/GameTest compilation passed.
- `git diff --check` passed.

## Retained broad-suite diagnostic

- Unfiltered branch run: `/tmp/powers-spl009-all-green-final2-20260909.log`
- SHA-256: `4d8e36dd35986745879173f1d97cf2933e12bf86f99d6632392a7f76aadc9bf5`
- It ran all 168 registered GameTests and stopped with the existing Living Force tick-420 patrol timeout; Blood Reading's new exact test passed separately above.
- This nonzero run is retained as a failure and is not represented as a green full gate or evidence of causal independence.

## Integration boundary

- Base commit: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.
- No plan checkbox or backlog entry was closed.
- No merge, protected-main mutation, or push was performed.
- A fresh independent review remains mandatory before this unit can be considered READY.
