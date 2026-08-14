# NET-010 completion report

## Status

DONE. The production catalogue now publishes one immutable, monotonic, atomic action snapshot; server-data reloads validate all aliases before publication; menus and action submissions carry the snapshot revision and canonical key; prepared casts retain their original snapshot; and every persisted selection owner migrates retired keys through the same alias resolver.

## TDD record

- Registry RED: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-red ./gradlew test --tests com.powers.magic.ActionRegistrySnapshotTest --no-daemon` failed at `compileTestJava` with 27 missing snapshot, reload, and submission symbols.
- Reload RED: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-red-reload ./gradlew test --tests com.powers.magic.ActionRegistryReloadListenerTest --no-daemon` failed at `compileTestJava` with 7 missing reload-listener symbols.
- Payload RED: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-red-payload ./gradlew test --tests com.powers.network.ActionPayloadRevisionTest --no-daemon` failed because the production packet constructors did not yet expose revisions and canonical keys.
- Actual-owner RED: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-red-live-favourite ./gradlew runGameTest -PgameTestFilter=powers-gametest:action_registry_reload_game_tests_saved_artifact_key_migrates_through_actual_owner --no-daemon` reached the production owner and failed with `Favourite owner rejected a resolvable retired key`.
- Focused GREEN: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-final-focused ./gradlew test --tests com.powers.magic.ActionRegistrySnapshotTest --tests com.powers.magic.ActionRegistryReloadListenerTest --tests com.powers.network.ActionPayloadRevisionTest --tests com.powers.network.ShadowSwordPacketsTest --no-daemon` passed 11 focused JVM tests.
- Focused live GREEN: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-final-live ./gradlew runGameTest -PgameTestFilter=powers-gametest:action_registry_reload_game_tests --no-daemon` passed all 5 NET-010 production-entrypoint GameTests.
- A first broad run reached 110/110 GameTests, then correctly failed 1 of 1,490 JVM tests because `PlayerPowers.java` had crossed the 450-line responsibility boundary. Stable selection persistence was extracted into `StableActionSelectionStore`; `POWERS_TEST_RUN_ID=net010-size-fix2` then passed the focused source-quality and registry tests.
- Final full gate: `python3 scripts/audit_java_sources.py && git diff --check && JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-final-check2 ./gradlew check --no-daemon` passed 1,490 JVM tests, 110/110 required Fabric GameTests, 27/27 Python tests, common/client/example/GameTest compilation, Java and asset audits, resource validation, and item/magic/rank documentation verification.
- Independent all-live gate: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-final-live-all ./gradlew runGameTest --no-daemon` passed 110/110 required Fabric GameTests.

## Design and migration contract

- `ActionRegistrySnapshot` owns revision, ordered canonical definitions, aliases, and validated counts. It defensively copies all maps. Alias keys are syntax/length bounded, limited to 256 entries and 16 hops, cannot collide with canonical keys, must terminate at a canonical definition, and reject cycles or unknown targets.
- `MagicActionCatalogue` is still the sole catalogue. Built-ins and NET-009 external actions publish through its volatile snapshot. Successful reload/registration/removal publishes one new revision; any validation failure retains the exact previous snapshot object and revision.
- `ActionRegistryReloadListener`, registered by `PowersBootstrap`, prepares sorted `data/*/powers_actions/*.json` server resources off-thread and applies one validated alias map at the server reload boundary. Malformed or duplicate entries fail preparation; partial state is never visible.
- Shadow Sword/Heavenly Partisan, grimoire, and crystal menu snapshots expose the revision. Every corresponding serverbound selection/action payload includes revision plus canonical action key. Validation occurs before rate limiting, payment, cooldown, selection mutation, or casting. A stale, future, unknown, or mismatched submission takes one authoritative current-menu refresh branch.
- `ServerMagicCasts.prepare` captures the snapshot before payment and puts it in `PreparedMagicCast`; its already-resolved definition and revision remain immutable across reload, so completion cannot mix definitions.
- Save compatibility is additive. Existing integer `spell_selections` and `crystal_selections` codecs remain deterministic fallbacks; new `spell_selection_keys` and `crystal_selection_keys` maps persist stable canonical IDs. Valid stable IDs remain unchanged.

### Persisted-owner audit

- `ArtifactSelectionState.selected/select`: resolves an `innate/`, `unique/`, or `dominion/` saved action through `ArtifactSelectionMigration`, validates it against the actual alignment/rank catalogue, and writes the canonical artifact key.
- `ArtifactSelectionState.favourites/bindFavourite`: applies the same migration to the real eight-slot attachment owner, preserving the owner's existing reconciliation, uniqueness, and padding contract.
- `PlayerPowers.selectedSpell`: reads `spell_selection_keys`, resolves aliases, rewrites a retired value to its canonical ID, and falls back to the legacy page map when the stable key is absent or unusable.
- `PlayerPowers.selectedCrystalMode`: performs the equivalent canonical rewrite through `crystal_selection_keys`, with the legacy crystal-index map retained as fallback.
- `ActionRegistryReloadGameTests.savedArtifactKeyMigratesThroughActualOwner` exercises selection and favourite attachments on a real mock server player. `savedSpellAndCrystalKeysMigrateThroughPlayerPowers` exercises both stable maps through the public `PlayerPowers` owner. This is owner-level proof, not a pure migration-helper assertion.

## Production reachability

- Fabric invokes `ActionRegistryReloadListener` from the server-data reload registration in `PowersBootstrap`.
- `ShadowSwordPackets`, `GrimoirePackets`, and `CrystalSelectorPackets` enforce revision/key validation at their registered server receivers; `PowersClient` and the concrete screens send and retain the authoritative revision.
- `ServerMagicCasts.prepare`, reached by normal innate, spell, crystal, artifact, and extension casting routes, captures the current snapshot.
- `SpellCastingManager`, `GrimoirePackets`, and `ModeCrystalAbility` read/write the stable `PlayerPowers` owners; artifact weapon/menu flows use `ArtifactSelectionState`.

## Files and commit

- Registry/runtime: `ActionRegistrySnapshot`, `ActionRegistryReloadListener`, `ActionSubmissionValidation`, `MagicActionCatalogue`, `PreparedMagicCast`, `ServerMagicCasts`, and bootstrap registration.
- Network/client: the Shadow Sword, grimoire, crystal, teleport, catalogue, convergence, acceptance, scroll, and client snapshot paths.
- Persistence: `ArtifactSelectionMigration`, `ArtifactSelectionState`, `PlayerPowerAttachments`, `PlayerPowers`, `StableActionSelectionStore`, `ModeCrystalAbility`, and `SpellCastingManager`.
- Proof: three focused JVM test classes, the updated Shadow Sword packet test, `ActionRegistryReloadGameTests`, its Fabric entrypoint, and client GameTest compatibility updates.
- Documentation: README, CHANGELOG, integration API, action reload guide, migration guide, evidence README, backlog, stage plan, and regenerated Java audit.
- Cohesive direct-main commit subject: `feat(network): add revisioned atomic action reload`. The resulting SHA is returned with task completion; no branch, worktree, push, or unrelated QA staging was used.

## Self-review and concerns

- Reviewed all packet handlers to confirm rejection precedes limiter/mutation/payment/cooldown/cast work and emits one refresh from the failing branch.
- Reviewed snapshot publication for immutable capture, monotonic revision, failed-reload identity retention, external-action participation, alias collision/cycle/depth/count handling, and server-only resource authority.
- Audited all real persisted action-selection attachments: artifact selection, artifact favourites/loadout, spell selection, and crystal selection. Legacy integer fields remain readable.
- Re-ran source audit after extracting persistence mechanics; `PlayerPowers.java` is within the reviewed boundary and the generated manifest matches production sources.
- Untracked `.codex-tmp/`, `.lwjgl/`, and QA-005 screenshots were preserved and excluded from staging.
- Concerns: none. The GameTest server emitted its existing transient `Can't keep up` warning during the broad run, but all required tests completed successfully.

## Review correction round 2 (2026-08-14)

All three remaining Important findings (canonical owner keys, rejection-time owner mutation/crystal invalidation, and truthful live entrypoint proof) are addressed.

### Observed RED

- Canonical-owner RED: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-review2-red-qualified ./gradlew test --tests com.powers.magic.ActionRegistrySnapshotTest --no-daemon` ran 8 tests and failed the 2 new literal tests: impossible `unique/fireball`, `crystal/fireball`, and `dominion/augury` keys resolved and were accepted alias targets.
- Registered-packet RED: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-review2-red-packets2 ./gradlew runGameTest '-PgameTestFilter=powers-gametest:action_submission_packet_game_tests_*' --no-daemon` reached all 3 registered receivers through `ServerboundCustomPayloadPacket`. Stale cycle and teleport both migrated the raw attachment before validation; crystal owner loss emitted a full `OpenPayload` rather than the required explicit invalidation.
- Client-surface RED: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-review2-red-client ./gradlew test --tests com.powers.client.ClientActionRefreshTest --no-daemon` failed compilation because no surface-aware refresh router existed.
- The first real datapack fixture intentionally exposed lifecycle ordering: `net010-review2-live-reload-cast` failed initial server resource loading because a datapack alias targeted the NET-009 external action before extension registration. The final fixture aliases a built-in action; the post-start registered reload proves the external definition remains in the same published snapshot.

### Fix and owner/handler audit

- `ActionRegistrySnapshot` now owns an immutable `menuKeys` set in addition to typed definitions, aliases, revision, and validation counts. `MagicActionCatalogue` derives that set from the real immutable `ArtifactActionCatalogue` at every publish boundary. Qualified resolution accepts only the 45 actually owned innate/crystal/unique/dominion keys; arbitrary namespace/action cross-products cannot be canonical or alias targets. Unqualified built-in and external IDs retain their typed `MagicActionId` contract, alias depth/count/cycle/collision checks, and atomic failed-reload identity retention.
- `ArtifactSelectionState.peekSelected` is the non-mutating persisted-selection read used by cycle and teleport before `ActionSubmissionService`. Rejected packets now emit one explicit artifact invalidation instead of reopening a menu whose owner path save-migrates attachments. Live stale tests retain the exact retired raw attachment, energy, cooldown deadline, position/action state, and complete limiter capacity.
- Crystal rejection resends a full selector only while the player still holds a radial Rainbow convergence owner. Owner/item loss sends exactly one `RefreshPayload("crystal")`. `ClientActionRefresh` closes only the matching crystal, artifact/teleport, or grimoire screen and leaves unrelated surfaces open.
- Handler audit reconfirmed all seven revisioned routes: Shadow Sword/Partisan select, commit, cycle, favourite bind, and teleport; grimoire selection; and crystal selection. Each enters `ActionSubmissionService`, whose order remains revision/key, live owner/context, limiter, then mutation. Cycle/teleport have no mutating pre-read; all rejection callbacks now have one transport outcome. Client senders continue to send the captured revision and canonical key.
- Persisted-owner coverage remains complete: `PlayerPowers` spell and crystal stable selections use the unqualified canonical resolver; `ArtifactSelectionState` selected attachment and favourites use qualified aliases without namespace loss and retain their reconciliation/deduplication rules. The live owner test exercises literal `innate/`, `crystal/`, `unique/`, and `dominion/` migration.

### Truthful production proof and GREEN

- `ActionRegistryReloadGameTests.successfulAndFailedReloadAreAtomic` now invokes `server.reloadResources(...)` against `data/powers/powers_actions/net010_live.json`, reaching the registered Fabric listener prepare/parser/apply path. It proves one revision publication, real alias visibility, failed-publication object identity, and retention of the NET-009 `example_resonant_field` definition.
- `ActionSubmissionPacketGameTests` injects real custom payload packets through `ServerGamePacketListenerImpl.handleCustomPayload` and observes actual `ClientboundCustomPayloadPacket` writes at the mock player's Netty connection. This replaces source-string reachability proof and validates exactly one typed refresh at the network boundary.
- The active-cast test now completes a real Dimensional Anchor channel under its captured snapshot and asserts the authored anchored-target effect. The cancellation test removes the held grimoire, asserts exactly one half-energy interruption, no authored effect, no later charge, and no later completion.
- Focused JVM GREEN: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-review2-focused-final ./gradlew test --tests com.powers.magic.ActionRegistrySnapshotTest --tests com.powers.magic.ActionRegistryReloadListenerTest --tests com.powers.network.ActionSubmissionServiceTest --tests com.powers.network.ActionPayloadRevisionTest --tests com.powers.network.ShadowSwordPacketsTest --tests com.powers.client.ClientActionRefreshTest --no-daemon` passed.
- Registered-packet live GREEN: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-review2-live-packets-final ./gradlew runGameTest '-PgameTestFilter=powers-gametest:action_submission_packet_game_tests_*' --no-daemon` passed 3/3.
- Reload/cast/migration live GREEN: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-review2-live-reload-final ./gradlew runGameTest '-PgameTestFilter=powers-gametest:action_registry_reload_game_tests_*' --no-daemon` passed 6/6.
- The first aggregate invocation stopped before compilation/tests because the generated Java audit correctly detected the added classes. After `python3 scripts/audit_java_sources.py`, `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-review2-final-check2 ./gradlew check --no-daemon` passed 1,517 JVM tests, 114/114 required Fabric GameTests, 27 Python tests, common/client/example/GameTest compilation, resource validation, source/non-item audits, and item/magic/rank documentation verification.

### Self-review and concerns

- Reviewed the complete diff for canonical-key ownership, volatile snapshot publication, registered-listener reachability, receiver ordering, owner-loss behavior, client surface routing, cast terminal states, and exact QA-artifact exclusion. `git diff --check` is clean.
- The removed `ActionSubmissionHandlerContractTest` only counted source strings; its production claim is superseded by registered packet and outbound-transport GameTests plus the focused service tests.
- Untracked `.codex-tmp/`, `.lwjgl/`, and QA-005 screenshots remain untouched and unstaged. No branch, worktree, push, or unrelated change was used.
- Concern: the live transport assertion uses reflection solely in GameTest code to observe Minecraft's private in-memory connection/channel; it is runtime-proven on the locked Minecraft 26.2 target but will intentionally fail loudly if those internals change on a future Minecraft upgrade.

## Review correction round 1 (2026-08-14)

All three Important review findings are addressed.

### Observed RED

- Qualified resolver/service RED: `POWERS_TEST_RUN_ID=net010-review1-red-unit ./gradlew test --tests com.powers.magic.ActionRegistrySnapshotTest --tests com.powers.network.ActionSubmissionServiceTest --no-daemon` failed compilation because `resolveKey` and the ordered submission service did not exist.
- Registered-handler reachability RED: `POWERS_TEST_RUN_ID=net010-review1-red-routing ./gradlew test --tests com.powers.network.ActionSubmissionHandlerContractTest --no-daemon` failed because none of the three packet owners called the service.
- Qualified submission RED: `POWERS_TEST_RUN_ID=net010-review1-red-qualified-submit ./gradlew test --tests com.powers.magic.ActionRegistrySnapshotTest --no-daemon` rejected the literal canonical key `innate/fireball`.
- Live-production RED: `POWERS_TEST_RUN_ID=net010-review1-red-live ./gradlew compileGametestJava --no-daemon` failed because the registered reload parser/publisher and live channel snapshot were not reachable by the GameTest.
- Refresh protocol RED: `POWERS_TEST_RUN_ID=net010-review1-red-refresh-payload ./gradlew test --tests com.powers.network.ActionPayloadRevisionTest --no-daemon` failed compilation because no explicit authoritative invalidation payload existed.

### Fix and owner audit

- `ActionRegistrySnapshot.resolveKey` is now the one canonical string resolver. It preserves `innate/`, `crystal/`, `unique/`, and `dominion/` namespaces, validates the suffix as a typed action, and retains bounded, acyclic, unknown-target and collision rejection. `resolve` remains the typed built-in/external action adapter.
- `ArtifactSelectionMigration` no longer strips prefixes. The real `ArtifactSelectionState` selected and favourite owners persist full canonical menu keys; its live test covers every supported prefix literally. The real `PlayerPowers` spell and crystal stable-key owners also migrate and rewrite aliases loaded through the production listener/parser.
- `ActionSubmissionService` orders revision/key validation, then live context validation, then limiter, then mutation. Invalid submissions invoke exactly one supplied authoritative refresh. Shadow select, commit, cycle, bind and teleport plus grimoire and crystal handlers all call it; context covers held item/alignment or grimoire, authorization/rank, action membership, selection option/slot/direction, convergence mode, teleport subject/input/dimension coordinates, and current selected action.
- Full menus are resent when the current owner remains available. If the owner disappeared or changed, the explicit `RefreshPayload` carries the current revision and closes the stale client surface. Limiter lanes, payment, cooldown, mutation and cast are not reached on invalid context.
- Live GameTests now call `ActionRegistryReloadListener.reloadDocuments`, the same parser and atomic publication method used by the registered Fabric reload listener. They prove failed-reload identity retention, NET-009 external action inclusion, one refresh/zero side effects through the production submission service, and a real Augury channel completing with its captured `SpellCastTransaction` snapshot after reload.

### GREEN and correction note

- Focused JVM/service/handler/payload tests passed under `net010-review1-green-unit`, `net010-review1-green-routing`, and `net010-review1-green-refresh`; the affected magic/network/artifact/spell/source-quality suite passed under `net010-review1-affected`.
- Authoritative focused live command: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-review1-live-final ./gradlew runGameTest '-PgameTestFilter=powers-gametest:action_registry_reload_game_tests_*' --no-daemon`; 5/5 passed. The earlier report's filter without `_*` selected no tests in this environment and is superseded by this quoted wildcard command.
- The first broad review run reported one GameTest failure without retaining its identity after later runs. An immediate independent full-batch reproduction under `net010-review1-repro` passed 110/110, so no speculative production change was made; the final gates below are authoritative.
- Broad production reachability RED: the first review `check` passed 110/110 GameTests, then failed 1/1,515 JVM tests because the standalone `ActionSubmissionValidation` was only test-reached after handler centralization. `ActionSubmissionService` now delegates its revision/key gate to that production validator; `net010-review1-reachability-green` passed reachability, resolver, and service tests.
- Final aggregate GREEN: `JAVA_HOME='/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home' POWERS_TEST_RUN_ID=net010-review1-final-check2 ./gradlew check --no-daemon` passed 1,515 JVM tests, 110/110 required Fabric GameTests, 27 Python tests, compilation, audits, resources, and documentation gates.
- Independent full live `net010-review1-final-live-all` encountered the pre-existing unrelated `fx_coalescing_game_tests_event_scale_lod_reaches_near_mid_and_far_observers` timing assertion after the aggregate gate had passed. The isolated retry `net010-review1-unrelated-fx-retry` passed 1/1. NET-010 focused live remained 5/5 and the separate all-live reproduction remained 110/110.
