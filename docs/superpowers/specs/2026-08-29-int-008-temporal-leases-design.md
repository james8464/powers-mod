# INT-008 temporal leases and explicit clocks design

## Goal

Make every selected temporal system state which clock it uses, replace implicit global Time Stop
ownership with source-scoped leases, and guarantee that POWERS never steals or releases an external
administrator/mod freeze.

INT-008 covers innate Time Freeze, Blue Crystal Chrono Stop, Shadow Time Freeze, vanilla projectile
and entity suspension, grimoire channels, spell fields, Celestial Ruin, and realm cycles. It does not
add local time bubbles, time dilation, rewind, new powers, or changes to the authored durations.

## Clock vocabulary

POWERS has exactly two authoritative server clocks:

- **Control clock:** `MinecraftServer#getTickCount()`. Server lifecycle callbacks continue to observe
  it while vanilla world simulation is frozen. Lease expiry, ownership reconciliation, energy drain,
  and sparse Time Stop presentation use this clock.
- **Frozen/world clock:** `ServerLevel#getGameTime()`. Gameplay durations and world cadence use it.
  Projectiles, entities, scheduled ticks, channels, spell fields, Celestial Ruin, and realm cycles
  must not advance while vanilla reports the server frozen.

`TemporalClocks` is the only common vocabulary. It exposes typed `ControlTick` and `WorldTick`
values plus `worldAdvances(server)`. The types reject negative values and cannot be interchanged in
deadline helpers. Production adapters read Minecraft state; pure tests construct values directly.

World-owned managers that still execute from the server-end callback must check `worldAdvances`
before mutation. This prevents modulo-based work from repeating every control tick while
`gameTime` is parked on a divisible value. External freezes and POWERS-owned freezes have identical
world-clock behavior.

## Lease model

`TimeStopLeaseRules` owns a pure immutable state machine. One server may have zero or one POWERS
lease. A lease records a monotonic runtime token, owner UUID, source (`INNATE`, `CRYSTAL`, or
`SHADOW`), acquired control tick, optional control deadline, and optional Shadow body UUID.

Acquisition succeeds only when there is no active POWERS lease and vanilla is not already frozen.
Each production start path receives its source-specific lease identity. Releases must match token,
owner, and source; a stale innate toggle cannot release a newer crystal or Shadow lease owned by the
same player.

The tick-manager mixin observes every `setFrozen` call. An internal write is associated with the
current lease token. Any write without that token is external, including a same-value `true` write,
and immediately marks the lease externally superseded. Reconciliation then retires POWERS state,
clears its toggle/journal, and leaves the external clock value untouched. A normal matching release
unfreezes only when vanilla is still frozen and the lease was never superseded.

Acquisition is transactional: journal first, then freeze. If persistence fails, no lease or clock
mutation remains. Release removes durable authority before writing the clock, then clears the
journal. Shutdown and disconnect use the same source-aware release path. At startup, a stale active
journal is retired without changing the vanilla clock: after a crash it is impossible to distinguish
a POWERS-owned freeze from a later external supersession whose journal retirement could not be
persisted. This conservative policy never thaws an administrator/mod freeze.

## Production behavior

Innate Time Freeze and Shadow Time Freeze have no deadline. Their authority is reconciled on each
control tick; energy drains every 20 control ticks. Chrono Stop expires after exactly 1,200 control
ticks even though world time is frozen. HUD remaining time and sustain effects use the same control
deadline, eliminating mixed-clock drift.

Vanilla remains responsible for suspending projectiles, entities, block entities, scheduled ticks,
and dimensions. POWERS adds no per-entity freeze list. A live GameTest proves a projectile remains
stationary under the lease and resumes after release.

Channels retain their existing world-time completion deadline and cannot complete during either a
POWERS or external freeze. Spell fields, Celestial Ruin countdown/destruction, realm event pressure,
realm event transitions, and Herald spawn cadence also use the frozen/world clock and execute no
repeated side effects while frozen. Player ownership reconciliation remains on the control clock so
the server can always escape a broken lease.

## Persistence and compatibility

`TimeStopSavedData` advances to schema 2 and stores the lease token, acquired control tick, optional
deadline, source, owner, and Shadow body. Schema-1 snapshots decode conservatively with token zero
and are treated as stale recovery journals, never as live resumable powers. Active Time Stop is not
resumed after restart because no player authority has yet been re-established.

Existing public entry points (`start`, `startCrystal`, `startShadow`, `stop*`, `mayAct`, and
`snapshot`) remain source-compatible. Their internals delegate to the lease manager. The HUD
snapshot retains owner/source/deadline/remaining fields and adds the lease token and clock kind for
diagnostics.

## Failure handling and bounds

- Lease state is one bounded record per server in an identity-keyed map.
- Tokens are positive saturating sequence values; wrap clears authority rather than reusing an
  active token.
- Malformed persisted UUIDs, sources, tokens, or deadlines are rejected into stale-journal cleanup.
- Persistence failure aborts acquisition; clear failure is logged and retried while process lease
  state remains. Startup retires stale authority without changing an ambiguous vanilla clock.
- External mutation always wins. POWERS emits one release presentation but never calls `setFrozen`
  on behalf of the external owner.
- No scan is added beyond the existing bounded player observer list and existing subsystem work.

## Verification

JUnit tests cover typed clock separation, world-advance gating, acquisition refusal, all three lease
sources, stale/mismatched release, normal release, external supersession, control deadlines,
saturating tokens, journal schema migration, and every subsystem's selected clock.

Fabric GameTests cover:

1. an administrator freeze blocks acquisition and remains frozen;
2. an external same-value write supersedes an active lease and remains frozen after cleanup;
3. Chrono Stop expires after 1,200 control ticks while world time stays fixed;
4. a projectile pauses and resumes without a POWERS-side entity ledger;
5. channel and Celestial Ruin progress remain unchanged under both owned and external freezes;
6. realm pressure/event work does not repeat at a parked divisible world tick; and
7. disconnect, death, dampening, Shadow loss, and shutdown release only the matching lease.

The evidence package contains exact-SHA server logs, structured temporal assertions, test totals,
source inventory, checksums, and privacy results. Acceptance requires focused gates, the literal
Java 25 full `check --rerun-tasks` gate, independent READY review, fast-forward merge, a second
literal gate on merged `main`, push, and clean synchronized worktrees.
