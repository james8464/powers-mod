# INT-009 Authoritative Mind Sessions — Task Report

Date: 2026-09-08

Branch: `int-009-mind-sessions`

Required base: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`

## Outcome

`MindSessionManager` is the sole participant-admission owner for realm travel, Astral projection,
teleport marking, possession, controlled Dreamwalking, Soul Compass, Soul Link, and Shadow when it
participates in those effects. Admission is atomic and bounded to 16 unique identities, requires
exactly one declared owner role, and reports the conflicting participant plus its existing session
kind. The former possession/Dreamwalking-only `ParticipantPowerLock` was removed.

Body return and fatal body/avatar paths release the authoritative session. Controlled-host loss
returns possession/Dreamwalking controllers according to their existing rules; Soul Compass closes
without wrath; a lost Soul Link target is removed without erasing surviving links. Realm cohorts
hold one group session from asynchronous admission through return, including physical Shadow travel.
The generated lifecycle matrix now exhaustively contains 11 forms × 6 sources × 14 events = 924
decisions.

## TDD evidence

All commands used Java 25, `--no-daemon`, and `--max-workers=2`.

1. Admission/lifecycle RED: focused compilation failed before production changes with 59
   `compileTestJava` errors. The new tests could not resolve `MindSessionManager`,
   `SOUL_COMPASS_CONTROLLER`, `SOUL_LINK_OWNER`, `SOUL_LINK_TARGET`, or
   `REMOVE_PARTICIPANT`. This was the expected missing-authority failure.
2. Production-entrypoint RED: the isolated GameTest
   `powers_game_tests_astral_entrypoint_owns_the_participant_and_refuses_nested_soul_link` ran one
   required test and failed with: `Astral production entrypoint did not acquire authoritative
   participant ownership on tick 0`.
3. Target-loss/casting-policy RED: the focused manager test compilation failed with eight errors
   because `UnavailableOutcome`, `participantUnavailable`, and `blocksOrdinaryMagic` did not yet
   exist.
4. Strengthened diagnostic RED: after the first GREEN, the production GameTest asserted the journal's
   exact refusal reason and failed with `Nested refusal did not retain the precise authoritative
   diagnostic reason on tick 0`. Root-cause tracing showed that Astral's nearby physical mannequin
   was selected by Soul Link, mapped back to the caster identity, and made admission invalid before
   the busy-participant reporter ran. Soul Link now excludes its caster's body identity and
   deduplicates proxy/avatar identities; the same isolated GameTest then passed 1/1.

## Focused GREEN verification

- 36 JUnit tests across `MindSessionManagerTest`, `MagicLifecycleRulesTest`,
  `MagicDocumentationTest`, `MagicDiagnosticAnswerTest`, `MagicUseGateTest`, `SoulLinkMathTest`,
  possession rules/end rules, Dreamwalking rules, realm cohort contract, mindscape mob return, and
  `SourceQualityTest`: all passed.
- New Astral → nested Soul Link production-entrypoint GameTest: 1/1 passed.
- Existing fatal Astral return GameTest: 1/1 passed.
- Existing dead possessed-host / Divine Wrath GameTest: 1/1 passed.
- Existing Dreamwalking release/return GameTest: 1/1 passed.
- Existing realm cohort with mob and persistent Shadow GameTest: 1/1 passed.
- `compileGametestJava`: passed.
- `generateMagicDocs`, `verifyMagicDocs`, and `validatePowerResources`: passed; generated lifecycle
  CSV has 924 data rows.
- `auditJavaSources`: passed after regenerating the exact-source manifest.

## Cross-lane reconciliation

The `pwr004_impl` worker accidentally contributed two INT-009 hunks in this worktree: the test
`aSessionHasExactlyOneOwnerRoleAndItMustMatchTheDeclaredOwner()` and its production preflight check
that an `OWNER` participant must equal the declared owner before the matching-owner count is
accepted. They were independently inspected, retained because they enforce the required single-owner
invariant without mutation, and covered both by the focused `MindSessionManagerTest` pass and an
independent one-method rerun. No foreign-task path remains.

## Constraints

This lane intentionally did not run the machine-wide check or the full Fabric GameTest suite while
parallel worktrees were active. Validation is limited to the focused unit, generated-contract,
resource/source-audit, compilation, and five production GameTests listed above. INT-008 evidence,
the master stage checklist, and the INT-009 backlog row were not modified.
