# POWERS

A Rainbow-Quest / Bliss-SMP-inspired Fabric mod for Minecraft 26.2. Every player starts with three randomly assigned superpowers drawn from a pool of 28, charges them with a shared energy pool, and grows from a humble "Unawakened" into a world-shattering "Origin" — or abandons the light and walks the hidden 30-stage path of **The Darkness**.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Power Assignment](#power-assignment)
3. [The Energy System](#the-energy-system)
4. [The Skill System (Light Path)](#the-skill-system-light-path)
5. [The Darkness System](#the-darkness-system)
6. [Amethyst Dampening](#amethyst-dampening)
7. [The 28 Powers](#the-28-powers)
8. [Crystal Powers](#crystal-powers)
9. [Crystal Items](#crystal-items)
10. [Runestones & Grimoires](#runestones--grimoires)
11. [Weapons (84)](#weapons-84)
12. [Imported Items (164)](#imported-items-164)
13. [Blocks](#blocks)
14. [Mob Effects](#mob-effects)
15. [Dimensions](#dimensions)
16. [Advancements](#advancements)
17. [Recipes](#recipes)
18. [Commands](#commands)
19. [Client Features](#client-features)
20. [Notes & Caveats](#notes--caveats)

---

## Quick Start

- **Join a server**: three powers are assigned to you automatically on first login. They are permanent.
- **Controls**:

| Key | Action |
|---|---|
| `V` | Activate power in slot 1 |
| `X` | Activate power in slot 2 |
| `C` | Activate power in slot 3 |
| `P` | Reserved (power menu — currently disabled) |

- **Creative tab**: everything is in the dedicated **POWERS** creative tab; weapons also appear in the vanilla **Combat** tab; crystals, blocks and items also appear in the vanilla **Ingredients** tab.

---

## Power Assignment

- Every player has **3 power slots**, assigned randomly and distinct on first login. They persist across logins and server restarts.
- Powers are permanent — **the Rainbow Crystal re-roll is currently disabled**, so there is no in-game way to change your powers yet.
- Admins can force assignment with `/powers assign <player> <power> <slot>` or `/powers reroll [player]`.
- Each regular power grants a **permanent passive effect** while assigned (e.g. Haste, Night Vision, Speed, Fire Resistance, Absorption, Strength, Regeneration…) — see the power list below.
- Crystal powers are a tier above regular powers: **never** assigned randomly and never rolled by the Rainbow Crystal — the only way to hold one is to craft and use the crystal item itself.

---

## The Energy System

All abilities draw from a single shared **energy pool** (shown as a 10-segment bar above the hunger row).

| Rule | Value |
|---|---|
| Base capacity (light path) | **250** |
| Capacity per skill level | **+25 per level** (max **775** at level 21) |
| Base capacity (darkness path) | **500** |
| Darkness capacity per level | **+45 per level** (max **1,850** at level 30) |
| Regen (light path) | **1 per second** |
| Regen (darkness path) | **2 per second** (4 per second in the Dark Realm or at night) |
| Sleep | Fully refills the pool when you stop sleeping |
| Runes | Right-clicking a runestone channels **+100 energy** |

**Key rules:**

- **Costs never scale with level.** Higher skill/darkness levels never make powers cheaper — they only enlarge the pool, so it is harder to fully run out.
- Toggle abilities (Flight, Invisibility, Time Freeze) pay their activation cost up front, then drain energy **once per second** while active (1–3 per second). If the pool runs dry the toggle force-disables.
- **Energy backlash:** letting a draining toggle burn out on an empty pool is punished — the power is torn off but you take **70% of your max health in magic damage**, a lightning storm (like the teleport power) crashes down on you, and particle explosions/rings erupt around you.
- Failed activations **refund** their energy cost.
- The **Exhaustion** effect locks your energy at 0 — no regen, no sleep refill.

### Activation costs

| Cost | Abilities |
|---|---|
| 4 | Lightning Strike, Fireball |
| 10 | Speed Burst, Shadow Step, Super Speed, Invisibility |
| 18 | Time Shift |
| 20 | default (crystal powers) |
| 22 | Energy Beam, Void Beam, Frost Nova, Ice Manipulation |
| 24 | Telekinesis, Vessel Possession, Plant Healing, Double Health |
| 28 | Elemental Blast, Gravity Displacement, Breezy Bash |
| 30 | Energy Drain |
| 32 | Astral Projection |
| 35 | Ground Slam, Forcefield, Cozy Campfire |
| 45 | Starfall, Slow World, Time Freeze, Dimensional Anchor |

### Ongoing toggle drain (per second)

| Toggle | Drain |
|---|---|
| Flight | 1 |
| Invisibility | 1 |
| Time Freeze | 3 |

---

## The Skill System (Light Path)

Progression on the **light path** is driven entirely by the **21-route advancement chain** (`powers:skill/level_01` → … → `level_21`). Complete each route's challenge to advance one level; your level updates automatically.

**Rank titles (21):** Unawakened → Spark → Awakened → Channeler → Adept → Weaver → Arcanist → Vanguard → Luminary → Riftwalker → Starforged → Soulbound → Chronarch → Astral → Voidcaller → Paragon → Ascendant → Transcendent → Mythic → Apex → **Origin**.

**What levels do:**

| Effect | Formula |
|---|---|
| Ability damage | `base × (1.0 + level × 0.025)` (+2.5%/level, **+52.5% at 21**) |
| Ability range | `base × (1.0 + level × 0.01)` (+1%/level, **+21% at 21**) |
| Energy capacity | `250 + level × 25` |

**Presentation:** your chat messages are broadcast as `[Rank] Name: message` in the rank's color, and the rank prefix also appears above your head as a custom name.

---

## The Darkness System

The hidden second progression track. **By default it is completely invisible** — the advancement tab and its quests only exist for players carrying the entity tag `darkness` (`/tag <player> add darkness`). Tagged players immediately receive the hidden "Darkness Initiation" root advancement, which reveals the Darkness tab.

**What changes for darkness-tagged players:**

- Their energy pool switches to the **darkness pool** (500 base, +45/level — much harder to drain) and regenerates 2/sec (4/sec in the Dark Realm or at night).
- Their **effective level** becomes their darkness level instead of their skill level — this drives all damage/range scaling.
- Their chat/name prefix uses the darkness rank instead of the skill rank.
- They can traverse the Dark Realm freely (see traversal rules below).

**Rank titles (30):** Acolyte of Gloom, Shade Initiate, Nightbound, Shadowpriest, Duskwarden, Umbra Cultist, Voidborn, Abyssal Adept, Witch of Wraiths, Sable Seeker, Obsidian Oracle, Coven Herald, Malediction Master, Onyx Savant, Ravenous Shade, Nightmare Binder, Sinister Paragon, Midnight Marshall, Soulblight, Ebon Sovereign, Gravecaller, Umbral Tyrant, Dread Reaver, Cryptic Overlord, Void Emperor, Darkstar Primarch, Nocturne Lord, Abyssal Archon, Eclipsed Herald, **Nightfall Sovereign**.

**The 30 stages** are a sequential chain of evil-themed collection quests (stage 2 requires stage 1, etc.) — see the [Advancements table](#darkness-stages-30).

**Dark Realm traversal rules** (applies to Time Shift and Dark/Light Crystal travel across the Dark Realm boundary): allowed if you (a) have the `darkness` tag, (b) are being assisted by a darkness-tagged player, or (c) have effective level **10 or higher**. Otherwise: *"Only those touched by darkness can cross the dark realm at this stage."*

---

## Amethyst Dampening

Amethyst is the natural enemy of powers. A player is **dampened** if any of these are true (checked every second):

1. Any item whose ID contains `amethyst` is in their inventory, offhand, or armor (e.g. Amethyst Shards, the Amethyst Greatblade, Amethyst Blocks).
2. Any block whose ID contains `amethyst` is within a **6-block cube** around them (ore veins, budding amethyst, clusters).
3. A **powered** Amethyst Power Ward is within **20 blocks** (the ward is redstone-powered; see [Blocks](#blocks)).

While dampened the player carries **Amethyst Poisoning** and suffers:

- **Powers completely suppressed** — abilities and crystals refuse to activate.
- **Damage immunity from players** (dampened players cannot be hurt by player sources at all).
- **Cannot be teleported or possessed** by other players' powers.
- Their energy HUD renders fully empty with a purple screen border.
- Teleporting into a powered ward's radius **repels the teleport and deals 20 magic damage** (10 hearts).

---

## The 28 Powers

All costs are activation energy; cooldowns are the declared design values. Passives are permanent while the power is assigned.

### Movement & Utility

| Power | Passive | Cost / Cooldown | Mechanic |
|---|---|---|---|
| **Flight** `powers:flight` | Speed I | 10 / toggle, 1/sec | Creative-style flight with a rainbow trail; restores prior fly settings on toggle-off, 3 s Slow Falling on landing. |
| **Super Speed** `powers:super_speed` | Speed II | 10 / 15 s | 8 s self-buff: Speed V, Jump Boost III, Slow Falling. |
| **Speed Burst** `powers:speed_burst` | Jump Boost II | 10 / 7 s | Dash 2.2 blocks/s in your look direction with 6 s Slow Falling. |
| **Shadow Step** `powers:shadow_step` | Night Vision I | 10 / 5 s | Blink up to 12 blocks through darkness to the block you're looking at. |
| **Time Shift** `powers:time_shift` | Haste I | 18 / 20 s | Input-driven: teleport to coordinates in any dimension or to a player, with a 2.5 s particle storm and companions. Blocked by anchors, wards, and realm rules. Player-target mode = spectator "marking mode". |
| **Telekinesis** `powers:telekinesis` | Slow Falling I | 24 / 12 s | Flings all living entities in a 16×12×16 area toward you in an arc. |
| **Gravity Displacement** `powers:gravity_displacement` | Slow Falling I | 28 / 15 s | Levitation IV for 4 s on all enemies within 8 blocks; you float too. |
| **Breezy Bash** `powers:breezy_bash` | Slow Falling I | 28 / 20 s | Launch everything in a 16³ area skyward, then slam them back down 0.9 s later. |

### Offense

| Power | Passive | Cost / Cooldown | Mechanic |
|---|---|---|---|
| **Fireball** `powers:fireball` | Fire Resistance I | 4 / none | Summons a large fireball that hovers before you — punch it to launch it at your aim. |
| **Lightning Strike** `powers:lightning_strike` | Resistance I | 4 / none | Single lightning bolt at the block you're looking at (up to 64 blocks). |
| **Energy Beam** `powers:energy_beam` | Strength I | 22 / 4 s | 48-block beam; first target takes 10 magic damage + 3 s of fire. |
| **Void Beam** `powers:void_beam` | Absorption I | 22 / 6 s | 32-block beam; first target takes 6 magic damage + Wither II (5 s). |
| **Frost Nova** `powers:frost_nova` | Water Breathing I | 22 / 15 s | Freezes water in a 4-block radius to frosted ice; 4 damage + Slowness III (6 s) in a 12×8×12 area. |
| **Ice Manipulation** `powers:ice_manipulation` | Water Breathing I | 22 / 5 s | 32-block beam: water→ice, lava→obsidian, air→snow; targets take 8 freeze damage, Slowness V, Weakness II. |
| **Starfall** `powers:starfall` | Health Boost I | 45 / 15 s | 3 lightning bolts within a 6-block radius of your aim (up to 64 blocks). |
| **Ground Slam** `powers:ground_slam` | Strength I | 35 / 10 s | Hulk ground-pound: block-breaking explosion, then 6 damage + heavy knockback in a 10×6×10 area. |
| **Elemental Blast** `powers:elemental_blast` | Fire Resistance I | 28 / per-element | One power, four phases — cycles **Fireball → Frost Nova → Lightning Strike → Ground Slam** on every use, each with its own cooldown. |
| **Energy Drain** `powers:energy_drain` | — | 30 / — | Target player within 32 blocks: **empties their entire energy pool** and applies Exhaustion for 30 s (no regen at all). |
| **Dimensional Anchor** `powers:dimensional_anchor` | Strength I | 45 / 60 s | Anchors a target player to their current dimension for 2 minutes — no teleporting out. |

### Defense & Support

| Power | Passive | Cost / Cooldown | Mechanic |
|---|---|---|---|
| **Forcefield** `powers:forcefield` | Resistance I | 35 / 25 s | 8 s of Absorption X, Resistance V, Fire Resistance — and complete damage immunity while active. |
| **Cozy Campfire** `powers:cozy_campfire` | Regeneration I | 35 / 30 s | 10 s stationary 6-block aura: heals 2 HP + 1 hunger to everyone inside every 5 ticks. |
| **Plant Healing** `powers:plant_healing_acceleration` | Regeneration I | 24 / none | Bonemeal any growable crop within 12 blocks. |
| **Double Health** `powers:double_health` | — | 24 / toggle | Toggle: +20 max HP (Health Boost V), heals up to 20 on activation, re-clamps on toggle-off. |

### Status & Manipulation

| Power | Passive | Cost / Cooldown | Mechanic |
|---|---|---|---|
| **Slow World** `powers:slow_world` | Haste I | 45 / 60 s | 5 s local time dilation: everyone within 10 blocks gets Slowness V while you get Speed III + Jump Boost II. |
| **Time Freeze** `powers:time_freeze` | Speed I | 45 / toggle, 3/sec | Toggle: every mob within **48 blocks** loses AI, gravity and velocity — restored when you turn it off. |
| **Invisibility** `powers:invisibility` | — | 10 / toggle, 1/sec | Toggle: completely invisible, re-applied every tick. |
| **Vessel Possession** `powers:vessel_possession` | Night Vision I | 24 / 30 s | Look at a player within 32 blocks and spectate through their eyes for 10 s. |
| **Astral Projection** `powers:astral_projection` | Night Vision I | 32 / none | Leave your body as a spectator soul-form for up to 30 s, tethered to a 150-block radius; press the key again (or cross the boundary) to return. |

---

## Crystal Powers

Crystal powers are a tier above regular powers — they are **never** assigned randomly and never rolled by the Rainbow Crystal. Right-click the crystal to activate (all draw from your energy pool, default cost 20).

| Crystal | Power | Mechanic |
|---|---|---|
| Orange Crystal | **Creativity Manifestation** | Builds a 5×5 pocket shelter at the aimed block (concrete frame, stained-glass walls, glowstone ceiling) within 16 blocks. |
| Green Crystal | **Space-Time Manipulation** | Sneak-right-click cycles 3 modes: **Slow** (Slowness III 6 s), **Accelerate** (Hunger II + Speed II 6 s), **Freeze** — freezes every entity in every loaded dimension for 6 s while you move freely. |
| Blue Crystal | **Dreamwalking** | Possess a player within 32 blocks for 2 minutes; the host's max health is halved while you dream. Press again to end. |
| Indigo Crystal | **Middleworld Gateway** | Teleports you to the Middleworld dimension (2 min cooldown). |
| Light Crystal | **Light Crystal** | Teleport yourself (sneak-right-click) or a targeted player to the Light Realm (2 min cooldown, 1.5 s storm delay). |
| Dark Crystal | **Dark Crystal** | Mirror of the Light Crystal — sends you or a target to the **Dark Realm** (enforces the darkness traversal rules). |

---

## Crystal Items

| Item | Behavior |
|---|---|
| **Rainbow Crystal** | **Temporarily inert** — right-clicking does nothing. The re-roll/power-selection flow is disabled (placeholder code kept for restoration). Crafted from all seven color crystals; still collectible/craftable for the skill route. |
| **All crystals** | **Indestructible when dropped**: they never despawn, never burn in lava/fire, take no damage (lightning, explosions), survive `/kill @e`, can't be picked up by mobs, and are saved from the void. |
| **Red / Yellow / Violet Crystals** | Currently **inert** — their lore abilities (Inferno, Size Shift, Soul Link) are designed but not yet wired up. |
| **Infected Rainbow Crystal** | Intentionally inert — "its purpose is not yet revealed." |
| **Light / Dark Crystals** | See Crystal Powers above. |

---

## Runestones & Grimoires

- **Runestones (28 variants):** right-click to channel **+100 energy** into your pool. Reusable, stack of 16.
- **Grimoires (13 variants):** lore placeholder items — right-click tells you the book "is waiting for its lore chapters."

---

## Weapons (84)

All 84 weapons are registered in the `powers` namespace with tier-appropriate stats and **no recipes** — they are obtained from the POWERS / Combat creative tabs. 74 swords, 5 pickaxes, 5 shovels.

<details>
<summary><b>Netherite tier (39)</b></summary>

`ancient_greatslab`, `black_iron_clobberer`, `black_iron_greataxe`, `black_iron_greatsword`, `calamity_blade`, `crimson_cleaver`, `demonic_sword`, `demons_blood_blade`, `demons_blood_pick`, `demons_blood_shovel`, `demonslayers_greatsword`, `dragon_sword`, `emerald_greatcleaver`, `gilded_phoenix_greataxe`, `gloomsteel_greataxe`, `gloomsteel_katana`, `gloomsteel_knife`, `lycanbane`, `nocturne`, `oculus`, `phantomguard_greatsword`, `phantomguard_partisan`, `ravenous_blade`, `revenants_darkscepter`, `revenants_gravecleaver`, `revenants_gravescepter`, `solstice`, `talonbrand`, `treacherous_axe`, `treacherous_bludgeon`, `treacherous_cleaver`, `uchigatana`, `vaelith`, `valhakyra`, `vindicator`, `void_oculus`, `windreaper`, `winterthorn`, `zenith`

</details>

<details>
<summary><b>Diamond tier (30)</b></summary>

`amethyst_greatblade`, `amethyst_greatpick`, `amethyst_greatshovel`, `azure_dagger`, `azure_greataxe`, `azure_greatsword`, `azure_pickaxe`, `azure_sabre`, `azure_scythe`, `azure_shovel`, `berserkers_cleaver`, `berserkers_greataxe`, `claymore`, `crescent_greataxe`, `crystal_frostblade`, `crystal_frostscythe`, `ethereal_frostblade`, `flamberge`, `grand_claymore`, `heavenly_partisan`, `moonlight`, `nature_sword`, `piercer`, `runic_piercer`, `sacrificial_cleaver`, `spider_sword`, `talonpick`, `talonshovel`, `vengeance_blade`, `vesper`

</details>

<details>
<summary><b>Iron tier (15)</b></summary>

`iron_battle_axe`, `iron_broadsword`, `iron_dagger`, `iron_greataxe`, `iron_halberd`, `iron_hay_sickle` (display "Iron Scythe"), `iron_mace`, `iron_polearm`, `iron_sai`, `skeleton_axe`, `viridian_greataxe`, `viridian_pickaxe`, `viridian_shovel`, `wooden_bludgeon`, `wooden_tonfa`

</details>

---

## Imported Items (164)

164 items imported from an external texture pack, registered as `powers:imported_<name>` (dots in filenames become underscores) and available in the POWERS creative tab. **No recipes.**

- **77 food items** (`food_*`): cooked/smoked/stew items give nutrition 6 + saturation 0.6; raw ingredients nutrition 4 + saturation 0.3. All use an eat animation (1.6 s) and camel-eat sounds. Examples: `imported_food_bacon_cooked`, `imported_food_slab_beef_salted`, `imported_food_stew_sweetpod`, `imported_food_tomato`, plus fruits, vegetables, berries, grains and dried rations.
- **13 grimoires** (`book_grimoire_*`): abyssal, blight, celestial, deep, infernal, wild, recolor, and 6 recolor-overlay variants.
- **28 runestones/runes** (`artifact_*runestone*` / `artifact_*rune*`): 6 bound runestones (active/inert), runestone_back, 8 dark runestones (4 sizes + 4 inscribed), frigid, inert, and 11 overlay variants.
- **46 artifacts & misc** (`artifact_*`, `magic_essence_*`, `device_*`, `book_*`, `blood_salts_2`): ammolite, amulets, beating heart, blackpearl, bloodstone, bone figurines, bowls, bullion, coins, rings, dripping orbs, flute, ghoul heart, heart mechanism, lodestone, malignember, oddstone, philosopherstone, ritual dagger, small pot, soul matrix, soulstones (3 sizes, inert + charged), stars (incl. animated), trilobite fossils, woodheart, magic essences (blood/sacred/soul dust), miniportals, written/tattered pages.

---

## Blocks

| Block | Behavior |
|---|---|
| **Darkness Block** `powers:darkness` | Utterly black, **unbreakable** (hardness −1, blast resistance 3,600,000), no light. |
| **Pure Light Block** `powers:pure_light` | **Unbreakable**, emits **light level 15**. |
| **Amethyst Power Ward** `powers:amethyst_ward` | Harvestable (hardness 5, blast resistance 1200); emits light 2 unpowered / **10 when redstone-powered**. When powered it projects a **20-block anti-power field** (see [Amethyst Dampening](#amethyst-dampening)) with rotating END_ROD particles. Craftable (see [Recipes](#recipes)). |

---

## Mob Effects

| Effect | Color | Applied by | Behavior |
|---|---|---|---|
| **Exhaustion** `powers:exhaustion` | deep violet | Energy Drain (30 s) | **Locks your energy at 0** — no regen, no sleep refill, no rune charging. |
| **Amethyst Poisoning** `powers:amethyst_poisoning` | light purple | Amethyst Dampening (while dampened) | Marker for the anti-power field: powers suppressed, player-damage immunity, can't be teleported/possessed. |

---

## Dimensions

All three custom dimensions are **flat, barren single-layer worlds** (no mobs, no worldgen, no loot) with forced **Adventure** gamemode (restored on leaving). No damage can be dealt in the Dark and Light Realms.

| Dimension | Ground | Ambience | Notes |
|---|---|---|---|
| **Dark Realm** `powers:dark_realm` | Darkness Block (light 0) | Black sky, dark water, Nether cave/valley ambience, fixed time | Reached via Dark Crystal, Time Shift (with traversal rules), or `/powers travel`. Energy regens at 4/sec here. |
| **Light Realm** `powers:light_realm` | Pure Light Block (light 15) | White sky, bright water | Reached via Light Crystal, Time Shift, or `/powers travel`. The exit route from the Dark Realm. |
| **Middleworld** `powers:middleworld` | Pure Light Block (light 15) | White sky, bright water | Reached via Indigo Crystal or `/powers travel`. Time Shift is explicitly **blocked** from entering it. |

---

## Advancements

### Skill Routes (21)

Chain: `skill_root` → `level_01` → … → `level_21`. Each route completes the challenge to advance one skill level.

| # | Challenge | # | Challenge |
|---|---|---|---|
| 01 | Collect Amethyst Shard | 12 | Slay a Zombie |
| 02 | Collect Rainbow Crystal | 13 | Slay a Skeleton |
| 03 | Collect Bread (big) | 14 | Visit the Nether |
| 04 | Consume Bread (big) | 15 | Visit the End |
| 05 | Collect Amethyst Ward | 16 | Collect Dark Crystal |
| 06 | Visit the Light Realm | 17 | Collect Light Crystal |
| 07 | Visit the Dark Realm | 18 | Visit the Middleworld |
| 08 | Craft the Rainbow Crystal | 19 | Collect Philosopher's Stone |
| 09 | Collect a Grimoire (deep) | 20 | Slay a Warden |
| 10 | Collect a Frigid Runestone | 21 | Collect Soul Matrix |
| 11 | Hurt any entity in combat | | |

### Darkness Stages (30)

Chain: `darkness_root` (hidden until tagged) → `level_01` → … → `level_30`. Tagged players collect each item to advance a stage. Titles carry the rank name (e.g. "Darkness Stage 01: Acolyte of Gloom").

| # | Collect | # | Collect |
|---|---|---|---|
| 01 | Coal Block | 16 | Fermented Spider Eye |
| 02 | Obsidian | 17 | Soul Torch |
| 03 | Nether Bricks | 18 | Blackstone |
| 04 | Soul Sand | 19 | Lava Bucket |
| 05 | Wither Skeleton Skull | 20 | Bone |
| 06 | Fire Charge | 21 | Ink Sac |
| 07 | Ghast Tear | 22 | Dragon's Breath |
| 08 | Spider Eye | 23 | End Crystal |
| 09 | Rotten Flesh | 24 | Nether Star |
| 10 | Blaze Rod | 25 | Tipped Arrow (Poison) |
| 11 | Ender Pearl | 26 | Shulker Shell |
| 12 | Eye of Ender | 27 | Soul Campfire |
| 13 | Wither Rose | 28 | Magma Block |
| 14 | Crying Obsidian | 29 | Coal |
| 15 | Magma Cream | 30 | Soul Sand |

---

## Recipes

| Recipe | Result |
|---|---|
| Red + Orange + Yellow + Green + Blue + Indigo + Violet Crystal (shapeless) | **Rainbow Crystal** |
| Ring pattern: amethyst shards + redstone + amethyst block | **Amethyst Power Ward** |
| Smelting (200 ticks) | Cooked bacon, cooked fish fillet, cooked salmon fillet, cooked sausage, cooked slab beef, cooked slab cheval, cooked slab mooshroom, cooked slab pork |
| Smoking (100 ticks) | Smoked fish fillet, smoked salmon fillet |

Weapons and imported items deliberately have **no recipes**.

---

## Commands

| Command | Permission | Behavior |
|---|---|---|
| `/powers list` | anyone | Lists all power IDs |
| `/powers slots` | anyone | Shows your 3 assigned powers |
| `/powers slots <player>` | op 2 | Shows another player's powers |
| `/powers assign <player> <power> <slot>` | op 2 | Assigns a power to a slot (0–2) |
| `/powers reroll` | anyone | Re-rolls your powers randomly |
| `/powers reroll <player>` | op 2 | Re-rolls another player |
| `/powers travel <dimension>` | op 2 | Teleports you to a dimension (e.g. `powers:dark_realm`) |

The `darkness` tag is applied with the vanilla command: `/tag <player> add darkness`.

---

## Client Features

- **Power HUD** (bottom-right): three 40×40 boxes showing your keybind, power name, and power color (toggle powers glow only while active).
- **Energy HUD**: 10-segment bar above the hunger row, depleting left-to-right; renders empty with a purple border while amethyst-dampened.
- **Power Selection screen** (disabled): the re-roll UI behind the Rainbow Crystal is currently inactive — the screen class and its packets remain as placeholders for restoration.
- **Teleport Input screen** (Time Shift): coordinate entry, dimension cycle (Overworld / Nether / End / Dark Realm / Light Realm), and "To Player" mode with spectator marking (fly to the spot, press the power key to confirm within 10 s).
- **Keybinds:** `V` / `X` / `C` for powers, `P` reserved for the (disabled) power menu — rebindable in the POWERS category.

---

## Notes & Caveats

- **Cooldowns** are declared per ability (e.g. "15 s"), but the activation pipeline currently gates on **energy and amethyst dampening only** — cooldown enforcement is not yet wired into the activation path.
- **Designed-but-unbound crystal abilities** exist in the code (their tick systems run) but are not attached to any item yet: Red=**Inferno** (firestorm), Orange=**Cloning** (wolf swarm), Yellow=**Size Shift**, Green=**Life Bloom**, Blue=**Chrono Stop** (30 s global time stop), Indigo=**Portal Rift**, Violet=**Soul Link** (damage mirroring). The orange/green/blue/indigo crystals are currently bound to their *other* planned powers (see [Crystal Powers](#crystal-powers)).
- Red, Yellow, Violet and Infected Rainbow crystals are intentionally **inert** pending lore.
- The **Rainbow Crystal re-roll flow is temporarily disabled** (item, recipe, advancements and screen/packet code all remain as placeholders).
- Loot tables: none — realm blocks drop nothing and there is no worldgen beyond the flat realms.
