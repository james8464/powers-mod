# Task QA-016 report — intent-first source contracts

## Status

Complete on direct `main`, starting from `edd898943b121aa3ef5a55044650fe502da2ea8b`. The exact resulting commit SHA is returned with the handoff because a commit cannot embed its own object ID.

## Delivered policy

- Replaced 77 narration/comment lines with bounded authority, lifecycle, transaction, cleanup, or presentation invariants without changing gameplay behavior or mass-deleting useful comments.
- Added a Java-aware comment lexer which ignores strings, characters, and escape-aware text blocks, keeps leading comment paragraphs together, audits inline comments independently, and reports the exact finding line inside multiline comments.
- Rejects unfinished `TODO`/`FIXME`/`XXX`/`HACK` comments, vague/mechanical narration, unsupported certainty, undocumented public integration callables, wildcard/debug residue, files over 450 lines, and files over 350 lines with multiple externally visible behavioural class/interface owners.
- Uses parse-only JDK syntax trees and doc trees for multiline methods, explicit and compact constructors, nested/annotated interfaces, and meaningful contract vocabulary. Overrides may inherit interface contracts; private nested methods are not public surface.
- Preserved and regenerated the exact `docs/quality/code-audit.md` inventory. Corrected its generator to use the declaration-adjacent public-type contract rather than unrelated helper Javadoc.

## RED/GREEN evidence

1. RED: the new JVM fixture could not compile because `SourceAudit.Result` had none of the new finding categories. GREEN: the parser and result model exposed exact generic, unfinished, certainty, contract, and responsibility findings.
2. RED: the manifest fixture selected an earlier package-private helper comment. GREEN: the Python generator selected the public declaration's adjacent contract.
3. RED: inline public callables and adjacent inline comments escaped the first parser revision. GREEN: both have isolated regression coverage and exact line diagnostics.
4. Aggregate RED: ritual tests inferred payment from net deltas, which vary when ordinary regeneration occurs before rollback. GREEN: both now assert the authoritative reserve, baseline restore, and half-charge event sequence directly.
5. Review RED: focused fixtures exposed declaration, exact-line, weak-token, mixed-owner, and escaped-text-block gaps; additional fixtures proved that a lone outcome verb and constructor-only data class are not meaningful contracts or behavioural owners. GREEN: the AST/comment policy passes all focused fixtures and the exact production scan.
6. Aggregate RED: random chunk origins, reused mock-player state, and natural regeneration exposed three unrelated fixture assumptions. GREEN: the production tests now own deterministic perception geometry, ritual state, and health baselines; each passed in isolation and in the final aggregate.

## Verification

```text
JAVA_HOME=/opt/homebrew/Cellar/openjdk@25/25.0.4/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon
```

- Build: successful in 1m47s.
- JVM: 1,566 tests in the current `test/default` result set across 368 result suites; 0 failures, 0 errors, 0 skipped.
- Python: 45 passed in 1.511s.
- Fabric GameTests: 125/125 required tests passed in 52.21s.
- Aggregate gates passed: Java source audit, non-item asset audit, resource validation, item/magic/rank documentation verification.
- Focused perception, Augury, packet-fault, interrupted-ritual, and reload-cancellation GameTests: each passed before the final aggregate.

## Changed surfaces

- Policy/tests: `SourceAudit`, `SourceQualityTest`, `SourceAuditPolicyTest`, `audit_java_sources.py`, and its isolated Python tests.
- Contracts/comments: bounded production comment and public-API contract corrections across 40 source files.
- Review correction: API compact-constructor/decision contracts and one client cache invariant; extracted `PrivateCompanionSession` as the real package-shared behavioural owner while retaining data-only nested records.
- Deterministic evidence: five aggregate-sensitive GameTest fixtures now isolate authored geometry/state or assert transaction events instead of phase-dependent ambient deltas.
- Records: README, changelog, generated source audit, QA-016 evidence, selected-stage plan, and active backlog.

## Self-review and concerns

The checker is deliberately bounded to production/client Java roots and callable contracts under `com.powers.api`; it does not impose public-API prose on internal implementation methods. Mixed responsibility means an externally visible class/interface with declared behaviour, not merely another syntax-level type or data record. Generated inventory hashes remain identity evidence, not semantic proof. No QA-006 files/processes or protected untracked artifacts were touched, and nothing was pushed. No known QA-016 acceptance concern remains.
