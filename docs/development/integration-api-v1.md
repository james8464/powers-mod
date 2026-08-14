# POWERS integration API v1

External registrations join the same immutable action snapshot used by built-ins and datapack aliases. Registration and lifecycle removal publish a new monotonic revision atomically; callers that captured a prior `PreparedMagicCast` retain its exact definition/revision rather than consulting a partially changed registry. Datapack alias reload does not bypass the v1 registration, protection, payment, cooldown, targeting, or work-budget contracts.

`com.powers.api.v1` is the stable server-only integration boundary. Its semantic version is `1.0`:
minor releases may add methods/types, while removing or changing an existing binary signature requires
a new major package/version. It deliberately contains no `net.minecraft.client` references.

## Discovery and lifecycle

Declare a Fabric custom entrypoint named `powers:v1` whose value implements `PowersExtension`.
POWERS inspects at most 256 entries for each server epoch, reads their stable lowercase IDs, sorts by
ID, rejects duplicates, and invokes `register` on the server thread during `SERVER_STARTING`.
Registration closes before `SERVER_STARTED`; started hooks are emitted only by Fabric's actual
`SERVER_STARTED` event. An exception, linkage failure, or registration-limit breach rejects that
extension and rolls back only its action, protection, and hook registrations; other extensions continue.

Each extension may register at most 64 actions, 16 protection services, and 16 hooks. Epoch totals are
bounded to 512 actions and 256 each of protections and hooks. Reaching a limit returns `LIMIT` and
rejects the entire current extension transaction, even if extension code ignores the result.

Lifecycle hooks receive `SERVER_STARTED` and `SERVER_STOPPING` in registration order. Hook failures are
logged and isolated. At stop, external presences, protection adapters, actions, hooks, and extension IDs
are removed so an integrated-server reload begins a clean epoch. Late registrations return `LATE`.

## Authority and safety

- `registerAction` adds collision metadata to the same `MagicRuntime.catalogue()` used by POWERS casts;
  it does not install a separate mutable registry or grant a gameplay activation route.
- `castContext` accepts only the exact connected, alive, non-removed player-list instance on the bound
  server and an action registered in the current extension epoch. The returned object is opaque,
  epoch-bound, and one-shot; forged, replacement, reused, or post-stop contexts are rejected.
- Fixed presences derive actor, owner, and action exclusively from that context. They must remain in
  the actor's current level and within the registered action's range, radius, and lifetime bounds.
  Registration checks spectator/suppression state, safe zones, unanimous protection, canonical magic
  collisions, authoritative energy and cooldown state, and per-tick work budgets before entering
  `PhysicalMagicPresences`. Payment is refunded if physical registration fails.
- At most 128 live presences per extension and 1,024 per epoch are retained. A player may attempt four
  accepted presence commits and the server 64 per tick. Ended effects must release their opaque
  `PresenceHandle`; server stop clears all remaining handles and budgets.
- Protection services join the existing unanimous `PowerProtectionAdapters` chain. Any denial,
  runtime exception, or linkage failure denies the protected operation; no extension can weaken safe
  zones or another adapter's denial.

## Compatibility example

`src/exampleExtension` is a separate Gradle source set and Fabric test mod compiled only against main
output and the public v1 types. It registers `example_resonant_field`, a deny-at-x=13 protection
service, and a lifecycle hook. `ApiCompatibilityGameTests` discovers it through Fabric and proves
actual STARTED delivery, forged/stale/synthetic actor rejection, location bounds, protection denial,
energy/cooldown/work budgets, and accepted physical-field registration/removal on a dedicated server.

No save codec or network protocol changes are introduced by API v1. Action and presence registrations
are runtime-only and therefore need no migration.
