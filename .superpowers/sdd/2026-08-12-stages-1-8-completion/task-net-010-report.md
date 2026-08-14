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
