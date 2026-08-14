# QA-005 innate-power evidence

This bundle closes the manual live row for every registered innate action. The four collision-oriented rows already proven by four independent clients remain linked to build `7455a02` in the acceptance ledger; this bundle records the other nineteen actions without relabelling earlier builds as the final build.

## Exact builds

- `3491749`: rendered movement, Lightning Strike, Telekinesis, Breezy Bash, Invisibility, and Astral Projection evidence.
- `86a45d1`: Fireball, Gravity Repel, Ice Manipulation, Energy Drain, Vessel Possession, Plant Healing, and the corrected two-times Double Health baseline.
- `956ad4f`: retained-toggle fix plus final Starfall, Size Morphing, Time Freeze, and distant Time Shift proof.

Minecraft was `26.2`, Fabric Loader `0.19.3`, Fabric API `0.156.0+26.2`, Java `25`, and POWERS `1.0.2`. Runs used a real rendered Fabric client connected over localhost to a dedicated Fabric server in offline development mode. Energy and cooldown limits were disabled only through the authenticated testing command; normal targeting, casting, movement, collision, state ownership, and cleanup remained active.

## Authoritative observations

- Starfall reduced `FinalTarget` from `100` to `98.81566` health.
- Fireball reduced `RetryTarget` from `100` to `96.493256` health.
- Gravity Repel produced target z-motion `0.830769`.
- Ice Manipulation reduced health to `94.56`, set `TicksFrozen=120`, and created Weakness/Slowness with `show_particles:0`.
- Energy Drain created `powers:exhaustion` for `580` ticks with `show_particles:0`.
- Vessel Possession entered `playerGameType=3` with one proxy and one forced chunk, moved the controlled target from z=`4.5` to z=`11.496`, then returned to `playerGameType=0` with zero proxies and forced chunks.
- Plant Healing satisfied the live `PLANT_GROWN` block predicate.
- Double Health changed maximum health `20 → 40 → 20`.
- Size Morphing changed scale `1.0 → 0.5`, retained `0.5` across loadout reconciliation, then restored `1.0`.
- Time Freeze made vanilla `tick query` report `frozen`; owner release made it report `running normally`.
- Time Shift accepted the exact unloaded destination `(192.5,101,0.5)`. The caster and nearby `TravelMate` both arrived at x=`192.5`; the caster subsequently fell because explicit coordinate travel intentionally performs no environmental safety correction. Diagnostics then reported `travelLoads=0`, `proxies=0`, and `forcedChunks=0`.

Each scenario file is the exact ordered input sent by the development-only client agent. The archived server logs prove real connections, target creation, disconnects, and clean server stops. Screenshots were inspected at their native 1708×960 resolution. `SHA256SUMS` makes every captured artifact tamper-evident.

This is an innate-action slice of QA-005, not a claim that the entire 429-row release checklist is complete.
