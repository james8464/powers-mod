# QA-005 Celestial Ruin acceptance

Exact tested commit: `3f07ba4d09f33e3881ece9fc6b2dceba34d72bc8`.

Two rendered Fabric 26.2 client passes exercised the real Celestial Grimoire selection packet, vanilla item-use route, block targeting, 200-tick channel, and persistent Heavenfall event. The first pass remained at the focus so server diagnostics and entity state could be measured. It recorded one active event, one successful catastrophic-ritual audit, and an intact First Vessel at `1024.0f` before impact. The post-impact query found no First Vessel. During bounded crater processing the event held 361 temporary chunks; after completion the server reported `celestialEvents=0`, `forcedChunks=0`, and `orphanedRuinEvents=0`.

The second pass repeated the production cast and moved only after channel completion to a side-view platform 120 blocks from the focus. Its screenshots show the long-distance atmosphere column, layered orbiting runes, one-minute countdown, increasing whiteout, impact, and destructive aftermath. The blast displaced the observer and destroyed the staged centre while the no-AI First Vessel again fell from `1024.0f` to absent. The server then saved every dimension and stopped cleanly.

Evidence:

- `functional-scenario.tsv` and `functional-client.log.gz`: activation, diagnostics, exact boss-health queries, impact, and aftermath.
- `visual-scenario.tsv` and `visual-client.log`: independent long-distance presentation pass and second exact boss result.
- `server.log`: both successful ritual audits, clean event/ticket cleanup, and orderly save/stop.
- `screenshots/`: channel, near-field and side-view columns, countdown, whiteout/impact, and aftermath frames.
- `SHA256SUMS`: checksums for every evidence file other than the manifest itself.

Two earlier exploratory fixtures were rejected: one had no floor and one was underwater, so ordinary movement interrupted their channels. Neither is included here.
