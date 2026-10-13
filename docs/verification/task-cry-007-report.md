# CRY-007 implementation report

Status: **implementation staged; aggregate main gate and independent review pending.**

## Scope and dependency

This unit implements the Stage 5 Yellow Size Shift improvement without changing the stage checkbox
or removing the backlog row. It is intentionally stacked on the PWR-004 repair commit
`8391598639922044abd52ead60c93e6d54385ce9`: CRY-007 requires Yellow to reuse the same
collision-preview service as innate Size Morphing.

## Implemented contract

- The server previews the complete destination body and a 1.5-block forward sweep before changing
  the player's scale, pose, selection state, effects, or cast ownership.
- A blocked transition is atomic: the current safe scale and authoritative hitbox remain in place,
  and the player receives the localized collision reason.
- Mounted activation is rejected before any mutation with the distinct localized dismount reason.
  Mounting during an active cast normalizes only when a vanilla-sized body fits; otherwise the
  unsafe mount is ended while the existing safe body remains intact.
- Expiry and authority-loss cleanup also wait for a collision-free vanilla pose. This prevents a
  tenfold or miniature body from being normalized into suffocation geometry.
- Yellow and innate Size Morphing now share `SafeScaleTransitionService`; PWR-004's authored
  fallback, crawl, restoration, reach, mass, and ownership behavior remains unchanged.
- Transitions emit a bounded ground-level ring and push at most 16 living targets without damage.
  The old dense eye-adjacent bursts were removed, and miniature/titan sounds use comfortable,
  scale-specific pitches.

## TDD evidence

The RED run failed compilation because the new deterministic selection, mount gate, bounded
shockwave/comfort rules, and shared transition-policy methods did not exist. The retained raw log is
`/tmp/powers-cry007-red-20260909.log` with SHA-256
`6863a67b2332918f9fd1abcfb980fff67acbf64661de515fde3c38f0c6094a53`.

## Verification

- Focused JUnit: 16 tests across Yellow rules, shared transition rules, PWR-004 rules, serialized
  power state, and modifier isolation; zero failures, errors, or skips.
- Yellow live Fabric GameTests: 2/2 required tests green. They prove collision rejection is atomic,
  the later clear titan transition owns a synchronized 10×/6-block-wide hitbox, the bounded
  shockwave moves a nearby target, and mounted activation preserves both the mount graph and scale.
- Shared PWR-004 regression Fabric GameTests: exact `powers-gametest:*size_morph*` filter, 11/11
  required tests green.
- Existing all-crystal production acceptance: exact `powers-gametest:*ordinary_crystal*` filter, 1/1
  required test green after moving its Yellow fixture into explicit clear space.
- Main, client, example-extension, and GameTest Java compilation: green.
- Java-source and non-item-asset audits were regenerated and verified; strict resource validation
  is green; `git diff --check` is green.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-cry007-focused-green-final-20260909.log` | `c5f0c725b72e6d91e33b18792b6ee881dc4586eddeb5947ac967553c57adb6d2` |
| `/tmp/powers-cry007-live-final-20260909.log` | `505c6cf32939bce46cee14f57550570c439a0b34f9df25de894672ce7f6ebea5` |
| `/tmp/powers-cry007-shared-scale-live-final-20260909.log` | `ce5745a0357b3c0acdc4c2e93911110c80a9fb4b3145852b81103fced61a9a65` |
| `/tmp/powers-cry007-existing-crystal-live-final-20260909.log` | `42f92452d05298f54def7a7b307c1d058ce6f80359a1f8d7977b0946fd97cac5` |

## Explicit limitations

Per the continuation gate, the machine-wide main gate and unfiltered Fabric GameTest suite were not
run for this isolated unit. No independent READY verdict or real-client visual capture is claimed.
Camera comfort is evidenced by the production choreography boundary: transition particles are now
ground-level and bounded rather than dense around the player's eyes.
