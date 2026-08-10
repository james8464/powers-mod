# Final Expansion: Master Design

**Date:** 2026-08-10  
**Target:** Fabric, Minecraft Java Edition 26.2, Java 25  
**Status:** Approved; the user delegated all remaining implementation decisions

## Outcome

Finish POWERS as a multiplayer-safe, high-power magic mod rather than a collection of loosely connected abilities. This expansion closes every remaining playtest and audit gap, adds opposed indestructible artifacts, a compatible weapon-conversion forge, an owner-private darkness companion, and a tactical player-shaped boss, while preserving the deliberately recipe-less crystals and story artifacts.

The detailed contracts live in the companion specifications dated 2026-08-10. This document is the acceptance map and source of precedence when an older specification conflicts with the final expansion.

## Non-Negotiable Product Rules

- Every client packet is a request. The server resolves identity, item, target, rank, cost, protection, range, line of sight, and outcome.
- Player powers scale with normal or darkness rank; crystal powers and grimoire spells do not.
- Max-rank darkness removes Shadow Sword ability cooldowns. Costs, target validation, safe-zone rules, anti-spam budgets, and one-instance caps still apply.
- The Heavenly Partisan is the light-aligned peer of the Shadow Sword and receives the same framework quality and power depth.
- The Shadow Sword and Heavenly Partisan never lose durability, cannot be consumed by the weapon forge, and cannot be produced by it.
- Crystals and deliberately story-gated custom items remain without recipes.
- Lightning remains visually restrained and has no player-power cooldown. Time freeze, celestial rituals, artifact ultimates, and boss phases receive the most significant presentation.
- Potion effects caused by POWERS hide vanilla effect particles and HUD icons unless an effect explicitly requires an icon for counterplay.
- Mindscape, possession, astral, and teleport-selection bodies remain vulnerable and exactly resemble the departing player.
- Effects are impressive without becoming a packet or particle denial-of-service vector.
- All destructive actions honour server policy, world borders, protected regions, block-entity protection, and configuration.

## Canonical Architecture

### Significance-driven presentation

Every registered action declares one of five presentation levels:

| Level | Contract | Examples |
|---|---|---|
| `NONE` | No generic ceremony; bespoke gameplay feedback only | block spread, amethyst ticks |
| `MINIMAL` | One short local cue, no screen-filling foreground particles | lightning, fireball |
| `STANDARD` | Anticipation and release | most combat powers |
| `RITUAL` | Four readable beats and lingering seal | grimoire spells |
| `COSMIC` | Six-beat world event, dimension-aware sound/sky cue | time freeze, celestial ruin, artifact ultimates, boss phases |

The canonical action catalogue stores this value. Generic choreography is omitted when bespoke effects already satisfy it. Generated documentation lists it, and catalogue validation rejects missing values.

### Shared opposed-artifact framework

`ArtifactAlignment` (`DARKNESS`, `LIGHT`) drives authorization, palette, guardian faction, passive aura, menu theme, and common action routing. The two mythic weapons share infrastructure while retaining separate named unique actions. Artifact state is server-authoritative and never inferred from a client-selected menu index.

### Bounded server systems

All delayed tasks, storms, FX, boss plans, companion messages, projections, and chunk tickets have hard capacity, per-tick work, lifetime, and cleanup limits. Overflow refuses safely and records a rate-limited diagnostic rather than growing memory or blocking the server thread.

## Delivery Slices

1. **Runtime integrity:** scheduler bounds, spatial aura lookup, async travel completion, consolidated player tick, packet/particle budgets, and significance-driven FX.
2. **Frozen-body fidelity:** full appearance snapshot and exact player skin/model/pose render, with existing vulnerability and return guarantees.
3. **Opposed artifacts:** common runtime, true indestructibility, inventory auras, complete dark/light action rosters, guardian rules, menus, and documentation.
4. **Arcane Crucible:** two-input forge, server-authenticated choice workflow, alignment conversion, star lightning binding, rune XP, compatibility tags, models, textures, and recipes/loot only for ordinary components.
5. **Private companion:** owner-only network visibility and interaction, dark follower behaviour, deterministic lore dialogue, optional bounded AI provider.
6. **Omnipower boss:** real player-shaped entity with tactical adapters for every player power, unique phases, dialogue, spawn controls, and multiplayer scaling.
7. **Completion audit:** regenerate every source/resource/interaction manifest, cover every current and historical user requirement, update README, run all unit/GameTests/client/server/resource/performance checks, and commit a clean tree.

## Multiplayer Performance Budgets

Default limits are configuration-backed but never configurable to an unsafe unbounded state:

- scheduled tasks: 8,192 total, 256 executions per server tick, exceptions isolated per task;
- active lightning storms: 32 server-wide and one per caster/action token;
- semantic FX events: 256 per server tick, culled beyond 128 blocks unless cosmic;
- server particles: 512 actual recipient-particles per server tick and 128 per viewer;
- companion AI provider: one in-flight request per owner, four globally, 30-second owner rate limit, 2.5-second timeout, 256-character response;
- artifact summons: four normal or two elite guardians per owner;
- boss planner: at most 24 scored candidates per decision and one committed action per cadence;
- asynchronous travel tickets: one pending destination per player, deadline-bounded and always released;
- block conversion and singularity work: queued, capped per tick, never a single unbounded radius traversal.

## Completion Evidence

Completion requires all of the following, not a claim based only on compilation:

- focused red/green tests for every new pure rule;
- GameTests for Crucible transactions, artifact authorization/guardians, body snapshots, companion routing, boss planning, and chunk-ticket cleanup;
- generated action and interaction documents with no missing pair;
- source, JSON, texture, model, translation, recipe, loot, tag, and advancement validators;
- dedicated-server boot and clean shutdown with all dimensions and registries loaded;
- client boot/visual smoke for HUD, screens, player-shaped renderers, and effect packets;
- simulated 20- and 50-player tick workloads within the established regression thresholds;
- explicit manual-test checklist for behaviours automation cannot visually prove;
- README coverage for every power, spell, artifact action, forge path, realm rule, rank effect, keybind, countermeasure, configuration, command, mob, and limitation;
- `git status --short` empty after intentional commits.

## Out of Scope

- Copying proprietary assets, names, dialogue, or exact ability implementations from other mods or television series.
- Letting an external AI service control gameplay outcomes or execute commands.
- Making the optional AI provider mandatory for the companion or boss.
- Adding recipes for crystals or story artifacts whose absence is intentional.

