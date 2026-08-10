# Playtest completion verification — 2026-08-10

Target: Minecraft Java Edition 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25

This record closes the issues raised during the 2026-08-09/10 playtest. It supplements the earlier 1.0.0 and magic-quality verification records.

## Requirement evidence

| Area | Implemented contract | Verification |
|---|---|---|
| Minimal presentation | Power results use action-bar feedback for routine state; status effects created by POWERS hide world particles; lightning/fireball use bounded close-range budgets; global Time Freeze has a server-wide ceremony. | `PowerMessagesTest`, `PowerStatusEffectsTest`, `ParticleBudgetTest`, `TimeStopFxTest`, resource/client smoke |
| Energy HUD | Exactly ten independent 9-pixel full/half symbols occupy the vanilla hunger-row footprint, directly one row above it. Normal, empty, amethyst-dampened, darkness, and projection rows are authored; the power rail cannot overlap the hotbar. | `HudMathTest`, `HudLayoutTest`, non-item asset audit, successful GUI-atlas reload |
| Power behaviour | Lightning has no cooldown; Size Morphing is a selectable player power with distance-scaled upkeep; Time Slow was removed; Time Freeze uses true server tick freezing; Elemental Blast retains an explicitly selected element. | Ability/rules tests and generated action catalogue |
| Innate allegiance and ranks | Random loadouts guarantee a compatible Radiant/Darkness exclusive; allegiance changes preserve compatible slots and migrate forbidden ones without duplicates. Every innate power uses numeric rank scaling and maze perks; equipment powers do not. | `PowerRegistryTest`, `PowerScalingServiceTest`, rank-maze tests |
| Targeting and testability | Applicable hostile/support powers accept living mobs as well as players. Named remote viewing supports uniquely named mobs. Player-like test mobs and Darkness Creatures provide live targets. | Target rules tests plus live Darkness Creature GameTest |
| Grimoire reliability | Channels lock targets and revalidate life, dimension, range, line of sight, movement, damage, held book, amethyst, and protection at completion. Dimensional Anchor is a spell. Celestial Ruin survives chunk unload and server restart, retains its area ticket, and exposes explicit terrain/block-entity policy. | `SpellTargetRulesTest`, spell tests, `CelestialRuinSavedDataTest`, cursor/config tests |
| Realm travel | Light/Dark crystals fall back to the wielder when no valid aimed target exists. Travel loads destination chunks asynchronously. Realm departure rules are revalidated, including after death; underqualified players remain confined. The Light Realm uses a white sky renderer. | travel/realm tests and dedicated-server dimension boot/save smoke |
| Teleportation | Destination chunks are loaded before safe-position validation; server-advertised modded dimensions are shown without truncating their identifiers. | travel chunk-loader, destination-policy, and menu layout tests |
| Living realm matter | Darkness and Pure Light spread through random ticks with bounded conversion rules. Darkness harms non-faction entities and restores tagged players. Opposed matter triggers bounded catastrophic annihilation. | rules tests plus live random-tick spread GameTest |
| Progression UI | Only the allegiance-appropriate advancement path is visible; both canvases have backgrounds; title prefixes and both 28-node mazes are data-driven. | advancement, rank-maze, resource, and language validation |
| Shadow Sword | Non-dark carriers are cursed and guarded against. Tagged wielders route every innate/crystal action through a darkened echo of its original palette and use seven original plus eleven new artifact rites. Darkness level 10 ignores existing cooldowns, starts no new cooldown, and receives the 900-energy apotheosis pulse. | `ShadowSwordPowerRulesTest`, `ShadowSwordPacketsTest`, palette/catalogue/selection tests, exhaustive magic tests |
| Heavenly Partisan | The opposed unbreakable artifact routes every innate/crystal action through light presentation and adds eleven group-protection/judgement rites, sentinels, fields, gates, covenant links, and death wards. | artifact selection/menu/rules tests, exhaustive magic tests |
| Crucible, companion, and boss | The Arcane Crucible provides an atomic three-stage compatibility forge; the Shadow Sword unlocks an owner-private bounded companion; the First Vessel supplies a 5,000-vitality, multiplayer-scaled 28-power endgame encounter and ritual. | crucible transaction/resource tests, companion privacy/provider tests, tactical planner/rules tests |
| Security/performance | Serverbound packet lanes are independently rate-limited and string codecs are length-bounded. Entity, particle, projectile, spread, explosion, summon, field, target scan, dialogue, and ritual work is capped, asynchronous, or tick-budgeted. Target/action identifiers are server validated. | packet bound/limiter tests, source audit, bounded-work tests |
| Interactions | 97 magic actions produce every unordered same/cross-action combination, each with deterministic mechanics, visual cue, sound cue, and accessibility shape. | 4,753 generated rows; exhaustive resolver and documentation drift tests |
| Recipes | Crystal and reserved custom-item recipes remain intentionally absent. Natural and upgrade-chain runestone recipes/loot are present. | strict resource validator and README catalogue |

## Automated gate

`./gradlew clean check build` completed successfully on 2026-08-10. It ran 399 JUnit tests and 12 live Fabric GameTests (including actual Darkness random-tick spread and runtime Darkness Creature faction targeting), compilation for common/client/GameTest sources, strict resource validation, Java/non-item-asset audits, generated interaction-document drift checks, and release assembly.

The final clean release gate is:

```text
./gradlew clean check build
```

## Runtime smoke

- Dedicated server: reached `Done`, initialized 28 powers, 97 actions and 4,753 interactions, then loaded and saved the Overworld, Nether, End, Light Realm, Dark Realm, and Middleworld before accepting `stop` and exiting 0 without a POWERS error.
- Development client: initialized OpenGL/OpenAL, loaded POWERS, reloaded all resources, and created the particle, item, block, GUI, celestial, and other atlases without a missing POWERS asset. The smoke initially exposed Arcane Crucible textures outside the stitched block atlas; the textures were moved into `textures/block`, a validator regression rule was added, and the clean rerun had no missing-texture warning. The client was then deliberately stopped with SIGINT. Development-profile Mojang/Realms authentication and Apple/Metal shader diagnostics were external/vanilla messages.

## Remaining experiential work

No automated suite can establish subjective multiplayer balance or whether every ceremony feels ideal to every player. The implementation and deterministic contracts are release-gated; normal playtesting on the intended modpack/server remains the correct place to tune audiovisual density and boss balance without weakening the safety limits.
