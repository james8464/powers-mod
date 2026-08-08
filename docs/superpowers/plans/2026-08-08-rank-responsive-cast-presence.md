# Rank-Responsive Cast Presence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make cast ceremony strength visibly reflect authoritative rank depth and ancient mastery.

**Architecture:** A pure overload adds bounded mastery intensity to the existing presentation profile. The successful server commit passes progression depth and variants; packet and client contracts stay unchanged.

**Tech Stack:** Java 25, Minecraft 26.2 Mojang mappings, Fabric Loader/API, JUnit 6, Gradle Loom.

## Global Constraints

- Preserve the 1–5 intensity range and every particle/network bound.
- Read only server-authoritative progression state after successful execution.
- Add no packet fields, assets, entities, or persistent data.

---

### Task 1: Pure mastery scaling

**Files:**
- Modify: `src/main/java/com/powers/magic/fx/MagicCastPresentation.java`
- Modify: `src/test/java/com/powers/magic/fx/MagicCastPresentationTest.java`

- [ ] Write failing threshold, ancient-mastery, clamping, and exhaustive catalogue tests.
- [ ] Run the focused test and confirm the missing overload fails.
- [ ] Implement `forAction(MagicActionDefinition, int, Set<String>)` with 4/8 thresholds and hard clamping.
- [ ] Run focused and complete tests; regenerate Java audit; commit.

### Task 2: Authoritative server wiring and release

**Files:**
- Modify: `src/main/java/com/powers/magic/runtime/ServerMagicCasts.java`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Regenerate: `docs/quality/code-audit.md`

- [ ] Pass `SkillSystem.effectiveLevel(player)` and `PowerScalingService.forPlayer(...).unlockedVariants()` into the profile at the completed-cast boundary.
- [ ] Compile server and client sources and run all unit tests.
- [ ] Document progression-responsive ceremony strength.
- [ ] Run Java audit and `clean check build`, complete an isolated server startup/shutdown smoke test, commit, and leave `main` clean.
