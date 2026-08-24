# VFX-011 Task 6 report — 2026-08-24

## Outcome

VFX-011 is closed with a fresh exact-build successor at `docs/verification/evidence/2026-08-24-vfx-011`. The historical `2026-08-21-vfx-011` provisional bundle is preserved. VFX-009 remains present in the backlog and unchecked in the completion plan.

The exact implementation is commit `3376c8b97405e53804b12439b976e73874ff2ea0` (`test(vfx): isolate VFX-011 client capture gate`). Its runtime JAR is `powers-1.0.2.jar`, SHA-256 `0f0b70ac6840408a8be304bc27a79f52dbf92fb40d1df1b16a5e9ae1fee427d0`. The final evidence/closure commit is the commit containing this report; its exact SHA is reported in the Task 6 handoff because a commit cannot embed its own SHA.

## Implementation and TDD

The first unfiltered client aggregate produced the complete 971-row VFX-011 gallery but then failed on the unrelated open VFX-009 `LightRealmSkyClientGameTests` timeout. I did not weaken the VFX-011 capture requirements. A temporary contract command proved RED: processing GameTest resources with `-Pvfx011ClientOnly` still retained the Light Realm client entrypoint (exit 1). I then added the property-bound resource filter to `build.gradle`/the GameTest descriptor, committed it before capture, and proved GREEN: the filtered descriptor contains `PowersClientGameTests` and `VfxGalleryClientGameTests` but not `LightRealmSkyClientGameTests`; the default descriptor still contains all three. Focused Light Realm contract and GameTest compilation also passed.

Commands:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew processGametestResources -Pvfx011ClientOnly --rerun-tasks --no-daemon --console=plain
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew processGametestResources test --tests com.powers.client.visual.LightRealmSkyGalleryContractTest compileGametestJava -Pvfx011ClientOnly --rerun-tasks --no-daemon --console=plain
```

Observed results: RED exit 1; GREEN/focused exit 0. Logs: `processGametestResources-red.log`, `implementation-green.log`. No runtime production class or rendered behavior changed; the JAR hash remained identical.

The evidence scripts also followed RED/GREEN. New fresh-bundle tests initially failed because the successor decisions/checksums did not exist, then passed after the ledger accepted a selectable evidence directory, verified every retained raw PNG against the original emitted digest, emitted fresh `client_capture` rows, and the sanitizer/checksummer accepted a selectable evidence directory. A final strengthened checksum test first failed on unbound newly retained logs/README/metadata, then passed after the final manifest was regenerated.

## Exact capture and packaging

The exact capture command was:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runClientGameTest -Pvfx011ClientOnly --rerun-tasks --no-daemon --console=plain
```

Result: exit 0 in 4m03s; 14 Gradle tasks; 971 metadata rows; 971 unique screenshot names; 9,034 unique capture IDs; zero missing screenshots; zero emitted/raw digest mismatches. Original emitted metadata SHA-256: `9f1573526005a5d0d8e7f979a415aab772e99f6add61f73fa97a067f883e2f7a`. Sanitized retained client log SHA-256: `bd1ce57d64f71d30da08debb912fb949d1cc00537c32220ab05576636b7d4675`.

Packaging command:

```text
python3 -B scripts/package_vfx011_evidence.py --captures build/run/clientGameTest/vfx-011-gallery/captures.jsonl --screenshots build/run/clientGameTest/screenshots --output docs/verification/evidence/2026-08-24-vfx-011 --implementation-commit 3376c8b97405e53804b12439b976e73874ff2ea0 --jar build/libs/powers-1.0.2.jar
```

Result: exit 0. The successor retains 971 content-addressed raw PNGs, all original emitted metadata, a raw index, capture index, 49 contact pages, 15 unique full-resolution representative PNGs backing 20 required IDs, and the already accepted sanitized two-client proof. Receipt SHA-256: `93ef57339596fe1497bd0ad6b5d63f79475133370b2abe05c1318a7b2af84259`. Raw-index SHA-256: `157c0e1983a788c1ece565ef760380d5989edde125fe13edb2acf456a502199a`. Capture-index SHA-256: `90089e7fa97c019dd0e6ddcf4cd8f9574a2bcd62f07cc116a63e2cc13f28b9a4`.

## Visual review and decisions

I explicitly opened and inspected every generated page and required full-resolution representative:

- 90/90 asset pages with `view_image` (30 checker, 30 light, 30 dark).
- 49/49 fresh client contact pages with `view_image`.
- 15/15 unique representative raw PNGs with `view_image(detail=original)`, covering all 20 representative IDs.

No VFX repair was indicated. Item and spawn-egg views were coherent; thin/edge-on first- and third-person views match authored transforms. Entity fronts and wide/slim owner-skin overlays were coherent. Screen wide/narrow and GUI-scale variants were legible. HUD energy/combination states, boss bars, and first/third-person gameplay representatives appeared correct. Reduced-motion captures emitted `reducedMotion=true`, `particles=MINIMAL`, and `screenEffectScale=0.0`.

No PASS was inferred from filenames, decoding, logs, hashes, or contact sheets. `review-decisions.tsv` has 2,082 explicit digest-bound rows: 968 asset-source PASS, two asset-source REPAIRED, 90 asset-page PASS, 15 directly opened client-raw PASS, 956 client-raw LIMITED, 49 client-page LIMITED, and two accepted two-client REPAIRED decisions. The 956 raw limitations say the raw bytes were retained/digest-verified and their sheet was viewed, but the raw image was not directly opened. The 49 sheets are navigation-only. `review-decisions.tsv` SHA-256: `8585d338df28f18ecd7040bb82ea0181804275442da24384cb02a59515e4b871`.

The generated ledger has 27,032 rows plus its header and SHA-256 `4f1ed956bbeb933d2481e0c38793ebf05329619334c8dcdf4a2f3b2613489a94`.

## Environment

- Minecraft 26.2; Fabric Loader 0.19.3; Fabric API 0.156.0+26.2.
- OpenJDK 25.0.4 at the mandated Java home.
- Apple M3 Pro; OpenGL 4.1 Metal - 90.5.
- 1280×720 physical surface; requested/effective GUI scales, mip 0–4, normal/reduced mode, particle/screen-effect settings, render distance, graphics mode, weather, game time, and the full active resource-pack list are retained per capture.
- 53 loaded mods. Emitted resource packs contain vanilla, POWERS, Fabric Loader/API modules, and no external user pack.

## Verification commands and exact outcomes

```text
JAVA_HOME=... ./gradlew test --tests 'com.powers.client.visual.*' verifyVfxAssetAudit verifyVisualGoldens validatePowerResources compileGametestJava --rerun-tasks --no-daemon --console=plain
python3 -B -m unittest scripts.tests.test_audit_non_item_assets scripts.tests.test_build_vfx011_review_ledger
JAVA_HOME=... ./gradlew runGameTest -PgameTestFilter='powers-gametest:vfx_gallery_game_tests_publishes_every_renderer_family' --rerun-tasks --no-daemon --console=plain
JAVA_HOME=... ./test.sh gametest
JAVA_HOME=... ./gradlew check --rerun-tasks --no-daemon --console=plain
python3 -B scripts/sanitize_vfx011_evidence.py --evidence docs/verification/evidence/2026-08-24-vfx-011 --check
python3 -B scripts/build_vfx011_review_ledger.py --evidence docs/verification/evidence/2026-08-24-vfx-011 --check
python3 -B scripts/update_vfx011_checksums.py --evidence docs/verification/evidence/2026-08-24-vfx-011
shasum -a 256 -c docs/verification/evidence/2026-08-24-vfx-011/SHA256SUMS
git diff --cached --check
```

Outcomes:

- Focused JVM/resource gates: exit 0; resource validation, visual goldens, VFX asset audit, client visual tests, and GameTest compilation passed.
- Focused Python evidence gates: exit 0; 18/18 tests passed.
- Gallery server GameTest: exit 0; 1/1 passed.
- Accepted authoritative full GameTest: underlying `./test.sh gametest` completed 131/131 required tests and printed `BUILD SUCCESSFUL` in 1m36s. The surrounding tee wrapper then attempted to assign zsh's reserved variable `status`, making only the outer wrapper exit nonzero after Gradle had exited successfully. The retained log records both facts.
- Final literal aggregate on the completed source/docs state: exit 0; 131/131 required GameTests, 1,680/1,680 JUnit tests, 138/138 Python-script tests, audits/resources, and `BUILD SUCCESSFUL` in 2m59s. Final green log SHA-256: `e69254071eb9b454d69c72ec181fc8a442de37f56f55e65f5239f52dbc5fcd37`.
- Privacy: exit 0; 38 non-PNG evidence files scanned after sanitizing 22 owned logs; no home path or ephemeral loopback endpoint remained.
- Ledger check: exit 0.
- Checksums: exit 0; 1,166/1,166 entries verified. Final `SHA256SUMS` SHA-256: `603ace3b1914b893870e4ce4fa3e2296576bd6d700a1a88591a3e61602cb216a`.

## GameTest instability investigation and ruling

Before the accepted run, literal full GameTest attempts failed with different unrelated tests: Light Crystal tick-0 activation rejection; stale teleport state/energy change; realm-crystal mob move/return-cohort failures; packet loss not injected; packet-fault authoritative payment timing; resource-reload timeout; and invasion timing. One clean-reset run reported server overload. All failed logs are retained; none was relabelled or discarded.

Bytecode inspection established that Minecraft 26.2 hard-codes batches of 50 and runs tests inside each batch concurrently. Repository tests in those batches mutate process-wide managers through `clearAll`/`clearGlobal`, while the fault matrix also mixes a random mock-player UUID into its nominal fixed-seed loss decision. This explains the changing state/reset/timing failures and probabilistic loss assertion. Isolated diagnostics were used only to investigate, never as an aggregate substitute. No unrelated harness repair or weakened filter was folded into VFX-011.

The root-agent ruling accepted the retained authoritative 131/131 `./test.sh gametest` execution as the required aggregate because its log proves `All 131 required tests passed` and `BUILD SUCCESSFUL`; it required the later reserved-variable wrapper error to be recorded truthfully. The final independent literal `check` also exited 0 with 131/131. Closure proceeded only after those gates plus ledger, checksum, and privacy validation were green.

## Closure and self-review

Only VFX-011 was removed from `docs/planning/IMPROVEMENT_BACKLOG.md` and checked in the completion plan. VFX-009 remains open in both. README and CHANGELOG point to the dated successor and explicitly preserve limitations and the historical bundle.

Self-review checks:

- Historical provisional evidence was not modified or cited as fresh raw proof.
- Every one of the 971 raw PNGs and original emitted metadata rows is retained and hash-bound.
- Decision keys/digests are exact; no blank, pending, filename-derived, or contact-sheet-derived PASS exists.
- Every generated contact page and every required representative raw file was explicitly viewed.
- All warning/error lines remain in retained logs after line-count-preserving sanitization.
- Implementation capture is bound to the implementation-first commit/JAR.
- Failed aggregate logs and the concurrency/probabilistic limitation remain visible.
- VFX-009 is still open.
- No push was performed.
