# POWERS integration API v1

`com.powers.api.v1` is the stable server-only integration boundary. Its semantic version is `1.0`:
minor releases may add methods/types, while removing or changing an existing binary signature requires
a new major package/version. It deliberately contains no `net.minecraft.client` references.

## Discovery and lifecycle

Declare a Fabric custom entrypoint named `powers:v1` whose value implements `PowersExtension`.
POWERS discovers at most 256 entries for each server epoch, reads their stable lowercase IDs, sorts by
ID, rejects duplicates, and invokes `register` on the server thread during `SERVER_STARTING`.
Registration closes before `SERVER_STARTED`. An exception or linkage failure rejects that extension
and rolls back only its action, protection, and hook registrations; other extensions continue.

Lifecycle hooks receive `SERVER_STARTED` and `SERVER_STOPPING` in registration order. Hook failures are
logged and isolated. At stop, external presences, protection adapters, actions, hooks, and extension IDs
are removed so an integrated-server reload begins a clean epoch. Late registrations return `LATE`.

## Authority and safety

- `registerAction` adds collision metadata to the same `MagicRuntime.catalogue()` used by POWERS casts;
  it does not install a separate mutable registry or grant a gameplay activation route.
- `castContext` accepts only a player attached to the bound server and an action registered in the
  current extension epoch. Extensions must derive requests from their own authenticated server-side
  entrypoints; client claims cannot construct an authorised context.
- Action energy and cooldown values are metadata for interoperability. The v1 API does not execute
  arbitrary actions, so it cannot bypass POWERS payment, permission, realm, targeting, or work-budget
  pipelines. An extension adding gameplay must keep those checks in its server-authoritative owner.
- Fixed presences require a live `ServerLevel`, registered action, finite coordinates, radius `0..128`,
  absolute server-tick expiry, and the bound server thread. They use `PhysicalMagicPresences` and
  `MagicRuntime`, and must be removed early with their opaque `PresenceHandle` when their effect ends.
- Protection services join the existing unanimous `PowerProtectionAdapters` chain. Any denial,
  runtime exception, or linkage failure denies the protected operation; no extension can weaken safe
  zones or another adapter's denial.

## Compatibility example

`src/exampleExtension` is a separate Gradle source set and Fabric test mod compiled only against main
output and the public v1 types. It registers `example_resonant_field`, a deny-at-x=13 protection
service, and a lifecycle hook. `ApiCompatibilityGameTests` discovers it through Fabric, creates an
authoritative cast context, registers/removes a real field presence, and observes the production
protection chain on a dedicated GameTest server.

No save codec or network protocol changes are introduced by API v1. Action and presence registrations
are runtime-only and therefore need no migration.
