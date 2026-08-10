# Changelog

## 1.0.2 - 2026-08-11

- Removed the five retired innate powers (Cozy Campfire, Frost Nova, Elemental Blast, Ground Slam, and Shadow Step), their runtime managers, selectors, protocol state, boss/artifact adapters, translations, and collision entries.
- Removed every automatically assigned innate passive and the unconditional player aura dust emitter; saved loadouts now migrate deterministically to valid allegiance-safe powers.
- Reduced the canonical innate roster to 23 and regenerated its exhaustive interaction documentation and audit manifests.
- Made every indefinite innate toggle continuously consume energy; Time Freeze now drains at least 15% of the caster's capacity each second.
- Replaced raw invisibility flags with a particle-free, icon-free, amplifier-255 effect owned and safely removed by POWERS.
- Added the zero-cooldown crouching Plant Healing pulse for injured players inside an inclusive two-block radius while retaining ordinary aimed plant growth.

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
- Expanded the canonical collision kernel to 82 actions and all 3,403 unordered pairs including same-action resonance.
- Kept the survival HUD energy well as ten separate vanilla-aligned symbols directly above the hunger bar.
- Added a non-pausing eight-favourite combat wheel, searchable invocation library, persistent artifact loadouts, dedicated rank-maze panels, and an enhanced five-state energy atlas.
- Hid every power-owned status-effect cloud and replaced potion-like power visuals with bounded, colour-authored dust.
- Strengthened Celestial Ruin to a 2,048-block living shockwave with 50,000 peak damage, persistent warning beam, whiteout, tinnitus, crater, and distant fire scars.
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
- Built Light and Dark Realm memory sites, lore rewards, custom obelisks, ambient magic, and a distinct Middleworld biome.
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
