# Deterministic GameTest Lane Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the complete server GameTest gate independent of shared runner state.

**Architecture:** A Python orchestrator discovers registered server GameTest IDs and invokes the existing filtered Fabric task in a fresh process for each ID. Gradle exposes it as a verification task and makes `check` depend on it.

**Tech Stack:** Python 3 standard library, Gradle Groovy DSL, Fabric GameTest.

**Spec:** `docs/superpowers/designs/2026-10-04-deterministic-gametest-lane.md`

## Global Constraints

- Use Java 25 supplied by the caller environment.
- Preserve existing `runGameTest` semantics for targeted runs.
- Retain raw child output and fail fast without synthetic pass claims.
- Do not modify gameplay or protected main.

## Review Focus

- Duplicate Java class/method discovery must fail rather than silently drop a test.
- A failed child must stop subsequent launches and preserve its raw log.
- A child must receive exactly one exact Fabric test filter.
- Only server `@GameTest` methods may enter the inventory; client tests must not.
- A zero-test discovery result must be rejected.

---

### Task 1: Deterministic GameTest orchestrator

**Files:**
- Create: `scripts/run_deterministic_gametests.py`
- Test: `scripts/tests/test_run_deterministic_gametests.py`

**Interfaces:**
- Produces: `discover_test_ids(root: Path) -> list[str]` and a CLI that invokes one filtered Gradle process per discovered ID.

- [ ] **Step 1: Write failing discovery and fail-fast tests.**
- [ ] **Step 2: Run the Python test to verify it fails because the module is absent.**
- [ ] **Step 3: Implement source discovery, unique-log child launches, sorted inventory, and fail-fast propagation.**
- [ ] **Step 4: Run the Python test to verify it passes.**
- [ ] **Step 5: Commit the orchestrator and tests.**

### Task 2: Gradle verification integration

**Files:**
- Modify: `build.gradle`
- Test: `scripts/tests/test_run_deterministic_gametests.py`

**Interfaces:**
- Consumes: the orchestrator CLI from Task 1.
- Produces: `runDeterministicGameTests`, a verification task required by `check`.

- [ ] **Step 1: Write a failing source-contract test for the Gradle task and `check` dependency.**
- [ ] **Step 2: Run it to verify it fails.**
- [ ] **Step 3: Add the Gradle Exec task and replace shared `runGameTest` as the full-check dependency.**
- [ ] **Step 4: Run Python tests and one targeted deterministic child to verify the integration.**
- [ ] **Step 5: Commit the Gradle integration.**
