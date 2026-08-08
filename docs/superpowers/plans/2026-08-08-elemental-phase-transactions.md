# Elemental Phase Transactions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Elemental Blast's transaction, collision residue, ceremony, and sound match its actual phase while safely normalizing persisted phase data.

**Architecture:** A pure phase enum maps cyclic state to existing canonical actions. A default polymorphic ability method exposes server-derived semantic identity, and every innate packet preparation path consumes it.

**Tech Stack:** Java 25, Minecraft 26.2 Mojang mappings, Fabric Loader/API, JUnit 6, Gradle Loom.

## Global Constraints

- Preserve Elemental Blast energy cost, activation order, phase-on-success rule, and phase-specific cooldown.
- Reuse existing canonical fireball, frost nova, lightning strike, and ground slam actions.
- Never accept a client-selected action ID.

---

### Task 1: Typed elemental cycle

**Files:**
- Create: `src/main/java/com/powers/power/abilities/ElementalPhase.java`
- Create: `src/test/java/com/powers/power/abilities/ElementalPhaseTest.java`
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`

- [ ] Write failing literal mapping, catalogue-presence, cycle, and malformed-index tests.
- [ ] Implement stable phases with floor-modulo lookup/navigation.
- [ ] Normalize attachment reads and writes through the enum.
- [ ] Run focused/full tests, regenerate Java audit, and commit.

### Task 2: Dynamic authoritative action identity

**Files:**
- Modify: `src/main/java/com/powers/power/Ability.java`
- Modify: `src/main/java/com/powers/power/abilities/ElementalBlastAbility.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Regenerate: `docs/quality/code-audit.md`

- [ ] Add `Ability.magicActionId(ServerPlayer, PlayerPowersData)` with a safe default.
- [ ] Override it in Elemental Blast and replace raw array/index/colour handling with `ElementalPhase`.
- [ ] Use the resolved ID in all four innate preparation paths before any energy spend.
- [ ] Run the Java audit, `clean check build`, isolated server smoke test, commit, and leave `main` clean.
