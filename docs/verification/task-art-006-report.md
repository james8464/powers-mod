# ART-006 Heart Relics Verification Report

## Status

`STAGED — independent review pending; not integrated or pushed.`

The implementation is isolated on `art-006-heart-relics`, based on exact clean
main commit `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`. The protected main checkout and
`origin/main` were not changed.

## Implemented boundary

- Every villager produces exactly one Living Heart in a guaranteed, single-item
  loot pool. The existing produce roll remains a separate optional pool.
- Light may eat Living and Wildwood hearts; Darkness may eat Ghoul and
  Bloodstone hearts; either alignment may eat a Clockwork Heart. Invalid use is
  rejected before consumption and explained to the player.
- One deterministic hierarchy selects the only carried-heart passive owner:
  Living I < Ghoul II < Wildwood III < Clockwork IV < Bloodstone V. Duplicate
  and mixed hearts cannot stack passive effects or death wards.
- Eating uses vanilla consumption timing and consumes exactly one item. Each
  relic retains a distinct item model, tooltip, sound identity, active effect,
  and strongest-only passive presentation.

## TDD and live proof

| Proof | Result | Retained evidence | SHA-256 |
|---|---|---|---|
| RED compile | Missing alignment/tier/selection contracts failed compilation | `/tmp/powers-art006-red-20260909.log` | `54879d75cac0e5e29e17e9fa201759ac0eff2f17c8460b1674e3951c17e50a95` |
| Focused GREEN | Rules, loot, and GameTest compilation passed | `/tmp/powers-art006-focused-green-20260909.log` | `331853571f0ac3c2c98dec8180888c2d3ced30cb1d55ec58f5ed4564ed150a18` |
| Eating GameTest | **1/1 required passed** | `/tmp/powers-art006-eating-live-20260909.log` | `8e39aa7b17966f7fd91d939e48c73512f83e943824a8ce3dae90e8ca33cbb2b8` |
| Passive GameTest | **1/1 required passed** | `/tmp/powers-art006-passive-live-20260909.log` | `c2348dfb2ec2fe1863031a14f662faaf2f6e8abe6dc3bafbd8ef805c0dc24b91` |

The eating test proves wrong-alignment rejection and exact one-item
consumption. The passive test carries multiple hearts and proves that only the
highest tier owns the runtime effect.

## Deterministic branch gate

With Java 25, the bundled Python runtime, isolated Python dependencies, and run
ID `art006-20260909-final2`:

`./gradlew check compileGametestJava -x runGameTest --rerun-tasks --no-daemon --console=plain`

completed **GREEN** in 2m 2s. Mechanical XML aggregation found **424 suites,
1,839 tests, 0 failures, 0 errors, and 0 skipped**. Python reported **240/240
passed**. Resource validation, generated documentation, Java/non-item/visual
asset audits, the example extension, and GameTest compilation all passed.

Retained log: `/tmp/powers-art006-deterministic-green-20260909.log`

SHA-256: `a4f72afecbe64bb5a877c6a1ae99f650a25819c95dd0068d649b977eb9c796a9`

## Integration boundary

- No plan checkbox or backlog row is closed by this staged commit.
- No merge, protected-main mutation, push, or publication was performed.
- A fresh independent review must return READY before any integration decision.
