# PERF-010 + UX-004 — Virtual artifact catalogue brief

## Scope

Replace page-based catalogue reconstruction with one revision-aware virtual view and a fixed pool of visible action buttons. Add search, category/favourites/recents filters, stable canonical-key selection, direct eight-slot binding, mouse-wheel and keyboard navigation, and truthful narration. Keep every submission on the existing server-authoritative revision/key validation path.

## Architecture

- `ArtifactCatalogueViewModel` owns immutable action/label indexes, current filter/query, bounded recent keys, canonical selected key, and a clamped first-visible row. It exposes only the current visible slice and preserves selection by canonical key across revision refreshes.
- The screen creates exactly `columns * rows` action buttons during `init`. Scrolling, searching, filtering, and selecting only rebind those widgets; none of those operations reconstructs the screen or allocates widgets.
- Scroll offset is row-based. Slot mapping is column-major to preserve the existing visual reading order. Empty slots are hidden and inactive.
- Search matches stable keys, ability IDs, and localized labels. Tabs cover favourites, recent actions, innate powers, crystals, and artifact-specific actions. Alignment remains the server-authored current artifact and is displayed/narrated as part of the result summary.
- Clicking a result selects it; clicking a numbered quick-wheel slot sends the existing authenticated `BindFavouritePayload`. Thus a searched action is directly bindable in two clicks. The separate Select action keeps its existing authenticated `SelectPayload` behavior.
- Recents are bounded to eight canonical keys per alignment, persisted server-side, updated only after a successful authoritative selection, reconciled against the current catalogue, and included in `OpenMenuPayload`.
- A new menu payload refresh replaces the current catalogue screen in place while copying query/tab/scroll/selection when alignment matches. Stale server submissions still trigger the existing revision refresh/close behavior.

## Acceptance tests

1. RED then GREEN pure tests: 10,000 synthetic actions, constant pool size, deterministic visible mapping, search/filter/scroll, bounded recents, stable selection across a new revision, and two-click direct-bind contract.
2. Client tests: fixed widget allocation count across repeated scroll/search/filter/selection refresh, keyboard navigation, focus/narration labels, and in-place revision refresh.
3. GameTest: the registered bind/select packets preserve revision, authorization, rank, slot, and canonical-key validation; a successful selection records and transports bounded recents.
4. Integrated client: open/search/scroll/select/direct-bind capture on the production screen when the harness supports input; otherwise capture the exact production screen plus client instrumentation evidence.
5. Focused suites, `compileClientJava`, resource/document/source audits, full GameTests, and `check` must pass before both backlog rows are removed.

## Non-goals

- No client-authoritative selection, cast, rank, energy, cooldown, or alias decision.
- No replacement of the eight-slot quick wheel.
- No unbounded history, per-scroll widget creation, or eager rendering of all filtered entries.
