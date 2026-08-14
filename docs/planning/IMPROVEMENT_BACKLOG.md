# POWERS improvement backlog

This is the proposal register for work **not claimed as shipped** in `README.md`. It was derived from the production registries, generated item/action/lifecycle catalogues, source hot paths, current assets, and release evidence on 2026-08-11.

Kinds: **Defect** = reproduced source/content contradiction; **Guarantee** = preventive correctness proof; **Enhancement** = deeper existing behavior; **Expansion** = new content; **Research** = prototype/measure before committing. Priorities: **P0** release/data safety, **P1** next stabilization, **P2** major improvement, **P3** optional depth. An acceptance condition is evidence required to close an item, not a claim that it is already complete.

## 1. Correctness, saves, and lifecycle

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| COR-018 | Guarantee | P2 | Add deterministic replay seeds for all random target, strike, scar, loot, and learning decisions. | A captured seed reproduces behavior and visuals without changing production randomness. |

## 2. Performance and scalability

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| PERF-010 | Enhancement | P2 | Virtualize artifact catalogue rows and replace page rebuilds with a scrollable reusable grid. | Constant widget count and smooth search with thousands of datapack actions. |
| PERF-011 | Enhancement | P2 | Cache rank profiles and translated menu snapshots by revision. | No per-frame/per-cast reconstruction when rank/config/registry revision is unchanged. |
| PERF-017 | Enhancement | P2 | Add query heat maps to diagnostics for wards, names, forces, bodies, and fields. | Operator can identify the top hot chunks without installing a profiler. |
| PERF-018 | Enhancement | P3 | Add dormant-dimension suspension for realm ambience and landmark logic. | Empty dimensions perform zero periodic work beyond persisted deadlines. |

## 3. Innate power depth

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| PWR-004 | Enhancement | P2 | Size Morphing: body-volume-aware crawling, doorway prediction, mount handling, reach/UI feedback, and mass-based knockback. | All scales navigate standard collision fixtures without suffocation or reach desync. |
| PWR-005 | Enhancement | P2 | Teleport: saved named waypoints, recent destinations, portal previews, and explicit unloaded progress. | Searchable UI remains server-authorized and never exposes forbidden dimensions. |
| PWR-006 | Enhancement | P2 | Flight: momentum turns, sonic threshold, air-braking, water transition, and stamina-readable audio. | Movement is responsive under latency without creative flags or anti-cheat false positives. |
| PWR-007 | Enhancement | P2 | Starfall: constellation presets and team-painted safe lanes. | Presets change geometry, not hidden damage, and warnings remain readable. |
| PWR-008 | Enhancement | P2 | Void Beam: destructible bore material tiers and temporary gravity lens. | Protection-first ray tests prove bounded block and entity work. |
| PWR-011 | Enhancement | P2 | Thunderclap: terrain dust wave, glass/fragile-tag interaction, and directional echo in caves. | Damage, grief, and audio respect policy and obstruction. |
| PWR-012 | Enhancement | P2 | Speed Burst: wall-run or ricochet branch with telegraphed collision normals. | No clipping, fall exploit, or forced-movement bypass. |
| PWR-013 | Enhancement | P2 | Telekinesis: aim-held single-target manipulation, object orbit, and intentional projectile release. | Ownership, collision, reach, and consent remain server-owned. |
| PWR-014 | Enhancement | P2 | Energy Beam: optional continuous visual interpolation and material scorch decals. | Damage beats stay discrete and server load does not increase with frame rate. |
| PWR-015 | Enhancement | P2 | Super Speed: client camera/FOV comfort controls and path-aware wake LOD. | Reduced-motion mode removes camera distortion without gameplay advantage. |
| PWR-016 | Enhancement | P2 | Breezy Bash: caster-directed landing zone and allied rescue mode. | Hostile control still needs consent/policy; rescue never teleports through walls. |
| PWR-017 | Enhancement | P2 | Invisibility: light/shadow exposure meter, footprints, rain silhouettes, and Insight counterplay. | Counter cues are consistent, configurable, and do not reveal through walls. |
| PWR-021 | Enhancement | P2 | Vessel Possession: richer mob action adapters (doors, ranged use, special attack) without fabricating player inventories. | Each supported host type declares allowed controls and exact cleanup. |
| PWR-022 | Enhancement | P2 | Astral Projection: interact-only spirit clues, ward sight, and return-path indicator. | Spirit cannot move items, attack, load arbitrary chunks, or bypass progression. |
| PWR-023 | Enhancement | P2 | Energy Drain: visible tether stress, interruption minigame, and boss-scaled capped conversion. | No infinite reservoir loop or percent-health bypass. |
| PWR-024 | Enhancement | P2 | Ice Manipulation: melt lifecycle, ice bridges, brittle armour, and fire interaction. | Temporary terrain restores safely and block updates are bounded. |
| PWR-025 | Enhancement | P2 | Plant/Healing: species-aware growth, root shields, blight cleansing, and nature boss utility. | Never duplicates crops/drops and respects bonemeal/protection hooks. |
| PWR-027 | Expansion | P3 | Add cooperative three-caster Concord rituals combining complementary powers. | Each recipe is discoverable, consented, interruption-safe, and in the pair/triad catalogue. |
| PWR-028 | Expansion | P3 | Add alignment-exclusive rank-10 ascension forms with a reversible ceremony. | Form has meaningful risk/counterplay and no permanent player-data corruption. |

## 4. Grimoires and practical magic

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| SPL-003 | Enhancement | P2 | Soul Compass: placeable scrying focus and permission-aware shared viewing. | Viewer list is visible; body vulnerability and name uniqueness remain. |
| SPL-004 | Enhancement | P2 | Augury: forecast living-force spread fronts, Whiteout/Eclipse severity, and safe ritual windows. | Forecast uses loaded authoritative state and labels uncertainty. |
| SPL-005 | Enhancement | P2 | Cartographer's Star: route breadcrumbs and reusable discovered-site journal. | It never forces search chunks or reveals progression-locked sites. |
| SPL-007 | Enhancement | P2 | Celestial Ruin: vertical atmosphere column, cloud displacement, post-blast ash/weather, and distance-scaled structural scars. | Client LOD and server terrain budgets remain bounded across 6,000 blocks. |
| SPL-009 | Enhancement | P2 | Blood Reading: trend recent damage/healing and diagnose force/amethyst vulnerability. | No hidden equipment, private data, or consent bypass leaks. |
| SPL-011 | Enhancement | P2 | Purification Circle: ingredient/rune variants that choose cleanse, link sever, or corruption relief. | Amethyst Poisoning remains explicitly non-cleansable except by its own counter. |
| SPL-012 | Enhancement | P2 | Verdant Tending: reforest bounded templates and repair biome vegetation. | No loot duplication, protected placement, or runaway scheduled ticks. |
| SPL-013 | Enhancement | P2 | Hearth Sanctuary: persistent visible floor rune and voluntary ally exclusion. | Every ward remains individually owned and overkill-safe. |
| SPL-014 | Enhancement | P2 | Ward Breaking: contest mechanic where defenders reinforce a Ward during the channel. | Both sides get readable progress; final state is deterministic. |
| SPL-016 | Expansion | P3 | Add a Celestial **Oath of Return** that safely recalls consenting companions/bodies, never escaped mindscapes. | Travel matrix proves confinement and consent on every form. |
| SPL-017 | Expansion | P3 | Add a Wild **Mending of Place** ritual to restore POWERS-owned temporary terrain scars. | It restores only recorded mod changes and cannot regenerate mined resources. |
| SPL-018 | Expansion | P3 | Add an Archivist **Memory Echo** spell that replays redacted local magic residues. | Bounded history, no private chat/coordinates beyond the casting area, clear expiry. |
| SPL-019 | Expansion | P3 | Add a Deep **Threshold Survey** diagnostic, not a gateway, to report why a dimension route is blocked. | Purely informative; cannot bypass anchor, body, rank, or realm policy. |
| SPL-020 | Research | P3 | Prototype player-built multi-block ritual circles sourced from datapacks. | Demonstrate validation, chunk-unload safety, protection hooks, and readable construction errors. |

## 5. Crystals and convergence

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| CRY-003 | Enhancement | P2 | Add crystal attunement discovery so modes show unknown silhouettes before first use. | Save migration and accessibility do not hide controls permanently. |
| CRY-004 | Enhancement | P2 | Red Inferno: moving storm boundary, fuel-aware fire color, and protected burn aftermath. | Entity and terrain work stays capped for full duration. |
| CRY-005 | Enhancement | P2 | Orange Echoes: formation choice, defend point, and visible lifetime/integrity. | Echoes remain equipment-free and cannot pick up or duplicate items. |
| CRY-006 | Enhancement | P2 | Creativity Manifestation: choose from small validated datapack blueprints. | Templates have size/material/protection budgets and atomic preflight. |
| CRY-007 | Enhancement | P2 | Yellow Size Shift: transition shockwave, camera comfort, collision preview, and mount rejection reason. | Extreme scales never suffocate or desync the server hitbox. |
| CRY-008 | Enhancement | P2 | Green Life Bloom: resurrect only tagged temporary allied summons and restore corrupted flora. | Never revives players/bosses or duplicates entities. |
| CRY-010 | Enhancement | P2 | Dreamwalking: host-compatible ability hints and voluntary host emergency eject. | Eject cannot strand controller or body. |
| CRY-011 | Enhancement | P2 | Indigo Middleworld: memory trails, liminal hazards, and discoverable exits tied to exact origin. | Origin persistence and confinement survive restart. |
| CRY-014 | Enhancement | P2 | Light/Dark group travel: preview eligible/denied companions with individual reason. | No identity or consent leakage beyond nearby candidates. |
| CRY-015 | Expansion | P3 | Design non-crafting story acquisition trials for each crystal while retaining recipe absence. | Every trial is non-circular, multiplayer-safe, and documented; no recipe JSON added. |
| CRY-016 | Research | P3 | Explore crystal fractures/temporary exhaustion as optional high-stakes server policy. | Default remains indestructible; prototype is opt-in and save-safe. |

## 6. Progression, ranks, energy, and balance

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| PRG-003 | Enhancement | P2 | Add party contribution to Herald/First Vessel credit with anti-AFK rules. | Nearby meaningful damage/support receives deterministic credit once. |
| PRG-004 | Enhancement | P2 | Make each of 28 nodes alter at least one named mechanic and display it numerically. | No node is title-only; tooltip matches executable profile tests. |
| PRG-005 | Enhancement | P2 | Add rank-maze route preview, dependency highlights, respec delta, and confirmation. | Keyboard/narration users can inspect every consequence before spending XP. |
| PRG-006 | Enhancement | P2 | Add server-configurable ethical alternatives to Darkness tasks without weakening default lore. | Alternate objectives are explicit, equally severe, and disabled by default. |
| PRG-008 | Enhancement | P2 | Define diminishing returns for stacking attunements, Darkness aura, Shadow link, sleep, runestones, and reservoirs. | One generated table covers every source combination and prevents overflow. |
| PRG-009 | Enhancement | P2 | Add runestone degradation/repair only as an opt-in economy mode. | Existing worlds retain reusable behavior by default. |
| PRG-010 | Enhancement | P2 | Add alignment tension meter driven by actions rather than instant binary swaps. | It cannot silently remove powers/items; transitions are previewed and reversible until committed. |
| PRG-011 | Expansion | P3 | Add cooperative **Concordance** progression for groups mixing Light and Darkness. | Rewards coordination without erasing each player's alignment identity. |
| PRG-012 | Expansion | P3 | Add post-rank-10 mastery challenges that unlock cosmetics/variants, not raw infinite scaling. | Boss balance and server budgets remain bounded. |
| PRG-014 | Enhancement | P3 | Add operator-import/export of a player's progression snapshot. | Signed/validated format, dry run, backups, and no arbitrary NBT injection. |

## 7. Artifacts, relics, Crucible, food, and loot

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| ART-003 | Enhancement | P2 | Give every ring/amulet a visible attunement school and one bounded situational modifier. | Variants are mechanically distinct without stack multiplicative abuse. |
| ART-006 | Enhancement | P2 | Give heart relics unique models/beat audio and explicit mutual-exclusion policy. | Multiple hearts cannot stack hidden death wards or unbounded passives. |
| ART-007 | Enhancement | P2 | Expand Philosopher's Stone through datapack transmutation recipes with entropy cost. | Recipes are discoverable, protected, non-circular, and cannot duplicate value. |
| ART-009 | Enhancement | P2 | Flute: formation/stance wheel and guardian status. | Commands affect only owned eligible guardians under caps. |
| ART-012 | Enhancement | P2 | Give fossils/pages/jewels in-world archaeology clue chains instead of only XP values. | Each clue has source, interpretation, and non-circular reward. |
| ART-013 | Enhancement | P2 | Expand Crucible output previews with retained/lost components and exact level curve. | Server snapshot drives all displayed data; stale preview cannot commit. |
| ART-014 | Enhancement | P2 | Add alignment-specific weapon models/animations for the six conversion outputs. | All GUI/ground/first/third-person views pass asset QA. |
| ART-015 | Enhancement | P2 | Add weapon-archetype mastery challenges and signature finishers. | Proc remains cooldown-bounded and no ordinary weapon eclipses mythics. |
| ART-016 | Enhancement | P2 | Rebalance food affinity using tags so third-party foods opt into normal/foul/neutral behavior. | Unknown foods default neutral; no hidden starvation loop. |
| ART-018 | Expansion | P3 | Add an Archivist relic-restoration bench for damaged archaeology finds. | It complements rather than duplicates the Arcane Crucible. |
| ART-019 | Expansion | P3 | Add set-bonus lore collections for non-combat relics. | Bonuses are utility/cosmetic, bounded, and visible before activation. |
| ART-020 | Guarantee | P2 | Audit mythic artifact ownership in nested containers and inventory-component mods. | Policy explicitly supports or rejects each container with no ghost authorization. |

## 8. Realms, forces, structures, and lore

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| WRLD-003 | Enhancement | P2 | Make Archives contain collectible memories and branch-dependent records. | Spoiler visibility follows progression and collection persists per player/site. |
| WRLD-004 | Enhancement | P2 | Build real maze algorithms/puzzles for Labyrinths with multiplayer reset. | Solvable seed, no permanent trap, bounded generation, and meaningful reward. |
| WRLD-005 | Enhancement | P2 | Turn Shrines into attunement/Concord ritual spaces. | Ritual rules are explained in-world and protection-compatible. |
| WRLD-006 | Enhancement | P2 | Populate remembered Settlements with aligned NPC factions and schedules. | NPC count/AI is capped; dialogue reflects player history. |
| WRLD-007 | Enhancement | P2 | Make Force Fonts dangerous renewable energy objectives rather than passive scenery. | Reward/risk scales by group size without infinite idle generation. |
| WRLD-008 | Enhancement | P2 | Give Herald Courts multi-stage arena mechanics and recoverable exits. | Court cannot softlock under boss unload/death/restart. |
| WRLD-009 | Enhancement | P2 | Add visible Force Pressure horizon distortion, audio, and compass feedback. | Three distance tiers remain readable with reduced motion/audio off. |
| WRLD-010 | Enhancement | P2 | Deepen Whiteout/Dark Eclipse with temporary routes, mobs, hazards, and opportunities. | Event work stops in empty realms and persists deadlines correctly. |
| WRLD-011 | Enhancement | P2 | Add Eclipse Scars in Overworld as finite recoverable mini-dungeons. | Scars cannot spread indefinitely and have amethyst containment paths. |
| WRLD-012 | Enhancement | P2 | Add faction invasions keyed to excessive living-force spread. | Warning, cap, despawn, protection, and server opt-out are explicit. |
| WRLD-013 | Enhancement | P2 | Add player-built containment ceremonies with tiered ward networks. | Networks use indexed chunks and cannot create quadratic scans. |
| WRLD-014 | Enhancement | P2 | Add force ecology: Pure Light crystallizes corruption; Darkness blights crops; amethyst slowly scars both. | Transformation table is reversible/bounded and included in interaction docs. |
| WRLD-015 | Enhancement | P2 | Add Middleworld libraries, forgotten roads, dream weather, and neutral Archivist outposts. | Content retains exact-origin travel and never becomes an ordinary teleport hub. |
| WRLD-016 | Expansion | P3 | Add the **Great Library Between**, Archivist faction hub, and relic-identification progression. | Provides Wisdom Fruit and lore acquisition without replacing Shadow. |
| WRLD-017 | Expansion | P3 | Add the **Hollow Basilica**, a Darkness settlement with competing subfactions. | Choices alter lore/rewards, not permanent server-wide grief. |
| WRLD-018 | Expansion | P3 | Add the **Ivory Orrery**, a Light settlement exposing the Concord's authoritarian side. | Light is powerful/ambiguous rather than simply benevolent. |
| WRLD-019 | Expansion | P3 | Add cross-realm **Eclipse Breach** events requiring Light/Dark cooperation. | Event cannot directly transport trapped minds out of their realm. |
| WRLD-020 | Expansion | P3 | Add archaeology trails linking Overworld ruins to all four cosmological Claims. | Each trail has discoverable clues, loot, and a documented conclusion. |

## 9. Mobs, bosses, factions, and encounters

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| MOB-003 | Enhancement | P2 | Add distinct Hollowed variants: Stalker, Binder, Herald Acolyte. | Shared base plus data-authored stats; each has a readable counter. |
| MOB-004 | Enhancement | P2 | Add Radiant variants: Witness, Warden, Choir. | Roles complement rather than mirror Darkness one-for-one. |
| MOB-005 | Enhancement | P2 | Add neutral Amethyst Covenant Golems that contain force spread. | They never convert protected/player blocks and have repair limits. |
| MOB-006 | Enhancement | P2 | Improve Herald phases, dialogue, arena edits, loot tables, and multiplayer scaling. | Both bosses have three proven phases and no unload/reset exploit. |
| MOB-007 | Enhancement | P2 | Give First Vessel readable action telegraphs and a boss-bar phase vocabulary. | Every lethal attack has an accessible warning and counter window. |
| MOB-008 | Enhancement | P2 | Expand First Vessel's 23 adapters with prioritized synergy combos. | Planner remains bounded and cannot cast incompatible simultaneous states. |
| MOB-009 | Expansion | P3 | Add **The Pale Archivist**, a memory-stealing Middleworld boss. | Fight tests knowledge, naming, and body mechanics without deleting real player data. |
| MOB-010 | Expansion | P3 | Add **The Root Beneath Dawn**, a Pure Light nature boss. | Uses growth/healing as threat and rewards Wild progression. |
| MOB-011 | Expansion | P3 | Add **The Nameless Choir**, multi-entity Darkness boss. | Shared health/formation has bounded synchronization and clean despawn. |
| MOB-012 | Expansion | P3 | Add **Amethyst Leviathan**, force-neutral raid guardian. | Counters both alignments and supplies containment progression. |
| MOB-013 | Expansion | P3 | Add **Eclipse Regent**, optional dual-alignment postgame boss. | Requires Concord mechanics and cannot be brute-forced by one infinite cooldown loop. |
| MOB-014 | Enhancement | P2 | Add generated boss loadout/interaction tests against every player action. | Each action is effective, resisted with feedback, or deliberately immune. |
| MOB-015 | Enhancement | P2 | Expand test actor with configurable rank, alignment, consent, armour, movement, and scripted actions. | `/powers testing actor` can reproduce every player-compatible case. |
| MOB-016 | Enhancement | P3 | Add replayable encounter arenas and operator presets. | Arena cleanup deletes only tagged test content and restores affected test terrain. |

## 10. Shadow intelligence and behavior

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| SHD-006 | Enhancement | P2 | Add owner-approved construction/harvest errands using safe allowlisted actions. | Never mines protected blocks, opens private containers, or loads distant chunks. |
| SHD-007 | Enhancement | P2 | Add item delivery paths through actual nearby inventories only with explicit owner permission. | No conjured duplication or third-party inventory access. |
| SHD-008 | Enhancement | P2 | Teach combat planner encounter roles: peel, interrupt, rescue, suppress, execute, disengage. | Role choice is inspectable, bounded, friendly-fire safe, and preference-aware. |
| SHD-009 | Enhancement | P2 | Learn from explicit owner feedback (`good`, `don't do that`) separately from combat reward. | Learning remains owner-local, capped, resettable, and cannot alter hard safety. |
| SHD-010 | Enhancement | P2 | Add spatial memory of temporary owner-designated places, not global world surveillance. | Bounded named points, explicit delete, dimension-safe, and no forced loading. |
| SHD-011 | Enhancement | P2 | Expand offline lore/registry knowledge to all 260 catalogue rows, 64 actions, ranks, realms, and interaction rules. | Generated knowledge coverage report reaches 100% with source links. |
| SHD-013 | Enhancement | P2 | Make Shadow's subtle ulterior Darkness agenda stateful but never deceptive about mechanics/safety. | Tone tests distinguish persuasion from fabricated facts. |
| SHD-014 | Enhancement | P2 | Add spoken/visual combat callouts with frequency controls. | Critical warnings survive; chatter respects global/private and accessibility settings. |
| SHD-016 | Research | P2 | Evaluate a local small-language-model provider for optional private dialogue. | Benchmarks cover latency, memory, moderation, redaction, CPU/RAM, and offline fallback. |
| SHD-018 | Enhancement | P3 | Add Shadow/Herald/First Vessel relationship memories and evolving dialogue. | State is compact, migratable, and cannot change gameplay permissions invisibly. |
| SHD-019 | Enhancement | P3 | Allow Shadow to coordinate owned Hollowed formations. | Total AI/cast quotas stay shared and capped. |
| SHD-020 | Guarantee | P2 | Add deterministic combat-learning replay and export for debugging. | Export is redacted, bounded, and faithfully reproduces action scoring. |

## 11. UI, accessibility, visuals, and audio

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| UX-004 | Enhancement | P2 | Replace artifact catalogue pagination with virtual scroll, filters, and recent actions. | Any action is bindable in two interactions after search. |
| UX-007 | Enhancement | P2 | Add complete controller/gamepad navigation for all screens and selectors. | Every action is reachable without mouse and focus never traps. |
| UX-008 | Guarantee | P2 | Complete narration labels, order, live-region throttling, and high-contrast focus for every custom screen. | Automated accessibility audit plus manual screen-reader pass. |
| UX-009 | Enhancement | P2 | Add configurable reduced flashes, tinnitus, camera shake, FOV, and large-beam opacity. | Celestial Ruin remains mechanically readable with all comfort options enabled. |
| VFX-003 | Enhancement | P1 | Establish per-action near-camera particle exclusion/cone limits for Lightning and Fireball. | First-person captures preserve aim visibility at all ranks. |
| VFX-004 | Enhancement | P2 | Add material-aware impact decals/scars for beam, slam, thunderclap, ice, and fire. | Decals are bounded, protected, and restore/expire cleanly. |
| VFX-005 | Enhancement | P2 | Create unique silhouettes for each rank-10 transformation. | A distant observer identifies power/alignment without reading chat. |
| VFX-006 | Enhancement | P2 | Add animation controllers for Shadow, guardians, Heralds, and First Vessel casting poses. | Client/server action timing stays synchronized under latency. |
| VFX-007 | Enhancement | P2 | Expand 14 sounds into layered near/mid/far variants and occlusion-aware mixes. | Loud events do not clip; volume falls off correctly; subtitles exist. |
| VFX-009 | Enhancement | P2 | Add custom Light Realm sky shader/skybox with accessibility fallback. | Pure white target remains stable across render distance/resource packs. |
| VFX-010 | Enhancement | P2 | Add weather/biome-responsive ancient rune color grading. | Meaningful action colors remain consistent and color-blind safe. |
| VFX-011 | Guarantee | P2 | Audit texture mipmaps, alpha seams, spawn eggs, item transforms, and atlas bleeding. | Pixel-perfect captures at all mip levels/views. |
| VFX-012 | Expansion | P3 | Add original music/ambient soundscapes for Light, Dark, Middleworld, bosses, and Eclipse events. | Loop points, licensing, subtitles, volume categories, and performance validated. |

## 12. Cross-system interactions and counterplay

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| INT-006 | Enhancement | P2 | Expand conductive networks across Lightning, water, copper, Pure Light, amethyst, and forcefields. | Graph is capped and each terminal has distinct visuals/damage. |
| INT-007 | Enhancement | P2 | Expand thermal rules across Fireball/Inferno, Ice, water, snow, plants, and realm blocks. | State table prevents contradictory conversions and scheduled-tick storms. |
| INT-008 | Enhancement | P2 | Expand temporal rules across Time Freeze, Chrono Stop, projectiles, channels, Ruin countdown, realm cycles, and external tick state. | Each clock explicitly uses frozen or unfrozen time and is test-proven. |
| INT-009 | Enhancement | P2 | Expand mind rules across possession, Dreamwalking, projection, Soul Compass, Shadow, body death, and Soul Link. | Nested sessions are refused with precise reason; fatal outcomes match lifecycle matrix. |
| INT-010 | Enhancement | P2 | Expand alignment rules for Light/Dark artifacts, forces, bosses, Concord, food, and amethyst. | One data-driven allegiance matrix replaces scattered boolean assumptions. |
| INT-011 | Enhancement | P2 | Add forcefield/linked-soul ordering visualization and damage accounting. | No damage multiplication, recursion, or ward double-consumption. |
| INT-012 | Enhancement | P2 | Let Dimensional Anchor stabilize portals, bodies, and gates under explicit spell interactions. | It never becomes a realm escape or permanent chunk ticket. |
| INT-013 | Enhancement | P2 | Add amethyst crystallization of temporary void/fire/ice scars with recoverable residue. | Transformation is bounded and cannot farm rare blocks. |
| INT-014 | Enhancement | P2 | Add boss-specific reactions to Concords and opposing artifacts. | Reactions reward setup without hard-requiring one player alignment. |
| INT-015 | Expansion | P3 | Add rare **Eclipse Synthesis** when matched Light/Dark dominions collide. | It is telegraphed, consensual near allies, protected, and globally budgeted. |

## 13. Multiplayer, administration, compatibility, and API

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| NET-010 | Enhancement | P2 | Add datapack reload migration for action/menu keys without reconnect. | Active casts either finish on old revision or cancel safely; clients receive one revision. |
| NET-011 | Guarantee | P2 | Validate behavior with common performance/render/voice/claim/inventory mods. | Published compatibility matrix names tested versions and known limitations. |
| NET-012 | Enhancement | P3 | Add localization framework and first complete non-English translation. | No string concatenation blocks grammar; all UI/tooltips/subtitles localize. |
| NET-013 | Enhancement | P3 | Add server presets: lore survival, PvP-balanced, cinematic testing, low-spec. | Presets are explicit diffs and never overwrite hand-edited config without confirmation. |

## 14. Testing, observability, release, and documentation

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| QA-001 | Guarantee | P0 | Run clean full build, JUnit, all GameTests, dedicated server, client smoke, visual suite, soak, and manual acceptance before release tags. | One signed release report contains exact commands, revisions, counts, logs, and unresolved limitations. |
| QA-006 | Guarantee | P1 | Add long-duration world soak with forced restart every few minutes. | No leaked ticket, index, field, summon, body, freeze, or Ruin state over 24 hours. |
| QA-009 | Guarantee | P2 | Add network latency/loss/reorder simulation for menus, movement control, wheel binding, and FX. | State converges without double cast, ghost selection, or unsafe prediction. |
| QA-010 | Guarantee | P2 | Add test worlds for claims, borders, low ceilings, void, fluids, mounts, passengers, portals, and modded dimensions. | Each critical action has at least one hostile-environment scenario. |
| QA-015 | Enhancement | P2 | Add in-game operator test dashboard summarizing coverage and recent failures. | Dashboard is read-only outside explicit testing actions and has negligible idle cost. |
| QA-016 | Guarantee | P2 | Audit all comments for intent/invariants rather than line-by-line narration. | Source quality check rejects stale TODOs, misleading claims, and public APIs without contracts. |
| QA-019 | Enhancement | P3 | Produce a concise operator handbook separate from the player README. | Includes install, config, backups, recovery, diagnostics, permissions, and catastrophe policy. |
| QA-020 | Enhancement | P3 | Produce an in-game bestiary/ritual codex from the same generated data. | It contains no unearned spoilers and cannot drift from registries. |

## Recommended delivery order

1. **Release guarantees:** all P0 items, then P1 save/lifecycle/network/interaction proof.
2. **Measured performance:** real-player profiling, index instrumentation, fair work quotas, and ticket backpressure.
3. **Usability and visual consistency:** HUD/screenshot matrix, artifact library virtualization, authored particle audit, spell guidance.
4. **System depth:** rank/quest rebalance, individual power refinements, relic and Crucible expansion, Shadow task planner.
5. **World expansion:** authored structures, factions, realm ecology, new mobs/bosses, cooperative Concord content.

Each completed subsystem should update its registry-derived documentation, migration notes, changelog, automated evidence, and manual acceptance record in the same commit. New creative work must first define counterplay, protection policy, persistence, cleanup, workload budget, accessibility presentation, and cross-system interaction rows.
