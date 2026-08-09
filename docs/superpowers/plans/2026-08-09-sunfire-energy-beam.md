# Sunfire Energy Beam implementation plan

## Task 1: Pure beam rules

- [ ] Write failing tests for timing boundaries, consecutive scorch, burn/steam scaling, rank caps, flare admission, finite terminal ordering, and lifecycle validity.
- [ ] Implement `EnergyBeamRules` and run its focused suite.
- [ ] Commit the rules slice.

## Task 2: Server channel and collisions

- [ ] Replace the instant activation with snapshot-scaled channel state and authoritative live aim.
- [ ] Add nearest block/ward/water/entity resolution, body counters, shield integrity impact, steam transformation, flare, mastery splits, and bounded target selection.
- [ ] Wire respawn/disconnect/stop/tick cleanup and compile both source sets.

## Task 3: Presentation and documentation

- [ ] Add dedicated focus, firing, endpoint, steam, counter, split, flare, interruption, and completion choreography outside the shared FX responsibility class.
- [ ] Add randomized lore, update description/README/changelog, and document bespoke Energy Beam interactions.
- [ ] Regenerate Java and non-item asset audits.

## Task 4: Release verification

- [ ] Run focused and complete tests, audit checks, `git diff --check`, and `clean check build` with Java 25.
- [ ] Start an isolated dedicated server, reach `Done`, stop normally, and verify all six dimensions save.
- [ ] Review, commit to `main`, and leave the worktree clean.
