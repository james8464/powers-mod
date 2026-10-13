# ART-016 tag-first food affinity verification

Date: 2026-09-09

Status: **STAGED — independent review pending; not integrated or pushed.**

## Scope

- Keeps `powers:food_neutral`, `powers:food_abnormal`, and `powers:food_normal` authoritative, in that fail-safe precedence order.
- Defaults every untagged third-party food to neutral, including IDs whose paths imitate vanilla food.
- Preserves released vanilla and POWERS food classifications through the explicit compatibility rosters.
- Prevents an unrecognized integration food from silently applying the Darkness hunger penalty.

## Test-first evidence

- Initial focused RED: `/tmp/powers-art016-red-20260909.log`
  - SHA-256: `d08e91d5c9667b6dcb65588ec78fa54b8f08179e623376813c50b8af383cc3d9`
  - Both new third-party neutrality cases failed against the released normal-food fallback.
- Focused GREEN: `/tmp/powers-art016-focused-green-20260909.log`
  - SHA-256: `a97badec2d0557eba7176041715fe3449c2c5112fd766eb4925b55fe913e46d3`
  - Four focused affinity cases and GameTest compilation passed.
- Exact registered live proof: `/tmp/powers-art016-exact-live-20260909.log`
  - SHA-256: `47629a1f43c7e714b5d3f4363d4a225df06e1b61eeeb4acb27a1a3f6dd7243f7`
  - Exactly 1/1 required GameTest passed.
  - Runtime-loaded item tags classified bread as neutral, rotten flesh as abnormal, and cooked beef as normal.

## Deterministic branch gate

Java 25 and the disposable `/tmp` Python verification environment were used.

- Log: `/tmp/powers-art016-deterministic-green-20260909.log`
- SHA-256: `9d468d8e05301dcad44825a64bee94971d2d977d7df3fd98450ab6286947f272`
- 1,838 JUnit tests: 0 failures, 0 errors, 0 skipped (424 XML suites).
- 240 Python tests passed.
- Resource validation, exact Java/non-item/VFX audits, item/magic/rank documentation checks, and all common/client/GameTest compilation passed.
- `git diff --check` passed.

## Integration boundary

- Base commit: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.
- No broad/unfiltered main gate was run.
- No plan checkbox or backlog entry was closed.
- No merge, protected-main mutation, or push was performed.
- A fresh independent review remains mandatory before this unit can be considered READY.
