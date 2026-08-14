# QA-005 Celestial locator acceptance

Exact tested commit: `5fcb14201d351135808f66e4694e029dc35e6cf3`.

One rendered Fabric 26.2 client used the Celestial Grimoire's real Soul Compass screen to submit the unique loaded mob name `SoulWitness`. The server resolved the indexed target without a fallback scan, moved the caster into the remote view, and reported one vulnerable body proxy plus one forced chunk. The client was then disconnected while detached. Server diagnostics immediately returned to zero proxies and zero forced chunks.

The same offline identity reconnected through a second rendered client. The authoritative entity query showed Creative game mode (`playerGameType: 1`), no `powers:mind_body`, and no proxy or forced-chunk residue. That client selected Cartographer's Star, submitted `biome minecraft:plains` through the actual locator screen, and received the loaded authoritative result `minecraft:overworld · 0, 63, 0` without forcing a search chunk.

Evidence:

- `soul-client.log.gz` and `soul-scenario.tsv`: exact rendered Soul Compass actions and private result.
- `cartographer-client.log` and `cartographer-scenario.tsv`: reconnect and rendered Cartographer's Star actions/result.
- `server.log`: indexed-name query, active proxy/ticket diagnostics, disconnect cleanup, restored game mode, absent mind-body state, clean final diagnostics, save, and orderly stop.
- Four PNGs: the real input/result screens for both spells.

The earlier underground fixture attempt was rejected and is not included. This directory contains only the corrected known-air run from the exact commit above.
