# Command live acceptance — `7052ead`

All 32 registered `/powers` command rows were exercised through a rendered Fabric 26.2 client against a dedicated Fabric server built from commit `7052ead`. The scripts use the same offline test identity and persisted world so state changes can be checked before and after each route.

## Evidence sets

- `scenario-body-travel.tsv` proves ordinary body return, administrator recovery, and namespaced administrator travel. The server log records entry into Middleworld, successful return/recovery, Nether and Overworld arrivals, zero leaked travel resources, and a clean six-dimension save.
- `scenario-remaining-commands.tsv` exercises listing, slots, assignment, reroll, consent preferences, config reload, both rank paths, prefixes, boss lifecycle, Shadow learning reset, diagnostics, every testing override, coverage, telemetry, arena/actor lifecycle, and the profiler.
- `scenario-ruin-cancel.tsv` is the positive catastrophic-control case away from spawn protection. Diagnostics first report `celestialEvents=1`; `/powers ruin cancel` succeeds; the next diagnostic reports `celestialEvents=0`.
- `profiles/connected-1p-1m.json` and its JFR contain exactly 1,200 ticks over 59.873 seconds with one connected rendered client. The profile completed at 8.635 ms p95 and 9.964 ms p99 without data loss.

The initial Celestial Ruin attempt in the broad script deliberately landed inside spawn protection and rolled back. It is retained in the raw log as denial-path proof and is not used as the positive cancellation result; the dedicated follow-up supplies that proof.

## Environment

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3
- Fabric API 0.156.0+26.2
- Java 25
- macOS on Apple M3 Pro
- Dedicated server in offline mode, local rendered client on `localhost:25565`

`SHA256SUMS` covers every evidence artifact other than the checksum file itself.
