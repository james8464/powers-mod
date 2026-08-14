# QA-005 targeted-spell evidence

- Build: `b0e3f2c193b2e62e7fee5370818c2e2448def70f`
- Runtime: Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25
- Participants: one real rendered Fabric client (`SpellTargeter`) and one dedicated Fabric server
- Route: production grimoire-selection packets followed by vanilla main-hand use

## Accepted observations

- Blood Reading locked a stationary living target and reported its 10/10 health, zero armour, ordinary alignment, and empty effect list.
- Ward-Breaking was cast from 25 blocks away, outside the configured 20-block suppression radius. At six blocks after the channel, the caster had no active effects; the server then proved that the ward remained present and powered at level 15 (`QA_WARD_STILL_POWERED`).
- Dispel locked a stationary cow carrying Resistance and removed the effect; the following authoritative entity-data query found no `active_effects` elements.
- Final diagnostics reported no forced chunks, travel loads, fields, proxies, celestial events, guardians, summons, or Shadow sessions.

The Dimensional Anchor attempt is retained but not accepted: the hostile-forced-movement policy correctly rejected the non-allied test actor. It remains pending for an allied-target rerun.

The selected screenshots, complete logs, exact scenario, and checksums are retained beside this report.
