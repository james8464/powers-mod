# VFX-004 — Transient Material-Aware Visual Scars

Status: accepted on the exact implementation commit; production runtime, real fault-path recovery, visual matrix,
resource-reload continuity, opaque-wall occlusion, and full repository gates are GREEN.

## Presentation-only boundary

Energy Beam, Breezy Bash/slam, Thunderclap, Ice Manipulation, and Fireball may request temporary
surface visuals. Support terrain is never mutated. There is no marker block/entity/block entity,
loot, collision, occlusion, fluid/neighbour work, SavedData, recovery, or chunk ticket. Fireball's
ordinary-fire gameplay remains unchanged and scars are not added to shared crater/ray helpers.

The server reads only already-loaded state. It must prove `LoadedChunks.contains` independently for
the support position and face-adjacent visual origin before either position's `getBlockState` or
`getBlockEntity`. Admission models the two positions independently: `supportLoaded`, `originLoaded`,
`supportPolicy`, `supportBE`, `supportFluid`, `sturdy`, `classifiable`, `originOpen`, `originBE`, and
`originFluid`. `PowerProtection.blockDecision` must allow the support. Each denial is distinguishable,
including cross-chunk support/origin cases. Exact state/tags classify one of STONE, EARTH, WOOD,
METAL, SAND, or COLD.

## Server epoch state and work

An active record holds dimension, support position/face, owner, impact/material, visual seed,
server-owned positive monotonic generation, canonical support fingerprint, created tick, and
absolute server expiry. Limits are hard ceilings:

- active: 128/owner, 2,048 global;
- queued requests: 128/owner, 2,048 global, latest coalesced by support key;
- inspections: 64 requests and 64 active support revalidations/tick;
- delivery: 256 global sends/tick;
- pending delivery: 2,048 latest-by-key entries/observer, 32,768 globally;
- lifetime: 40–1,200 server ticks.

Requests use one bounded `VisualScarRequestQueue` whose stable lanes combine dimension,
protection-policy provider, owner, and impact, rotating one request per lane. Active expiry is reachable only through a
bounded `TreeMap<Long, LinkedHashSet<Key>>` poll; no full-ledger expiry scan API exists. Mandatory loaded-only
revalidation uses an O(1) intrusive keyed ring backed by `Map<Key, Node>`. Exactly one physical node
exists per key; insert, remove, and reinsert update exact links with no lazy nodes or duplicates.
Physical nodes always equal membership and remain at most 2,048. This survives 64 removals/inserts per
tick, including same-key cycles around the current node, while at most 64 inspections/tick cover every
continuously present member within 32 ticks. Changed or invalid loaded support produces REMOVE; either
unloaded position retains the record without loading.
Server stop/restart and dimension unload discard epoch state.

## Exact wire and generation contract

`ScarFxPayload` has exactly eight fields:

`operation, position, face, impact, material, visualSeed, generation, leaseTicks`.

Operations are CREATE_OR_UPDATE, exact-generation REMOVE, and canonical RESET_DIMENSION. RESET uses
the same fields with zero position/face/impact/material/seed, its delivery generation, and lease 1.

Generation is independent of seed and time, positive, server-owned, and compared with strict
unsigned ordering. CREATE_OR_UPDATE duplicates are idempotent; a strictly newer generation replaces;
older/reordered updates are ignored. REMOVE applies only to the exact generation. Same-tick same-seed
recreation is therefore distinct, while different seeds never define ordering.

The positive counter never wraps or reuses in a running server. Allocating at `Long.MAX_VALUE`
permanently enters `DISABLE_NEW_SCARS_UNTIL_SERVER_RESTART`; expiry and exact REMOVE delivery for
existing records continue, but no new scar generation is admitted. Only an actual server restart,
which necessarily disconnects clients and resets their connection epochs, starts a fresh allocator at
generation 1.

Server records retain absolute expiry, but every send derives `leaseTicks = clamp(expires-now,1,1200)`.
The client converts the lease to its receipt-local lifecycle tick. This is independent of arbitrary
server/client clock offsets and continues during custom Time Freeze. Exact server REMOVE is
authoritative; local expiry is only a fail-safe and is never used to declare server convergence.

## Observer sessions, movement, and bounded delivery

Each observer session key contains player UUID, connection identity, dimension, and session
generation. Delivery uses `PowersPlayNetworking.sendGuarded(player, payload, sessionPredicate,
successCallback, failureCallback)`. Both its direct branch and the callback invoked by packet-fault delay/reorder
re-evaluate that exact session predicate and `ServerPlayNetworking.canSend(current, payload.type())`
immediately before `ServerPlayNetworking.send`. Unsupported capability cancels permanently without
retry or resync. Other failure paths invoke the callback: a still-current observer is marked for
bounded resync, while a stale session is discarded. Each tracked session owns one positive,
monotonic delivery epoch that remains authoritative after a pass completes and is removed only by
exact session cancellation. Starting a distinct resync pass advances that epoch and invalidates older
delayed guards; repeated overflow or movement while RESET or snapshot paging is active only marks one
coalesced follow-up pass and cannot invalidate the in-flight RESET. Each pass queues one
highest-priority RESET_DIMENSION. RESET retries on failure; snapshot CREATEs begin only after its
exact guarded-send success callback. Authoritative snapshots reject RESET rows, and send callbacks
carry the exact immutable send rather than a boolean-current shortcut. Logout, respawn, dimension change,
or connection replacement therefore cancels stale pending work, including a fault-delayed tail.

Join/dimension hooks start bounded resync, and `ChunkSpatialIndex` plus a bounded player-movement
cursor detects walking/teleporting into observation range. Each pass holds one shared immutable
dimension snapshot reference and stable key, never active-record copies per observer. Every held row
is checked against current authority before send: removed rows skip and a current tombstone wins.
Revision changes schedule a following pass after the held tail instead of restarting it. Expired
records are skipped page-by-page.

Pending delivery coalesces latest-by-key per session. Repeated UPDATE replaces its pending predecessor;
exact-generation REMOVE takes precedence; a strictly newer CREATE may supersede an older REMOVE.
At either pending cap, a REMOVE evicts/coalesces a CREATE where possible and marks `needsResync`.
All-tombstone global saturation may defer a REMOVE and must retain bounded resync work. The
receipt-local maximum 1,200-tick lease remains a client safety fallback, but neither tests nor runtime
may use lease expiry to declare convergence. Authoritative convergence remains
revisioned, stable-key paged, dimension-partitioned, and equal while records are still active. When
live dirty work and resync coexist, the fair
drain reserves 192 sends for live work and 64 for resync, lends unused capacity between lanes, rotates
observers/dimensions/owners, and never exceeds 256. Ongoing live work therefore cannot prevent resync
from refilling, clearing its flags, and converging connected observers.
Overflow never materialises active-by-observer copies; disconnect cleanup removes all session entries.
Tracked-session admission and ownership are O(1), use one exact bounded membership index, and do not
allocate unions of pending and resync owners.

## Client state and rendering

`ClientVisualScarManager` validates before mutation and caps active state at 2,048. A current guarded
RESET_DIMENSION clears the scoped dimension/session state before snapshot CREATEs. A 2,049th new key
is rejected while a valid update to an existing key at cap remains legal. Its connection epoch resets
on reconnect/server restart; dimension change clears that dimension view. A callback captured under
an old connection/dimension epoch is ignored after reset. Duplicate/reordered payloads obey generation
rules. Receipt-local lifecycle ticks advance even while world game time is frozen. Six-tick delivery
delay does not depend on server/client tick alignment.

Resource reload does not clear semantic records. Only renderer GPU buffers close/recreate; records
continue and render again after resources return. Disconnect/dimension reset and exact REMOVE clear.

`VisualScarPresentation` is a closed 5-by-6 profile table. Its distinct impact motifs are linear rune,
radial crack, forked wave, frost branch, and ember ring; its distinct material bases are stone, earth,
wood, metal, sand, and cold. All 30 combinations are present and unique, with bounded opaque RGB,
alpha, segment count, stroke, inset, and seeded variation, and no texture or custom shader.

The renderer consumes that profile and uses MC 26.2's built-in `RenderPipelines.DEBUG_QUADS`:
`POSITION_COLOR`, `QUADS`, translucent, no-cull, reverse-depth test, and no depth write. Pure geometry
uses thin surface-aligned quads to construct mechanically distinct meshes for linear rune, radial
crack, forked wave, frost branch, and ember ring. `VisualScarMotifGeometry` consumes segment, stroke,
inset, variation, and colour fields from the selected profile; changing any geometric profile field
changes emitted vertices. Every primitive is finite, nondegenerate, outward-epsilon, within the scar
bounds, and consistently wound on all six faces.

Hard renderer ceilings are 16 quads/64 vertices per scar and 512 nearest visible scars, therefore
8,192 quads/32,768 vertices per frame. Near, mid, and far LODs retain a nonzero recognisable topology
while monotonically reducing bounded detail. Lightweight candidates are selected deterministic
nearest-first with stable-key tie-breaking before any mesh factory call; no more than 512 meshes or
32,768 attempted vertices are generated. Packed RGBA derives from both profile colours and alpha and
is preserved into the one `DEBUG_QUADS` batch/draw. Range, frustum, client
chunk availability, and current client support-face validity hide a mesh without deleting its semantic
record. Runtime visual acceptance captures the actual motif shapes in the exact 30-cell presentation
matrix plus representative full-resolution views and proves a scar behind an opaque wall is occluded
rather than bleeding through it.

## Acceptance sequence

1. Pure RED/GREEN rules, ledger, protocol, delivery, and geometry contracts.
2. Server epoch owner, exact payload, session/movement resync, bounded queues.
3. Client semantic manager and renderer/resource lifecycle.
4. Exactly five caller migrations with Fireball gameplay preserved.
5. GameTests cover unsupported clients, false predicates, injected loss, queue overflow, expiry, and
   1%/5% packet-fault profiles until actual active-client convergence. Multiplayer movement/reconnect/
   dimension tests and the exact 30-cell visual matrix follow before full gates and documentation.

No completion claim is permitted before all five steps pass.
