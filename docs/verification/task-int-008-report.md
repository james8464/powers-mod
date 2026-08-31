# INT-008 verification status

Status: **READY for closure reconciliation; final recapture and integration pending.**

Fresh independent reviewer `int008_review7` returned READY with no Critical,
Important, or Minor findings for implementation
`aaff0b0f88312b66d232f6e1f4ef0741a8690928` and PENDING evidence commit
`9b245015bcfe4a852fb625f58895ae9c9d3c9159`. This is not final merge approval.
The repaired source passed the exact unfiltered capture and literal Java 25
aggregate: 167 GameTests, 1,836 JUnit tests across 424 raw XML suites, and
240 Python tests. All 22 checkout/verifier/package tests independently passed.
The reviewer checked production ownership/clock paths, byte-identical XML and
runtime rows, ordered clean-SHA receipts, immutable-base/deletion inventory,
checksums, privacy, and deterministic archives. Three 437-file archives match
SHA-256 `e6bd35f53a629e78f0ac040b912d43d4fb729caf03a9e296a3c01c29d22b2dd8`.

The additional literal gate on evidence head `9b245015` failed a portal fixture
at tick 0 before JUnit. It is not a passing final-head gate. The failing fixture
is unchanged and review found no established temporal cause. The successful
`aaff0b0f` gate and that failed diagnostic are kept distinct.
These documentation changes precede a mandatory new clean-SHA recapture.
Final-head verification/review, main integration and synchronization remain
pending in the [closure ledger](evidence/2026-08-29-int-008/closure.md).

## Final review repair, 2026-08-31

The new isolated production-path GameTest sets world time two million ticks ahead
of control time, creates a real Void Scar, and advances the real shared index.
It observed the intended RED: the scar's live collision presence disappeared on
the next world tick. The repair supplies a typed world-time deadline to the index
while retaining the scar manager's authored control-time gameplay lifetime,
pulse cadence, damage and cleanup. The same live test now passes, including the
last-live/deadline boundary and owner cleanup. The full required count is **167**.

Behavioral RED and GREEN transcripts: `powers-int008-review6-scar-red3.log` and
`powers-int008-review6-scar-green.log`. Earlier `scar-red.log` failed compilation
on the existing deprecated mock-player helper; `scar-red2.log` selected no tests.
Neither is behavioral RED. Verifier fixtures were advanced to 167 before the
strict count: `powers-int008-review6-verifier-red.log` recorded rejection of that
new full-suite count; the count was then raised without weakening any checks.
All 22 checkout/verifier/package tests pass in `powers-int008-review6-verifier-green.log`.
Focused Void Beam rules, magic-runtime, typed-time and temporal-boundary JUnit tests,
the regenerated Java source audit, and item/magic/rank documentation gates pass in
`powers-int008-review6-focused-green.log`. The focused live GREEN is diagnostic;
the subsequent full post-repair capture and independent review are recorded above.

The ca1469d4 capture and 73d81c13 package/gate remain historical PENDING evidence.
They cannot stand in for acceptance of the repaired source. The final reviewer
reported no other Critical, Important or Minor findings.

Earlier independent reviewer `int008_review5` returned READY for closure reconciliation,
with no Critical, Important, or Minor findings, on implementation
`051d105f52b845f8f399995211050b627d03f113` and PENDING evidence commit
`a6fd807515529115d96e5eef011ff36ea8b54084`. This was not final merge approval.
Both the clean exact-SHA capture and unchanged literal Java 25 full gate passed
166 GameTests; the latter also passed 1,836 JUnit tests across 424 raw XML suites
and 240 Python tests. All 22 focused checkout/verifier/package tests passed.
The reviewer independently checked every XML and runtime row against its raw
capture, ordered clean-SHA receipts, source inventories, checksums, and privacy.
Three deterministic 436-file archives matched SHA-256
`c5546585e4d98612e8fe87b9100189bd1af8693abed0261dc1900a1856e8c666`.

These non-evidence documentation changes precede a new clean-SHA recapture.
The [evidence closure ledger](evidence/2026-08-29-int-008/closure.md) is authoritative
for the remaining capture, final-head review/gate, merged-main gate, push, and
clean-worktree receipts. No final integration is claimed here. Earlier packages
at `deeb3cda` / `d629acb8`, `49e1c652` / `b7acf4a2`, and their predecessors remain
historical evidence in Git history; they must not substitute for final capture.

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
2. Commit the repaired source and reconciled documentation, then capture the clean exact SHA with all
   167 GameTests and the unchanged literal Java 25 full check.
3. Retain actual clean-SHA pre/post receipts and raw-JUnit digest, derive totals,
   regenerate PENDING evidence, prove deterministic packages/checksums/privacy,
   and obtain fresh independent READY review.
4. Keep all subsequent status receipts within the evidence closure ledger.
   Only evidence-package changes may follow the final capture.
5. Complete closure-head and merged-main literal gates, final READY review,
   fast-forward integration, push, and clean-worktree proof before INT-009.
