# INT-008 verification status

Status: **NOT READY — repaired findings require fresh exact-SHA capture and review.**

The PENDING package at `deeb3cdae69e7b0eb2825350b26773c0d2019599`
faithfully records implementation `d629acb8c86beff3ef52dacb8fd85f12d00ea57e`:
163 GameTests, 1,835 JUnit tests, and 239 Python tests passed. Its 424 raw
JUnit suites, ordered clean-SHA receipts, exact runtime rows, and deterministic
archive were independently verified. Archive SHA-256:
`e6481c45f590684b12ef4819282b47bec1fa4cc06555cb3abbd003d840bc7e17`.
The review nevertheless returned NOT READY. Those bytes remain historical
PENDING evidence, not acceptance of the subsequent repairs below. No closure or
merge is claimed; the earlier `49e1c652` / `b7acf4a2` package is also historical.

## Fourth independent review repair, 2026-08-31

Two Important gameplay findings and one Minor fact-schema finding were reproduced
before repair:

| Finding | Observed RED | Repair and observed GREEN |
| --- | --- | --- |
| Cinderheart advanced control-time simulation during global freeze | A real hovering Cinderheart disappeared during 400 frozen control ticks. A strengthened boundary also proved overdue expiry ran while world simulation was parked. | Creation, charge, launch, deflection, presence binding, and expiry use owning-world time. Simulation and expiry park under every vanilla freeze; owner lifecycle and local-freeze interruption remain control-owned. Hovering and launched cases retain their remaining world lifetime under external, own-Crystal, and other-owner Crystal freezes. |
| Shadow executor imposed a finite toggle deadline and left stale lease markers | Executor-started Time Freeze failed the 1,300-control-tick survival check. A stale body's stop callback removed a newer body's lease/marker. | Time Freeze's marker is indefinite, unlike other finite toggles. Matching lease exits retire its marker without recursive release; stale-body callbacks cannot release replacement authority. Explicit stop, both external clock values, body loss, unrelated-toggle preservation, diagnostic counts, and finite Flight expiry pass live checks. |
| Numeric JSON values impersonated booleans/integers | Seven matching transcript/JSONL mutations were incorrectly accepted, including numeric booleans and a floating-point duration. | Exact fact types are checked before values. All 22 checkout/verifier/package tests pass. |

The combined diagnostic temporal suite passes all 10 tests, including the three
new tests in isolated environments. The required full suite is now **166**, and
the verifier's fixtures and acceptance count are raised accordingly. Diagnostic
runs are not exact-SHA acceptance evidence. Initial Cinderheart selectors selected
no tests and do not count as RED; the behavioral RED used the exact registered id.
Its launch fixture was also corrected to use both crouch input and pose before
asserting measured launch velocity.

Focused Fireball, companion, lease-state, typed-time, magic-runtime, and temporal
source-boundary JUnit regressions pass, as do the regenerated Java source audit
and GameTest compilation. A prior diagnostic audit attempt detected a stale
manifest after the final source cleanup; regeneration and the unchanged rerun
passed. No failed attempt is represented as acceptance.

Retained diagnostic transcripts use the prefix `powers-int008-review4-`:
`fireball-red3.log`, `fireball-expiry-red.log`, `shadow-red.log`,
`types-red.log`, `live-green.log`, and `verifier-green.log`.

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
   166 GameTests and the unchanged literal Java 25 full check.
3. Retain actual clean-SHA pre/post receipts and raw-JUnit digest, derive totals,
   regenerate PENDING evidence, prove deterministic packages/checksums/privacy,
   and obtain fresh independent READY review.
4. Reconcile non-evidence closure documentation before the final clean-SHA
   recapture. Only evidence-package changes may follow that capture.
5. Complete closure-head and merged-main literal gates, final READY review,
   fast-forward integration, push, and clean-worktree proof before INT-009.
