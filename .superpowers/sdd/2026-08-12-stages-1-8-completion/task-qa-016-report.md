# Task QA-016 report — intent-first source contracts

## Status

Complete on direct `main`, starting from `edd898943b121aa3ef5a55044650fe502da2ea8b`. The exact resulting commit SHA is returned with the handoff because a commit cannot embed its own object ID.

## Delivered policy

- Replaced 77 narration/comment lines with bounded authority, lifecycle, transaction, cleanup, or presentation invariants without changing gameplay behavior or mass-deleting useful comments.
- Added a Java-aware comment lexer which ignores strings, characters, and text blocks, keeps leading comment paragraphs together, audits inline comments independently, and reports portable file/line diagnostics.
- Rejects unfinished `TODO`/`FIXME`/`XXX`/`HACK` comments, short generic/mechanical narration, unsupported certainty, undocumented public integration callables, wildcard/debug residue, files over 450 lines, and files over 350 lines with multiple top-level owners.
- Added callable contracts to `CastContext`, `PowersApiV1`, and public `PowersApiRuntime` accessors. Overrides may inherit interface contracts.
- Preserved and regenerated the exact `docs/quality/code-audit.md` inventory. Corrected its generator to use the declaration-adjacent public-type contract rather than unrelated helper Javadoc.

## RED/GREEN evidence

1. RED: the new JVM fixture could not compile because `SourceAudit.Result` had none of the new finding categories. GREEN: the parser and result model exposed exact generic, unfinished, certainty, contract, and responsibility findings.
2. RED: the manifest fixture selected an earlier package-private helper comment. GREEN: the Python generator selected the public declaration's adjacent contract.
3. RED: inline public callables and adjacent inline comments escaped the first parser revision. GREEN: both have isolated regression coverage and exact line diagnostics.
4. Aggregate RED: the interrupted-ritual GameTest assumed no ordinary regeneration during a three-tick wait. Energy history showed correct rollback/half-charge plus phase-dependent regeneration. GREEN: the assertion now sums transaction-local history excluding `REGENERATION`; isolated and aggregate GameTests pass without production changes.

## Verification

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --no-daemon
```

- Build: successful in 1m3s.
- JVM: 1,557 tests in the current `test/default` result set across 368 result suites; 0 failures, 0 errors, 0 skipped.
- Python: 45 passed in 1.651s.
- Fabric GameTests: 125/125 required tests passed in 49.82s.
- Aggregate gates passed: Java source audit, non-item asset audit, resource validation, item/magic/rank documentation verification.
- Focused interrupted-ritual GameTest: 1/1 passed before the final aggregate.

## Changed surfaces

- Policy/tests: `SourceAudit`, `SourceQualityTest`, `SourceAuditPolicyTest`, `audit_java_sources.py`, and its isolated Python tests.
- Contracts/comments: bounded production comment and public-API contract corrections across 40 source files.
- Deterministic evidence: one GameTest assertion now separates transaction cost from routine regeneration.
- Records: README, changelog, generated source audit, QA-016 evidence, selected-stage plan, and active backlog.

## Self-review and concerns

The checker is deliberately bounded to production/client Java roots and callable contracts under `com.powers.api`; it does not impose public-API prose on internal implementation methods. Generated inventory hashes remain identity evidence, not semantic proof. No QA-006 files/processes or protected untracked artifacts were touched, and nothing was pushed. No known QA-016 acceptance concern remains.
