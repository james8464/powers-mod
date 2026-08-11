# Bounded P2 acceptance ledger — 2026-08-11

Target: POWERS 1.0.2, Minecraft Java Edition 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25.

This ledger closes only the small, release-quality P2 work selected in the approved bounded-hardening design. Large travel, flight, realm, boss, replay, catalogue, and progression expansions remain in the backlog. The eight P0/P1 rows requiring real people or elapsed live evidence remain explicitly open in the [P0/P1 ledger](2026-08-11-p0-p1-acceptance.md).

## Accepted rows

| ID | Implemented result and repeatable evidence |
| --- | --- |
| COR-017 | `CooldownPresentation` is the sole seconds/tenths conversion used by the HUD, artifact screens, grimoire, activation feedback, crystals, spells, and Shadow diagnosis. `CooldownPresentationTest` covers tick boundaries and ceiling behavior. |
| PWR-009 | Crouch-use deliberately releases a hovering Cinderheart along live aim without changing original owner/controller or reflection count. Existing punch catch/deflection, expiry, trail, protected impact, and ground-scorch paths remain intact; `FireballRulesTest` and the Cinderheart GameTest cover the boundaries. |
| PWR-010 | Water, tagged copper contact, and tagged armour feed the finite conductor graph. Tagged lightning rods have priority, deal zero damage, cannot relay, and end in `GROUNDING_ROD`; node/candidate caps and body protection are preserved. `LightningStrikeRulesTest`, `LightningStrikeBodyResolverTest`, and the live rod-versus-copper GameTest cover the graph. |
| PWR-018 | Time Freeze reports authoritative energy/second and payable whole seconds before activation and warns above 50 MSPT without inventing a refusal. `TimeFreezeDrainRulesTest` covers capacity, clamping, threshold, and low-energy cases. |
| ART-010 | A paid Empyrean override produces bounded gold/violet ceremony, a persistent target-facing chat notice naming caster/category/cost, and a structured audit event. Safe-zone and server policy remain earlier absolute denials. `ConsentOverrideNoticeTest`, `ConsentOverrideRulesTest`, and live all-category override coverage pass. |
| ART-017 | Wisdom Fruit now has a 3.5% additive Realm Archive source through the existing loot event; generated item/acquisition documentation reports zero accidental unobtainable items. `LootInjectionCatalogTest`, item-doc verification, and dedicated datapack load pass. |
| SHD-012 | Shadow resolves bounded follow-up references using at most 24 redacted turns and refuses ambiguous names. `ShadowConversationMemoryTest` and `ShadowRequestParserTest` cover eviction, redaction, pronouns, and ambiguity. |
| UX-006 | Every artifact-wheel segment derives cooldown pips and energy sufficiency from the authenticated snapshot; insufficient cost is coloured red without a per-frame packet. `ArtifactWheelRulesTest` covers exact status normalization and client-side cooldown advancement. |
| NET-005 | Consent override, recovery, forced travel, testing controls, and catastrophic ritual control emit sanitized structured actions/results. The bounded ledger and denied-override limiter clear on server stop. Audit, limiter, command, protection, and Celestial tests pass. |
| NET-006 | `/powers diagnose export` atomically replaces world-relative `powers/diagnostics/latest.json` using aggregate-only schema 1. Tests reject names, UUIDs, chat, coordinates, credentials, remote content, absolute paths, and unbounded records. |
| NET-008 | Config load/reload retains a 64-entry bounded report with active revision, field, safe original/sanitized representation, and reason. Free text, credentials, and safe-zone detail are redacted; diagnostics export keeps counts only. `ConfigValidationReportTest` and diagnostic tests pass. |
| NET-014 | Shadow global/private visibility, 24-turn redacted retention, provider opt-in, HTTPS/loopback boundary, timeout/rate/concurrency caps, and no remote gameplay authority are documented in the README and threat model and covered by companion, config, dialogue, and knowledge-provider tests. |
| QA-011 | `syntheticSoak` is a separately attributed CI lane. The real 10/50/100 deterministic workload enforces time, packet, particle, scan, field, queue, ray, tactical, memory, ticket, cleanup, and a Java-25 per-thread allocation ceiling with subsystem-specific failures. |
| QA-013 | `ReadmeContractTest` derives item totals and runtime versions from packaged definitions/build properties, checks advertised diagnostic command literals, requires all generated appendices, and resolves every local Markdown link. |
| QA-014 | CI verifies and regenerates item, action/interaction/lifecycle, and 56-node rank appendices, then requires `git diff --exit-code`. Generator fixture tests and `ReleasePipelineContractTest` guard every lane. |
| QA-018 | The deterministic resource graph validator rejects missing local item/tag/loot references and recipe/loot/tag cycles; the Java-25 dedicated boot validates actual Minecraft/Fabric registries and loaded 1,605 recipes before world readiness. Invalid fixtures and the clean packaged graph both pass their expected outcomes. |

## Fresh combined evidence

- `POWERS_TEST_RUN_ID=final-clean ./gradlew clean check --no-daemon`: successful in 54 seconds; 1,312 JUnit tests and all 67 required Fabric GameTests passed, along with source/asset audits, generated docs, Python fixtures, and strict resource validation.
- `./gradlew pitest --no-daemon`: 242 of 264 mutations killed (92%), 93% mutated-line coverage, and 93% covered-test strength.
- `./gradlew verifyScreenshots saveMigrationCorpus syntheticSoak testPythonScripts validatePowerResources verifyMagicDocs verifyItemDocs verifyRankDocs --rerun-tasks --no-daemon`: successful; deterministic client goldens, save corpus, allocation-aware workload, scripts, resources, and generated appendices passed.
- `./test.sh restart-soak --hours 0.01 --cycle-seconds 10`: two isolated boots of the same world passed in 46.451 seconds. Both loaded Overworld, Nether, End, Light Realm, Dark Realm, and Middleworld; diagnostics reported zero leaked forced chunks, proxies, travel loads, and Celestial events, with no server-thread errors.

The manual 90-minute connected-player profile, full 24-hour restart soak, multiplayer quest sample collection, first-person capture review, current-source interactive client playthrough, and signed 428-row checklist remain open evidence gates. This ledger does not represent them as completed.
