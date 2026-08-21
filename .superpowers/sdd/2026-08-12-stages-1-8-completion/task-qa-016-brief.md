# Task QA-016 — intent-first source contracts

## Requirement

Audit production comments so they explain intent, authority, lifecycle, or invariants instead of narrating the adjacent statement. Strengthen the existing exact-source quality owner to reject generic comment noise, stale unfinished markers, unsupported certainty claims, undocumented public integration contracts, and oversized mixed-responsibility source units.

## Acceptance

- Add fixture-driven JVM tests first and observe each fail against the existing `SourceAudit`: generic mechanical narration; `TODO`/`FIXME`/`XXX`/`HACK` in actual comments while ignoring string literals; unsupported claims such as “should never happen” or “always works”; undocumented public API members in `com.powers.api`; and oversized/mixed-responsibility source units.
- The checker parses Java comments rather than matching arbitrary string literals. Diagnostics identify the exact portable file and line, with distinct result sections for narration noise, unsupported claims, unfinished comments, undocumented public contracts, and responsibility size.
- Public integration API types and callable members have useful contracts covering authority, validation, lifecycle, or observable outcomes. Overrides may inherit an already documented interface contract; private/package-internal implementation details are not forced into public API prose.
- Audit every production/client comment under the new rules. Rewrite only violations, preserving useful intent/invariant comments and behavior; do not mass-delete comments or manufacture boilerplate.
- Keep the existing exact-version `docs/quality/code-audit.md` contract and generator. Regenerate it only after intentional production source changes; hashes remain identity evidence, never semantic proof.
- Keep responsibility checks bounded and deterministic. A source unit above the reviewed hard limit fails; a near-limit unit with multiple independent top-level responsibilities also fails. Generated code is not silently exempted.
- Add Python behavior tests for the audit manifest/check path where it materially complements the JVM policy tests; tests execute the tool on isolated temporary trees rather than grepping its source.
- Run focused JVM/Python tests, production/client/GameTest compilation, full Fabric GameTests, resource/documentation/audit gates, and aggregate `check`. Update README, changelog, evidence, plan, and backlog only after literal proof, then make one cohesive direct-main commit without pushing.

## Locked boundaries

- Fabric/Minecraft 26.2, Java 25; authoritative gameplay behavior and save formats do not change.
- Existing generated audit/document contracts remain reproducible.
- QA-006 continues in its isolated worktree; do not touch its job, processes, report, log, or files.
- Preserve unrelated untracked artifacts. Never stage `.codex-tmp/`, `.lwjgl/`, Python caches, compatibility artifacts, or QA-005 screenshots.

## Report contract

Write `.superpowers/sdd/2026-08-12-stages-1-8-completion/task-qa-016-report.md` with the audited policy, RED/GREEN evidence, exact violations repaired, commands/counts, changed files, commit SHA, self-review, and concerns. Return status, SHA, focused/full verification summary, and concerns.
