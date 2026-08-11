# POWERS improvement backlog

This is the proposal register for work **not claimed as shipped** in `README.md`. It was derived from the production registries, generated item/action/lifecycle catalogues, source hot paths, current assets, and release evidence on 2026-08-11.

Kinds: **Defect** = reproduced source/content contradiction; **Guarantee** = preventive correctness proof; **Enhancement** = deeper existing behavior; **Expansion** = new content; **Research** = prototype/measure before committing. Priorities: **P0** release/data safety, **P1** next stabilization, **P2** major improvement, **P3** optional depth. An acceptance condition is evidence required to close an item, not a claim that it is already complete.

## 1. Correctness, saves, and lifecycle

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| COR-001 | Guarantee | P0 | Build a versioned save-migration corpus from 1.0.0, 1.0.1, corrupt partial sessions, retired powers, retired spells, and artifact loadouts. | Every fixture loads without loss; second save is canonical and idempotent. |
| COR-002 | Guarantee | P0 | Make every multi-step cast use one explicit transaction object for validation, cost, cooldown, presence, and rollback. | Fault injection at every phase leaves no duplicate cost, cooldown, field, ticket, or entity. |
| COR-003 | Guarantee | P0 | Exhaustively prove detached-body/avatar fatal ordering under simultaneous damage. | Same-tick body/avatar hits produce exactly one recall/death and no duplicate drops/respawn. |
| COR-004 | Guarantee | P0 | Persist and reconcile Time Freeze ownership through crash, `/tick freeze`, owner logout, and server restart. | All state-machine traces restore the correct external/global tick state. |
| COR-005 | Guarantee | P0 | Add crash-safe journaling for active Celestial Ruin phase/cursor before destructive commits. | Kill-and-restart at every phase resumes once with no skipped or repeated destructive slice. |
| COR-006 | Guarantee | P1 | Centralize source-item authorization for all artifact toggles, companions, gates, summons, and death wards. | Removing, moving, destroying, or losing authorization stops every owned state within one reconciliation tick. |
| COR-007 | Guarantee | P1 | Verify all 672 lifecycle rows against executable handlers instead of generated expectations alone. | Mutation test of each handler is caught by at least one lifecycle test. |
| COR-008 | Guarantee | P1 | Add logout/rejoin proofs for cooldowns, rank state, consent, reservoirs, Miniportal charges, Shadow memory, and realm landmarks. | Rejoin and restart preserve exactly documented persistent state and clear all session-only state. |
| COR-009 | Guarantee | P1 | Verify simultaneous Empyrean Jewel overrides cannot double-charge or leak between consent categories. | Same-tick duplicate/cross-category casts pay one surcharge per unique cast and remain isolated. |
| COR-010 | Guarantee | P1 | Harden unique-name resolution against formatting, Unicode confusables, renamed entities, duplicates, unloads, and cross-dimension ambiguity. | Bounded tests return one authenticated target or a precise non-leaking failure. |
| COR-011 | Guarantee | P1 | Prove forcefield overkill interception for every custom damage type, `/kill`, void, starvation, environmental, and third-party damage. | Documented interceptable damage is fully absorbed once; non-interceptable sources are explicit. |
| COR-012 | Guarantee | P1 | Add atomicity tests for Crucible hoppers, chunk unload, menu close, death, concurrent viewers, and server crash. | Exactly one input/output outcome and no component duplication for every interruption point. |
| COR-013 | Guarantee | P1 | Reconcile old Adventure-mode mindscape saves without altering legitimate player-created Adventure mode. | Migration changes only sessions marked by the legacy POWERS owner. |
| COR-014 | Guarantee | P1 | Add world-border contraction tests while a storm, body session, gate, Miniportal, or Ruin is active. | Every feature relocates, cancels, or completes under its documented policy without illegal coordinates. |
| COR-015 | Guarantee | P1 | Prove dimension deletion/datapack removal recovery for bodies, anchors, portals, landmarks, and persistent events. | Missing registry keys produce safe operator diagnostics and recoverable state. |
| COR-016 | Guarantee | P1 | Add cross-mod attribute-modifier collision tests for scale, speed, health, armour, flight, and knockback. | POWERS removes only its UUID/owner modifiers and preserves foreign state. |
| COR-017 | Guarantee | P2 | Normalize all player-facing cooldown units and rounding. | HUD, action bar, Shadow explanation, catalogue, and command diagnostics agree to one tick. |
| COR-018 | Guarantee | P2 | Add deterministic replay seeds for all random target, strike, scar, loot, and learning decisions. | A captured seed reproduces behavior and visuals without changing production randomness. |
| COR-019 | Guarantee | P2 | Add explicit capability fallback for non-player living entities used by player-only mechanics. | Every action declares supported entity contract and returns a typed failure instead of casting/crashing. |
| COR-020 | Guarantee | P2 | Audit every scheduled callback for stale entity, stale level, and stopped-server references. | Static/runtime audit proves UUID re-resolution, deadline, and cancellation ownership. |

## 2. Performance and scalability

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| PERF-001 | Enhancement | P0 | Add real dedicated-server profiling at 10/50/100 connected bot players, not only deterministic simulation. | Published spark/JFR traces and p95/p99 tick time stay inside an agreed budget for 30 minutes. |
| PERF-002 | Enhancement | P1 | Replace the remaining cached 13³ natural-amethyst block scan with section/tag indexes invalidated by block change. | Amethyst lookup is proportional to nearby indexed sections, with equivalence tests. |
| PERF-003 | Enhancement | P1 | Instrument every spatial index with query candidates, misses, fallback scans, stale removals, and memory. | `/powers diagnose` exposes bounded counters by dimension and subsystem. |
| PERF-004 | Enhancement | P1 | Introduce adaptive visual budgets based on server MSPT and each client's particle setting. | FX degrades density before geometry/meaning and recovers without oscillation. |
| PERF-005 | Enhancement | P1 | Coalesce repeated magic-presence updates by chunk/action/observer. | Packet captures show fewer bytes/packets with identical collision behavior. |
| PERF-006 | Enhancement | P1 | Pool short-lived immutable geometry buffers and semantic packet builders. | Allocation profile shows materially lower young-generation churn in mass combat. |
| PERF-007 | Enhancement | P1 | Make living-force spread queues persist a compact frontier instead of rediscovering faces after restart. | Equivalent spread with bounded disk size and no restart burst. |
| PERF-008 | Enhancement | P1 | Add fair per-owner quotas inside global projectile/entity/field budgets. | One rank-10 artifact user cannot starve unrelated players' magic. |
| PERF-009 | Enhancement | P1 | Add backpressure for temporary chunk tickets and expose owner/reason/deadline. | Refused work fails clearly; ticket count returns to zero after soak and crash recovery. |
| PERF-010 | Enhancement | P2 | Virtualize artifact catalogue rows and replace page rebuilds with a scrollable reusable grid. | Constant widget count and smooth search with thousands of datapack actions. |
| PERF-011 | Enhancement | P2 | Cache rank profiles and translated menu snapshots by revision. | No per-frame/per-cast reconstruction when rank/config/registry revision is unchanged. |
| PERF-012 | Enhancement | P2 | Batch Shadow perception into shared nearby-entity snapshots with other AI guardians. | Planner candidate work is shared per chunk/tick and behavior remains deterministic. |
| PERF-013 | Enhancement | P2 | Add LOD for distant beams, realm events, Herald ceremonies, and Celestial Ruin. | Distant observers receive bounded silhouettes/audio without full near-field packets. |
| PERF-014 | Enhancement | P2 | Persist only compact owner/task IDs for permitted long-lived summons; exclude all derivable caches. | Save size is bounded and load rebuilds indexes exactly once. |
| PERF-015 | Research | P2 | Benchmark Fabric networking compression thresholds for semantic FX bursts. | Choose packet shape using bytes, CPU, and latency measurements rather than intuition. |
| PERF-016 | Enhancement | P2 | Budget block transformations by dimension and claim provider, not only globally. | A catastrophic event cannot freeze ordinary spread/containment forever. |
| PERF-017 | Enhancement | P2 | Add query heat maps to diagnostics for wards, names, forces, bodies, and fields. | Operator can identify the top hot chunks without installing a profiler. |
| PERF-018 | Enhancement | P3 | Add dormant-dimension suspension for realm ambience and landmark logic. | Empty dimensions perform zero periodic work beyond persisted deadlines. |

## 3. Innate power depth

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| PWR-001 | Guarantee | P1 | Live-test every rank 0–10 profile for all 23 innates, including terrain tier and capacity. | 253 GameTest scenarios match the generated level table. |
| PWR-002 | Enhancement | P1 | Give all destructive innates a server-configurable minimum environmental signature while respecting protection. | Rank 0 and rank 10 each leave visibly distinct, bounded, reversible test scars. |
| PWR-003 | Enhancement | P1 | Add boss resistance categories that reduce control duration without silently nullifying a power. | Every control power reports/visualizes full, resisted, immune, and reflected outcomes. |
| PWR-004 | Enhancement | P2 | Size Morphing: body-volume-aware crawling, doorway prediction, mount handling, reach/UI feedback, and mass-based knockback. | All scales navigate standard collision fixtures without suffocation or reach desync. |
| PWR-005 | Enhancement | P2 | Teleport: saved named waypoints, recent destinations, portal previews, and explicit unloaded progress. | Searchable UI remains server-authorized and never exposes forbidden dimensions. |
| PWR-006 | Enhancement | P2 | Flight: momentum turns, sonic threshold, air-braking, water transition, and stamina-readable audio. | Movement is responsive under latency without creative flags or anti-cheat false positives. |
| PWR-007 | Enhancement | P2 | Starfall: constellation presets and team-painted safe lanes. | Presets change geometry, not hidden damage, and warnings remain readable. |
| PWR-008 | Enhancement | P2 | Void Beam: destructible bore material tiers and temporary gravity lens. | Protection-first ray tests prove bounded block and entity work. |
| PWR-009 | Enhancement | P2 | Fireball: deliberate charge-release key path, catch/deflect timing, and ground streak continuity. | No accidental punch requirement; reflection attribution remains exact. |
| PWR-010 | Enhancement | P2 | Lightning: chain conductance through water, copper, armour, and rods with clear grounding counterplay. | Deterministic conductor graph caps nodes and never uses harmful vanilla bolt side effects. |
| PWR-011 | Enhancement | P2 | Thunderclap: terrain dust wave, glass/fragile-tag interaction, and directional echo in caves. | Damage, grief, and audio respect policy and obstruction. |
| PWR-012 | Enhancement | P2 | Speed Burst: wall-run or ricochet branch with telegraphed collision normals. | No clipping, fall exploit, or forced-movement bypass. |
| PWR-013 | Enhancement | P2 | Telekinesis: aim-held single-target manipulation, object orbit, and intentional projectile release. | Ownership, collision, reach, and consent remain server-owned. |
| PWR-014 | Enhancement | P2 | Energy Beam: optional continuous visual interpolation and material scorch decals. | Damage beats stay discrete and server load does not increase with frame rate. |
| PWR-015 | Enhancement | P2 | Super Speed: client camera/FOV comfort controls and path-aware wake LOD. | Reduced-motion mode removes camera distortion without gameplay advantage. |
| PWR-016 | Enhancement | P2 | Breezy Bash: caster-directed landing zone and allied rescue mode. | Hostile control still needs consent/policy; rescue never teleports through walls. |
| PWR-017 | Enhancement | P2 | Invisibility: light/shadow exposure meter, footprints, rain silhouettes, and Insight counterplay. | Counter cues are consistent, configurable, and do not reveal through walls. |
| PWR-018 | Enhancement | P2 | Time Freeze: per-owner drain forecast and pre-freeze server warning at low TPS. | UI predicts safe duration and refuses freeze only under an explicit operator budget. |
| PWR-019 | Enhancement | P2 | Forcefield: visible crack stages, ownership colour, repair/merge rules, and ally opt-out. | Integrity and sacrificial protection remain authoritative in all views. |
| PWR-020 | Enhancement | P2 | Gravity Displacement: player-chosen pull/orbit/repel modes and stable boss resistance. | Mode selection is fast, bounded, and represented in artifact snapshots. |
| PWR-021 | Enhancement | P2 | Vessel Possession: richer mob action adapters (doors, ranged use, special attack) without fabricating player inventories. | Each supported host type declares allowed controls and exact cleanup. |
| PWR-022 | Enhancement | P2 | Astral Projection: interact-only spirit clues, ward sight, and return-path indicator. | Spirit cannot move items, attack, load arbitrary chunks, or bypass progression. |
| PWR-023 | Enhancement | P2 | Energy Drain: visible tether stress, interruption minigame, and boss-scaled capped conversion. | No infinite reservoir loop or percent-health bypass. |
| PWR-024 | Enhancement | P2 | Ice Manipulation: melt lifecycle, ice bridges, brittle armour, and fire interaction. | Temporary terrain restores safely and block updates are bounded. |
| PWR-025 | Enhancement | P2 | Plant/Healing: species-aware growth, root shields, blight cleansing, and nature boss utility. | Never duplicates crops/drops and respects bonemeal/protection hooks. |
| PWR-026 | Enhancement | P2 | Double Health: clear heart-row animation, heal-to-cap rules, and anti-toggle exploit lock. | Repeated toggle cannot heal, kill, or overlap HUD rows. |
| PWR-027 | Expansion | P3 | Add cooperative three-caster Concord rituals combining complementary powers. | Each recipe is discoverable, consented, interruption-safe, and in the pair/triad catalogue. |
| PWR-028 | Expansion | P3 | Add alignment-exclusive rank-10 ascension forms with a reversible ceremony. | Form has meaningful risk/counterplay and no permanent player-data corruption. |

## 4. Grimoires and practical magic

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| SPL-001 | Guarantee | P1 | Create a live ritual acceptance suite for all 12 spells, interruption causes, and source isolation. | Each spell proves success, every typed failure, half-refund, and no innate scaling. |
| SPL-002 | Enhancement | P1 | Give every grimoire a compact contents/index page with purpose, target, range, channel, and counter. | A new player can select and cast without consulting README. |
| SPL-003 | Enhancement | P2 | Soul Compass: placeable scrying focus and permission-aware shared viewing. | Viewer list is visible; body vulnerability and name uniqueness remain. |
| SPL-004 | Enhancement | P2 | Augury: forecast living-force spread fronts, Whiteout/Eclipse severity, and safe ritual windows. | Forecast uses loaded authoritative state and labels uncertainty. |
| SPL-005 | Enhancement | P2 | Cartographer's Star: route breadcrumbs and reusable discovered-site journal. | It never forces search chunks or reveals progression-locked sites. |
| SPL-006 | Enhancement | P1 | Celestial Ruin: add operator staging preview, cancellation-before-lock policy, and protected-region dry run. | Preview lists affected chunks/entities/claims; committed events remain irreversible. |
| SPL-007 | Enhancement | P2 | Celestial Ruin: vertical atmosphere column, cloud displacement, post-blast ash/weather, and distance-scaled structural scars. | Client LOD and server terrain budgets remain bounded across 6,000 blocks. |
| SPL-008 | Enhancement | P2 | Dimensional Anchor: visible tether to anchor point, duration diagnostics, and renewable group anchor circle. | All travel paths consult one owner state and show the same remaining duration. |
| SPL-009 | Enhancement | P2 | Blood Reading: trend recent damage/healing and diagnose force/amethyst vulnerability. | No hidden equipment, private data, or consent bypass leaks. |
| SPL-010 | Enhancement | P2 | Grave Recall: optional compass bearing and death-marker expiry without teleportation. | Only the owner sees it and coordinates remain dimension-correct. |
| SPL-011 | Enhancement | P2 | Purification Circle: ingredient/rune variants that choose cleanse, link sever, or corruption relief. | Amethyst Poisoning remains explicitly non-cleansable except by its own counter. |
| SPL-012 | Enhancement | P2 | Verdant Tending: reforest bounded templates and repair biome vegetation. | No loot duplication, protected placement, or runaway scheduled ticks. |
| SPL-013 | Enhancement | P2 | Hearth Sanctuary: persistent visible floor rune and voluntary ally exclusion. | Every ward remains individually owned and overkill-safe. |
| SPL-014 | Enhancement | P2 | Ward Breaking: contest mechanic where defenders reinforce a Ward during the channel. | Both sides get readable progress; final state is deterministic. |
| SPL-015 | Enhancement | P2 | Dispel: inspect mode that names the exact nearest removable field before committing. | The server advertises only currently legal choices and revalidates release. |
| SPL-016 | Expansion | P3 | Add a Celestial **Oath of Return** that safely recalls consenting companions/bodies, never escaped mindscapes. | Travel matrix proves confinement and consent on every form. |
| SPL-017 | Expansion | P3 | Add a Wild **Mending of Place** ritual to restore POWERS-owned temporary terrain scars. | It restores only recorded mod changes and cannot regenerate mined resources. |
| SPL-018 | Expansion | P3 | Add an Archivist **Memory Echo** spell that replays redacted local magic residues. | Bounded history, no private chat/coordinates beyond the casting area, clear expiry. |
| SPL-019 | Expansion | P3 | Add a Deep **Threshold Survey** diagnostic, not a gateway, to report why a dimension route is blocked. | Purely informative; cannot bypass anchor, body, rank, or realm policy. |
| SPL-020 | Research | P3 | Prototype player-built multi-block ritual circles sourced from datapacks. | Demonstrate validation, chunk-unload safety, protection hooks, and readable construction errors. |

## 5. Crystals and convergence

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| CRY-001 | Defect | P1 | Rename the stale Shadow knowledge title **The Thirteen Crystals** to match ten usable items/11 actions, or explain an intentional lore count. | Knowledge entry, README, catalogue, and registry counts agree. |
| CRY-002 | Guarantee | P1 | GameTest every crystal via ordinary use, crouch mode change, artifact routing, cooldown sharing, death, and realm policy. | All 11 actions pass with fixed scaling and no item-swap bypass. |
| CRY-003 | Enhancement | P2 | Add crystal attunement discovery so modes show unknown silhouettes before first use. | Save migration and accessibility do not hide controls permanently. |
| CRY-004 | Enhancement | P2 | Red Inferno: moving storm boundary, fuel-aware fire color, and protected burn aftermath. | Entity and terrain work stays capped for full duration. |
| CRY-005 | Enhancement | P2 | Orange Echoes: formation choice, defend point, and visible lifetime/integrity. | Echoes remain equipment-free and cannot pick up or duplicate items. |
| CRY-006 | Enhancement | P2 | Creativity Manifestation: choose from small validated datapack blueprints. | Templates have size/material/protection budgets and atomic preflight. |
| CRY-007 | Enhancement | P2 | Yellow Size Shift: transition shockwave, camera comfort, collision preview, and mount rejection reason. | Extreme scales never suffocate or desync the server hitbox. |
| CRY-008 | Enhancement | P2 | Green Life Bloom: resurrect only tagged temporary allied summons and restore corrupted flora. | Never revives players/bosses or duplicates entities. |
| CRY-009 | Enhancement | P2 | Blue Chrono Stop: visible world-edge temporal fracture and owner/deadline HUD. | True tick ownership and one-minute release remain exact. |
| CRY-010 | Enhancement | P2 | Dreamwalking: host-compatible ability hints and voluntary host emergency eject. | Eject cannot strand controller or body. |
| CRY-011 | Enhancement | P2 | Indigo Middleworld: memory trails, liminal hazards, and discoverable exits tied to exact origin. | Origin persistence and confinement survive restart. |
| CRY-012 | Enhancement | P2 | Violet Soul Link: visible topology and per-target remaining mirrored-damage cap. | No recursive damage or forcefield double-count. |
| CRY-013 | Enhancement | P2 | Rainbow convergence: one radial selector shared with artifact glyph language. | Mode can be chosen in combat without chat or pagination. |
| CRY-014 | Enhancement | P2 | Light/Dark group travel: preview eligible/denied companions with individual reason. | No identity or consent leakage beyond nearby candidates. |
| CRY-015 | Expansion | P3 | Design non-crafting story acquisition trials for each crystal while retaining recipe absence. | Every trial is non-circular, multiplayer-safe, and documented; no recipe JSON added. |
| CRY-016 | Research | P3 | Explore crystal fractures/temporary exhaustion as optional high-stakes server policy. | Default remains indestructible; prototype is opt-in and save-safe. |

## 6. Progression, ranks, energy, and balance

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| PRG-001 | Guarantee | P1 | Telemetry-driven audit of all Light/Dark quest thresholds in real multiplayer. | Median/p90 completion time is published; changes include migration and rationale. |
| PRG-002 | Enhancement | P1 | Replace pure grind counters with authored milestone alternatives for accessibility while retaining moral divergence. | Every level has at least two equally costly routes and cannot double-count one event. |
| PRG-003 | Enhancement | P2 | Add party contribution to Herald/First Vessel credit with anti-AFK rules. | Nearby meaningful damage/support receives deterministic credit once. |
| PRG-004 | Enhancement | P2 | Make each of 28 nodes alter at least one named mechanic and display it numerically. | No node is title-only; tooltip matches executable profile tests. |
| PRG-005 | Enhancement | P2 | Add rank-maze route preview, dependency highlights, respec delta, and confirmation. | Keyboard/narration users can inspect every consequence before spending XP. |
| PRG-006 | Enhancement | P2 | Add server-configurable ethical alternatives to Darkness tasks without weakening default lore. | Alternate objectives are explicit, equally severe, and disabled by default. |
| PRG-007 | Enhancement | P2 | Add energy consumption history and source breakdown to diagnostics/HUD tooltip. | Totals reconcile with authoritative transactions and reservoirs. |
| PRG-008 | Enhancement | P2 | Define diminishing returns for stacking attunements, Darkness aura, Shadow link, sleep, runestones, and reservoirs. | One generated table covers every source combination and prevents overflow. |
| PRG-009 | Enhancement | P2 | Add runestone degradation/repair only as an opt-in economy mode. | Existing worlds retain reusable behavior by default. |
| PRG-010 | Enhancement | P2 | Add alignment tension meter driven by actions rather than instant binary swaps. | It cannot silently remove powers/items; transitions are previewed and reversible until committed. |
| PRG-011 | Expansion | P3 | Add cooperative **Concordance** progression for groups mixing Light and Darkness. | Rewards coordination without erasing each player's alignment identity. |
| PRG-012 | Expansion | P3 | Add post-rank-10 mastery challenges that unlock cosmetics/variants, not raw infinite scaling. | Boss balance and server budgets remain bounded. |
| PRG-013 | Guarantee | P2 | Verify chat prefixes against teams, nicknames, signed chat, death messages, and common formatting mods. | Prefix is compatible or cleanly disabled without mutating signed content. |
| PRG-014 | Enhancement | P3 | Add operator-import/export of a player's progression snapshot. | Signed/validated format, dry run, backups, and no arbitrary NBT injection. |

## 7. Artifacts, relics, Crucible, food, and loot

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| ART-001 | Guarantee | P1 | Live-test every one of the 262 catalogue rows for registry, model, translation, purpose, acquisition status, and save alias. | Generated catalogue and executable audit have no unexplained row. |
| ART-002 | Guarantee | P1 | Add loot-distribution simulations for every injected pool and modpack-weight interaction. | Expected items/hour and duplicate rates are published for vanilla and representative packs. |
| ART-003 | Enhancement | P2 | Give every ring/amulet a visible attunement school and one bounded situational modifier. | Variants are mechanically distinct without stack multiplicative abuse. |
| ART-004 | Enhancement | P2 | Add reservoir transfer UI showing exact main/auxiliary balance and pending cast shortfall. | No client-authored quantities; atomic server result. |
| ART-005 | Enhancement | P2 | Add ritual dagger blood-rune visuals and a clear health safety preview. | It never kills below the documented floor or bypasses health cost. |
| ART-006 | Enhancement | P2 | Give heart relics unique models/beat audio and explicit mutual-exclusion policy. | Multiple hearts cannot stack hidden death wards or unbounded passives. |
| ART-007 | Enhancement | P2 | Expand Philosopher's Stone through datapack transmutation recipes with entropy cost. | Recipes are discoverable, protected, non-circular, and cannot duplicate value. |
| ART-008 | Enhancement | P2 | Miniportal: named anchors, durability bar tooltip, and charged/empty model variants. | Still two charges, same dimension, async, and commit-after-arrival. |
| ART-009 | Enhancement | P2 | Flute: formation/stance wheel and guardian status. | Commands affect only owned eligible guardians under caps. |
| ART-010 | Enhancement | P2 | Empyrean Jewel: conspicuous consent-override ceremony and audit log for operators. | Target sees who overrode what; safe-zone/policy remains absolute. |
| ART-011 | Enhancement | P2 | Malignember: display eligible destructive actions and actual saved energy. | Tooltip is registry-derived and never promises an ineligible discount. |
| ART-012 | Enhancement | P2 | Give fossils/pages/jewels in-world archaeology clue chains instead of only XP values. | Each clue has source, interpretation, and non-circular reward. |
| ART-013 | Enhancement | P2 | Expand Crucible output previews with retained/lost components and exact level curve. | Server snapshot drives all displayed data; stale preview cannot commit. |
| ART-014 | Enhancement | P2 | Add alignment-specific weapon models/animations for the six conversion outputs. | All GUI/ground/first/third-person views pass asset QA. |
| ART-015 | Enhancement | P2 | Add weapon-archetype mastery challenges and signature finishers. | Proc remains cooldown-bounded and no ordinary weapon eclipses mythics. |
| ART-016 | Enhancement | P2 | Rebalance food affinity using tags so third-party foods opt into normal/foul/neutral behavior. | Unknown foods default neutral; no hidden starvation loop. |
| ART-017 | Defect | P2 | Resolve Wisdom Fruit's sole naturally unobtainable status with an Archivist orchard or explicitly hide it. | Catalogue and `NATURALLY_UNOBTAINABLE_ITEMS.md` reach zero accidental entries. |
| ART-018 | Expansion | P3 | Add an Archivist relic-restoration bench for damaged archaeology finds. | It complements rather than duplicates the Arcane Crucible. |
| ART-019 | Expansion | P3 | Add set-bonus lore collections for non-combat relics. | Bonuses are utility/cosmetic, bounded, and visible before activation. |
| ART-020 | Guarantee | P2 | Audit mythic artifact ownership in nested containers and inventory-component mods. | Policy explicitly supports or rejects each container with no ghost authorization. |

## 8. Realms, forces, structures, and lore

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| WRLD-001 | Enhancement | P1 | Replace flat authored realm bases with custom noise/surface rules while preserving existing site coordinates. | Old realms migrate safely; new chunks match white/dark mindscape art direction. |
| WRLD-002 | Enhancement | P1 | Replace procedural block boxes with authored structure templates and processor lists. | Six sites/realm have unique layouts, loot, puzzles, and bounded placement. |
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
| MOB-001 | Guarantee | P1 | Run skin/UV/animation screenshot tests for every player-model entity and all spawn eggs. | No transparent/misaligned face at any supported skin type/view. |
| MOB-002 | Enhancement | P1 | Give Darkness Creatures and Radiant Sentinels tactical melee/ranged transitions, cover, and retreat. | AI is bounded, faction-safe, and does not pathfind every tick. |
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
| SHD-001 | Guarantee | P0 | Fuzz the `shadow,` parser, signing interception, Unicode, long messages, and permission boundaries. | No command injection, unsigned-chat impersonation, crash, or unintended public reply. |
| SHD-002 | Guarantee | P1 | Prove revealed/hidden tracking, death, recall, owner logout, dimension transfer, and artifact loss with two observers. | Exactly one body; visibility and replies match global/private state. |
| SHD-003 | Guarantee | P1 | Audit every Shadow-cast innate against entity-safe adapters and crystal exclusion. | Catalogue lists 23+3 only; unsupported actions fail with a reason. |
| SHD-004 | Enhancement | P1 | Build a structured intent planner with explicit goals, prerequisites, costs, and rollback rather than keyword-only chains. | Every task shows current step/failure and remains interruptible. |
| SHD-005 | Enhancement | P1 | Expand failure diagnosis from last cast to recent target, protection, cooldown, energy, geometry, and source-item evidence. | Answer cites the exact typed server cause and timestamp without leaking secrets. |
| SHD-006 | Enhancement | P2 | Add owner-approved construction/harvest errands using safe allowlisted actions. | Never mines protected blocks, opens private containers, or loads distant chunks. |
| SHD-007 | Enhancement | P2 | Add item delivery paths through actual nearby inventories only with explicit owner permission. | No conjured duplication or third-party inventory access. |
| SHD-008 | Enhancement | P2 | Teach combat planner encounter roles: peel, interrupt, rescue, suppress, execute, disengage. | Role choice is inspectable, bounded, friendly-fire safe, and preference-aware. |
| SHD-009 | Enhancement | P2 | Learn from explicit owner feedback (`good`, `don't do that`) separately from combat reward. | Learning remains owner-local, capped, resettable, and cannot alter hard safety. |
| SHD-010 | Enhancement | P2 | Add spatial memory of temporary owner-designated places, not global world surveillance. | Bounded named points, explicit delete, dimension-safe, and no forced loading. |
| SHD-011 | Enhancement | P2 | Expand offline lore/registry knowledge to all 262 catalogue rows, 64 actions, ranks, realms, and interaction rules. | Generated knowledge coverage report reaches 100% with source links. |
| SHD-012 | Enhancement | P2 | Add conversational continuity for pronouns and follow-up questions. | Resolution uses only 24-turn redacted memory and admits ambiguity. |
| SHD-013 | Enhancement | P2 | Make Shadow's subtle ulterior Darkness agenda stateful but never deceptive about mechanics/safety. | Tone tests distinguish persuasion from fabricated facts. |
| SHD-014 | Enhancement | P2 | Add spoken/visual combat callouts with frequency controls. | Critical warnings survive; chatter respects global/private and accessibility settings. |
| SHD-015 | Enhancement | P2 | Add owner-visible Shadow energy/status HUD only while relevant. | No permanent clutter; server snapshot is authoritative. |
| SHD-016 | Research | P2 | Evaluate a local small-language-model provider for optional private dialogue. | Benchmarks cover latency, memory, moderation, redaction, CPU/RAM, and offline fallback. |
| SHD-017 | Guarantee | P1 | Threat-model remote dialogue endpoint, credential loading, redaction, and malicious response text. | Security review plus tests prove no server command/item/recipe authority is delegated. |
| SHD-018 | Enhancement | P3 | Add Shadow/Herald/First Vessel relationship memories and evolving dialogue. | State is compact, migratable, and cannot change gameplay permissions invisibly. |
| SHD-019 | Enhancement | P3 | Allow Shadow to coordinate owned Hollowed formations. | Total AI/cast quotas stay shared and capped. |
| SHD-020 | Guarantee | P2 | Add deterministic combat-learning replay and export for debugging. | Export is redacted, bounded, and faithfully reproduces action scoring. |

## 11. UI, accessibility, visuals, and audio

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| UX-001 | Guarantee | P1 | Screenshot-test every GUI scale, resolution/aspect ratio, extra-heart count, mount, air, armour, spectator, and accessibility combination. | Golden images prove no overlap/clipping and exactly ten hunger-aligned symbols. |
| UX-002 | Enhancement | P1 | Redraw energy symbols after in-game comparison with vanilla hearts/armour/hunger pixel weight. | Five states remain distinguishable at 1× GUI scale and color-blind palettes. |
| UX-003 | Enhancement | P1 | Add configurable HUD anchor/margins while preserving vanilla-aligned default. | Layout validator prevents overlap and reset returns exact default. |
| UX-004 | Enhancement | P2 | Replace artifact catalogue pagination with virtual scroll, filters, and recent actions. | Any action is bindable in two interactions after search. |
| UX-005 | Enhancement | P2 | Add hold-to-preview wheel aim and optional release-to-cast mode. | Default behavior remains safe; narration/keyboard parity. |
| UX-006 | Enhancement | P2 | Add cooldown/energy insufficiency forecasts directly on wheel segments. | Numbers match server snapshots and update without per-frame packets. |
| UX-007 | Enhancement | P2 | Add complete controller/gamepad navigation for all screens and selectors. | Every action is reachable without mouse and focus never traps. |
| UX-008 | Guarantee | P2 | Complete narration labels, order, live-region throttling, and high-contrast focus for every custom screen. | Automated accessibility audit plus manual screen-reader pass. |
| UX-009 | Enhancement | P2 | Add configurable reduced flashes, tinnitus, camera shake, FOV, and large-beam opacity. | Celestial Ruin remains mechanically readable with all comfort options enabled. |
| UX-010 | Enhancement | P2 | Add spell/crystal/artifact favorites to the first-join guide as discoverable diagrams. | Guide remains concise and updates from current bindings. |
| VFX-001 | Enhancement | P1 | Replace remaining generic End Rod, Reverse Portal, Soul, Cloud, and potion-style visuals with authored semantic sprites where appropriate. | Visual audit classifies every remaining vanilla particle as deliberate. |
| VFX-002 | Guarantee | P1 | Verify every mod-created status effect uses hidden particles/icons unless explicitly documented. | Source audit rejects direct visible `MobEffectInstance` construction. |
| VFX-003 | Enhancement | P1 | Establish per-action near-camera particle exclusion/cone limits for Lightning and Fireball. | First-person captures preserve aim visibility at all ranks. |
| VFX-004 | Enhancement | P2 | Add material-aware impact decals/scars for beam, slam, thunderclap, ice, and fire. | Decals are bounded, protected, and restore/expire cleanly. |
| VFX-005 | Enhancement | P2 | Create unique silhouettes for each rank-10 transformation. | A distant observer identifies power/alignment without reading chat. |
| VFX-006 | Enhancement | P2 | Add animation controllers for Shadow, guardians, Heralds, and First Vessel casting poses. | Client/server action timing stays synchronized under latency. |
| VFX-007 | Enhancement | P2 | Expand 14 sounds into layered near/mid/far variants and occlusion-aware mixes. | Loud events do not clip; volume falls off correctly; subtitles exist. |
| VFX-008 | Enhancement | P2 | Add dedicated short tinnitus/ringing variants for beam collision and boss impacts. | No long Celestial Ruin effect is reused for small collisions. |
| VFX-009 | Enhancement | P2 | Add custom Light Realm sky shader/skybox with accessibility fallback. | Pure white target remains stable across render distance/resource packs. |
| VFX-010 | Enhancement | P2 | Add weather/biome-responsive ancient rune color grading. | Meaningful action colors remain consistent and color-blind safe. |
| VFX-011 | Guarantee | P2 | Audit texture mipmaps, alpha seams, spawn eggs, item transforms, and atlas bleeding. | Pixel-perfect captures at all mip levels/views. |
| VFX-012 | Expansion | P3 | Add original music/ambient soundscapes for Light, Dark, Middleworld, bosses, and Eclipse events. | Loop points, licensing, subtitles, volume categories, and performance validated. |

## 12. Cross-system interactions and counterplay

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| INT-001 | Guarantee | P0 | Turn all 2,080 generated action pairs into executable deterministic resolver tests. | A mutation in any pair rule fails a focused test. |
| INT-002 | Guarantee | P1 | Add live tests for each physical collision family: beam/beam, projectile/projectile, projectile/field, beam/field, force/block, body/field. | Real runtime presence reaches the expected resolver once and cleans up. |
| INT-003 | Guarantee | P1 | Build three-way interaction arbitration rules for simultaneous impacts. | Ordering is commutative where promised and otherwise explicitly prioritized. |
| INT-004 | Guarantee | P1 | Add same-tick conflict tests for amethyst, forcefield, safe zone, consent override, anchor, and realm confinement. | One documented precedence table matches every runtime entrypoint. |
| INT-005 | Enhancement | P2 | Add visible interaction sigils naming counter/resonance without chat spam. | Short action-bar/icon cue appears once per bounded collision. |
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
| INT-016 | Guarantee | P2 | Add third-party projectile/damage/effect compatibility fixtures. | Unknown sources follow safe defaults and never crash resolver casts. |

## 13. Multiplayer, administration, compatibility, and API

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| NET-001 | Guarantee | P0 | Protocol-version handshake with clear mismatch disconnect text. | Old/new client-server combinations fail before unsafe payload decoding. |
| NET-002 | Guarantee | P1 | Fuzz every serverbound packet lane and snapshot codec. | Invalid IDs, sizes, order, rate, and stale revisions cannot crash or mutate state. |
| NET-003 | Enhancement | P1 | Integrate optional claim/protection APIs through a documented adapter interface. | Representative claim mods block damage, movement, terrain, portals, and rituals consistently. |
| NET-004 | Enhancement | P2 | Add permissions-node integration alongside vanilla operator levels. | Server owners can separate diagnose, testing, travel, assign, recover, and boss controls. |
| NET-005 | Enhancement | P2 | Add operator audit events for consent overrides, recovery, catastrophic rituals, testing mode, and forced travel. | Structured log identifies actor/action/result without secrets. |
| NET-006 | Enhancement | P2 | Add `/powers diagnose export` redacted JSON snapshot. | Export is bounded, schema-versioned, and contains no chat/API key/private coordinates by default. |
| NET-007 | Enhancement | P2 | Add per-world/dimension policy overrides. | Fallback order is deterministic and shown by diagnose. |
| NET-008 | Enhancement | P2 | Add config validation report on reload rather than silently only clamping. | Operator sees original, sanitized value, reason, and active revision. |
| NET-009 | Enhancement | P2 | Add formal public integration API for custom powers, cast sources, actions, presences, and protection. | API has lifecycle contract, examples, compatibility tests, and semantic versioning. |
| NET-010 | Enhancement | P2 | Add datapack reload migration for action/menu keys without reconnect. | Active casts either finish on old revision or cancel safely; clients receive one revision. |
| NET-011 | Guarantee | P2 | Validate behavior with common performance/render/voice/claim/inventory mods. | Published compatibility matrix names tested versions and known limitations. |
| NET-012 | Enhancement | P3 | Add localization framework and first complete non-English translation. | No string concatenation blocks grammar; all UI/tooltips/subtitles localize. |
| NET-013 | Enhancement | P3 | Add server presets: lore survival, PvP-balanced, cinematic testing, low-spec. | Presets are explicit diffs and never overwrite hand-edited config without confirmation. |
| NET-014 | Guarantee | P2 | Add privacy review for Shadow global chat and remote dialogue provider. | Documentation and tests cover visibility, redaction, retention, and opt-in. |

## 14. Testing, observability, release, and documentation

| ID | Kind | Priority | Improvement | Acceptance condition |
| --- | --- | --- | --- | --- |
| QA-001 | Guarantee | P0 | Run clean full build, JUnit, all GameTests, dedicated server, client smoke, visual suite, soak, and manual acceptance before release tags. | One signed release report contains exact commands, revisions, counts, logs, and unresolved limitations. |
| QA-002 | Guarantee | P0 | Add CI jobs for Java 25, Fabric server boot, GameTest, assets, generated-doc drift, and save migration. | Pull requests cannot merge with a failing mandatory job. |
| QA-003 | Guarantee | P1 | Split unit tests by subsystem and remove shared mutable Gradle result races. | Parallel filtered suites produce isolated reports and deterministic outcomes. |
| QA-004 | Guarantee | P1 | Add headless client screenshot harness for HUD, wheel, catalogue, maze, advancements, locator, teleport, realm sky, and entity skins. | Golden review supports approved baseline updates and reduced-motion variants. |
| QA-005 | Guarantee | P1 | Add manual acceptance checklist for every registered action/item/entity/screen/command. | Tester records pass/fail/evidence/build ID; omissions are visible. |
| QA-006 | Guarantee | P1 | Add long-duration world soak with forced restart every few minutes. | No leaked ticket, index, field, summon, body, freeze, or Ruin state over 24 hours. |
| QA-007 | Guarantee | P1 | Add property-based tests for energy/cooldown/reservoir arithmetic and overflow. | Balances never become negative, exceed cap, duplicate, or wrap for arbitrary inputs. |
| QA-008 | Guarantee | P1 | Add mutation testing for protection, consent, realm, transaction, and cleanup kernels. | Minimum mutation score is enforced for critical packages. |
| QA-009 | Guarantee | P2 | Add network latency/loss/reorder simulation for menus, movement control, wheel binding, and FX. | State converges without double cast, ghost selection, or unsafe prediction. |
| QA-010 | Guarantee | P2 | Add test worlds for claims, borders, low ceilings, void, fluids, mounts, passengers, portals, and modded dimensions. | Each critical action has at least one hostile-environment scenario. |
| QA-011 | Enhancement | P2 | Add performance regression budgets to CI for allocations and deterministic workload units. | Significant regressions fail with subsystem attribution. |
| QA-012 | Enhancement | P2 | Add crash-report sections for active POWERS sessions and last typed failure. | Report is useful, bounded, and redacted. |
| QA-013 | Guarantee | P2 | Validate every README number/link against registries or a named verification source. | Documentation check catches count, version, command, and path drift. |
| QA-014 | Guarantee | P2 | Generate the item/action/rank/interaction appendices in CI and fail dirty diffs. | Production registries and committed docs cannot diverge. |
| QA-015 | Enhancement | P2 | Add in-game operator test dashboard summarizing coverage and recent failures. | Dashboard is read-only outside explicit testing actions and has negligible idle cost. |
| QA-016 | Guarantee | P2 | Audit all comments for intent/invariants rather than line-by-line narration. | Source quality check rejects stale TODOs, misleading claims, and public APIs without contracts. |
| QA-017 | Guarantee | P2 | Add resource-pack fallback and missing-asset tests. | Missing optional custom art degrades visibly and never crashes model baking. |
| QA-018 | Guarantee | P2 | Add deterministic loot/recipe/tag data validation against vanilla and modded registries. | Invalid or circular entries fail before world load. |
| QA-019 | Enhancement | P3 | Produce a concise operator handbook separate from the player README. | Includes install, config, backups, recovery, diagnostics, permissions, and catastrophe policy. |
| QA-020 | Enhancement | P3 | Produce an in-game bestiary/ritual codex from the same generated data. | It contains no unearned spoilers and cannot drift from registries. |

## Recommended delivery order

1. **Release guarantees:** all P0 items, then P1 save/lifecycle/network/interaction proof.
2. **Measured performance:** real-player profiling, index instrumentation, fair work quotas, and ticket backpressure.
3. **Usability and visual consistency:** HUD/screenshot matrix, artifact library virtualization, authored particle audit, spell guidance.
4. **System depth:** rank/quest rebalance, individual power refinements, relic and Crucible expansion, Shadow task planner.
5. **World expansion:** authored structures, factions, realm ecology, new mobs/bosses, cooperative Concord content.

Each completed subsystem should update its registry-derived documentation, migration notes, changelog, automated evidence, and manual acceptance record in the same commit. New creative work must first define counterplay, protection policy, persistence, cleanup, workload budget, accessibility presentation, and cross-system interaction rows.
