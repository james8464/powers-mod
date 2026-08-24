# VFX-011 accepted exact-build evidence

This dated successor closes VFX-011 against implementation commit `3376c8b97405e53804b12439b976e73874ff2ea0` and runtime JAR SHA-256 `0f0b70ac6840408a8be304bc27a79f52dbf92fb40d1df1b16a5e9ae1fee427d0`. The historical `2026-08-21-vfx-011` provisional bundle is preserved unchanged. VFX-009 remains separate and open; this bundle proves only the current Light Realm fallback.

## Accepted coverage

- The immutable audit inventories 970 assets, including 362 PNGs. Its 5,629 physical frame/mip tiles produce 16,887 traceable light/dark/checker rows on 90 deterministic pages. All 90 pages were explicitly inspected with `view_image`; no additional repair was indicated.
- A fresh exact-build client run emitted 971 metadata rows covering 9,034 unique capture IDs. All 971 original raw PNGs are retained content-addressably in `client-raw/`, and `client-emitted-captures.jsonl` preserves the original emitted metadata and runtime options.
- All 49 generated client contact pages were explicitly inspected as navigation aids. Fifteen unique representative full-resolution PNGs covering 20 required representative IDs were directly inspected with `view_image(detail=original)`.
- `review-decisions.tsv` contains 2,082 digest-bound decisions: 970 asset sources, 90 asset pages, 971 fresh client raw files, 49 client pages, and two accepted two-client captures. The generated `review-ledger.tsv` contains 27,032 evidence rows plus its header.
- Fifteen directly opened representative raw PNGs are `PASS`. The other 956 retained raw PNGs are explicitly `LIMITED`: their bytes and emitted digests are verified and their contact pages were viewed, but they were not directly opened, so no visual PASS is inferred. All 49 contact pages are likewise `LIMITED` navigation evidence.

## Visual findings

No VFX repair was indicated by this audit. Items, spawn eggs, entities, wide/slim owner-skin overlays, screens, HUD combinations, boss bars, and first/third-person gameplay representatives rendered coherently. Thin or edge-on first/third-person item views match the authored display transforms and are recorded as an observation, not automatically repaired. Screen scale/width variants remained legible; reduced-motion captures report the emitted `screenEffectScale=0.0`, `particles=MINIMAL`, and `reducedMotion=true` settings.

## Exact environment and identity

- Minecraft 26.2; Fabric Loader 0.19.3; Fabric API 0.156.0+26.2.
- OpenJDK 25.0.4 from `/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.
- Apple M3 Pro; OpenGL 4.1 Metal - 90.5.
- Physical client surface 1280×720. The original per-capture metadata records requested/effective GUI scale, mip level, particle mode, screen-effect scale, motion mode, render distance, graphics mode, resource packs, game time, and weather.
- The client run loaded 53 mods. The exact active resource-pack list is repeated in every emitted metadata row; it consists of vanilla, POWERS, Fabric Loader/API modules, and no external user resource pack.

## Commands and outcomes

All Gradle commands used the Java home above.

```text
./gradlew runClientGameTest -Pvfx011ClientOnly --rerun-tasks --no-daemon --console=plain
./gradlew test --tests 'com.powers.client.visual.*' verifyVfxAssetAudit verifyVisualGoldens validatePowerResources compileGametestJava --rerun-tasks --no-daemon --console=plain
python3 -B -m unittest scripts.tests.test_audit_non_item_assets scripts.tests.test_build_vfx011_review_ledger
./gradlew runGameTest -PgameTestFilter='powers-gametest:vfx_gallery_game_tests_publishes_every_renderer_family' --rerun-tasks --no-daemon --console=plain
./test.sh gametest
./gradlew check --rerun-tasks --no-daemon --console=plain
```

The exact client run exited 0 with 971/971 captures. Focused JVM/resource gates and 18 Python tests passed. The gallery server test passed 1/1. The accepted authoritative `./test.sh gametest` execution printed `All 131 required tests passed` and `BUILD SUCCESSFUL`; its surrounding tee wrapper later hit zsh's reserved `status` variable, so that wrapper is truthfully recorded as nonzero outside Gradle. A later literal `check` run exited 0 with all 131 required GameTests, 1,680 JUnit tests, resource/audit gates, and `BUILD SUCCESSFUL`.

## Retained aggregate limitation

The full GameTest harness is timing-sensitive. Minecraft 26.2 hard-codes up to 50 tests per batch; repository GameTests in those parallel batches mutate process-wide runtime/reset state. The packet-loss matrix also mixes each random mock-player UUID into its nominal fixed-seed fault decisions. Failed runs therefore produced different failures (crystal travel/return, stale teleport, packet-loss injection, reload timeout, or invasion timing), and one run reported server overload. Every failed log is retained. No unrelated harness change was folded into VFX-011, no isolated test was substituted for the aggregate, and closure relies on the accepted 131/131 authoritative execution plus the independent literal check exit 0.

Logs are complete but privacy-sanitized without dropping warning/error lines: home prefixes become `<HOME>` and ephemeral loopback endpoints become `<LOOPBACK>`. `SHA256SUMS` binds every successor file plus the asset manifest, reviewed exceptions, and all 90 asset pages.
