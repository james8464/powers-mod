# SHD-011 verification report

## Scope

Generate source-linked Shadow offline knowledge for every required catalogue row: 260 items, 64 magic actions, 56 Light/Darkness rank nodes, three realms, and 50 interaction-rule families.

## Implementation

- `scripts/generate_knowledge_docs.py` parses the checked canonical catalogues and emits 433 bounded datapack entries plus `docs/gameplay/knowledge-coverage.md`.
- Every entry retains a source link and satisfies the production parser's identifier, title, answer, keyword, source, and reveal-rank bounds.
- `verifyKnowledgeDocs` depends on the item, magic, and rank documentation checks and rejects missing, stale, or extra generated entries. The task is part of `check` and the CI regeneration/diff lane.
- A JVM coverage test parses all 433 generated files through `KnowledgeEntryParser` and independently retrieves each from `KnowledgeIndex`.
- A live GameTest requires exactly 447 loaded entries (14 authored + 433 generated) and exercises natural-language item, action, rank, realm, and interaction-rule lookups.

## Evidence

| Evidence | Result | SHA-256 |
|---|---|---|
| `/tmp/powers-shd011-red-20260909.log` | RED: tests fail because the generator is absent | `a28974de596b0951315bc2d37da6c9c0b1b9bdacc6922e13057ba67f82f1ed0c` |
| `/tmp/powers-shd011-focused-green-20260909.log` | Focused parser/index and generated-doc gates pass | `f47791aae72d4ed97af3058ad5e7acd7f7d96d4c2fb86bace64dae01c3091677` |
| `/tmp/powers-shd011-exact-live-20260909.log` | Retained diagnostic: 447 entries load; action sample exposed a catalogue mismatch | `391add0ff58ee4f092c558e280ec9c50ccfdc32e472734ba73e64b88594c9d92` |
| `/tmp/powers-shd011-exact-live-diagnostic-20260909.log` | Retained diagnostic: identified interaction-rule scoring over the action query | `4a8ba7f16b59390b3b88de7eff4f92db030c362b42ade8887aea70c59099a13a` |
| `/tmp/powers-shd011-exact-live-final-20260909.log` | Retained diagnostic: human action phrase worked, revealing the requested example was not one of the 64 canonical actions | `b4ec1066b83f955f92e038d5355b83617afcc23b0e83a6004c8655bee971c0a5` |
| `/tmp/powers-shd011-exact-live-final2-20260909.log` | GREEN: exact isolated live test, 1/1 | `1e8ad9d3c0d4bf485dc61b73d341499cb8eb837386514769d06d8c484575576f` |
| `/tmp/powers-shd011-deterministic-green-20260909.log` | GREEN: literal Java 25 `check --rerun-tasks`; 168/168 GameTests, 1,837 JUnit tests across 425 suites, 243 Python tests, and all audits/resource/generated-doc gates | `39b96ec337176bcf77553247b36db19a081f050e794f02aa5a0c2e6402194eb4` |

## State

Implemented and verified on an isolated worktree based exactly on `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`. Independent review remains required before integration; this report does not self-approve the unit.
