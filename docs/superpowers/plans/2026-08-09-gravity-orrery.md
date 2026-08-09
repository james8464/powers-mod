# Gravity Orrery implementation plan

> Execute with test-driven development and verify the complete server/client release surface before committing.

## Task 1: Pure rules

- [ ] Add failing tests for capture priority, rank caps, deterministic orbit geometry, finite velocity, collapse impulse, and field termination.
- [ ] Implement `GravityDisplacementRules` with immutable decisions and bounded vector math.
- [ ] Run the focused test and commit the rules slice.

## Task 2: Server-owned field

- [ ] Replace instant Levitation with one anchored field per owner.
- [ ] Scan nearest targets on a two-tick cadence, enforce every protection/counter, steer captures, curve mastered projectiles, and release safely.
- [ ] Add forced-movement spell-field lookup and lifecycle cleanup in `PowersMod`.
- [ ] Compile production and client source sets, run focused/full unit tests, and commit the runtime slice.

## Task 3: Ancient-magic presentation

- [ ] Add opening, sustain, tether, resistance, projectile-curve, and collapse choreography to `PowerFx`.
- [ ] Add localized cast lore and update player-facing documentation/changelog.
- [ ] Regenerate source and non-item asset audits.

## Task 4: Release verification

- [ ] Run focused tests, full `clean check build`, both audit checks, and `git diff --check`.
- [ ] Start an isolated dedicated server, reach `Done`, stop cleanly, and verify every custom dimension saves.
- [ ] Review the complete diff, commit, and leave `main` clean.
