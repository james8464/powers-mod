# Deterministic GameTest Lane Design

## Problem

The Fabric server GameTest runner keeps one server process alive for a large batch
of tests. POWERS tests create players, worlds, schedulers and force managers that
are intentionally production-real. Those owners can leak across unrelated tests,
so a green or red result depends on batch ordering instead of the test's input.

## Decision

Add a repository-owned Python orchestrator and Gradle verification task.
The orchestrator discovers the registered server GameTest identifiers from the
GameTest Java sources, then launches the existing `runGameTest` task once per
identifier with `-PgameTestFilter=<id>`. `runGameTest` already deletes its owned
world before each launch, therefore every selected test receives a fresh server
process and fresh game world without replacing Fabric, altering production code,
or weakening assertions.

## Contract

- `runDeterministicGameTests` is the authoritative full server GameTest gate.
- Discovery rejects duplicate identifiers and zero discovered tests.
- Every child uses Java/Gradle from the caller environment and exits immediately
  on its first failure, retaining the child's unmodified stdout in a unique log.
- The orchestrator prints a sorted, complete passed-test inventory only after all
  child processes exit zero.
- `check` depends on this task, so a normal full gate cannot silently use the
  shared-state runner.
- The existing `runGameTest` task remains available for targeted developer runs.

## Scope

This changes verification infrastructure only. It does not alter gameplay,
GameTest assertions, registered production paths, test counts, or protected main.
