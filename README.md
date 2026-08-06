# POWERS

A **Fabric mod for Minecraft** that turns every player into a superhero.

POWERS is a "Rainbow Quest"-style powers framework: on your first join you are
assigned three random powers, and from then on you live with what the rainbow
gave you. Each power has its own **active ability** and often a **passive
bonus**, everything is balanced by a shared **energy reserve**, and a tier of
craftable **crystal powers** sits above the regular ones for when the fight
really matters.

The mod is inspired by the Rainbow Quest universe (FavreMySabre) and classic
superpower mods such as Superheroes Unlimited and PMT Powers.

> **Status: IN DEVELOPMENT (v0.1.0)**
> This is an early, work-in-progress version. Mechanics, costs and balance are
> subject to change, and several features are still unfinished — see the
> [Development Status](#development-status) section below.

---

## Table of Contents

- [Features](#features)
- [How Powers Work](#how-powers-work)
- [The Powers](#the-powers)
- [Crystal Powers](#crystal-powers)
- [Energy System](#energy-system)
- [Controls](#controls)
- [The Counter-Play: Amethyst](#the-counter-play-amethyst)
- [Dimensions](#dimensions)
- [Items, Weapons and Blocks](#items-weapons-and-blocks)
- [Commands](#commands)
- [Project Structure](#project-structure)
- [Building and Running](#building-and-running)
- [Requirements](#requirements)
- [Development Status](#development-status)
- [License](#license)

---

## Features

- **Three power slots per player.** On first login every player is randomly
  assigned 3 distinct powers from the registry. The assignment is saved on the
  player permanently and survives restarts, logins and even respawns.
- **26 regular powers**, each with a unique active ability and an optional
  passive effect (Haste, Speed, Fire Resistance, Night Vision, ...).
- **Shared energy reserve.** Every power costs energy to use. Energy
  regenerates slowly over time and is fully restored when you wake from sleep.
- **Toggle powers.** Flight, Invisibility, Time Freeze and more are toggles
  with an ongoing per-second energy drain while active.
- **Crystal tier.** 10 craftable crystals grant game-changing abilities
  (Inferno, Chrono Stop, Clone Swarm, Soul Link ...). Crystal powers are never
  handed out randomly — you must obtain the crystal itself.
- **Rainbow Crystal.** A craftable item that opens a power-selection screen to
  re-roll your three powers. Never consumed.
- **HUD.** A power HUD showing your three slot powers (with a toggle "ON/OFF"
  indicator and a blink marker when a slot is fired) plus an energy HUD bar.
- **Keybinds.** One key per slot plus a power-menu key.
- **Amethyst counter-play.** Amethyst is the one substance that cancels
  powers — both as a held item and as a redstone-powered Amethyst Ward block.
- **Custom dimensions.** The Light Realm, Dark Realm and Middle World are
  flat, rule-restricted pocket dimensions with their own biomes.
- **85 registered weapons.** Swords, axes, picks and shovels in themed sets
  (Amethyst, Azure, Black Iron, Demon's Blood, Gloomsteel, ...).
- **Visual flair.** Colored particle auras around players (one hue per power,
  rainbow for Flight), lightning storms, and per-power particle effects.

---

## How Powers Work

1. **On your first join**, the game deals you 3 random, distinct powers into
   slots 1–3. They are yours permanently — this is a "you get what you get"
   system.
2. **Press `V`, `X` or `C`** to use the power in that slot (left-click for
   aiming powers, or just press the key).
3. **Press `P`** to open the Power Menu, or use the **Rainbow Crystal** to
   re-roll your powers from the full list — you pick exactly 3.
4. **Energy matters.** Every ability costs energy. Watch the energy bar; if
   you run dry, abilities refuse to fire until you regenerate (or sleep).
5. **Passives are automatic.** Each power's passive effect (e.g. Haste from
   Slow World) is re-applied on a schedule so it never expires while you hold
   the power.

Your powers persist as synced data attachments on the player entity, so the
assignment is identical on server and client and survives restarts.

---

## The Powers

All powers listed below are drawn from `PowerRegistry`. Each entry shows its
active ability and the passive effect that comes with it.

| Power | Active Ability | Passive Effect |
| --- | --- | --- |
| **Slow World** | Bend time around you — the world crawls while you move freely | Haste |
| **Time Shift** | Teleport anywhere, any dimension (with a marking beacon) | Haste |
| **Shadow Step** | Blink through the dark to the block you're looking at | Night Vision |
| **Flight** | Soar freely through the sky — toggle, drains energy while active | Speed |
| **Elemental Blast** | Cycle fire, frost, storm and earth energy at your target | Fire Resistance |
| **Starfall** | Rain lightning down on your target from the heavens | Health Boost |
| **Void Beam** | Wither everything on your sight line | Absorption |
| **Fireball** | Summon a fireball that hovers before you — punch it to launch it | Fire Resistance |
| **Frost Nova** | Freeze nearby foes solid | Water Breathing |
| **Lightning Strike** | Smite your target from the sky | Resistance |
| **Ground Slam** | Crush everything nearby with a shockwave | Strength |
| **Speed Burst** | Launch yourself forward at super speed | Jump Boost II |
| **Telekinesis** | Drag everything nearby toward you | Slow Falling |
| **Energy Beam** | Fire a devastating beam of energy | Strength |
| **Super Speed** | Move at impossible speed | Speed II |
| **Breezy Bash** | A gust of wind that sends your target flying | Slow Falling |
| **Cozy Campfire** | Regenerate and warm up at a conjured campfire | Regeneration |
| **Invisibility** | Turn invisible — toggle, drains energy while active | — |
| **Time Freeze** | Stop time around you while you move freely — toggle | Speed |
| **Forcefield** | A barrier that blocks damage while active | Resistance |
| **Gravity Displacement** | Rearrange gravity to your advantage | Slow Falling |
| **Vessel Possession** | Possess and control a living vessel | Night Vision |
| **Astral Projection** | Project your spirit out of your body | Night Vision |
| **Energy Drain** | Leech the energy of those around you | — |
| **Dimensional Anchor** | Anchor yourself to a point in space | Strength |
| **Ice Manipulation** | Shape and weaponize ice | Water Breathing |

---

## Crystal Powers

Crystal powers are a **tier above** the regular powers. They are never drawn
by the Rainbow Crystal and never assigned randomly — the only way to hold one
is to craft and carry the crystal itself. Most are on a shared cooldown while
they recharge.

| Crystal | Ability | Effect |
| --- | --- | --- |
| **Red Crystal** | Inferno | The world around you becomes a firestorm |
| **Orange Crystal** | Clone Swarm | Conjure three loyal clones from the air that fight alongside you |
| **Yellow Crystal** | Size Shift | Shrink past every blade, or tower over enemies and crush them |
| **Green Crystal** | Life Bloom | Every ally within twenty blocks is fully healed and renewed |
| **Blue Crystal** | Chrono Stop | Time itself freezes while you move freely |
| **Indigo Crystal** | Portal Rift | Blink from enemy to enemy in a chain of crushing strikes |
| **Violet Crystal** | Soul Link | Bind the souls of your foes — wound one, wound them all |
| **Light Crystal** | *Not yet revealed* | Its purpose is not yet revealed |
| **Dark Crystal** | *Not yet revealed* | Its purpose is not yet revealed |
| **Reverse Rainbow Crystal** | Middle World | Its purpose is not yet revealed (dimension access is wired up) |
| **Infected Rainbow Crystal** | — | Intentionally inert for now |

---

## Energy System

- Every player has a shared energy reserve with a **maximum of 1000**.
- Abilities cost energy **per use** (from 4 for Lightning Strike up to 45 for
  Starfall, Slow World, Time Freeze and Dimensional Anchor).
- Toggle abilities drain energy **continuously** while active (e.g. Flight
  and Invisibility: 1/s, Time Freeze: 3/s). If energy runs out mid-toggle, the
  ability turns itself off.
- Energy **regenerates** 1 point per second automatically, and a full night's
  sleep restores the entire reserve.
- If an activation fails partway, the energy is refunded.

---

## Controls

| Key | Action |
| --- | --- |
| `V` | Use power in slot 1 |
| `X` | Use power in slot 2 |
| `C` | Use power in slot 3 |
| `P` | Open the Power Menu |
| Right-click | Use the Rainbow Crystal to open the power-selection screen |

All keys are configurable in the vanilla Controls menu under the **POWERS**
category.

---

## The Counter-Play: Amethyst

Amethyst is the bane of every power user:

- **Holding any amethyst item** (or wearing amethyst armor) suppresses your
  powers — crystal activations are refused and you are afflicted with
  **Amethyst Poisoning** while carrying it.
- **Amethyst blocks within 6 blocks** of you do the same.
- The **Amethyst Ward** block, when powered by redstone, creates a
  20-block-radius suppression field around itself. Crystal powers simply
  refuse to fire inside it.
- Players suffering Amethyst Poisoning are also **immune to player damage** —
  a defenseless-but-safe trade-off.

---

## Dimensions

Three custom flat dimensions are registered (`data/powers/dimension/`):

| Dimension | Description |
| --- | --- |
| **Light Realm** (`powers:light_realm`) | A flat realm of Pure Light blocks, full-bright, white sky |
| **Dark Realm** (`powers:dark_realm`) | A flat realm of Darkness blocks, hostile and lightless |
| **Middle World** (`powers:middleworld`) | A flat realm of Pure Light blocks — the heart of the crystal story |

Players in any of these realms are forced into **Adventure mode** (restored on
leave), take **no damage** from any source, and cannot sleep or set spawn
there. The realms are accessed through crystal powers, the Time Shift power,
or the `/powers travel` command.

---

## Items, Weapons and Blocks

**Items**
- `rainbow_crystal` — re-rolls your powers, never consumed
- `red_crystal` … `violet_crystal` — the seven color crystals
- `light_crystal`, `dark_crystal`, `reverse_rainbow_crystal`,
  `infected_rainbow_crystal`

**Blocks**
- `darkness` / `pure_light` — the realm filler blocks
- `amethyst_ward` — redstone-powered anti-power field emitter

**Weapons** (85 total, all in the creative tab)
Themed sets including **Amethyst**, **Azure**, **Black Iron**, **Crimson**,
**Crystal Frost**, **Demon's Blood**, **Gloomsteel**, **Golden Phoenix** and
more — greatswords, greataxes, claymores, katanas, scythes, daggers, picks and
shovels. They are registered with balanced damage/speed stats, but **no
recipes yet**.

---

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/powers list` | List every registered power | anyone |
| `/powers slots [player]` | Show a player's current slot assignment | op for others |
| `/powers assign <player> <power> <slot>` | Assign a specific power to a slot | op |
| `/powers reroll [player]` | Roll a fresh set of 3 random powers | op for others |
| `/powers travel <dimension>` | Teleport to a registered dimension | op |

The Rainbow Crystal is the friendly player-facing path; the command is there
for server admins and testing.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/powers/
│   │   ├── PowersMod.java            # Mod entrypoint, server ticks, realms, storms
│   │   ├── command/PowerCommand.java # /powers command tree
│   │   ├── network/PowersPackets.java# Client/server sync (power state)
│   │   ├── player/PlayerPowers.java  # Persistent per-player slot attachments
│   │   ├── power/
│   │   │   ├── PowerRegistry.java    # The 26 regular powers
│   │   │   ├── Power.java / Ability.java / ToggleAbility.java / PassiveEffect.java
│   │   │   ├── PowerEnergy.java      # Cost tables
│   │   │   ├── AmethystDampening.java
│   │   │   ├── abilities/            # 26 ability implementations
│   │   │   └── crystals/             # Crystal powers + registry
│   │   ├── item/  # CrystalItem, RainbowCrystalItem
│   │   ├── fx/    # Particle helpers
│   │   └── ...    # Blocks, weapons, items, effects, creative tab
│   └── resources/
│       ├── fabric.mod.json
│       ├── assets/powers/            # lang, textures
│       └── data/powers/              # dimensions, biomes, dimension types, recipes
└── client/
    └── java/com/powers/client/
        ├── PowersClient.java         # Keybinds, HUD registration, packets
        ├── screen/                   # PowerSelectionScreen, TeleportInputScreen
        └── ...                       # HUD renderers, client power state
```

---

## Building and Running

```bash
# Build the mod jar (outputs to build/libs/)
./gradlew build

# Launch a dev client
./test.sh client        # or: ./gradlew runClient

# Launch a dev server
./test.sh server        # or: ./gradlew runServer

# Validate registered power resources
./gradlew validatePowerResources
```

Requires **Java 25** (see `test.sh`; set `JAVA_HOME` if you use another JDK).

Built jars are also dropped into `dist/` after a build.

---

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | `26.2` |
| Fabric Loader | `>= 0.19.3` |
| Fabric API | `>= 0.156.0+26.2` |
| Java | `>= 25` |
| Gradle (wrapper) | bundled, via Loom `1.17-SNAPSHOT` |

---

## Development Status

**POWERS is in active, early development (v0.1.0).** It is not yet a polished
release — expect rough edges and change.

What still needs work / is planned:

- **Infected Rainbow Crystal** is intentionally inert; its power is not
  implemented yet.
- **Light Crystal, Dark Crystal and Reverse Rainbow Crystal** exist with items
  and (for the Reverse Rainbow) the Middle World dimension, but their
  purposes are "not yet revealed" — the abilities are placeholders waiting for
  their story content.
- **Weapons have no recipes** yet — they are obtainable only via the creative
  tab or commands.
- A **Feral Roar** power appears in the language files but is not yet
  registered in `PowerRegistry`.
- Balance is untested at scale: energy costs, passive amplifier levels and
  ability cooldowns are likely to change.
- More powers, realm content and story progression are planned.

Feedback, issue reports and contributions are welcome.

---

## License

[MIT](LICENSE)
