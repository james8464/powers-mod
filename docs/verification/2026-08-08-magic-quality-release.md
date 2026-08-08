# Magic quality release verification — 2026-08-08

## Result

The quality pass is release-ready for Minecraft Java Edition 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, and Java 25. The mod remains server-authoritative, contains no released crystal recipe, and builds from the sole local branch `main`.

## Requirement evidence

| Requirement | Evidence |
|---|---|
| Every production Java source reviewed | `docs/quality/code-audit.md` pins every main/client Java file by line count and SHA-256 prefix. CI rejects missing package contracts, undocumented public types, wildcard imports, debug writes, unfinished markers, oversized responsibility classes, and manifest drift. |
| Every power, crystal force, spell, and suppressor participates | The canonical catalogue contains 63 actions: 27 innate, 13 crystal, 20 spell, and three amethyst actions. Unit coverage rejects missing IDs and origin drift. |
| Every possible collision accounted for | `docs/interactions/interaction-matrix.csv` contains all 2,016 unordered same-or-cross-action pairs. Every row has outcome, both-side scaling, motif, semantic sound, and mechanics. |
| Exceptional collision mechanics | Steam, eclipse reveal, star-rift projectile consumption, POWERS-summon banishment, soul-link purification, cleansing rain, finite ward fracture, grounded storm discharge, hostile pressure, and concordant healing have bounded server mechanics. Remaining exhaustive cases apply the resolver's real potency/duration/range multipliers. |
| Impressive accessible presentation | Four-beat client choreography expands compact semantic payloads into deterministic geometry. Eight original particle sprites and 13 original normalized mono Vorbis sounds provide shape and sound channels, with distance culling, reduced-motion adaptation, event deduplication, and hard budgets. |
| Rank maze affects powers and player traits | Both 28-node graphs have distinct capped perks. Focus provides a bounded 1.5× node emphasis; profiles affect damage, healing, control, range, duration, energy, cooldown, resistance, movement, wards, stealth, detection, soul/summon forces, knockback, interaction priority, and backlash. The `B`-key maze submits request-only awaken/attune actions that the server revalidates. |
| Mindscape bodies remain vulnerable | Realm travel, astral projection, dreamwalking, possession, and teleport marking retain vulnerable skin-matched physical proxies. Body damage mirrors to the real player; death/disconnect/stop and `/powers return` restore or clean up ownership. |
| Non-item assets reviewed | `docs/quality/asset-audit.md` pins all 149 in-scope assets. Generated contact sheets covered block, GUI, advancement, effect, particle, imported GUI/block/fluid, and mod-icon groups. Three stale unreferenced 9×27 HUD strips were removed. |
| UI replacement | Energy has five authored states; power slots use rune medallions and correct rank-adjusted cooldown maxima; teleport and locator screens use responsive ritual panels; advancement roots use light/dark mindscape backgrounds and milestone frames; the rank maze is interactive, narrated, keyboard reachable, and server-authoritative. |
| Recipe exception preserved | Strict resource validation and direct search both report zero crystal-recipe hits. |

## Automated gate

Command:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew clean test build
```

Result: `BUILD SUCCESSFUL`; 94 tests, zero failures, zero errors, zero skipped. The run executed compilation for common/client sources, tests, source audit, non-item asset audit, strict POWERS resource validation, interaction-document drift verification, assembly, and the complete build.

The Minecraft testing-layout validator passed. It reported one informational warning because this project deliberately uses pure JUnit plus real server/client smoke rather than MockBukkit or committed GameTest structures.

The generic standalone resource-pack validator correctly decoded the mod resources and all 13 sound references, but is not an applicable final gate: it expects a standalone `pack.mcmeta` and local copies of vanilla textures. POWERS is an embedded mod resource namespace, so its five findings were those expected false positives. The mod-aware validator and real client resource reload are authoritative.

## Dedicated-server smoke

An isolated new universe under `/tmp/powers-smoke-clean.*` ran with `--port 0`. RCON was disabled only for this smoke and the original ignored `run/server.properties` was restored byte-for-byte by a shell trap.

Observed:

- Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, POWERS 1.0.0, and Java 25 loaded.
- `Loaded rank mazes: 28 light nodes, 28 darkness nodes`.
- `Magic collision kernel loaded: 63 actions, 2016 exhaustive interactions`.
- `POWERS framework initialized with 27 power(s)`.
- New-world datapacks loaded 1,596 recipes and 1,710 advancements.
- Server reached `Done (1.609s)` on port 0.
- Overworld, Nether, End, Dark Realm, Light Realm, and Middleworld all loaded and saved.
- `stop` completed cleanly and Gradle exited 0 with `BUILD SUCCESSFUL`.

## Client resource/render smoke

The dev client reached the main menu on Apple M3 Pro/OpenGL. It loaded the same action/rank diagnostics, reloaded the `powers` resource namespace, started OpenAL, built the particle atlas and GUI/item/block atlases, and logged no missing POWERS model, texture, particle, sound, invalid animation, shader linkage, or POWERS rendering exception.

The dev account produced expected Mojang/Realms 401 messages because Fabric's offline development identity is not a Microsoft session; these were unrelated to POWERS resources or rendering. The unbundled Java game window was not exposed through the desktop automation accessibility list, so no in-game screenshot is claimed. Visual evidence instead consists of the inspected contact sheets, exact PNG checks, pure HUD/maze geometry tests, successful client compilation, and the real resource/atlas reload.

## Artifact

| File | Entries | SHA-256 |
|---|---:|---|
| `build/libs/powers-1.0.0.jar` | 1,349 | `ff4d8a3386bebc2bd0ee3465946b6497f2de83d1b1cd5f1a1e2308f7a4f97296` |
| `build/libs/powers-1.0.0-sources.jar` | 1,307 | `00c5b3f243b064349988f3d356f9a1fea8b992cb0638d324ecff41816e75e063` |

The runtime JAR contains no `.DS_Store`, `Thumbs.db`, logs, crash reports, run directory, or Java source entries.

## Visual provenance

The art direction source is `docs/assets/ancient-magic-ui-concept.png`, generated with OpenAI image generation from an original ancient-cosmic Minecraft UI prompt covering five energy states, rune medallions, ritual panels, and light/dark advancement canvases. Shipping PNGs were not cropped from the concept: `scripts/generate_ui_assets.py` recreates them deterministically as original pixel assets. `scripts/generate_magic_sounds.py` likewise synthesizes the original sound bank from deterministic signal recipes.
