# Action registry reload and network contract

`MagicRuntime.catalogue().snapshot()` is the single immutable action view. It contains the monotonic process-local revision, stable canonical definitions, aliases, and successful validation counts. Built-ins and API v1 external actions use this same publication boundary.

Server datapacks may provide `data/<namespace>/powers_actions/<name>.json`:

```json
{"aliases":{"retired_fire":"fireball","older_fire":"retired_fire"}}
```

Aliases may also retain an authored menu namespace, for example `innate/old_fire` to `innate/fireball`, `crystal/old_inferno` to `crystal/inferno`, `unique/old_call` to `unique/call_hollowed`, or `dominion/old_host` to `dominion/host_heaven`. The canonical resolver validates the underlying typed action while preserving the complete qualified key; namespaces are never stripped or folded together.

Resources merge in identifier order. Duplicate aliases, malformed keys, canonical collisions, unknown targets, cycles, chains over 16 steps, or more than 256 aliases reject the whole reload. On cold start, the registered reload listener parses and stages aliases, then atomically publishes them after installed API extensions register during `SERVER_STARTING`; aliases may therefore target installed external actions. Later resource reloads publish directly from the listener apply phase. Any validation failure preserves the prior snapshot object and revision.

Artifact wheel/catalogue/cycle/bind/teleport, grimoire, and Rainbow submissions carry the menu revision and canonical key. One shared production service validates revision, canonical spelling, held owner, alignment/book/mode membership, rank, option, slot, direction, target, and finite coordinates before rate limiting or gameplay mutation. A stale, future, unknown, alias, or mismatched submission spends no energy, starts no cooldown, changes no selection, and casts nothing; its owner sends exactly one current authoritative menu snapshot or explicit invalidation when that owner disappeared.

`PreparedMagicCast` captures the accepted `ActionRegistrySnapshot`. A delayed/channelled cast therefore retains its exact definition and revision through completion. Existing lifecycle interruption remains the sole cancellation owner if ordinary safety checks make continuation impossible.

Persistent artifact selections and favourites canonicalize and deduplicate through the alias resolver. Spell and crystal owners persist stable action IDs in additive attachments while retaining the released integer maps as legacy decode fallbacks. Saves without the additive fields use existing deterministic defaults and are upgraded on the next explicit selection or alias resolution.
