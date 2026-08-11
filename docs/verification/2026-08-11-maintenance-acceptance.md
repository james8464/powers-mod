# Repository maintenance and acceptance — 2026-08-11

Target: Minecraft Java Edition 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2 and Java 25.

## Scope and changes

- Reviewed all 553 production/client Java source units through the exact-version audit, reachability gate, source-quality rules, dependency/search audits and affected behavior suites.
- Replaced the 409-line `PowersMod` coordinator with a 67-line entrypoint, ordered `PowersBootstrap`, callback-owning `PowersServerLifecycle` and single-pass `PlayerPowerTicker`.
- Removed four production types that had no production/resource consumer: `ImportedItemPurpose`, `FxSequence`, `FaultboundVerdict` and `FreezeOwner`. Their isolated tests were removed with them; save identifiers and compatibility aliases were not touched.
- Removed superseded internal plans/specifications from the release tree. Their exact contents remain in Git through commit `ebac214`; implemented contracts remain in source, tests, README and generated catalogues.
- Corrected the package version to 1.0.2 and added a test that keeps it synchronized with the latest changelog heading.
- Repaired the location-independent launcher and GameTest bootstrap, added a registry-synchronized acceptance catalogue, bounded test arena, and operator coverage report.

## Defects and risk findings

| Finding | Resolution / evidence |
|---|---|
| Five historical server crash reports shared one cause: Cinderheart block impact called the direct-target forcefield path with no living target. | The existing null-safe resolver was retained and a real-server block-impact GameTest now protects it. No other POWERS stack was found in current/historical crash or log review. |
| Changelog/runtime claimed 1.0.2 while `gradle.properties` packaged 1.0.0. | `mod_version=1.0.2`; `ReleaseMetadataTest` fails on future drift. Dedicated server and client both logged `powers 1.0.2`. |
| Direct Gradle entrypoints failed on this host when no system Java was registered. | `test.sh` resolves Java 25 from an explicit override, valid environment, Minecraft runtime, Homebrew, or `PATH`; its modes are tested from outside the repository. |
| Isolated GameTest boot produced misleading missing-EULA/properties telemetry. | `runGameTest` now prepares only its build-local run directory before launch. |
| A second deliberate server start encountered `session.lock` while the first live verification server was still active. | This was an operator/test-harness collision, not a mod fault. The original server accepted `/powers diagnose` and shut down cleanly; no lock file was deleted or bypassed. |
| Potion-effect clouds or generic entity-effect particles could regress. | Production contains one `MobEffectInstance` constructor, inside `PowerStatusEffects.hidden`; source-quality checks prohibit bypasses and `ENTITY_EFFECT` particles. |
| Hot paths could regress to whole-world entity scans. | All production entity collection routes through `BoundedEntityCandidates`; named targets, wards, forces, fields, presences and viewers use indexes/capped fallback work. |

## Runtime evidence

- 587 JUnit tests and every exact audit executed successfully in the clean gate with no failures.
- 39 Fabric GameTests passed on a live server, including crystal travel, vulnerable bodies, forcefields, Darkness/Pure Light, testing controls, catastrophic magic, First Vessel damage, Shadow Sword lightning and the Cinderheart regression.
- Dedicated server reached `Done`, accepted `/powers diagnose` and `/powers testing coverage`, reported zero leaked fields/tickets/proxies/forced chunks in an empty world, and saved Overworld, Nether, End, Dark Realm, Light Realm and Middleworld on `stop`.
- Development client loaded POWERS 1.0.2, 23 powers, both 28-node rank mazes and the 73-action/2,701-interaction kernel. OpenGL/OpenAL and every vanilla/POWERS atlas initialized without a missing POWERS resource, mixin, model or texture error. Offline-development Mojang/Realms 401s and vanilla shader-link warnings are external expected telemetry.
- The deterministic 10/50/100-player workload ran 1,200 ticks per population. It exercised magic presences, generic fields, amethyst wards, named-target lookup and rotating work queues while enforcing 512 particles, 2,048 entity inspections, packet-lane limits, exact index cleanup and one body-proxy ticket chunk.
- Strict resource validation passed. The external Minecraft test-layout helper warns because it only recognizes `src/main/test` fixtures, while this Fabric Loom project correctly uses the configured `src/gametest` source set; the actual 39-test Fabric server run is authoritative.

## Evidence boundary

The Gradle-launched LWJGL window is a bundle-less Java process and did not appear as an accessibility-controlled macOS app. Client startup, atlases, logs, deterministic HUD geometry/resources and live server scenarios were verified, but subjective claims such as ideal particle density at every display/GUI scale still require a human staging-world pass. The test arena and `/powers testing` controls make that pass repeatable. Catastrophic terrain features should always be accepted on a disposable backup before a valuable server world.
