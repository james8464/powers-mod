# Combat, Spells, and Visuals Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make combat reliable, boss-capable, environmentally consequential, readable, and bounded, with Celestial Ruin matching its catastrophic fiction.

**Architecture:** Pure rule objects calculate damage, radius, destruction, shield consumption, and FX envelopes. Server managers apply bounded world mutations; compact semantic packets let clients expand circles, beams, flashes, dust, and audio locally.

**Tech Stack:** Fabric API networking/events, Minecraft damage/explosion/particle APIs, Java 25, JUnit 6, Fabric GameTest.

## Global Constraints

- All POWERS status effects suppress vanilla particles.
- Lightning is reliable, cooldown-free, and non-destructive.
- Destructive abilities damage terrain at every innate level, subject to policy.
- Visual work stops when budgets are exhausted.

---

### Task 1: Crash fixes and core ability contracts

**Files:** `FireballImpactResolver.java`, lightning, Breezy Bash, Plant Healing, tests/GameTests.

- [ ] Add a block-impact regression test proving a null direct target cannot crash, an artifact-lightning test proving the bolt spawns, and a landing-position Breezy Bash test.
- [ ] Run tests and verify the exact null dereference/spawn/position failures.
- [ ] Null-guard forcefield lookup, route artifact source correctly, and store the launched victim until its landing event.
- [ ] Run focused tests, relevant GameTests, and replay archived crash steps.
- [ ] Commit as `fix: stabilise projectile and impact abilities`.

### Task 2: Forcefield durability and sacrifice

**Files:** `ForcefieldAbility.java`, forcefield manager/combat hook/FX, tests/GameTests.

**Interfaces:** `ForcefieldDamage.absorb(int durability, float incoming)` returns zero applied damage and zero remaining durability when incoming exceeds durability; activation covers players within two blocks and persists until depleted.

- [ ] Add literal boundary tests for exact, under, and overkill hits plus group activation and logout cleanup.
- [ ] Implement persistent shield ownership and a prominent bounded shell/ring packet; ensure the breaking hit is fully cancelled.
- [ ] Run combat and live Celestial-impact shield tests.
- [ ] Commit as `feat: make forcefields sacrificial group wards`.

### Task 3: Authored destruction and crystal combat

**Files:** destructive innate abilities, `WorldDestructionPolicy`, `CrystalAbilityCatalog`, Inferno/orange/yellow/green/blue implementations, tests/GameTests.

- [ ] Add table tests proving each destructive action has a positive level-0 terrain envelope and monotonic level scaling; add crystal roster/scale/inferno assertions.
- [ ] Implement bounded crater/ray/streak queues; keep player Lightning non-destructive; thicken Inferno beams and fire; add 0.1×-10× Yellow controls; retain only Life Bloom for Green; share Time Stop for Blue Chrono Stop.
- [ ] Run interaction, ability, crystal, resource, and live terrain GameTests.
- [ ] Commit as `feat: make high-rank combat world-shaping`.

### Task 4: Spell combat and presentation

**Files:** spell catalogue/executor/effects, `MagicPresentation`, spell FX packets/client handlers, tests/GameTests/docs.

- [ ] Add a literal spell table asserting every spell has a boss-useful damage/control/defence/escape role, valid counterplay, rank independence, and ritual presentation.
- [ ] Replace weak effects with bounded boss-relevant mechanics and emit compact circle/rune/beam semantic events; expand client-side with reduced-motion and distance caps.
- [ ] Audit every `MobEffectInstance` constructor and route through hidden POWERS factories.
- [ ] Run all spell, presentation, interaction, particle-budget, and GameTests.
- [ ] Commit as `feat: elevate grimoires for boss combat`.

### Task 5: Celestial Ruin and force clash

**Files:** Celestial Ruin rules/manager/saved data/tickets/FX/packets, living-force clash, tests/GameTests.

**Interfaces:** warning 1,200 ticks; client aftermath 400 ticks; entity radius 6,000; center-out terrain completion target 400 ticks; distance-falloff damage remains boss-relevant; restart resumes cursors.

- [ ] Add failing literal rule tests, First Vessel center/edge damage tests, persistence/restart tests, unloaded-caster tests, shield-sacrifice tests, and clash explosion tests.
- [ ] Implement progressive pre-impact tickets, spatial loaded-entity shockwave, center-out terrain queues, many capped streak events, 20-second flash/ringing, loud 6,000-block pulses, and true bounded Light/Dark clash explosion.
- [ ] Run focused tests, GameTests, server restart scenario, and performance counters.
- [ ] Commit as `feat: unleash persistent celestial ruin`.

### Task 6: Entity and GUI asset corrections

**Files:** First Vessel/Radiant Sentinel textures/models/renderers/spawn egg, spell/HUD assets, asset audits.

- [ ] Add resource dimension/registration checks and deterministic UV/model contract tests.
- [ ] Correct UV layouts and spawn-egg model; visually inspect rendered assets in client.
- [ ] Regenerate non-item asset manifest and run `validatePowerResources auditNonItemAssets`.
- [ ] Commit as `fix: align magical entity and effect assets`.
