# Queue 1 acceptance ledger — 2026-08-11

Target: POWERS 1.0.2, Minecraft Java Edition 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25.

This ledger records the 25 implemented Queue 1 rows. It distinguishes deterministic automated proof from interactive visual acceptance; it does not treat source or asset hashes as semantic review.

| ID | Implemented result and focused evidence |
| --- | --- |
| SPL-010 | Grave Recall persists an exact timestamp, expires new memories after seven in-game days, preserves legacy untimed saves, and gives an owner-only same-dimension compass bearing. `LastDeathRecordTest` covers codec, expiry, and direction. |
| ART-011 | Malignember derives eligible destructive actions from the canonical registry and advertises exact cost/savings in artifact snapshots and tooltips. `ArtifactEnergyModifiersTest` covers eligibility and arithmetic. |
| VFX-008 | Beam collisions and boss impacts use separate short generated mono OGG cues, leaving long Celestial Ruin tinnitus exclusive to the catastrophe. Resource validation and sound registration tests cover IDs/files. |
| INT-005 | Physical magic collisions emit bounded semantic counter/resonance sigils and action-bar cues once per accepted collision. `InteractionPresentationTest` covers outcome mapping. |
| ART-005 | Ritual Dagger previews health cost, survival floor, and gain before applying the same server-owned rules. `RitualDaggerRulesTest` covers non-lethal boundaries. |
| SPL-015 | Dispel advertises the exact nearest legal field, captures an immutable target snapshot, and revalidates it at release. `SpellTargetRulesTest` covers nearest-legal selection and stale snapshots. |
| QA-012 | Crash reports add bounded aggregate session counts and latest typed failure reason/tick with identifying data excluded. `CrashDiagnosticSectionTest` covers redaction and lifecycle. |
| PRG-007 | A 32-entry per-player ledger reconciles consumption/restoration by source into synced HUD diagnostics and `/powers diagnose`. `EnergyHistoryLedgerTest` and payload tests cover caps and totals. |
| ART-008 | Miniportals store sanitized anchor names, expose charged/empty models and durability, and share one inventory-order anchor rule between tooltip and travel. `MiniportalRulesTest` covers selection and names. |
| SHD-015 | Owner-only Shadow energy/status appears only while active/rebuilding, syncs lifecycle changes immediately, suppresses tick churn, and heartbeats every second. `ShadowHudRulesTest` and `ShadowStatusSyncRulesTest` cover visibility/cadence. |
| UX-010 | The first-awakening written guide derives concise innate, spell, crystal, and artifact binding diagrams from current controls. `PlayerGuideTest` prevents stale hard-coded bindings. |
| SPL-008 | Dimensional Anchor exposes owner, point, group circle, renewal, tether, and authoritative remaining duration through one shared state. Focused anchor and spell-field tests cover renewal/expiry. |
| CRY-012 | Soul Link exposes owner topology and independent remaining mirrored-damage caps, consumes only health actually removed, and cannot recursively bounce. `SoulLinkMathTest` and crystal tests cover caps. |
| CRY-009 | Chrono Stop exposes owner/deadline status and temporal-fracture presentation while retaining true global tick ownership. `GlobalTimeStopRulesTest` covers ownership/deadline. |
| PWR-026 | Double Health heals only to its authored cap, locks repeated toggle healing, and drives a reduced-motion-safe extra-heart pulse. Focused Double Health/HUD tests cover the rules. |
| PWR-019 | Forcefields expose source ownership, crack stage, safe same-source repair/merge, and crouching nearby-player opt-out while preserving sacrificial overkill; ordinary hostile mobs are excluded. `MagicShieldManagerTest` covers integrity, sharing policy, and merge. |
| PWR-020 | Gravity has server-owned pull/orbit/repel modes, shared control resistance, stable overlap snapshots, and an authenticated artifact variant. `GravityDisplacementRulesTest`, `ControlResistanceCoverageTest`, and `ArtifactMenuRulesTest` cover rules/snapshot routing. |
| CRY-013 | Rainbow crouch-use opens a narrated non-pausing radial; the client sends only an index and the server validates/persists it. `CrystalSelectorRulesTest` covers normalization. |
| ART-004 | Reservoir transfer UI shows exact main/auxiliary balance and shortfall; packets send direction only and the server commits fixed atomic steps. `ArtifactEnergyReservoirTest` covers transfer invariants. |
| UX-005 | Artifact wheel preview is safe by default; release-to-cast is persisted opt-in, server-revalidated, and retains keyboard/narration parity. `ArtifactWheelRulesTest` covers release actions. |
| QA-017 | Missing optional HUD art resolves through a same-contract procedural ten-symbol renderer, while missing POWERS item models are intercepted by the production item-model resolver and use the vanilla barrier model instead of failing model bake. `OptionalAssetFallbackTest` covers both decisions. |
| NET-004 | Six independent permission nodes cover diagnose, testing, travel, assign, recover, and boss controls through optional Fabric Permissions API with vanilla-tier fallback. `PermissionNodePolicyTest` covers fallback/routing. |
| PRG-013 | Rank prefixes decorate the existing display component and can be disabled without rewriting signed content, teams, nickname style, or death-message identity. `RankNameFormatterTest` covers preservation. |
| COR-019 | All 64 canonical actions declare a player/living/non-living target capability contract and return typed unsupported outcomes. `MagicActionCatalogueTest` and participant contract tests enforce completeness. |
| INT-016 | Foreign projectiles, damage sources, effects, infinite durations, and null direct targets follow explicit safe defaults at live boundaries. Compatibility, status-effect, and damage tests cover fixtures. |

## Combined release evidence

- `POWERS_TEST_RUN_ID=queue1-final-review ./gradlew clean check --no-daemon`: clean combined build, 1,361 JUnit tests, all 67 required Fabric GameTests, exact source/non-item-asset audits, generated item/magic/rank documentation, six Python fixture suites, and strict resource validation passed.
- `./gradlew verifyScreenshots saveMigrationCorpus syntheticSoak --rerun-tasks --no-daemon`: deterministic visual goldens/screenshot contracts, save migration corpus, and separately attributed 10/50/100-player workload budgets passed.
- `./test.sh restart-soak --hours 0.01 --cycle-seconds 10`: two isolated boots of the same dedicated world loaded and saved Overworld, Nether, End, Light Realm, Dark Realm, and Middleworld; final diagnostics reported zero leaked forced chunks, proxies, travel loads, Celestial events, or Shadow state.

Interactive appearance and gameplay feel remain manual acceptance work even when deterministic rendering checks pass. An external LuckPerms provider was not installed for this run; the Fabric Permissions API adapter and vanilla fallback are covered deterministically.
