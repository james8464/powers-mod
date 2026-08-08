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

On first join, a player receives three distinct powers from a pool of 27. They persist with the player. The default keys are `V`, `X`, and `C`; they can be rebound in Minecraft's Controls screen.

The HUD shows the three powers as compact rune medallions with live toggle and rank-adjusted cooldown states. An authored ancient reliquary above the hotbar has separate normal, empty, darkness, amethyst-sealed, and projected-body treatments.

The assignable powers are:

- Movement and control: Flight, Super Speed, Speed Burst, Shadow Step, Time Shift, Telekinesis, Gravity Displacement, and Breezy Bash.
- Offence: Fireball, Lightning Strike, Energy Beam, Void Beam, Frost Nova, Ice Manipulation, Starfall, Ground Slam, Elemental Blast, and Energy Drain.
- Defence and support: Forcefield, Cozy Campfire, Plant and Healing Acceleration, and Double Health.
- Time and mind: Slow World, Time Freeze, Invisibility, Vessel Possession, and Astral Projection.

Telekinesis is a true radial release: permitted living targets are thrown away from the caster while up to 16 hostile projectiles are reflected along the caster's aim. If neither can be affected, its collapsed violet rune refunds the offered energy and starts no cooldown or collision residue.

Elemental Blast cycles flame, frost, storm, and earth only after a successful release. Each phase uses the underlying canonical Fireball, Frost Nova, Lightning Strike, or Ground Slam identity for collision counterplay, residue, sound, and ceremony rather than presenting every phase as the same composite force. Its HUD medallion adopts the primed element and shows the authoritative four-rune cycle before energy is spent.

Speed Burst is a synchronized physical dash, not a teleport. It predicts body-volume collisions, leaves an eight-tick cyan-white afterimage wake, and ends in a rank-scaled kinetic shockwave that respects safe zones, amethyst, and forced-movement consent without damaging terrain. Motion-ranked players can pay for one stronger Second Step during a 2.5-second window while the original persistent cooldown remains armed; alternating cyan-gold runes and an `II` medallion mark that server-authorized follow-up.

Every power uses the same energy well. Light progression grows it from 250 to 770; darkness progression grows its separate well from 500 to 1,850. Failed casts refund their activation cost. Toggle powers drain once per second and cause backlash if they exhaust the well. Sleeping and runestones restore energy unless Exhaustion is active.

Darkness and Pure Light are living realm matter. Vanilla random ticks make either block convert adjacent breakable terrain without requiring kills or loading distant chunks. Darkness withers nearby living entities without the `darkness` tag at strength III; tagged players instead receive a rank-scaled 24-energy pulse each second unless amethyst has poisoned their connection. Dark Resurgence strengthens that affinity by 50%, doubling it at or below one-quarter energy with a distinct eclipse awakening. When Pure Light touches Darkness, a catastrophic eclipse blast damages and throws exposed entities, then radially erases both forces from the epicentre through a 48-block sphere over several server-budgeted ticks without destroying unrelated builds.

## Mind travel and vulnerable bodies

Astral projection, vessel possession, dreamwalking, player-marking during Time Shift, and travel to the Light or Dark Realm leave a skin-matched Minecraft mannequin where the player's physical body remains.

- The spirit or mind moves; the body remains loaded and visible.
- Damage to the body is mirrored to the real player, so projection never grants invincibility.
- Death, disconnect, server shutdown, invalid state, or `/powers return` cleans up or restores the session safely.
- Return placement is collision-, border-, ward-, and safe-zone-validated.

The Light Realm and Dark Realm are mindscapes rather than ordinary destinations. Each contains six persistent memory sites with custom obelisks, lore, rewards, magical effects, and choices that feed the title maze. Middleworld has its own muted dream biome rather than borrowing the Light Realm's appearance.

## Rank maze

Advancements still determine earned rank depth, damage scaling, range scaling, and energy capacity. At each earned depth, players can also unlock and focus different connected titles instead of following one mutually exclusive class.

The light and darkness graphs each contain 28 nodes, including legacy titles and paths through might, motion, insight, wardcraft, veils, communion, and dominion. Converging paths create hybrid titles such as Runeblade, Riftwalker, Soulwarden, Eclipse Weaver, and their endgame forms.

Named branch variants have mechanical consequences as well as stronger ceremonies: Might empowers selected impacts, Motion grants second steps, Insight grants True Sight, Wardcraft reflects forcefields, Communion strengthens soul transfer, Veil reduces readable residue, Dominion deepens ancient mastery, and the Darkness path awakens Dark Resurgence.

Press `B` (rebindable) to open the synchronized Labyrinth of Names, inspect every connected title and perk, awaken reachable nodes, or attune a previously earned title. The server revalidates every click. The equivalent commands are `/powers path list`, `/powers path unlock <node>`, `/powers path focus <node>`, and `/powers path respec`; respeccing preserves earned depth and costs 30 experience levels by default.

Players with the `darkness` entity tag use the darkness advancement track and energy well. `/powers darkprefix` controls whether the focused darkness title is publicly shown.

## Grimoires and rituals

Sneak-use a grimoire to turn its pages; use it normally to cast the selected spell. Channeled rituals break if the caster moves, takes damage, changes books, or becomes amethyst-dampened, and return half of the offered energy.

| School | Spells |
| --- | --- |
| Celestial | Soul Compass, Mark of the Far Star, Tempest Sigil |
| Deep | Dimensional Anchor, Deepbinding Sigil, Seal of Closed Ways, Kinetic Ward |
| Blight | Crimson Transference, Threefold Hex, Veil of Unremembering |
| Wild | Circle of Cleansing Rain, Old-Root Binding, Sanctuary of the First Grove |
| Infernal | Ashen Threshold, Circle of Banishment, Leashed Hellfire |
| Abyssal | Amethyst Unmaking, Severing Word, Unweaving, Invocation of the Ninth Echo |

Soul Compass uses a server-authenticated target selection flow and honours locator consent. Insight-ranked True Sight can pierce either mindscape's normal path/rank veil during that consented ritual, signalled by a cyan-gold third-eye glyph, but cannot bypass consent or strip concealment. Dimensional Anchor is a Deep Grimoire spell, not a randomly assigned power. Counterspell, dispel, sanctuary, anti-portal fields, kinetic wards, and temporary ward suppression provide direct counterplay.

## Crystals

Every crystal has a bound ability or selectable convergence. Sneak-use turns a multi-mode crystal; normal use releases its selected force.

| Crystal | Ability |
| --- | --- |
| Red | Inferno |
| Orange | Cloning / Creativity Manifestation |
| Yellow | Size Shift |
| Green | Life Bloom / Space-Time control |
| Blue | Chrono Stop / Dreamwalking |
| Indigo | Portal Rift / Middleworld gateway |
| Violet | Soul Link |
| Rainbow | Sevenfold convergence of the chromatic forces |
| Infected Rainbow | Fractured convergence of Inferno, Chrono Stop, Portal Rift, and Soul Link |
| Light | Light Realm mind travel |
| Dark | Dark Realm mind travel |

Convergence cooldowns are shared with their underlying forces, so swapping crystals cannot bypass a rare ability's recharge. Crystal crafting recipes are deliberately not included; they are reserved for later progression design. The resource validator prevents accidental crystal recipes from entering a release.

## Counterplay and server safety

- Amethyst items, tagged amethyst blocks, and powered Amethyst Wards suppress powers without making normal melee damage harmless.
- Player-targeted teleportation, locating, companion travel, dreamwalking, and possession have per-player consent controls.
- Safe zones can block power harm, hostile movement, and terrain damage.
- Terrain and block-entity damage are disabled by default.
- Teleports validate world borders, loaded-distance limits, collisions, floors, wards, anchors, and destination dimensions.
- Temporary entities are marked ephemeral and excluded from saves.
- Time freezes use shared ownership, bounded radii, and safe restoration when effects overlap.
- Particles use a server-wide per-tick budget; amethyst scanning and state network syncs are cached.
- Custom food and mob drops are injected additively instead of replacing vanilla loot tables.

## Magic collisions and presentation

All 27 innate powers, 20 grimoire spells, 13 crystal actions, three amethyst suppressors, and two living realm forces participate in one canonical 65-action collision system. Every one of the 2,145 unordered same-or-cross-action pairs has a deterministic outcome, potency/duration/range adjustment, accessible shape cue, and semantic sound cue. Named high-impact combinations add mechanics such as steam pressure, eclipses that reveal concealment, realm-matter annihilation, projectile-consuming star rifts, summon banishment, soul-link purification, finite ward fracture, grounded storms, hostile pressure waves, and concordant healing.

The complete catalogue is in [`docs/interactions/action-catalogue.md`](docs/interactions/action-catalogue.md), and every possible pair is listed in [`docs/interactions/interaction-matrix.csv`](docs/interactions/interaction-matrix.csv). Every successful innate, crystal, and grimoire cast now receives a signature-driven anticipation, release, impact, and aftermath ceremony in addition to its bespoke gameplay effects. Flame fractures, frost shards, storms fork, time spirals, space and soul tether, life roots, and darkness eclipses; the server chooses the matching authored sound and both signature colours. Ritual glyphs form on the ground beneath the caster, while vertical glyph, eclipse, and lightning sigils face each observer locally instead of becoming edge-on. Ceremony radius, density, motion, volume, and pitch intensify at rank depths 4 and 8, with another bounded step for Ancient Mastery. Client effects use eight authored particle sprites, 13 original mono Vorbis sounds, distance culling, reduced-motion geometry and velocity clamps, and hard client/server particle budgets.

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
- `/powers reload`

## Configuration

The server creates `config/powers.json`. `/powers reload` applies valid changes without a restart and retains the previous configuration if parsing fails.

Important defaults:

| Setting | Default | Purpose |
| --- | ---: | --- |
| `allowTerrainDamage` | `false` | Allows power explosions and transformations to alter terrain |
| `allowBlockEntityDamage` | `false` | Allows powers to affect block entities |
| `hostileForcedMovement` | `false` | Lets hostile movement bypass normal consent rules |
| `require*Consent` | `true` | Enables each multiplayer consent boundary |
| `projectionBodiesVulnerable` | `true` | Mirrors mannequin-body damage to projected players |
| `persistCooldowns` | `true` | Saves cooldown deadlines across reconnects |
| `wardRadius` | `20` | Powered Amethyst Ward range |
| `maxParticlesPerTick` | `512` | Server-wide effect budget |
| `teleportMaxChunkDistance` | `8` | Maximum unloaded travel distance |
| `spaceTimeRadius` | `32` | Green Crystal freeze radius |
| `chronoStopRadius` | `64` | Indigo Crystal chrono-stop radius |
| `rankRespecExperienceLevels` | `30` | Rank-maze respec price |
| `livingForces.spreadingEnabled` | `true` | Enables random-tick Darkness and Pure Light conversion |
| `livingForces.spreadAttempts` | `2` | Face-adjacent conversion attempts per selected random tick |
| `livingForces.auraRadius` | `8` | Darkness affinity range around indexed blocks |
| `livingForces.energyRefillPerSecond` | `24` | Base darkness-tag energy pulse before rank scaling |
| `livingForces.clashRadius` | `48` | Realm-matter annihilation sphere radius |
| `livingForces.clashChecksPerTick` | `4096` | Maximum in-sphere clash positions processed per tick |
| `safeZones` | `[]` | Protected dimension-centred spheres |

A safe-zone entry has `dimension`, `x`, `y`, `z`, and `radius` fields.

## Building and verification

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
./gradlew clean test build
```

`check` includes strict validation of JSON, duplicate keys, PNG headers/alpha/dimensions, Ogg/Vorbis streams, particle and sound references, models, translations, namespace safety, dimension biomes, exhaustive interaction-document drift, every production Java source, all non-item assets, and intentionally absent crystal recipes. The release JAR is written to `build/libs/`.

For manual development runs, use `./test.sh client` or `./test.sh server`.

## Licence

The source code is available under the MIT licence. See [LICENSE](LICENSE).
