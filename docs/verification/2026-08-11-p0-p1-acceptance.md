# P0/P1 acceptance ledger — 2026-08-11

Target: POWERS 1.0.2, Minecraft Java Edition 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25.

This ledger is the evidence boundary for removing rows from `docs/planning/IMPROVEMENT_BACKLOG.md`. A row is removed only when its implementation and stated acceptance condition have repeatable evidence. Harness code is not presented as proof that a long-duration or human test was run.

## Accepted rows

| IDs | Acceptance evidence |
| --- | --- |
| COR-001–005 | `SaveMigrationCorpusTest`, transactional fault injection, `FatalResolutionGateTest`, 30/30 killed `MagicLifecycleRules` mutations, `TimeStopSavedDataTest`, `CelestialRuinSavedDataTest`, and the live body/Ruin GameTests. |
| COR-006–012 | Central artifact reconciliation and `ArtifactRevocationCoverageTest`; executable 672-row lifecycle digest; attachment/item/entity SavedData tests; `ConsentPaymentLedgerTest`; Unicode/authenticated name tests; forcefield damage matrix; Crucible single-boundary transaction/atomicity tests. |
| COR-013–016 | Legacy-owned Adventure migration, border/recovery rules at live entrypoints, missing-dimension diagnostics, and `AttributeModifierIsolationTest`, which removes every POWERS scale/speed/health/knockback ID while preserving a foreign modifier and creative-flight authority. |
| PERF-002–004 | Lazy section/tag natural-Amethyst index with block/chunk invalidation and equivalence tests; every principal spatial index attributes queries, candidates, misses, fallback scans, stale removals, and estimated memory by dimension in `/powers diagnose`; hysteretic MSPT/client-setting visual budgets with readable-minimum tests. |
| PERF-007–009 | Compact persistent living-force frontier, fair per-owner work queues, bounded ticket admission plus owner/reason/deadline diagnostics, cleanup tests, and two clean isolated restart-smoke cycles. |
| PWR-001–003 | Live loop over all 253 innate/rank profiles, rank-0/rank-10 protected terrain GameTests, and explicit full/resisted/immune/reflected control categories with feedback coverage. |
| SPL-001, SPL-002, SPL-006 | Live acceptance across all 12 practical rituals plus interruption/half-refund/source-isolation tests; in-game grimoire index metadata; Celestial Ruin preview, protected dry run, pre-lock cancellation, and persistent irreversible commit tests. |
| CRY-001–002 | Registry/catalogue/knowledge count agreement and live ordinary-use coverage for every local crystal family, alongside mode, artifact-route, shared-cooldown, death, and realm-policy tests. The live matrix exposed and fixed Life Bloom caster healing and collision-safe three-Echo placement. |
| PRG-002 | Both Light and Darkness levels expose two mutually exclusive, equal-threshold routes; completion consumes one route and cannot double-count an event. |
| ART-001–002 | Live 262-item registry audit plus executable model/translation/purpose/acquisition/alias checks; deterministic 200,000-trial results for every additive pool with vanilla and extreme foreign-weight comparisons in `2026-08-11-loot-distribution.md`. |
| WRLD-001–002 | Custom Light/Dark noise and surface rules, migration-safe dimensions, and twelve authored six-site structure families with processor lists, loot/puzzle contracts, bounded cursors, and dedicated-server load proof. |
| MOB-001–002 | Reviewed entity/egg UV goldens and asset seams; bounded faction-safe guardian close/ranged/cover/retreat tactics exercised by live and deterministic tests. |
| SHD-001–005, SHD-017 | Parser/signing/Unicode/length fuzzing; two-observer lifecycle and single-body rules; exact 23+3 non-crystal catalogue; typed interruptible task plans; timestamped protected failure journal; bounded remote-provider threat model, sanitization, timeout/rate-limit, and no-authority tests. |
| UX-001–003 | 8,192-condition layout matrix plus reviewed GUI-scale/heart/mount/air/armour/spectator goldens; five-state vanilla-weight energy atlas; validated configurable anchor/margins with exact default reset. |
| VFX-001–002 | Common and client source audit rejects generic End Rod/Reverse Portal/Soul/Cloud placeholders except the compatibility classifier; all POWERS effects route through `PowerStatusEffects.hidden`, and production rejects direct effect construction/entity-effect clouds. |
| INT-001–004 | Exact executable 2,080-pair resolver digest; live beam/projectile/field/force/body collision families resolving once; deterministic three-way arbitration; one tested amethyst/forcefield/safe-zone/consent/anchor/realm precedence table. |
| NET-001–003 | Pre-payload protocol handshake with mismatch text; bounded deterministic fuzzing of all serverbound lanes/codecs; documented protection adapter exercised across damage, movement, terrain, portal, and ritual decisions. |
| QA-002–004 | Mandatory Java-25 check/GameTest/server/save/mutation/resource/doc/visual CI jobs; run-ID-isolated test reports; deterministic headless presentation goldens and approved-baseline workflow. |
| QA-007–008 | `EnergyCooldownPropertyTest` runs 100,000 hostile randomized invariant cases; PIT enforced 80% minimum and passed with 237/259 mutations killed (92%), 93% mutated-line coverage, and 93% covered-test strength. |
| PERF-001 | Commit `9d69a92` completed isolated 10/50/100 embedded-player profiles and a separate ten-rendered-client network profile for exactly 36,000 ticks and at least 1,800 seconds each. All 63,003 server-authoritative casts succeeded; the worst p95/p99 was 18.640/30.635 ms, and all four JFRs report zero data loss. Exact JSON, JFR, logs, configurations, screenshots, checksums, and rejected-attempt records are in `evidence/2026-08-14-perf-001/`. |
| QA-005 | The regenerated current 427-row checklist has an evidence-backed PASS for every action, artifact action, item, entity, system, screen, and command that requires live acceptance. Exact-build real-client campaigns include four-client combat/dialogue, all crystal and artifact paths, all 260 item identities, Middleworld entry/return, and alignment-exclusive advancement views. No `MANUAL LIVE PENDING` row remains. |
| PRG-001 | Commit `751b3bc` completed ten real Light and ten real Darkness Fabric-client sessions through the authoritative quest trackers. All 20 rows contain ten samples; median cumulative completion was 8.17h Light/7.50h Darkness and p90 was 9.33h/8.47h. Evidence-driven Darkness threshold changes preserve completed ranks and raw deed progress. Exact results and rationale are in `evidence/2026-08-14-prg-001-751b3bc/`. |
| PERF-005 | Commit `b2bff00` coalesces only semantic visual sustain updates by tick, observer, dimension, chunk, action, and phase; physical collisions and lifecycle state remain authoritative and uncoalesced. A real Fabric client received 1 of 64 duplicate semantic packets and 59 of 3,776 encoded payload-body bytes, reducing both by 98.438%. The dedicated collision GameTest and all 85 live GameTests passed on the same commit. Exact reports, key log lines, and checksums are in `evidence/2026-08-14-perf-005-b2bff00/`. |
| PERF-006 | The exact 25,600-operation, 64-observer, 48-point mass-combat profile fell from 7,587.795 to 95.653 allocated bytes per operation (98.739% lower) and p99 fell from 8,458 to 3,292 ns (61.078% faster). Retained geometry stayed bounded at 16 entries, retained payloads fell from 1,024 to zero, both Java 25 JFRs have zero data loss, and all 86 Fabric GameTests passed on accepted commit `9d2d31a`. Exact JSON, JFR, summaries, and checksums are in `evidence/2026-08-14-perf-006-9d2d31a/`. |

## Still open: evidence that cannot be fabricated

| ID | Implemented foundation | Required closure evidence |
| --- | --- | --- |
| VFX-003 | First-person exclusion/cone budgeting for Lightning and Fireball is implemented and unit-tested. | Record and review first-person captures at every rank in live gameplay. |
| QA-001 | Automated release pipeline, evidence templates, and prior client/server smoke reports exist. | Finish the current-tree client smoke, long soaks, performance runs, and signed manual acceptance before a release tag. |
| QA-006 | Isolated repeated-restart harness defaults to 24 hours and publishes clean-state JSON; a two-cycle smoke passed. | Complete the full 24-hour run and review every cycle for ticket/index/field/summon/body/freeze/Ruin leaks. |

## Verification commands

```text
./gradlew clean check --no-daemon
./gradlew pitest --no-daemon
./gradlew verifyScreenshots saveMigrationCorpus --no-daemon
./test.sh soak
./test.sh restart-soak --hours 0.01 --cycle-seconds 10
```

The 24-hour restart soak and signed final release envelope remain deliberately outside completion
claims. `VFX-003` was explicitly excluded from the selected programme. QA-005's completed checklist
is runtime evidence, not a substitute for elapsed-time gates.
