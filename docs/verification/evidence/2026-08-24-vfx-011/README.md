# VFX-011 accepted exact-build evidence

This dated successor closes VFX-011 against implementation commit `44e3a7c58c6426f2cfa8f64e4cb5fabd29822279` and runtime JAR SHA-256 `0f0b70ac6840408a8be304bc27a79f52dbf92fb40d1df1b16a5e9ae1fee427d0`. The historical `2026-08-21-vfx-011` provisional bundle is preserved unchanged. VFX-009 remains separate and open; this bundle proves only the current Light Realm fallback.

## Accepted coverage

- The immutable audit inventories 970 assets, including 362 PNGs. Its 5,629 physical frame/mip tiles produce 16,887 traceable light/dark/checker rows on 90 deterministic pages. All 90 pages were explicitly inspected with `view_image`; no additional repair was indicated.
- A fresh exact-build client run emitted 971 metadata rows covering 9,034 unique capture IDs. All 971 original raw PNGs are retained content-addressably in `client-raw/`, and `client-emitted-captures.jsonl` preserves the original emitted metadata and runtime options.
- All 49 generated client contact pages were explicitly inspected as navigation aids. Fifteen unique representative full-resolution PNGs covering 20 required representative IDs were directly inspected with `view_image(detail=original)`.
- `review-decisions.tsv` contains 2,080 digest-bound decisions: 970 asset sources, 90 asset pages, 971 fresh client raw files, and 49 client pages. The generated `review-ledger.tsv` contains 27,030 evidence rows plus its header.
- Fifteen directly opened representative raw PNGs are `PASS`. The other 956 retained raw PNGs are explicitly `LIMITED`: their bytes and emitted digests are verified and their contact pages were viewed, but they were not directly opened, so no visual PASS is inferred. All 49 contact pages are likewise `LIMITED` navigation evidence.

## Visual findings

No VFX repair was indicated by this audit. Items, spawn eggs, entities, wide/slim owner-skin overlays, screens, HUD combinations, boss bars, and first/third-person gameplay representatives rendered coherently. Thin or edge-on first/third-person item views match the authored display transforms and are recorded as an observation, not automatically repaired. Screen scale/width variants remained legible; reduced-motion captures report the emitted `screenEffectScale=0.0`, `particles=MINIMAL`, and `reducedMotion=true` settings.

## Exact environment and identity

- Minecraft 26.2; Fabric Loader 0.19.3; Fabric API 0.156.0+26.2.
- OpenJDK 25.0.4 from `/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.
- Apple M3 Pro; OpenGL 4.1 Metal - 90.5.
- Physical client surfaces are 960×720 or 1280×720 for GUI scales 1–3 and 1280×960 or 1600×960 for GUI scale 4. Minecraft 26.2's `Window.calculateScale` requires at least 320×240 logical pixels, so 1280×960 is the exact minimum framebuffer for scale 4. Every scale-tagged capture ID is checked before screenshotting and its emitted requested/effective runtime values equal the nominal scale; all 68 scale-4 IDs record 4/4. The original per-capture metadata also records mip level, particle mode, screen-effect scale, motion mode, render distance, graphics mode, resource packs, game time, and weather.
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

The exact client command has a fresh full wrapper transcript and machine receipt recording `BUILD SUCCESSFUL`, exit 0, 971 unique captures, 9,034 unique capture IDs, and 971 verified raw digests. The final-review scale regression tests and focused JVM/resource gates passed. The gallery server test passed 1/1. The accepted authoritative `./test.sh gametest` execution printed `All 131 required tests passed` and `BUILD SUCCESSFUL`; its surrounding tee wrapper later hit zsh's reserved `status` variable, so that wrapper is truthfully recorded as nonzero outside Gradle. A later finalized-head literal `./gradlew check --rerun-tasks --no-daemon --console=plain` passed with exit 0: 131/131 required GameTests, 1,680/1,680 JUnit tests, 143/143 Python tests, audits/resources, and `BUILD SUCCESSFUL in 3m 4s`. Its 840-line privacy-sanitized transcript is `logs/final-review-check-final-green.log`, SHA-256 `e7b003d662316144b7dc1d46e621e00c63d9bc584fd21a301624411032090dd3`.

The two-client campaign in the preserved `2026-08-21-vfx-011` provisional bundle was captured at commit `c99ad4330c8c5ed9000e9dfb2a5cd310a7e3f581`, which is not the exact implementation commit and is not its ancestor. It remains historical only. No two-client artifact, decision, ledger row, or checksum entry is accepted by this successor.

## Retained aggregate limitation

The full GameTest harness is timing-sensitive. Minecraft 26.2 hard-codes up to 50 tests per batch; repository GameTests in those parallel batches mutate process-wide runtime/reset state. The packet-loss matrix also mixes each random mock-player UUID into its nominal fixed-seed fault decisions. Earlier runs therefore produced different failures (crystal travel/return, stale teleport, packet-loss injection, reload timeout, ritual-channel timing, or invasion timing), and one run reported server overload. Every failed log is retained. No unrelated harness change was folded into VFX-011 and no isolated test was substituted for the aggregate. The later literal finalized-head aggregate passed cleanly; the harness concurrency/probabilistic behavior remains a disclosed reliability limitation rather than an outstanding VFX-011 acceptance failure.

Logs are complete but privacy-sanitized without dropping warning/error lines: home prefixes become `<HOME>` and ephemeral loopback endpoints become `<LOOPBACK>`. `SHA256SUMS` binds every successor file plus the asset manifest, reviewed exceptions, and all 90 asset pages.
