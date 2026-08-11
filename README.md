# POWERS

POWERS is a server-first Fabric mod for Minecraft Java Edition 26.2. It combines randomly assigned innate powers, branching light and darkness titles, ritual grimoires, crystal abilities, vulnerable mind-projection bodies, and three custom mindscape dimensions.

The tone is heavily inspired by FavreMySabre's *The Rainbow Quest*, with additional ancient-magic and supernatural-horror influences. This is an independent fan project and is not affiliated with those creators or franchises.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.156.0+26.2 or newer
- Java 25
- The mod installed on both the server and every client

## Playing

On first join, a player receives three distinct powers from a pool of 23. They persist with the player. The default keys are `V`, `X`, and `C`; they can be rebound in Minecraft's Controls screen. Crouch while pressing a power key to open that power's option menu when it has selectable modes.

Innate loadouts are allegiance-aware. Radiant players can uniquely receive Starfall and Plant Healing Acceleration; darkness-infected players can uniquely receive Void Beam and Energy Drain. Every random loadout guarantees at least one matching exclusive power. If allegiance changes, compatible powers remain in place while forbidden or retired slots migrate deterministically without duplicates. Crystal, grimoire, and Shadow Sword access is unaffected; the sword deliberately bypasses innate allegiance locks. Merely owning an innate power applies no passive effects or ambient particles.

The HUD shows the three powers as compact 30-pixel rune medallions at the right edge, with live toggle and rank-adjusted cooldown states. Energy is rendered as ten separate nine-pixel symbols directly above and exactly aligned with the vanilla hunger bar. Full, half, and empty symbols use normal, darkness, amethyst-poisoned, or projected-body colours; the bar is moved with the vanilla survival HUD so extra heart rows never overlap it.

On first join, each persistent character receives one vanilla written book, **POWERS: First Awakening**. It explains V/X/C, the B-key Rank Maze, energy, grimoires, crystals, mind-body vulnerability, artifacts, Shadow chat, and the major counter-magic systems. The persistent delivery flag prevents reconnect, respawn, or death duplication; if the inventory is full the book is dropped safely at the player.

The assignable powers are:

- Movement and control: Size Morphing, Flight, Super Speed, Speed Burst, Teleport, Telekinesis, Gravity Displacement, and Breezy Bash.
- Offence: Fireball, Lightning Strike, Thunderclap, Energy Beam, Void Beam, Ice Manipulation, Starfall, and Energy Drain.
- Defence and support: Forcefield, Plant Healing Acceleration, and Double Health.
- Time and mind: Time Freeze, Invisibility, Vessel Possession, and Astral Projection.

| Innate power | What it does |
| --- | --- |
| Size Morphing | Selects `0.25×`–`2×` from rank 0, then unlocks `2.5×` at rank 4, `0.125×` at rank 6, `3×` at rank 7, and `4×` at rank 10. Distance from normal size determines the per-second energy drain. |
| Teleport | Opens a server-advertised dimension/coordinate menu and asynchronously loads, validates, and enters even currently unloaded destination chunks. A two-stage 100-tick storm announces the vulnerable transfer at both ends. It can instead locate one uniquely named player or mob, subject to consent and concealment rules. |
| Flight | Uses server-authoritative survival propulsion, including directional ascent/descent and a much faster sprint-flight mode; it never grants creative-mode flight. |
| Starfall | Opens a warned, finite celestial strike sequence with rank branches for extra strikes, a moving storm eye, concealment reveal, projectile diversion, echoes, and a dominion crown. |
| Void Beam | Charges a penetrating live-aim ray, with ranked target penetration and a temporary terrain-safe void scar that interacts with later magic. |
| Fireball | Summons one chargeable Cinderheart. Recasts add tiers, punching launches it, and bounded reflection/counter rules prevent uncontrolled projectile clouds. |
| Lightning Strike | Opens a warned Storm Tribunal at the aimed column. It has no cooldown, while energy, protection, and concurrency checks still prevent invalid spam. |
| Thunderclap | Creates a wide boss-scale pressure cone, inflicting heavy rank-scaled damage and stun while deflecting incoming projectiles. |
| Speed Burst | Performs a collision-predicted physical dash and final shockwave. Motion ranks can spend a second step during the brief marked window. |
| Telekinesis | Throws permitted living targets radially away and reflects a bounded set of hostile projectiles; an empty cast refunds its energy. |
| Energy Beam | Channels four live-aim Sunfire beats whose damage and burn intensify on one target; water produces steam and advanced ranks unlock a flare or forks. |
| Super Speed | Runs an eight-second Chronal Overdrive with server-owned motion, restrained afterimages, water grounding, collision rites, memory slips, and mastered projectile curvature. |
| Breezy Bash | Captures permitted bodies in a two-stage wind rite, raises them through a visible apex, then calls each safely down with rank-scaled force. |
| Invisibility | Toggles an infinite, POWERS-owned amplifier-255 concealment effect whose particles and HUD icon are hidden. It drains energy continuously and can be exposed by attacks, counter-magic, or Insight effects without deleting unrelated invisibility. |
| Time Freeze | Claims Minecraft's global server tick freeze across every loaded dimension, preventing entities, projectiles, attacks, and world ticks while the caster remains able to act. Its upkeep consumes at least 15% of the caster's full energy capacity each second, so an undisturbed full well lasts only about seven seconds. |
| Forcefield | Gives the caster and compatible players/test actors within two blocks a finite integrity ward. It has no timer and completely absorbs the overkill impact that breaks it. During mind travel, an owner's ward remains around the vulnerable physical body rather than the invulnerable remote avatar. |
| Gravity Displacement | Opens a five-second orrery that collision-safely orbits nearby bodies, resolves overlapping claims, and can collapse or curve projectiles at advanced ranks. |
| Vessel Possession | Moves the mind into a consented player or suitable mob for at most 30 seconds while a vulnerable skin-matched physical body remains at the casting point. Movement, aim, jumping, crouching, hotbar selection, and attacks are server-authoritative; mobs are released with their original AI state. A higher-ranked player cannot be possessed. |
| Astral Projection | Leaves a vulnerable physical body and releases a bounded soul-form scout; return is validated and cannot be used as invulnerability. |
| Energy Drain | Channels against a player or mob for two seconds. Players lose energy and receive particle-free Exhaustion; mobs take repeated percentage-health damage plus a capped 30% completion strike, while the caster recovers energy. |
| Ice Manipulation | Fires a freezing ray that harms and freezes targets, converts water to ice and lava to obsidian, and lays snow only where terrain policy permits. |
| Plant Healing Acceleration | Normally grows the aimed bonemealable plant with stronger growth at higher potency. Crouching instead releases a zero-cooldown healing pulse that restores the caster and injured players within an inclusive two-block radius. |
| Double Health | Toggles a mod-owned maximum-health multiplier, preserves unrelated modifiers, drains energy while active, and proportionally restores the vanilla heart layout when released. |

Telekinesis is a true radial release: permitted living targets are thrown away from the caster while up to 16 hostile projectiles are reflected along the caster's aim. If neither can be affected, its collapsed violet rune refunds the offered energy and starts no cooldown or collision residue.

Speed Burst is a synchronized physical dash, not a teleport. It predicts body-volume collisions, leaves an eight-tick cyan-white afterimage wake, and ends in a rank-scaled kinetic shockwave that respects safe zones, amethyst, and forced-movement consent without damaging terrain. Motion-ranked players can pay for one stronger Second Step during a 2.5-second window while the original persistent cooldown remains armed; alternating cyan-gold runes and an `II` medallion mark that server-authorized follow-up.

Super Speed is an eight-second server-owned Chronal Overdrive rather than a bundle of anonymous potion effects. A POWERS-owned movement modifier follows rank potency, drops to 35% strength in water with a visible hydroplane transition, and leaves only measured cyan-white wakes so teleports cannot forge trails. Wall contact fractures a time seal: Motion may rewind through one collision-safe Second Step, Might may release one non-damaging eight-body pressure corona, Veil periodically slips the runner from at most eight nearby hostile target memories without granting invisibility, and Dominion curves at most 16 approaching hostile projectiles once without reflection or ownership theft. Consent and safe zones, amethyst, projection bodies, forcefields, powered wards, time locks, and blocked geometry each resist with distinct counter-sigils. Death, disconnect, respawn, dimension change, suppression, time freeze, or losing the power removes only this ability's modifier and grants a safe fall release.

Fireball now summons one server-owned Cinderheart per caster instead of allowing an uncapped cloud of delayed-task projectiles. Recasting while it hovers pays for tiers two and three; Ancient Mastery unlocks a fourth seal. Punching the heart begins a six-second measured flight, after which only two reflections are permitted, plus one each from Reflective Ward and Ancient Mastery. Current player control is tracked independently from the original caster so reflected kills, consent, safe zones, and lifecycle cleanup remain correct. Tagged amethyst, powered wards, personal forcefields, water, ice, snow, missing controllers, and protected targets all have distinct terminals; water and frost become a reduced no-ignition steam pressure wave. Might adds a consent-safe impact corona. Vanilla explosion grief is never used: the impact carves a bounded rank-scaled scar directly, never removes protected infrastructure, and adds only capped surface fire when the configured terrain policy permits it.

Starfall is now a finite Astral Convergence rather than three simultaneous random bolts. A one-second astrolabe omen reveals the complete field before eight deterministic golden-angle strikes descend six ticks apart. Might adds strikes, damage, radius and consent-safe pressure; Motion leashes the storm eye to the initially aimed body; Insight reveals successfully struck veils; Wardcraft diverts at most 16 hostile projectiles without stealing them; Communion mirrors every third strike at reduced power; and Dominion adds two strikes plus a central crown. Roofs catch the sky path, water conducts a wider reduced pulse, Pure Light amplifies it, Darkness consumes it, and amethyst, safe zones, powered wards, forcefields, time locks and projection bodies retain distinct protections or counter-cues. Every body has repeat and total-hit caps, every search is nearest-first and bounded, and each impact leaves a bounded rank-scaled scar without using harmful vanilla lightning.

Void Beam now opens through a visible 0.6-second server-owned charge, then follows the caster's live aim through up to three ordered bodies. Later penetrations fall to 72% and 52% damage; Empowered Impact and Ancient Mastery each bore through one additional target, while Dark Resurgence deepens the Wither and aftermath pulse. Pure Light, tagged amethyst, powered wards, safe zones, and personal forcefields stop the ray with distinct inversion or fracture ceremonies. An unopposed release leaves a rank-scaled, terrain-safe void scar for up to eight seconds, pulsing against at most 16 nearby permitted targets and carrying collision residue at the real impact point so later light magic can tear it into an eclipsing star rift.

Gravity Displacement anchors a five-second gravitational orrery instead of applying a disposable Levitation effect. Up to 16 nearest permitted bodies are pulled through deterministic, collision-checked orbits and receive a safe Slow Falling release; Empowered Impact and Ancient Mastery each add eight capture slots, while Empowered Impact turns a natural collapse into a bounded damaging shock and Ancient Mastery curves up to 24 hostile projectiles without stealing ownership. Consent and safe zones, amethyst, soul-anchored projection bodies, personal forcefields, powered wards, and time locks resist with distinct ceremonies. Overlapping orreries assign each shared body to the nearer field with hysteresis and a visible violet-cyan resonance handoff, eliminating velocity jitter.

Energy Beam is a two-second live-aim Sunfire channel rather than a disposable ray. After an eight-tick solar focus it releases four server-authoritative beats; consecutive scorches on one body climb through three damage and burn tiers, while changing targets resets the sequence. Water transforms a beat into a bounded steam pressure pulse with no ignition. Ordinary matter, tagged amethyst, Pure Light, Darkness, safe zones, powered wards, and personal forcefields stop the ray through different ceremonies. Empowered Impact may erupt one protected, terrain-safe solar flare after a full scorch sequence; Ancient Mastery forks successful hits to at most two visible nearby bodies without chaining or crossing wards. Death, suppression, power loss, dimension changes, disconnects, and time locks break the channel cleanly.

Breezy Bash is an eighteen-tick, server-owned Tempest Rite rather than a delayed task holding stale entities. It claims the nearest 16 permitted bodies in a true sphere, launches them through collision-checked outward lift, shows their shared apex, and independently revalidates each downward verdict. Consent and safe zones, amethyst, projection bodies, forcefields, powered wards, time locks, ceilings, and an existing gust claim resist with distinct wind fractures; an empty rite refunds its offer. Empowered Impact adds eight claims, deepens the slam, and closes as a terrain-safe pressure corona. Ancient Mastery adds eight claims and curves at most 16 hostile projectiles away without reflecting them or stealing ownership. Every interruption safely releases captured bodies with Slow Falling.

Every power uses the same energy well. Light progression grows it from 250 to 770; darkness progression grows its separate well from 500 to 1,850. Failed casts refund their activation cost. Toggle powers drain once per second and cause backlash if they exhaust the well. Sleeping and runestones restore energy unless Exhaustion is active.

Darkness and Pure Light are living realm matter. Vanilla random ticks make either block convert adjacent breakable terrain without requiring kills or loading distant chunks. Amethyst, unbreakable/server infrastructure, and every authored mindscape palette block contain that conversion; datapacks can extend `#powers:living_force_immune`. Darkness withers nearby living entities without the `darkness` tag at strength III; tagged players instead receive a rank-scaled energy pulse unless amethyst has poisoned their connection. Pure Light mirrors this allegiance: ordinary beings receive particle-hidden Regeneration and energy, while Darkness-tagged beings Wither under its radiance. Dark Resurgence strengthens Darkness affinity by 50%, doubling it at or below one-quarter energy with a distinct eclipse awakening.

Living-force scars can manifest finite patrols. An Eclipse Scar calls up to three temporary Hollowed against nearby outsiders; a Dawn Scar calls Radiant Sentinels against Darkness. The server tracks at most 64 such invaders and inspects at most 64 player anchors per ten-second pulse. A powered amethyst ward surrounded by solid amethyst exactly two blocks north, south, east and west begins a containment ceremony. It crystallises only loaded Darkness/Pure Light through a six-block sphere under one 256-position global tick budget. When Pure Light touches Darkness, a catastrophic eclipse blast damages and throws up to 256 inspected living entities, then radially erases both forces from the epicentre through a 48-block sphere over several server-budgeted ticks without destroying unrelated builds. Even when spreading is disabled, opposed blocks keep retrying their clash check.

## Mind travel and vulnerable bodies

Astral projection, vessel possession, dreamwalking, named-target Teleport, Middleworld travel, and travel to the Light or Dark Realm leave a skin-matched Minecraft mannequin where the player's physical body remains.

- The spirit or mind moves; the body remains loaded and visible.
- Damage to the physical body is mirrored to the real player, so projection never grants invincibility. The remote mind/avatar cannot be damaged a second time.
- A fatal hit first attempts the exact validated return; if return cannot be completed, the player dies and realm confinement owns the respawn.
- A player or mob already participating in one mind session cannot start or become the target of another.
- Death, disconnect, server shutdown, invalid state, or `/powers return` cleans up or restores the session safely.
- Return placement is collision-, border-, ward-, and safe-zone-validated.

The Light Realm and Dark Realm are mindscapes rather than ordinary destinations. Each progressively materialises six persistent, protected sites around the first thought: an Archive, playable Labyrinth, Shrine, remembered Settlement, hazardous Force Font and Herald Court. Construction starts at each loaded centre and spends at most 128 block edits per five-tick pulse, so a fresh realm never freezes the server. Every site has localized lore, an explained energy reward and a route into the title maze. The Light Realm uses a pure white sky without a sun, moon, stars, weather tint, or black void; the Dark Realm remains an enclosed hostile thoughtscape. Middleworld has its own muted dream biome rather than borrowing the Light Realm's appearance.

Force Pressure rises as a mind travels away from the entry. Aligned players regain energy and resistance; intruders lose energy and progressively receive Weakness, Slowness and Wither. Every twelve-minute realm cycle ends with a two-minute **Whiteout** or **Dark Eclipse** that amplifies those effects and covers each observer in a bounded realm-wide rune/spiral ceremony. Once a Herald Court is built, its 1,024-health faction boss manifests under a persistent twenty-minute defeat timer. The Dark Herald hunts outsiders and drops the Shadow Sword; the Light Herald hunts Darkness and drops the Heavenly Partisan. These boss paths make both artifacts obtainable without already owning the artifact they unlock.

The original cosmology names four competing claims. The **Luminous Concord** seeks a perfect pattern through healing or erasure; the **Hollow Court** offers freedom by dissolving identity into Darkness; the **Amethyst Covenant** contains both extremes; and the **Archivists of the Between** preserve memories either force would delete. The First Vessel was their failed attempt to carry both Light and Darkness, while each Herald is a self-aware verdict rather than a god.

Realm departure is intentionally stricter than entry. A player trapped in the Dark Realm cannot leave through gameplay travel until they have the `darkness` tag and darkness level 5; leaving the Light Realm requires level 5 in either progression. `/powers return` is reserved for an ordinary validated detached-body return; operators can use `/powers recover <player>` for the explicit administrative recovery route. Ordinary crystals, teleports, dreamwalking, dimension menus, and Nether/End/gateway blocks cannot bypass the lock. A fatal avatar or physical-body hit uses a separate soul-recall route: the mind is restored to its recorded body first and then ordinary player death proceeds there.

Entering a mindscape therefore never becomes invulnerability: both the avatar and frozen physical body remain valid fatal surfaces, while non-fatal damage and forcefields retain their documented ownership. The realm gate applies to voluntary travel rather than preventing death or corrupting vanilla respawn.

## Rank maze

Advancements determine earned rank depth, innate-power scaling, and energy capacity. Crystals and grimoire spells never inherit innate scaling; artifact-routed powers use only their explicit relic and rank-10 apotheosis profiles. Each of the 23 innate powers has its own authored level-0 to level-10 shape for damage/healing, range, duration, destructive work, capacity, and named transformations; there is no generic perk percentage that can accidentally leak into unrelated equipment magic. At level 10, combat powers reach boss-scale multipliers of up to 8×, utility powers favour range/duration/capacity instead, energy cost is reduced by up to 25%, and cooldown is reduced by up to 30%. The exact capstone table is documented in [`docs/gameplay/innate-levels.md`](docs/gameplay/innate-levels.md).

The light and darkness graphs each contain 28 nodes, including legacy titles and paths through might, motion, insight, wardcraft, veils, communion, and dominion. Converging paths create hybrid titles such as Runeblade, Riftwalker, Soulwarden, Eclipse Weaver, and their endgame forms.

Named branch variants still have mechanical consequences as well as stronger ceremonies: Might empowers selected impacts, Motion grants second steps, Insight grants True Sight, Wardcraft reflects forcefields, Communion strengthens soul transfer, Veil reduces readable residue, Dominion deepens ancient mastery, and the Darkness path awakens Dark Resurgence. Authored rank breakpoints also unlock the relevant transformations at levels 3, 6, 9, and 10, so every player becomes materially stronger even before choosing a maze focus.

Two aligned players who release the same **innate** power within two seconds and twelve blocks form a cooperative **Radiant Concord** or **Umbral Concord**. Both recover 20% of their energy capacity and gain ten seconds of Absorption V and Resistance II; a bounded ten-block pulse deals 48 magic damage to at most 24 opposed-faction bodies. The same pair cannot concord again for ten seconds. Spells, crystals and artifact-routed casts never enter this system.

Press `B` (rebindable) to open the synchronized Rank Maze, inspect every connected title and its real transformation, awaken reachable nodes, or attune a previously earned title. Light uses a pale carved-stone/parchment panel with gold-white runes; Darkness uses ancient blackstone, violet tendrils, and worn silver edging. The server revalidates every click. The equivalent commands are `/powers path list`, `/powers path unlock <node>`, `/powers path focus <node>`, and `/powers path respec`; respeccing preserves earned depth and costs 30 experience levels by default.

Players with the `darkness` entity tag use the darkness advancement track and energy well. `/powers darkprefix` controls whether the focused darkness title is publicly shown.

Only the applicable advancement background and track is visible: radiant players cannot see the darkness rites and darkness-infected players cannot see the radiant mastery track. Focused titles are synchronized into the player's display-name, tab-list, and chat prefix rather than being client-only decoration.

Normal mastery is cumulative and intentionally long-form: levels 1-10 require respectively 100/300/750/1,500/2,500/4,000/6,000/8,500/12,000/18,000 successful innate casts and 10/50/150/300/500/800/1,200/1,800/2,500/4,000 power kills. Levels 4-10 additionally require 1-6 Light Realm memories, while levels 5-10 require 1/2/4/7/12/25 boss kills. Vanilla bosses count, and modded living bosses with at least 200 maximum health count so progression remains compatible with large content mods.

Darkness progression records permanent atrocities rather than trivial inventory checks. Levels 1-2 require 25 and 100 passive kills. From level 3 onward the cumulative demands become increasingly cruel, culminating at level 10 in 500 villagers, 500 wolves, 100 baby villagers, and 50 iron golems. Deeds cannot be undone by dropping items, dying, or switching advancements.

## Grimoires and rituals

Sneak-use a grimoire to turn its pages; use it normally to cast the selected spell. Costs, cooldowns, and channels are fixed equipment values and never inherit player rank. Channeled rituals lock their entity or block target when the channel starts, so looking elsewhere cannot silently retarget the release. They break if the caster moves, takes damage, changes books or dimensions, dies, loses range or line of sight, loses the target, or becomes amethyst-dampened, and return half of the offered energy.

| Grimoire spell | Cost / cooldown / channel | Complete effect |
| --- | --- | --- |
| Celestial: Soul Compass | 14 / 10s / instant | Opens the authenticated locator screen. It accepts an online player or the unique custom name of one loaded mob, rejects ambiguous or incomplete world scans, enforces player locator consent, reveals the result, then begins a one-minute vulnerable-body camera view through that target. |
| Celestial: Augury of the Living Sky | 16 / 30s / 1s | Reads the current weather, moon phase, nearby Darkness/Pure Light pressure, and the time remaining before the next realm event. |
| Celestial: Cartographer's Star | 24 / 60s / instant | Opens an authenticated structure/biome/realm-landmark search. Searches are parsed strictly, bounded to 64 chunks for structures or 4,096 blocks for biomes, start only for loaded registry IDs, and report registry ID, dimension, coordinates, distance, and compass direction without forcing chunks. |
| Celestial: Heavenfall — Celestial Ruin | 100 / 60m / 10s | Locks a ground point and starts an irreversible one-minute warning. A pulsing 100-block-diameter sky beam is visible and audible across 6,000 blocks even after players leave; countdown and exact ruin-wave cursor survive server restarts. The strike holds an opaque white flash for three seconds before fading across a 20-second whiteout, sustains tinnitus, deals up to 50,000 custom damage with quadratic falloff and extreme knockback across 6,000 blocks, excavates a 120-block-radius crater, and sends 96 loaded-chunk fire-scar rays outward. Only the 19×19 detonation area is progressively ticketed shortly before impact. |
| Deep: Dimensional Anchor | 22 / 60s / 2s | Anchors a consent-valid targeted player or player-compatible test actor against dimensional and teleport movement. |
| Blight: Blood Reading | 12 / 10s / 1s | Reads a living target's health, maximum health, armour, alignment, and active effect registry IDs; player consent and all normal protections remain authoritative. |
| Blight: Grave Recall | 10 / 10s / instant | Reports only the exact dimension and block coordinates of the caster's most recently recorded death. It does not teleport, recover items, or create a second resource. |
| Wild: Purification Circle — Cleansing Rain | 20 / 30s / 2.5s | Cleans harmful effects except amethyst poisoning, heals allies, clears dimensional anchors, and severs soul links inside an eight-block circle. |
| Wild: Verdant Tending | 22 / 30s / 2s | Boundedly grows crops and plants, hydrates farmland, and extinguishes fire inside the protected local tending area. |
| Wild: Hearth Sanctuary | 28 / 50s / 2s | Gives the caster and every living entity within a strict three-block radius an independent 40-integrity magical forcefield. Each ward remains until broken and sacrificially absorbs an overkill hit in full. |
| Abyssal: Ward-Breaking Ritual — Amethyst Unmaking | 26 / 60s / 4s | Locks the aimed powered Amethyst Ward and suppresses it for 45 seconds without destroying the structure. |
| Abyssal: Dispel — Unweaving | 18 / 25s / 1s | Dispels the nearest field and/or clears non-amethyst effects and a dimensional anchor from the locked permitted target. |

Insight-ranked True Sight can pierce either mindscape's normal path/rank veil during a consented Soul Compass ritual, signalled by a cyan-gold third-eye glyph, but cannot bypass consent. Dimensional Anchor is exclusively the Deep Grimoire's one spell. The Infernal item IDs remain hidden, inert compatibility aliases for old saves and expose no active school. All twelve active spells use only the existing magic-energy pool; there is no essence, ritual-amplification, or secondary-resource system.

## Crystals

Every crystal has a bound ability or selectable convergence. Sneak-use turns a multi-mode crystal; normal use releases its selected force.

| Crystal | Ability |
| --- | --- |
| Red | Inferno creates an eight-second, 12-block firestorm that repeatedly damages and ignites permitted targets without a vanilla terrain explosion. |
| Orange | Clone Swarm creates three unarmed, player-skinned 80-health combat echoes for 60 seconds; they follow, teleport and fight for their owner without copying equipment. Creativity Manifestation builds a fixed protected orange-concrete, orange-glass and glowstone creation chamber at the aimed point when terrain policy allows. |
| Yellow | Size Shift alternates a 20-second `0.0625×` miniature form and an immense `10×` titan form, with corresponding speed/jump or strength/resistance/knockback changes. This fixed crystal rite remains separate from the freely selectable innate Size Morphing power. |
| Green | Life Bloom fully heals and cleanses living allies in a 20-block radius. |
| Blue | Chrono Stop claims Minecraft's true global server tick freeze and may be toggled off by using the crystal again; it releases automatically after one minute and has no ongoing energy drain. Dreamwalking controls one uniquely named, consented player or mob through the authenticated possession channel for up to 30 seconds while leaving a vulnerable body. |
| Indigo | Middleworld opens vulnerable, persisted mind travel to the muted dream dimension; using it again returns to the exact physical origin when travel policy allows. |
| Violet | Soul Link binds up to eight nearby souls for ten seconds and mirrors a bounded share of later wounds across survivors. |
| Rainbow | Sixfold convergence of the chromatic combat forces. In a darkness-tagged holder's hands its same item model becomes visibly corrupted; no duplicate infected gameplay item is required. |
| Light | Aims at a consented player or defaults to the wielder, leaves a vulnerable body, asynchronously loads the destination, and enters the Light Realm beneath a white celestial storm. Crouch-use takes the caster and up to 15 eligible, consented players within two blocks, each to a distinct safe arrival. |
| Dark | Aims at a consented player or defaults to the wielder, leaves a vulnerable body, asynchronously loads the destination, and enters the Dark Realm beneath a corrupted storm. Crouch-use takes the caster and up to 15 eligible, consented players within two blocks, each to a distinct safe arrival. |

Convergence cooldowns are shared with their underlying forces, so swapping crystals cannot bypass a rare ability's recharge. Crystal crafting recipes are deliberately not included; they are reserved for later progression design. The resource validator prevents accidental crystal recipes from entering a release.

## Shadow Sword

The item whose compatibility identifier remains `powers:lycanbane` is presented everywhere as the **Shadow Sword** in bold dark grey. It is ancient Pure Darkness made solid.

- A non-darkness carrier is struck with particle-free Blindness and Wither, cannot use the sword, and provokes up to four nearby Darkness Creatures through lightning-marked protection summons.
- A darkness-tagged wielder regenerates 50-250 darkness energy each second according to rank; level 10 receives a 900-energy apotheosis refill pulse.
- Right-click casts the selected action. Crouch-right-click opens a non-pausing eight-segment combat wheel. Every segment shows its glyph, abbreviated name, energy cost, cooldown progress, active/locked state, and relevant variant. Hover and release crouch, click, or press `1`–`8` to bind a favourite; crouch-scroll cycles the same persistent loadout without opening a screen. The centre opens a searchable icon library with loadout-ordered Favourites, Innate, Crystals, and Sword tabs, live energy/cooldown/toggle/lock state, and a contextual Size Morph control.
- Every innate and underlying crystal action is available from rank 1. Existing actions retain their mechanics but receive a black/violet corruption of their own original colour, darker sounds, and evil residue when routed through the sword.
- At darkness level 10, every action cast through the sword ignores existing cooldowns and starts no new cooldown. Energy, target validation, amethyst, sanctuary, safe-zone, and bounded-entity protections still apply.

Exactly three actions are unique to the Shadow Sword. Retired saved selections migrate to the nearest surviving action; their strongest motifs remain as corrupted presentations rather than duplicate mechanics.

| Invocation | Rank | Effect |
| --- | ---: | --- |
| Call the Hollowed | 1 | Summons a bounded squad of owner-aligned Darkness Creatures beneath lightning-marked seals. |
| Blight Ground | 1 | Queues a protected six-block Darkness conversion beneath the wielder; fluids, block entities, unbreakable blocks, safe zones, and living-force immunity remain intact. |
| Nightfall Dominion | 10 | Toggles Strength X, Resistance IV, Regeneration V, Fire Resistance, Speed IV, and a 24-block Wither pressure aura. |

## Heavenly Partisan

The **Heavenly Partisan** is the unbreakable Pure Light counterpart. Only non-dark players may wield it; a darkness-infected carrier is judged with hidden Glowing/radiant damage and guarded by lightning-arriving Radiant Sentinels. It deliberately has a smaller curated roster than the Shadow Sword: Flight, Starfall, Lightning Strike, Thunderclap, Energy Beam, Forcefield, Plant Healing Acceleration, Double Health, Creativity Manifestation, Life Bloom, and Light-realm travel, all with gold-white presentation. Normal rank 10 reduces Partisan cooldowns by 60%, increases its regeneration aura, and strengthens its support effects; unlike darkness apotheosis, it does not remove cooldowns.

| Partisan rite | Rank | Effect |
| --- | ---: | --- |
| Call the Radiant | 1 | Summons a bounded squad of Radiant Sentinels. |
| Consecrate Ground | 2 | Queues a six-block protected Pure Light conversion. |
| Covenant Chain | 4 | Gives an ally 30 seconds of regeneration/absorption and bounded damage sharing, or binds an opposed hostile. Each owner is capped at eight allies. |
| Daybreak Wave | 5 | Damages darkness hostiles, purifies removable harmful effects, heals others, and clears unprotected projectiles. |
| Heaven Gate | 6 | Opens one owner-only pair of temporary radiant gates. |
| Solar Firmament | 8 | Opens an eight-second field that repels hostiles/projectiles and heals light-aligned occupants. |
| Second Dawn | 9 | Arms one five-minute, single-use radiant death ward. |
| Host of Heaven | 10 | Calls two elite sentinels, a radiant dominion field, consecrated ground, and a cosmic heaven-beam ceremony. |

## Runestones

Runestones are reusable energy focuses, stack to 16, show a small actionbar result, and cannot work through Exhaustion. Their authored tier restores between 40 and 600 energy; recharge ranges from three to fifteen seconds. Inert runestones have a natural recipe, and dark tiny/small/medium/large runestones form a craftable upgrade chain restoring 60/125/250/400 energy. Additional inscribed and bound variants restore up to 600. Dungeon, abandoned-mineshaft, and ancient-city chests can supply the natural tiers without replacing vanilla loot.

## Imported relic families

Formerly decorative imported relics now have bounded server-owned roles:

- Rings and amulets are inventory attunements with distinct one-to-three-energy recovery weights. Their combined recovery caps at six energy per second and their particle-free Resistance caps at level II, so duplicate stacks cannot create unbounded work or strength.
- Soulstones and the Soul Matrix are persistent auxiliary energy reservoirs with 200/400/800/1,600 capacity. Active variants begin charged, inert variants begin empty, normal use withdraws up to 100 energy, and sneak-use deposits up to 100. If the player's pool cannot fund a cast, carried reservoirs pay the exact shortfall atomically in inventory order; an insufficient aggregate balance changes nothing.
- The Ritual Dagger directly sacrifices four health to restore 80 existing magic energy. It never creates essence and cannot be made free by armour, forcefields, spawn invulnerability, or cancelled incoming damage.
- The Living Heart heals and regenerates; the Heart of the Wildwood heals more strongly; the Ghoul Heart trades weaker healing for active and passive energy; the Clockwork Heart raises a timed absorption ward; and the Bloodstone arms one five-minute lethal-damage ward that is consumed when it saves the wielder.
- The Philosopher's Stone performs controlled, energy-priced transmutation of stone, deepslate, netherrack, or end stone. It refuses protected blocks and block entities.
- Use a Lodestone relic on a safe destination to bind its dimension and coordinates. A Miniportal carries two visible charges and asynchronously returns to that same-dimension anchor through the normal border, realm, ward, anchor, collision, and hazard policy; a charge is committed only after successful arrival. Drop an empty Miniportal beside one amethyst shard to restore both charges. The First Vessel always drops one, and ruined portals can rarely contain another.
- The Flute recalls, heals, and rebinds nearby player-shaped guardians to its wielder under the normal guardian caps.
- The Empyrean Jewel pays one fixed 40-energy surcharge to override every player-consent gate—teleport/forced movement, locator, companion transport, Dreamwalking, and possession—without ever bypassing safe zones or protections. Duplicate jewels do not stack.
- Malignember reduces the existing energy cost of explicit destructive magic by 20%, never below one and never through rank scaling. Celestial focuses add one bounded passive-energy pulse. Blood/Sacred/Soul Arcane Energy Dust, blood salts, fossils, jewels, stones, ritual vessels, and archaeology reagents contribute documented Arcane Crucible XP tiers. Tattered pages and remaining lore relics become contextual clues that Shadow can decipher rather than fake weapons.

Representative relics are injected additively into dungeon, pyramid, temple, mansion, ruined-portal, fortress, buried-treasure, stronghold, ancient-city, bastion, and end-city loot. Their variants share the same family behavior. Crystals and deliberately deferred story items remain recipe-less.

The exhaustive purpose and acquisition status of every registered gameplay item is in [`docs/gameplay/item-catalogue.md`](docs/gameplay/item-catalogue.md). Hidden compatibility aliases are listed there explicitly instead of masquerading as obtainable content.

## Fantasy weapon archetypes

Every non-mythic fantasy weapon is a real survival item rather than an identical texture swap. Its name assigns one of twelve visible archetypes with a distinct damage/swing profile and a particle-hidden, cooldown-bounded on-hit proc: Frostbound slows/weakens; Quicksteel grants speed; Reaper executes wounded targets; Crusher launches/weakens; Berserker adds damage/strength; Arcane returns energy; Vital heals; Radiant burns/reveals; Abyssal withers/lifesteals; Guardian shields; Hunter marks/poisons; and Piercer adds armour-ignoring magic damage. Amethyst and protected zones suppress these procs.

Every ordinary weapon appears in exactly one low-chance additive survival loot family across shipwreck treasure, igloos, wither skeletons, village smiths, outposts, evokers, witches, underwater ruins and vindicators. The Shadow Sword and Heavenly Partisan are excluded from these pools and remain the guaranteed Herald trophies described above. Any ordinary weapon can still enter the Arcane Crucible alignment path.

## Shadow knowledge

The custom Knowledge Book interface, network protocol, advancement, and loot injection have been removed; vanilla Knowledge Books retain vanilla behaviour. A darkness-tagged Shadow Sword wielder instead addresses their Shadow through ordinary chat beginning exactly with `shadow, …`. Shadow answers offline from datapack `powers:knowledge_entries`, loaded item/block/entity registries, verified loaded recipe IDs, progression visibility, and redacted context for the held item, targeted block/entity, biome, and dimension. It admits uncertainty and never invents recipes. The optional OpenAI-compatible endpoint is disabled by default and may answer only low-confidence non-recipe questions; it receives the chosen question and bounded registry context, never identity, coordinates, chat history, IPs, secrets, or raw world data. HTTPS/loopback validation, timeouts, concurrency, cooldowns, and offline fallback remain mandatory.

## Arcane Crucible

The Arcane Crucible is a two-input, server-owned weapon forge. Put one eligible base weapon in the left slot and one catalyst in the right, choose a server-advertised result, and press **Transmute**. Its versioned mutation lock, one atomic commit, hopper rules, break recovery, full-inventory drop fallback, and stale-choice validation prevent duplicated or lost results.

Its three-stage path is:

1. A Darkness or Pure Light block converts a tagged ordinary weapon into one of the registered non-mythic alignment peers while preserving safe components, enchantments, custom name, lore, repair cost, and proportional durability.
2. An Animated Artifact Star binds zero-cooldown alignment lightning without replacing the weapon. Each right-click still needs a valid aimed target, energy, line of sight, same-tick rate allowance, and every normal protection check.
3. A runestone infuses a star-bound weapon with 25/75/225/675 XP according to tier. Authored archaeology and spell-school reagents provide bounded 35–175 XP alternatives. Levels are derived from overflow-safe exponential thresholds and cap at 30; lightning damage grows to a 1,200 non-player cap and a 120 player cap.

Datapacks can add base weapons to `#powers:arcane_crucible_base_weapons`. Java integrations can call `CrucibleEligibility.registerBaseWeapon(Predicate<ItemStack>)` or `registerExclusion(...)`. The Shadow Sword, Heavenly Partisan, stacks with mythic identity, and already converted weapons are hard exclusions even if another pack tags them. Crystals and story artifacts still receive no recipe.

## Shadow companion

A darkness-tagged player carrying the Shadow Sword may press `G`, say `shadow, come to me`, or ask any `shadow, …` question to call Shadow explicitly. Merely holding the sword never spawns it. Hidden Shadow is an owner-only collisionless player apparition. `shadow, reveal yourself` replaces that apparition with one globally tracked, skin-matched player-model body: it carries no item or armour, follows at 5 Hz to a rear-side doglike position, and teleports through a custom eclipse effect beyond 12 blocks. The revealed body has no equipment or independent inventory and creates no chunk ticket, but it is deliberately mortal. Killing it collapses the manifestation without erasing player-keyed lore or failed-cast memories; an eligible wielder may call it back as if nothing changed.

Shadow is private by default. `shadow, reveal yourself` makes both its physical manifestation and replies global; the body is naturally visible only to observers sharing its dimension. `shadow, hide yourself` removes the mortal body, restores the owner's private apparition and makes replies owner-only. `shadow, leave me` dismisses it completely. Death, projection, artifact loss, logout and server stop also dismiss the current manifestation. All addressed messages are removed from ordinary signed chat and answered as clearly attributed server messages; unrelated chat remains untouched.

Shadow keeps the latest 16 server-authoritative magic attempts for five minutes. Asking why an action failed returns the exact recorded cause—including required and available energy, cooldown time, target failure, rank/alignment lock, consent, amethyst, safe-zone, range, line-of-sight, channel, dimension, or server-budget refusal—before general lore is considered. The journal stores immutable action/reason facts only, never coordinates, target identities, entity references, or chat history; it is erased on logout and server stop. Three identical failures can produce one rate-limited private prompt suggesting that the player ask Shadow for help.

An optional OpenAI-compatible text endpoint may improve only the surrounding wording. It is disabled by default, never receives UUIDs, names, chat, IPs, coordinates, or secrets, cannot change gameplay, and must preserve Shadow's authoritative diagnosis verbatim. Credentials come only from the configured environment-variable name. Requests are asynchronous, bounded per owner and globally, limited to 2.5 seconds and 1,024 response characters, and always fall back to the offline answer.

## Player-like entities

`Darkness Creature` is a naturally spawning Dark Realm monster with a completely black player model, 100 health, 12 armour, 16 melee damage, and player-like movement. It attacks every living entity without the `darkness` tag and alternates bounded, terrain-safe lightning and custom darkness fireballs at range. Sword guardians use the same creature and disappear naturally rather than accumulating permanent server entities.

`Power Test Actor` is a player-model test opponent with the same boss-capable base attributes. Every actor has a persistent, visible username (`Test_<id>` by default); name tags and `/powers testing actor spawn <username>` assign a normalized username. Player-target powers can resolve that unique name for remote viewing and Teleport, and the actor supplies player-like energy, dimensional-anchor and sacrificial-forcefield state while preserving ordinary safe-zone, amethyst, ward, death and collision rules. This makes possession, drains, forced movement, projectile counters and particle presentation testable in a single-player world. Self-contained custom spawn eggs for the actor, Darkness Creature, Radiant Sentinel, and First Vessel are available in creative/operator testing.

`Radiant Sentinel` is the Partisan's light-aligned player-shaped guardian. Both guardian factions preserve owner and alignment, expire when their owner leaves the dimension, enforce four-normal/two-elite owner caps and a global cap, and cycle bounded melee, lightning, fireball, and alignment-field tactics. Their fields heal allies or punish the opposed faction without crossing safe zones or amethyst counterplay.

## First Vessel boss

The **First Vessel** is an original player-shaped ancient boss that never spawns naturally. It has a 5,000-point virtual vitality layer, 16 armour, 16 toughness, 36 attack damage, 0.33 movement speed, 0.8 knockback resistance, persistent boss-bar state, original skin/sounds, a spawn egg, loot and advancement hooks. When the first eligible players approach, vitality snapshots `1 + 0.55 × (players − 1)` and caps at four times base health; it never shrinks mid-fight.

Its tactical planner evaluates at most 24 candidates per ten-tick decision, with separate action cooldowns and facts for distance, line of sight, verticality, player clusters, projectiles, health, movement, cover, protection, and repetition. Its catalogue is tested directly against all 23 innate powers, and every adapter is entity-safe—no player-only handler is cast onto the boss. At intervals it may also mirror a power observed in the target's current loadout.

The encounter has three named states: **Waking Vessel** (`AWAKENING`, 100–70%), **Broken Constellation** (`UNBOUND`, 70–35%), and **Crownless God** (`LAST_COVENANT`, below 35%). Phase changes perform Sevenfold Step. At half health it attempts one five-second Vessel Reconstitution; eight percent maximum-health damage, amethyst, or a light dominion interrupts it. Later phases gain projectile-consuming World-Suture, and Crownless God releases one terrain-safe Last Firmament below 15%. Safe zones, magical forcefields, amethyst, and bounded candidate limits remain valid boss counterplay.

Operators may use `/powers boss spawn`. The survival ritual requires a darkness-tagged level-10 player to sneak-use the Shadow Sword on an Arcane Crucible surrounded by Darkness at the four cardinal positions three blocks away and Pure Light at the four diagonals two blocks away. A successful invocation consumes the eight anchors and creates one boss; an existing nearby First Vessel blocks another ritual.

## Counterplay and server safety

- Amethyst items, tagged amethyst blocks, and powered Amethyst Wards suppress powers without making normal melee damage harmless.
- Player-targeted teleportation, locating, companion travel, dreamwalking, and possession have per-player consent controls.
- Safe zones can block power harm, hostile movement, and terrain damage.
- Rank-scaled combat terrain scars are enabled by default; safe zones, protected realm matter,
  amethyst, indestructible blocks, block entities, and the administrator override remain protected.
- Teleports validate world borders, loaded-distance limits, collisions, floors, wards, anchors, and destination dimensions.
- Temporary entities are marked ephemeral and excluded from saves.
- Innate Time Freeze and the Blue Crystal's Chrono Stop share guarded `/tick freeze` ownership and safe restoration. Chrono Stop is global, toggleable, and automatically releases after one minute.
- Particles use a server-wide per-tick budget; amethyst scanning and state network syncs are cached.
- Serverbound activation, selection, travel, rank, artifact, locator, and ritual packets each have independent per-player rate lanes, so one noisy client cannot monopolize the server thread or starve ordinary play.
- Custom food and mob drops are injected additively instead of replacing vanilla loot tables.

## Magic collisions and presentation

The 23 assignable powers have 23 innate action identities. Player Size Morphing keeps its legacy saved power identifier but uses the distinct `size_morph` collision identity; the Yellow Crystal retains its fixed-strength `size_shift` rite and never inherits innate rank scaling. Those 23 innate actions, 12 practical grimoire spells, 11 crystal actions, 13 artifact actions (three Shadow Sword rites, eight Partisan rites, and two star-bound lightning alignments), three amethyst suppressors, and two living realm forces form a 64-action collision system. Every one of the 2,080 unordered same-or-cross-action pairs, including same-action resonance, has a deterministic outcome, potency/duration/range adjustment, accessible shape cue, and semantic sound cue. Named high-impact combinations add mechanics such as steam pressure, eclipses that reveal concealment, realm-matter annihilation, projectile-consuming star rifts, soul-link purification, finite ward fracture, grounded storms, hostile pressure waves, concordant healing, and a physical Sunfire/Void ray collision with a pressure blast, caster lightning omens and short celestial ring.

The complete inventory and resolution order are in [`docs/interactions/system-catalogue.md`](docs/interactions/system-catalogue.md). Every action pair is listed in [`docs/interactions/interaction-matrix.csv`](docs/interactions/interaction-matrix.csv), while all 672 combinations of eight active forms, six cast sources and fourteen termination events are listed in [`docs/interactions/lifecycle-matrix.csv`](docs/interactions/lifecycle-matrix.csv). Every successful innate, crystal, grimoire, Shadow Sword, and Heavenly Partisan cast receives a signature-driven anticipation, release, impact, and aftermath ceremony in addition to its bespoke gameplay effects. Flame fractures, frost shards, storms fork, time spirals, space and soul tether, life roots, darkness eclipses, and light forms celestial crowns; the server chooses the matching authored sound and both signature colours. Ritual glyphs form on the ground beneath the caster, while vertical glyph, eclipse, and lightning sigils face each observer locally instead of becoming edge-on. Ceremony radius, density, motion, volume, and pitch intensify at rank depths 4 and 8, with another bounded step for Ancient Mastery. Client effects use eight authored particle sprites, 13 original mono Vorbis sounds, distance culling, reduced-motion geometry and velocity clamps, and hard client/server particle budgets.

## Commands

Player commands:

- `/powers slots`
- `/powers consent <teleport|locator|companion|dreamwalk|possession> <allow|deny>`
- `/powers return`
- `/powers path list`
- `/powers path unlock <node>`
- `/powers path focus <node>`
- `/powers path respec`
- `/powers darkprefix [true|false]`

Administrative commands:

- `/powers list`
- `/powers slots <player>`
- `/powers assign <player> <power> <slot>` where slot is `0`, `1`, or `2`
- `/powers reroll [player]`
- `/powers travel <dimension>`
- `/powers boss spawn`
- `/powers diagnose`
- `/powers reload`
- `/powers testing on|off|status`
- `/powers testing energy on|off`
- `/powers testing cooldowns on|off`
- `/powers testing refill`
- `/powers testing reset`
- `/powers testing actor spawn [username]`
- `/powers testing arena [spawn|clear]`
- `/powers testing coverage`

Testing mode is operator-only and affects only its executor. `on` temporarily disables both energy limits and gameplay cooldowns; the narrower branches change one limit, `refill` restores energy and clears saved cooldowns, and `reset` returns both overrides to normal. It covers innate powers, spells, crystals, artifacts, runes and ongoing energy drains at their shared server-authoritative boundaries. It does not bypass amethyst, protection, permissions, damage, targeting, realm gates or Time Stop. The switches are session-only and clear on disconnect/server stop; run `/powers testing reset` before returning to ordinary survival testing. `/powers diagnose` includes the executor's current testing flags.

`/powers testing arena` replaces only nearby entities tagged as POWERS acceptance targets, then creates seven stationary, named targets: neutral/radiant/dark test actors, a zombie, an iron golem, a Hollowed and a Radiant Sentinel. `/powers testing coverage` prints the registry-synchronized acceptance inventory so a newly registered action cannot silently disappear from the verification catalogue.

## Configuration

The server creates `config/powers.json`. `/powers reload` applies valid changes without a restart and retains the previous configuration if parsing fails.
Legacy schema-v1 files are rewritten atomically as schema v2. This one-time migration changes the old generated `allowTerrainDamage: false` default to `true`; after migration, an administrator's explicit v2 `false` remains authoritative.

Important defaults:

| Setting | Default | Purpose |
| --- | ---: | --- |
| `allowTerrainDamage` | `true` | Allows bounded, rank-scaled combat scars and transformations; set false to suppress ordinary power terrain damage |
| `allowBlockEntityDamage` | `false` | Allows powers to affect block entities |
| `hostileForcedMovement` | `false` | Lets hostile movement bypass normal consent rules |
| `require*Consent` | `true` | Enables each multiplayer consent boundary |
| `projectionBodiesVulnerable` | `true` | Mirrors mannequin-body damage to projected players |
| `persistCooldowns` | `true` | Saves cooldown deadlines across reconnects |
| `celestialRuinTerrainDamage` | `true` | Lets Heavenfall erase ordinary terrain; Darkness and Pure Light are purged regardless |
| `celestialRuinBlockEntityDamage` | `true` | Lets Heavenfall erase block entities when catastrophic terrain damage is enabled |
| `wardRadius` | `20` | Powered Amethyst Ward range |
| `maxParticlesPerTick` | `512` | Server-wide effect budget |
| `teleportMaxChunkDistance` | `8` | Maximum radius, in chunks, for the spectator marking phase around its target; coordinate-menu travel itself can asynchronously load any in-border destination |
| `rankRespecExperienceLevels` | `30` | Rank-maze respec price |
| `adminPermissionLevel` | `2` | Permission level required by administrative commands |
| `livingForces.spreadingEnabled` | `true` | Enables random-tick Darkness and Pure Light conversion |
| `livingForces.spreadAttempts` | `2` | Face-adjacent conversion attempts per selected random tick |
| `livingForces.auraRadius` | `8` | Darkness affinity range around indexed blocks |
| `livingForces.energyRefillPerSecond` | `24` | Base darkness-tag energy pulse before rank scaling |
| `livingForces.clashRadius` | `48` | Realm-matter annihilation sphere radius |
| `livingForces.clashChecksPerTick` | `4096` | Maximum in-sphere clash positions processed per tick |
| `safeZones` | `[]` | Protected dimension-centred spheres |
| `dialogueProvider.enabled` | `false` | Enables optional remote boss dialogue and low-confidence, non-recipe Shadow answers |
| `dialogueProvider.endpoint` | `""` | HTTPS or loopback HTTP OpenAI-compatible endpoint |
| `dialogueProvider.model` | `""` | Provider model identifier |
| `dialogueProvider.credentialEnvironmentVariable` | `POWERS_DIALOGUE_API_KEY` | Name of the server environment variable containing the credential |
| `dialogueProvider.timeoutMillis` | `2500` | Bounded provider timeout, clamped to 250-2,500 ms |
| `dialogueProvider.maxGlobalRequests` | `4` | Global in-flight request cap, clamped to 1-4 |
| `dialogueProvider.ownerCooldownSeconds` | `30` | Per-owner request cooldown, clamped to 10-3,600 seconds |

A safe-zone entry has `dimension`, `x`, `y`, `z`, and `radius` fields.

## Performance and compatibility

All casts, packets, scans, summons, and persistent world actions are server-authoritative and bounded. Candidate searches sort only capped nearby sets; named living entities, wards, force blocks, fields, presences, and proxies use dimension/chunk indices with bounded fallback work; particle emission uses the configured global budget; ritual block edits and living-force clashes resume through rotating per-tick cursors; temporary chunk tickets have deadlines; guardian, storm, field, gate, companion, and boss planners have per-owner/global caps or fixed tick intervals. No power waits for network I/O on the server thread. Serverbound names and action identifiers have codec length limits in addition to rate lanes and semantic validation. `/powers diagnose` reports the live work counters needed to spot a leaking field, proxy, celestial event, ticket, or visual budget.

POWERS treats other mods' living bosses generically, counting any living entity with at least 200 maximum health for progression and using capped percentage or absolute damage where a player-only energy mechanic is unavailable. Datapacks can extend living-force immunity and Arcane Crucible eligibility through the documented tags/API. Safe zones, Fabric events, vanilla damage sources, block hardness, block entities, world borders, dimension identifiers, consent, and claims configured through the POWERS API remain authoritative; a third-party mod that bypasses vanilla/Fabric damage or movement hooks may need a dedicated compatibility adapter.

For troubleshooting, first run `/powers reload` and inspect the server log for a retained-invalid configuration warning. Verify that the same mod version and Fabric API are installed on client and server, that Java 25 is active, and that custom dimensions are included in the server's datapack registry. An under-ranked player inside a mindscape is intentionally unable to escape without another player or operator assistance; this is not a travel failure.

## Building and verification

```bash
./test.sh doctor
./test.sh check
./test.sh gametest
./test.sh soak
```

The launcher resolves Java 25 from `POWERS_JAVA_HOME`, a valid `JAVA_HOME`, Minecraft's bundled macOS runtime, Homebrew, or `PATH`. `check` includes 644 deterministic JUnit cases plus strict validation of JSON, duplicate keys, PNG headers/alpha/dimensions, Ogg/Vorbis streams, particle and sound references, models, translations, namespace safety, dimension biomes, release metadata, production-source reachability, exhaustive interaction/lifecycle-document drift, every production Java source, all non-item assets, and intentionally absent crystal recipes. `gametest` boots a real Fabric server and runs 55 registered live tests covering critical combat, artifacts, travel, bodies, creatures, realm forces, progression, testing tools, catastrophic magic, crystal travel, beam collisions, detached-avatar and physical-proxy death, vessel wrath, Shadow mortality, and the historical Cinderheart block-impact crash. `soak` exercises the actual spatial indexes, rotating queues, physical ray caps, lifecycle cleanup and global budgets for deterministic 10/50/100-player workloads. The release JAR is written to `build/libs/`.

Automated tests validate rules, registries, resources, packet bounds, lifecycle cleanup, and dedicated-server behavior. A final manual playtest is still appropriate before deploying to a valuable multiplayer world, especially for subjective particle density, HUD scale at a player's chosen GUI scale, controls alongside other mods, and catastrophic terrain settings. Back up the world before enabling Celestial Ruin terrain damage or placing opposed living realm matter.

For manual development runs, use `./test.sh client` or `./test.sh server`. The current source of architectural and historical truth is indexed in [`docs/development/history.md`](docs/development/history.md); superseded implementation drafts remain available in Git history rather than cluttering the release tree.

## Licence

The source code is available under the MIT licence. See [LICENSE](LICENSE).
