# ART-014 conversion-weapon presentation verification

Date: 2026-09-09

Status: **STAGED — independent review pending; not integrated or pushed.**

## Scope

- Covers the three Darkness outputs (Nocturne, Calamity Blade, and Revenant's Gravecleaver) and three Pure Light outputs (Solstice, Valhakyra, and Zenith).
- Retains each weapon's distinct authored three-dimensional model and all eight GUI, ground, fixed, head, first-person, and third-person transforms.
- Adds a deterministic four-frame pulse with an eased `0, 1, 2, 3, 2, 1` timeline. Light crests brighter; Darkness recedes toward its authored dark palette.
- Preserves alpha, normalizes invisible edge colour, and keeps frame zero's visible pixels unchanged.
- Provides an idempotent generator so the shipped sheets and metadata can be reproduced rather than hand-edited.

## Test-first evidence

- Exact-base focused RED: `/tmp/powers-art014-focused-red-20260909.log`
  - SHA-256: `84bbffcb835e4295fd89144745b68b4b0a1bc684bf4adc7877873891ec76053c`.
  - The new acceptance test failed on the exact `8c7c50b7` base because the released textures had only one frame and no animation metadata.
- Focused GREEN: `/tmp/powers-art014-focused-green-20260909.log`
  - SHA-256: `df1f3ddd8e17934bb79e915fa880ea2afc738b9a6de871a7c6f618d01ad70d3b`.
  - Generator unit tests, the six-weapon structural/pixel contract, complete VFX asset audit, and resource validation passed.

## Asset QA

- Every weapon retains all eight authored item transforms and at least 16 model elements.
- Every texture is a 128×512 RGBA sheet with four 128×128 physical frames and explicit two-tick timing.
- Peak-frame changed-pixel counts are 1,391 / 1,597 / 1,501 for the Darkness set and 1,518 / 1,196 / 793 for the Light set.
- Aggregate luminance moves downward for all three Darkness weapons and upward for all three Light weapons.
- The regenerated exact VFX manifest and owned light/dark/checker contact sheets cover every physical frame and every mip level. Reviewed exception and evidence checksum inventories remain complete.
- Existing renderer captures remain applicable to geometry and transforms because no model or item-definition file changed. A fresh independent review remains mandatory before READY.

## Deterministic branch gate

Java 25 and the disposable `/tmp` Python verification environment were used.

- Log: `/tmp/powers-art014-deterministic-green-retry1-20260909.log`
- SHA-256: `e90d3c008e8199ce8df9bf4f01fb2be1a3baf84d004bb595c4a2203f255ef1c0`.
- 1,837 JUnit tests: 0 failures, 0 errors, 0 skipped (425 XML suites).
- 242 Python tests passed.
- Resource validation, exact Java/non-item/VFX audits, item/magic/rank documentation checks, and all common/client/test compilation passed.
- `git diff --check` passed.

## Integration boundary

- Base commit: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.
- No broad/unfiltered main gate was run.
- No plan checkbox or backlog entry was closed.
- No merge, protected-main mutation, or push was performed.
- A fresh independent review remains mandatory before this unit can be considered READY.
