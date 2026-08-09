# Chronal Overdrive implementation plan

## Task 1: Pure overdrive rules

- [x] Write failing tests for timing, modifier bounds, water grounding, trail admission, rebound, pressure, projectile curvature, rank caps, and lifecycle.
- [x] Implement `SuperSpeedRules` and run the focused suite.

## Task 2: Owned runtime

- [x] Replace potion-owned speed/jump state with a stable transient movement modifier and server-owned duration.
- [x] Add wake tracking, environmental grounding, collision response, rank mechanics, counterplay, and exact cleanup.
- [x] Wire respawn, disconnect, stop, and tick ownership through `PowerAbilityRuntime`.

## Task 3: Presentation and evidence

- [x] Add dedicated opening, wake, water, collision, rebound, pressure, veil, projectile, interruption, and completion choreography.
- [x] Update lore, descriptions, README/changelog, bespoke interactions, and generated audits.

## Task 4: Release verification

- [x] Run focused/full tests, generated checks, Java 25 clean build, and six-dimension dedicated-server smoke.
- [x] Review, commit to `main`, and leave the worktree clean.
