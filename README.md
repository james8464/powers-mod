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

On first join, a player receives three distinct powers from a pool of 28. They persist with the player. The default keys are `V`, `X`, and `C`; they can be rebound in Minecraft's Controls screen. Crouch while pressing a power key to open that power's option menu when it has selectable modes.

Innate loadouts are allegiance-aware. Radiant players can uniquely receive Starfall, Cozy Campfire, and Plant and Healing Acceleration; darkness-infected players can uniquely receive Shadow Step, Void Beam, and Energy Drain. Every random loadout guarantees at least one matching exclusive power. If allegiance changes, compatible powers remain in place while forbidden slots are migrated without duplicates. Crystal, grimoire, and Shadow Sword access is unaffected; the sword deliberately bypasses innate allegiance locks.

The HUD shows the three powers as compact 30-pixel rune medallions at the right edge, with live toggle and rank-adjusted cooldown states. Energy is rendered as ten separate nine-pixel symbols directly above and exactly aligned with the vanilla hunger bar. Full, half, and empty symbols use normal, darkness, amethyst-poisoned, or projected-body colours; the bar is moved with the vanilla survival HUD so extra heart rows never overlap it.

The assignable powers are:

- Movement and control: Size Morphing, Flight, Super Speed, Speed Burst, Shadow Step, Time Shift, Telekinesis, Gravity Displacement, and Breezy Bash.
- Offence: Fireball, Lightning Strike, Thunderclap, Energy Beam, Void Beam, Frost Nova, Ice Manipulation, Starfall, Ground Slam, Elemental Blast, and Energy Drain.
- Defence and support: Forcefield, Cozy Campfire, Plant and Healing Acceleration, and Double Health.
- Time and mind: Time Freeze, Invisibility, Vessel Possession, and Astral Projection.

| Innate power | What it does |
| --- | --- |
| Size Morphing | Toggles between `0.25x`, `0.5x`, `0.75x`, `1x`, `1.25x`, `1.5x`, `1.75x`, and `2x`. Distance from normal size determines the per-second energy drain. |
| Time Shift | Opens a server-advertised dimension/coordinate menu and asynchronously loads, validates, and enters even currently unloaded destination chunks. It can also mark another player with consent while a vulnerable body remains behind. |
| Shadow Step | Performs a short, collision-safe line-of-sight blink and leaves a restrained shadow wake. |
| Flight | Uses server-authoritative survival propulsion, including directional ascent/descent and a much faster sprint-flight mode; it never grants creative-mode flight. |
| Elemental Blast | Lets the player explicitly select flame, frost, storm, or earth. The choice persists and delegates to the complete underlying power rather than cycling after every cast. |
| Starfall | Opens a warned, finite celestial strike sequence with rank branches for extra strikes, a moving storm eye, concealment reveal, projectile diversion, echoes, and a dominion crown. |
| Void Beam | Charges a penetrating live-aim ray, with ranked target penetration and a temporary terrain-safe void scar that interacts with later magic. |
| Fireball | Summons one chargeable Cinderheart. Recasts add tiers, punching launches it, and bounded reflection/counter rules prevent uncontrolled projectile clouds. |
| Frost Nova | Freezes and heavily slows permitted nearby targets in a rank-scaled radius. |
| Lightning Strike | Opens a warned Storm Tribunal at the aimed column. It has no cooldown, while energy, protection, and concurrency checks still prevent invalid spam. |
| Ground Slam | Opens a warned Faultbound Verdict with rank-scaled seismic beats, safe forced movement, and an always-present, tightly capped rank-scaled crater. |
| Thunderclap | Creates a wide boss-scale pressure cone, inflicting heavy rank-scaled damage and stun while deflecting incoming projectiles. |
| Speed Burst | Performs a collision-predicted physical dash and final shockwave. Motion ranks can spend a second step during the brief marked window. |
| Telekinesis | Throws permitted living targets radially away and reflects a bounded set of hostile projectiles; an empty cast refunds its energy. |
| Energy Beam | Channels four live-aim Sunfire beats whose damage and burn intensify on one target; water produces steam and advanced ranks unlock a flare or forks. |
| Super Speed | Runs an eight-second Chronal Overdrive with server-owned motion, restrained afterimages, water grounding, collision rites, memory slips, and mastered projectile curvature. |
| Breezy Bash | Captures permitted bodies in a two-stage wind rite, raises them through a visible apex, then calls each safely down with rank-scaled force. |
| Cozy Campfire | Creates a ten-second hearth that repeatedly heals friendly living entities and restores hunger to players. |
| Invisibility | Toggles true concealment with recurring energy drain and can be exposed by counter-magic or Insight effects. |
| Time Freeze | Claims Minecraft's global server tick freeze across every loaded dimension, preventing entities, projectiles, attacks, and world ticks while the caster remains able to act. |
| Forcefield | Toggles an owned, high-capacity defensive field without overwriting unrelated attribute effects. |
| Gravity Displacement | Opens a five-second orrery that collision-safely orbits nearby bodies, resolves overlapping claims, and can collapse or curve projectiles at advanced ranks. |
| Vessel Possession | Moves the mind into a consented player or suitable mob while a vulnerable skin-matched physical body remains at the casting point. |
| Astral Projection | Leaves a vulnerable physical body and releases a bounded soul-form scout; return is validated and cannot be used as invulnerability. |
| Energy Drain | Channels against a player or mob for two seconds. Players lose energy and receive particle-free Exhaustion; mobs take repeated percentage-health damage plus a capped 30% completion strike, while the caster recovers energy. |
| Ice Manipulation | Fires a freezing ray that harms and freezes targets, converts water to ice and lava to obsidian, and lays snow only where terrain policy permits. |
| Plant and Healing Acceleration | Grows the aimed bonemealable plant, with stronger growth at higher potency, and supplies the registered regeneration passive. |
| Double Health | Toggles a mod-owned maximum-health multiplier, preserves unrelated modifiers, and proportionally restores the vanilla heart layout when released. |

Telekinesis is a true radial release: permitted living targets are thrown away from the caster while up to 16 hostile projectiles are reflected along the caster's aim. If neither can be affected, its collapsed violet rune refunds the offered energy and starts no cooldown or collision residue.

Elemental Blast keeps the player's explicit flame, frost, storm, or earth selection until it is changed. Each phase uses the underlying canonical Fireball, Frost Nova, Lightning Strike, or Ground Slam identity for collision counterplay, residue, sound, and ceremony rather than presenting every phase as the same composite force. Its HUD medallion adopts the selected element before energy is spent.

Speed Burst is a synchronized physical dash, not a teleport. It predicts body-volume collisions, leaves an eight-tick cyan-white afterimage wake, and ends in a rank-scaled kinetic shockwave that respects safe zones, amethyst, and forced-movement consent without damaging terrain. Motion-ranked players can pay for one stronger Second Step during a 2.5-second window while the original persistent cooldown remains armed; alternating cyan-gold runes and an `II` medallion mark that server-authorized follow-up.

Super Speed is an eight-second server-owned Chronal Overdrive rather than a bundle of anonymous potion effects. A POWERS-owned movement modifier follows rank potency, drops to 35% strength in water with a visible hydroplane transition, and leaves only measured cyan-white wakes so teleports cannot forge trails. Wall contact fractures a time seal: Motion may rewind through one collision-safe Second Step, Might may release one non-damaging eight-body pressure corona, Veil periodically slips the runner from at most eight nearby hostile target memories without granting invisibility, and Dominion curves at most 16 approaching hostile projectiles once without reflection or ownership theft. Consent and safe zones, amethyst, projection bodies, forcefields, Sanctuary/Kinetic Ward, time locks, and blocked geometry each resist with distinct counter-sigils. Death, disconnect, respawn, dimension change, suppression, time freeze, or losing the power removes only this ability's modifier and grants a safe fall release.

Fireball now summons one server-owned Cinderheart per caster instead of allowing an uncapped cloud of delayed-task projectiles. Recasting while it hovers pays for tiers two and three; Ancient Mastery unlocks a fourth seal. Punching the heart begins a six-second measured flight, after which only two reflections are permitted, plus one each from Reflective Ward and Ancient Mastery. Current player control is tracked independently from the original caster so reflected kills, consent, safe zones, and lifecycle cleanup remain correct. Tagged amethyst, Sanctuary, Kinetic Ward, personal forcefields, water, ice, snow, missing controllers, and protected targets all have distinct terminals; water and frost become a reduced no-ignition steam pressure wave. Might adds a consent-safe impact corona. Vanilla explosion grief is never used: the impact carves a bounded rank-scaled scar directly, never removes protected infrastructure, and adds only capped surface fire when the configured terrain policy permits it.

Starfall is now a finite Astral Convergence rather than three simultaneous random bolts. A one-second astrolabe omen reveals the complete field before eight deterministic golden-angle strikes descend six ticks apart. Might adds strikes, damage, radius and consent-safe pressure; Motion leashes the storm eye to the initially aimed body; Insight reveals successfully struck veils; Wardcraft diverts at most 16 hostile projectiles without stealing them; Communion mirrors every third strike at reduced power; and Dominion adds two strikes plus a central crown. Roofs catch the sky path, water conducts a wider reduced pulse, Pure Light amplifies it, Darkness consumes it, and amethyst, safe zones, Sanctuary, Kinetic Ward, forcefields, time locks and projection bodies retain distinct protections or counter-cues. Every body has repeat and total-hit caps, every search is nearest-first and bounded, and each impact leaves a bounded rank-scaled scar without using harmful vanilla lightning.

Ground Slam now opens a twelve-tick Faultbound Verdict instead of destroying a crater and moving every nearby body in the same tick. Its loaded support surface is named before impact: water softens the quake, Darkness hollows it, Pure Light refracts it, and unsupported air, safe zones, tagged amethyst, or powered wards close the fault. Bodies are nearest-first and capped; shields, Sanctuary, Kinetic Ward, projection anchors, time locks, movement consent, blocked volumes, and airborne footing each retain distinct behavior. Might expands the primary fault, Motion carries its warning clock, Insight reveals struck veils, Wardcraft grants a short absorption mantle, Communion releases a reduced offset echo, Veil's dust shroud clears at most eight visible hostile mob memories, and Dominion adds a deeper central crown. Terrain never changes by default; an opted-in server can remove only 8 soft deterministic samples, or 16 with Dominion, without drops, block-force damage, fluid loss, or vanilla explosions. The delegated Elemental Blast earth phase owns the same finite lifecycle correctly.

Void Beam now opens through a visible 0.6-second server-owned charge, then follows the caster's live aim through up to three ordered bodies. Later penetrations fall to 72% and 52% damage; Empowered Impact and Ancient Mastery each bore through one additional target, while Dark Resurgence deepens the Wither and aftermath pulse. Pure Light, tagged amethyst, powered wards, Sanctuary, Kinetic Ward, safe zones, and personal forcefields stop the ray with distinct inversion or fracture ceremonies. An unopposed release leaves a rank-scaled, terrain-safe void scar for up to eight seconds, pulsing against at most 16 nearby permitted targets and carrying collision residue at the real impact point so later light magic can tear it into an eclipsing star rift.

Gravity Displacement anchors a five-second gravitational orrery instead of applying a disposable Levitation effect. Up to 16 nearest permitted bodies are pulled through deterministic, collision-checked orbits and receive a safe Slow Falling release; Empowered Impact and Ancient Mastery each add eight capture slots, while Empowered Impact turns a natural collapse into a bounded damaging shock and Ancient Mastery curves up to 24 hostile projectiles without stealing ownership. Consent and safe zones, amethyst, soul-anchored projection bodies, personal forcefields, Sanctuary/Kinetic Ward fields, and time locks resist with distinct ceremonies. Overlapping orreries assign each shared body to the nearer field with hysteresis and a visible violet-cyan resonance handoff, eliminating velocity jitter.

Energy Beam is a two-second live-aim Sunfire channel rather than a disposable ray. After an eight-tick solar focus it releases four server-authoritative beats; consecutive scorches on one body climb through three damage and burn tiers, while changing targets resets the sequence. Water transforms a beat into a bounded steam pressure pulse with no ignition. Ordinary matter, tagged amethyst, Pure Light, Darkness, safe zones, Sanctuary, Kinetic Ward, and personal forcefields stop the ray through different ceremonies. Empowered Impact may erupt one protected, terrain-safe solar flare after a full scorch sequence; Ancient Mastery forks successful hits to at most two visible nearby bodies without chaining or crossing wards. Death, suppression, power loss, dimension changes, disconnects, and time locks break the channel cleanly.

Breezy Bash is an eighteen-tick, server-owned Tempest Rite rather than a delayed task holding stale entities. It claims the nearest 16 permitted bodies in a true sphere, launches them through collision-checked outward lift, shows their shared apex, and independently revalidates each downward verdict. Consent and safe zones, amethyst, projection bodies, forcefields, Sanctuary/Kinetic Ward, time locks, ceilings, and an existing gust claim resist with distinct wind fractures; an empty rite refunds its offer. Empowered Impact adds eight claims, deepens the slam, and closes as a terrain-safe pressure corona. Ancient Mastery adds eight claims and curves at most 16 hostile projectiles away without reflecting them or stealing ownership. Every interruption safely releases captured bodies with Slow Falling.

Every power uses the same energy well. Light progression grows it from 250 to 770; darkness progression grows its separate well from 500 to 1,850. Failed casts refund their activation cost. Toggle powers drain once per second and cause backlash if they exhaust the well. Sleeping and runestones restore energy unless Exhaustion is active.

Darkness and Pure Light are living realm matter. Vanilla random ticks make either block convert adjacent breakable terrain without requiring kills or loading distant chunks. Amethyst, unbreakable/server infrastructure, and the authored gold/crying-obsidian mindscape landmarks contain that conversion; datapacks can extend `#powers:living_force_immune`. Darkness withers nearby living entities without the `darkness` tag at strength III; tagged players instead receive a rank-scaled 24-energy pulse each second unless amethyst has poisoned their connection. Dark Resurgence strengthens that affinity by 50%, doubling it at or below one-quarter energy with a distinct eclipse awakening. When Pure Light touches Darkness, a catastrophic eclipse blast damages and throws up to 256 inspected living entities, then radially erases both forces from the epicentre through a 48-block sphere over several server-budgeted ticks without destroying unrelated builds. Even when spreading is disabled, opposed blocks keep retrying their clash check.

## Mind travel and vulnerable bodies

Astral projection, vessel possession, dreamwalking, player-marking during Time Shift, and travel to the Light or Dark Realm leave a skin-matched Minecraft mannequin where the player's physical body remains.

- The spirit or mind moves; the body remains loaded and visible.
- Damage to the body is mirrored to the real player, so projection never grants invincibility.
- Death, disconnect, server shutdown, invalid state, or `/powers return` cleans up or restores the session safely.
- Return placement is collision-, border-, ward-, and safe-zone-validated.

The Light Realm and Dark Realm are mindscapes rather than ordinary destinations. Each contains six persistent memory sites with custom obelisks, localized lore, explained rewards, magical effects, and choices that feed the title maze. The Light Realm uses a pure white sky without a sun, moon, stars, weather tint, or black void; the Dark Realm remains an enclosed hostile thoughtscape. Middleworld has its own muted dream biome rather than borrowing the Light Realm's appearance.

Realm departure is intentionally stricter than entry. A player trapped in the Dark Realm cannot leave through gameplay travel until they have the `darkness` tag and darkness level 5; leaving the Light Realm requires level 5 in either progression. `/powers return` is reserved for restoring a valid detached body and operator travel remains an explicit recovery route, so ordinary crystals, portals, teleports, deaths, and dimension menus cannot bypass the lock.

Death does not provide a realm escape: an underqualified player respawns at the corresponding mindscape entry instead of being accepted at a vanilla Overworld spawn. The rule is shared with every travel policy so later gateways cannot accidentally disagree with it.

## Rank maze

Advancements determine earned rank depth, innate-power scaling, and energy capacity. Rank never changes crystal abilities or grimoire spells. Every innate power receives at least the numeric ladder: +5% potency, +2% range, +2.5% duration, -1% energy cost, and -1.5% cooldown per level. Control, support, and defence gain another +1.5% duration per level. Maze perks then specialize those values, with combined caps of +90% potency, +55% range, +65% duration, -35% cost, and -40% cooldown.

The light and darkness graphs each contain 28 nodes, including legacy titles and paths through might, motion, insight, wardcraft, veils, communion, and dominion. Converging paths create hybrid titles such as Runeblade, Riftwalker, Soulwarden, Eclipse Weaver, and their endgame forms.

Named branch variants have mechanical consequences as well as stronger ceremonies: Might empowers selected impacts, Motion grants second steps, Insight grants True Sight, Wardcraft reflects forcefields, Communion strengthens soul transfer, Veil reduces readable residue, Dominion deepens ancient mastery, and the Darkness path awakens Dark Resurgence.

Press `B` (rebindable) to open the synchronized Labyrinth of Names, inspect every connected title and perk, awaken reachable nodes, or attune a previously earned title. The server revalidates every click. The equivalent commands are `/powers path list`, `/powers path unlock <node>`, `/powers path focus <node>`, and `/powers path respec`; respeccing preserves earned depth and costs 30 experience levels by default.

Players with the `darkness` entity tag use the darkness advancement track and energy well. `/powers darkprefix` controls whether the focused darkness title is publicly shown.

Only the applicable advancement background and track is visible: radiant players cannot see the darkness rites and darkness-infected players cannot see the radiant mastery track. Focused titles are synchronized into the player's display-name, tab-list, and chat prefix rather than being client-only decoration.

Normal mastery is cumulative and intentionally long-form: levels 1-10 require respectively 100/300/750/1,500/2,500/4,000/6,000/8,500/12,000/18,000 successful innate casts and 10/50/150/300/500/800/1,200/1,800/2,500/4,000 power kills. Levels 4-10 additionally require 1-6 Light Realm memories, while levels 5-10 require 1/2/4/7/12/25 boss kills. Vanilla bosses count, and modded living bosses with at least 200 maximum health count so progression remains compatible with large content mods.

Darkness progression records permanent atrocities rather than trivial inventory checks. Levels 1-2 require 25 and 100 passive kills. From level 3 onward the cumulative demands become increasingly cruel, culminating at level 10 in 500 villagers, 500 wolves, 100 baby villagers, and 50 iron golems. Deeds cannot be undone by dropping items, dying, or switching advancements.

## Grimoires and rituals

Sneak-use a grimoire to turn its pages; use it normally to cast the selected spell. Costs, cooldowns, and channels are fixed equipment values and never inherit player rank. Channeled rituals lock their entity or block target when the channel starts, so looking elsewhere cannot silently retarget the release. They break if the caster moves, takes damage, changes books or dimensions, dies, loses range or line of sight, loses the target, or becomes amethyst-dampened, and return half of the offered energy.

| Grimoire spell | Cost / cooldown / channel | Complete effect |
| --- | --- | --- |
| Celestial: Soul Compass | 14 / 10s / instant | Opens the authenticated locator screen. It accepts an online player or the unique custom name of one loaded mob, rejects ambiguous or incomplete world scans, enforces player locator consent, reveals the result, then begins a one-minute vulnerable-body camera view through that target. |
| Celestial: Mark of the Far Star | 18 / 25s / 2s | Locks a permitted living target and marks it with particle-free Glowing for 30 seconds. |
| Celestial: Tempest Sigil | 22 / 60s / 4s | Starts a localized ancient storm centred on the caster. |
| Celestial: Heavenfall — Celestial Ruin | 100 / 60m / 10s | Locks a ground point and starts an irreversible one-minute warning. A pulsing 100-block-diameter sky beam remains even after players leave; countdown and exact ruin-wave cursor survive server restarts. The strike flashes the screen white for three seconds, applies a tinnitus cue, deals up to 50,000 custom damage with quadratic falloff across 2,048 blocks, excavates a 120-block-radius crater, and sends 96 loaded-chunk fire-scar rays outward. Only the 19×19 detonation area is progressively ticketed shortly before impact. |
| Deep: Dimensional Anchor | 22 / 60s / 2s | Anchors a consent-valid targeted player against dimensional and teleport movement. |
| Deep: Deepbinding Sigil | 16 / 20s / 1.5s | Locks a living target under severe Slowness and Weakness for 30 seconds. |
| Deep: Seal of Closed Ways | 24 / 50s / 3s | Creates a seven-block anti-portal field for 30 seconds. Recasting replaces the caster's earlier field instead of accumulating unbounded wards. |
| Deep: Kinetic Ward | 18 / 30s / 1s | Creates a seven-block field that resists forced movement and participates in projectile and impact counters. |
| Blight: Crimson Transference | 18 / 25s / 1.5s | Deals six magic damage to a permitted target and heals the caster by the health actually removed. |
| Blight: The Threefold Hex | 20 / 40s / 2s | Applies particle-free Weakness, Slowness, and Darkness to a permitted target. |
| Blight: Veil of Unremembering | 16 / 30s / 1s | Grants a tracked 30-second concealment veil that can be specifically revealed without deleting unrelated invisibility effects. |
| Wild: Circle of Cleansing Rain | 20 / 30s / 2.5s | Cleans harmful effects except amethyst poisoning, heals allies, clears dimensional anchors, and severs soul links inside an eight-block circle. |
| Wild: Old-Root Binding | 16 / 20s / 1.5s | Roots a permitted target with stronger Slowness and Weakness than the Deep binding. |
| Wild: Sanctuary of the First Grove | 24 / 50s / 3s | Creates a seven-block, 30-second sanctuary that rejects hostile power damage and hostile spell targeting. |
| Infernal: Ashen Threshold | 20 / 40s / 2s | Creates a seven-block infernal seal used by banishment, portal, and collision counterplay. |
| Infernal: Circle of Banishment | 24 / 50s / 3s | Instantly dismisses ephemeral summons; other permitted targets are hurled away and struck with bounded magic damage. |
| Infernal: Leashed Hellfire | 18 / 25s / 1s | Deals six terrain-safe magic damage and ignites a permitted target for six seconds. |
| Abyssal: Amethyst Unmaking | 26 / 60s / 4s | Locks the aimed powered Amethyst Ward and suppresses it for 45 seconds without destroying the structure. |
| Abyssal: Severing Word | 16 / 15s / instant | Counters the nearest active hostile channel within spell range. |
| Abyssal: Unweaving | 18 / 25s / 1s | Dispels the nearest field and/or clears non-amethyst effects and a dimensional anchor from the locked permitted target. |
| Abyssal: Invocation of the Ninth Echo | 22 / 45s / 2.5s | Empowers the caster's next ritual, increasing potency, duration, radius, displacement, and ward suppression while modestly accelerating its channel. |

Insight-ranked True Sight can pierce either mindscape's normal path/rank veil during a consented Soul Compass ritual, signalled by a cyan-gold third-eye glyph, but cannot bypass consent or strip concealment. Dimensional Anchor is exclusively a Deep Grimoire spell. Counterspell, Unweaving, Sanctuary, anti-portal fields, Kinetic Ward, Amethyst Unmaking, and Celestial Ruin's safe-zone boundary provide direct counterplay.

## Crystals

Every crystal has a bound ability or selectable convergence. Sneak-use turns a multi-mode crystal; normal use releases its selected force.

| Crystal | Ability |
| --- | --- |
| Red | Inferno creates an eight-second, 12-block firestorm that repeatedly damages and ignites permitted targets without a vanilla terrain explosion. |
| Orange | Clone Swarm creates three 80-health combat clones for 60 seconds; Creativity Manifestation builds a protected obsidian/glass/glowstone redoubt at the aimed point when terrain policy allows. |
| Yellow | Size Shift alternates a 20-second titan form and miniature form, with appropriate strength, speed, resistance, reach, and knockback changes. This remains separate from the freely selectable innate Size Morphing power. |
| Green | Life Bloom fully heals and cleanses living allies in a 20-block radius; Space-Time alternates six seconds of extreme personal acceleration and a six-second local entity/projectile freeze. The obsolete self-slow mode has been removed. |
| Blue | Chrono Stop freezes entities and projectiles in the configured local radius; Dreamwalking watches through a consented player's eyes for up to two minutes while leaving a vulnerable body. |
| Indigo | Portal Rift repeatedly strikes and repositions an aimed target through short rifts; Middleworld opens vulnerable mind travel to the muted dream dimension. |
| Violet | Soul Link binds up to eight nearby souls for ten seconds and mirrors a bounded share of later wounds across survivors. |
| Rainbow | Sevenfold convergence of the chromatic forces |
| Infected Rainbow | Fractured convergence of Inferno, Chrono Stop, Portal Rift, and Soul Link |
| Light | Aims at a consented player or defaults to the wielder, leaves a vulnerable body, asynchronously loads the destination, and enters the Light Realm beneath a white celestial storm. |
| Dark | Aims at a consented player or defaults to the wielder, leaves a vulnerable body, asynchronously loads the destination, and enters the Dark Realm beneath a corrupted storm. |

Convergence cooldowns are shared with their underlying forces, so swapping crystals cannot bypass a rare ability's recharge. Crystal crafting recipes are deliberately not included; they are reserved for later progression design. The resource validator prevents accidental crystal recipes from entering a release.

## Shadow Sword

The item whose compatibility identifier remains `powers:lycanbane` is presented everywhere as the **Shadow Sword** in bold dark grey. It is ancient Pure Darkness made solid.

- A non-darkness carrier is struck with particle-free Blindness and Wither, cannot use the sword, and provokes up to four nearby Darkness Creatures through lightning-marked protection summons.
- A darkness-tagged wielder regenerates 50-250 darkness energy each second according to rank; level 10 receives a 900-energy apotheosis refill pulse.
- Right-click casts the selected action. Crouch-right-click opens a non-pausing eight-segment combat wheel. Hover and release crouch, click, or press `1`–`8` to bind a favourite; crouch-scroll cycles the same persistent loadout without opening a screen. The centre opens a searchable icon library with Favourites, Innate, Crystals, and Sword tabs, live energy/cooldown/toggle/lock state, and contextual Size Morph or Element controls.
- Every innate and underlying crystal action is available from rank 1. Existing actions retain their mechanics but receive a black/violet corruption of their own original colour, darker sounds, and evil residue when routed through the sword.
- At darkness level 10, every action cast through the sword ignores existing cooldowns and starts no new cooldown. Energy, target validation, amethyst, sanctuary, safe-zone, and bounded-entity protections still apply.

Exactly three actions are unique to the Shadow Sword. Retired saved selections migrate to the nearest surviving action; their strongest motifs remain as corrupted presentations rather than duplicate mechanics.

| Invocation | Rank | Effect |
| --- | ---: | --- |
| Call the Hollowed | 1 | Summons a bounded squad of owner-aligned Darkness Creatures beneath lightning-marked seals. |
| Blight Ground | 1 | Queues a protected six-block Darkness conversion beneath the wielder; fluids, block entities, unbreakable blocks, safe zones, and living-force immunity remain intact. |
| Nightfall Dominion | 10 | Toggles Strength X, Resistance IV, Regeneration V, Fire Resistance, Speed IV, and a 24-block Wither pressure aura. |

## Heavenly Partisan

The **Heavenly Partisan** is the unbreakable Pure Light counterpart. Only non-dark players may wield it; a darkness-infected carrier is judged with hidden Glowing/radiant damage and guarded by lightning-arriving Radiant Sentinels. It routes the same 28 innate and 13 crystal actions through a gold-white presentation, while its eleven unique rites emphasize protection and group play. Normal rank 10 reduces Partisan cooldowns by 60%, increases its regeneration aura, and strengthens its support effects; unlike darkness apotheosis, it does not remove cooldowns.

| Partisan rite | Rank | Effect |
| --- | ---: | --- |
| Call the Radiant | 1 | Summons a bounded squad of Radiant Sentinels. |
| Consecrate Ground | 2 | Queues a six-block protected Pure Light conversion. |
| Dawnstride | 3 | Performs a legal 24-block beam step. |
| Covenant Chain | 4 | Gives an ally 30 seconds of regeneration/absorption and bounded damage sharing, or binds an opposed hostile. Each owner is capped at eight allies. |
| Daybreak Wave | 5 | Damages darkness hostiles, purifies removable harmful effects, heals others, and clears unprotected projectiles. |
| Heaven Gate | 6 | Opens one owner-only pair of temporary radiant gates. |
| Banish Darkness | 7 | Queues a protected nine-block darkness purge; touching living opposed matter can still trigger its catastrophic clash. |
| Divine Decree | 8 | Resolves five-second radiant judgement against a hostile or a major heal/absorption blessing on an ally. |
| Solar Firmament | 8 | Opens an eight-second field that repels hostiles/projectiles and heals light-aligned occupants. |
| Second Dawn | 9 | Arms one five-minute, single-use radiant death ward. |
| Host of Heaven | 10 | Calls two elite sentinels, a radiant dominion field, consecrated ground, and a cosmic heaven-beam ceremony. |

## Runestones

Runestones are reusable energy focuses, stack to 16, show a small actionbar result, and cannot work through Exhaustion. Their authored tier restores between 40 and 600 energy; recharge ranges from three to fifteen seconds. Inert runestones have a natural recipe, and dark tiny/small/medium/large runestones form a craftable upgrade chain restoring 60/125/250/400 energy. Additional inscribed and bound variants restore up to 600. Dungeon, abandoned-mineshaft, and ancient-city chests can supply the natural tiers without replacing vanilla loot.

## Imported relic families

Formerly decorative imported relics now have bounded server-owned roles:

- Rings and amulets are inventory attunements. Up to three improve passive energy recovery and maintain hidden Resistance; additional copies do not increase server work or strength.
- Soulstones drain a targeted permitted living entity without killing it and return energy. Larger stones are stronger; the Soul Matrix is the capstone and also passively stabilises energy.
- The Ritual Dagger sacrifices four health to amplify the next grimoire ritual for 30 seconds.
- Beating, wooden, mechanical, and ghoul hearts heal on use and sustain a small particle-free regeneration attunement while carried.
- The Philosopher's Stone performs controlled, energy-priced transmutation of stone, deepslate, netherrack, or end stone. It refuses protected blocks and block entities.
- Use a Lodestone relic on a safe destination to bind its dimension and coordinates. A carried Miniportal asynchronously loads and returns to that anchor through the same border, realm, ward, anchor, collision, and hazard policy as other travel.
- The Flute recalls, heals, and rebinds nearby player-shaped guardians to its wielder under the normal guardian caps.
- Essences, blood salts, fossils, jewels, pearls, stones, ember fragments, and other archaeology reagents contribute documented Arcane Crucible XP tiers. Tattered pages and remaining lore relics are contextual clues for the Knowledge Book rather than fake weapons.

Representative relics are injected additively into dungeon, pyramid, temple, mansion, ruined-portal, fortress, buried-treasure, stronghold, ancient-city, bastion, and end-city loot. Their variants share the same family behavior. Crystals and deliberately deferred story items remain recipe-less.

## Knowledge Book

Vanilla's normally command-only Knowledge Book opens a non-consuming searchable question screen with bounded history. The server answers offline from datapack `powers:knowledge_entries`, loaded item/block/entity registries, loaded recipe identifiers, progression visibility, and redacted context for the held item, targeted block/entity, biome, and dimension. Answers show confidence, sources, and registry IDs; unknown or hidden information is admitted rather than invented. Stronghold libraries can contain the book and award the Archivist advancement. The disabled-by-default OpenAI-compatible endpoint may answer only low-confidence non-recipe questions; it receives the text the player chose to type plus bounded registry context, while the server adds no identity, coordinates, chat, IPs, secrets, or raw world data. HTTPS/loopback validation, timeouts, concurrency, cooldowns, and offline fallback remain mandatory.

## Arcane Crucible

The Arcane Crucible is a two-input, server-owned weapon forge. Put one eligible base weapon in the left slot and one catalyst in the right, choose a server-advertised result, and press **Transmute**. Its versioned mutation lock, one atomic commit, hopper rules, break recovery, full-inventory drop fallback, and stale-choice validation prevent duplicated or lost results.

Its three-stage path is:

1. A Darkness or Pure Light block converts a tagged ordinary weapon into one of the registered non-mythic alignment peers while preserving safe components, enchantments, custom name, lore, repair cost, and proportional durability.
2. An Animated Artifact Star binds zero-cooldown alignment lightning without replacing the weapon. Each right-click still needs a valid aimed target, energy, line of sight, same-tick rate allowance, and every normal protection check.
3. A runestone infuses a star-bound weapon with 25/75/225/675 XP according to tier. Authored archaeology and spell-school reagents provide bounded 35–175 XP alternatives. Levels are derived from overflow-safe exponential thresholds and cap at 30; lightning damage grows to a 1,200 non-player cap and a 120 player cap.

Datapacks can add base weapons to `#powers:arcane_crucible_base_weapons`. Java integrations can call `CrucibleEligibility.registerBaseWeapon(Predicate<ItemStack>)` or `registerExclusion(...)`. The Shadow Sword, Heavenly Partisan, stacks with mythic identity, and already converted weapons are hard exclusions even if another pack tags them. Crystals and story artifacts still receive no recipe.

## Owner-private shadow companion

A darkness-tagged player carrying the Shadow Sword may press `G` to explicitly call an owner-private lore apparition; it never appears merely because the sword is held. It is client-rendered from a server-authoritative session rather than a tracked world mob: other clients receive no spawn, position, particle, sound, collision, pathfinding, target, or dialogue data. It follows at 5 Hz, privately steps behind the owner if separated by more than 20 blocks, and vanishes on dismissal, lost eligibility, logout, invalid dimension state, death/projection, or server stop.

Press `G` again while near and facing the apparition to request a line; crouch-`G` dismisses it. The server authenticates the current session, distance, and view cone. The offline lore engine reacts to realm, low health/energy, darkness rank, nearby living forces, selected rite, recent death, a nearby First Vessel, and rank milestones without repeating its last topics.

An optional OpenAI-compatible text endpoint may replace only the wording. It is disabled by default, never receives UUIDs, names, chat, IPs, coordinates, or secrets, and cannot change gameplay. Credentials come only from the configured environment-variable name. Requests are asynchronous, capped at one per owner/four globally, limited to 2.5 seconds and 256 output characters, and always fall back to offline lore.

## Player-like entities

`Darkness Creature` is a naturally spawning Dark Realm monster with a completely black player model, 100 health, 12 armour, 16 melee damage, and player-like movement. It attacks every living entity without the `darkness` tag and alternates bounded, terrain-safe lightning and custom darkness fireballs at range. Sword guardians use the same creature and disappear naturally rather than accumulating permanent server entities.

`Power Test Actor` is a player-model test opponent with the same boss-capable base attributes. It retaliates when hit and fights hostile mobs, making powers, spells, possession, projectile counters, and particle presentation testable in a single-player world. Spawn eggs for both entities are available in creative/operator testing.

`Radiant Sentinel` is the Partisan's light-aligned player-shaped guardian. Both guardian factions preserve owner and alignment, expire when their owner leaves the dimension, enforce four-normal/two-elite owner caps and a global cap, and cycle bounded melee, lightning, fireball, and alignment-field tactics. Their fields heal allies or punish the opposed faction without crossing safe zones or amethyst counterplay.

## First Vessel boss

The **First Vessel** is an original player-shaped ancient boss that never spawns naturally. It has a 5,000-point virtual vitality layer, 16 armour, 16 toughness, 36 attack damage, 0.33 movement speed, 0.8 knockback resistance, persistent boss-bar state, original skin/sounds, a spawn egg, loot and advancement hooks. When the first eligible players approach, vitality snapshots `1 + 0.55 × (players − 1)` and caps at four times base health; it never shrinks mid-fight.

Its tactical planner evaluates at most 24 candidates per ten-tick decision, with separate action cooldowns and facts for distance, line of sight, verticality, player clusters, projectiles, health, movement, cover, protection, and repetition. Its catalogue is tested directly against all 28 innate powers, and every adapter is entity-safe—no player-only handler is cast onto the boss. At intervals it may also mirror a power observed in the target's current loadout.

The encounter has three named states: **Waking Vessel** (`AWAKENING`, 100–70%), **Broken Constellation** (`UNBOUND`, 70–35%), and **Crownless God** (`LAST_COVENANT`, below 35%). Phase changes perform Sevenfold Step. At half health it attempts one five-second Vessel Reconstitution; eight percent maximum-health damage, amethyst, or a light dominion interrupts it. Later phases gain projectile-consuming World-Suture, and Crownless God releases one terrain-safe Last Firmament below 15%. Safe zones, Sanctuary, amethyst, and bounded candidate limits remain valid boss counterplay.

Operators may use `/powers boss spawn`. The survival ritual requires a darkness-tagged level-10 player to sneak-use the Shadow Sword on an Arcane Crucible surrounded by Darkness at the four cardinal positions three blocks away and Pure Light at the four diagonals two blocks away. A successful invocation consumes the eight anchors and creates one boss; an existing nearby First Vessel blocks another ritual.

## Counterplay and server safety

- Amethyst items, tagged amethyst blocks, and powered Amethyst Wards suppress powers without making normal melee damage harmless.
- Player-targeted teleportation, locating, companion travel, dreamwalking, and possession have per-player consent controls.
- Safe zones can block power harm, hostile movement, and terrain damage.
- Rank-scaled combat terrain scars are enabled by default; safe zones, protected realm matter,
  amethyst, indestructible blocks, block entities, and the administrator override remain protected.
- Teleports validate world borders, loaded-distance limits, collisions, floors, wards, anchors, and destination dimensions.
- Temporary entities are marked ephemeral and excluded from saves.
- Global Time Freeze uses shared `/tick freeze` ownership and safe restoration; local crystal freezes retain bounded radii and overlap-safe cleanup.
- Particles use a server-wide per-tick budget; amethyst scanning and state network syncs are cached.
- Serverbound activation, selection, travel, rank, artifact, locator, and ritual packets each have independent per-player rate lanes, so one noisy client cannot monopolize the server thread or starve ordinary play.
- Custom food and mob drops are injected additively instead of replacing vanilla loot tables.

## Magic collisions and presentation

The 28 assignable powers resolve to 27 distinct innate action identities because player Size Morphing and the Yellow Crystal deliberately share the canonical `size_shift` force. Those 27 innate actions, 21 grimoire spells, 13 crystal actions, 16 artifact actions (three Shadow Sword rites, 11 Partisan rites, and two star-bound lightning alignments), three amethyst suppressors, and two living realm forces form an 82-action collision system. Every one of the 3,403 unordered same-or-cross-action pairs, including same-action resonance, has a deterministic outcome, potency/duration/range adjustment, accessible shape cue, and semantic sound cue. Named high-impact combinations add mechanics such as steam pressure, eclipses that reveal concealment, realm-matter annihilation, projectile-consuming star rifts, summon banishment, soul-link purification, finite ward fracture, grounded storms, hostile pressure waves, and concordant healing.

The complete catalogue is in [`docs/interactions/action-catalogue.md`](docs/interactions/action-catalogue.md), and every possible pair is listed in [`docs/interactions/interaction-matrix.csv`](docs/interactions/interaction-matrix.csv). Every successful innate, crystal, grimoire, Shadow Sword, and Heavenly Partisan cast receives a signature-driven anticipation, release, impact, and aftermath ceremony in addition to its bespoke gameplay effects. Flame fractures, frost shards, storms fork, time spirals, space and soul tether, life roots, darkness eclipses, and light forms celestial crowns; the server chooses the matching authored sound and both signature colours. Ritual glyphs form on the ground beneath the caster, while vertical glyph, eclipse, and lightning sigils face each observer locally instead of becoming edge-on. Ceremony radius, density, motion, volume, and pitch intensify at rank depths 4 and 8, with another bounded step for Ancient Mastery. Client effects use eight authored particle sprites, 13 original mono Vorbis sounds, distance culling, reduced-motion geometry and velocity clamps, and hard client/server particle budgets.

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
| `spaceTimeRadius` | `32` | Green Crystal freeze radius |
| `chronoStopRadius` | `64` | Indigo Crystal chrono-stop radius |
| `rankRespecExperienceLevels` | `30` | Rank-maze respec price |
| `adminPermissionLevel` | `2` | Permission level required by administrative commands |
| `livingForces.spreadingEnabled` | `true` | Enables random-tick Darkness and Pure Light conversion |
| `livingForces.spreadAttempts` | `2` | Face-adjacent conversion attempts per selected random tick |
| `livingForces.auraRadius` | `8` | Darkness affinity range around indexed blocks |
| `livingForces.energyRefillPerSecond` | `24` | Base darkness-tag energy pulse before rank scaling |
| `livingForces.clashRadius` | `48` | Realm-matter annihilation sphere radius |
| `livingForces.clashChecksPerTick` | `4096` | Maximum in-sphere clash positions processed per tick |
| `safeZones` | `[]` | Protected dimension-centred spheres |
| `dialogueProvider.enabled` | `false` | Enables optional remote lore wording and low-confidence, non-recipe Knowledge Book fallback |
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
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
./gradlew clean test runGameTest build
```

`check` includes strict validation of JSON, duplicate keys, PNG headers/alpha/dimensions, Ogg/Vorbis streams, particle and sound references, models, translations, namespace safety, dimension biomes, exhaustive interaction-document drift, every production Java source, all non-item assets, and intentionally absent crystal recipes. `runGameTest` boots a real Fabric server and exercises live Darkness spreading and tagged Darkness Creature targeting. The release JAR is written to `build/libs/`.

Automated tests validate rules, registries, resources, packet bounds, lifecycle cleanup, and dedicated-server behavior. A final manual playtest is still appropriate before deploying to a valuable multiplayer world, especially for subjective particle density, HUD scale at a player's chosen GUI scale, controls alongside other mods, and catastrophic terrain settings. Back up the world before enabling Celestial Ruin terrain damage or placing opposed living realm matter.

For manual development runs, use `./test.sh client` or `./test.sh server`.

## Licence

The source code is available under the MIT licence. See [LICENSE](LICENSE).
