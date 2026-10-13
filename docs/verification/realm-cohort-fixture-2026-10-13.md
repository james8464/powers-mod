# Realm cohort fixture repair

The exploratory tail on exact candidate `32315e5e` passed 31 cases, then failed
`realmCrystalCarriesNearbyMobAndPersistentShadowThenReturnsThem` at tick 100.
This is not a full-gate receipt.

Normal-rate test pacing did not change the failure. Temporary diagnostics showed
that preparation succeeded, but commit could no longer resolve the captured
participants. The zombie passenger was `KILLED` with zero health; Shadow remained
alive at 100 health. Captured feedback reported an interrupted journey, not a
load timeout. Shadow's combat controller selects nearby hostile monsters, so
the fixture conflicted with the now-active companion combat behavior.

The fixture now uses an ordinary passive pig. Shadow combat remains active;
no invulnerability, production travel change, or relaxed travel assertion was
introduced. The same caster/body-session, passenger tracking, Shadow dimension,
and return-to-origin assertions pass. Temporary production diagnostics and the
unhelpful clock experiment were removed. Failure output retains passenger and
Shadow identity/removal/health information.

One focused required GameTest passed; build successful in 36s. Further broad
candidate and exact-release gates remain outstanding.

Raw logs are retained in the common Git directory under
`codex-backups/stages-candidate-rjj18l/`:

- `tail-32315e5e-4rfmpbnq/032-powers_game_tests_realm_crystal_carries_nearby_mob_and_persistent_shadow_then_returns_them.log`
- `powers-realm-cohort-normal-clock-20261013.log`
- `powers-realm-cohort-feedback-20261013.log`
- `powers-realm-cohort-participant-diagnostics-20261013.log`
- `powers-realm-cohort-entity-diagnostics-20261013.log`
- `powers-realm-cohort-passive-passenger-20261013.log`
