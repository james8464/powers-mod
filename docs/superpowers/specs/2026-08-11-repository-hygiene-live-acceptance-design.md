# Repository Hygiene and Live Acceptance Design

## Objective

Leave POWERS easier to navigate, safer to change, reproducible to launch, and supported by fresh live evidence. Preserve gameplay, registry identifiers, saved attachments, deliberately recipe-less content, and the public commands documented for server operators.

## Chosen approach

Use a conservative, evidence-driven cleanup rather than a package-wide rewrite.

- A broad rewrite could remove dependency cycles quickly, but would create unnecessary save, mixin, networking, and gameplay risk.
- A cosmetic-only cleanup would be safer, but would leave the bootstrap god class, dead production types, launcher failure, and weak live acceptance traceability untouched.
- The selected middle path removes only proven orphans, extracts orchestration from the Fabric entrypoint, strengthens architectural gates, and tests gameplay through the real Minecraft server wherever possible.

## Repository structure

The checked-in repository contains source, resources, reproducible generators, current documentation, and verification evidence. Generated Gradle output, development worlds, logs, crash reports, and caches stay ignored. Historical Superpowers planning documents are recoverable from Git and will be collapsed into a compact development history so current documentation remains discoverable.

An asset is removable only when registry/resource traversal and text-reference checks prove it unreachable. Compatibility aliases, legacy registry identifiers, migration keys, and per-texture animation metadata are not dead merely because their content resembles another file.

## Object design

`PowersMod` remains the stable Fabric entrypoint and public identifier facade. Content registration, server lifecycle event wiring, runtime shutdown, and per-player ticking become focused collaborators. Static registries and server-thread lifecycle managers remain where they match Fabric/Minecraft ownership; introducing dependency injection around global Minecraft registries would add ceremony without improving safety.

Architecture tests enforce:

- no orphan top-level production type;
- a small entrypoint that delegates rather than owning gameplay;
- package documentation and public contracts;
- reviewed source-size, unfinished-marker, wildcard-import, and debug-output boundaries.

## Launch and test harness

`test.sh` resolves Java 25 from an explicit `POWERS_JAVA_HOME`, a valid `JAVA_HOME`, Minecraft's bundled runtime on macOS, or a compatible `java` on `PATH`. It supports clean verification, GameTests, dedicated server, client, and synthetic soak entrypoints. GameTest run files are seeded before launch to prevent false error telemetry about missing EULA/server properties.

## Gameplay verification

Maintain a machine-readable acceptance catalogue covering registries and critical behavior families: innate abilities, spells, crystals, both artifacts, realms, body proxies, living forces, progression, items, mobs, commands, HUD/resources, and collision logic. Pure policy stays in JUnit. Anything requiring real entities, damage, dimensions, chunks, commands, or registry bootstrap belongs in Fabric GameTests.

Historical crash reports are treated as reproduction evidence. A reported stack is checked against current source and receives a regression test when the failure contract is not already explicit. Authentication and Realms failures from Fabric's offline development identity are classified as environment noise, not mod defects.

Live verification consists of:

1. clean full Gradle check;
2. Fabric GameTest server;
3. dedicated-server boot with all custom dimensions;
4. registry and interaction acceptance matrix;
5. 10/50/100-player deterministic work-budget soak tests;
6. development-client resource/atlas initialization and crash-log inspection;
7. operator-command and testing-arena smoke scenarios.

The bundle-less Gradle client may not expose a macOS accessibility application. When that occurs, live Minecraft GameTests, controllable terminal/RCON-style commands, generated UI geometry/resource tests, and client logs are the authoritative automated evidence; visual claims remain limited to what those checks prove.

## Performance policy

No ordinary tick may scan all tracked entities, fields, wards, force blocks, named targets, or force chunks. Audits verify caps, spatial indices, rotating cursors, lifecycle cleanup, and forced-chunk deadlines. Soak tests use deterministic counters and fail when work grows faster than the documented per-player/per-tick budget.

## Compatibility and deletion policy

- Do not rename registered items, blocks, entities, dimensions, damage types, powers, spells, or data components.
- Do not remove saved-data migration branches or hidden compatibility aliases.
- Do not add recipes for crystals or items deliberately listed as unobtainable.
- Do not delete development worlds or user-authored saves.
- Every removed tracked file remains recoverable from Git; the final handoff lists material removals.

## Completion gate

Completion requires a clean worktree, synchronized remote branch, current audit manifests and generated docs, no unexplained crash/error telemetry, all tests green from a clean build, a clean dedicated-server stop, and a client bootstrap with POWERS resources loaded.
