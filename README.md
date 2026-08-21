# POWERS

POWERS is a server-authoritative Fabric mod for Minecraft Java Edition 26.2. It adds 23 innate powers, two branching rank mazes, 12 practical grimoire spells, ten usable crystals, Light/Dark mindscapes, three bosses, living Darkness and Pure Light, mythic artifacts, an Arcane Crucible, player-like test entities, and a conversational combat companion called Shadow.

Its atmosphere is inspired by FavreMySabre's *The Rainbow Quest*, ancient ritual magic, and supernatural horror. It is an independent fan project and is not affiliated with those creators or franchises.

This README documents released behavior only. Exact per-item, per-level, per-action, and per-interaction appendices are linked throughout; proposed work is kept separately in [the improvement backlog](docs/planning/IMPROVEMENT_BACKLOG.md).

## Requirements and launch

| Requirement | Version |
| --- | --- |
| Minecraft Java Edition | 26.2 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.156.0+26.2 or newer |
| Java | 25 |
| Installation | Server and every connecting client |

From the repository root:

```bash
./test.sh client     # launch a development client
./test.sh server     # launch a development dedicated server
./test.sh check      # clean build, unit tests, audits, and resource validation
```

## Integration API

Server-side extensions may implement `com.powers.api.v1.PowersExtension` and expose it through the
Fabric entrypoint key `powers:v1`. API version `1.0` registers semantic actions into the canonical
collision catalogue, fixed physical presences into the live spatial runtime, unanimous fail-closed
protection services, authoritative cast contexts, and server lifecycle hooks. Registration is
deterministic and closes before `SERVER_STARTED`; all mutation is server-thread-only and every
extension registration is removed at server stop. Physical presences require one-shot live-player
cast authority and pass range, lifetime, safe-zone, protection, collision, energy, cooldown, and work
budgets before commit. See [API v1 integration](docs/development/integration-api-v1.md)
and the independently compiled `src/exampleExtension` compatibility mod.

## First awakening and controls

Each persistent player receives three distinct allegiance-compatible innate powers on first join and one non-duplicating vanilla written guide, **POWERS: First Awakening**. A full inventory drops the guide safely at the player instead of losing it.

| Input | Action |
| --- | --- |
| `V`, `X`, `C` | Cast the three innate slots; all keys are rebindable. |
| Crouch + power key | Open that power's selector when it has modes. |
| `B` | Open the alignment-specific Rank Maze. |
| `G` | Manifest or interact with an available Shadow. |
| Right-click artifact | Cast its selected invocation. |
| Crouch-right-click artifact | Open the non-pausing eight-slot combat wheel. |
| Crouch-scroll artifact | Cycle favourites without opening a screen. |
| Crouch-use grimoire/crystal | Change the selected spell or supported crystal mode. |

The HUD places three rune medallions at the right edge and ten separate nine-pixel energy symbols directly above and aligned to vanilla hunger. Full, half, and empty symbols have normal, Darkness, amethyst-poisoned, and projected-body palettes. Vanilla extra hearts, armour, air, mount health, hunger, and GUI scale move the row rather than overlap it.

## Authoritative casting rules

Every cast resolves its applicable targeting, protection, amethyst, safe-zone, realm, and workload policy before committing energy and cooldown. Ordinary hostile actions retain their consent and collision rules. Authored travel routes are the deliberate exception: they automatically carry a bounded nearby living cohort without consent, and entered coordinates are used exactly without searching for a collision-free or hazard-free landing. A rejected activation refunds its offered cost and leaves no magic residue.

| Cast source | Rank scaling |
| --- | --- |
| `INNATE` | Uses the caster's 0–10 rank profile and selected rank nodes. |
| `SPELL` | Uses fixed grimoire values; never inherits innate rank. |
| `CRYSTAL` | Uses fixed crystal values; never inherits innate rank. |
| `ARTIFACT` | Uses only explicit artifact/apotheosis modifiers. |

Terrain effects are authored, capped transformations rather than unbounded vanilla explosions. They obey configuration, protected blocks, block entities, world borders, safe zones, and the `#powers:living_force_immune`/related tags unless a feature explicitly documents its catastrophic policy.

## Innate powers

Random assignment guarantees one allegiance-exclusive option and never duplicates a slot. Radiant characters may uniquely receive Starfall and Plant & Healing Acceleration; Darkness characters may uniquely receive Void Beam and Energy Drain. Alignment changes preserve compatible powers and deterministically migrate forbidden or retired slots. Merely owning a power grants no passive potion effects or ambient particle cloud.

| Power | Implemented behavior |
| --- | --- |
| Size Morphing | Selectable player scale from `0.25×`–`2×` initially; `2.5×` at rank 4, `0.125×` at rank 6, `3×` at rank 7, and `4×` at rank 10. Upkeep increases with distance from normal size. |
| Teleport | Server-advertised dimension/coordinate or unique named-player/mob targeting with asynchronous destination tickets and a vulnerable five-second origin/destination storm. Entered coordinates are honoured exactly—even inside blocks, fluids, or hazardous terrain—while finite coordinates, world bounds, protections, anchors, and realm confinement remain authoritative. The caster automatically carries up to 15 living players, mobs, or a nearby Shadow within two blocks, preserving offsets and asking no consent. Middleworld is not an ordinary target. |
| Flight | Server-owned survival propulsion with directional rise/descent and faster sprint flight; never creative flight. While authorised propulsion is active it resets only that connection's vanilla floating counter, so a normal `allow-flight=false` server does not kick the caster. |
| Starfall | A finite warned Astral Convergence: deterministic strikes, roof/water/force reactions, repeat-hit caps, bounded scars, and rank branches for more strikes, moving focus, revelation, diversion, echoes, and dominion. |
| Void Beam | A telegraphed live-aim penetrating ray with diminishing later hits, rank bores, distinct force/ward/amethyst terminals, and a finite terrain-safe void scar. |
| Fireball | One server-owned chargeable Cinderheart per caster; recast charges and crouch-use deliberately releases it without changing ownership. Punch catch/deflection remains finite, and impact creates bounded terrain/fire instead of vanilla explosion grief. |
| Lightning Strike | Opens a warned Storm Tribunal at the aimed column. It has no gameplay cooldown; energy, concurrency, protection, and targeting still apply. Water, tagged copper contact, and tagged conductive armour relay a capped chain; lightning rods ground it harmlessly with a distinct cue. |
| Thunderclap | Wide boss-scale pressure cone with heavy rank-scaled damage, stun, projectile deflection, and bounded terrain impact. |
| Speed Burst | Collision-predicted physical dash with afterimages and an ending shockwave; Motion rank can pay for one Second Step while the original cooldown remains. |
| Telekinesis | Radially throws permitted living targets and reflects up to 16 hostile projectiles along aim. An empty release refunds energy and cooldown. |
| Energy Beam | Four live-aim Sunfire beats with escalating scorch; water creates steam, advanced ranks add a protected flare or non-chaining forks, and beam intersections are physical. |
| Super Speed | Eight-second Chronal Overdrive using an owned movement modifier, restrained wakes, 35% strength in water, collision branches, memory slips, projectile curvature, and exact cleanup. |
| Breezy Bash | Eighteen-tick two-stage Tempest Rite: bounded spherical capture, collision-safe lift/apex, independent slam revalidation, and safe Slow Falling release. |
| Invisibility | Infinite owned toggle with amplifier 255, no vanilla particles or effect icon, continuous energy drain, and counter-magic/revelation without removing unrelated invisibility. |
| Time Freeze | Owns Minecraft's true global tick freeze across loaded dimensions. Before activation it reports the exact per-second drain and safe whole seconds, with an advisory warning above 50 MSPT. Upkeep consumes at least 15% of full capacity each second; external `/tick freeze` ownership is preserved. |
| Forcefield | Gives the caster and compatible entities within two blocks independent finite-integrity wards. Ownership colour and three crack stages expose integrity; same-caster recasts repair/merge safely, crouching allies opt out, and the breaking ward sacrificially absorbs the complete overkill hit. It follows a mind traveller's vulnerable physical body. |
| Gravity Displacement | Five-second deterministic pull/orbit/repel field for up to 16 nearby permitted bodies, collision-safe steering, stable boss resistance and overlap arbitration, ranked collapse, projectile curvature, and Slow Falling release. Crouch-selection and artifact snapshots expose the server-owned mode. |
| Vessel Possession | Up to 30 seconds of server-owned movement, aim, jump, crouch, hotbar, and attack control over a consented player or suitable mob while the caster's body remains vulnerable. Mob AI is restored; higher-ranked players resist. Host death returns the controller under Divine Wrath. |
| Astral Projection | Bounded 150-block soul-form scouting that begins clear of the vulnerable skin-matched body, with validated return and no invulnerability shortcut. |
| Energy Drain | Two-second channel. Players lose energy and receive hidden-particle Exhaustion; mobs take repeated percentage-health damage plus a capped completion strike; the caster refills. |
| Ice Manipulation | Freezing ray that harms/freezes targets, water to ice, lava to obsidian, and protected snow placement. |
| Plant & Healing Acceleration | Normal use grows an aimed bonemealable plant. Crouching emits a zero-cooldown heal for the caster and injured players in an inclusive two-block radius. |
| Double Health | Owned maximum-health toggle with a real 2× baseline that grows to the authored 6× rank-10 capacity, continuous drain, proportional cleanup, a clear extra-heart-row pulse, heal-to-cap rules, anti-toggle healing lock, and no removal of unrelated attribute modifiers. |

Every innate has authored transformations at ranks 3, 6, 9, and 10 rather than only generic percentages. The exact 253 level profiles, capstone damage/range/duration, destruction tier, capacity, and transformation names are in [Innate power levels](docs/gameplay/innate-levels.md).

Two different players casting the same aligned innate within 12 blocks and 40 ticks create an **Umbral** or **Radiant Concord**: both regain the greater of 50 energy or 20% capacity, receive ten seconds of protection, and release a 48-damage aligned impact against nearby opposed beings. Each pair has a ten-second resonance cooldown.

## Energy, runestones, and Exhaustion

| System | Normal | Darkness |
| --- | ---: | ---: |
| Rank-0 capacity | 250 | 500 |
| Per-rank capacity | +52 | +135 |
| Rank-10 capacity | 770 | 1,850 |
| Baseline regeneration | 1/second | 2/second; 4/second at night or in the Dark Realm |

Sleeping fills the well unless Exhaustion blocks restoration. Every indefinite innate drains once per second. Exhausting a toggle removes it safely and inflicts a 70%-maximum-health magical backlash rather than leaving a free state active.

Runestones stack to 16, do not work through Exhaustion, restore 40–600 energy by tier, and recharge for 60–300 ticks. Inert and Dark tiny/small/medium/large/inscribed tiers have survival recipes and selected natural loot. Hidden helper-layer aliases remain save-safe but are not presented as gameplay items.

Soulstones and the Soul Matrix are auxiliary reservoirs of 200/400/800/1,600 energy. Use withdraws up to 100; crouch-use deposits up to 100. If the main well cannot fund a cast, carried reservoirs pay the exact shortfall atomically in inventory order; insufficient aggregate energy changes nothing.

## Rank Maze and progression

Light and Darkness each have 28-node persistent mazes spanning Might, Motion, Insight, Wardcraft, Veil, Communion, Dominion, and Darkness-only Abyss. `B` opens dedicated carved Light or blackstone Dark artwork; only the current alignment tree is visible. Players focus, unlock, or respec nodes without being forced into one mutually exclusive class. Respec costs 30 XP levels by default and retains earned numeric depth.

Each path has levels 0–10, canonical titles, alternate branch titles, visible chat prefixes, owned attribute perks, ceremonies, and power-specific transformations. Darkness players can hide their prefix with `/powers darkprefix`; alignment swaps retain the earned numeric floor while changing the visible maze and legal innate roster.

Light's canonical sequence is Dormant, Unawakened, Spark, Awakened, Adept, Weaver, Arcanist, Luminary, Voidcaller, Ascendant, Origin. Its branch titles are Ember-Blooded, Wind-Touched, Star-Listener, Sigil-Bearer, Veil-Stepper, Soul-Whisperer, Runeblade, Riftwalker, Pale Oracle, Confluence Weaver, Soulwarden, Crownless Magus, Ancient Hand, Sevenfold Paragon, Worldsinger, Voice of First Light, and Living Genesis. Darkness's canonical sequence is Unmarked, Murk, Shade, Umbra, Wraith, Revenant, Dread, Soulblight, Abyssal, Voidwight, Nightfall; its branches are Black-Fanged, Mist-Stalker, Grave-Echo, Dreadwright, Shroud-Born, Hollow Medium, Dusk Reaper, Nightstep, Bone Oracle, Eclipse Weaver, Gravewarden, Crownless Dreadlord, Elder Hunger, Sevenfold Harbinger, Voidsinger, The Last Shadow, and Living Extinction.

| Level | Light requirement: casts / power kills / memories / bosses | Darkness requirement: passive / villagers / wolves / baby villagers / golems |
| ---: | --- | --- |
| 1 | 100 / 10 / – / – | 25 / – / – / – / – |
| 2 | 300 / 50 / – / – | 100 / – / – / – / – |
| 3 | 750 / 150 / – / – | – / 60 / – / – / 8 |
| 4 | 1,500 / 300 / 1 / – | – / 70 / 40 / 10 / – |
| 5 | 2,500 / 500 / 2 / 1 | – / 85 / 60 / 12 / 11 |
| 6 | 4,000 / 800 / 3 / 2 | – / 125 / 100 / 20 / 15 |
| 7 | 6,000 / 1,200 / 4 / 4 | – / 200 / 175 / 35 / 20 |
| 8 | 8,500 / 1,800 / 5 / 7 | – / 300 / 250 / 50 / 30 |
| 9 | 12,000 / 2,500 / 6 / 12 | – / 400 / 375 / 75 / 40 |
| 10 | 18,000 / 4,000 / 6 / 25 | – / 500 / 500 / 100 / 50 |

Requirements are cumulative. A boss is an authored vanilla major boss or a living entity with at least 200 maximum health. Light memory sites supply explained progression memories. Exact node IDs, titles, parent links, perks, and unlock state are server-authored by the rank registries.

## Grimoires and practical spells

Crouch-use turns the selected page; normal use casts. A channel locks its item, source, target, position, and dimension, then revalidates life, range, line of sight, consent, amethyst, movement, damage, item loss, and realm policy at release. Interruption refunds half the activation cost. Spells use only the existing energy well—there is no essence or ritual-amplification resource.

| School and spell | Energy / cooldown / channel | Result |
| --- | --- | --- |
| Celestial — Soul Compass | 14 / 10s / instant | Remote view through one uniquely named loaded player or mob; player consent applies and the caster's body remains vulnerable. |
| Celestial — Augury of the Living Sky | 16 / 30s / 1s | Reports weather, moon, nearby force pressure, and time to the next realm event. |
| Celestial — Cartographer's Star | 24 / 60s / instant | Authenticated `biome <id>`, `structure <id>`, or `landmark <name>` search; bounded to 64 structure chunks or 4,096 biome blocks and never forces search chunks. |
| Celestial — Heavenfall: Celestial Ruin | 100 / 60m / 10s | Locks a point, then persists a one-minute catastrophe across logout/restart: a full-height, 100-block omen visible to distant observers and sustained through progressive 19×19 chunk preparation; 50,000 peak quadratic entity damage to 6,000 blocks; extreme knockback; a 120-block crater; 96 loaded-chunk fire scars; a three-second white flash; 20-second whiteout; and tinnitus. |
| Deep — Dimensional Anchor | 22 / 60s / 2s | Anchors a consent-valid living target against teleport and dimension movement. Renewals preserve the shared group circle, tether and authoritative remaining-duration feedback. This is the Deep Grimoire's only active spell. |
| Blight — Blood Reading | 12 / 10s / 1s | Reports health, maximum health, armour, alignment, and active effect IDs; player consent applies. |
| Blight — Grave Recall | 10 / 10s / instant | Reports the dimension and block coordinates of the caster's last retained death; in the same dimension it also gives a private compass bearing. Death memories expire after seven in-game days; legacy untimed records remain readable. |
| Wild — Purification Circle | 20 / 30s / 2.5s | Heals allies, removes ordinary harmful effects except Amethyst Poisoning, clears anchors, and severs Soul Links in eight blocks. |
| Wild — Verdant Tending | 22 / 30s / 2s | Bounded plant growth, farmland hydration, and local fire extinguishing. |
| Wild — Hearth Sanctuary | 28 / 50s / 2s | Gives every living entity in a strict three-block radius an independent 40-integrity sacrificial forcefield. |
| Abyssal — Ward-Breaking Ritual | 26 / 60s / 4s | Suppresses the aimed powered Amethyst Ward for 45 seconds without destroying it. |
| Abyssal — Dispel | 18 / 25s / 1s | Inspection names the exact nearest currently removable field before channel commitment; release revalidates that snapshot, then removes it and/or removable non-amethyst effects and anchor from a valid target. |

Infernal grimoire IDs are hidden, inert compatibility aliases for old saves. Recolour/Unbound aliases resolve to their supported schools rather than creating extra spell systems. Insight True Sight can reveal concealed path/rank information during a consented Soul Compass ritual, but never bypasses consent.

## Crystals

There are ten usable crystal items exposing 11 distinct actions. Multi-mode crystals change mode without paying energy or cooldown; ordinary use activates the selected action. Underlying convergence cooldowns are shared so item swapping cannot bypass recharge. Crystals are fireproof, single-stack, immune to despawn and mob pickup, and intentionally have no crafting recipes.

| Crystal | Actions |
| --- | --- |
| Red | **Inferno:** eight-second, 12-block firestorm with repeated permitted damage/ignition and no vanilla terrain explosion. |
| Orange | **Clone Swarm:** three unarmed, owner-skinned 80-health Echoes for 60 seconds. **Creativity Manifestation:** protected fixed orange-concrete/glass/glowstone chamber. |
| Yellow | **Size Shift:** alternates fixed 20-second `0.0625×` miniature and `10×` titan forms with corresponding movement/combat changes; separate from innate Size Morphing. |
| Green | **Life Bloom:** fully heals and cleanses living allies in 20 blocks. |
| Blue | **Chrono Stop:** toggles true global tick freeze, shows owner/deadline state plus a temporal-fracture edge, and auto-releases after one minute without upkeep. **Dreamwalking:** controls one uniquely named consented player or mob for up to 30 seconds through the vulnerable body channel; right-click again performs an authenticated emergency return even though the controller is spectating the host. |
| Indigo | **Middleworld:** persisted vulnerable group mind travel to the muted Between; use again to return each traveller to its recorded origin. Ordinary mobs remain attached to their caster's journey and are recalled even when realm terrain or movement separates them beyond the entry radius. |
| Violet | **Soul Link:** binds up to eight nearby souls for ten seconds, renders its topology, and mirrors later wounds under an independently visible remaining-damage cap per target without recursive or forcefield double-counting. |
| Rainbow | Six-mode convergence: Inferno, Clone Swarm, Size Shift, Life Bloom, Chrono Stop, or Soul Link. Crouch-use opens a non-pausing narrated radial selector backed only by a server-validated mode index. A Darkness holder receives the same item's corrupted model; the legacy infected item is a hidden inert save alias. |
| Light | Vulnerable group travel to the Light Realm for the caster and up to 15 living players, mobs, or Shadow bodies within two blocks. No consent or special group mode is required; each player receives an independent vulnerable body session and ordinary mobs retain a bounded return origin. |
| Dark | Equivalent automatic vulnerable group travel to the Dark Realm under corrupted presentation. |

## Mythic artifacts and combat UI

Crouch-right-click the Shadow Sword or Heavenly Partisan for a non-pausing eight-segment wheel. Hover and release crouch, click, or press `1`–`8` to select a favourite; release-to-cast is a separately persisted opt-in and defaults off. The centre opens a searchable icon library with Favourites, Innate, Crystals, and alignment tabs; entries show glyph, translated name, registry key, exact live cost/savings, cooldown progress, toggle, rank lock, and applicable size/gravity variant. Loadouts persist and retired keys migrate without duplicates.

### Shadow Sword

`powers:lycanbane` is retained as the compatibility ID, but the item is presented as the bold dark-grey **Shadow Sword**, made from Pure Darkness.

- A non-Darkness carrier cannot use it, receives hidden-particle Blindness II and Wither III, and periodically provokes lightning-marked Hollowed protectors.
- A Darkness wielder receives very fast rank-based energy restoration; rank 10 gains up to 900 energy/second through apotheosis.
- Starting an invocation requires the artifact in hand. A multi-tick or toggled invocation remains authorised while that artifact stays in a top-level inventory slot; moving it into a nested container, dropping it, losing alignment, dying, or disconnecting ends the state safely.
- It routes every one of the 23 innates and all 11 crystal actions using corrupted presentation and artifact—not innate—scaling.
- Darkness level 10 ignores existing artifact cooldowns and starts no new gameplay cooldown. Energy, validation, safe zones, amethyst, and workload budgets remain.
- Its default wheel is Lightning, Fireball, Teleport, Forcefield, Flight, Call the Hollowed, Blight Ground, and Nightfall Dominion.

Exactly three actions are sword-exclusive:

| Invocation | Rank | Effect |
| --- | ---: | --- |
| Call the Hollowed | 1 | Summons a capped owner-aligned squad of Darkness Creatures through lightning seals. |
| Blight Ground | 1 | Queues a protected six-block Darkness conversion beneath the wielder. |
| Nightfall Dominion | 10 | Toggleable Strength X, Resistance IV, Regeneration V, Fire Resistance, Speed IV, and a 24-block hostile Wither pressure aura. |

The Dark Herald always drops the sword and a large inscribed runestone. A rank-10 Darkness player can also crouch-use it on an Arcane Crucible surrounded by four cardinal Darkness blocks three spaces away and four diagonal Pure Light blocks two spaces away to awaken the First Vessel; anchors are consumed only after a successful spawn.

### Heavenly Partisan

The indestructible **Heavenly Partisan** is Pure Light's counterpart. Darkness carriers cannot use it, are revealed and damaged by radiant judgement, and call lightning-arriving Radiant Sentinels. It routes Flight, Starfall, Lightning, Thunderclap, Energy Beam, Forcefield, Plant Healing, Double Health, Creativity, Life Bloom, and Light-Realm travel. Its multi-tick invocations use the same top-level-inventory ownership rule as the Shadow Sword. Rank 10 reduces cooldowns by 60% and strengthens support/regeneration; it never inherits innate scaling.

| Dominion | Rank | Effect |
| --- | ---: | --- |
| Call the Radiant | 1 | Summons capped Radiant Sentinels. |
| Consecrate Ground | 2 | Queues protected Pure Light conversion. |
| Covenant Chain | 4 | Thirty-second allied healing/absorption and bounded sharing, or hostile binding; eight allies/owner. |
| Daybreak Wave | 5 | Damages Darkness, heals others, purifies removable effects, and clears unprotected projectiles. |
| Heaven Gate | 6 | One temporary owner-only pair of radiant gates. |
| Solar Firmament | 8 | Eight-second projectile/hostile repulsion and Light-aligned healing field. |
| Second Dawn | 9 | One five-minute single-use radiant death ward. |
| Host of Heaven | 10 | Two elites, a dominion field, consecration, and a cosmic heaven-beam ceremony. |

The Light Herald always drops the Partisan and Sacred Arcane Energy Dust.

## Shadow companion

A Darkness player carrying the Shadow Sword can press `G`, say `shadow, come`, or address any message beginning `shadow,`. Holding the sword alone never creates the old unwanted entity behind the player. Shadow continuously reads a bounded 60-second, 64-line, speaker-labelled view of public chat, even when several conversations overlap. Addressing it opens a 45-second owner-local dialogue focus: the owner's ordinary unprefixed follow-ups remain visible as normal public chat but can receive natural Shadow replies. Unprefixed chat can never authorize powers, combat, items, travel, or other actions; those still require an explicit `shadow,` command. A ten-tick reply cadence and 12-line answer snapshot bound work, while UUID-labelled speakers and owner-local persistent turns prevent equal names or simultaneous conversations from crossing.

Shadow is one server-authoritative, owner-skinned player-model body with no copied armour, held items, or inventory rendering. Hidden mode is owner-only, collisionless, invulnerable, and ticket-free. `shadow, reveal yourself` exposes that same mortal body and its replies globally; `shadow, hide yourself` restores privacy. Killing a revealed Shadow clears combat, tasks, energy state, and manifestation but preserves owner-keyed memory and bounded learning; the sword may recall it after 100 ticks with 25% energy.

Recognized tasks include follow, stay, guard/protect, stop, attack/fight a uniquely named target, use/cast a named power, close/skirmish/ranged preference, retrieve dropped items, conjure approved items, scout, explain a failed cast, reveal, hide, summon, and dismiss. It retrieves at most 64 candidates within 32 blocks and bounds task duration.

Shadow has its own 1,850-point Darkness well, natural Darkness/Pure Light/amethyst reactions, safe-zone combat policy, and max-Darkness access to all 23 entity-safe innates plus the three sword invocations—never crystal abilities. As embodied Darkness, Shadow's own travel is exempt from consent, route, and mindscape-departure restrictions: when within a travel cohort it follows across dimensions, its authoritative body is rebound after Minecraft replaces the entity, and it may return from the Dark Realm without a vulnerable proxy. Its planner evaluates at most 64 targets every ten ticks, casts at most once per 20 ticks, checks friendly fire and firing lanes, and selects close, skirmish, ranged, rescue, or recovery behavior. Owner-local contextual learning is capped at 64 contexts and 32 target profiles, adjusts choices by at most ±25%, explores safely at no more than 5%, and can be reset by an operator.

Conjuration permits ordinary approved materials and supplies, but forbids admin blocks, spawn eggs, the Shadow Sword, Partisan, and all crystals except the Dark Crystal. A Dark Crystal requires a full 1,850 energy, revealed stationary Shadow, a 60-second interruptible channel, and no duplicate in the player's inventory or ender chest.

The failure journal stores the latest 16 server-authoritative magic attempts for five minutes—only typed causes, never chat, coordinates, or entity IDs—so `shadow, why didn't that work?` can explain the real server rejection. Conversation memory is a bounded 24 redacted turns with 160-character lines. Fourteen datapack knowledge entries and registry/recipe context provide offline answers and never invent recipes. An optional OpenAI-compatible service may reword low-confidence non-recipe answers only; it receives bounded redacted context, is disabled by default, times out at 2.5 seconds, permits at most four global requests, and never replaces the authoritative cause.

## Relics, devices, weapons, food, and acquisition

The registry currently contains 260 gameplay/block rows. The exact ID, family, implemented purpose, acquisition route, recipe status, and hidden/save-alias state of every row is maintained in [the exhaustive item catalogue](docs/gameplay/item-catalogue.md). The [natural-acquisition audit](NATURALLY_UNOBTAINABLE_ITEMS.md) records no accidental gaps; deliberately deferred crystal recipes are not classified as accidental gaps.

| Family | Purpose |
| --- | --- |
| Rings and amulets | Inventory attunements with one-to-three recovery weight, combined energy cap six/second, and hidden-particle Resistance capped at II. |
| Soulstones / Soul Matrix | Persistent auxiliary energy reservoirs and atomic cast-shortfall payment. |
| Ritual Dagger | Its tooltip previews exact health cost, survival floor and energy gain. Use sacrifices four real health above that floor for 80 energy; armour, forcefields, and cancelled damage cannot make it free. |
| Five heart relics | Living Heart healing/regeneration; Wildwood stronger healing; Ghoul healing plus energy; Clockwork timed absorption; Bloodstone five-minute single lethal-damage ward. |
| Philosopher's Stone | Protected 30-energy transmutation: stone/cobble to iron ore, deepslate variants, netherrack to quartz, end stone to amethyst. |
| Lodestone / Miniportal | Bind a named same-dimension anchor; charged/empty model and durability tooltip expose two-charge state. Async return uses the first valid inventory anchor exactly and automatically carries the bounded nearby living cohort without consent. Empty Miniportal plus a dropped amethyst shard restores both charges. |
| Flute | Recalls, heals, and rebinds nearby player-shaped guardians under owner/global caps. |
| Empyrean Jewel | Pays one 40-energy surcharge to override consent-gated hostile forced movement, locator viewing, Dreamwalking, or possession without bypassing protections or safe zones. Automatic group-travel routes no longer need or charge for an override. The target receives a conspicuous permanent chat notice and the attempt enters the bounded operator audit. |
| Malignember | Its registry-derived tooltip lists every eligible destructive action and each artifact snapshot reports the actual saved energy. It reduces only those costs by 20%, never below one and never through rank scaling. |
| Stars, dusts, salts, fossils, jewels, stones, vessels, pages | Bounded Crucible XP/catalyst tiers, archaeology, and contextual Shadow lore; no essence economy. |

There are 82 non-mythic fantasy weapons across 12 real archetypes: Frostbound slows/weakens; Quicksteel grants speed; Reaper executes wounded targets; Crusher launches/weakens; Berserker adds damage/strength; Arcane returns energy; Vital heals; Radiant burns/reveals; Abyssal withers/lifesteals; Guardian shields; Hunter marks/poisons; Piercer deals armour-ignoring magic damage. Procs have cooldowns, hidden vanilla particles, authored magic FX, and amethyst/safe-zone suppression. Every ordinary weapon belongs to one low-chance additive survival loot family and can enter the Arcane Crucible; mythic artifacts cannot.

The 68 provisions use real nutrition/saturation and additive mob/block/structure acquisition. Raw staples normally restore 4 hunger and cooked staples 6. Darkness affinity turns ordinary cooked food into meagre nourishment plus hidden Hunger while foul/raw food becomes more sustaining. Ten cooked/smoked variants have recipes.

## Arcane Crucible

The two-input server-owned forge commits one atomic 40-tick transaction and guards stale menus, automation, break recovery, full inventories, duplicate components, and invalid outputs.

1. A Darkness block converts an eligible ordinary weapon into Nocturne, Calamity Blade, or Revenant's Gravecleaver; Pure Light offers Solstice, Valhakyra, or Zenith. Safe enchantments, name, lore, repair cost, components, and proportional durability are preserved.
2. An Animated Artifact Star binds zero-gameplay-cooldown aimed alignment lightning. Each cast still needs energy, line of sight, a valid target, and same-tick rate allowance.
3. A runestone adds 25/75/225/675 XP by tier; archaeology/school catalysts add 35–175. Overflow-safe exponential levels cap at 30; lightning caps at 120 damage to players and 1,200 to mobs.

Datapacks may add `#powers:arcane_crucible_base_weapons`; Java integrations can register eligibility/exclusions. Crystals, mythic identity, already-converted items, Shadow Sword, and Partisan are hard exclusions. The Crucible and Amethyst Ward have survival recipes.

## Blocks and living forces

POWERS registers Darkness, Pure Light, Amethyst Ward, and Arcane Crucible blocks.

Darkness and Pure Light are unbreakable living matter. Random ticks attempt two adjacent conversions by default, replacing only loaded, unprotected, vulnerable, fluid-free, non-block-entity terrain. Spread requires no kills. Mindscape palette, infrastructure, amethyst, unbreakable blocks, protection callbacks, and datapack immunity contain it.

Within the default eight-block aura, Darkness gives `darkness` players 24 energy/second before rank modifiers and inflicts hidden-particle Wither III on outsiders. Pure Light restores ordinary beings and harms Darkness. Dark Resurgence strengthens low-energy Darkness affinity. Finite Eclipse/Dawn Scars may manifest at most three temporary Hollowed/Sentinels each; the global invader cap is 64.

Contact between Darkness and Pure Light begins an extreme staged eclipse: up to 256 nearby living entities are damaged and thrown, then both forces—not unrelated blocks—are erased radially through a default 48-block sphere under a 4,096-check/tick global budget. A powered Amethyst Ward with solid amethyst exactly two blocks north, south, east, and west instead crystallizes loaded force through a six-block sphere under one 256-position/tick containment budget.

The powered Ward suppresses magic in a configurable default 20-block radius. Natural/tagged amethyst suppresses within six blocks; power use near it inflicts hidden-particle Amethyst Poisoning and 2.5 magic damage. Ward Breaking suppresses a powered Ward temporarily and excludes that ward from natural-amethyst poisoning for the same lease. Carried amethyst artifacts also suppress their own procs.

## Dimensions, mind travel, and confinement

POWERS adds `powers:light_realm`, `powers:dark_realm`, and `powers:middleworld`.

Light and Dark are mindscapes, not invulnerability dimensions. Crystal entry automatically captures the caster and up to 15 living entities within two blocks without consent. Each player leaves an independent vulnerable owner-skinned body at the exact physical origin while retaining normal game mode; ordinary mobs use a bounded caster-owned origin record and return with that journey even if separated inside the realm; Shadow travels as its real persistent body. Damage to a player's body mirrors to that player, and fatal body or avatar damage recalls and kills the physical player. Astral projection, possession, Dreamwalking, named-target travel, Middleworld, and mindscapes share one mutually exclusive session channel and lifecycle cleanup. A controlled host's death returns the controller with Divine Wrath instead of killing the controller.

Each force realm progressively constructs six protected persistent sites—Archive, Labyrinth, Shrine, Settlement, Font, and Herald Court—at a fixed hex around the first thought. Construction spends at most 128 edits per five-tick pulse. Force Pressure grows at 24/48/72-block tiers from entry; alignment restores/resists while intrusion drains and applies Weakness, Slowness, or Wither. Every 12-minute cycle ends in a two-minute Whiteout or Dark Eclipse. Courts respawn their defeated Herald after 20 minutes.

The Light Realm has a pure white sky without sun, moon, stars, or black void; the Dark Realm is an enclosed hostile thoughtscape; Middleworld has a muted Between biome. The realms preserve the Luminous Concord, Hollow Court, Amethyst Covenant, and Archivists of the Between cosmology through sites, bosses, item clues, and Shadow knowledge.

Ordinary player-controlled travel may move inside the current mindscape but cannot leave it directly; the mind must return to its body. Dark departure requires the `darkness` tag and Darkness level 5. Light departure requires level 5 in either progression. Shadow alone is exempt because it is the travelling embodiment rather than a projected mind. `/powers recover` is a separate operator-only corruption recovery route. Confinement uses bounded retries and enters a diagnosed locked Spectator holding state only when every recovery attempt fails; it is never an ordinary travel shortcut.

## Entities, guardians, and bosses

| Entity | Role |
| --- | --- |
| Darkness Creature / Hollowed | Player-shaped black hostile; naturally spawns in Dark Realm, targets non-Darkness, and uses authored lightning/fireball. |
| Radiant Sentinel | Light-aligned counterpart targeting Darkness. |
| Power Test Actor | Persistent player-compatible target with username `Test_<id>` or an operator-supplied unique name; supports powers that normally require player-like identity. |
| Echo Clone | Owner-skinned, unarmed, 80-health Orange Crystal summon with finite life/follow/combat. |
| Shadow Companion | Owner-skinned conversational/tactical Darkness participant described above. |
| Dark Herald — The Veiled Regent | 1,024 health faction boss, 26 armour, 12 toughness, 38 attack, ranged magic, 2,500 XP, and guaranteed Shadow Sword. |
| Light Herald — The Aureate Witness | Mirrored Light boss with guaranteed Heavenly Partisan. |
| First Vessel | Three-phase 5,000 virtual-vitality raid boss using all 23 entity-safe innate adapters, tactical range choice, Reconstitution, World-Suture, and Last Firmament. |

Common player-shaped guardians use 100 health, 12 armour, 16 attack, 0.32 movement, 48-block follow, and bounded ranged magic. Owner caps are four normal and two elite guardians; the server-wide guardian/invader cap is 64. Summons expire or clean up on owner/dimension failure.

The First Vessel scales virtual vitality by 55% per extra nearby participant up to 4×. Its phases are Waking Vessel above 70%, Broken Constellation at 70–35%, and Crownless God below 35%. Once below 50% it may channel a five-second Reconstitution, interrupted by 8% maximum damage, amethyst, or a Light dominion; below 15% it invokes Last Firmament. Its planner runs every ten ticks with at most 24 candidates and 40/28/18-tick cast intervals. It always drops a Miniportal, drops a Nether Star, and has a one-in-three Animated Artifact Star chance.

Spawn eggs exist for Darkness Creature, Power Test Actor, Radiant Sentinel, First Vessel, Dark Herald, and Light Herald. Echoes and Shadow are lifecycle-owned and have no eggs.

## Protection, consent, bodies, and lifecycle

Consent remains authoritative for hostile forced movement, locator viewing, Dreamwalking, and possession. Deliberate travel routes—Teleport, realm crystals, Miniportal return, and artifact gates—never ask consent: they carry at most 16 living entities total from the caster's two-block cohort. The Empyrean Jewel may pay to override the remaining consent gates only; it cannot override safe zones, protected terrain, ordinary-player realm confinement, world borders, anchors, amethyst, or operator policy.

Safe zones are dimension/position/radius records and protect damage, hostile movement, terrain, fields, and destructive rituals. Forcefields sacrifice themselves before overkill. Dimensional Anchor stops travel. Powered wards suppress casts. Time Freeze has explicit owner/external state arbitration. All toggle and artifact-owned states terminate on death, logout, power loss, item loss, lost authorization, dimension/session failure, or server stop. This remains true while global ticks are frozen.

Delayed magic and queued play packets retain stable UUID, dimension, deadline, cancellation-owner, and lifecycle-epoch data rather than player or level objects. They re-resolve the live subject at execution and cancel on logout, death/respawn, missing dimensions, replacement requests, and server shutdown/reload.

The live hostile-environment suite exercises those shared owners against external claim denial, an actual world border, solid low ceilings, exact void-floor travel, water and lava, nested mount/passenger graphs, physical Nether portals in ordinary and confined realms, and a real GameTest-only foreign dimension. Its [coverage and result matrix](docs/verification/evidence/2026-08-21-qa-010/README.md) records authoritative payment, mutation, and cleanup outcomes without changing direct-coordinate travel semantics.

The generated [lifecycle matrix](docs/interactions/lifecycle-matrix.csv) covers eight forms, six cast sources, and 14 termination events (672 outcomes). [Interaction rules](docs/interactions/interaction-rules.md) explain targeting, protection, forcefield, amethyst, terrain, body, and cleanup precedence.

## Magic interactions and presentation

The canonical registry contains 64 actions: 23 innate, 12 spells, 11 crystal, 13 artifact, three amethyst, and two living-force actions. The generated [action catalogue](docs/interactions/action-catalogue.md) documents every action; the [2,080-row matrix](docs/interactions/interaction-matrix.csv) covers every unordered pair including same-action resonance.

Server datapacks may add bounded retired-key aliases under `data/<namespace>/powers_actions/*.json`. A reload validates the complete alias graph before atomically publishing one monotonic action revision; failed reloads retain the prior snapshot. Artifact, grimoire, and Rainbow menus carry that revision and canonical action keys, reject stale or mismatched submissions without payment or mutation, and return one current snapshot. Active casts retain their captured definition and revision. See the [reload and network contract](docs/development/action-registry-reload.md).

Notable physical interactions include Energy Beam × Void Beam creating a bounded no-grief pressure blast, visual lightning at both casters, and a short celestial ring; fire × frost producing steam; Pure Light × Darkness producing eclipse annihilation; Void scar × Light tearing a star rift; purification severing Soul Link; forcefields sacrificing against catastrophic damage; amethyst grounding/suppressing magic; and aligned same-innate Concords. Projectiles, beams, fields, impact points, bodies, wards, forces, and spell presences use dimension/chunk indexes rather than whole-world searches.

Successful casts use anticipation, release, impact, and aftermath ceremonies. Bounded collision sigils identify counters/resonance once without chat spam. Eight authored particle sprites—mote, shard, glyph, ribbon, spark, eclipse, root, fracture—and 16 original mono Vorbis sound events include separate short beam/boss ringing cues rather than reusing Celestial Ruin tinnitus. Common failures/cooldowns use concise action-bar text; chat is reserved for unusual outcomes. Reduced motion substitutes static bounded geometry, and both server and client stop spawning visuals when their budgets are exhausted.

## Commands

Player commands:

```text
/powers list
/powers slots
/powers consent <teleport|locator|companion|dreamwalk|possession> <allow|deny>
/powers return
/powers path list
/powers path unlock <node>
/powers path focus <node>
/powers path respec
/powers darkprefix [true|false]
/powers reroll                    # only when allowSelfReroll is enabled
```

Operator commands:

```text
/powers slots <player>
/powers assign <player> <power> <0|1|2>
/powers reroll [player]
/powers reload
/powers recover <player>
/powers boss spawn
/powers diagnose
/powers diagnose export
/powers shadow learning reset <player>
/powers travel <namespaced-dimension>  # for example minecraft:the_nether
```

Administrator travel resolves the registered dimension, loads only its fixed entry chunk through a bounded temporary ticket, and arrives at the terrain surface without retaining a body proxy. It is an audited recovery control rather than an ordinary player escape route.

Optional Fabric Permissions API providers may grant the independent nodes `powers.command.diagnose`, `powers.command.testing`, `powers.command.travel`, `powers.command.assign`, `powers.command.recover`, and `powers.command.boss`; without a provider, the configured vanilla operator tier remains authoritative.

Testing commands are operator-only, executor-local, session-only, and never bypass protection, amethyst, targeting, realm gates, damage, permissions, or Time Stop:

```text
/powers testing [status]
/powers testing on|off|reset
/powers testing energy on|off
/powers testing cooldowns on|off
/powers testing refill
/powers testing actor spawn [username]
/powers testing arena [spawn|clear]
/powers testing coverage
/powers testing quest-telemetry
/powers testing quest-campaign start light|dark
/powers testing quest-campaign status|clear
/powers testing profile [status]
/powers testing profile start <minutes> <expectedPlayers>
/powers testing packets status|off|reset
/powers testing packets profile <delay150|delay300|loss1|loss5|duplicate|reorder> <seed>
```

Packet fault profiles affect only POWERS custom play payloads and remain disabled by default. Delay uses server ticks (three for 150 ms and six for 300 ms); loss, duplication, and reordering are deterministic for the supplied seed. Per-connection and global queue/work/lifetime limits fail closed, while selection snapshots converge by logical owner and discrete casts remain exactly-once. Disconnect, death, dimension transfer, testing reset, and server stop cancel queued work. Diagnostics expose only the profile, seed, and aggregate counters—never identities, payload contents, or coordinates.

The arena creates seven named acceptance targets: neutral/radiant/dark test actors, zombie, iron golem, Hollowed, and Radiant Sentinel. Coverage is derived from live registries so a newly added action cannot silently disappear from the manual test inventory. `/powers diagnose` reports fields, forced chunks, body proxies, Celestial events, per-dimension spatial-index work and memory, scan/work budgets, packets, particles, testing flags, packet-fault aggregates, cleanup state, active delayed-task owner/subject/dimension/purpose/deadline records, config-validation counts, bounded privileged-action audit totals, the effective global/world/dimension policy value and source for every overridable rule, and the executor's last 32 authoritative energy transactions reconciled by source. Crash reports add only bounded aggregate POWERS session counts and the latest typed failure reason/tick—never names, chat, IDs, coordinates, or remote content. `/powers diagnose export` atomically writes aggregate-only schema-v1 JSON to the world's `powers/diagnostics/latest.json`; it excludes chat, names, UUIDs, precise player coordinates, credentials, and remote-provider content.

Quest telemetry stores bounded, anonymous Light/Dark completion durations and route names; completed samples contain no player identity. Publication remains locked until each alignment/level has at least ten independent samples. The operator-only quest campaign command is an evidence tool: it accepts exactly ten fresh, purpose-named connected clients and replays server-authoritative deeds at documented human-equivalent game-tick cadences; it never inserts telemetry rows directly. The opt-in profiler records full server ticks, connected-player counts, authoritative network-cast success, work-budget peaks, p95/p99 MSPT, and sampled allocations to `profiles/*.json` and `profiles/*.jfr`; a run closes only after both its exact tick-sample count and corresponding wall duration are complete, and it has no recording/allocation overhead while inactive.

The accepted PRG-001 campaign used 20 real Fabric clients on commit `751b3bc`. Median cumulative progression was 8.17 hours for Light and 7.50 hours for Darkness; p90 was 9.33 and 8.47 hours. Every live rank interval exceeded five minutes. The complete 20-row publication and balance rationale are in [the PRG-001 evidence bundle](docs/verification/evidence/2026-08-14-prg-001-751b3bc/README.md).

## Configuration

The server file uses schema version 4. Values are sanitized at load; `/powers reload` reapplies policy and reports each bounded original-to-sanitized delta, reason, and active revision. Credentials, free-form strings, and safe-zone coordinates are represented only by redacted summaries.

| Key | Default | Meaning |
| --- | ---: | --- |
| `allowTerrainDamage` | `true` | Ordinary authored magic may alter allowed terrain. |
| `allowBlockEntityDamage` | `false` | Ordinary magic may affect block entities. |
| `allowSelfReroll` | `false` | Non-operators may reroll their own innate loadout. |
| `hostileForcedMovement` | `false` | Hostile player displacement without consent. |
| `requireTeleportConsent` | `true` | Hostile forced-movement consent gate; authored group-travel routes ignore it. |
| `requireLocatorConsent` | `true` | Player remote-view/locator consent gate. |
| `requireCompanionConsent` | `true` | Save-compatible legacy companion-consent policy; bounded group travel is automatic. |
| `requireDreamwalkConsent` | `true` | Dreamwalking consent gate. |
| `requirePossessionConsent` | `true` | Possession consent gate. |
| `projectionBodiesVulnerable` | `true` | Detached physical bodies mirror damage. |
| `persistCooldowns` | `true` | Cooldowns survive logout/restart. |
| `celestialRuinTerrainDamage` | `true` | Heavenfall excavates its authored crater/scars. |
| `celestialRuinBlockEntityDamage` | `true` | Heavenfall may destroy block entities inside its explicit catastrophic policy. |
| `wardRadius` | `20` | Powered Amethyst Ward suppression radius (1–64). |
| `maxParticlesPerTick` | `512` | Server visual packet budget (32–16,384). |
| `teleportMaxChunkDistance` | `8` | Teleport ticket search limit (1–128 chunks). |
| `rankRespecExperienceLevels` | `30` | Rank-maze respec price (0–1,000). |
| `adminPermissionLevel` | `2` | Operator command permission (0–4). |
| `safeZones` | `[]` | Up to 256 sanitized dimension/x/y/z/radius records. |

`policyOverrides` can patch the eleven world-sensitive boolean rules above for at most 128 exact world names and 128 namespaced dimensions. Resolution is field-by-field and deterministic: the global value is the base, an exact `worlds` entry patches it, then a matching `dimensions` entry patches that result. Omitted fields inherit. Safe-zone and external protection-adapter denials remain absolute regardless of an override.

```json
"policyOverrides": {
  "worlds": {
    "Lore World": { "allowTerrainDamage": false }
  },
  "dimensions": {
    "powers:dark_realm": {
      "allowTerrainDamage": true,
      "requireLocatorConsent": true
    }
  }
}
```

`livingForces` defaults to spreading enabled, two attempts, radius 8, Wither amplifier 2 (Wither III), 24 energy/second, clash radius 48, and 4,096 checks/tick. Bounds are enforced at load.

`dialogueProvider` defaults to disabled with blank endpoint/model, credential environment variable `POWERS_DIALOGUE_API_KEY`, 2,500 ms timeout, four global requests, and 30-second owner cooldown. Only HTTPS or loopback endpoints pass runtime validation; no network work runs on the server tick thread.

Back up a world before enabling or testing Celestial Ruin terrain/block-entity destruction or placing opposed living forces.

## Performance model

- Active living-force chunks use capped rotating queues around players/entities rather than whole-force scans.
- Wards, spell fields, magic presences, body proxies, guardians, and named entities are dimension/chunk indexed with bounded fallback scans.
- Fields pulse on staggered schedules with hard per-tick entity/projectile budgets.
- Rings, runes, spirals, beams, and whiteouts expand client-side from compact semantic packets.
- Body proxies share the smallest safe temporary ticket footprint; all travel tickets have deadlines and cleanup.
- Celestial Ruin persists coordinates and cursors but progressively loads only its 19×19 detonation area shortly before impact.
- Realm landmarks persist completion per site and build incrementally.
- Shadow planning, retrieval, learning, dialogue, and remote requests have independent hard bounds.
- Shadow and both guardian alignments share immutable per-level/per-cell observations for each server tick. Narrow firing lanes reuse only semantically complete captures, saturated prefixes cannot answer different bounds, and every selected UUID is re-resolved before live mutation. `/powers diagnose` reports query, hit, inspection, and cached-cell totals.
- Finite guardians persist only owner, task, archetype, and absolute expiry beside Minecraft's entity UUID. Owner/tier caps are reconstructed on load, expired or malformed records fail closed, and no navigation, targets, attributes, or index caches are serialized.
- Powered force-containment ceremonies share one hard 256-slot block-work cap across active dimensions under the current unanimous protection-policy snapshot. Both ceremony validation and block inspection consume bounded round-robin slots; repeated ward heartbeats are O(1) de-duplicated requests, protection denial remains absolute, and task counts above the cap cannot create an all-task scan.
- Ephemeral summons and visual entities are excluded from ordinary saves where appropriate.

The separately attributed CI soak exercises 10/50/100 simulated players and asserts packet, particle, scan, field, queue, forced-chunk, tactical-decision, and Java-25 per-thread allocation budgets. `./test.sh restart-soak` runs an isolated 24-hour repeated-restart harness by default and writes `build/restart-soak/restart-soak-report.json`; it never opens the ordinary `run/world`. The opt-in connected-bot GameTest profiles 10, 50, and 100 embedded connections for 30 minutes each and publishes JFR/JSON evidence. Pass `-PpowersProfilePopulation=10`, `50`, or `100` to run one population in an isolated exact-build archive; omitting it retains the sequential three-population run. The accepted exact-build embedded and ten-rendered-client results are recorded in [the PERF-001 evidence report](docs/verification/evidence/2026-08-14-perf-001/README.md).

Repeated semantic sustain visuals are coalesced once per tick by observer, dimension, chunk, action, and phase. Physical collisions and lifecycle transitions never use this path, and a saturated coalescer fails open instead of suppressing new effects. On exact commit `b2bff00`, a real Fabric client capture reduced a duplicate-heavy burst from 64 to 1 packet and from 3,776 to 59 encoded payload-body bytes (98.438% for both) while the dedicated collision proof and all 85 GameTests passed. The exact report and logs are in [the PERF-005 evidence bundle](docs/verification/evidence/2026-08-14-perf-005-b2bff00/README.md).

Semantic geometry now uses a reusable primitive transform buffer, and each visual event caches only its bounded observer variants. The exact 25,600-operation PERF-006 profile reduced allocation from 7,587.795 to 95.653 bytes per operation (98.739%) while p99 improved by 61.078%; retained caches remained bounded, both Java 25 JFRs lost no data, and all 86 Fabric GameTests passed. Exact recordings and results are in [the PERF-006 evidence bundle](docs/verification/evidence/2026-08-14-perf-006-9d2d31a/README.md).

The exact PERF-012 mixed-AI scene uses separated, moving Shadow/guardian/target positions over four independent planning cadences. Shared snapshots reduced identical legacy entity inspections from 540 to 348 (35.56%) while retaining the former ordinary scan caps; all 95 live GameTests passed. Exact results and boundary cases are in [the PERF-012 evidence bundle](docs/verification/evidence/2026-08-14-perf-012-3baa5da/README.md).

PERF-014 replaced legacy remaining-lifetime guardian saves with a compact absolute-deadline schema. Compact/legacy loading, unloaded expiry, duplicate lifecycle callbacks, loaded owner/tier changes, malformed records, authoritative attributes, and unowned cap isolation passed 1,431 JVM tests and all 100 live GameTests. Exact proof is in [the PERF-014 evidence bundle](docs/verification/evidence/2026-08-14-perf-014-836e691/README.md).

PERF-016 reproduced cross-dimension starvation after 102 ticks with 30 catastrophic Overworld ceremonies, then replaced global first-level scheduling with bounded fair lanes and task queues. The accepted scene advanced the ordinary Nether ceremony in one tick, retained claim denial, passed 1,438 JVM tests and all 100 ordinary live GameTests, and has exact proof in [the PERF-016 evidence bundle](docs/verification/evidence/2026-08-14-perf-016-4f85042/README.md).

PERF-015 measures actual Minecraft 26.2 framed and compression-aware Magic/Beam/Shape packets before choosing a transport. Ordered same-tick tails batch only when the measured wire shape is smaller, saving 55.524% at 256 B and 79.489% at 8 KiB under the normal 256-byte compression threshold while keeping lead cues immediate and collision state separate. Exact results and live-client ordering proof are in [the PERF-015 evidence bundle](docs/verification/evidence/2026-08-14-perf-015-65401e7/README.md).

PERF-013 gives semantic magic three server-authoritative distance tiers. Near observers retain full authored density, mid-range observers retain the complete geometry at reduced density, and far observers receive recognisable event silhouettes plus restrained layered audio. Beam diameter, rune/spiral extent, event colour, timing, and collision authority never change with LOD; authorised distant particles use Minecraft's always-visible path only after a per-observer range decision. Celestial Ruin keeps its 100-block column and 6,000-block event reach while using bounded density, tiered ringing gain, and independent overlapping-event audio state. Exact unit, live-server, and rendered-client proof is in [the PERF-013 evidence bundle](docs/verification/evidence/2026-08-14-perf-013-a88a039/README.md).

## Datapack and integration surfaces

Datapacks can extend recipes, loot, tags, `powers:knowledge_entries`, Crucible eligibility, living-force immunity, artifact conjuration policy, and authored data registries without replacing server validation. Java integrations may register Crucible predicates. Save migrations preserve legacy artifact keys, inactive grimoire IDs, hidden item aliases, rank/loadout state, cooldowns, body sessions, landmarks, Shadow memory/learning, and Celestial Ruin events where supported.

The exact generated appendices are:

- [Item catalogue](docs/gameplay/item-catalogue.md): all registered item/block rows, roles, and acquisition status.
- [Rank catalogue](docs/gameplay/rank-catalogue.md): all 56 Light/Dark maze nodes and exact costs, depths, branches, prerequisites, and titles.
- [Innate levels](docs/gameplay/innate-levels.md): every rank profile and transformation.
- [Action catalogue](docs/interactions/action-catalogue.md): all 64 canonical magic actions.
- [Interaction matrix](docs/interactions/interaction-matrix.csv): all 2,080 unordered action pairs.
- [Lifecycle matrix](docs/interactions/lifecycle-matrix.csv): all 672 form/source/termination outcomes.
- [System catalogue](docs/interactions/system-catalogue.md): collision and runtime systems.
- [Requirement matrix](docs/verification/final-requirement-matrix.md): prompt-to-evidence coverage without claiming hashes prove semantic review.

## Verification status

Current evidence is recorded in [the 2026-08-11 verification report](docs/verification/2026-08-11-release.md), [bounded P2 ledger](docs/verification/2026-08-11-bounded-p2-acceptance.md), [Queue 1 ledger](docs/verification/2026-08-11-queue-1-acceptance.md), [connected-player profile bundle](docs/verification/evidence/2026-08-14-perf-001/README.md), [quest telemetry bundle](docs/verification/evidence/2026-08-14-prg-001-751b3bc/README.md), [semantic-FX coalescing bundle](docs/verification/evidence/2026-08-14-perf-005-b2bff00/README.md), and [FX allocation bundle](docs/verification/evidence/2026-08-14-perf-006-9d2d31a/README.md), not inferred from documentation hashes. The 24-hour restart soak remains an explicit elapsed-time gate and is not represented as complete.

Useful checks:

```bash
./test.sh doctor
./test.sh check
./test.sh gametest
./test.sh soak
./test.sh restart-soak
./test.sh client
./test.sh server
```

The generated [manual acceptance checklist](docs/verification/manual-acceptance-checklist.md) covers every registered action, item, entity, screen, and command. Its exact-build live campaigns now provide a PASS and evidence path for every manual row; automated checks remain labelled separately and never impersonate that sign-off. Current P0/P1 closure evidence and deliberately pending duration gates are recorded in [the P0/P1 acceptance ledger](docs/verification/2026-08-11-p0-p1-acceptance.md).

## Lore and design boundary

The Luminous Concord seeks perfect order through healing or erasure. The Hollow Court dissolves identity into Darkness. The Amethyst Covenant contains both extremes. The Archivists of the Between preserve memories either force would overwrite. The First Vessel is their failed dual-force experiment; the Heralds are conscious verdicts; Shadow is a helpful reflection whose advice always carries a quiet self-serving pull toward Darkness.

This cosmology keeps the large-scale Light-versus-Darkness atmosphere that inspired the project while using original factions, places, artifacts, mechanics, and dialogue.

## Licence

Source code is available under the [MIT Licence](LICENSE).
