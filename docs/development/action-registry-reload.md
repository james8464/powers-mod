# Action registry reload and network contract

`MagicRuntime.catalogue().snapshot()` is the single immutable action view. It contains the monotonic process-local revision, stable canonical definitions, aliases, and successful validation counts. Built-ins and API v1 external actions use this same publication boundary.

Server datapacks may provide `data/<namespace>/powers_actions/<name>.json`:

```json
{"aliases":{"retired_fire":"fireball","older_fire":"retired_fire"}}
```

Resources merge in identifier order. Duplicate aliases, malformed keys, canonical collisions, unknown targets, cycles, chains over 16 steps, or more than 256 aliases reject the whole reload. Preparation is read-only and publication occurs on the server reload apply phase; failure preserves the prior object and revision.

Artifact wheel/catalogue/cycle/teleport, grimoire, and Rainbow submissions carry the menu revision and canonical key. The server validates both before rate limiting or gameplay mutation. A stale, future, unknown, alias, or mismatched submission spends no energy, starts no cooldown, changes no selection, and casts nothing; its owner sends one current authoritative menu snapshot.

`PreparedMagicCast` captures the accepted `ActionRegistrySnapshot`. A delayed/channelled cast therefore retains its exact definition and revision through completion. Existing lifecycle interruption remains the sole cancellation owner if ordinary safety checks make continuation impossible.

Persistent artifact selections and favourites canonicalize and deduplicate through the alias resolver. Spell and crystal owners persist stable action IDs in additive attachments while retaining the released integer maps as legacy decode fallbacks. Saves without the additive fields use existing deterministic defaults and are upgraded on the next explicit selection or alias resolution.
