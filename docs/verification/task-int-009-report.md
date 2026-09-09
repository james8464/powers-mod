# INT-009 Authoritative Mind Sessions — Task Report

Date: 2026-09-08

Branch: `int-009-mind-sessions`

Required base: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`

Status: **STAGED — fresh independent review pending; not integrated or pushed.**

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
Each Realm member now retires only its exact participant authority on death, disconnect, or return.
Owner-first return transfers the session's single owner role deterministically, preserving exclusivity
for distant detached players, ordinary mobs, and Shadow until each independent lifecycle ends.
The generated lifecycle matrix now exhaustively contains 11 forms × 6 sources × 14 events = 924
decisions.

## TDD evidence

All successful Gradle commands used Java 25, `--no-daemon`, and `--max-workers=2`. Two unqualified
environment preflight invocations failed before configuration because no default Java runtime was
available; rerunning the unchanged commands with the repository's Java 25 path produced the evidence
below.

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
5. Fresh split-cohort manager RED: the two new JUnit tests failed compilation with four unresolved
   `realmParticipantReturned(UUID, UUID)` calls. The manager had no operation that could retire one
   Realm member while preserving the other participant indexes or transfer the owner role.
6. Fresh owner-first production RED: the isolated GameTest failed with `Owner-first return unlocked
   or recalled the distant detached member on tick 0` because the owner's body still released the
   whole Realm session.
7. Fresh tracked-member production RED: the isolated mob return failed with `Returned tracked Realm
   mob retained its participant index on tick 0`; the isolated Shadow return likewise failed with
   `Returned Shadow retained its Realm participant authority on tick 0`. Neither return path retained
   the exact Realm session token needed for idempotent participant cleanup.
8. Final source-quality RED: the first expanded verification run completed 39/40 JUnit tests and
   rejected `PrivateCompanionManager` (473 lines) and `BodyProxyManager` (452 lines). Realm-token
   bookkeeping was moved into the existing package-owned companion session state and the body cleanup
   branch was simplified; the regenerated audit records 445 and 450 lines, and the exact 40-test gate
   then passed.
9. Strengthened tracked-death RED: the mob/Shadow death GameTest was extended to require the tracked
   mob's bounded return-origin record to retire with its participant authority. It failed with `Dead
   Realm mob retained a stale return-origin record on tick 2`; routing the common death hook through
   the tracker's token-matched, remove-once cleanup made the isolated test pass 1/1.

## Focused GREEN verification

- 40 JUnit tests across `MindSessionManagerTest`, `MagicLifecycleRulesTest`,
  `MagicDocumentationTest`, `MagicDiagnosticAnswerTest`, `MagicUseGateTest`, `SoulLinkMathTest`,
  possession rules/end rules, Dreamwalking rules, realm cohort contract, mindscape mob return, and
  `SourceQualityTest`: all passed.
- New Astral → nested Soul Link production-entrypoint GameTest: 1/1 passed.
- Existing fatal Astral return GameTest: 1/1 passed.
- Existing dead possessed-host / Divine Wrath GameTest: 1/1 passed.
- Existing Dreamwalking release/return GameTest: 1/1 passed.
- Existing realm cohort with mob and persistent Shadow GameTest: 1/1 passed.
- Existing separated tracked-mob return GameTest: 1/1 passed.
- New owner-first and independent player-return Realm GameTest: 1/1 passed.
- New non-owner disconnect Realm GameTest: 1/1 passed.
- New mob/Shadow death Realm GameTest: 1/1 passed.
- New tracked-mob independent return Realm GameTest: 1/1 passed.
- New Shadow independent return Realm GameTest: 1/1 passed.
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

## Deterministic branch gate and constraints

After the focused proof was committed, the exact Java 25 branch gate was run serially:

`./gradlew check compileGametestJava -x runGameTest --rerun-tasks --no-daemon --console=plain`

The first attempt stopped on the strict stale non-item-asset manifest check. Regenerating that
mechanical manifest exposed matching VFX-audit drift on the second attempt. After regenerating both
audits, the unchanged command completed **GREEN** in 2m 1s. Mechanical XML aggregation found **424
suites, 1,842 JUnit tests, 0 failures, 0 errors, and 0 skipped**; Python reported **240/240 passed**.
Resource validation, generated documentation, source/non-item/visual audits, example-extension
compilation, and GameTest compilation all passed.

Final log: `/tmp/powers-int009-deterministic-green-retry2-20260909.log`

SHA-256: `44c5c0c432976e38573145b6d53d6e8e371f8f8fac9123f75845e078036b564f`

The full Fabric GameTest suite remains deferred; the 11 exact production GameTests listed above are
the authoritative live evidence for this lane. INT-008 evidence, protected main, the master stage
checklist, and the INT-009 backlog row were not modified. A fresh independent review must return
READY before integration.
