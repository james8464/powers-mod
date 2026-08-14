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
| PERF-001 | Commit `6fb6991` completed paced live profiles at 10/50/100 connected embedded players for 36,000 ticks and at least 1,800 seconds each. All 54,000 authenticated casts succeeded; p95 was 7.23/5.85/6.58 ms and p99 was 8.83/8.50/9.33 ms. Three 1,800-second JFRs have zero data loss in `evidence/2026-08-12-perf-001/`. |
| QA-005 | The regenerated current 427-row checklist has an evidence-backed PASS for every action, artifact action, item, entity, system, screen, and command that requires live acceptance. Exact-build real-client campaigns include four-client combat/dialogue, all crystal and artifact paths, all 260 item identities, Middleworld entry/return, and alignment-exclusive advancement views. No `MANUAL LIVE PENDING` row remains. |

## Still open: evidence that cannot be fabricated

| ID | Implemented foundation | Required closure evidence |
| --- | --- | --- |
| PERF-005 | Per-tick observer/dimension/chunk/action coalescer and collision-equivalence tests exist. | Capture and publish before/after encoded bytes and packet counts in live mass combat. |
| PERF-006 | Geometry/payload canonicalization and allocation-sampling JFR are implemented. | Publish a before/after allocation profile showing materially lower young-generation churn. |
| PRG-001 | Persistent anonymous route/duration telemetry, median/p90 summaries, and a 20-sample-per-level publication lock are implemented. | Collect real multiplayer samples for all 20 alignment/level rows, then publish and justify any quest changes/migration. |
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

The final connected-player profiles, 24-hour restart soak, quest data collection, first-person
capture review, and signed release envelope remain deliberately outside automated completion
claims. QA-005's completed checklist is runtime evidence, not a substitute for those elapsed-time
gates.
