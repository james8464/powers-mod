# VFX-004 Transient Visual Scars Implementation Plan

> Accepted: all five tasks are implemented and verified by focused, real-client, visual, and full
> repository gates. Evidence is retained in `docs/verification/evidence/2026-08-26-vfx-004`.

## Task 1 — Pure contracts

- `VisualScarRulesTest`: split support/origin loaded checks, protection/material eligibility,
  presentation-only invariants, active/request/send caps, coalesced overlap.
- `VisualScarLedgerTest`: hierarchical lane-fair request selection, bounded TreeMap expiry, O(1) intrusive keyed-ring
  revalidation under 64 removals/inserts per tick, same-key churn, exact physical-node bounds, and
  mandatory 32-tick continuous-member coverage,
  remove-only stale support, epoch/dimension clear, movement/teleport resync paging and session checks.
- `ScarFxProtocolRulesTest`: exact eight-field wire with canonical RESET_DIMENSION, independent unsigned generations, permanent
  admission disable at exhaustion until real server restart, exact REMOVE, reorder/replay, client cap,
  validate-before-mutation, stale-handler rejection, receipt-local leases, freeze tick,
  epoch/dimension reset, reload continuity.
- `VisualScarDeliveryRulesTest`: guarded RESET-before-snapshot recovery, bounded session/pending ownership, latest-by-key and REMOVE precedence,
  tombstone-priority eviction with victim resync, held snapshots under revision churn, evolving nonempty authoritative convergence,
  reserved 192 live/64 resync fair share with unused lending, many-observer global fairness, stale
  session cleanup and convergence.
- `VisualScarGeometryTest`: closed unique 5x6 presentation profiles; five mechanically distinct motif
  meshes; profile-parameter sensitivity; six-face finite/epsilon/bounds/winding checks; exact
  `DEBUG_QUADS` topology/depth/cull state; near/mid/far silhouette retention; 16-quad scar,
  512-visible-scar, 8,192-quad/32,768-vertex frame ceilings; lightweight nearest-first selection
  before at most 512 mesh-factory calls; packed profile-derived RGBA preservation;
  one batched draw; and visibility.
- `VisualScarReachabilityTest`: exactly five callers; Fireball additive; loaded-check ordering; no
  mutation/persistence/tickets/registrations; guarded direct/fault-delayed session + capability
  rechecks, failure callback behavior, unsupported-client cancellation, and existing semantic network
  and client lifecycle hooks.

## Task 2 — Server and protocol

- Implement active maps/owner counts, bounded-only `TreeMap` expiry, key-coalesced hierarchical request lanes,
  `BlockWorkBudget` lanes, and an O(1) intrusive keyed revalidation ring with one node/key and a
  64-entry/tick inspection bound.
- Read state/BE only after independent support/origin `LoadedChunks.contains`; protection/read-only
  classification only. No mutation or tickets.
- Add exact `ScarFxPayload` registration/direct delivery beside existing semantic FX. Generation
  allocator permanently disables new scars at exhaustion until actual server restart. Derive remaining
  lease at each send.
- Implement observer session identity, `ChunkSpatialIndex` movement entry detection, bounded resync
  cursors, tombstone-priority coalescing delivery queues, and lifecycle cancellation. The 256 global
  fair drain reserves 192 live-dirty and 64 revision-bound stable-key resync sends under contention,
  lends unused share, and partitions shared authoritative rows by observer dimension.
  Persist one positive monotonic delivery epoch for each exact tracked session. A distinct resync pass
  advances it, while repeated triggers during RESET or paging coalesce into one later pass without
  invalidating the in-flight barrier. Await successful guarded RESET_DIMENSION delivery, then page one
  held immutable dimension snapshot that cannot contain RESET rows; newer revisions schedule a later pass. Route sends
  through `PowersPlayNetworking.sendGuarded` with success and failure callbacks, rechecking both the
  observer session and `canSend` in direct and packet-fault-delayed callbacks immediately before
  Fabric send. Unsupported clients are cancelled permanently; current-session failures mark bounded
  resync while stale failures discard.

## Task 3 — Client and renderer

- Implement 2,048-cap semantic manager with connection epoch, RESET_DIMENSION clearing, unsigned generation ordering,
  exact-generation REMOVE, validation-before-mutation, at-cap existing-key updates, stale-handler
  rejection, receipt-local lifecycle expiry, and hide-without-delete visibility.
- Disconnect/server reconnect and dimension change reset state. Resource reload preserves records and
  closes/recreates renderer resources only.
- Build bounded motif meshes from thin surface-aligned quads through built-in
  `RenderPipelines.DEBUG_QUADS` (`POSITION_COLOR`, `QUADS`, translucent, no-cull, reverse-depth,
  no depth write). Drive topology, colour, segment count, stroke, inset, and variation from the closed
  30-entry `VisualScarPresentation` table. Keep each scar at no more than 16 quads/64 vertices; select
  at most 512 visible scars nearest-first; keep the frame below 8,192 quads/32,768 vertices; and upload
  every accepted mesh through one batch/draw. Near/mid/far LODs must retain each motif's silhouette.

## Task 4 — Callers

- Add requests only to Energy Beam, Thunderclap, Breezy Bash, Ice Manipulation, and Fireball impact.
- Preserve Fireball ordinary fire plus all existing destruction/thermal behavior.

## Task 5 — Acceptance

- Focused GameTests and packet fault suites, including 6-tick reorder, custom Time Freeze, generation
  exhaustion/restart, actual fault-delayed reconnect/dimension switch, unsupported capability, false
  predicate, injected loss, queue overflow, expiry, 1%/5% profiles through actual active-client
  convergence, mid-page session changes, movement/teleport convergence, flood caps, resource reload.
- Multiplayer visual inspection captures the actual rendered motif geometry—not keys or colour
  swatches—for the exact 5-impact by 6-material matrix and representative full-resolution views across
  compatibility settings, including an opaque wall between camera and scar to prove depth occlusion.
- Full ordinary gates, documentation/evidence, backlog removal, cohesive commit.
