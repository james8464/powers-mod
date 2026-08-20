# NET-011 report — pinned Minecraft 26.2 compatibility matrix

## Outcome

Fix rounds 1 and 2 resolve the independent-review defects in the NET-011 harness and evidence. The owned isolated complete-stack task passed all 115 required GameTests; Inventory Extended's added slots are described truthfully as top-level inventory; artifact staging is descriptor-pinned and owned paths are no-follow validated; and full sanitized logs, receipts, checksums, plus eight visually inspected Sodium frames are committed. No third-party binary or production dependency is committed.

The matrix remains bounded: ClaimMod-specific integration is unsupported, microphone/audio is unclaimed, nested containers remain dormant, and the future enhanced VFX-009 Light Realm renderer is not claimed. NET-011 is closed because its compatibility guarantee now has exact versions, real-process proof and explicit limitations; VFX-009 remains the separate owner of enhanced-sky implementation.

## Authoritative artifacts

Official Modrinth project/version APIs and release pages were checked on 2026-08-14. The strict manifest ties both source and CDN paths to these exact IDs.

| Artifact | Project/version | Bytes | SHA-256 | License / handling |
| --- | --- | ---: | --- | --- |
| Sodium 0.9.1 | [`AANobbMI` / `2Yom1N68`](https://modrinth.com/mod/AANobbMI/version/2Yom1N68) | 1,834,384 | `de406c7a0ca5e748dfbe44740278400882a44e3109e2584b243ec02d4003344b` | PolyForm Shield 1.0.0; local cache only |
| Lithium 0.25.3 | [`gvQqBUqZ` / `f7vZ0VWU`](https://modrinth.com/mod/gvQqBUqZ/version/f7vZ0VWU) | 912,850 | `fdde92e238e8075f89ad7f701f2a3d5854af88ba9a67657184a4407b104ac563` | LGPL-3.0-only |
| Simple Voice Chat 2.6.22 | [`9eGKb6K1` / `DKSq5wO6`](https://modrinth.com/plugin/9eGKb6K1/version/DKSq5wO6) | 5,576,838 | `1b6a8c6c41d6d7edaa10543ac623a70b0c60f22f34567969b6999c345aa277b2` | All Rights Reserved; local cache only |
| ClaimMod 1.0.5 | [`XoTGYdpA` / `3q3p5GRT`](https://modrinth.com/mod/XoTGYdpA/version/3q3p5GRT) | 466,936 | `2e1166cb6c1f02f328422b7a3b1ac848100ca12191391537063f8aea3996934e` | All Rights Reserved; local cache only |
| Inventory Extended 1.1.2 | [`ovStb4Jg` / `b0CvTRNk`](https://modrinth.com/mod/ovStb4Jg/version/b0CvTRNk) | 82,228 | `7cdbe2079d5e8be9c5faba8b03dbccc1ddccb7f99ec48987079cc9bd8e235bc6` | Apache-2.0; preserve notices |

Sodium's newer alpha was not substituted for the latest stable release. ClaimMod's official Modrinth metadata says server required/client unsupported, while the pinned JAR declares `environment: "*"` and contains `main` plus `client` entrypoints. That packaging contradiction is documented; it is not treated as an adapter contract.

## TDD and debugging record

Observed REDs before implementation:

- Harness hardening: 12 assertions failed for absent `--allowed-root`, permissive IDs/types/size/hash/URLs, Modrinth ID mismatch, unsafe/symlink run directories, incomplete receipts and absent sanitizer. Minimal implementation produced 6/6 GREEN.
- Sanitizer phase-boundary regression: a new assertion showed `[23:32:19]` was incorrectly redacted as IPv6. The IPv6 rule was narrowed to compression or valid hextet counts; the test then passed and all evidence logs were regenerated.
- Registry-reload GameTest: two repeatable 113/114 runs failed `invalid_continuation_cancels_exactly_once_without_completion`, while its focused run passed. Investigation showed the test compared net energy across four ticks and suite phase crossed passive regeneration at `tick % 20`. The replacement asserts authoritative ledger semantics: initial `PLAYER_POOL_COST`, cancellation cost computed as pool/reservoir cost minus `TRANSACTION_ROLLBACK`, exact net 11, distinct forced `REGENERATION`, and unchanged transaction deltas on the later tick. Focused GREEN preceded the single final aggregate.
- Inventory Extended live test: first RED exposed the pinned mixin's actual 70 entries versus the assumed 68; the next RED corrected a mistaken assertion about vanilla mayfly ownership. Final test proves 43 vanilla + 27 added entries, authorization from added slot 36, removal/revocation, and POWERS toggle/snapshot cleanup.
- Acceptance scripting: a real client RED showed synthetic key down did not open Rank Maze. A bounded click path plus parsing test made it GREEN. A second visual RED showed clearing chat in the screenshot operation occurred after framebuffer capture; an explicit prior `CLEAN ui` operation and unit coverage fixed it.
- Fix-round-2 harness RED: four tests proved `mods/`, `eula.txt`, `server.properties`, and the receipt could be symlinks, and a deterministic cache-path swap staged unverified bytes. Assembly now holds a no-follow verified source descriptor, writes through owned directory descriptors, rejects non-regular children before mutation, verifies the streamed bytes, and removes partial staged output on failure. All external symlink targets remain untouched.
- Reduced-motion evidence RED: the prior saved client options contained `particles:0` (ALL), so those reduced frames were retired rather than relabelled. A test-first acceptance-only `SETTING reduced_motion` directive applies options after Minecraft initialization, saves and re-reads them, and emits the resolved marker. The single authorized corrected replay saved `particles:2` (MINIMAL) and `screenEffectScale:0.0`; its exact sanitized options, marker, receipt, logs and four replacement frames are committed.

Focused commands/results:

```text
python3 -B -m unittest scripts.tests.test_compatibility_harness
# 9 tests, PASS

./gradlew test --tests com.powers.quality.LauncherContractTest --tests com.powers.client.acceptance.AcceptanceClientScriptTest
# PASS

./gradlew runGameTest -PgameTestFilter=powers-gametest:action_registry_reload_game_tests_invalid_continuation_cancels_exactly_once_without_completion
# 1/1 PASS

./gradlew runCompatibilityGameTest -PgameTestFilter=powers-gametest:compatibility_inventory_game_tests_inventory_extended_added_slot_authorizes_then_revokes
# 1/1 PASS with exact pinned complete profile
```

## Owned harness and runtime proof

`runCompatibilityGameTest` depends on `prepareCompatibilityGameTest`, which verifies hashes and assembles `complete/server` under `build/compatibility-runs/complete-gametest`. Its receipt records exact file/version/project IDs, byte sizes, hashes, URLs, channels, sides, licenses and retrieval dates. Assembly pins each verified artifact through one no-follow file descriptor, streams it into an exclusive no-follow destination, checks the streamed size/hash, and writes the receipt last. Every owned child is inspected through directory descriptors before deletion/write. Ordinary `runGameTest` deletes its own `mods/`, so ignored manually staged JARs cannot pollute it.

The harness validates schema, safe IDs/profile membership, exact non-boolean positive sizes, lowercase SHA-256, HTTPS/no-credential URLs, project/version path linkage for Modrinth pages and CDN downloads, release dates/channels, filenames, artifact symlinks, run-directory containment/equality/symlink escape, and each owned child with no-follow metadata. Assembly only removes validated regular `.jar` files through the owned `mods/` directory descriptor.

Final complete-stack command (the only post-fix high-load run):

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runCompatibilityGameTest --console=plain
```

Result: PASS in 45 s. Fabric loaded 51 mods and reported `115 tests are now running`, then `All 115 required tests passed :)`. A retained `Can't keep up` warning reported 18.990 s/379 ticks; ledger-based assertions remained correct. QA-006 was never signalled or interrupted.

The three pre-fix full logs and focused pass are committed alongside the final log. They establish that the old failure was deterministic suite-order/tick-phase coupling, not merely a host stall. The client-GameTest extension-launch defect remains precisely scoped to test-mod topology: that launch omitted the example-extension target keys and was not used as compatibility evidence.

## Runtime matrix truth

- Sodium: real pinned macOS client, exact isolated game directories and process-role markers. Normal and corrected reduced-motion runs both loaded Sodium and produced clean unpaused static-white Light Realm, ten energy symbols/power rail, and Rank Maze frames. Double Health provides identifiable current semantic FX. The corrected replay applied settings after options initialization and its log plus saved options independently prove `MINIMAL`/`2` and scale `0.0`; the four reduced frames replace the invalid prior set. The eight selected frames were visually inspected; automatic toast frames and illegible beam frames were excluded. Enhanced sky remains `UNAVAILABLE`/deferred to VFX-009 and is not claimed.
- Lithium: complete-stack server tests cover ticks, scheduled work, Time Freeze, teleport/body/realm, save/reload and action lifecycles.
- Simple Voice Chat: UDP start, secret/authentication/connection check, and simultaneous POWERS payload/FX traffic pass. macOS microphone permission was denied; no audio was recorded or claimed.
- ClaimMod: load/config/zero claims/save/stop coexistence passes. No source/integration API or POWERS adapter exists, so ClaimMod-specific denial/scar/destruction/teleport/observe semantics remain unsupported. NET-007's generic absolute-denial/fail-closed contract is still tested.
- Inventory Extended: its mixin extends `PlayerInventory` to 70 entries. Those 27 added entries are top-level and therefore authorized by POWERS; they are not dormant. Removal revokes authorization and reconciles POWERS-owned toggles. Nested/unknown containers remain dormant.

## Evidence and warning attribution

Committed under `docs/verification/evidence/2026-08-14-net-011/`:

- complete sanitized logs for all pre-fix failures, focused pass, final 115/115, normal/reduced Sodium clients and visual server;
- exact receipts for the final GameTest and visual processes;
- exactly eight privacy-safe inspected PNGs;
- machine-readable `runtime-results.tsv` and deterministic `SHA256SUMS`.

The sanitizer redacts IPv4/ports, IPv6/loopback/ports, localhost endpoints, UUIDs, home paths, seeds, named identities and secrets while preserving timestamps and diagnostic severity. Retained warnings/errors are attributable: offline dev authentication produces Mojang/Realms 401s; Voice Chat debug/offline mode and microphone denial bound the audio claim; vanilla shader unused-attribute warnings did not prevent rendering; the final host stall did not fail any test. No renderer mixin conflict/resource failure appears.

## Verification, changed files, and concerns

Final gates:

- harness Python tests: 9/9 PASS;
- affected JVM tests: PASS;
- final owned complete-stack GameTests: 115/115 PASS;
- fresh fix-round-2 `./gradlew runCompatibilityGameTest`: PASS on 2026-08-20; all 115 required GameTests passed through the descriptor-hardened owned assembler;
- fresh aggregate `./gradlew verifyCompatibilityArtifacts check -x runGameTest --rerun-tasks`: PASS after regenerating the Java source audit; 36 Python tests and all resource/docs/audit/JVM gates passed. GameTests were excluded here because the complete pinned-stack run passed immediately beforehand;
- privacy scan, resource/docs/audit checks, exact staged-file review: recorded before commit.

Changed areas: Gradle isolated launch contract; strict compatibility manifest/harness/tests; registry ledger regression; pinned Inventory Extended live GameTest; acceptance clean/key scripting; matrix/report/evidence/visuals; Java source audit; NET-011 plan/backlog closure. VFX-009 remains open and owns the enhanced renderer.

Commit lineage: fix round 1 is `cca6944a3ad3b3091c04ce28a20ec453e5896c64`. Fix round 2 implementation/evidence SHA is appended after its cohesive direct-main commit.

Concerns: the current static renderer is proven but VFX-009 enhanced sky is not; ClaimMod has no supported adapter; microphone audio is untested; nested containers remain deliberately dormant. These are explicit matrix limits, not inferred successes.
