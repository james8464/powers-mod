# Elemental Phase Transactions Design

**Date:** 2026-08-08

**Target:** Fabric, Minecraft Java Edition 26.2, Java 25

**Status:** Approved through the user's standing authorization to make autonomous design decisions

## Problem

Elemental Blast executes Fireball, Frost Nova, Lightning Strike, or Ground Slam, but the packet handler currently prepares every transaction as `elemental_blast`. The actual phase therefore collides, leaves residue, sounds, and renders as the same flame-primary composite action. A corrupt persisted phase outside 0–3 can also index beyond the ability array.

## Approaches

- Recolour only the cast event fixes presentation but leaves counterplay and residue wrong.
- Add four new phase definitions duplicates actions already present in the canonical catalogue and expands every exhaustive matrix.
- **Resolve the existing underlying action before execution — selected:** a polymorphic ability contract returns the server-derived action ID. Elemental Blast maps its current phase to `fireball`, `frost_nova`, `lightning_strike`, or `ground_slam`; ordinary abilities keep their own ID.

## Typed Phase Model

`ElementalPhase` owns stable index, action ID, and RGB identity for flame, frost, storm, and earth. `fromIndex`, `nextIndex`, and `previousIndex` use floor-modulo normalization, making negative or oversized persisted values safe.

`Ability.magicActionId(player, data)` defaults to the ability ID. Elemental Blast overrides it from the current normalized phase. The packet layer calls this method immediately before `ServerMagicCasts.prepare`, so validation, interaction resolution, successful residue, universal ceremony, authored sound, and mastery scaling all describe the attack that actually executes. Energy remains the Elemental Blast cost and its phase-specific cooldown behaviour remains unchanged.

## Verification

Pure tests exhaust phase/action/colour mappings, cyclic navigation, and malformed indices. Catalogue tests prove every returned action exists. Full verification includes Java/resource audits, `clean check build`, and an isolated server startup/shutdown smoke test.
