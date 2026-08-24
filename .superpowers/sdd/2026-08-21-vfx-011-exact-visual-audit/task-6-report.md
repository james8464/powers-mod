# VFX-011 Task 6 report — 2026-08-24

## Outcome

VFX-011 is closed with a fresh exact-build successor at `docs/verification/evidence/2026-08-24-vfx-011`. The historical `2026-08-21-vfx-011` provisional bundle is preserved. VFX-009 remains present in the backlog and unchecked in the completion plan.

The exact implementation is commit `3376c8b97405e53804b12439b976e73874ff2ea0` (`test(vfx): isolate VFX-011 client capture gate`). Its runtime JAR is `powers-1.0.2.jar`, SHA-256 `0f0b70ac6840408a8be304bc27a79f52dbf92fb40d1df1b16a5e9ae1fee427d0`. The final evidence/closure commit is the commit containing this report; its exact SHA is reported in the Task 6 handoff because a commit cannot embed its own SHA.

## Implementation and TDD

The first unfiltered client aggregate produced the complete 971-row VFX-011 gallery but then failed on the unrelated open VFX-009 `LightRealmSkyClientGameTests` timeout. I did not weaken the VFX-011 capture requirements. I then added the property-bound resource filter to `build.gradle`/the GameTest descriptor, committed it before capture, and proved GREEN: the filtered descriptor contains `PowersClientGameTests` and `VfxGalleryClientGameTests` but not `LightRealmSkyClientGameTests`; the default descriptor still contains all three. Focused Light Realm contract and GameTest compilation also passed. The file named `processGametestResources-red.log` records `BUILD SUCCESSFUL`, not a failure; the reported out-of-band predicate failure and its exit status were not retained. It therefore cannot be cited as an observed RED.

Commands:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew processGametestResources -Pvfx011ClientOnly --rerun-tasks --no-daemon --console=plain
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew processGametestResources test --tests com.powers.client.visual.LightRealmSkyGalleryContractTest compileGametestJava -Pvfx011ClientOnly --rerun-tasks --no-daemon --console=plain
```

Observed retained results: the misleadingly named `processGametestResources-red.log` command exited 0; the GREEN/focused command exited 0. The original predicate failure was not retained, so no retroactive RED claim is made. No runtime production class or rendered behavior changed; the JAR hash remained identical.

The originally retained `focused-python-evidence-gates.log` contains GREEN only. It does not preserve the claimed earlier evidence-script RED and is not cited as TDD proof. Fix round 1 adds a new, genuine retained RED for the cross-commit two-client acceptance and missing terminal receipt before their implementation.

## Exact capture and packaging

The original capture log ended at Minecraft shutdown and did not retain Gradle's terminal result, so its former exit-0 claim is withdrawn. Fix round 1 reran the exact command through the committed wrapper:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home python3 -B scripts/run_vfx011_client_capture.py --transcript docs/verification/evidence/2026-08-24-vfx-011/logs/runClientGameTest-vfx011-terminal.log --receipt docs/verification/evidence/2026-08-24-vfx-011/client-command-receipt.json --implementation-commit 3376c8b97405e53804b12439b976e73874ff2ea0 --jar build/libs/powers-1.0.2.jar
```

Result: wrapper exit 0 in 4m14s; 14 Gradle tasks; retained terminal markers `BUILD SUCCESSFUL` and `VFX011_CLIENT_COMMAND_EXIT=0`; 971 metadata rows; 971 unique screenshot names; 9,034 unique capture IDs; and 971 emitted/raw digest matches. Original emitted metadata SHA-256: `db2959e19685123d0cbdbbec450e586fe523e47406a447c6b264d803308b317c`. Sanitized terminal transcript SHA-256: `d6794e99f7e34b01801611a699a5a91c49df4153659da40bfadeae14bdd1ca67`. Machine receipt SHA-256: `8df19d0522796fe059de441dacc35e8f09a5e52d07b8de98f02ddc0cf1f2bf18`.

Packaging command:

```text
python3 -B scripts/package_vfx011_evidence.py --captures build/run/clientGameTest/vfx-011-gallery/captures.jsonl --screenshots build/run/clientGameTest/screenshots --output docs/verification/evidence/2026-08-24-vfx-011 --implementation-commit 3376c8b97405e53804b12439b976e73874ff2ea0 --jar build/libs/powers-1.0.2.jar
```

Result: exit 0. The successor retains 971 content-addressed raw PNGs, all original emitted metadata, a raw index, capture index, 49 contact pages, and 15 unique full-resolution representative PNGs backing 20 required IDs. The differently built two-client campaign is excluded. Receipt SHA-256: `62e6267f0e6079bc888c51cfb47ac38cf687a8e76dfb8c34c3924c3f50dabf53`. Raw-index SHA-256: `d1cf527be7bb07097cca8cb75fde91b28c6dc12d8f35bbbbbe04b2c7e874f4c9`. Capture-index SHA-256: `0e6a01a5e0823b5fbe54c26b49184fb6e297a610d934619f0779665d0e97b75e`.

## Visual review and decisions

I explicitly opened and inspected every generated page and required full-resolution representative:

- 90/90 asset pages with `view_image` (30 checker, 30 light, 30 dark).
- 49/49 fresh client contact pages with `view_image`.
- 15/15 unique representative raw PNGs with `view_image(detail=original)`, covering all 20 representative IDs.

No VFX repair was indicated. Item and spawn-egg views were coherent; thin/edge-on first- and third-person views match authored transforms. Entity fronts and wide/slim owner-skin overlays were coherent. Screen wide/narrow and GUI-scale variants were legible. HUD energy/combination states, boss bars, and first/third-person gameplay representatives appeared correct. Reduced-motion captures emitted `reducedMotion=true`, `particles=MINIMAL`, and `screenEffectScale=0.0`.

No PASS was inferred from filenames, decoding, logs, hashes, or contact sheets. `review-decisions.tsv` has 2,080 explicit digest-bound rows: 968 asset-source PASS, two asset-source REPAIRED, 90 asset-page PASS, 15 directly opened client-raw PASS, 956 client-raw LIMITED, and 49 client-page LIMITED. The 956 raw limitations say the raw bytes were retained/digest-verified and their sheet was viewed, but the raw image was not directly opened. The 49 sheets are navigation-only. `review-decisions.tsv` SHA-256: `aefa729e436ee4545114fc156bd311561c2bff7eb7e50135ba8b7381aa1d68a1`.

The generated ledger has 27,030 rows plus its header and SHA-256 `94100e06bd706502980c36a68bc3e1db42ce89087023d080beeb6d31ff3cab59`.

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

## Fix round 1/5 — exact-build binding and retained terminal proof

Fix base: `5035276d89e9783570e7e2f56134797992211845`. The gameplay implementation remains `3376c8b97405e53804b12439b976e73874ff2ea0` and the exact JAR remains SHA-256 `0f0b70ac6840408a8be304bc27a79f52dbf92fb40d1df1b16a5e9ae1fee427d0`. This fix is the commit containing this appendix; its SHA is supplied in the handoff because it cannot truthfully embed its own identity.

### Reviewer findings and disposition

1. The copied two-client receipt named commit `c99ad4330c8c5ed9000e9dfb2a5cd310a7e3f581`, while the successor build metadata names `3376c8b97405e53804b12439b976e73874ff2ea0`. `git merge-base --is-ancestor c99ad4330c8c5ed9000e9dfb2a5cd310a7e3f581 3376c8b97405e53804b12439b976e73874ff2ea0` exited 1. I chose the explicit historical-only resolution: all 12 copied `two-client/` successor artifacts, both accepted successor decisions, both generated ledger rows, and their checksum entries were removed. The original `docs/verification/evidence/2026-08-21-vfx-011/two-client/` evidence remains untouched; `git diff --quiet 5035276d... -- docs/verification/evidence/2026-08-21-vfx-011` exited 0. The fresh ledger generator now rejects a `two-client/` directory in an exact-build bundle. Successor totals are 2,080 decisions and 27,030 ledger rows.
2. The original report's RED claims were corrected in place. `processGametestResources-red.log` proves `BUILD SUCCESSFUL`, so it is now described as a misleadingly named successful precondition command; the unretained predicate/exit is not claimed. `focused-python-evidence-gates.log` is GREEN only, so it is not cited as a preserved RED. No retroactive failure was manufactured.
3. The original client log lacked a terminal Gradle/result receipt. `scripts/run_vfx011_client_capture.py` now streams a privacy-sanitized complete command transcript and writes a machine receipt only after validating the literal command, terminal exit, `BUILD SUCCESSFUL`, implementation/JAR identity, 971 metadata rows, 971 unique screenshot names, 9,034 unique capture IDs, and all 971 raw digests. The ledger refuses a fresh bundle without that exact receipt/transcript binding.

### TDD evidence

The first retained RED command was:

```text
python3 -B -m unittest scripts.tests.test_build_vfx011_review_ledger.Vfx011EvidenceTest.test_fresh_bundle_excludes_cross_commit_two_client_acceptance scripts.tests.test_build_vfx011_review_ledger.Vfx011EvidenceTest.test_fresh_client_command_receipt_proves_terminal_success_and_exact_binding
```

It exited 1 with two assertion failures: the cross-commit successor `two-client/` directory existed and `client-command-receipt.json` did not. `logs/fix-round1-red.log` preserves the complete sanitized result, SHA-256 `651d37c3a042a544f1f4b07aa72ec694c61a35a8aeb17a22cd7e977e18862160`. The same two tests subsequently passed 2/2.

Privacy sanitization then exposed a second real RED: the sanitized transcript digest differed from the receipt. The new idempotence test exited 1 and is retained at `logs/fix-round1-sanitizer-red.log`, SHA-256 `1b60bd72c5491c4c11b3473e62192d9da3b3e993ad7ed670b6eb7373d3d5ae32`. `sanitize_vfx011_evidence.py` now refreshes the terminal transcript digest as well as the historical two-client receipt when present. The focused test passed, followed by all 19 `test_build_vfx011_review_ledger` tests.

### Fresh terminal proof and exact bytes

After verifying that `git diff --name-only 3376c8b9... -- build.gradle gradle.properties settings.gradle src/main src/client src/gametest` was empty, I rebuilt `build/libs/powers-1.0.2.jar`; it reproduced the required SHA-256. The live command was:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home python3 -B scripts/run_vfx011_client_capture.py --transcript docs/verification/evidence/2026-08-24-vfx-011/logs/runClientGameTest-vfx011-terminal.log --receipt docs/verification/evidence/2026-08-24-vfx-011/client-command-receipt.json --implementation-commit 3376c8b97405e53804b12439b976e73874ff2ea0 --jar build/libs/powers-1.0.2.jar
```

It exited 0 after the underlying literal Gradle command printed `BUILD SUCCESSFUL in 4m 14s`; the transcript ends with `VFX011_CLIENT_COMMAND_EXIT=0`. Receipt result is `PASS`, exit code 0, rows 971, unique screenshots 971, unique capture IDs 9,034, verified digests 971. Sanitized transcript SHA-256 is `d6794e99f7e34b01801611a699a5a91c49df4153659da40bfadeae14bdd1ca67`; receipt SHA-256 is `8df19d0522796fe059de441dacc35e8f09a5e52d07b8de98f02ddc0cf1f2bf18`; emitted metadata SHA-256 is `db2959e19685123d0cbdbbec450e586fe523e47406a447c6b264d803308b317c`.

The live render changed 885 of 971 raw digests relative to the first successor capture. I did not reuse the old byte-bound review. I repackaged the new run with the existing packaging command, regenerated every digest-bound client decision/ledger row, explicitly inspected all 49/49 new contact sheets with `view_image`, and directly inspected all 15/15 new unique representative raw PNGs with `view_image(detail=original)`. No new VFX defect or repair was indicated. The same honest limitations apply: 15 directly opened raws are PASS, 956 non-opened raws are LIMITED, and all 49 navigation sheets are LIMITED. No PASS comes from a filename, decoder, log, digest, or sheet alone.

The exact raw/metadata revalidation command loaded emitted JSONL and both TSV indexes, recomputed every retained PNG digest, and asserted the receipt/implementation/JAR bindings. It exited 0 with:

```text
VFX011_CAPTURE_REVALIDATION=PASS
metadata_rows=971 unique_screenshots=971
capture_ids=9034 unique_capture_ids=9034 contact_pages=49
verified_raw_digests=971 metadata_sha256=db2959e19685123d0cbdbbec450e586fe523e47406a447c6b264d803308b317c
implementation_commit=3376c8b97405e53804b12439b976e73874ff2ea0 jar_sha256=0f0b70ac6840408a8be304bc27a79f52dbf92fb40d1df1b16a5e9ae1fee427d0 terminal_exit=0
```

### Covering verification

The property-bound resource contract was rerun in both modes. Each Gradle command exited 0, and explicit JSON predicates proved that `-Pvfx011ClientOnly` contains exactly `PowersClientGameTests` plus `VfxGalleryClientGameTests`, while the default descriptor additionally contains `LightRealmSkyClientGameTests`. The complete terminal/predicate log is `logs/fix-round1-property-contract.log`, SHA-256 `c5345d5a30f2f2c5f44c9ff3a0b99f681f21ecf702556abbe9b277ea51a0cb5e`.

Final commands and results:

```text
python3 -B -m unittest scripts.tests.test_build_vfx011_review_ledger
# exit 0; 19/19

JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon --console=plain
# exit 0; 131/131 required GameTests; 1,680/1,680 JUnit; 141/141 Python; BUILD SUCCESSFUL in 3m26s

python3 -B scripts/sanitize_vfx011_evidence.py --evidence docs/verification/evidence/2026-08-24-vfx-011 --check
# exit 0; privacy scan passed for 35 non-PNG files

python3 -B scripts/build_vfx011_review_ledger.py --evidence docs/verification/evidence/2026-08-24-vfx-011 --check
# exit 0

python3 -B scripts/update_vfx011_checksums.py --evidence docs/verification/evidence/2026-08-24-vfx-011
shasum -a 256 -c docs/verification/evidence/2026-08-24-vfx-011/SHA256SUMS
# exit 0; 1,161/1,161 entries
```

The full check log is retained at `logs/fix-round1-check.log`, SHA-256 `474a5d07a2984e9a27249e7152cee2ef54628af490e0e5f2c5d550debab2a52a`; it contains `All 131 required tests passed`, `Ran 141 tests ... OK`, `BUILD SUCCESSFUL`, and wrapper exit 0. Final `SHA256SUMS` SHA-256 is `62a17eba7a5746c3b37007e15ea2077ad381cc33d09d909b8b9a80ce3a1412f5`.

Self-review: the historical provisional directory has no diff; the successor contains no two-client path/decision/ledger/checksum acceptance; the exact client receipt and transcript agree after sanitization; raw and metadata counts/digests agree independently; all regenerated visual pages/representatives were viewed; the property filter is tested in filtered and default modes; VFX-009 remains open; and nothing was pushed.

## Final-review fix wave — actual GUI scale 4

Fix base: `83b9704d853a4beea40f734ca1b99983c6487571`. Implementation-first commit: `44e3a7c58c6426f2cfa8f64e4cb5fabd29822279` (`fix(vfx): bind gallery captures to actual GUI scale`). The final evidence commit is the commit containing this appendix and is reported in the handoff because a commit cannot embed its own SHA.

### Root cause and genuine RED

The prior 1280×720 wide and 960×720 narrow surfaces could not render GUI scale 4. Minecraft 26.2 bytecode for `Window.calculateScale(int, boolean)` increments the scale only while `framebufferWidth / (scale + 1) >= 320` and `framebufferHeight / (scale + 1) >= 240`. Consequently, the minimum scale-4 framebuffer is 1280×960. Every one of the 68 nominal `scale4` capture IDs in the superseded successor metadata reported `requestedGuiScale=3` and `effectiveGuiScale=3`.

Before changing production code, I added behavior tests for the two acceptance boundaries. `test_packager_rejects_nominal_gui_scale_that_runtime_clamped` names and catches the production mutation where nominal `/scale4/` metadata reports runtime 3/3. It exercised the real packager with emitted metadata and genuinely failed because no `ValueError` was raised:

```text
python3 -B -m unittest scripts.tests.test_build_vfx011_review_ledger.Vfx011EvidenceTest.test_packager_rejects_nominal_gui_scale_that_runtime_clamped
# exit 1; retained at logs/final-review-scale-binding-red.log
```

`test_ledger_rejects_digest_bound_nominal_gui_scale_runtime_mismatch` exercised the generated-ledger acceptance path and genuinely failed for the same missing predicate:

```text
python3 -B -m unittest scripts.tests.test_build_vfx011_review_ledger.Vfx011EvidenceTest.test_ledger_rejects_digest_bound_nominal_gui_scale_runtime_mismatch
# exit 1; retained at logs/final-review-ledger-scale-binding-red.log
```

Both retained REDs fail with `ValueError not raised`; their SHA-256 digests are respectively `3b5d681b8044962be0a606deeba9de1ba933bb9b550117e7839e873953570d38` and `df391092a2d79764ae7daa122626c225ca7d22099e09d7a1eb33e90500a2f694`.

### Minimal implementation and GREEN

`VfxUiGallery` now uses the unchanged 960×720/1280×720 surfaces for scales 1–3 and 1280×960/1600×960 for scale 4. Every screenshot passes through a helper that invokes `VfxGalleryClientAgent.assertRuntimeScaleMatchesCaptureIds` immediately before `takeScreenshot`; a requested or effective runtime scale that disagrees with any scale-tagged capture ID throws and aborts capture. Both evidence scripts independently enforce the same capture-ID-to-runtime-metadata binding, so a mislabeled exact bundle cannot be packaged or accepted into the ledger.

```text
python3 -B -m unittest scripts.tests.test_build_vfx011_review_ledger.Vfx011EvidenceTest.test_packager_rejects_nominal_gui_scale_that_runtime_clamped scripts.tests.test_build_vfx011_review_ledger.Vfx011EvidenceTest.test_ledger_rejects_digest_bound_nominal_gui_scale_runtime_mismatch
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew compileGameTestJava --rerun-tasks --no-daemon --console=plain
# exit 0; 2/2 Python tests plus GameTest compilation; logs/final-review-scale-binding-green.log
```

The GREEN log SHA-256 is `0afab0c57755a30325e179476837625ab7456054b5c015f47b129297fb7b7336`. I committed this implementation before capture as `44e3a7c58c6426f2cfa8f64e4cb5fabd29822279`. The exact rebuilt `build/libs/powers-1.0.2.jar` SHA-256 is `0f0b70ac6840408a8be304bc27a79f52dbf92fb40d1df1b16a5e9ae1fee427d0`.

### Fresh exact capture, package, and visual review

The fresh command was:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home python3 -B scripts/run_vfx011_client_capture.py --transcript docs/verification/evidence/2026-08-24-vfx-011/logs/runClientGameTest-vfx011-terminal.log --receipt docs/verification/evidence/2026-08-24-vfx-011/client-command-receipt.json --implementation-commit 44e3a7c58c6426f2cfa8f64e4cb5fabd29822279 --jar build/libs/powers-1.0.2.jar
```

It exited 0 after `BUILD SUCCESSFUL in 4m 8s`. The machine receipt binds the implementation SHA/JAR and proves 971 metadata rows, 971 unique screenshots, 9,034 unique capture IDs, and 971 verified raw digests. Original emitted metadata SHA-256 is `2513efffaab8642ea5acbca147d196003279363aee2349e11b08c93e268d9425`. Packaging exited 0.

Runtime-scale revalidation, retained with the authoritative bytecode excerpt in `logs/final-review-scale-runtime-evidence.log`, exited 0 and found:

```text
scale 1: 34 IDs at 960x720; 34 at 1280x720; all 1/1
scale 2: 34 IDs at 960x720; 36 at 1280x720; all 2/2
scale 3: 34 IDs at 960x720; 34 at 1280x720; all 3/3
scale 4: 34 IDs at 1280x960; 34 at 1600x960; all 4/4
```

I refreshed every changed digest-bound decision (1,005 rows changed), then regenerated the 2,080-decision review file and 27,030-row ledger. I explicitly inspected all 49/49 regenerated contact pages with `view_image` and all 15/15 unique required full-resolution representative raw PNGs with `view_image(detail=original)`, including the scale-4 Artifact Catalogue and Grimoire surfaces. They remained legible and coherent; no new visual repair was indicated. Thin edge-on item views remain the already-recorded authored-transform observation. Old byte-bound visual verdicts were not reused.

### Verification and aggregate limitation

Focused JVM/resource tests, the property-bound GameTest resource contract in filtered and default modes, and the gallery server GameTest all exited 0; the gallery test passed 1/1. The final evidence suite passed 21/21. The exact successor contains 971 retained raw PNGs, 971 emitted metadata rows, 9,034 capture-index rows, 49 contact sheets, 2,080 decisions, and 27,030 ledger rows.

The mandated literal full command was run repeatedly with the mandated Java home and without weakened filters:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon --console=plain
```

- Attempt 1 exited 1 after unrelated realm-crystal and invasion GameTests failed.
- After the repository-authoritative clean entrypoint, attempt 2 exited 1 after an unrelated Augury tick-0 GameTest failed.
- Attempt 3 passed all 131 required GameTests, all 1,680 JUnit tests, then exited 1 at two in-progress evidence assertions: the newly retained failure logs were not checksum-bound and the live receipt test still pinned the predecessor implementation SHA. Those exact failures were corrected; the evidence suite then passed 21/21.
- The post-finalization attempt exited 1 after unrelated packet-fault payment and hostile-environment reload/return GameTests failed; the server reported it was 597 ticks behind.

All four complete transcripts and terminal exits are retained as `logs/final-review-check-attempt1-failed.log` through `logs/final-review-check-attempt4-failed.log`. This matches the already diagnosed hard-coded batch concurrency, process-global reset/state, timing, and probabilistic-seed limitation; no unrelated harness repair was folded into VFX-011. A diagnostic aggregate excluding only `runGameTest` then passed exit 0 with 1,680/1,680 JUnit tests, 143/143 Python tests, resource/audit gates, and `BUILD SUCCESSFUL in 1m 42s`; it is not represented as the required full aggregate. The subsequently retained finalized-head literal aggregate passed cleanly as documented in the closure reconciliation below.

### Final integrity and self-review

Privacy sanitization, ledger validation, exact raw/metadata digest revalidation, and checksum verification were rerun after all evidence bytes changed. The privacy check passed over 50 owned non-PNG files. Revalidation proved 971 metadata rows, 971 raw-index rows, 971 recomputed raw digests, 9,034 capture-index rows, and actual nominal scale counts `1:68,2:70,3:68,4:68`; metadata SHA-256 remained `2513efffaab8642ea5acbca147d196003279363aee2349e11b08c93e268d9425`. `shasum -a 256 -c` verified 1,176/1,176 entries; final `SHA256SUMS` SHA-256 is `f57ff59e2f139e9adf5627a6a7e07b9b670f272d0b6fa87992e33044292efa77`. The historical `docs/verification/evidence/2026-08-21-vfx-011` directory has no diff from the fix base. VFX-009 remains open; VFX-011 alone remains closed. No visual PASS was inferred from a filename, decoder, log, digest, or contact sheet. All genuine RED/GREEN and aggregate-failure logs are retained. No push or merge was performed.

The earlier missing-clean-exit concern was resolved by the finalized-head literal aggregate documented below. The known unrelated concurrent GameTest harness behavior remains an honest reliability limitation, with every earlier failed transcript retained.

## Closure reconciliation — finalized-head literal aggregate

At finalized evidence head `936f0c8f06aaa6a979bd4951ba83f289b1644da9`, the exact required command was run without a filter or excluded task:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon --console=plain
```

It exited 0 and recorded `All 131 required tests passed`, 1,680/1,680 JUnit tests, `Ran 143 tests ... OK` for Python, all audits/resources, `BUILD SUCCESSFUL in 3m 4s`, and terminal marker `VFX011_HEARTBEAT_CHECK_EXIT=0`. The exact 840-line transcript source was retained as the privacy-sanitized `docs/verification/evidence/2026-08-24-vfx-011/logs/final-review-check-final-green.log`; its post-sanitization SHA-256 is `e7b003d662316144b7dc1d46e621e00c63d9bc584fd21a301624411032090dd3`. Sanitization replaced local home prefixes but dropped no lines or warnings.

This clean literal aggregate resolves the sole final-review acceptance concern. The four earlier failed literal attempts remain retained and checksum-bound, and the hard-coded batch concurrency, global reset/state interaction, timing sensitivity, and probabilistic UUID/seed behavior remain disclosed as harness reliability limitations. No prior failure was relabelled or removed.

Closure wording was reconciled in the successor README and completion plan. The top-level README and CHANGELOG already accurately stated that literal aggregates pass and required no byte change. VFX-009 remains open, the historical `2026-08-21-vfx-011` evidence remains unchanged, and neither production/runtime code nor decisions/raw visual evidence changed in this reconciliation.

Final reconciliation gates all exited 0: privacy scan passed for 51 owned non-PNG files; ledger `--check` passed; `python3 -B -m unittest scripts.tests.test_build_vfx011_review_ledger` passed 21/21; and `shasum -a 256 -c` verified 1,177/1,177 entries. Final `SHA256SUMS` SHA-256 is `9ea3260167cb3f1adb05b08dd37ab8b5cbf85290f305e9da4c522ad1d18e2c93`. `git diff --check` exited 0, and comparison against `936f0c8f...` proved the historical evidence directory unchanged. The root agent will rerun the full aggregate on the reconciliation commit; this narrow wave did not rerun it or alter runtime code.
