# NET-011 report — pinned Minecraft 26.2 compatibility matrix

## Outcome

Implemented an exact-artifact compatibility manifest and isolated assembly harness, exercised each requested fixture with real Fabric processes, published a bounded compatibility matrix and privacy-safe evidence ledger, and did not add a production dependency or redistribute a third-party binary. ClaimMod, Inventory Extended nested ownership, microphone audio, and the future enhanced Light Realm sky are explicitly limited rather than represented as supported.

## Authoritative artifact research

Research was performed on 2026-08-14 against official Modrinth project/version pages and the official `api.modrinth.com/v2` project/version metadata. Exact file sizes and hashes were then checked locally by the harness.

| Artifact | Upstream release | Project/version | Bytes | SHA-256 | License / handling |
| --- | --- | --- | ---: | --- | --- |
| Sodium 0.9.1 | https://modrinth.com/mod/sodium/version/2Yom1N68 | `AANobbMI` / `2Yom1N68` | 1,834,384 | `de406c7a0ca5e748dfbe44740278400882a44e3109e2584b243ec02d4003344b` | PolyForm Shield 1.0.0; local cache only |
| Lithium 0.25.3 | https://modrinth.com/mod/lithium/version/f7vZ0VWU | `gvQqBUqZ` / `f7vZ0VWU` | 912,850 | `fdde92e238e8075f89ad7f701f2a3d5854af88ba9a67657184a4407b104ac563` | LGPL-3.0-only |
| Simple Voice Chat 2.6.22 | https://modrinth.com/plugin/simple-voice-chat/version/DKSq5wO6 | `9eGKb6K1` / `DKSq5wO6` | 5,576,838 | `1b6a8c6c41d6d7edaa10543ac623a70b0c60f22f34567969b6999c345aa277b2` | All Rights Reserved; local cache only |
| ClaimMod 1.0.5 | https://modrinth.com/mod/claimmod/version/3q3p5GRT | `XoTGYdpA` / `3q3p5GRT` | 466,936 | `2e1166cb6c1f02f328422b7a3b1ac848100ca12191391537063f8aea3996934e` | All Rights Reserved; local cache only |
| Inventory Extended 1.1.2 | https://modrinth.com/mod/inventory-extended/version/b0CvTRNk | `ovStb4Jg` / `b0CvTRNk` | 82,228 | `7cdbe2079d5e8be9c5faba8b03dbccc1ddccb7f99ec48987079cc9bd8e235bc6` | Apache-2.0; preserve notices |

All selected artifacts are stable Fabric releases explicitly tagged for Minecraft 26.2. Sodium had a newer alpha, but the latest stable release was intentionally pinned. ClaimMod's official metadata advertises server-required/client-unsupported and provides no source or published integration API; the fixture is therefore server-only and its POWERS protection integration is an unsupported boundary.

## TDD record

RED was observed before the harness existed:

```text
python3 -B -m unittest scripts.tests.test_compatibility_harness -v
```

Result: 3/3 tests failed because `scripts/compatibility_harness.py` could not be opened (exit 2). Tests required hash mismatch rejection, side-aware isolated assembly, and required authoritative source metadata.

Minimal implementation added manifest schema checks, exact size/SHA-256 validation, side/profile selection, isolated JAR copies, EULA/properties seeding, and a run receipt. Focused GREEN:

```text
python3 -B -m unittest scripts.tests.test_compatibility_harness -v
```

Result: 3/3 passed. An intermediate first GREEN attempt exposed a test-fixture mistake: the tampered bytes had changed size, exercising size rejection before hash rejection. The fixture was corrected to same-length tampering; production behavior was unchanged.

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew verifyCompatibilityArtifacts --no-daemon --console=plain
```

Result: PASS, all five pinned files verified from ignored `.compatibility-cache/net-011/`.

## Runtime evidence

Each fixture used an isolated `build/compatibility-runs/<profile>-<side>` game directory and was run sequentially alongside, without signalling, the QA-006 side-worktree soak. Dedicated servers used ports 25611–25615. The complete server used 25615; voice used UDP 24454. Raw logs remain ignored to avoid identities, UUIDs, IPs and world-private data. Privacy-safe exact markers and the full result ledger are committed at `docs/verification/evidence/2026-08-14-net-011/`.

The harness command pattern was:

```text
python3 -B scripts/compatibility_harness.py assemble --manifest config/compatibility/net-011.json --cache .compatibility-cache/net-011 --profile <profile> --side <side> --run-dir build/compatibility-runs/<profile>-<side>
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runServer --no-daemon --console=plain -PpowersRunDir=$PWD/build/compatibility-runs/<profile>-server
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew runClient --no-daemon --console=plain -PpowersRunDir=$PWD/build/compatibility-runs/<profile>-client --args='<unique development identity / optional quick-play target>'
```

Client logs for Minecraft 26.2 development launches were written relative to the Gradle process working directory, not `--gameDir`. Each was copied immediately after that exact uniquely named process exited into its isolated profile (`client-latest.log`; final combined run `client-green-latest.log`). The receipt, unique username, timestamp, and exact mod list provide run attribution; no stale root-log inference is used.

Per-fixture results:

- Sodium client: PASS for Fabric/mixin load, Sodium Apple M3 Pro OpenGL initialization, POWERS resource reload and block/item/GUI model/atlas bake. Static Light Realm joined in combined run. Enhanced/reduced-motion visual acceptance is unavailable/untested.
- Lithium client/server: PASS for load, resource bake, dedicated boot, six-dimension save and clean stop.
- Simple Voice Chat client/server: PASS for load and UDP networking. Combined client received secret/authentication/connection check while POWERS registry, travel, and activation traffic proceeded. Microphone permission was denied; no voice was recorded and audio is not claimed.
- ClaimMod server: PASS for load/config/messages/claim load/save/clean stop; LIMITED overall because no published adapter exists. Claim-denial precedence is not claimed.
- Inventory Extended client/server: PASS for load/resources/boot/save/stop; LIMITED overall because only POWERS's existing top-level ownership contract is proven and nested/unknown extended slots have no adapter.
- Combined remote pair: PASS for exact compatible mod lists, remote join, resources/models/atlases, Sodium renderer, voice authentication, POWERS testing controls, Speed Burst assignment and client activation submission, datapack/registry reload revision 2, Light Realm travel, save, ClaimMod save, disconnect and clean stop. This is one representative innate payload; the ledger does not inflate it into every action family.

Complete-stack server-side GameTests were additionally run with exact Lithium, Voice Chat, ClaimMod and Inventory Extended JARs in `build/run/gameTest/mods`:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home POWERS_TEST_RUN_ID=net011-complete-stack ./gradlew runGameTest --no-daemon --console=plain
```

First result: FAIL, 113/114 passed. The host reported a 17.706 s / 354-tick stall and `action_registry_reload_game_tests_invalid_continuation_cancels_exactly_once_without_completion` missed its tick-4 half-payment assertion.

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home POWERS_TEST_RUN_ID=net011-stack-repro ./gradlew runGameTest --no-daemon --console=plain -PgameTestFilter=powers-gametest:action_registry_reload_game_tests_invalid_continuation_cancels_exactly_once_without_completion
```

Result: PASS, 1/1 with the identical four-mod stack, despite a smaller 2.390 s / 47-tick startup stall. No speculative code change was made. One controlled full rerun result is recorded below.

An exploratory `runClientGameTest` was not used as compatibility proof. Its observed Fabric loader list contained POWERS and the test mod but neither the staged third-party JARs nor `powers-example-extension`; the integrated server then staged the two `net010_live.json` aliases and rejected them because their example-extension target keys were absent. World loading consequently timed out. This precisely scopes the failure to the client-GameTest/test-mod launch topology and the attempt's pre-launch directory deletion, not to a tested third-party combination. No durable raw client-GameTest log survived the later Loom run-directory deletion, so this observation is not a machine-result row and was not rerun. Real isolated clients provide the GUI/renderer evidence required here.

## Warning and failure attribution

- A first Lithium server launch collided with QA-006 on default port 25565 and exposed POWERS's existing failed-start diagnostics NPE because no overworld existed. The isolated profile was moved to 25611 and passed; QA-006 was never interrupted.
- The complete server raw log contains an earlier offline `/op` profile lookup 404 and rejected pre-operator client commands. Final successful server-console choreography follows in the same log.
- Development offline identities cause Mojang/Realms 401 errors; these do not occur in resource/model/network code paths.
- Voice Chat reports debug/offline encryption warnings because Fabric development mode and `online-mode=false` are intentional. Microphone denial bounds the claim to networking.
- Vanilla shader-linker unused-attribute warnings and macOS's one-time `gldCopyBufferSubData` message appeared across render runs; resources baked and frames continued. No mixin conflict or resource failure occurred.
- A 114-test complete-stack attempt had one timing-sensitive failure under a recorded host stall; focused reproduction passed. The controlled rerun is the final full-stack gate.

## Compatibility decisions

- No third-party mod became an implementation or production dependency.
- No reflection adapter was invented for ClaimMod. Existing NET-007/NET-009 public protection boundaries remain the only supported integration route.
- Unknown and nested inventory storage is fail-closed/dormant. Adding broad container traversal would violate bounded ownership and belongs to a separately specified adapter/task.
- The current static Light Realm sky is compatible with Sodium. Enhanced sky and reduced-motion acceptance remain VFX-009, so NET-011 does not relabel future work as proof.

## Verification gates

Final entries are populated from fresh commands before commit:

- focused Python harness: PASS, 30/30 repository Python tests including 3 NET-011 tests
- artifact verification: PASS, 5/5 exact files
- affected JVM/common/client/GameTest compilation and resource/docs/audits: PASS (`test`, `compileJava`, `compileClientJava`, `compileGametestJava`, all six validation/audit/doc tasks)
- all ordinary Fabric GameTests: PENDING
- controlled full compatibility-stack GameTest rerun: FAIL, 113/114; same NET-010 tick-4 assertion after a 16.738 s / 334-tick host stall. No further high-load rerun was made while QA-006 remained active.
- aggregate `./gradlew check`: FAIL because this repository's `check` depends on `runGameTest`; it reproduced 113/114 with the same exact assertion after a 15.831 s / 316-tick host stall. Its compile/audit prerequisites passed before the failure.

## Changed files and commit

Changed: `.gitignore`, `build.gradle`, `config/compatibility/net-011.json`, `scripts/compatibility_harness.py`, `scripts/net011-compatibility-client.tsv`, `scripts/tests/test_compatibility_harness.py`, compatibility matrix/evidence, platform compatibility docs, changelog, and this report. The plan checkbox and backlog row were deliberately left open.

Commit: `HEAD` (cohesive NET-011 commit; exact SHA returned after commit).

## Self-review and concerns

The manifest/harness is deliberately small and absent-safe. It validates exact metadata, size, hash, side and profile before copying; generated run configs and downloads are ignored. Runtime claims are tied to real processes and explicit evidence lines. The implementation and bounded matrix are ready to commit, but NET-011's literal all-GameTests-green completion gate is **not met**: all three full-stack executions ended 113/114 on the same tick-4 assertion during 15.8–17.7-second host stalls, while the test passed 1/1 alone under the identical stack. The plan checkbox and backlog row therefore remain open. The primary breadth limitation is that one combined real-client innate payload plus the server GameTest surface is not direct visual/manual execution of every spell/crystal/artifact with every mod. ClaimMod integration, nested Inventory Extended ownership, voice audio, enhanced/reduced-motion sky, and subjective screenshot review remain unproven. The client-GameTest launch topology's absent example extension is also unresolved and is not attributed to the compatibility fixtures.
