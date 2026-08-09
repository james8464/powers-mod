# Cinderheart Fireball implementation plan

## Task 1: Pure projectile rules

- [x] Write failing tests for tiers, expiry, reflection caps, rank scaling, falloff, trails, targets, and terminal priority.
- [x] Implement `FireballRules` and run the focused suite.

## Task 2: Owned runtime

- [ ] Replace delayed callbacks and mutable-owner damage with bounded server-owned Cinderheart state.
- [ ] Add safe spawn, charge, launch, trail, reflection, impact, steam, protection, terrain, and exact cleanup.
- [ ] Wire lifecycle and tick ownership through `PowerAbilityRuntime` and route the owned projectile mixin through the runtime.

## Task 3: Presentation and evidence

- [ ] Add dedicated opening, charge, hover, launch, wake, reflection, refusal, terminal, steam, impact, interruption, and expiry choreography.
- [ ] Update lore, descriptions, README/changelog, bespoke interactions, and generated audits.

## Task 4: Release verification

- [ ] Run focused/full tests, generated checks, Java 25 clean build, and six-dimension dedicated-server smoke.
- [ ] Review, commit to `main`, and leave the worktree clean.
