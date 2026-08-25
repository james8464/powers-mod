# VFX-009 final-review recovery/fix report

## Result and immutable bindings

Status: **DONE**.

The final implementation/evidence revision is `e78bf8f01fc79d6e05838c083685d348c214502c`. It contains implementation commit `e6da63bd00b3af656272b9d10e0ac946548d5a2f`, both accepted live galleries, truthful rejected attempts, and retained `mod/powers-1.0.2.jar` SHA-256 `80147beb0793e37bc19d49133d11bde57b4c9c07dfff5b80c32bce25ce15d074`. This later documentation/report commit records those already-immutable identities without a recursive self-binding.

QA-001 remains open. QA-006 and VFX-011 evidence was not modified.

## Findings closed

### Visible upward silhouettes

The client fixture now sets the player pitch to `-55.0` degrees and waits until the main camera observes it within one degree. Every accepted raw 1280x720 PNG was opened at original resolution before acceptance: nine isolated Fabric frames and nine pinned-Sodium frames.

| Lane | Rows | Original-resolution result |
| --- | ---: | --- |
| Fabric | 9 | Normal rows visibly show the large warm-white/gold radial rune/halo; reduced rows visibly retain two large static, lower-contrast silhouettes; fallback positively shows no enhanced silhouette. |
| Sodium 0.9.1+mc26.2 | 9 | The same normal/reduced/fallback distinctions remain visible, including distance 4/12/24, rain-command, reload, and fallback rows. |

`manual-review.tsv` binds 18 positive decisions to the client-emitted PNG digests. Each decision covers dominant upward field, mode-specific silhouette, tonal separation or fallback absence, and no black void, celestial body, seam, clipping, excessive glare, or HUD readability loss. Incidental POWERS particles in some normal frames do not obscure the judged upper-field silhouette.

The rain command succeeded, but Light Realm has no skylight and the client observed `weather=clear`. Both facts are retained; rendered rain is not claimed. Rejected attempts 01–06 remain checksum-bound and truthfully identify direct-weather timeout, observed-clear diagnosis, fallback wait, and three clipping/draw experiments.

### Angular-velocity runtime contract

The recovered renderer path reconstructed time from an already time-derived rotation and added another dynamic phase, so `angularVelocity` was either dead or double-counted. The focused runtime assertion was first changed to require that one elapsed world tick changes each effective angle by exactly its layer velocity; it failed before the production correction and passed afterward.

The fixed profile carries finite `animationTimeTicks`, and computes one angle as `phase + angularVelocity * animationTimeTicks`, reduced modulo one revolution. The renderer consumes that single effective angle for rotation and pulse, so camera orientation does not drive animation and time is not applied twice. Normal mode retains bounded slow drift (absolute velocity no greater than `0.001` radians/tick). Reduced motion enforces zero animation time, angular velocity, pulse, and phase at construction time; runtime coverage asserts an effective angle of zero.

### Sodium artifact custody

The pinned third-party artifact remains only in the immutable external compatibility cache. It was never committed in this replacement history:

- file: `sodium-fabric-0.9.1+mc26.2.jar`
- version: `0.9.1+mc26.2`
- size: `1,834,384` bytes
- SHA-256: `de406c7a0ca5e748dfbe44740278400882a44e3109e2584b243ec02d4003344b`
- source: `https://cdn.modrinth.com/data/AANobbMI/versions/2Yom1N68/sodium-fabric-0.9.1%2Bmc26.2.jar`

Evidence retains `artifact.json`, the expected-digest receipt, the loaded-mod/runtime logs, generated Sodium configuration, and exact client options. A path scan found no Sodium JAR in the worktree or any commit after base `22755ad9`. A content scan hashed all 132 blobs reachable from the two new commits and found no blob matching the pinned Sodium digest. The scan is repeated after this successor commit during final verification.

## Commands and results

All Gradle invocations used Java 25 from `/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.

```text
./gradlew test --tests com.powers.visual.LightRealmSkyRulesTest \
  --tests com.powers.visual.LightRealmSkyGeometryTest \
  --tests com.powers.client.visual.LightRealmSkyBoundaryTest \
  --tests com.powers.client.visual.LightRealmSkyGalleryContractTest \
  compileClientJava compileGametestJava processGametestResources \
  -Pvfx009ClientOnly --rerun-tasks --no-daemon --console=plain
# BUILD SUCCESSFUL in 15s; 11 actionable tasks executed

./gradlew runClientGameTest -Pvfx009ClientOnly \
  --rerun-tasks --no-daemon --console=plain
# nine accepted rows; BUILD SUCCESSFUL in 54s

./gradlew runClientGameTest -Pvfx009ClientOnly \
  -Pvfx009SodiumJar=<EXTERNAL_COMPATIBILITY_CACHE>/net-011/sodium-fabric-0.9.1+mc26.2.jar \
  --rerun-tasks --no-daemon --console=plain
# loaded 54 mods including sodium 0.9.1+mc26.2; nine accepted rows;
# BUILD SUCCESSFUL in 47s

./gradlew validatePowerResources auditJavaSources auditNonItemAssets verifyVfxAssetAudit \
  --rerun-tasks --no-daemon --console=plain
# BUILD SUCCESSFUL in 19s; 10 actionable tasks executed

python3 scripts/sanitize_vfx009_evidence.py
# sanitized four retained text receipts and refreshed manifests
python3 scripts/sanitize_vfx009_evidence.py --check
# privacy and checksums passed: 38 text files
python3 -m unittest scripts.tests.test_sanitize_vfx009_evidence
# Ran 1 test; OK

shasum -a 256 -c <each of the ten VFX-009 root/local SHA256SUMS manifests>
# all entries passed; root manifest covers 127 files, excluding itself

./gradlew check --rerun-tasks --no-daemon --console=plain
# first attempt: only the known timing-sensitive aggregate stale-teleport GameTest failed
./gradlew runGameTest \
  -PgameTestFilter='powers-gametest:action_submission_packet_game_tests_stale_teleport_does_not_migrate_raw_selection_or_consume_travel_lane' \
  --rerun-tasks --no-daemon --console=plain
# isolated test passed; BUILD SUCCESSFUL in 20s
./gradlew check --rerun-tasks --no-daemon --console=plain
# aggregate retry passed; BUILD SUCCESSFUL in 2m 49s; 24 actionable tasks executed;
# 144 Python tests passed

git diff --check
# clean
```

The initial aggregate failure is unchanged, non-VFX server GameTest timing behavior already present in retained VFX-011 logs; no unrelated production or test code was changed. Its isolated rerun and the full aggregate retry both passed.

## Evidence integrity and privacy

The accepted lane manifests cover raw PNG bytes, emitted JSONL metadata, options, and logs. The root manifest covers both accepted lanes, the retained POWERS JAR, manual decisions, and rejected attempts. A read-only binding check recomputed all 18 PNG digests and matched each manual row to its emitted capture ID, mode, and PASS verdict. The sanitizer rejects private home prefixes and validates both local and root manifests; absolute home paths were replaced with `<HOME>` without deleting diagnostic lines.

No third-party resource-pack live matrix was captured. The renderer's untextured built-in pipeline and source/resource audits support resource-pack independence as architecture evidence only, not visual proof.

## Clean logical commit sequence

1. `e6da63bd00b3af656272b9d10e0ac946548d5a2f` — `fix(vfx): prove upward Light Realm sky silhouettes`
2. `e78bf8f01fc79d6e05838c083685d348c214502c` — `test(vfx): bind upward Light Realm client proof`
3. This successor — closes README, CHANGELOG, compatibility matrix, backlog, Stage plan, source audit, and this final-review report while binding commit 2 and the retained POWERS JAR.

No merge or push was performed.

## Self-review and concerns

The requested final-review findings are closed together: upward normal/reduced silhouettes and fallback absence are visibly proven in both lanes, animation time is consumed exactly once, reduced motion is static by invariant and runtime test, and Sodium's identity/runtime proof is retained without storing its JAR. The worktree is left clean after the successor commit and final scans.

The only disclosed limitation is the truthful no-skylight weather result and absence of a third-party resource-pack visual matrix. Neither contradicts the acceptance claims above. The first broad aggregate's known timing-sensitive server GameTest failure passed both isolation and the clean full retry; it is recorded as a harness reliability concern, not hidden or altered in this VFX fix.
