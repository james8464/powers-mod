# VFX-011 provisional exact-build evidence

This bundle proves the immutable asset audit and two-client proofs on the working tree above base commit `c99ad4330c8c5ed9000e9dfb2a5cd310a7e3f581`. VFX-011 is **pending fresh raw capture**: the prior 971 screenshot files and original client-emitted metadata were not retained, so their historical contact pages are navigation aids, not visual acceptance evidence. A fresh post-soak gallery and one literal aggregate `check` are required. The enhanced Light Realm renderer remains separate, open VFX-009 work.

## Accepted evidence and historical navigation

- The immutable asset manifest inventories 970 assets, including all 362 PNGs. Every physical frame has a premultiplied-alpha mip chain through 1×1. The 5,629 unique frame/mip tiles appear on light, dark, and checker backgrounds as 16,887 traceable rows across 90 owned pages. Sixty-six transparent-RGB cases use explicit path-and-source-SHA review exceptions; decode success is never described as renderer PASS.
- The historical Java 25 gallery index maps 9,034 capture IDs to 971 screenshot names and 49 contact pages. Those pages and the 20 representative IDs mapped to 15 retained PNGs remain useful navigation, but cannot substitute for the missing 971 raw files or prove their bytes/settings.
- A separate Java 25 dedicated-server campaign used two real Fabric clients. The accepted full-resolution locator frame contains the real `VfxObserver` player and the accepted advancement frame visibly selects the Darkness root. Exact joins, leaves, command/state markers, options, logs, and hashes are in `two-client/`.
- `review-decisions.tsv` retains accepted asset/two-client decisions. All 971 historical client digests are explicitly `PENDING_RAW_RECAPTURE`; all 49 client pages are `LIMITED` navigation-only. The ledger generator rejects any PASS/REPAIRED claim for those historical rows.

## Historical repairs awaiting raw recapture

The historical pages show corrected `vaelith` and `void_oculus` transforms, label bands, catalogue layout/tab state, and HUD reset/readiness. These observations must be repeated against retained raw bytes before acceptance. The separate two-client screenshots remain retained and accepted.

## Exact environment and commands

The historical run used Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, OpenJDK 25.0.4, Apple M3 Pro, and OpenGL 4.1 Metal. Its reconstructed metadata/options were removed because they are not an acceptable substitute for original client-emitted values. The repaired gallery agent now emits each raw screenshot SHA and actual runtime options; the packager refuses missing fields, rehashes and retains every raw PNG content-addressably, and binds original metadata, implementation SHA, and JAR SHA in a receipt.

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

## Open acceptance limitations

The historical 971 raw screenshots and original emitted metadata no longer exist on disk. Do not infer their review from contact sheets, filenames, or the historical index. After QA-006 releases the host, rerun the exact gallery, package all raw bytes and emitted metadata, conduct explicit digest-bound review, then rerun the literal aggregate. Earlier ordinary 128/128 GREEN, isolated confirmations, and non-live aggregate remain historical test evidence only.

`SHA256SUMS` binds the current bounded bundle, including historical navigation pages and accepted sanitized two-client proof. `build-metadata.json` declares the missing inputs. A future acceptance successor must retain and checksum every fresh raw screenshot plus the original client-emitted metadata and exact implementation/JAR identity.
