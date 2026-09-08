# PWR-004 implementation report

## Scope

This unit implements the Stage 5 Size Morphing improvement without changing the stage checkbox or
removing the backlog row. The existing `SizeMorphAbility` remains the authoritative owner.

## Implemented contract

- A transition previews both the candidate body and its 1.5-block forward sweep. A requested form
  that overlaps the current cavity still fails atomically; a form blocked only by the route ahead
  falls back through authored smaller scales and restores itself when clear.
- Normal-or-smaller bodies may use crouching and crawling dimensions. A crawl pose owned by Size
  Morphing is released on safe toggle-off.
- Mounted activation is rejected with explicit action-bar feedback. If a player mounts after
  activation, the next authoritative tick restores the normal body, releases toggle ownership,
  and leaves the rider attached to the vehicle.
- Block reach grows from 4.5 blocks to a 12-block cap; entity reach grows from 3 blocks to a
  10-block cap. Body mass is cubic in scale and contributes at most 0.8 knockback resistance.
- Recurring energy cost follows the actually applied fallback form and retains innate rank
  efficiency. The normal sync packet carries applied scale, both reach values, and resolved drain;
  the active HUD slot renders those server-authored values.

## TDD evidence

The following focused tests were observed failing before their corresponding production changes:

- `powers_game_tests_innate_size_morph_rejects_mounted_activation_without_changing_either_body`:
  `Mounted Size Morphing entered the authoritative toggle pipeline on tick 0`.
- `SizeMorphRulesTest.transitionPolicyExposesSafeFallbackReachDrainAndMassFeedback`:
  `NoSuchMethodException: com.powers.power.abilities.SizeMorphRules.transitionCandidates(int)`.
- `powers_game_tests_innate_size_morph_uses_doorway_fallback_then_restores_authoritative_reach_and_mass`:
  `Doorway prediction rejected the safe fallback with the selected form on tick 0`.
- `powers_game_tests_innate_size_morph_crawl_pose_is_released_when_the_toggle_ends`:
  `Toggle-off retained the forced crawl pose in clear space on tick 0`.
- `PowerStatePayloadTest.sizeMorphFeedbackCarriesAppliedScaleReachAndRankAdjustedDrain`:
  `NoSuchMethodException: com.powers.network.PowerStatePayload.sizeMorphAppliedScale()`.
- `powers_game_tests_innate_size_morph_relinquishes_its_body_when_the_player_mounts`:
  `Mounted player retained stale Size Morphing toggle ownership on tick 0`.

## Focused verification

- `SizeMorphRulesTest` and `PowerStatePayloadTest`: green.
- Filter `powers-gametest:*size_morph*`: five production-entrypoint GameTests green.
- Existing hostile low-ceiling collision regression: green.

The full machine-wide check and complete GameTest suite were intentionally not run while parallel
lanes were active.
