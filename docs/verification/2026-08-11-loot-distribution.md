# Additive loot-distribution acceptance

Every POWERS injection is a separate one-roll pool, so foreign loot weights cannot dilute it. Results below use 200,000 deterministic triggers per pool. “Items/hour” uses an explicit 60 matching table triggers/hour comparison baseline; actual play rates depend on the structure, mob, block and server.

| Loot table | Authored chance | Simulated chance | Items / 1,000 triggers | Items / baseline hour | Same-item consecutive-trigger chance | Foreign weight 50,000 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `minecraft:chests/simple_dungeon` | 0.220 | 0.221 | 220.95 | 13.26 | 4.8400% | identical |
| `minecraft:chests/abandoned_mineshaft` | 0.140 | 0.141 | 140.98 | 8.46 | 1.9600% | identical |
| `minecraft:chests/ancient_city` | 0.100 | 0.100 | 100.36 | 6.02 | 1.0000% | identical |
| `minecraft:chests/stronghold_library` | 0.120 | 0.121 | 120.66 | 7.24 | 1.4400% | identical |
| `minecraft:chests/bastion_treasure` | 0.080 | 0.080 | 80.27 | 4.82 | 0.6400% | identical |
| `minecraft:chests/end_city_treasure` | 0.040 | 0.040 | 40.42 | 2.43 | 0.1600% | identical |
| `minecraft:chests/desert_pyramid` | 0.100 | 0.100 | 100.36 | 6.02 | 0.5000% | identical |
| `minecraft:chests/jungle_temple` | 0.080 | 0.080 | 80.27 | 4.82 | 0.3200% | identical |
| `minecraft:chests/woodland_mansion` | 0.045 | 0.045 | 45.23 | 2.71 | 0.1013% | identical |
| `minecraft:chests/ruined_portal` | 0.070 | 0.070 | 70.36 | 4.22 | 0.2450% | identical |
| `minecraft:chests/nether_bridge` | 0.060 | 0.060 | 60.21 | 3.61 | 0.1800% | identical |
| `minecraft:chests/buried_treasure` | 0.120 | 0.121 | 120.66 | 7.24 | 0.4800% | identical |
| `minecraft:blocks/birch_leaves` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0625% | identical |
| `minecraft:blocks/cherry_leaves` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0625% | identical |
| `minecraft:blocks/jungle_leaves` | 0.040 | 0.040 | 40.42 | 2.43 | 0.0320% | identical |
| `minecraft:blocks/melon` | 0.350 | 0.352 | 351.53 | 21.09 | 3.0625% | identical |
| `minecraft:blocks/oak_leaves` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0208% | identical |
| `minecraft:blocks/pumpkin` | 0.350 | 0.352 | 351.53 | 21.09 | 12.2500% | identical |
| `minecraft:entities/cod` | 0.450 | 0.451 | 450.72 | 27.04 | 20.2500% | identical |
| `minecraft:entities/cow` | 0.650 | 0.651 | 651.23 | 39.07 | 42.2500% | identical |
| `minecraft:entities/fox` | 0.250 | 0.251 | 250.58 | 15.03 | 0.6250% | identical |
| `minecraft:entities/horse` | 0.650 | 0.651 | 651.23 | 39.07 | 42.2500% | identical |
| `minecraft:entities/husk` | 0.350 | 0.352 | 351.53 | 21.09 | 3.0625% | identical |
| `minecraft:entities/mooshroom` | 0.650 | 0.651 | 651.23 | 39.07 | 42.2500% | identical |
| `minecraft:entities/pig` | 0.650 | 0.651 | 651.23 | 39.07 | 14.0833% | identical |
| `minecraft:entities/salmon` | 0.450 | 0.451 | 450.72 | 27.04 | 20.2500% | identical |
| `minecraft:entities/slime` | 0.150 | 0.151 | 150.74 | 9.04 | 2.2500% | identical |
| `minecraft:entities/villager` | 0.650 | 0.651 | 651.23 | 39.07 | 2.1125% | identical |
| `minecraft:chests/village/village_plains_house` | 0.220 | 0.221 | 220.95 | 13.26 | 0.0712% | identical |
| `minecraft:chests/stronghold_corridor` | 0.120 | 0.121 | 120.66 | 7.24 | 0.1108% | identical |
| `minecraft:chests/trial_chambers/reward_common` | 0.100 | 0.100 | 100.36 | 6.02 | 0.0625% | identical |
| `minecraft:archaeology/desert_well` | 0.080 | 0.080 | 80.27 | 4.82 | 0.0152% | identical |
| `minecraft:chests/shipwreck_treasure` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0089% | identical |
| `minecraft:chests/village/village_weaponsmith` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0035% | identical |
| `minecraft:chests/igloo_chest` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0078% | identical |
| `minecraft:chests/pillager_outpost` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0033% | identical |
| `minecraft:entities/wither_skeleton` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0063% | identical |
| `minecraft:entities/vindicator` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0089% | identical |
| `minecraft:chests/underwater_ruin_big` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0156% | identical |
| `minecraft:entities/witch` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0625% | identical |
| `minecraft:entities/evoker` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0125% | identical |
| `minecraft:chests/village/village_toolsmith` | 0.025 | 0.025 | 25.17 | 1.51 | 0.0208% | identical |

## Interpretation

- The simulator uses the same independent chance, inclusive count range and equal item choice as the runtime catalogue.
- The representative-pack column deliberately adds an extreme foreign weight; every seeded result must remain byte-for-byte identical because POWERS never edits a foreign pool.
- “Same-item consecutive-trigger chance” is the probability that two adjacent table triggers both drop from this POWERS pool and select the same item. It is not a duplication exploit.
