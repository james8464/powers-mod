# QA-016 intent-first source-quality evidence

Date: 2026-08-21
Platform: Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25

## Enforced policy

The exact-source owner scans all 812 common/client Java units. Its comment lexer ignores string, character, and text-block contents (including escaped delimiter-like sequences); reports exact portable file/line locations inside multiline comments; and keeps adjacent leading comment paragraphs together while auditing inline comments independently.

The gate rejects:

- `TODO`, `FIXME`, `XXX`, or `HACK` in actual comments;
- vague labels and mechanical narration without a strong paragraph-level intent/invariant signal; weak words such as `when`, `so`, or `only` cannot bypass the rule, while short factual comments remain valid;
- unsupported certainty such as “should never happen”, “always works”, or “cannot fail”;
- undocumented public types and callable members under `com.powers.api`; JDK syntax trees cover multiline methods, constructors, compact record constructors, and nested/annotated interfaces, while overrides may inherit their interface contract;
- wildcard imports, direct debug writes, source units over 450 lines, and source units over 350 lines containing multiple externally visible behavioural class/interface owners (private helpers and constructor-only nested data types are not independent owners);
- any mismatch between the exact production/client source inventory and `docs/quality/code-audit.md`.

## Audit outcome

- Parsed all 812 production/client Java source units, including 310 line-comment lines and 2,213 Javadoc openings.
- Rewrote 77 existing narration/comment lines into authority, lifecycle, bounded-work, transaction, cleanup, or presentation invariants. Useful existing intent comments were retained; comments were not mass-deleted.
- Added callable contracts to `CastContext`, `PowersApiV1`, public `PowersApiRuntime` accessors, API compact constructors, and `ProtectionService.Decision`.
- Extracted package-shared `PrivateCompanionSession` from the 450-line manager so state shared by the body, command, and diagnostics owners has one explicit package boundary; data-only nested records remain colocated.
- Corrected the manifest generator so a file's responsibility comes from the declaration-adjacent public type contract rather than an unrelated earlier helper Javadoc.
- The final scan reports no generic, unfinished, unsupported-certainty, undocumented-contract, wildcard/debug, oversized, mixed-owner, or manifest-drift finding.

## TDD record

- RED: fixture suite did not compile because the old audit exposed none of the new finding categories.
- GREEN: comment-aware diagnostics, public API member contracts, and mixed-owner limits passed isolated JVM fixtures.
- RED: the manifest selected an earlier package-private helper comment as the public responsibility.
- GREEN: the generator selected the declaration-adjacent public type contract; isolated Python fixture passed.
- RED: inline public methods and adjacent inline comments escaped the first parser revision.
- GREEN: inline public callables require contracts and inline comments remain independently audited.
- RED: review fixtures demonstrated missed multiline/compact/nested API declarations, meaningless one-word contracts, imprecise multiline locations, weak-token narration bypasses, false data-record/constructor-only ownership results, and an escaped text-block delimiter leak.
- GREEN: parse-only `JavacTask`/`DocTrees` declaration ownership, matcher-offset lines, paragraph-aware narration, behavioural-owner classification, and escape-aware text blocks pass the isolated policy suite and the full production source scan.

## Verification

The final aggregate command was:

```text
JAVA_HOME=/opt/homebrew/Cellar/openjdk@25/25.0.4/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon
```

It completed successfully in 1 minute 47 seconds. The current `test/default` result set contained 1,566 JVM test cases across 368 result suites, with zero failures, errors, or skips; 45 Python tests passed in 1.511 seconds. The Fabric run recorded 125/125 required GameTests passing in 52.21 seconds. `auditJavaSources`, `auditNonItemAssets`, resource validation, `verifyItemDocs`, `verifyMagicDocs`, and `verifyRankDocs` all passed in the same aggregate.

Repeated aggregate RED runs exposed four unrelated fixture assumptions: random structure origins could split one perception cluster across chunks; embedded players could retain earlier ritual state; natural regeneration could change health during a presentation-only assertion; and two ritual tests inferred payment from aggregate deltas that vary when regeneration occurs before rollback. The fixtures now align authored perception actors to one chunk cell, establish clean ritual state, begin presentation checks at full health, and assert the authoritative reserve/restore/half-charge event sequence directly. Each affected production-entrypoint test passed in isolation before the final 125-test aggregate passed.

Generated hashes are byte-identity evidence only; they are not represented as semantic review proof.
