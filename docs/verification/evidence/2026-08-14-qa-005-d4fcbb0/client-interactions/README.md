# QA-005 production item/menu interaction evidence

- Build: `d4fcbb035841fff69f60f75e7b046348d7e07a4c`
- Runtime: Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25
- Participants: one real rendered Fabric client (`SmokeTest`) and one dedicated Fabric server
- Acceptance route: the development-only script drove vanilla item use and the production grimoire, crystal-selector, and artifact-commit packets. It did not call the abilities directly.

## Proven outcomes

- `spell/hearth_sanctuary`: the Wild Grimoire selected index 2, vanilla use reached the spell handler, and chat reported one independent forcefield.
- `crystal/life_bloom`: the Rainbow Crystal selected index 3 and vanilla use produced saturation, absorption IV, and regeneration V. The captured entity NBT records `show_particles: 0b` for all three effects.
- `artifact/unique/blight_ground`: the Shadow Sword commit packet selected and activated the rank-10 action; a server-side block predicate emitted `QA_BLIGHT_PRESENT` for `powers:darkness` beneath the caster.
- Final `/powers diagnose`: no forced chunks, travel loads, fields, proxies, celestial events, guardians, summons, or Shadow sessions leaked.

The exploratory vanilla attack step is intentionally excluded from accepted evidence: the summoned zombie used an obsolete custom-name representation and burned in daylight, so its result was not causal proof of the scripted attack operation.

`scenario.tsv`, both complete logs, three selected screenshots, and `SHA256SUMS` are retained beside this report.
