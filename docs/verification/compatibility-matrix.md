# Minecraft 26.2 Fabric compatibility matrix

NET-011 was exercised on 2026-08-14 with Java 25, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, and POWERS 1.0.2. `SUPPORTED` means the named tested behavior passed; `LIMITED` means coexistence passed but an integration or behavior remains unavailable; `UNTESTED` is not a support claim.

| Mod | Exact release | Topology | Result | Proven behavior | Limits |
| --- | --- | --- | --- | --- | --- |
| Sodium | [`mc26.2-0.9.1-fabric`](https://modrinth.com/mod/sodium/version/2Yom1N68) | real macOS client; combined remote client | LIMITED | Fabric/mixins load; Apple M3 Pro Sodium renderer starts; POWERS/Sodium resources, block/item/GUI atlases and models bake; HUD/screens and semantic FX code paths remain loaded; live combined client enters Light Realm and captures a frame without a renderer crash | POWERS currently has only its static Light Realm sky. The enhanced shader/skybox and reduced-motion visual acceptance remain unimplemented under VFX-009, so neither is claimed. Captures were not visually graded in NET-011. |
| Lithium | [`mc26.2-0.25.3-fabric`](https://modrinth.com/mod/lithium/version/f7vZ0VWU) | real client; dedicated server; combined GameTest server | SUPPORTED | client resources bake; dedicated boot/save/stop; complete-stack server runs POWERS tick, Time Freeze, scheduled work, teleport/body/realm, reload, artifact and crystal lifecycle tests | One full 114-test attempt had a timing failure after a 17.7 s host stall; the exact failing test passed alone under the same stack. The aggregate rerun result is recorded in the evidence ledger. |
| Simple Voice Chat | [`fabric-2.6.22+26.2`](https://modrinth.com/plugin/simple-voice-chat/version/DKSq5wO6) | real client + dedicated server; combined remote pair | LIMITED | server UDP binds independently; client receives secret, authenticates, and receives the connection check while POWERS assignment, activation packet, registry reload and realm travel traffic proceeds | macOS denied microphone access, so no audio was captured or asserted. Development/offline mode warnings are expected in the isolated fixture. |
| ClaimMod | [`1.0.5`](https://modrinth.com/mod/claimmod/version/3q3p5GRT) | dedicated/combined server only, matching upstream side metadata | LIMITED | ClaimMod loads configuration/messages/zero claims, coexists with the POWERS server and full GameTest surface, saves its data, and stops cleanly | Its official project/version metadata publishes no source or integration API. POWERS therefore has no ClaimMod adapter. ClaimMod denial precedence, scars/destruction/teleport/observe containment, and adapter exception fail-closed behavior are **unsupported/untested**, not inferred from coexistence. NET-007's generic adapter contract remains separately tested. |
| Inventory Extended | [`1.1.2`](https://modrinth.com/mod/inventory-extended/version/b0CvTRNk) | real client; dedicated server; combined client/server | LIMITED | client resources and inventory UI code load; server initializes; POWERS top-level ownership authorization and revocation contracts pass with the complete server stack | POWERS recognizes only its bounded vanilla top-level player-inventory scan. Extended or nested/unknown container slots have no explicit adapter and remain dormant; moving an artifact through every added slot was not live-tested. |

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

Place files with the exact manifest filenames in `.compatibility-cache/net-011/`, then run:

```text
./gradlew verifyCompatibilityArtifacts
./gradlew prepareCompatibilityRun -PpowersCompatibilityProfile=complete -PpowersCompatibilitySide=server
./gradlew runServer -PpowersRunDir=$PWD/build/compatibility-runs/complete-server
./gradlew prepareCompatibilityRun -PpowersCompatibilityProfile=complete -PpowersCompatibilitySide=client
./gradlew runClient -PpowersRunDir=$PWD/build/compatibility-runs/complete-client --args='--username <unique-test-username> --quickPlayMultiplayer <server-host>:<port>'
```

Use Java 25 and run profiles sequentially. The harness refuses a missing, size-mismatched, or SHA-256-mismatched artifact and writes a receipt to the isolated game directory. Client console logging in this Minecraft development launch is rooted at the Gradle process working directory; each claimed client log was copied immediately after its uniquely named process exited into its isolated profile as `client-latest.log` (combined final: `client-green-latest.log`). Receipts, unique usernames, timestamps, and mod lists bind each copy to its run. Raw logs remain ignored because they contain development identities, UUIDs, and loopback addresses.

Committed, redacted markers and machine-readable outcomes are in [`docs/verification/evidence/2026-08-14-net-011/`](evidence/2026-08-14-net-011/). The complete server log also contains an earlier pre-join offline `/op` profile lookup 404 and rejected non-operator client commands before the final successful console choreography; those are attributed fixture attempts, not compatibility failures. Offline-client 401/Realms errors, vanilla unused-shader-attribute warnings, macOS `gldCopyBufferSubData` output, voice debug mode, and microphone denial are likewise explicitly recorded rather than treated as clean-log success.
