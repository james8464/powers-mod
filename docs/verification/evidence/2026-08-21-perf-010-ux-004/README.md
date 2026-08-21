# PERF-010 + UX-004 virtual catalogue evidence

Exact implementation commit: recorded after the cohesive commit; all commands below ran from the tracked direct-`main` tree before commit.

## Proven contracts

- `ArtifactCatalogueViewModelTest` builds 10,000 synthetic actions, searches the final action, scrolls to index 4,000, preserves canonical selection across revisions, and proves the visible pool remains exactly 24 slots.
- `ArtifactRecentRulesTest` proves newest-first de-duplication, an eight-key cap, and fail-closed reconciliation against available canonical keys.
- `successfulArtifactSelectionRecordsAndTransportsBoundedRecents` enters through the registered `SelectPayload` receiver, then observes the persistent server owner and real `OpenMenuPayload` transport.
- The integrated Fabric client uses the production catalogue screen. It sends a real mouse-wheel input, verifies allocation/widget counters remain identical, searches for Fireball, clicks the result and quick-wheel slot `1`, waits for the server-authoritative favourite, and captures the resulting screen.
- Every bind/select payload still carries the captured registry revision, alignment, canonical action key, and existing bounded option/slot values. The server continues to validate owner, rank, alignment, revision, canonical spelling, and rate limit before mutation.

## Visual evidence

- `catalogue-full.png` — production fixed-grid catalogue at 1280×720.
- `catalogue-fireball-filter.png` — one-result localized Fireball search after the real direct-bind interaction.

```text
00d362d3916bb4d09fef67320927f2899da2661eca6965eaf98778dbb8afd3c1  catalogue-full.png
04e72e9105ad819e16eabf51a95c645eca937d89c53600a98402d841fa598d25  catalogue-fireball-filter.png
```

## Verification commands

```text
./gradlew test --tests com.powers.item.artifact.ArtifactCatalogueViewModelTest --tests com.powers.item.artifact.ArtifactRecentRulesTest --tests com.powers.item.artifact.ArtifactCatalogueRulesTest --tests com.powers.player.PlayerAttachmentPersistenceTest --tests com.powers.network.ShadowSwordPacketsTest --tests com.powers.client.ClientActionRefreshTest --no-daemon
./gradlew runGameTest -PgameTestFilter=powers-gametest:action_submission_packet_game_tests_successful_artifact_selection_records_and_transports_bounded_recents --no-daemon
./gradlew runClientGameTest --no-daemon
./gradlew check -x runGameTest --rerun-tasks --no-daemon
```

The full 126-test GameTest aggregate was also attempted twice. Its unrelated timing-sensitive failures and the immediately green isolated reruns are recorded transparently in the task report. No QA-006 process/worktree or protected QA-005 capture was touched.
