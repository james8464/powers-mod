# QA-005 practical-spell evidence

- Build: `6879fd4564504c98ed86348cc44f665313722442`
- Runtime: Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25
- Participants: one real rendered Fabric client (`SpellCaster`) and one dedicated Fabric server
- Route: production grimoire-selection packets followed by vanilla main-hand use

## Accepted observations

- Augury reported the authoritative weather, moon phase, and loaded Light/Dark force counts after its channel.
- Grave Recall followed a real player death and respawn, then reported `minecraft:overworld`, `10, 101, 0`, and a north-west bearing.
- Purification Circle removed the deliberately seeded Poison III effect; the following entity-data command found no `active_effects` elements.
- Verdant Tending reported that it renewed two prepared blocks (dry farmland and immature wheat).
- The clean shutdown followed diagnostics with no forced chunks, travel loads, fields, proxies, celestial events, guardians, summons, or Shadow sessions.

## Rejected observations

Dimensional Anchor, Blood Reading, Ward-Breaking, and Dispel are present in the retained script/log for auditability, but are not accepted here: the first target moved out of the aim line, the two cow rays were vertically offset, and the ward attempt began inside the configured 20-block suppression radius. Corrected isolated runs are required before those checklist rows pass.

The selected screenshots, full client/server logs, exact scenario, and checksums are retained beside this report.
