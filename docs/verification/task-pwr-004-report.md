# PWR-004 implementation report

Status: **review repair staged; fresh independent rereview and full gate pending.**

## Scope

This unit implements and repairs the Stage 5 Size Morphing improvement without changing the stage
checkbox or removing the backlog row. `SizeMorphAbility` remains the authoritative owner.

## Implemented contract

- Every scale transition previews the candidate body and its 1.5-block forward sweep. Fallbacks
  remain monotonic, use only authored scales unlocked at the player's effective rank, and restore the
  selected form when the route clears.
- Normalization performs a vanilla-scale collision preflight across standing, current, crouching,
  and swimming poses before removing the scale modifier. Toggle-off uses a safe compact pose where
  available and is rejected atomically where no vanilla pose fits.
- Mounted activation is rejected. A player who mounts while morphed is normalized without
  dismounting when a vanilla pose fits; if none fits, the unsafe mount is rejected while the safe
  transformed body and toggle ownership remain intact.
- Block reach grows from a morph-owned 4.5-block baseline contribution to a 12-block profile cap;
  entity reach similarly grows from 3 to 10 blocks. Morph modifiers compose with rank and third-party
  modifiers and remove only their own identifiers.
- Body mass is cubic in scale and contributes at most 0.8 knockback resistance. Recurring drain and
  HUD values use the actually applied fallback scale and retain innate rank efficiency.
- Fallback and restoration call the normal production sync path immediately. The real outbound
  connection test observes applied scale, live reach, and resolved drain in `PowerStatePayload`.
- Disconnect/respawn cleanup forgets the per-player forced-crawler preference, and server shutdown
  clears the complete UUID set.

## TDD evidence

The repair was developed in red/green cycles. The following focused regressions were observed failing
before their corresponding production changes:

- `SizeMorphRulesTest.transitionPolicyExposesSafeFallbackReachDrainAndMassFeedback`: compilation
  failed because rank-aware `transitionCandidates(int, int)` did not exist.
- `innateSizeMorphPreservesForeignReachAndRankMassModifiers`: failed with
  `Size Morphing replaced a foreign block-reach contribution`.
- `innateSizeMorphToggleOffNormalizesThroughASafeCompactPose`: failed with
  `Confined toggle-off did not choose the collision-free vanilla crawl pose`.
- `innateSizeMorphRejectsToggleOffWhenNoVanillaPoseFits`: failed because toggle-off succeeded in a
  half-block cavity with no collision-free vanilla pose.
- `sizeMorphFallbackAndRestorationSyncActualProfileImmediately`: failed because fallback emitted no
  immediate production state payload.
- `innateSizeMorphDisconnectForgetsForcedCrawlerPreference`: failed because disconnect retained the
  old UUID's standing-first crawler preference.

## Verification

- Focused JUnit: `SizeMorphRulesTest` and `PowerStatePayloadTest`, 10 tests, green.
- Complete JUnit: 1,838 tests, zero failures, green.
- Exact focused Fabric GameTests: filter `powers-gametest:*size_morph*`, 11/11 required tests, green.
- Common, client, example-extension, and GameTest Java compilation: green.
- Main resource processing and strict POWERS resource validation: green.
- Java-source and non-item-asset audits were regenerated; both exact-manifest checks are green.
- Generated item, magic, and rank documentation checks: green.
- `git diff --check`: green.

Fresh repair evidence:

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-pwr004-repair-focused-final-20260909.log` | `7c81de4c86ec97dcfaac6cf531d95fd138800a0dc477a004821b9733d4d7088d` |
| `/tmp/powers-pwr004-repair-live-final-20260909.log` | `78e1d0c2ec8227e86a743cf917914563c859101be4b4114224cbcb081266c159` |

## Explicit limitations

Per task instruction, the machine-wide main gate and unfiltered Fabric GameTest suite were not run.
No real-client visual capture is claimed; HUD correctness is covered at the serialized production
connection boundary and by the client-state/unit coverage already in the PWR-004 unit.
