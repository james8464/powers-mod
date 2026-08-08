# Elemental Phase HUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the authoritative primed Elemental Blast phase in its existing HUD medallion.

**Architecture:** The normal state payload carries one normalized integer. Client state mirrors it; pure HUD math derives ARGB rune colours; the renderer adds four bounded procedural marks and a localized phase label.

**Tech Stack:** Java 25, Minecraft 26.2, Fabric, JUnit 6.

### Task 1: Tested HUD phase language

**Files:**
- Modify: `src/main/java/com/powers/hud/HudMath.java`
- Modify: `src/test/java/com/powers/hud/HudMathTest.java`

- [ ] Write failing literal active/inactive/pulse/malformed phase colour tests.
- [ ] Implement `HudMath.elementalRuneColor(int, int, int)` through `ElementalPhase`.
- [ ] Run focused/full tests and commit.

### Task 2: Synchronization and rendering

**Files:**
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `src/client/java/com/powers/client/ClientPowerState.java`
- Modify: `src/client/java/com/powers/client/PowerHudRenderer.java`
- Modify: `src/main/resources/assets/powers/lang/en_us.json`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Regenerate: `docs/quality/code-audit.md`

- [ ] Add the normalized phase to the payload/codec/builder and client update/reset/accessor.
- [ ] Render localized phase label, phase accent, and four rune pips only for Elemental Blast.
- [ ] Run Java/resource audits, `clean check build`, an isolated server smoke test, commit, and leave `main` clean.
