# PRG-001 multiplayer quest telemetry

Status: **accepted** on production commit `751b3bcecc5f5c13199e8079fe0b902918630f14`.

## Method

- Minecraft Java Edition 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25.
- Exact command: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home python3 scripts/quest_telemetry_campaign.py`.
- Ten isolated real Fabric clients used fresh `QuestLight1`–`QuestLight10` identities, followed by ten fresh `QuestDark1`–`QuestDark10` identities.
- The server replayed actual deeds through `SkillQuestTracker` and `DarknessQuestTracker` at ten distinct human-equivalent cadences. The harness never inserted telemetry samples or rank values directly.
- Light executed 700,000 human-equivalent ticks; Darkness executed 635,000. Every client remained connected until its scripted cleanup.
- The server reported no error line. All 20 clients emitted the expected offline-development profile-key warning; it was classified separately. There was no unexpected client error.

## Published result

| Alignment | Level | Samples | Median ticks | Median min | p90 ticks | p90 min | Winning route |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Dark | 1 | 10 | 8,820 | 7.35 | 10,080 | 8.40 | predation |
| Dark | 2 | 10 | 28,980 | 24.15 | 32,620 | 27.18 | predation |
| Dark | 3 | 10 | 11,200 | 9.33 | 13,300 | 11.08 | atrocity |
| Dark | 4 | 10 | 7,920 | 6.60 | 8,960 | 7.47 | atrocity |
| Dark | 5 | 10 | 12,740 | 10.62 | 14,240 | 11.87 | atrocity |
| Dark | 6 | 10 | 38,420 | 32.02 | 42,800 | 35.67 | atrocity |
| Dark | 7 | 10 | 81,000 | 67.50 | 91,500 | 76.25 | atrocity |
| Dark | 8 | 10 | 81,000 | 67.50 | 91,500 | 76.25 | atrocity |
| Dark | 9 | 10 | 135,000 | 112.50 | 152,500 | 127.08 | atrocity |
| Dark | 10 | 10 | 135,000 | 112.50 | 152,500 | 127.08 | atrocity |
| Light | 1 | 10 | 8,280 | 6.90 | 9,120 | 7.60 | pilgrimage |
| Light | 2 | 10 | 8,280 | 6.90 | 9,120 | 7.60 | pilgrimage |
| Light | 3 | 10 | 12,840 | 10.70 | 15,360 | 12.80 | pilgrimage |
| Light | 4 | 10 | 19,600 | 16.33 | 22,400 | 18.67 | pilgrimage |
| Light | 5 | 10 | 34,300 | 28.58 | 39,200 | 32.67 | pilgrimage |
| Light | 6 | 10 | 49,000 | 40.83 | 56,000 | 46.67 | pilgrimage |
| Light | 7 | 10 | 63,700 | 53.08 | 72,800 | 60.67 | pilgrimage |
| Light | 8 | 10 | 83,300 | 69.42 | 95,200 | 79.33 | pilgrimage |
| Light | 9 | 10 | 112,700 | 93.92 | 128,800 | 107.33 | pilgrimage |
| Light | 10 | 10 | 196,000 | 163.33 | 224,000 | 186.67 | pilgrimage |

The median cumulative journey was 7.50 hours for Darkness and 8.17 hours for Light; p90 was 8.47 and 9.33 hours respectively. Every live median interval exceeded five minutes.

## Balance and migration decision

The first accepted measurement exposed compressed Darkness milestones because a villager child correctly advances both villager counters. The predictor now models that overlap. The Darkness predation alternative at level 1 increased from 8 to 10 villagers, while the primary atrocity thresholds became:

- Level 3: 60 villagers and 8 iron golems.
- Level 4: 70 villagers, 40 wolves, and 10 villager children.
- Level 5: 85 villagers, 60 wolves, 12 villager children, and 11 iron golems.

Later thresholds and every Light threshold remained unchanged because their observed total and per-rank distributions were justified. Existing completed levels cannot be revoked: evaluation begins at the saved level. Raw deed maps retain their stable keys and counts, so unfinished players continue from their exact saved progress under the adjusted cumulative requirements; no destructive save rewrite is required.

## Evidence files

- `quest-telemetry-report.json`: all 20 anonymous median/p90 publication rows and exact build identity.
- `server-key-lines.log`: connection, completion, cleanup, and sprint evidence extracted from the accepted server log.
- `SHA256SUMS`: integrity hashes for both tracked evidence files.
