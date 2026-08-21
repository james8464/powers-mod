# Task PERF-010 + UX-004 report — fixed-widget virtual catalogue

## Status

Accepted on direct `main`. Initial implementation: `a5fe966fbf2974c1eb47b639ec5cb72979fae9bb`; review correction and exact verified implementation: `eb17a3a82a42d852916e3970710c880337f717dc`.

## Delivered behavior

- One column-major virtual scrolling grid reuses a fixed button pool; scroll, search, filter, refresh, select, and bind never rebuild widgets.
- Global nonblank search finds unlocked actions from the default Favourites surface. Empty search restores the chosen Favourites/Recent/Innate/Crystals/Sword filter.
- Canonical-key selection and clamped first-visible position survive monotonic registry refreshes; every revision-bearing receiver rejects reordered older payloads.
- Keyboard arrows move both model selection and visible GUI focus. Hidden pooled buttons cannot retain focus, and narration follows the focused action.
- Recents are newest-first, de-duplicated, persisted and decoded with an eight-key allocation bound.
- Bind clicks are non-optimistic. The server validates revision, owner, alignment, rank, slot, canonical key, and limiter; success sends an authoritative menu refresh. Commit additionally requires membership in server-owned favourites before payment or effect.
- The released eight-slot wheel and column-major reading order remain unchanged.

## TDD and live acceptance

RED was observed for missing virtualization, filtering, stable scroll, bounded recents, focus/narration, monotonic refresh, non-optimistic authority, and 10,000-action production-screen contracts. Focused JVM and registered packet-path tests then passed.

The registered authority proof is one sequential orchestrator because Fabric mock players share an identity: unbound commit denies payment/effect; stale bind emits exactly one matching invalidation; rate-limited current bind emits no false menu acknowledgement; successful bind returns the authoritative wheel and only then permits commit. Three formerly concurrent authority fixtures therefore became one stronger test, changing the required aggregate from 129 to 127.

The production client fixture loaded 10,000 synthetic actions without shipping them, proved real wheel movement changes the window while allocation/widget counts remain fixed, searched an unbound action from the default surface, bound it in two interactions, exercised focus/narration and revision refresh, and completed in 36 seconds.

## Final verification

- `./gradlew runGameTest --no-daemon`: 127/127 required GameTests passed in 51.50 seconds.
- `./gradlew check --rerun-tasks --no-daemon`: passed in 1 minute 43 seconds; its embedded 127/127 GameTests passed in 48.29 seconds, 1,589 JVM tests passed, 45 Python tests passed, and resource, source, asset, access-widener, and generated documentation checks passed.
- Integrated production client: passed in 36 seconds at both GUI scales; both accepted captures were manually inspected for clean title/summary/tabs/rows/selection/favourites geometry.
- `git diff --cached --check`: passed before the implementation commit.

## Save and authority boundaries

Older saves decode missing recents as empty and write only the newest bounded schema. Favourites, selections, released IDs, aliases, and server-owned revision checks remain compatible. No client catalogue state can authorize a bind or cast.
