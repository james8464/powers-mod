# QA-006 restart/reconnect soak evidence

This bundle retains the accepted schema-3 report and all 576 server/client logs for the
24-hour, 288-cycle restart soak at implementation commit
`0e8a3d4efa3e61f09cb0146ba6b9a00bbb235021`. Every cycle proves readiness, connected client work, observed disconnect,
startup/seed/settle/status/rollover phases, clean owner diagnostics, and the expected clean or hourly
SIGTERM boundary. The run recorded 18967 client ability activations and
24 flushed SIGTERM boundaries.

`restart-soak-logs.tar.gz` is deterministic and contains privacy-sanitized UTF-8 logs. The exact raw
and retained hashes/sizes are recorded per member in `logs-index.json`; private home paths, dynamic
loopback endpoints, and UUIDs are replaced without removing diagnostic lines. `SHA256SUMS` covers every other
file in this directory.

This evidence closes the QA-006 work unit only. The selected programme's final acceptance still
requires a new complete 24-hour soak on the exact final release commit.
