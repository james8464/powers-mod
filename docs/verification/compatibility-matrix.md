# Minecraft 26.2 Fabric compatibility matrix

NET-011 was exercised on 2026-08-14 with Java 25, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, and POWERS 1.0.2. `SUPPORTED` means the named tested behavior passed; `LIMITED` means coexistence passed but an integration or behavior remains unavailable; `UNTESTED` is not a support claim.

| Mod | Exact release | Topology | Result | Proven behavior | Limits |
| --- | --- | --- | --- | --- | --- |
| Sodium | [`mc26.2-0.9.1-fabric`](https://modrinth.com/mod/AANobbMI/version/2Yom1N68) | real macOS remote client, normal + reduced motion | SUPPORTED | Apple M3 Pro Sodium renderer starts; resources/models/atlases bake; visually inspected clean frames prove the unpaused static-white Light Realm, ten-symbol HUD/power rail, authored Rank Maze screen, and current Double Health semantic pulse. The corrected replay applied and re-read particles `MINIMAL` (`2`) and effect scale `0.0` after options initialization, saved those exact options, and captured the same surfaces with sparse Double Health particles. | This proves compatibility of the current static renderer and accessibility behavior. The enhanced VFX-009 renderer is **UNAVAILABLE/deferred** and is not claimed. |
| Lithium | [`mc26.2-0.25.3-fabric`](https://modrinth.com/mod/gvQqBUqZ/version/f7vZ0VWU) | real client; dedicated server; owned complete-stack GameTest server | SUPPORTED | client resources bake; dedicated boot/save/stop; all 115 complete-stack tests prove POWERS tick, Time Freeze, scheduled work, teleport/body/realm, reload, artifact and crystal lifecycles | The pre-fix 113/114 result was a deterministic test defect: a net-energy assertion crossed passive regeneration at `tick % 20`. Source-ledger assertions now prove payment/rollback independently of ambient regeneration and remain unchanged on the later tick. |
| Simple Voice Chat | [`fabric-2.6.22+26.2`](https://modrinth.com/plugin/9eGKb6K1/version/DKSq5wO6) | real client + dedicated server; combined remote pair | LIMITED | server UDP binds independently; client receives secret, authenticates, and receives the connection check while POWERS assignment, activation packet, registry reload and realm travel traffic proceeds | macOS denied microphone access, so no audio was captured or asserted. Development/offline mode warnings are expected in the isolated fixture. |
| ClaimMod | [`1.0.5`](https://modrinth.com/mod/XoTGYdpA/version/3q3p5GRT) | dedicated/combined server; embedded client metadata inspected | LIMITED | ClaimMod loads configuration/messages/zero claims, coexists with POWERS and all 115 GameTests, saves and stops cleanly; NET-007's generic absolute-denial/fail-closed contract remains tested | The Modrinth release declares server side required/client unsupported, but the exact JAR declares `environment: "*"` plus both `main` and `client` entrypoints. No published integration API exists, so POWERS has no ClaimMod adapter. ClaimMod-specific denial/scar/destruction/teleport/observe behavior is **unsupported/untested**, not inferred from coexistence. |
| Inventory Extended | [`1.1.2`](https://modrinth.com/mod/ovStb4Jg/version/b0CvTRNk) | real client; owned complete-stack GameTest server | SUPPORTED | the pinned mixin expands `PlayerInventory` from 43 to 70 entries. A Lycanbane placed in the first added slot (index 36) is therefore authorized top-level inventory; removing it revokes ownership and reconciliation clears the POWERS-owned flight toggle/snapshot. | Added `PlayerInventory` slots are not dormant. Nested or unknown container inventories remain a separate dormant boundary unless a bounded adapter is added. |

## Pinned artifacts

The authoritative manifest is [`config/compatibility/net-011.json`](../../config/compatibility/net-011.json). Downloads stay in ignored `.compatibility-cache/net-011/`; no third-party binary is distributed by POWERS.

| Project/version ID | File bytes | SHA-256 | License / redistribution |
| --- | ---: | --- | --- |
| Sodium `AANobbMI` / `2Yom1N68` | 1,834,384 | `de406c7a0ca5e748dfbe44740278400882a44e3109e2584b243ec02d4003344b` | PolyForm Shield 1.0.0; local test cache only |
| Lithium `gvQqBUqZ` / `f7vZ0VWU` | 912,850 | `fdde92e238e8075f89ad7f701f2a3d5854af88ba9a67657184a4407b104ac563` | LGPL-3.0-only |
| Simple Voice Chat `9eGKb6K1` / `DKSq5wO6` | 5,576,838 | `1b6a8c6c41d6d7edaa10543ac623a70b0c60f22f34567969b6999c345aa277b2` | All Rights Reserved; local test cache only |
| ClaimMod `XoTGYdpA` / `3q3p5GRT` | 466,936 | `2e1166cb6c1f02f328422b7a3b1ac848100ca12191391537063f8aea3996934e` | All Rights Reserved; local test cache only |
| Inventory Extended `ovStb4Jg` / `b0CvTRNk` | 82,228 | `7cdbe2079d5e8be9c5faba8b03dbccc1ddccb7f99ec48987079cc9bd8e235bc6` | Apache-2.0; preserve notices if redistributed |

These were the latest stable releases returned by the official Modrinth project/version API for Fabric and Minecraft 26.2 on the retrieval date. Sodium's newer `0.9.2-alpha.4` was not substituted for the stable release.

## Reproduction and evidence

Place files with the exact manifest filenames in `.compatibility-cache/net-011/`, then run the owned server gate:

```text
./gradlew verifyCompatibilityArtifacts
./gradlew runCompatibilityGameTest
./gradlew prepareCompatibilityRun -PpowersCompatibilityProfile=complete -PpowersCompatibilitySide=server
./gradlew runServer -PpowersRunDir=$PWD/build/compatibility-runs/complete-server
./gradlew prepareCompatibilityRun -PpowersCompatibilityProfile=complete -PpowersCompatibilitySide=client
./gradlew runClient -PpowersRunDir=$PWD/build/compatibility-runs/complete-client --args='--username <unique-test-username> --quickPlayMultiplayer <server-host>:<port>'
```

Use Java 25 and run profiles sequentially. `runCompatibilityGameTest` hash-verifies and stages the complete server profile into `build/compatibility-runs/complete-gametest`; ordinary `runGameTest` clears its own `mods/` and remains unpolluted. The harness rejects unsafe IDs/types/sizes/hashes/URLs/profiles, Modrinth URL-ID mismatches, missing/symlinked artifacts, arbitrary/equal/symlink-escaping run directories, and symlinked owned children. Verified artifacts remain pinned to one no-follow descriptor through staging; streamed size/hash mismatch removes the partial destination before any receipt is written.

Every claimed client log is the exact `<gameDir>/logs/latest.log` for the named process and receipt. The normal and corrected reduced clients used distinct run directories, role markers, timestamps, and screenshots. The reduced runtime receipt binds its acceptance script, resolved settings marker, saved exact options and selected image hashes. Complete logs were passed through the deterministic sanitizer and committed rather than referenced from ignored mutable paths.

Committed sanitized logs, exact receipts, eight inspected frames, checksums, and machine-readable outcomes are in [`docs/verification/evidence/2026-08-14-net-011/`](evidence/2026-08-14-net-011/). Offline-client 401/Realms errors, vanilla unused-shader-attribute warnings, voice debug/offline warnings, microphone denial, and the final host-stall warning are retained and attributed rather than hidden. The final owned complete-stack run passed all 115 required tests despite the host stall.
