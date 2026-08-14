# Changelog

## Unreleased

- Allowed the opt-in connected-player profiler to select one accepted population at a time, enabling reproducible parallel 10/50/100-player evidence runs from immutable copies of the same commit without changing their workloads or budgets.
- Counted real-client activation results at the server-authoritative packet boundary so rendered-client profiles can prove successful casts instead of inferring them from presentation output.
- Required connected-player profiles to satisfy both the exact tick-sample count and full wall-clock duration, retaining exactly 36,000 samples while a real server crosses the final subsecond boundary.

- Closed QA-005's current 427-row live acceptance register: all 260 items, every action and artifact route, Middleworld, both alignment-exclusive advancement screens, entities, commands, and systems now carry exact-build PASS evidence; the development client replays vanilla click-style keys so UI captures exercise Minecraft's real screen path.
- Kept artifact-started multi-tick powers alive while their authorised Shadow Sword or Heavenly Partisan remains in top-level inventory, and made item removal, lost alignment, death, logout, or shutdown end them safely without retaining player/world references.
- Made server-owned survival Flight compatible with vanilla `allow-flight=false` without creative-flight flags, added exact client-camera direction and artifact-coordinate travel to the acceptance harness, and expanded the live suite to 83 GameTests with artifact Astral Projection and aimed Partisan combat coverage.
- Prevented saved living-force frontier repair from re-entering Minecraft's chunk loader and watchdog-crashing a dedicated server when a force-heavy realm chunk reloads.
- Made administrator dimension travel load its fixed arrival chunk with a bounded ticket and land at that dimension's terrain height instead of an unsafe hard-coded void coordinate.
- Fixed `/powers travel` so full namespaced dimension IDs such as `minecraft:the_nether` are parsed as one destination argument.
- Fixed realm-crystal mob lifecycle ownership so terrain or movement cannot strand a carried mob outside the two-block return recapture radius; body return, death, and stale-session cleanup now recall every loaded mob owned by that journey.
- Fixed Blue Crystal Dreamwalking's spectator-mode exit: right-click now sends a dedicated authenticated release request, restores the physical body and original game mode, and cannot release another player's session.
- Completed rendered-client acceptance for all innate powers, including distant consent-free cohort travel, retained toggle reconciliation, a real two-times Double Health baseline, and ownership-safe Time Freeze release. The development-only acceptance script can now submit the exact validated destination payload used by the Time Shift screen.

- Preserved active innate toggles when allegiance reconciliation replaces a different slot; dropped powers still clean up exactly once, while retained Size Morphing and Time Freeze ownership no longer flicker or silently restart.
- Fixed mind-body return at player-list lifecycle boundaries: disconnect and early join recovery now
  complete through the live player reference, while delayed chunk callbacks continue to resolve the
  owner by UUID and cannot retain a stale entity.
- Prevented Energy and Void beams from crashing a dedicated server when protection, a ward, or
  another terminal counters the ray exactly at the caster's eye position; origin-local counters now
  resolve normally without publishing impossible zero-length collision geometry.
- Added an explicit development-only four-client acceptance runner that replays real commands,
  chat, innate activation/selection and vanilla respawn packets, and labelled render captures from strictly validated
  tick scripts; added a fail-closed evidence ledger so generated manual results cannot cite missing
  files, unknown rows, duplicate outcomes, or unsupported statuses.
- Corrected QA-005 visual acceptance defects: rank-maze titles now fit without clipping, Shadow's combat wheel keeps recognisable names at normal and compact scales, automated captures drain prior realm FX, and arrival lightning surrounds rather than intersects the traveller's camera.
- Corrected Ward-Breaking so a temporarily suppressed powered Amethyst Ward cannot continue poisoning its caster through the natural-amethyst index; added a production-entrypoint regression and revert-based mutation proof.
- Made ordinary GameTests reproducible by invalidating conditional test metadata when the connected-profile flag changes and resetting only the generated GameTest world before each launch.
- Extracted player attachment schema registration and Celestial Ruin warning presentation into focused owners without changing saved data or ritual behaviour.
- Recorded the owner-approved selected Stage 1–8 programme, exclusions, measurement gates, and final release-evidence requirements.

## 1.0.2 - 2026-08-11

- Registered the complete persistent player-attachment schema during Fabric bootstrap, before any saved player is decoded; ranks, energy, spell/crystal selections, and artifact loadouts now survive full client/server restarts instead of being discarded as unknown attachment types.
- Kept Celestial Ruin's full-height omen visible through progressive impact-chunk preparation, bypassed vanilla's 32-block particle limiter for distant observers, and completed every numbered warning/failure translation so live messages cannot leak raw keys.
- Made Cartographer's Star teach its accepted query grammar, wrap its guidance within the locator panel, and translate every success/failure result instead of leaking raw language keys.
- Moved the first Astral Projection frame just beyond and above its vulnerable body proxy so the owner no longer begins inside the mannequin skin.
- Added a real rendered-client acceptance harness with live Light/Dark Crystal journeys, body recovery, 19 HUD/screen/realm captures, compact layouts, and checked operator commands; expanded the live server suite to 69 GameTests and published an honest 429-identity campaign report.
- Corrected Rainbow Crystal's missing seventh Middleworld mode, high-GUI-scale artifact-wheel crowding, untranslated Locator prompts, low-contrast Crucible labels, the omitted Shadow acceptance entry, and false-positive dedicated-server/checklist evidence.
- Completed the 25-item Queue 1 pass: practical spell inspection/feedback, authoritative crystal and power state, relic previews/transfers, accessible artifact/Rainbow selectors, Shadow/energy diagnostics, permissions integration, compatibility contracts, resource fallbacks, and bounded collision presentation.
- Added Grave Recall retention/bearing, exact Dispel preflight, renewable Dimensional Anchor feedback, Soul Link topology/caps, Chrono Stop owner/deadline state, Double Health HUD/anti-toggle rules, Forcefield ownership/cracks/merge/opt-out, and selectable pull/orbit/repel Gravity modes including artifact snapshots.
- Added named charged Miniportals, Ritual Dagger safety previews, registry-derived Malignember savings, exact reservoir transfers, persisted safe-default release-to-cast, contextual Shadow status, first-awakening binding diagrams, short beam/boss ringing sounds, six granular permission nodes, crash/energy diagnostics, signed-chat-safe prefixes, target capability contracts, and optional-asset procedural fallbacks.
- Centralized all player-facing cooldown conversion, added live wheel energy-shortfall colouring, a deliberate crouch-use Cinderheart release, capped tag-driven lightning conduction with harmless rod grounding, and Time Freeze drain/MSPT forecasts.
- Added conspicuous Empyrean consent-override notices and bounded privileged-action auditing, aggregate-only atomic diagnostic export, and redacted config clamp/default reports with active revisions.
- Gave Wisdom Fruit a low-rate Realm Archive source, added generated 56-node rank documentation, hardened recipe/loot/tag graph validation, and isolated the synthetic workload plus Java-allocation budget in CI.
- Closed the automated P0/P1 stabilization programme with versioned save fixtures, transactional cast rollback, fatal body ordering, Time Freeze/Celestial Ruin recovery, artifact revocation, protocol negotiation, packet fuzzing, executable interaction/lifecycle matrices, mutation enforcement, live ritual/crystal/rank/collision GameTests, protection adapters, realm templates, tactical guardians, and deterministic visual goldens.
- Added persistent anonymous Light/Dark quest-completion telemetry, an opt-in full-tick JFR profiler with p95/p99 and connected-player/work peaks, a manual 10/50/100 embedded-player profile scenario, and an isolated repeated-restart soak harness that cannot touch the normal development world.
- Added hostile randomized arithmetic properties for energy, cooldowns, and reservoirs; proved POWERS attribute cleanup cannot remove foreign modifiers; expanded the ordinary crystal-use GameTest matrix; and fixed Green Life Bloom caster healing plus Orange Echo collision-safe three-clone placement.
- Replaced remaining client generic magic placeholders with authored semantic particles and generated an omission-visible manual checklist for every action, item, entity, screen, and command.
- Rewrote the README from authoritative registries and verification evidence into a concise complete player/operator manual, linked exhaustive per-item/per-rank/per-action/per-interaction appendices, and separated 264 unshipped guarantees, enhancements, expansions, defects, and research tasks into a prioritized acceptance-driven backlog.
- Rebuilt Shadow as one persistent player-model magic participant with owner skin, global/private presentation, typed conversation tasks, bounded item retrieval/conjuration, its own Darkness energy and all 23 non-crystal innates plus exactly three sword rites.
- Added server-only tactical Shadow combat: close, skirmish, ranged, rescue and recovery modes; capped targeting and firing-lane checks; real named executors; owner-local bounded contextual learning; diagnostics and an operator learning reset.
- Removed routine Adventure-mode coercion from Light/Dark mindscape entry and successful confinement while retaining safe migration of legacy Adventure sessions and Spectator only for a failed locked recovery.
- Added a live Energy Beam/Void Beam intersection: both rays terminate at the first crossing, release a bounded no-grief pressure blast, mark both casters with visual lightning, and play one short celestial ring.
- Defined and generated all 672 form/source/termination outcomes alongside the existing 2,080 action-pair matrix; fatal detached-avatar or physical-proxy damage now recalls and kills the physical player, while a dead controlled vessel returns its controller under Divine Wrath.
- Made revealed Shadow a globally visible, skin-matched, equipment-free mortal body. Killing it dismisses only the manifestation; its diagnostic/lore memory survives and it can be summoned again.
- Reconciled artifact ownership even during global Time Stop, so dropping the Shadow Sword or Partisan cannot preserve routed flight, invisibility, or companion state.
- Hardened mindscape travel so player-controlled routes may move within the current Light/Dark realm but cannot leave it except through the qualified body-return path; fatal soul recall and operator recovery remain separate internal routes.
- Replaced the active ritual roster with twelve practical spells: Soul Compass, Augury, Cartographer's Star, Celestial Ruin, Dimensional Anchor, Blood Reading, Grave Recall, Purification Circle, Verdant Tending, Hearth Sanctuary, Ward-Breaking Ritual, and Dispel. Infernal IDs remain hidden dormant aliases for save compatibility.
- Removed retired spell amplification, veil, counterspell, weather, binding, essence-distillation, Infernal casting, and obsolete field-creation paths. Spells use only existing magic energy and remain isolated from innate rank scaling.
- Added persistent exact last-death reporting, bounded world/biome/landmark search, independent three-block Hearth forcefields, and typed failure reporting for practical rituals.
- Made the Empyrean Jewel override every consent category for a fixed energy surcharge without bypassing safe zones, and converted soulstones/Soul Matrix into atomic energy reservoirs instead of soul/essence storage.
- Gave attunements distinct bounded recovery, specialised all five heart relics, made Bloodstone a timed lethal-damage ward, retained Malignember's explicit destructive-cost reduction, and documented every imported item's implemented role or hidden compatibility status.
- Added Shadow's private 16-attempt, five-minute server-authoritative failure journal, exact diagnostic answers, repetition hints, lifecycle clearing, and optional remote wording that cannot replace the recorded cause.
- Split the former 409-line Fabric entrypoint into a stable facade, ordered content bootstrap, lifecycle owner and single-pass player ticker while preserving callback order and save behavior.
- Removed four proven runtime-orphan source types and their orphan-only tests, and moved superseded implementation drafts out of the release tree while retaining their exact Git history.
- Added production-type reachability, bootstrap-architecture and release-version gates; corrected the packaged mod identity from stale 1.0.0 metadata to 1.0.2.
- Rebuilt `test.sh` as a location-independent Java-25 launcher with doctor, client, server, clean-check, GameTest and multiplayer-soak modes; GameTest now seeds its isolated EULA/properties without false error telemetry.
- Added a registry-synchronized acceptance catalogue, operator coverage report and bounded seven-target test arena; extended the real-server suite with Light Crystal travel, operator command-tree and historical Cinderheart block-impact regression scenarios.
- Expanded the deterministic 10/50/100-player soak to exercise live magic/field/ward/name spatial indexes, rotating work queues and exact cleanup in addition to packet, particle, entity-scan and chunk-ticket limits.
- Attributed every principal spatial-index diagnostic to its dimension, including bounded query, candidate, miss, fallback, stale-removal, and estimated-memory counters.
- Removed the five retired innate powers (Cozy Campfire, Frost Nova, Elemental Blast, Ground Slam, and Shadow Step), their runtime managers, selectors, protocol state, boss/artifact adapters, translations, and collision entries.
- Removed every automatically assigned innate passive and the unconditional player aura dust emitter; saved loadouts now migrate deterministically to valid allegiance-safe powers.
- Reduced the canonical innate roster to 23 and regenerated its exhaustive interaction documentation and audit manifests.
- Made every indefinite innate toggle continuously consume energy; Time Freeze now drains at least 15% of the caster's capacity each second.
- Replaced raw invisibility flags with a particle-free, icon-free, amplifier-255 effect owned and safely removed by POWERS.
- Added the zero-cooldown crouching Plant Healing pulse for injured players inside an inclusive two-block radius while retaining ordinary aimed plant growth.
- Fixed the rank-3 Cinderheart crash on block impacts by making the direct-target forcefield check null-safe.
- Replaced generic rank percentages with 253 complete, power-specific authored level profiles and boss-scale capstones while preserving strict innate/spell/crystal/artifact source isolation.
- Extended player Size Morphing save-safely to rank-gated `0.125×`, `2.5×`, `3×`, and `4×` forms; forcefield integrity and Double Health capacity now use their authored rank profiles.
- Repainted both Rank Maze panels as alignment-specific carved-stone/blackstone pixel art, gave the B-key screen a concise title, and replaced misleading percentage tooltips with the transformations the selected title actually unlocks.
- Rebuilt the energy atlas as five crisp vanilla-scale empty/half/full glyph families and moved the ten-symbol hunger-aligned row above conditional air and mount-health rows.
- Added a one-time persistent vanilla written-book guide and completed `/powers testing reset` plus testing-state diagnostics.
- Rebuilt Teleport around vulnerable five-second origin/destination storms, unloaded-chunk loading, uniquely named players or mobs, bounded companion transfer, and duplicate-storm rejection; Middleworld is never exposed as an ordinary destination.
- Made detached mind avatars and their physical mannequins vulnerable fatal surfaces with recall-before-death, including sacrificial forcefield interception, exact operator recovery, persistent Middleworld origins, and atomic nested-session prevention.
- Upgraded Vessel Possession to server-authoritative movement, aim, jumping, crouching, hotbar, and attack control for consented players and ordinary mobs, with original mob AI restoration and higher-rank resistance.
- Removed the obsolete Portal Rift crystal action. Indigo now owns only the persistent Middleworld journey, and Light/Dark crystals support a consent-safe crouching group journey of up to 16 players.
- Made integrity wards follow the physical side of an active mind-body tether, so a shield still sacrifices itself against complete overkill without making the detached avatar damageable.
- Increased the Celestial Ruin warning beam to a dense, client-bounded 100-block column visible across 6,000 blocks and extended its whiteout/tinnitus sequence while the boss-scale living shockwave reaches the same 6,000-block radius.
- Replaced borrowed spawn-egg references with four original self-contained egg textures and guarded every custom player-model base UV face against transparent/misaligned skins.
- Prevented the Wild purification circle from healing unallied hostile mobs and allowed the Deep anchor spell to target the player-compatible test actor.
- Replaced the custom Knowledge Book AI with the skin-matched Shadow companion: explicit summon, doglike follow/teleport, consumed `shadow,` chat, owner-private replies, a collisionless hidden apparition, and a globally visible mortal revealed body.
- Reduced the Shadow Sword to exactly three original rites while retaining every routed innate/crystal action, persistent eight-slot favourites, fast wheel/search library access, corrupted presentation, and rank-10 zero cooldowns.
- Completed chromatic crystal cleanup: Orange produces skin-matched unarmed Echoes, Green keeps only Life Bloom, Blue owns global Chrono Stop plus controlled Dreamwalking, Yellow reaches 0.0625× through 10×, and Infected Rainbow is a hidden inert save alias rather than a second gameplay item.
- Added guaranteed First Vessel Miniportal loot, two persistent same-dimension charges, exact-stack anchor binding, and dropped-amethyst recharge.
- Assigned every imported relic and fantasy weapon a documented purpose and additive survival acquisition path without inventing deferred crystal recipes.
- Finalized the 64-action/2,080-pair collision catalogue and retained dedicated-server, GameTest, resource, client-smoke, and 10/50/100-player synthetic workload verification.

## 1.0.1 - 2026-08-10

- Closed realm-death escape: underqualified deaths in Light/Dark mindscapes now respawn inside the same realm.
- Revalidated every locked spell target for life, dimension, range, and line of sight at channel completion.
- Added independent per-player rate limits for every serverbound gameplay packet lane.
- Added radiant- and darkness-exclusive innate powers with compatible, duplicate-free allegiance migration; artifacts remain exempt.
- Corrected Darkness Creature revenge AI so darkness-tagged attackers can never provoke friendly fire.
- Reduced the Shadow Sword to exactly three unique rites—Call the Hollowed, Blight Ground, and Nightfall Dominion—while retaining corrupted innate/crystal routes and migrating retired selections safely.
- Preserved the darkness-level-10 Shadow Sword apotheosis: artifact casts ignore existing cooldowns and create no new cooldown.
- Made Celestial Ruin persist its countdown, dimension, caster, detonation phase, and exact destruction cursor through server restarts.
- Added explicit catastrophic terrain and block-entity policy for Celestial Ruin while always purging Darkness and Pure Light.
- Added real Fabric GameTests for live Darkness spreading and Darkness Creature faction targeting.
- Expanded the then-current canonical collision kernel to 82 actions and all 3,403 unordered pairs including same-action resonance; version 1.0.2 later removes retired actions and regenerates the final 64/2,080 catalogue.
- Kept the survival HUD energy well as ten separate vanilla-aligned symbols directly above the hunger bar.
- Added a non-pausing eight-favourite combat wheel, searchable invocation library, persistent artifact loadouts, dedicated rank-maze panels, and an enhanced five-state energy atlas.
- Hid every power-owned status-effect cloud and replaced potion-like power visuals with bounded, colour-authored dust.
- Strengthened Celestial Ruin to a large living shockwave with 50,000 peak damage, persistent warning beam, whiteout, tinnitus, crater, and distant fire scars; version 1.0.2 extends the final radius to 6,000 blocks.
- Added shared integrity forcefields that absorb the complete overkill impact which breaks them.
- Added schema-v2 migration so obsolete non-destructive terrain defaults do not silently survive upgrades; explicit v2 administrator opt-out remains supported.

## 1.0.0 - 2026-08-08

- Stabilized Minecraft 26.2 and Java 25 builds with automated unit and resource validation.
- Hardened every travel, targeting, damage, cooldown, persistence, and temporary-entity path for multiplayer servers.
- Added configurable safe zones, consent controls, terrain policy, particle budgets, and bounded time effects.
- Added vulnerable skin-matched bodies for realm travel, astral projection, dreamwalking, possession, and teleport marking.
- Added persistent 28-node light and darkness title mazes without mutually exclusive player classes.
- Added 20 original ritual spells across six functional grimoires, including counterspell and Dimensional Anchor.
- Activated every crystal, including multi-mode chromatic, Rainbow, and Infected Rainbow convergences with swap-proof cooldowns.
- Built Light and Dark Realm memory sites, lore rewards, ambient magic, and a distinct Middleworld biome.
- Rebuilt the HUD as authored rune medallions and a five-state ancient energy reliquary, with responsive teleport and celestial-locator rituals.
- Added the interactive Labyrinth of Names screen, synchronized title perks, server-validated awaken/attune actions, and rank ceremonies.
- Added spreading Darkness and Pure Light, darkness-tag affinity restoration, hostile Wither auras, and a staged power-100-equivalent mutual-annihilation clash.
- Expanded the canonical magic kernel to 65 actions and all 2,145 possible collisions by adding both living realm forces.
- Added signature-driven four-beat ceremonies to every successful innate, crystal, and grimoire cast while preserving bespoke impact effects.
- Gave all 18 magical aspects deliberate geometry families, physically scaled anticipation/impact radii, eight original particle sprites, and 13 original normalized sounds with reduced-motion clamps.
- Spatially staged cast rituals at the caster's feet and billboarded vertical sigils per observer without adding packets or particles.
- Made cast presence visibly intensify at rank depths 4 and 8 and through the Ancient Mastery title while retaining hard FX bounds.
- Corrected Elemental Blast so every phase uses its real canonical collision, residue, sound, and visual identity and malformed saved phases normalize safely.
- Added an authoritative Elemental Blast HUD cycle with localized phase labels, phase-coloured slot accents, and a pulsing primed rune.
- Made living-force annihilation advance in bounded radial shells so terrain removal matches the expanding eclipse corona from epicentre to boundary.
- Activated True Sight and Dark Resurgence as consent-safe, amethyst-countered rank mechanics with distinct third-eye and eclipse-awakening ceremonies.
- Corrected Telekinesis to fling outward, refund empty releases, and defer nonblocking collision mechanics until gameplay successfully commits.
- Rebuilt Speed Burst as a synchronized collision-safe kinetic dash with afterimage wakes, consent-aware shockwaves, and a genuine paid Motion-rank Second Step shown directly on the HUD.
- Reforged Void Beam into a telegraphed penetrating abyssal ray with rank bores, seven semantic counters, impact-position magic residue, and bounded non-griefing void scars.
- Rebuilt Gravity Displacement as a persistent ancient orrery with collision-safe body orbits, deterministic multi-field resonance, ward/amethyst/time counterplay, ranked collapse impacts, mastered projectile curvature, and complete lifecycle release.
- Recast Energy Beam as a four-beat live-aim Sunfire channel with escalating scorch, water-to-steam transformation, ten semantic terminals, protected ranked flares, mastered non-chaining forks, and interruption-safe lifecycle ownership.
- Rebuilt Breezy Bash as an owned two-stage Tempest Rite with bounded spherical capture, collision-safe launch, per-body slam revalidation, multi-gust arbitration, eight visual counters, safe interruption release, empowered pressure, and mastered projectile curvature.
- Rebuilt Super Speed as finite Chronal Overdrive with an isolated rank-scaled movement modifier, hydroplane grounding, measured wakes, collision rites, consent-aware pressure, hostile memory slips, mastered projectile curvature, and exact lifecycle cleanup.
- Rebuilt Fireball as one chargeable server-owned Cinderheart per caster with paid tiers, bounded lifetimes, finite attributed reflections, semantic ward/amethyst/frost/water terminals, controlled splash and pressure, optional capped surface fire, and no vanilla explosion grief.
- Rebuilt Starfall as a telegraphed server-owned Astral Convergence with deterministic celestial strikes, roof and water grounding, realm-matter reactions, repeat-safe damage, seven distinct rank paths, bounded projectile diversion, and an ancient crown ceremony.
- Rebuilt Ground Slam as the finite Faultbound Verdict with a visible fault clock, transformed water/Darkness/Pure Light impacts, three authored rank beats, protection-first pressure, caster mantles, bounded optional soft-terrain fracture, and exact lifecycle cleanup.
- Audited every production Java source and all 149 non-item assets; removed three stale, unreferenced HUD strips.
- Replaced vanilla loot-table overrides with additive loot injection and removed unreleased crystal recipes.
- Optimized hot-path scans, state syncs, scheduled tasks, particles, freeze ownership, storms, and body proxies.
