# POWERS

A Rainbow-Quest / Bliss-SMP-inspired Fabric mod for Minecraft 26.2. Every player starts with three randomly assigned superpowers drawn from a pool of 28, charges them with a shared energy pool, and grows from a humble "Unawakened" into a world-shattering "Origin" — or abandons the light and walks the hidden 10-rank path of **The Darkness**.

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

- **Creative tab**: everything is in the dedicated **POWERS** creative tab; weapons also appear in the vanilla **Combat** tab; crystals, blocks and items also appear in the vanilla **Ingredients** tab.

---

## Power Assignment

- Every player has **3 power slots**, assigned randomly and distinct on first login. They persist across logins and server restarts.
- Powers are permanent — there is no in-game way to change your powers.
- Admins can force assignment with `/powers assign <player> <power> <slot>` or `/powers reroll [player]`.
- Each regular power grants a **permanent passive effect** while assigned (e.g. Haste, Night Vision, Speed, Fire Resistance, Absorption, Strength, Regeneration…) — see the power list below.
- Crystal powers are a tier above regular powers: **never** assigned randomly and never rolled by the Rainbow Crystal — the only way to hold one is to craft and use the crystal item itself.

---

## The Energy System

All abilities draw from a single shared **energy pool** (shown as a 10-segment bar above the hunger row).

| Rule | Value |
|---|---|
| Base capacity (light path) | **250** |
| Capacity per skill rank | **+52 per rank** (max **770** at rank 10) |
| Base capacity (darkness path) | **500** |
| Darkness capacity per rank | **+135 per rank** (max **1,850** at rank 10) |
| Regen (light path) | **1 per second** |
| Regen (darkness path) | **2 per second** (4 per second in the Dark Realm or at night) |
| Sleep | Fully refills the pool when you stop sleeping |
| Runes | Right-clicking a runestone channels **+100 energy** |

**Key rules:**

- **Costs never scale with level.** Higher skill/darkness levels never make powers cheaper — they only enlarge the pool, so it is harder to fully run out.
- Toggle abilities (Flight, Invisibility, Time Freeze) pay their activation cost up front, then drain energy **once per second** while active (1–3 per second). If the pool runs dry the toggle force-disables.
- **Energy backlash:** letting a draining toggle burn out on an empty pool is punished — the power is torn off but you take **70% of your max health in magic damage**, a divine-wrath sequence erupts (rune circle, shockwave rings, pillar of sparks, thunder) and a lightning storm crashes down on you, with a second wave a heartbeat later. The message itself is drawn randomly from six mythic phrasings.
- Failed activations **refund** their energy cost.
- The **Exhaustion** effect drains your energy pool over a few seconds (hunger-style: faster at higher amplifier) and blocks all refills — no regen, no sleep refill, no rune charging.

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

Progression on the **light path** is driven by the **10-route advancement chain** (`powers:skill/level_01` → … → `level_10`). Each rank demands **multiple challenges at once**, and the chain is cumulative — every rank above 1 also requires everything below it. Ranks get noticeably harder as you climb: early ranks just need a few curios, the middle demands realm travel and monster hunts, and the top requires the Warden's kill and endgame artifacts.

**Rank titles (10):** Unawakened → Spark → Awakened → Adept → Weaver → Arcanist → Luminary → Voidcaller → Ascendant → **Origin**.

**What levels do:**

| Effect | Formula |
|---|---|
| Ability damage | `base × (1.0 + level × 0.0525)` (+5.25%/rank, **+52.5% at 10**) |
| Ability range | `base × (1.0 + level × 0.021)` (+2.1%/rank, **+21% at 10**) |
| Energy capacity | `250 + level × 52` |

**Presentation:** your chat messages are broadcast as `[Rank] Name: message` in the rank's color, and the rank prefix also appears above your head as a custom name.

---

## The Darkness System

The hidden second progression track. **By default it is completely invisible** — the advancement tab and its quests only exist for players carrying the entity tag `darkness` (`/tag <player> add darkness`). Tagged players immediately receive the hidden "Darkness Initiation" root advancement, which reveals the Darkness tab.

**Darkness is not a choice — it is a takeover.** The moment the tag is applied the player switches to the darkness system entirely: the light-path **Skill tab disappears from their advancements screen** (the root advancement is revoked) and comes back only if the tag is removed. There is no way to use both at once.

**What changes for darkness-tagged players:**

- Their energy pool switches to the **darkness pool** (500 base, +135/rank — much harder to drain) and regenerates 2/sec (4/sec in the Dark Realm or at night).
- Their **effective level** becomes their darkness level instead of their skill level — this drives all damage/range scaling.
- Their chat/name prefix uses the darkness rank instead of the skill rank.
- They can enter the Dark Realm on their own once they reach **rank 5** (see traversal rules below).
- **Prefix hiding:** `/powers darkprefix` toggles whether their visible title is the real darkness rank or the **equivalent normal-ladder name** (same rank number, taken from the light-path titles). Great for players who don't want to advertise the darkness on their head.

**Rank titles (10):** Murk → Shade → Umbra → Wraith → Revenant → Dread → Soulblight → Abyssal → Voidwight → **Nightfall**.

**The 10 ranks** are a sequential chain of collection quests, **three items per rank**, escalating from common blocks to Nether relics, Wither trophies, End spoils and finally the Nether Star itself (rank 2 requires rank 1, etc.) — see the [Advancements table](#darkness-ranks-10).

**Dark Realm traversal rules** — the Dark Realm only appears as a Time Shift destination option for players with the `darkness` tag at **rank 5 or higher**. Unworthy players never see it as an option, but can still reach it two ways: stand close to another player teleporting there (companions ride along), or use the **Dark Crystal** (fused with darkness itself — no gate). Leaving the Dark Realm is always free; nobody who gets in is ever trapped.

**A darkness-marked appetite** — the darkness changes what a player can stomach. Every edible item in the game (vanilla and imported alike) is sorted into three buckets, and darkness-tagged players experience them differently:

- **Normal food** — the everyday cooked dishes of the surface world. To the darkness-touched these taste of rot: each bite fills barely any hunger, restores almost no saturation, and always leaves **30 seconds of Hunger**. This mirrors how ordinary people feel about rotten flesh.
- **Abnormal food** — raw meat and fish, wormy fruit, rot, poison, alien and earthy oddities. For the darkness-touched these are **the true feasts**: each bite restores boosted nutrition (at least 6, or 1.5× the item's normal value) with **tripled saturation**, with none of the ill effects ordinary eaters suffer.
- **Neutral food** — humble staples and plain produce that everyone can enjoy. The darkness-touched eat these exactly as before.

Neutral staples are the safest food for a darkness player to travel with; ordinary prepared meals must be avoided. The full inventory (bare item paths; vanilla under `minecraft:`, imported under `powers:`):

| Normal — foul to the darkness | Abnormal — a feast for the darkness | Neutral — fine for everyone |
| --- | --- | --- |
| mushroom_stew | rotten_flesh | bread |
| beetroot_soup | spider_eye | apple |
| cooked_porkchop | pufferfish | carrot |
| cooked_beef | pufferfish_bucket | potato |
| cooked_chicken | tropical_fish | baked_potato |
| cooked_rabbit | tropical_fish_bucket | golden_carrot |
| cooked_mutton | cod | golden_apple |
| cooked_cod | cod_bucket | enchanted_golden_apple |
| cooked_salmon | salmon | melon_slice |
| rabbit_stew | salmon_bucket | sweet_berries |
| imported_food_bacon_cooked | porkchop | glow_berries |
| imported_food_fish_fillet_cooked | beef | cookie |
| imported_food_fish_fillet_smoked | chicken | dried_kelp |
| imported_food_salmon_fillet_cooked | rabbit | beetroot |
| imported_food_salmon_fillet_smoked | mutton | honey_bottle |
| imported_food_sausage_cooked | poisonous_potato | pumpkin_pie |
| imported_food_slab_beef_cooked | chorus_fruit | imported_food_apple_green |
| imported_food_slab_cheval_cooked | suspicious_stew | imported_food_beans |
| imported_food_slab_mooshroom_cooked | imported_food_apple_wormy | imported_food_beet |
| imported_food_slab_pork_cooked | imported_food_apple_wormy_2 | imported_food_billberry |
| imported_food_stew_sweetpod | imported_food_bacon_raw | imported_food_blackberry |
|  | imported_food_fish_fillet_raw | imported_food_blueberries |
|  | imported_food_salmon_fillet_raw | imported_food_bread_big |
|  | imported_food_sausage_raw | imported_food_cabbage |
|  | imported_food_slab_beef_raw | imported_food_chickpeas |
|  | imported_food_slab_cheval_raw | imported_food_coconut_normal |
|  | imported_food_slab_mooshroom_raw | imported_food_coconut_opened |
|  | imported_food_slab_pork_raw | imported_food_coconut_straw |
|  | imported_food_slab_beef_salted | imported_food_cranberries |
|  | imported_food_slab_cheval_salted | imported_food_fig |
|  | imported_food_slab_pork_salted | imported_food_fisherberries |
|  | imported_food_muckroot | imported_food_garlic |
|  | imported_food_jerky | imported_food_grapes |
|  |  | imported_food_leek |
|  |  | imported_food_lentils |
|  |  | imported_food_lettuce |
|  |  | imported_food_mulberries |
|  |  | imported_food_mungbean |
|  |  | imported_food_onion |
|  |  | imported_food_pantao |
|  |  | imported_food_pepper |
|  |  | imported_food_prickleberries |
|  |  | imported_food_radish |
|  |  | imported_food_raspberries |
|  |  | imported_food_redbeans |
|  |  | imported_food_silver_pear |
|  |  | imported_food_slice_cantaloupe |
|  |  | imported_food_slice_honeydew |
|  |  | imported_food_slice_hornedmelon |
|  |  | imported_food_slice_squash |
|  |  | imported_food_slice_wintermelon |
|  |  | imported_food_spinach |
|  |  | imported_food_strawberries |
|  |  | imported_food_sunberries |
|  |  | imported_food_sweetpod |
|  |  | imported_food_tomato |
|  |  | imported_food_uradbean |
|  |  | imported_food_wisdomfruit |

The niche ingredient items (Food Bryony, Food Dough, Food Pile Rice, Food Butter, Food Hardtack, Food Spices, Food Cornkernels, Food Tomatillos, Food Slice Eggplant) have been **removed from the mod entirely**. The classification lives in `FoodAffinity.java`.

**Where the food comes from** — every food item above drops naturally in the world:

- **Villagers** drop a **Beating Heart** on death, plus one piece of garden produce or humble fare (tomatoes, lettuce, cabbage, onions, garlic, leeks, radishes, spinach, peppers, beets, beans, chickpeas, lentils, mungbean, uradbean, redbeans, sweetpod, big bread, sweetpod stew).
- **Pigs** drop pork slabs, raw bacon and sausages; **cows** drop beef slabs; **horses** drop cheval slabs; **mooshrooms** drop mooshroom slabs.
- **Cod** and **salmon** sometimes drop raw fillets; all raw cuts and fillets can be **smelted or smoked** into their cooked variants (recipes included).
- **Husks** (desert zombies) carry preserved fare: jerky and salted meat slabs.
- **Small slimes** occasionally dig up muckroot from the swamp.
- **Foxes** drop a forest berry (billberry, blackberry, blueberries, cranberries, fisherberries, mulberries, prickleberries, raspberries, strawberries or sunberries).
- **Oak leaves** rarely drop green and wormy apples; **jungle leaves** drop figs, grapes and coconuts; **cherry leaves** drop pantao (peach); **birch leaves** drop silver pears.
- **Melons** drop cantaloupe, honeydew, horned melon and winter melon slices; **pumpkins** drop squash slices.

Food items that still lack a natural source are listed in [NATURALLY_UNOBTAINABLE_ITEMS.md](NATURALLY_UNOBTAINABLE_ITEMS.md).

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
| **Energy Drain** `powers:energy_drain` | — | 30 / — | Target player within 32 blocks: drains their energy pool over a 2-second ritual, then applies Exhaustion for 30 s (no regen at all). |
| **Dimensional Anchor** `powers:dimensional_anchor` | Strength I | 45 / 60 s | Anchors a target player to their current dimension for 2 minutes — no teleporting out. |

### Defense & Support

| Power | Passive | Cost / Cooldown | Mechanic |
|---|---|---|---|
| **Forcefield** `powers:forcefield` | Resistance I | 35 / 25 s | 8 s of Absorption X, Resistance V, Fire Resistance — and complete damage immunity while active. |
| **Cozy Campfire** `powers:cozy_campfire` | Regeneration I | 35 / 30 s | 10 s stationary 6-block aura: heals 2 HP + 1 hunger to friendly players and mobs inside every 5 ticks (hostiles excluded). |
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
| Dark Crystal | **Dark Crystal** | Mirror of the Light Crystal — sends you or a target to the **Dark Realm** (fused with darkness, so it works for anyone; 2 min cooldown, 1.5 s storm delay). |

---

## Crystal Items

| Item | Behavior |
|---|---|
| **Rainbow Crystal** | **Inert placeholder** — no power is bound to it; it is reserved for a future purpose of its own. Crafted from all seven color crystals; collectible/craftable for the skill route. |
| **All crystals** | **Indestructible when dropped**: they never despawn, never burn in lava/fire, take no damage (lightning, explosions), survive `/kill @e`, can't be picked up by mobs, and are saved from the void. |
| **Red / Yellow / Violet Crystals** | Currently **inert** — their lore abilities (Inferno, Size Shift, Soul Link) are designed but not yet wired up. |
| **Infected Rainbow Crystal** | Intentionally inert — "its purpose is not yet revealed." |
| **Light / Dark Crystals** | See Crystal Powers above. |

---

## Runestones & Grimoires

- **Runestones (28 variants):** right-click to channel **+100 energy** into your pool. Reusable, stack of 16.
- **Grimoires (13 variants):** lore placeholder items — right-click tells you the book "is waiting for its lore chapters."
- **Book Grimoire Celestial — the Locator Spell:** right-click while holding **30 XP levels** opens a picker of every online player; choose one and the grimoire casts a celestial ritual (rising rune spiral, expanding rings, a pillar of starlight) before whispering the target's **dimension and coordinates** — to you alone. The 30 levels are spent even when the spell fails. The cast recoils with **20 s of nausea** (and nothing else revealed) when: the caster carries/holds anything amethyst (or stands near amethyst blocks or a powered ward); the target is in the **Light Realm** but the caster isn't a max-rank light-path player (or carries the darkness tag); or the target is in the **Dark Realm** but the caster isn't a max-rank darkness user. On a successful scry the target feels a brief prickle of starlight wherever they are.

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
| **Exhaustion** `powers:exhaustion` | deep violet | Energy Drain (30 s) | **Drains your energy pool** over a few seconds (faster at higher amplifier) — no regen, no sleep refill, no rune charging. |
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

### Skill Routes (10)

Chain: `skill_root` → `level_01` → … → `level_10`. Each rank requires **all of its challenges** (and, by chaining, everything below). Difficulty climbs steeply: the last ranks demand Warden slays and endgame artifacts. The Skill tab only appears for players **without** the `darkness` tag.

| # | Rank | Task (in full) |
|---|---|---|
| 01 | Unawakened | Obtain an **Amethyst Shard** — the first spark of mastery, mined from amethyst geodes. |
| 02 | Spark | **Hold the Rainbow Crystal** and **eat a Bread Big** (imported food). |
| 03 | Awakened | **Craft the Amethyst Power Ward** and **open a Grimoire Deep** (right-click it). |
| 04 | Adept | **Hold the Frigid Runestone** and **visit the Light Realm** (Light Crystal, Time Shift, or `/powers travel`). |
| 05 | Weaver | **Visit the Dark Realm**, **craft the Rainbow Crystal**, and **hurt an entity in combat**. |
| 06 | Arcanist | **Slay a Zombie** and a **Skeleton**, then **descend into the Nether**. |
| 07 | Luminary | **Visit the End**, **slay a Blaze**, and **obtain the Philosopher's Stone**. |
| 08 | Voidcaller | **Hold the Dark Crystal and the Light Crystal** and **step into the Middleworld**. |
| 09 | Ascendant | **Slay the Warden** and **collect the Nether Star**. |
| 10 | Origin | **Complete the Soul Matrix** and **hold an End Crystal**. |

### Darkness Ranks (10)

Chain: `darkness_root` (hidden until tagged) → `level_01` → … → `level_10`. Tagged players collect **all three items** of a rank to advance; each rank also requires the one before it. Titles carry the rank name (e.g. "Darkness Rank 03: Umbra"). The Darkness tab only appears for players **with** the `darkness` tag — untagged players never see it.

| # | Rank | Collect |
|---|---|---|
| 01 | Murk | Coal Block, Coal, Bone — a miner's first grave-goods. |
| 02 | Shade | Obsidian, Nether Bricks, Soul Sand — the foundations of the underworld. |
| 03 | Umbra | Blackstone, Magma Block, Soul Torch — darkness tempered by fire. |
| 04 | Wraith | Spider Eye, Rotten Flesh, Fermented Spider Eye — poisons and decay. |
| 05 | Revenant | Ink Sac, Magma Cream, Fire Charge — the shadow's tools of trade. |
| 06 | Dread | Blaze Rod, Ghast Tear, Lava Bucket — the fortress's bounty. |
| 07 | Soulblight | Wither Skeleton Skull, Soul Campfire, Wither Rose — the Wither's court. |
| 08 | Abyssal | Ender Pearl, Eye of Ender, Crying Obsidian — warding against the void's rivals. |
| 09 | Voidwight | Shulker Shell, Poison Tipped Arrow, End Rod — spoils of the End cities. |
| 10 | Nightfall | End Crystal, Nether Star, Dragon's Breath — the final claim over light itself. |

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
| `/powers darkprefix` | anyone | Toggles whether your visible title is your real darkness rank or the equivalent normal-ladder rank name (`/powers darkprefix true` to hide, `false` to reveal) |
| `/powers travel <dimension>` | op 2 | Teleports you to a dimension (e.g. `powers:dark_realm`) |

The `darkness` tag is applied with the vanilla command: `/tag <player> add darkness`.

---

## Client Features

- **Power HUD** (bottom-right): three 40×40 boxes showing your keybind, power name, and power color (toggle powers glow only while active).
- **Energy HUD**: 10-segment bar above the hunger row, depleting left-to-right; renders empty with a purple border while amethyst-dampened.
- **Teleport Input screen** (Time Shift): coordinate entry, dimension cycle (Overworld / Nether / End / Dark Realm / Light Realm), and "To Player" mode with spectator marking (fly to the spot, press the power key to confirm within 10 s).
- **Travel storms echo the destination realm:** the lightning summoned beneath a teleporting/banished player builds up that realm's signature — heavy campfire smoke when bound for the **Dark Realm**, glittering totem sparks, fireworks and starlight when bound for the **Light Realm**. The realm dimensions themselves are always clear — this buildup belongs to the cast, not the sky.
- **Keybinds:** `V` / `X` / `C` for powers — rebindable in the POWERS category.

---

## Notes & Caveats

- **Cooldowns** are declared per ability and enforced by the activation pipeline — slot keys and crystals both check the remaining cooldown (broadcast in seconds) before spending energy, and only a successful activation starts one.
- **Divine punishments** — the mod treats a few occasions as moments of divine judgement with a full godly FX sequence (rune circle, shockwave rings, rising spark pillar, thunderous sounds, and a delayed second wave) in `GodlyPunishment`:
  - **Energy burnout:** 70% max-health magic damage + lightning storm + divine strike (see [Energy & Backlash](#energy--backlash)).
  - **Teleporting into a powered ward:** 20 magic damage + purple divine strike (see [Amethyst Dampening](#amethyst-dampening)).
  - **Using powers while amethyst-dampened:** the amethyst bites back with 2.5 magic damage, violet sparks and a stinging message — it punishes defiance, so don't mash keys in a ward.
  - **Trying to act while frozen by Space-Time:** cold chime, frost sparks and a reminder that time itself holds you.
  - **Blocked travel:** dimensional anchors flash crimson chains; the middleworld shows a shimmering blue barrier; the dark realm drags the unworthy back with a void-swallowing whirl.
- **Randomized message variants** — every player-facing message is chosen at random from a group of 3–6 phrasings (`PowerMessages`), so repeated events never read the same twice. Failure, punishment and rejection messages lean into the godly mythos.
- **Designed-but-unbound crystal abilities** exist in the code (their tick systems run) but are not attached to any item yet: Red=**Inferno** (firestorm), Orange=**Cloning** (wolf swarm), Yellow=**Size Shift**, Green=**Life Bloom**, Blue=**Chrono Stop** (30 s global time stop), Indigo=**Portal Rift**, Violet=**Soul Link** (damage mirroring). The orange/green/blue/indigo crystals are currently bound to their *other* planned powers (see [Crystal Powers](#crystal-powers)).
- Red, Yellow, Violet and Infected Rainbow crystals are intentionally **inert** pending lore.
- The **Rainbow Crystal is an inert placeholder** — the re-roll mechanic is removed entirely and the crystal awaits its own dedicated purpose.
- Loot tables: none — realm blocks drop nothing and there is no worldgen beyond the flat realms.
