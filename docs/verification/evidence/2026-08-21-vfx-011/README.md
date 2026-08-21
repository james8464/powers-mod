# VFX-011 provisional exact-build evidence

This bundle proves the implemented VFX-011 asset and renderer audit on the working tree above base commit `c99ad4330c8c5ed9000e9dfb2a5cd310a7e3f581`. It is deliberately **provisional**: VFX-011 remains open until one post-implementation literal aggregate `check` can run after the protected QA-006 24-hour soak releases the host. The existing enhanced Light Realm renderer remains separate, open VFX-009 work; this bundle claims only the current static fallback.

## Accepted evidence

- The immutable asset manifest inventories 970 assets, including all 362 PNGs. Every physical frame has a premultiplied-alpha mip chain through 1×1. The 5,629 unique frame/mip tiles appear on light, dark, and checker backgrounds as 16,887 traceable rows across 90 owned pages. Sixty-six transparent-RGB cases use explicit path-and-source-SHA review exceptions; decode success is never described as renderer PASS.
- The Java 25 production client gallery completed 971 exact screenshots. It covers baked item contexts, spawn eggs, entity poses/UVs, Shadow/Echo wide and slim overlays, GUI 1–4 at physical 1280×720 and 960×720, normal/reduced motion, HUD half units and vanilla combinations, boss states, and first-/third-person gameplay. `captures.jsonl` is the accepted 971-row metadata file (SHA-256 `d80fbd866a7b99312dba938cf6a6d9cfe86fd902af70303e8d6d2e0a24eb82f6`); `client-capture-index.tsv` maps its 9,034 exact capture IDs to 971 screenshots and 49 bounded pages.
- Twenty representative capture IDs map to 15 unique full-resolution PNGs because several IDs intentionally share one item/entity sheet.
- A separate Java 25 dedicated-server campaign used two real Fabric clients. The accepted full-resolution locator frame contains the real `VfxObserver` player and the accepted advancement frame visibly selects the Darkness root. Exact joins, leaves, command/state markers, options, logs, and hashes are in `two-client/`.
- `review-decisions.tsv` contains 2,082 explicit digest-bound decisions: 970 source assets, 90 asset pages, 971 production screenshots, 49 client pages, and two real-client captures. The generator cannot infer PASS or REPAIRED from a filename; missing, extra, blank, or stale decisions fail. `review-ledger.tsv` joins those decisions into 27,032 source/page/tile/capture rows.

## Repairs represented by accepted replacements

The reviewed set includes corrected `vaelith` and `void_oculus` item transforms, dedicated item/entity label bands, catalogue title/summary and global-search tab state, clean two-client screenshots without tutorial/chat overlays, and authoritative HUD reset/readiness. The final HUD replacements retain hearts/hunger in reduced realm and energy views, clear retained dismount overlays before reduced captures, and present a clean Spectator frame. Superseded raw captures are not included in this bundle.

## Exact environment and commands

Runtime: Minecraft 26.2; Fabric Loader 0.19.3; Fabric API 0.156.0+26.2; OpenJDK 25.0.4; Apple M3 Pro; OpenGL 4.1 Metal backend. The integrated gallery used only the repository/default resource set and the mods enumerated in its retained log. Every capture's physical window, effective GUI scale, mip, reduced-motion state, camera, background, time, weather, capture IDs, and source keys are retained in `captures.jsonl`. `integrated-options.txt` binds the requested option matrix to the exact test-agent source SHA; Minecraft clamps requested GUI scale four to effective scale three at 720px, and both values are recorded truthfully.

Retained logs are complete but privacy-sanitized: user-home prefixes are represented by `<HOME>` and loopback endpoints by `<LOOPBACK>`. The sanitizer changes no line count and the evidence tests reject raw home paths, raw ephemeral loopback ports, stale two-client receipt hashes, or missing metadata/options.

Commands were run with `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`:

```text
./gradlew runClientGameTest --rerun-tasks --no-daemon
./gradlew test --tests 'com.powers.client.visual.*' verifyVfxAssetAudit verifyVisualGoldens validatePowerResources compileGametestJava --rerun-tasks --no-daemon
python3 -m unittest scripts.tests.test_audit_non_item_assets scripts.tests.test_build_vfx011_review_ledger
./gradlew runGameTest --rerun-tasks --no-daemon
./gradlew check -x runGameTest --rerun-tasks --no-daemon
./gradlew jar --no-daemon
python3 scripts/vfx011_two_client_gallery.py
```

Results: combined client gallery GREEN in 3m48; focused visual/resource gates GREEN; Python evidence gates GREEN; ordinary full server suite GREEN 128/128; non-live aggregate GREEN with 1,608 JUnit tests and 50 Python tests; JAR SHA-256 `9d9e75437f35c3500b9e54d2f268888715020606ab1b0540e1eae8755744bb70`.

## Open acceptance limitation

Two later aggregate attempts ran concurrently with the protected QA-006 server/client soak and each exposed a different already-green load-sensitive GameTest. The living-force invasion and realm-crystal cohort tests both passed immediately and unchanged in isolation. No unrelated fixture deadline or production behavior was relaxed. A third aggregate was stopped during preflight when the active QA-006 processes were identified. VFX-011 therefore remains open pending one literal post-soak aggregate; the ordinary 128/128 GREEN, both isolated confirmations, and the non-live aggregate are retained without being misrepresented as that final gate.

`SHA256SUMS` binds the durable bundle, including the accepted metadata, exact options, sanitized logs, and refreshed two-client receipt. `build-metadata.json` records the pre-commit source identity; a successor must replace it with the provisional implementation commit and final post-soak acceptance commit when that gate runs.
