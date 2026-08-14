# QA-005 crystal acceptance — `aaf08ac`

This bundle records a rendered Fabric-client campaign against an isolated dedicated
Minecraft 26.2 server built from commit `aaf08ac`. The client used the production
item-use, sneak-mode-selection, input, networking, dimension, body-proxy, damage,
effect, and tick-freeze paths. Testing mode disabled only energy and cooldown
limits; protection, targeting, collision, realm, damage, and cleanup rules remained
authoritative.

## Result

All eight previously pending crystal actions passed:

- Inferno damaged and ignited a 100-health player-like target.
- Clone Swarm created exactly three 80-health combat echoes.
- Creativity Manifestation placed its selected creation-chamber blueprint.
- Yellow Size Shift reached scale `0.0625`; all supporting effects used
  `show_particles:0b`.
- Soul Link transferred a bounded damage share without recursion.
- Chrono Stop acquired and released the vanilla tick-freeze lease.
- Dreamwalking entered spectator control, moved its host, returned through
  spectator right-click, and cleared its proxy and ticket.
- Middleworld transported a nearby cow. The cow was then moved six blocks
  horizontally and four blocks vertically away from the caster; return still
  restored both travellers to their exact Overworld origins. Final diagnostics
  reported `proxies=0`, `travelLoads=0`, and `forcedChunks=0`.

The server then saved all six dimensions and stopped cleanly. The client log's
HTTP 401 profile-key and Realms-authentication messages are expected for an
offline development account and did not affect resource reload or multiplayer
gameplay. The earlier non-operator attempt is not in the client log in this
bundle and is not counted as evidence; the raw server log retains it for audit
transparency, while the accepted session begins at `03:40:45`.

## Contents

- `scenario.tsv`: deterministic connected-client actions and capture points.
- `logs/client.log`: complete accepted client run.
- `logs/server.log`: complete isolated server run and clean shutdown.
- `screenshots/`: named rendered checkpoints for every tested action.
- `SHA256SUMS`: exact checksums for every other file in this bundle.
