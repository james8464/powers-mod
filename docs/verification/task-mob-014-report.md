# MOB-014 — ongoing boss/action acceptance

## Darkness artifact Time Shift

The isolated server case passed. The selected artifact action requested coordinates, and the server activation service spent energy and moved the caster to the exact destination. Both Heralds and the First Vessel arrived alive at their original relative offsets. The fixture exercises coordinate activation through the server service. Packet admission is covered separately.

Log: `/tmp/powers-artifact-time-shift.JYRPJfgU`.

## Darkness artifact Astral Projection

The isolated server test passed with all three bosses present. A connected Survival server player spent energy, entered spectator form, and left a physical body. Removing the artifact returned the player to the anchor, restored Survival, and cleared projection, body, and participant state. Boss health remained unchanged.

Log: `/tmp/powers-artifact-astral-player.wqxeIHww`.

## Darkness artifact Super Speed

Three isolated server cases passed. The selected route spent energy and raised the movement-speed attribute. The modifier remained at tick 200 and returned to baseline by tick 230, with boss vitality unchanged. Client camera behavior and moving collision effects were outside this fixture.

Logs: `powers-artifact-super-speed-boss-cuud2a0d`.

## Darkness artifact Speed Burst

Three isolated server cases passed. The selected dash spent energy and applied forward velocity. A wall bounded the dash, and the resulting endpoint shockwave reduced each boss's vitality.

Logs: `powers-artifact-speed-burst-boss-uaguy_9j`.

## Darkness artifact Time Freeze

The isolated server test passed with both Heralds and the First Vessel ticking before the cast. Their positions and entity ages stayed fixed for five control ticks while the world clock was frozen. Toggling the selected artifact action off released the clock lease and let all three bosses resume ticking.

Log: `/tmp/powers-artifact-time-freeze.sjfMjh8r`.

The first shared-fixture regression run passed artifact Chrono Stop. Blue Crystal stopped before casting because two bosses had yet to tick. The revised fixture waits for every boss to tick five times before casting. Artifact Time Freeze and both Chrono Stop routes passed with that fixture, including the original 1,200-control-tick deadline for Chrono Stop.

Logs: `powers-chrono-regression-_2ijw22m` and `powers-temporal-warmup-kciqew_9`.

## Darkness artifact Invisibility

Three isolated server tests passed. The selected route applied the invisibility effect and updated the server visibility flag. A successful player-attributed hit on each boss removed the effect and cleared the artifact toggle. The tests cover the veil effect and attack reveal. Boss detection and client rendering are outside this fixture.

Logs: `powers-artifact-veil-ticked-yot3x5lv`.

## Artifact Flight

Both artifact alignments passed with all three bosses present. The selected route initialized Flight, cleared its state on deactivation, and applied Slow Falling. Vanilla flight flags and boss health were unchanged. These tests cover setup and teardown. They do not exercise client propulsion or latency.

Logs: `powers-artifact-flight-bosses-qkmdwhum`.

## Darkness artifact innate Size Morph

One isolated server test passed with all three bosses present. The selected innate artifact route applied the 2× scale and increased entity reach. Toggling it off restored both values. Boss vitality was unchanged.

Log: `/tmp/powers-artifact-morph.DX9smnow`.

## Artifact Plant Healing

Both artifact alignments passed isolated server tests. The selected crouch cast spent energy, healed the caster and another nearby player, and left the health of all three nearby bosses unchanged.

Logs: `powers-artifact-plant-healing-o9pt27gi`.

## Matrix gaps before Plant Healing

The focused matrix suite ran 35 tests. Only the exhaustive response assertion failed. Its XML identifies 22 unclassified routes: nine Darkness innate, two Light innate, three Darkness unique, and eight Light dominion. The remaining coverage is tracked by exact action ID in the retained result.

Evidence: `/tmp/powers-matrix-inventory-current.8rR5Fynu`.

## Darkness artifact Ice Manipulation

Three isolated server cases passed. The selected artifact route spent energy, immediately reduced boss vitality, and applied frozen ticks to both Heralds and the First Vessel.

Logs: `powers-artifact-boss-ice-q6on0lk8`.

## Darkness artifact Energy Drain

All three isolated cases passed with passive regeneration blocked. The selected artifact cast spent energy, completed the channel, and reduced each boss's vitality. The energy history recorded a positive refund within the boss's health-scaled conversion cap. Net energy balance was unsuitable because it also reflects spending and other recovery.

Logs: `powers-drain-isolated-refund-8in_a9uf`. The earlier damage and net-balance run is `powers-artifact-energy-drain-m1_viuor`.

## Darkness artifact Gravity Displacement

Nine isolated server tests passed for the selected Pull, Orbit, and Repel modes against both Heralds and the First Vessel. Every cast spent energy and applied steering. Pull reduced distance to the caster, Orbit moved the boss off the original caster-to-target axis, and Repel increased distance by tick eight.

Logs: `powers-gravity-all-modes-xyyixq5z`. The earlier Pull-only run was interrupted by low disk space during Light Herald compilation. That case passed separately in `/tmp/powers-gravity-light-retry.Vz3eN8eH`.

## Darkness artifact Breezy Bash

Three isolated server tests passed. The selected artifact cast spent energy and applied upward velocity. Each actively ticking boss rose above its starting position by tick eight and received downward slam velocity by tick 19. The original immobilized fixture only demonstrated impulse assignment.

Logs: `powers-breezy-bash-displacement-36lxe_wf`.

## Darkness artifact Fireball

Three isolated server cases passed. Each selected artifact cast spent energy, hovered without damaging the boss, and dealt damage after the second activation with the caster crouched.

Logs: `powers-artifact-boss-fireball-72ntru_i`.

## Darkness artifact Void Beam

All three isolated server cases passed. The selected artifact route spent energy, left boss vitality unchanged through tick 10 of the charge, and damaged both Heralds and the First Vessel by tick 16.

Logs: `powers-artifact-void-beam-sqmtjoyg`.

## Artifact Starfall and Thunderclap

All six isolated Starfall cases passed. Both artifact alignments paid for the cast, left boss vitality unchanged at cast time, and damaged each boss during the storm. Both artifact Thunderclap cases passed against all three bosses in the forward cone. The focused response matrix tests passed for both abilities.

Logs: `powers-artifact-boss-starfall-fwbemog1`, `powers-artifact-boss-thunderclap-c79pq0xf`, `/tmp/powers-starfall-verification.BByoBRHb/matrix.log`, and `/tmp/powers-thunderclap-matrix.Csw1DOsZ`.

## Artifact Lightning Strike

All six isolated server tests passed. Both artifact alignments spent energy, preserved the warning before impact, and reduced vitality for each Herald and the First Vessel. The new matrix assertion reproduced the missing classification before the update.

Logs: `powers-artifact-boss-lightning-juimyup5` and `/tmp/powers-lightning-matrix.218wBkDl/`.

## Artifact Energy Beam

All six live cases passed for selected Light and Darkness artifact Energy Beam against both Heralds and the First Vessel. Each cast spent energy and reduced boss vitality during the two scheduled damage beats. The focused matrix test failed on the missing classifications before the update and passed afterward.

Logs: `powers-artifact-boss-beam-r68e0xhy`, `/tmp/powers-beam-matrix-red.dBDoSTSn`, `/tmp/powers-beam-matrix-evidence.L1rlw2u9/red.xml`, and `/tmp/powers-beam-matrix-green.vWswbWZY`.

## Candidate unit suite and remaining routes

Java 25 ran 2,008 unit tests. The exhaustive boss response matrix was the sole failure. Raw XML and the transcript are retained in `/tmp/powers-candidate-junit-evidence.gB3NtBS4`.

The exhaustive assertion now names each missing route and its affected bosses. The focused rerun found 36 unclassified artifact routes: 19 Darkness innate, three Darkness unique, six Light innate, and eight Light dominion. See `/tmp/powers-matrix-inventory.WKATezxL` and its JUnit XML.

## Forcefield damage admission

Three isolated GameTests passed for innate, Darkness artifact, and Light artifact Forcefield against both Heralds and the First Vessel. Each test used a connected Survival server player with the client-loaded handshake. The normal damage path absorbed the breaking boss-attributed hit, consumed the ward, and admitted the follow-up hit. Boss attacks were supplied by the fixture; autonomous boss attack timing is outside these tests.

Logs: `powers-forcefield-loaded-0i_d_por`.

## Three-boss realm travel

All seven realm routes passed in fresh server runs with both Heralds and the First Vessel in each cohort. The response matrix now records all 21 route and boss pairs as effective. The focused matrix test first failed on an unclassified route, then passed after classification.

Logs: `powers-three-boss-realms-0a4gp9oy`, `/tmp/powers-realm-matrix-red.UBCvTAde`, `/tmp/powers-realm-matrix-evidence.3LKacuma/red.xml`, and `/tmp/powers-realm-matrix-green.56nBtZ3a`.

## Artifact realm travel

Four artifact routes carried a Dark Herald and returned both travellers. Darkness artifact travel to the Dark and Light Realms passed before the fix. Middleworld initially blocked the artifact return through its magic suppression gate. The shared activation service now allows an established realm return, matching crystal activation while preserving artifact authorization and return placement checks. The Middleworld regression and Light artifact Light Realm round trip both passed after the fix.

Raw log directories under the macOS temporary directory: `powers-artifact-realms-sysrtuch`, `powers-artifact-return-lovbvr94`, and `powers-artifact-return-fix-igo1_nuh`. Coverage uses one boss type. The full release gate remains open.

## Crystal realm travel

The selected Dark and Light Crystal routes each carried a Dark Herald into the shipped realm and returned the caster and boss to the source world. The tests checked entity identity, return tracking, and body session cleanup. The caster held the required Darkness rank for realm exit.

The Indigo Crystal route initially refused outbound activation at tick 0. Captured feedback identified the missing Middleworld entry in the GameTest world preset. After adding the shipped Middleworld definition to that preset, the focused Indigo test passed the same outbound, return, and cleanup assertions.

Raw logs: `/var/folders/1p/373qyqz52f3820y62mkbfq880000gn/T/powers-realm-crystals-aebgrl2a/`, `/tmp/powers-light-realm.ApKeBRWd`, `/tmp/powers-indigo.W8GbZrvN`, and `/tmp/powers-indigo.kSOQBxJO`.

## Chrono Stop — 2026-10-13

Both real-route cases passed in `boss-chrono-ticking-c8yvemts/` (15s each):
Blue Crystal and Darkness artifact, each exercising all three bosses together.
The fixture first proves every boss is actively ticking in retained test chunks.
Activation spends energy, sets3600-tick cooldown, owns the real vanilla freeze,
and sets an exact1200-control-tick deadline. A server-control callback checks
unchanged world time, boss ages andpositions through1199ticks, then verifies
release at1200 andsubsequent resumed boss ticking. Artifact scaling does not
extend that explicit temporal cap. Test chunks are released after success.

Initial `boss-chrono-stop-98o9u3f_/` failed the resume assertion; it had not
established active ticking across its multi-chunk fixture. Retained as a fixture
failure, not evidence that previously inactive bosses were correctly frozen.
No production Chrono change was needed. Classification RED retained in
`boss-chrono-classification-red.xml` andrawlog; only the two verified routes
classified SETUP_EFFECTIVE.

Full JUnit:2,006 tests/474suites,2,005pass,onefailure,zeroerrors/skips(64s).
Sole failure remains exhaustive boss-action classification. Raw XML/log:
`boss-chrono-junit-20261013-pPzL1X/`; sorted XML inventory SHA-256:
`d3f25d05ec3fabbb00b40627ffb410d2fc78d07e79e4ee3ce88852537a08d583`.
Source audit/check anddiffcheck pass. No full acceptance claim.

## Soul Link — 2026-10-13

Six fresh-server real-route cases passed in `boss-soul-link-bcwnr4i1/`:
Violet Crystal and held Darkness artifact against each boss. Actual activation
spends energy, starts2400-tick cooldown, and binds both boss and a live wound
donor. Two real donor hits—early andnear the end of the200/280-tick lease—
reduce boss vitality, with no repeated bounce between those hits. After expiry,
the topology andall three participants' mind-session ownership are removed;
a subsequent donor hit no longer affects the boss. First Vessel assertions use
effective vitality. This batch does not newly prove exhausting the20-damage cap.
No Soul Link gameplay change was needed. Classification RED retained in
`boss-soul-link-classification-red.xml` andcorrespondingrawlog, then only the
two verified routes classified EFFECTIVE.

Full JUnit:2,005 tests/474suites,2,004pass,onefailure,zeroerrors/skips(63s).
Sole failure remains exhaustive boss-action classification.
Raw XML/log: `boss-soul-link-junit-20261013-ya7QCv/`; sorted XML inventory SHA-256
`0292837f6f331f72b454c82d758a3943fafaaaeb8e3d64b56554e833494023d5`.
Source audit/check anddiffcheck pass. Full acceptance remains open.

## Creativity Manifestation — 2026-10-13

Nine real-route cases passed in `boss-creation-aim-kqs0khkh/`: Orange Crystal
mode selection plus Light/Dark artifact selection against each boss, actual
chamber materials at the looked-at anchor, energy payment,200-tick cooldown,
and unchanged adjacent boss health. Initial `boss-creation-vp057ixd/` failed
because the fixture changed current aim without updating the previous aim used
by the raycast. Synchronized fixture position/aim and an explicit hit assertion
correct that setup; the failure is retained, not claimed as gameplay RED.

A separate real-route empty-sky test exposed a production bug: a MISS is also
a BlockHitResult, so the ability accepted it as a placement anchor. Actual RED:
`powers-creation-miss-red-20261013.log`. The ability now additionally requires
HitResult.Type.BLOCK. Eleven fresh-server cases then passed in
`boss-creation-anchor-hmz8pwqg/`: all nine valid routes, miss refusal with no
terrain/payment/cooldown, and existing atomic blueprint/stripped-template proof.
Only the three verified creation routes were classified SETUP_EFFECTIVE after
the retained classification RED (`boss-creation-classification-red.xml` andlog).

Full JUnit:2,004 tests/474suites,2,003pass,onefailure,zeroerrors/skips(65s).
The sole failure remains exhaustive boss-action classification.
Raw XML/log: `boss-creation-junit-20261013-jjpvT7/`; sorted XML inventory SHA-256
`c1157e3533bd30f78a46a58b4a789edc0884faa83aa1b495bd0b1e4d9d268aba`.
Audit/diff checks pass. README andchangelog describe the actual-block boundary.
No full acceptance, integration, or release claim.

## Clone owner lifecycle — 2026-10-13

All eight fresh-server Clone Swarm cases passed in
`boss-clone-dead-owner-_qw3c0h0/` (14–16s each), including the six existing
engagement/allegiance/expiry cases and two new dead-owner cleanup cases.
The latter verify three live echoes before owner death, then entity removal
within20ticks after establishing zero health and invoking the owner death
lifecycle. This tests the death-cleanup boundary, not lethal damage admission
or a connected client's disconnect. No production cleanup change was needed.
The earlier `boss-clone-owner-death-qzquwupx/` failure is retained: applying
damage did not kill the mock player, so it did not exercise clone cleanup.

## Size Shift routes — 2026-10-13

Six fresh-server cases pass in `boss-size-shift-xu2jamc7/` (14–16s each):
real Yellow Crystal and Darkness artifact selection/activation beside each boss,
energy consumption,600-tick cooldown,0.0625-scale body, nearby shockwave,
near-end400/560-tick leases, and return to normal scale without boss damage.
These cases cover the initial small form; giant-form collision/shockwave has
separate existing direct-activation coverage, not newly claimed routed evidence.
No Size Shift production change was needed. Classification regression observed
UNCLASSIFIED before adding only these two routes as SETUP_EFFECTIVE; raw log
andXML retained as `powers-boss-size-classification-red-20261013.log` and
`boss-size-classification-red.xml`.

Full JUnit:2,003 tests/474suites,2,002pass,onefailure,zeroerrors/skips(65s).
Sole failure remains the exhaustive boss-action classification test.
Raw XML/log: `boss-size-junit-20261013-AaNVSO/`.
Sorted XML filename/content inventory SHA-256:
`2ca84b3326e9048a0ed57f998fbeebab0cead9adee4649246db9f6c7225d1dd6`.
Source audit/check anddiffcheck passed; full acceptance remains open.

## Artifact scaling investigation — 2026-10-13 (open)

Clone Swarm exposed a shared scaling defect: artifact invocation reached an
ability helper that only permitted innate-rank scaling, so it selected the
unranked fallback instead of the explicit artifact profile. A Darkness clone
retained 80 HP and expired at age 1,200, rather than baseline artifact 200 HP
and 1,680 ticks. Actual diagnostic RED:
`powers-boss-clones-state-20261013.log`; initial run `boss-clones-3pl_wps6/`.
Another fixture attempt failed to engage by tick 120 and remains retained; that
is not evidence of lifetime failure.

`Ability.scaling` now forwards ARTIFACT through `PowerScalingService.forPlayer`,
which uses only its explicit equipment profile, not innate rank. The focused
First Vessel run passes (`powers-boss-clones-scaling-helper-20261013.log`, 15s).
An initial edit accidentally matched the cooldown helper; it was reverted before
applying the intended scaling-helper change. Its failed diagnostic is retained.
Broader affected-route verification is pending; no scaling completion claimed.

Follow-up run `boss-artifact-scaling-1unaq2fc/` passed the two hostile artifact
clone cases, then failed the allied idle lifetime case. Diagnostic
`powers-boss-clones-idle-expiry-20261013.log` showed ages 213, 41, and zero at
test tick 1,685: the enlarged arena crossed inactive chunks, so this was a
fixture-ticking failure, not production expiry evidence. The fixture now forces
only its surrounding 3×3 chunks during the test, warms them for 20 ticks, and
releases them after verification. Production EchoClone expiry is unchanged.

After ticking was fixed, the Light Herald case still had three correctly scaled
clones targeting the boss without dealing damage. `GuardianTacticalGoal` owned
MOVE/LOOK above melee and held ranged spacing even for EchoClone, whose ranged
attacks are disabled. Actual RED is retained in
`boss-artifact-scaling-active-_dpz_cha/002-boss_clone_game_tests_artifact_clones_fight_light_herald.log`.
Its admission now checks `usesSharedRangedCombat`; melee-only clones and bosses
with their own combat catalogue no longer run that shared ranged movement goal.
All twelve affected Clone/Inferno fresh-server cases passed in
`boss-clone-melee-8hpg2_ni/` (14–16s each), including hostile engagement,
allied restraint, explicit clone health/lifetime, and near-end Inferno activity.
Broader shared-code regressions and full acceptance remain outstanding.

Follow-up duration investigation: `artifact-scaling-regressions-vql7ws0w/`
stopped on the first Dreamwalking case because the new252-tick expectation
ignored the documented thirty-second shared control ceiling. That ceiling
applies before30% boss resistance, yielding180ticks, not252. The earlier
`powers-artifact-dreamwalk-duration-red-20261013.log` therefore does NOT prove
a gameplay defect. Removed the unnecessary Dreamwalking scaling-call change,
restored the180-tick expected lease, and retained failed diagnostic logs.
Artifact possession correctly expects84ticks: its280-tick scaled input does
not reach the ceiling. The37-case control/support/FirstVessel regression run
finished with34 passes under `artifact-capped-regressions-nvdxnnem/`, then
stopped on First Vessel live melee: health decreased, but no MOB_ATTACK was
observed by tick60. All21 control and12 support cases passed. One telegraph
case also passed; the final two were not reached in that run.

Adding only assertion diagnostics, the exact melee case passed in14s
(`/tmp/powers-vessel-melee-regression-diagnostic-20261013.log`). Three further
fresh-server repeats and the two remaining warning cases passed in
`vessel-melee-repeat-pprsqzzj/`. No production change was made for this
intermittent failure; its cause and independence remain unproven. It must not
be represented as a clean original37-case batch.

Full JUnit completed in64s: **2,002 tests /474 suites,2,001 pass,1 failure,
zero errors/skips**. Sole failure:
`BossActionResponseMatrixTest.everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss()`.
Raw before/afterXML andlog retained in
`boss-artifact-regression-junit-20261013-NHfaUr/`.
Sorted afterXML filename/content inventory SHA-256:
`72a044c1d14d89a421a625dbe126c569fc7b38c42ef1de4b05b576d1b5a6839d`.
Source audit/check anddiffcheck pass. No full acceptance claim.

Correction to earlier Inferno evidence: checking only after tick 230 proved an
upper bound, not activity throughout the claimed 224-tick artifact lease. The
strengthened test now checks actual damage/reignition at ticks 205–217 as well.
Earlier artifact-focused results are historical and require revalidation after
this shared change, including possession duration (baseline 200 × 1.4 × 0.3 =
84 ticks, not the previously observed unscaled 60). No gate is weakened.

## Inferno routes — 2026-10-13

Six isolated live cases pass in `boss-inferno-4nrbvnb0/` (14–17s each): Red
Crystal and held Darkness artifact against each boss. Actual registered crystal
activation or authorized artifact selection/activation spends energy and starts
the independently specified 1,800-tick cooldown. Each boss loses vitality and
ignites. First Vessel assertions use effective vitality rather than its layered
vanilla-health adapter. After the Red Crystal's 160-tick or baseline Darkness
artifact's 224-tick lease, extinguishing residual fire reveals no further
ignition or damage over 25 ticks. Bosses remain alive for that expiry check.
Fixture is airborne and cleared to avoid unrelated terrain fire/suffocation.

No Inferno gameplay change was needed. Classification regression observed
UNCLASSIFIED (`boss-inferno-classification-red.xml` and
`powers-boss-inferno-classification-red-20261013.log`), then only the two proven
routes were classified EFFECTIVE. No Light artifact Inferno route was invented.
Full JUnit: **2,001 tests / 474 suites, 2,000 pass, one failure, zero errors/skips**
(64s). Sole failure remains `everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss`.
Raw XML/log: `boss-inferno-junit-20261013-xOLX3v/`; sorted XML filename/content-digest
inventory SHA-256 `db8851b7a7637b34e3c64f5a05236d4d2eb34942a7462577d623076fb1937118`.
Audit/diff checks pass. The overall acceptance gate remains open.

## Locator acceptance update — 2026-10-13

Six fresh-server cases pass in `boss-locator-transaction-pjrtfunt/`: Soul Compass
and Cartographer's Star against/near each boss (14–15s per case). Tests open the
actual held-book selection, receive its nonce, submit the registered payload,
check exact payment/cooldown and nonce consumption, then observe actual camera,
coordinates, cleanup, or discovered biome journal and clientbound result.
Boss health remains unchanged. Soul Compass requires a unique custom name;
Herald entity-type titles are not custom names. Initial unnamed fixture failure
in `boss-locator-8k3o61_e/` and diagnostic log is not gameplay RED.

The corrected named fixture exposed a real self-blocking cast: the locator
acquired SOUL_COMPASS ownership before the common casting gate, which rejected
that same ownership. RED retained in
`boss-locator-named-ytu854lj/004-boss_locator_game_tests_soul_compass_observes_dark_herald.log`.
Production now preflights participants, then acquires ownership within the
validated spell transaction. Failed transactions still release ownership.
`powers-boss-locator-rollback-green-20261013.log` proves admission refusal
refunds reserved energy/cooldown, and insufficient energy never reaches admission
(one required test, 15s). Earlier rollback attempt failed compilation on an
unsuppressed deprecated test helper, not gameplay; its raw log is retained.

Classification regression observed UNCLASSIFIED, retained in
`boss-locator-classification-red.xml` and its raw log. Only Soul Compass
(EFFECTIVE, subject to named-target eligibility) and Cartographer's Star
(SETUP_EFFECTIVE) are newly classified.

Full JUnit rerun: **1,999 tests / 474 suites, 1,998 pass, one failure, zero
errors/skips** (66s). The sole failure remains
`everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss`. Raw XML/log:
`boss-locator-junit-20261013-WQGnQk/`; sorted XML filename/content-digest inventory
SHA-256 `f1de80fd2c0631fd631452e70d35104ab0db3af1171f8a6c04c82cc98bcada0d`.
Audit and whitespace checks pass. No complete gate or release acceptance is claimed.

The actual competing-session regression also passes (16s), raw
`powers-boss-locator-conflict-20261013.log`: an existing boss remote view keeps
its exact ownership and camera, a second caster receives nested-session refusal,
no energy/cooldown is consumed, no nonce or new ownership remains, and the
original view subsequently cleans up normally.

### Heavenfall full cast — 2026-10-13

`BossHeavenfallGameTests` passes its three-boss scenario (one required test, 17s),
raw `powers-boss-heavenfall-clear-20261013.log`. A real held Celestial grimoire
reserves 100 energy and 72,000-tick cooldown, remains channeling at tick 190,
starts its persistent ritual after the 200-tick channel, leaves all three
37-health unshielded bosses untouched at tick 1,350, and kills them after the
1,200-tick warning. The observer changes to Creative only after channel
completion; bosses remain damageable. No production damage shortcut is invoked.

Initial test and diagnostic failed on First Vessel template-wall suffocation,
not early Heavenfall damage: actual damage source was `inWall`. Bounded fixture
space was cleared; production ritual code was unchanged. Both failed logs are
retained (`powers-boss-heavenfall-live-20261013.log` and
`powers-boss-heavenfall-diagnostic-20261013.log`). Classification RED observed
UNCLASSIFIED; raw XML/log retained as `boss-heavenfall-classification-red.xml`
and `powers-boss-heavenfall-classification-red-20261013.log`. Only this proven
spell was classified EFFECTIVE. All twelve registered spell routes now have
focused live evidence; this does not establish exhaustive crystal/artifact/innate
coverage or any final release gate.

Full JUnit: **2,000 tests / 474 suites, 1,999 pass, one failure, zero errors/skips**
(64s). Sole failure: `everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss`.
Raw XML/log: `boss-heavenfall-junit-20261013-giUG0R/`; sorted XML filename/content
digest inventory SHA-256
`ab8de6ed3a39788c02728d51efd945b903f2c5ec18bb8236d3a7941fe910d58f`.
Audit/diff checks pass. All runners terminal; protected main and exploratory
candidate were reverified clean at their unchanged commits.

Not accepted, integrated, or ready for release. Enumeration is no substitute for
execution. The matrix now inventories all innate, spell, crystal, and aligned
artifact routes; new cases remain `UNCLASSIFIED` until their behavior is proved.
The all-classified assertion remains active and the complete gate is still open.

## First live batches, 2026-10-13

| Route | Bosses independently exercised | Observed response |
| --- | --- | --- |
| Innate Telekinesis | Light Herald, Dark Herald, First Vessel | Exactly 30% of the ordinary target's impulse, plus an actual clientbound resistance actionbar packet |
| Darkness artifact Telekinesis | Light Herald, Dark Herald, First Vessel | Authorized held-artifact selection/activation; exactly 30% impulse, clientbound resistance feedback, energy spent and cooldown started |
| Abyssal grimoire Dispel | Light Herald, Dark Herald, First Vessel | Real held-book selection and 20-tick channel; boss Strength removed, untargeted bystander Strength retained; 18 energy and 500-tick cooldown reserved |
| Double Health: innate, Darkness artifact, Light artifact | Each route independently against each boss (nine cases in three batches) | At least doubled caster capacity and bounded heal; opposing boss unchanged; payment consumed; toggle-off restores original capacity and health ratio without refund or extra payment |
| Green Crystal Life Bloom | Light Herald, Dark Herald, First Vessel | Caster heals to full, poison removed, regeneration applied; opposing boss remains injured and without regeneration; energy spent and cooldown started |
| Darkness and Light artifact Life Bloom | Each alignment independently against all three bosses (six cases) | Real held-artifact selection and activation; caster healed/cleansed, boss not healed, payment and rank-nine cooldown enforced. Opposing Herald receives 100-tick reaction, same-alignment Herald none, First Vessel 160 ticks |

`BossControlResponseGameTests` invokes `AbilityActivationService`, reads actual
entity velocities, and captures the player's outgoing connection. These tests
passed before gameplay changes: protection already emits resistance feedback.
The initial theory that Telekinesis lacked feedback was disproved. No gameplay
change was made. The classification regression instead failed because the matrix
said `EFFECTIVE` rather than `RESISTED_WITH_FEEDBACK`.

`BossSupportResponseGameTests` invokes `CrystalPowerRegistry.tryActivate` with
the actual Green Crystal item. Its three live tests passed; the classification
regression failed because Life Bloom remained unclassified. Its proven matrix
response is now `SETUP_EFFECTIVE`.

Three focused unit tests pass: exact every-family membership per boss,
Telekinesis classification, and Life Bloom classification. These do **not** replace
the intentionally still-failing complete classification gate or the remaining
live casts. Clientbound packet capture does not claim visual-client acceptance.

Raw logs retained under the common Git directory's
`codex-backups/stages-candidate-rjj18l/`:

- `powers-boss-telekinesis-feedback-red-20261013.log` — despite its initial filename,
  this is three passing characterization tests, not RED evidence.
- `powers-boss-telekinesis-classification-red-20261013.log` — observed wrong classification.
- `powers-boss-life-bloom-proof-20261013.log` — three live passes, then expected unit classification failure.
- `powers-boss-matrix-first-batches-green-20261013.log` — three focused unit passes.

Outstanding: classify and execute every remaining route, include actual artifact
alignment/selection/payment paths, prove intentional immunity or resistance with
feedback where applicable, update generated acceptance reporting, and run complete
candidate and final release gates. Previous innate labels are not newly validated
by this report.

The six artifact cases passed through `ArtifactWeaponManager.select` and
`activateSelected`, with actual alignment authorization and held weapons. Their
classification test first failed on `UNCLASSIFIED`, then passed after registering
only these two proven routes as `SETUP_EFFECTIVE`. Evidence alongside the logs
above: `powers-artifact-life-bloom-live-20261013.log`,
`powers-artifact-life-bloom-classification-red-20261013.log`,
`powers-artifact-life-bloom-classification-green-20261013.log`, and
`artifact-life-bloom-classification-green.xml`. These are focused results, not
complete artifact catalogue acceptance.

The Darkness artifact Telekinesis route also passed all three bosses; the same
run repeated the three innate cases with payment/cooldown assertions, for six
required live tests passing in 38s. Its classification test first observed
`UNCLASSIFIED` instead of `RESISTED_WITH_FEEDBACK`. No Light artifact route was
invented: that catalogue does not offer Telekinesis. Retained evidence:
`powers-artifact-and-innate-telekinesis-live-20261013.log`,
`powers-artifact-telekinesis-classification-red-20261013.log`, and
`artifact-telekinesis-classification-red.xml`.

Latest complete matrix unit class: six tests, five pass, exactly one failure
(`everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss`). Both artifact
classification regressions now pass; the exhaustive classification gate still
correctly rejects the remaining unclassified routes. Raw log and XML:
`powers-boss-matrix-artifact-batches-open-gate-20261013.log` and
`boss-matrix-artifact-batches-open-gate.xml`. This build is **not GREEN**.

Dispel's three live tests passed (35s); its classification regression first
failed on `UNCLASSIFIED`, then passed in the seven-test matrix class (six pass,
only the exhaustive gate fails). Evidence: `powers-boss-dispel-live-20261013.log`,
`powers-boss-dispel-classification-red-20261013.log`,
`boss-dispel-classification-red.xml`, `powers-boss-matrix-dispel-open-gate-20261013.log`,
and `boss-matrix-dispel-open-gate.xml`.

Double Health's nine route/boss cases passed in three required GameTests (39s).
The two artifact classification rows first failed on `UNCLASSIFIED`; they are
now explicitly classified, rather than inferred from every routed innate.
Evidence: `powers-boss-double-health-live-20261013.log`,
`powers-boss-double-health-classification-red-20261013.log`, and
`boss-double-health-classification-red.xml`. All are focused checks of existing
gameplay; no production ability behavior changed.

Latest support batch: all 12 required GameTests passed (21s), including the nine
Double Health combinations. Life Bloom artifact cooldown is checked against the
independently derived literal 2,400 ticks at rank nine, not against the same
production helper that sets it.

Full JUnit rerun: **1,991 tests / 474 suites, one failure, zero errors/skips**.
The sole failure is `everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss`;
the remaining 1,990 pass, including both new classification regressions. This is
still a **failed build**, not full acceptance. All raw XML plus logs retained in
`codex-backups/stages-candidate-rjj18l/boss-junit-20261013-z8YrGe/`.
Sorted `filename + space + file-SHA256 + newline` inventory SHA-256:
`b0040616cad4723f9ed6888d4f937347c5164c77499e6c934eb4ff4ab43498a4`.
The source audit was regenerated and its check passed; exhaustive live coverage,
candidate integration, and release verification remain open.

### Possession control regression (2026-10-13)

Actual innate possession reported boss resistance but retained its full lease.
The new live duration regression failed at tick 65 for all three bosses before
the fix. The shared session now applies the existing 30% resistance rule after
clamping the requested lease. Four isolated fresh-server tests pass: each boss
is still controlled at tick 50 and released by tick 65, with resistance feedback,
body cleanup, and original AI state preserved; an ordinary test actor stays
controlled at tick 190 and releases by tick 205. Evidence directory:
`codex-backups/stages-candidate-rjj18l/boss-possession-isolated-itvxrgde/`.

Combined-server reruns after the fix passed Light Herald but failed Dark Herald
and First Vessel at tick 65. Diagnostic output confirms elapsed=65, current
owner=true, body=true. A subsequent temporary production diagnostic proved
nearby mind-resonance amplification: the three requested durations were 200,
242, and 293 ticks, with adjustment duration multipliers 1, 1.21, and 1.4641.
Their resisted leases are therefore 60, 73, and 88, not uniformly 60. The
neutral-duration oracle must use the standard fresh-process runner. Diagnostic
logging was removed; `powers-boss-possession-adjustment-diagnostic-20261013.log`
retains the causal evidence. Other retained logs include
`powers-boss-possession-duration-red-20261013.log`, the unsuccessful
`powers-boss-possession-duration-green-20261013.log` (filename is not a GREEN
claim), `powers-boss-possession-duration-relative-20261013.log`,
`powers-boss-possession-dark-diagnostic-20261013.log`, and
`powers-boss-possession-batch-diagnostic-20261013.log`.

A second live regression proved full-control Dreamwalking accepted an explicitly
control-immune boss. Its admission now combines Dreamwalking permission with
the control-resistance policy; the separate view-only camera path is unchanged.
The regression then passed six admission cases (immune/reflected × three bosses)
in one required GameTest, checking rejection and no body/camera/AI mutation.
Evidence: `powers-boss-dreamwalk-admission-red-20261013.log` and
`powers-boss-dreamwalk-admission-green-20261013.log` (72s).
Duration/feedback for actual Dreamwalking activation, aligned artifact possession,
view-only camera regression, and whole-suite validation remain outstanding.

The innate possession classification regression independently failed on
`SETUP_EFFECTIVE` versus `RESISTED_WITH_FEEDBACK` before correcting that one
proven route. Raw log/XML: `powers-boss-possession-classification-red-20261013.log`
and `boss-possession-classification-red.xml`. Unlike preceding batches, this
batch changes production control behavior and is not yet integrated or accepted.

Latest complete matrix class: nine tests, eight pass, sole failure remains the
required exhaustive all-classified gate. Raw log/XML:
`powers-boss-matrix-possession-open-gate-20261013.log` and
`boss-matrix-possession-open-gate.xml`. This is not a GREEN build.

Post-fix full JUnit rerun: **1,992 tests / 474 suites, 1,991 pass, one failure,
zero errors/skips** (92s). Only the exhaustive boss-action classification fails.
All XML and the raw log are retained in
`codex-backups/stages-candidate-rjj18l/boss-control-junit-20261013-NvMtmS/`.
Sorted XML filename/content-digest inventory SHA-256:
`d350db60cc2e2291ac53a6b4658ed3911674cb1579ed42161cdbc9b950cff72f`.
Source audit regenerated and checked; no full-gate, integration, or release claim.

### Routed control follow-up (2026-10-13)

Nine real activation cases now pass independently: Blue Crystal Dreamwalking,
Darkness artifact Dreamwalking, and Darkness artifact Vessel Possession, each
against all three bosses. The shared fixture verifies actual selection and
activation, energy spent, cooldown started, camera ownership until near expiry,
restored camera/body ownership after expiry, original host AI state, and the
clientbound resistance actionbar. Neutral expected leases are 180 ticks for
Dreamwalking and 60 for possession. No Light artifact routes were invented.

`boss-control-all-routes-idtn2dy7/` retains six successful artifact cases; its
seventh case failed during Blue Crystal fixture selection, before Dreamwalking
activation. Setting only the mock's crouch input did not set its crouching pose.
Adding the actual crouching/standing poses fixes that fixture, not gameplay.
`boss-blue-and-view-l3xd0c_z/` then passed all three Blue Crystal cases and one
three-boss view-only regression. The latter proves control-immune bosses remain
observable for the full 180-tick camera lease, cannot be moved through the host
control channel, and release their camera/body session afterward.

Classification regression observed `UNCLASSIFIED` before adding only the three
verified routes. Retained evidence: `boss-routed-control-classification-red.xml`
and `powers-boss-routed-control-classification-red-20261013.log`.
These directories and files are under the same common Git evidence root above.
The interrupted 20-test collection is not a complete-class pass; remaining
pre-existing control tests still need their rerun after fixture refactoring.

Full JUnit after those classification changes: **1,993 tests / 474 suites,
1,992 pass, one failure, zero errors/skips** (80s). The sole failure remains
`everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss`; no gate weakened.
All raw XML/log retained in `boss-routed-control-junit-20261013-kz6kD7/`.
Sorted XML filename/content-digest inventory SHA-256:
`41f422a1c2e2ddc398bdde0ceb512632f4854634dbe67d3961b9605385424c5f`.
The remaining eleven control regressions are running separately; no complete
control-class or full release GREEN is claimed yet.

The eleven remaining control regressions subsequently all passed in
`boss-control-regression-tail-1u80bk5y/` (one fresh server per test). This covers
the refactored innate possession/baseline fixture, the six Telekinesis cases,
and immune/reflected Dreamwalking admission. Together with the routed-control
and camera batches above, every current control test has focused passing
evidence; this is not an exact-final full-suite acceptance receipt.

Blood Reading then passed three live boss cases (19s). Actual held Blight
grimoire selection/channeling reserves 12 energy and a 200-tick cooldown.
Outgoing vitals packets report the boss fixture's 37 health and actual maximum;
effect packets include its Strength effect. Channel completion does not change
boss health/effects or roll back the cooldown. The classification regression
first observed `UNCLASSIFIED`, then only `spell/blood_reading` was classified
effective. Evidence retained: `powers-boss-blood-reading-live-20261013.log`,
`powers-boss-blood-reading-classification-red-20261013.log`, and
`boss-blood-reading-classification-red.xml`. No gameplay behavior changed in
this follow-up; exhaustive matrix and release gates remain open.

Full JUnit afterward: **1,994 tests / 474 suites, 1,993 pass, one failure,
zero errors/skips** (76s), again only the exhaustive all-classified assertion.
All XML/log retained in `boss-blood-reading-junit-20261013-yuab0P/`; sorted XML
filename/content-digest inventory SHA-256:
`9b376d16ff308cd2a255a27d1060240a69e209220fe843ed0024d9b6d5c3fe8d`.

### Wild support and First Vessel ward accounting (2026-10-13)

Purification Circle passed three isolated real-grimoire boss cases in
`boss-purification-xeig2msp/`: 20 energy, 600-tick cooldown, completed channel,
caster health 5→9 and Slowness removed, opposing boss stays at 37 health with
Slowness retained. Hearth Sanctuary deliberately follows its different documented
rule: every living entity within three blocks, enemies included, gets an
independent 40-integrity ward. The caster and boss are warded; an entity four
blocks away is not.

The first Sanctuary batch failed only First Vessel. Diagnostic repeat showed
the boss stayed at 37 health but its ward retained 4 integrity after a 50,000-hit.
First Vessel clamped the hit to 36 before the shared damage hook, incorrectly
charging its ward that reduced health-adapter amount. The fix scopes the original
incoming amount around the vanilla health call with `finally` restoration; the
shared hook uses it for incoming/ward accounting only after the existing
protection checks. Body proxy behavior, time-stop/protection ordering, and the
boss's vanilla-health conversion remain intact.

All three Sanctuary cases then passed (16s), and the strengthened rerun passed
(17s): a 50,000-point hit is fully blocked, spends exactly 40 shield integrity,
leaves the caster's separate ward untouched, and a subsequent one-point hit
spends only one point of a newly raised ward. Retained logs:
`powers-boss-sanctuary-live-20261013.log` (failed),
`powers-boss-sanctuary-vessel-diagnostic-20261013.log` (failed),
`powers-boss-sanctuary-ward-fix-green-20261013.log`, and
`powers-boss-sanctuary-ledger-green-20261013.log`.

The Wild-support classification test first failed on UNCLASSIFIED. Only the two
proven spell routes were added as SETUP_EFFECTIVE. RED XML/log retained as
`boss-wild-support-classification-red.xml` and
`powers-boss-wild-support-classification-red-20261013.log`. This is a production
damage repair requiring affected-suite and final integration verification; no
MOB-014 or release completion is claimed.

Full JUnit after this fix: **1,995 tests / 474 suites, 1,993 pass, two failures,
zero errors/skips** (70s). Failures were the exhaustive all-classified assertion
and `SourceQualityTest.responsibilityClassesStayWithinTheReviewedSizeBoundary`
(FirstVessel grew to 462 lines). Raw XML/log are retained in
`boss-wild-support-junit-20261013-xobaZA/`; sorted XML filename/content-digest
inventory SHA-256:
`bf70fe7cad5fc45a9d0bf5385b2a21471628675700c354dbffabc6c8c5f9c797`.

Extracted the unchanged bounded world-observation logic into
`FirstVesselEncounterSnapshot`, preserving thresholds, candidate caps, seed,
and planner inputs. The existing size limit was not changed. All SourceQuality
tests then passed (34s), raw log `powers-boss-size-extraction-20261013.log`.
Affected live damage regressions remain a separate required check.

All nine affected GameTests subsequently passed in fresh servers, retaining logs
in `boss-ward-regressions-6ihg1x82/`: Sanctuary against each boss, Celestial Ruin
against First Vessel layered vitality, persistent First Vessel spawn, physical
body-tether protection, sacrificial overkill shields, independent Sanctuary
wards, and ward→Soul Link→health exactly-once ordering. These passes cover the
current extraction and ward fix, not the remaining exhaustive action matrix.

Post-extraction full JUnit: **1,995 tests / 474 suites, 1,994 pass, one failure,
zero errors/skips** (64s). The sole failure is
`BossActionResponseMatrixTest.everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss`.
All XML/log retained in `boss-ward-post-extraction-junit-20261013-Cip0rK/`;
sorted XML filename/content-digest inventory SHA-256:
`e07bab0b2e9c7cbe07668a94ca38c22b6b503e33726fdf835123391ee9e16e33`.
Source audit and whitespace checks pass. Remaining unclassified routes keep
MOB-014 and the overall plan open.

### Verdant Tending (2026-10-13)

Three real held-Wild-grimoire cases passed against Light Herald, Dark Herald,
and First Vessel (16s), with 22 energy, 600-tick cooldown, and channel completion.
Dry farmland with a crop above and no irrigation becomes moisture 7; the crop
grows. The nearby boss remains at 37 health with its Strength effect unchanged.
Raw live log: `powers-boss-verdant-live-20261013.log`.

The expanded Wild-support classification regression failed specifically on
`spell/verdant_tending` (expected SETUP_EFFECTIVE, actual UNCLASSIFIED), retained
in `boss-verdant-classification-red.xml` and
`powers-boss-verdant-classification-red-20261013.log`. Only that observed route
was added; no gameplay behavior or exhaustive gate was weakened.

Full JUnit afterward: **1,995 tests / 474 suites, 1,994 pass, one failure,
zero errors/skips** (67s). Wild-support classification now passes; the sole
failure remains the exhaustive all-classified assertion. Raw XML/log:
`boss-verdant-junit-20261013-7Qf9mi/`; sorted XML filename/content-digest
inventory SHA-256 `545eba317e1ce21de29c20c9cf926cc7bfeb92e1936ada8aeda9c81e2ca781e5`.

### Augury and Grave Recall (2026-10-13)

Six real held-grimoire cases passed (16s), one per spell/boss pair, with actual
clientbound reports captured. Augury spends 16 energy with a 600-tick cooldown,
completes its channel, reports current server weather, correctly reports no
known realm event in the test dimension, and includes loaded-only uncertainty.
Grave Recall spends 10 energy with a 200-tick cooldown and reports recorded
death coordinates/dimension rather than the caster's moved position or boss
position. All bosses retain 37 health and Strength; neither route leaks a
channel or rolls back a successful cooldown. Raw log:
`powers-boss-information-live-20261013.log`.

The new classification regression failed with Augury UNCLASSIFIED; raw evidence
is `boss-information-classification-red.xml` and
`powers-boss-information-classification-red-20261013.log`. Both proven utility
routes are now SETUP_EFFECTIVE, not direct boss attacks. No gameplay change was
needed; the exhaustive unclassified-route gate remains in place.

Full JUnit: **1,996 tests / 474 suites, 1,995 pass, one failure, zero errors/skips**
(64s). The information-spell classification passes; only the exhaustive
all-classified assertion fails. XML/log retained in
`boss-information-junit-20261013-n95Erq/`; sorted XML filename/content-digest
inventory SHA-256 `f68cadd6ec2584008fde30fba004b0cca3784f6e2a8c8f3b92445f4c314b32c4`.
Source audit and whitespace checks pass; MOB-014 remains open.

### Dimensional Anchor's target boundary (2026-10-13)

The Deep spell requires a player-compatible target; neither Herald nor First
Vessel implements that contract. Existing behavior is preserved and explicitly
classified DELIBERATELY_IMMUNE, an allowed MOB-014 outcome, rather than claiming
that merely storing an anchor would enforce boss travel. README states the
exclusion. Expanding it would require new boss state and travel policy, not a
metadata-only claim.

Three actual held-Deep-grimoire boss casts pass (16s): the visible target starts
the channel and reserves 22 energy, then failure restores energy and the prior
cooldown, leaves no anchor or health damage, and emits actual clientbound failure
feedback. A strengthened rerun adds a compatible-actor positive control through
the same spell route: all four cases pass (14s), with the actor retaining its
local-dimension anchor, committed 1,200-tick cooldown, and success feedback.
Raw logs: `powers-boss-anchor-refusal-live-20261013.log` and
`powers-boss-anchor-control-green-20261013.log`.

Classification RED (expected DELIBERATELY_IMMUNE, actual UNCLASSIFIED) is retained
in `boss-anchor-classification-red.xml` and
`powers-boss-anchor-classification-red-20261013.log`. No gameplay policy changed.

Full JUnit: **1,997 tests / 474 suites, 1,996 pass, one failure, zero errors/skips**
(64s). Anchor classification passes; the exhaustive all-classified assertion
is the sole failure. Raw XML/log: `boss-anchor-junit-20261013-V8R4jq/`;
sorted XML filename/content-digest inventory SHA-256
`166d4d2bbae96acfa6fa8b63dc350cc8d8275c9fc78be51afc84d9e7bc1888c2`.
Audit and whitespace checks pass; remaining action coverage and final gates are open.

### Ward Breaking (2026-10-13)

Three isolated real Abyssal-grimoire casts pass near the bosses, retained in
`boss-ward-ritual-clear-yxqcvxpa/` (Dark Herald 16s, First Vessel 15s, Light Herald
15s). The caster stands 24 blocks from the powered ward, outside its 20-block
field but inside the spell's 32-block targeting range. Each case proves the boss
begins inside the active field, a cast reserves 26 energy and a 1,200-tick
cooldown, channel completion suppresses the field without breaking the powered
block or damaging the boss, suppression remains near the end of its 900-tick
lease, and the field returns after expiry.

Initial fixture attempt `boss-ward-ritual-kqxduu7w/` and diagnostic
`powers-boss-ward-target-diagnostic-20261013.log` failed before channel admission:
the default test template wall intercepted the ray 3.5 blocks from the caster.
This was not a gameplay RED. Clearing the bounded test sightline and boss space
fixed the fixture; production targeting/suppression code is unchanged.

Classification regression then failed as expected (UNCLASSIFIED versus
SETUP_EFFECTIVE), retained as `boss-ward-ritual-classification-red.xml` and
`powers-boss-ward-ritual-classification-red-20261013.log`. Only this proven
environmental-support route was classified; exhaustive coverage remains open.

Full JUnit: **1,998 tests / 474 suites, 1,997 pass, one failure, zero errors/skips**
(65s). Ward classification passes; the only failure is the exhaustive
all-classified assertion. Raw XML/log: `boss-ward-ritual-junit-20261013-BimkuE/`;
sorted XML filename/content-digest inventory SHA-256
`bc8be0f83ae0d835f4616bdb17a3c8477a13b5d4930b4381e57f3c38b7c94ab0`.

### Daybreak Wave

The selected Heavenly Partisan action spent energy, damaged Dark Herald, and
removed Poison from Light Herald and First Vessel without damaging either.
First Vessel still gained Slowness from the independent artifact reaction.
Earlier Slowness-only assertions conflated that reaction with the wave's
cleanse; the regression now distinguishes them. No gameplay policy changed.
The isolated Java 25 GameTest passed (1 required test, 2m 56s;
`/tmp/powers-daybreak.DC6HMEBS`). This focused result is not release-gate evidence.

### Guardian calls and ground rites

Four isolated Java 25 GameTests passed. Call Radiant and Call Hollowed spent
energy and spawned four correctly aligned, caster-owned guardians near the
three bosses, without arrival damage. Consecrate Ground and Blight Ground
spent energy and converted the fixture floor via queued work without
immediate boss damage. These routes provide setup; sustained guardian
combat and ground effects were not measured by these cases.

- Call Radiant: `/tmp/powers-call_radiant_summons_owned_guardians_near_every_boss.Su6a3ETX` (46s).
- Call Hollowed: `/tmp/powers-call_hollowed_summons_owned_guardians_near_every_boss.Vx93GKiq` (18s).
- Consecrate Ground: `/tmp/powers-consecrate_ground_converts_arena_near_every_boss.M9ctE57c` (14s).
- Blight Ground: `/tmp/powers-blight_ground_converts_arena_near_every_boss.qMHqiJNT` (12s).

### Second Dawn

The selected Heavenly Partisan route spent energy and restored a Survival
server player after one lethal hit attributed to each boss. Each hit applied
absorption and consumed the ward exactly once. Earlier failed assertions used
Minecraft's mock player, which overrides its game mode to Creative even after
the fixture requests Survival. No production ward change was needed. The
connected Survival fixture passed in 13s (`/tmp/powers-second-dawn-real.xHnqLYjr`).

### Covenant Chain and Host of Heaven

Covenant Chain selected and paid for each cast with the Heavenly Partisan,
then the chain tick pulled the individually aimed Dark Herald, Light Herald,
and First Vessel. Host of Heaven spent energy, created two owned elite
sentinels, and established a field covering all three bosses without immediate
damage. These are focused live checks, not the full release gate.

- Covenant Chain: `/tmp/powers-covenant_chain_pulls_each_boss.kJ0mgMlp` (43s).
- Host of Heaven: `/tmp/powers-host_of_heaven_builds_setup_near_every_boss.aaavfOiN` (31s).

### Nightfall Dominion

The selected rank-ten Lycanbane invocation spent energy and buffed the caster.
Its live aura damaged Light Herald and First Vessel effective vitality, with
Dark Herald health unchanged. The focused GameTest passed (19s,
`/tmp/powers-nightfall_dominion_pressures_non_darkness_bosses.XLEE8N4i`).

### Heaven Gate

The paid Heavenly Partisan cast moved its owner at least four blocks and
carried all three nearby bosses, preserving each original offset. The
initial fixture incorrectly expected owner-only transport; only the owner
triggers the portal, while its existing two-block travel cohort follows.
No travel policy changed. The corrected live check passed (22s,
`/tmp/powers-heaven_gate_carries_caster_and_nearby_boss_cohort.nCvYci3b`).

### Solar Firmament

The selected Heavenly Partisan route spent energy and established a field in
the boss arena. A live pulse damaged Dark Herald while healing injured Light
Herald and First Vessel (measured by effective vitality for First Vessel).
The strengthened test passed in 13s (`/tmp/powers-solar-firmament-heal.rc9irqtY`).

### Candidate JUnit gate

Java 25 full JUnit passed: 2,034 tests across 474 suites, with no failures,
errors, or skips. All 51 matrix unit tests passed, including exhaustive
classification and exact route membership. Raw XML and log are retained
under `/tmp/powers-mob014-candidate.vXiTWX0H/`; SHA-256 of the sorted XML
path/content digest stream:
`26c3c8eff58df45f706dfd5c7a181f4179f4aaefe70c079ba283100f903b3476`.
The unfiltered candidate check is separate and has not yet passed.

The unfiltered run has replayed all 28 registered boss-support GameTests
successfully (logs 116–143 under `build/deterministic-gametests/run-k8ncl695/`).
SHA-256 of the sorted path/content digest stream for that subset:
`c31c39ef4c9cdfa6d101d62c31753377deab87d3ae214126b9c0e5c3409b287c`.
This records only the completed boss-support batch, not an overall pass.
