# INT-008 verification status

Status: **NOT READY — repaired findings require fresh exact-SHA capture and review.**

The prior PENDING package at `49e1c65284fff0a5a8cc0bd16485c5904f6e1f04`
faithfully records implementation `b7acf4a2c98107c582f6617850caede41ea0aa6f`:
162 GameTests, 1,835 JUnit tests, and 236 Python tests passed. Its independently
verified bytes remain historical evidence, not acceptance of the subsequent fixes.
No closure or merge is claimed.

## Independent review repair, 2026-08-31

The fresh reviewer identified three Important findings and one coverage finding.
Each was reproduced before its corresponding repair:

| Finding | Observed RED | Repair and focused GREEN |
| --- | --- | --- |
| External supersession left innate upkeep active | Live GameTest failed with a Time Freeze toggle still draining after an external write. | One-time toggle/release presentation cleanup runs independently of journal retirement retries; both external true/false writes clear innate and routed keys without clearing unrelated toggles. |
| Control-clock beam queries expired world-clock fields | Live GameTest failed because a beam query removed a still-live world-owned field. | Ray/field queries receive typed authoritative world time; the divergent-clock field survives and collides. |
| Verifier accepted incomplete gates and contradictory measured clocks | Thirteen mutation subcases were accepted incorrectly. | Executed full-gate tasks and passing GameTests are required; Crystal, parked-manager, and projectile measurements must obey their clock invariants. All 21 focused verifier/package tests pass with the expected full suite expanded to 163. |
| Projectile fixture replaced owned freeze with external freeze | Strengthened live assertion failed because no matching lease remained at thaw. | Fixture preserves its exact token/online owner and uses matching Crystal release; observed eight control ticks, four world ticks, zero frozen movement, and resumed movement. |

The beam case has its own isolated environment; it adds one required GameTest,
bringing the expected unfiltered suite to 163. Focused runs use diagnostic SHA
zeros and are not exact-SHA acceptance evidence. Initial beam fixture compilation
errors were corrected before the behavioral RED run; they are not counted as RED.

Affected state, time, magic-runtime, API, and temporal-boundary JUnit regressions,
the regenerated production source audit, and GameTest compilation also pass.

## Remaining acceptance sequence

1. Preserve the observed focused regression results; they do not replace full gates.
2. Commit the repaired implementation, then capture the clean exact SHA with all
   163 GameTests and the unchanged literal Java 25 full check.
3. Retain actual clean-SHA pre/post receipts and raw-JUnit digest, derive totals,
   regenerate PENDING evidence, prove deterministic packages/checksums/privacy,
   and obtain fresh independent READY review.
4. Reconcile non-evidence closure documentation before the final clean-SHA
   recapture. Only evidence-package changes may follow that capture.
5. Complete closure-head and merged-main literal gates, final READY review,
   fast-forward integration, push, and clean-worktree proof before INT-009.
