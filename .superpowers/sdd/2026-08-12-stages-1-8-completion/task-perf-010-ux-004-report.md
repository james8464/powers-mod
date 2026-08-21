# Task PERF-010 + UX-004 report — fixed-widget virtual catalogue

## Status

Implementation and focused live acceptance are complete on direct `main`, starting from `08110e5e2193c3d956ccb93d8a67c97c18fdbe5b`. The cohesive resulting commit is recorded at handoff because a commit cannot embed its own object ID.

## Delivered behavior

- Replaced numbered catalogue pages and `rebuildWidgets()` with a fixed `columns × rows` action-button pool. Scroll, search, filter, select, and bind operations only rebind those slots.
- Added a pure revision-aware view model with indexed localized/stable-key search, row scrolling, keyboard movement, canonical-key selection persistence, and deterministic Favourites/Recent/Innate/Crystals/Sword filters.
- Added newest-first, deduplicated, eight-entry Light/Dark recent histories as additive persistent attachments. Only a successful authoritative artifact selection records a recent key.
- Extended the server-authored menu payload with bounded recents. A compatible menu refresh can update an open catalogue in place; stale invalidations now close both wheel and catalogue surfaces.
- Preserved the eight-slot quick wheel and existing server validation. Search result click plus numbered-slot click is the direct two-interaction bind path.
- Added alignment/result position presentation, keyboard navigation, full action-state button messages/tooltips, and standard Minecraft button narration.

## TDD evidence

1. RED: the 10,000-action, stable-revision, bounded-recent, and direct-binding tests failed to compile because the view model, recent rules, and Recent tab did not exist. GREEN: all pure contracts passed.
2. RED: persistent-schema and menu-codec tests failed because recents had no authoritative owner or transport. GREEN: additive per-alignment attachments and bounded payload lists round-trip.
3. RED: the client invalidation test proved stale artifact responses did not close the catalogue. GREEN: catalogue and wheel now share the artifact invalidation boundary.
4. Live RED: the first packet GameTest fixture lacked the Darkness identity tag, correctly proving the authoritative receiver refused mutation. GREEN: the corrected authorised fixture recorded and transported exact recents.
5. Integrated RED: a deliberately unauthorised direct-screen fixture was closed by the real stale-response path. GREEN: the integrated player now holds the correct artifact, alignment tag, rank, and exact registry revision; real result/slot clicks are accepted and observed on the server.

## Verification

Focused JVM, packet GameTest, compile-client, and two full integrated-client suites passed. The final integrated run completed in 35 seconds, produced `catalogue-full.png` (`00d362d3916b…`) and `catalogue-fireball-filter.png` (`04e72e9105a…`), and proved the allocation counter remained identical across real mouse scrolling, search, selection, and binding. Manual image inspection found the favourites caption overlapping the selection row on the first run; the corrected second capture has clean separation.

The repository-wide non-GameTest gate passed from a clean rerun: Java tests, 45 Python tests, resource validation, source/asset audits, and generated item/magic/rank documentation checks. All three GameTests directly covering this unit passed in isolation: authoritative recent recording/transport and the two existing stale cycle/teleport revision boundaries. The existing QA-009 production packet-fault test also passed in isolation.

Two complete 126-test GameTest runs exposed pre-existing cross-test timing interference rather than a catalogue regression. The first run failed only the stale cycle/teleport tick-2 assertions; both immediately passed alone. The second failed only the QA-009 artifact-teleport payment deadline; it immediately passed alone. This exact broad-suite limitation is retained here rather than falsely reporting a green aggregate.

## Save migration and boundaries

Older saves have no recent attachments and therefore decode to empty histories. The next successful selection writes only the newest schema, at most eight current canonical keys per alignment. Favourites and selections retain their released IDs and alias migration. No client decision can bypass revision, owner, alignment, rank, slot, option, or rate-limit validation.
