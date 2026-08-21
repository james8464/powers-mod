# PERF-010 + UX-004 — Virtual artifact catalogue brief

## Scope

Replace page-based catalogue reconstruction with one revision-aware virtual view and a fixed pool of visible action buttons. Add search, category/favourites/recents filters, stable canonical-key selection, direct eight-slot binding, mouse-wheel and keyboard navigation, and truthful narration. Keep every submission on the existing server-authoritative revision/key validation path.

## Architecture

- `ArtifactCatalogueViewModel` owns immutable action/label indexes, current filter/query, bounded recent keys, canonical selected key, and a clamped first-visible row. It exposes only the current visible slice and preserves selection by canonical key across revision refreshes.
- The screen creates exactly `columns * rows` action buttons during `init`. Scrolling, searching, filtering, and selecting only rebind those widgets; none of those operations reconstructs the screen or allocates widgets.
- Scroll offset advances one item per visual-row step over a contiguous column-major window. Slot mapping preserves the released column-first reading order. Empty slots are hidden and inactive.
- Search matches stable keys, ability IDs, and localized labels. Tabs cover favourites, recent actions, innate powers, crystals, and artifact-specific actions. Alignment remains the server-authored current artifact and is displayed/narrated as part of the result summary.
- Clicking a result selects it; clicking a numbered quick-wheel slot sends the existing authenticated `BindFavouritePayload`. Local favourites never change optimistically. A successful server bind returns an authoritative catalogue/menu refresh, making a searched action directly bindable in two clicks. The separate Select action keeps its existing authenticated `SelectPayload` behavior.
- Artifact execution requires the canonical action to be present in the server-owned favourite set in addition to the existing owner, alignment, rank, revision, option, context, limiter, payment, and effect checks.
- Recents are bounded to eight canonical keys per alignment at the persistence decode boundary, persisted server-side, updated only after a successful authoritative selection, reconciled against the current catalogue, and included in `OpenMenuPayload`.
- A newer compatible menu payload refreshes the open catalogue in place while retaining query/tab/scroll and a still-canonical selection. Every revision-carrying menu receiver rejects reordered older payloads; disconnect remains the revision-reset owner.

## Acceptance tests

1. RED then GREEN pure tests: 10,000 synthetic actions, constant pool size, deterministic visible mapping, search/filter/scroll, bounded recents, stable selection across a new revision, and two-click direct-bind contract.
2. Client tests: fixed widget allocation count across repeated scroll/search/filter/revision refresh, column-major mapping, keyboard focus/narration parity, hidden-focus clearing, and in-place scroll/selection preservation.
3. GameTest: the registered bind/select/commit packets preserve revision, authorization, rank, slot, favourite membership, and canonical-key validation; successful binding returns an authoritative refresh and successful selection records bounded recents.
4. Integrated client: real open/search/scroll/select/direct-bind/ack on the production screen, a 10,000-action production fixture, and visually inspected 1280×720 GUI-scale 2 and GUI-scale 3 captures.
5. Focused suites, `compileClientJava`, resource/document/source audits, full GameTests, and `check` must pass before both backlog rows are removed.

## Non-goals

- No client-authoritative selection, cast, rank, energy, cooldown, or alias decision.
- No replacement of the eight-slot quick wheel.
- No unbounded history, per-scroll widget creation, or eager rendering of all filtered entries.
