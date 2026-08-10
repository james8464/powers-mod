# Final requirement matrix

This matrix maps the requested finalisation work to implementation and repeatable evidence. `Partial` is used wherever an advertised manual/client proof or the larger realm-expansion scope is still outstanding; generated hash inventories are not presented as semantic review.

| ID | Requirement | Status | Evidence |
| --- | --- | --- | --- |
| R01 | Full production-code audit | Partial | Every production/client Java path was inventoried and traced through registration, authority, lifecycle, targeting, persistence, networking, bounded-work, and cleanup passes; exact manifests and structural checks prevent unreviewed drift, while live playtesting remains open proof |
| R02 | Best practices, lifecycle cleanup, and explanatory comments | Complete | Source audit plus bounded managers, immutable rule objects, package documentation, and shutdown/disconnect cleanup tests |
| R03 | Non-item asset audit | Complete | `docs/quality/asset-audit.md`; `scripts/audit_non_item_assets.py`; `validateNonItemAssetAudit` |
| R04 | Intentionally absent crystal recipes | Complete | `validatePowerResources` rejects accidental crystal recipes; README records the deliberate omission |
| R05 | Hidden status-effect particles | Complete | `PowerStatusEffects.hidden`; `PowerStatusEffectsTest`; production constructor audit |
| R06 | Minimalist power messaging | Complete | `PowerMessages`; routine feedback uses actionbar overlays; chat is reserved for exceptional information |
| R07 | Bounded magical presentation | Complete | `MagicPresentation`, `ParticleBudget`, `TimeStopFx`, client distance/reduced-motion budgets and focused tests |
| R08 | Exhaustive power collision system | Complete | 82-action generated catalogue, all 3,403 pairs in `docs/interactions/interaction-matrix.csv`, drift tests |
| R09 | Lightning without cooldown | Complete | `LightningStrikeAbility`; power-definition and activation tests retain validation and energy gates |
| R10 | Player Size Morphing with selectable scale | Complete | `SizeMorphRules`; `SizeMorphRulesTest`; eight sizes with distance-based drain |
| R11 | Grimoire reliability and all remaining spells | Complete | 21 server-owned spells, channel revalidation, spell tests, and complete README table |
| R12 | Dimensional Anchor as a grimoire spell | Complete | Deep Grimoire registration, spell executor, anchor manager, README |
| R13 | Mob-compatible powers | Complete | Possession, named remote view, Energy Drain, targeting/protection paths accept suitable living entities |
| R14 | Player-like test actor | Complete | `PowerTestActor`, renderer/model, spawn egg, attributes, target AI, resources |
| R15 | Elemental selection instead of inconvenient cycling | Complete | Persistent flame/frost/storm/earth selection and canonical delegated actions |
| R16 | Global Time Freeze and removed slowdown | Complete | Shared tick-freeze ownership, restoration, cleanup, visuals, tests; obsolete self-slow path removed |
| R17 | Mindscape body vulnerability | Complete | Skin-matched body proxy sessions mirror damage for projection, possession, dreamwalking, marking, and realm travel |
| R18 | Light and Dark realm expansion | Partial | White Light sky renderer, six persistent memory sites per realm, lore/rewards, mindscape biomes and resource validation exist; the wider libraries/labyrinths/settlements/boss expansion remains open |
| R19 | Realm departure confinement | Complete | Shared travel policy, death confinement, level/tag gates, body return/admin recovery exemptions and tests |
| R20 | Unloaded-chunk and mod-dimension teleportation | Complete | Tick-bounded asynchronous chunk loader, full destination revalidation, dynamic dimension menu and full-ID tooltips |
| R21 | Named-mob remote viewing | Complete | Lifecycle name index with bounded fallback, uniqueness refusal, player consent, mob camera and vulnerable body |
| R22 | Rank maze, titles, visibility, and quests | Complete | Two 28-node graphs, impactful perks, prefix sync, exclusive advancement roots, long-form normal and atrocity darkness trackers |
| R23 | Rank scaling for every innate power only | Complete | Central `PowerScaling`, per-power branches, caps and tests; spells/crystals/artifacts keep equipment values |
| R24 | Vanilla-aligned ten-symbol energy HUD | Complete | `EnergyHudRenderer`, `HudLayout`, five full/half/empty symbol palettes, hunger alignment and `HudLayoutTest` |
| R25 | Compact power-slot HUD | Complete | Three right-edge rune medallions with key, toggle, rank, cooldown, nested-mode and reduced-motion states |
| R26 | Survival propulsion flight | Complete | Server-owned directional/sprint flight without creative-mode ability grants |
| R27 | Alignment-exclusive innate powers | Complete | Three radiant and three darkness powers, loadout migration and guaranteed compatible assignment tests |
| R28 | Natural tiered runestones | Complete | Craftable upgrade chain, additive loot injection, 40-600 restoration tiers and cooldowns |
| R29 | Darkness Creature | Complete | Black player model, Dark Realm spawn, player-like combat, lightning/fireball, tag targeting, summon caps |
| R30 | Shadow Sword carrier curse and protection | Complete | Dark-grey identity, hidden Blindness/Wither, lightning guardian response, authorization and cap tests |
| R31 | Shadow Sword complete power access | Complete | All 28 innate and 13 crystal routes plus grouped authenticated menu and corruption presentation |
| R32 | Shadow Sword apotheosis and custom powers | Complete | Exactly three unique rites; rank-10 cooldown bypass and 900-energy refill tests |
| R33 | Heavenly Partisan counterpart | Complete | Eleven unique light rites, all innate/crystal routes, sentinel protection and rank-10 support scaling |
| R34 | Living Darkness and Pure Light spread | Complete | Random-tick protected conversion, indexed auras, rank energy refill, Wither, immunity tag and GameTests |
| R35 | Catastrophic opposed-force clash | Complete | 48-block bounded purge, extreme damage/throw, resumable cursor, particles/sounds and collision evidence |
| R36 | Celestial Ruin containment spell | Complete | One-minute persistent 100-block beam, progressive chunk tickets, restart-safe cursor, 120-block crater and up-to-50,000 damage across 2,048 blocks |
| R37 | Arcane Crucible progression | Complete | Atomic three-stage forge, compatibility API/tag, safe component transfer, hopper/break/stale-menu tests |
| R38 | Owner-private shadow companion | Complete | Owner-only packets/rendering, authenticated interaction, offline lore, bounded optional provider and privacy tests |
| R39 | First Vessel boss | Complete | 5,000 vitality, multiplayer snapshot scaling, 28-power tactical catalogue, phases, counterplay, ritual, loot/advancement |
| R40 | Multiplayer performance and packet abuse resistance | Complete | Bounded candidates/cursors/summons/FX, rate lanes, string codec caps, asynchronous travel/provider and focused tests |
| R41 | Complete player documentation | Partial | README covers current catalogues and core systems; it must remain synchronized as the open realm/lore expansion lands |
| R42 | Game tests and release validation harness | Partial | 511 JUnit tests, 16 live Fabric GameTests, resource/code/asset validators and dedicated run tasks pass; the requested every-ability live matrix and automated client screenshots are not yet complete |

## Release interpretation

The automated suite proves deterministic rules, data/resource integrity, protocol bounds, and live registered server behaviors. It cannot prove subjective feel at every GUI scale or compatibility with every third-party mod combination. Before moving this build onto a valuable server, run the manual checklist in `docs/verification/2026-08-10-playtest-completion.md` against a backed-up test world.
