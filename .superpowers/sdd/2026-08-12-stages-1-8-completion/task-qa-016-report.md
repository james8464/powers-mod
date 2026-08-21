# Task QA-016 report — intent-first source contracts

## Status

Complete on direct `main`, starting from `edd898943b121aa3ef5a55044650fe502da2ea8b`. The exact resulting commit SHA is returned with the handoff because a commit cannot embed its own object ID.

## Delivered policy

- Replaced 77 narration/comment lines with bounded authority, lifecycle, transaction, cleanup, or presentation invariants without changing gameplay behavior or mass-deleting useful comments.
- Added a Java-aware comment lexer which ignores strings, characters, and escape-aware text blocks, keeps leading comment paragraphs together, audits inline comments independently, and reports the exact finding line inside multiline comments.
- Rejects unfinished `TODO`/`FIXME`/`XXX`/`HACK` comments, vague/mechanical narration, unsupported certainty, undocumented public integration callables, wildcard/debug residue, files over 450 lines, and files over 350 lines with multiple externally visible behavioural class/interface owners.
- Uses parse-only JDK syntax trees and doc trees for annotated public types, meaningful contracts on every visible API type, multiline methods, explicit and compact constructors, and nested/annotated interfaces. Only exact `Override` or `java.lang.Override` annotations inherit contracts. A nested type is public surface only when explicitly public or implicitly public as an interface member; package-private/private helpers are excluded. Non-API top-level types retain the presence-only documentation rule.
- Preserved and regenerated the exact `docs/quality/code-audit.md` inventory. Corrected its generator to use the declaration-adjacent public-type contract rather than unrelated helper Javadoc.

## RED/GREEN evidence

1. RED: the new JVM fixture could not compile because `SourceAudit.Result` had none of the new finding categories. GREEN: the parser and result model exposed exact generic, unfinished, certainty, contract, and responsibility findings.
2. RED: the manifest fixture selected an earlier package-private helper comment. GREEN: the Python generator selected the public declaration's adjacent contract.
3. RED: inline public callables and adjacent inline comments escaped the first parser revision. GREEN: both have isolated regression coverage and exact line diagnostics.
4. Aggregate RED: ritual tests inferred payment from net deltas, which vary when ordinary regeneration occurs before rollback. GREEN: both now assert the authoritative reserve, baseline restore, and half-charge event sequence directly.
5. Review RED: focused fixtures exposed declaration, exact-line, weak-token, mixed-owner, and escaped-text-block gaps; additional fixtures proved that a lone outcome verb and constructor-only data class are not meaningful contracts or behavioural owners. GREEN: the AST/comment policy passes all focused fixtures and the exact production scan.
6. Aggregate RED: random chunk origins, reused mock-player state, and natural regeneration exposed three unrelated fixture assumptions. GREEN: the production tests now own deterministic perception geometry, ritual state, and health baselines; each passed in isolation and in the final aggregate.
7. Re-review RED: five focused failures proved that annotated same-line public types, suffix-named fake overrides, package-private nested API methods, package-private top-level helpers, and package-private nested behavioural helpers escaped or polluted the policy. GREEN: one AST visibility model now drives type documentation, callable reachability, and public behavioural-owner counts; implicit-public interface members and exact qualified overrides have positive fixtures.
8. Final review RED: explicit-public nested API classes and implicit-public interface member types could expose documented methods without documenting their own purpose. GREEN: both visible nested-type forms require meaningful API type prose, while the global top-level and package-private exclusion policies remain unchanged.
9. Consistency RED: a top-level API type with `Type.` passed while the same placeholder failed on a visible nested API type. GREEN: every visible API type now requires at least four words and an explicit authority, validation, lifecycle, outcome, or type-purpose signal; non-API top-level types remain presence-only.

## Verification

```text
JAVA_HOME=/opt/homebrew/Cellar/openjdk@25/25.0.4/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon
```

- Build: successful in 1m49s.
- JVM: 1,575 tests in the current `test/default` result set across 368 result suites; 0 failures, 0 errors, 0 skipped.
- Python: 45 passed in 1.507s.
- Fabric GameTests: 125/125 required tests passed in 50.82s.
- Aggregate gates passed: Java source audit, non-item asset audit, resource validation, item/magic/rank documentation verification.
- Focused perception, Augury, packet-fault, interrupted-ritual, and reload-cancellation GameTests: each passed before the final aggregate.
- Final contract-consistency rerun: 33 focused source-policy tests passed; `check -x runGameTest` passed in 47s with 1,576 JVM tests across 368 suites and all 45 Python tests. Three unchanged full-check attempts passed every QA-016/audit/resource task but the concurrently running QA-006 server/client soak made the shared GameTest runtime intermittently fail presentation, mind-session, or proxy-cleanup timing assertions; the soak was preserved and no gameplay code was changed for this test-only correction.

## Changed surfaces

- Policy/tests: `SourceAudit`, `SourceQualityTest`, `SourceAuditPolicyTest`, `audit_java_sources.py`, and its isolated Python tests.
- Contracts/comments: bounded production comment and public-API contract corrections across 40 source files.
- Review correction: API compact-constructor/decision contracts and one client cache invariant; extracted `PrivateCompanionSession` as the real package-shared behavioural owner while retaining data-only nested records.
- Deterministic evidence: five aggregate-sensitive GameTest fixtures now isolate authored geometry/state or assert transaction events instead of phase-dependent ambient deltas.
- Records: README, changelog, generated source audit, QA-016 evidence, selected-stage plan, and active backlog.

## Self-review and concerns

The checker is deliberately bounded to production/client Java roots and callable contracts under `com.powers.api`; it does not impose public-API prose on internal implementation methods. Mixed responsibility counts the public top-level owner plus actually-public or interface-implicit-public nested class/interface owners with declared non-constructor behaviour—not package-private helpers, syntax-only types, or data records. Generated inventory hashes remain identity evidence, not semantic proof. No QA-006 files/processes or protected untracked artifacts were touched, and nothing was pushed. No known QA-016 acceptance concern remains.
