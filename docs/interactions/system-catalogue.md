# Runtime system and interaction catalogue

This document inventories the gameplay state that can meet at runtime and points to the exhaustive generated evidence. It describes implemented behaviour, not proposed features.

## Exhaustive evidence

- [`action-catalogue.md`](action-catalogue.md) defines all 64 server-authoritative magical actions: 23 innate powers, 12 spells, 11 crystals, 13 artifact actions, three amethyst counterforces, and two living realm forces.
- [`interaction-matrix.csv`](interaction-matrix.csv) resolves all 2,080 unordered action pairs, including same-action resonance. Every row owns mechanics, potency, duration, range, a visual motif, a semantic sound, and blocking state.
- [`lifecycle-matrix.csv`](lifecycle-matrix.csv) resolves all 672 combinations of eight forms, six cast sources, and fourteen termination events.
- [`interaction-rules.md`](interaction-rules.md) explains exceptional physical and policy families in readable form.

The matrices are generated from production policy and are checked byte-for-byte by the build. They do not claim that a CSV alone proves Minecraft integration; critical physical families are also exercised by live GameTests.

## Player and entity state

| System | Runtime state | Interaction invariant |
|---|---|---|
| Allegiance | Radiant/normal or Darkness tag | Governs innate assignment, rank maze, realm pressure, artifacts, guardians and progression. It never grants blanket immunity. |
| Rank maze | Alignment-specific nodes, levels 0–10 | Scales innate powers only. Spells and crystals remain fixed; artifacts use explicit artifact/apotheosis policy. |
| Energy | Capacity, current energy, regeneration and amethyst poison | All paid actions commit through one server transaction. Testing overrides bypass only the energy boundary, not protections or realm law. |
| Cooldowns | Per-action server timestamps | Testing overrides may ignore them. Rank-10 Darkness removes Shadow Sword cooldowns; no other source inherits that exception. |
| Toggles | Innate or source-owned ongoing state | Death removes all toggles. Power loss removes innate ownership. Dropping/losing an artifact removes only toggles routed through that artifact, even during global Time Stop. |
| Effects | POWERS-owned attributes and particle-hidden status effects | Cleanup removes only modifiers/effects owned by POWERS. Bespoke dust, rune, ribbon, fracture and sound cues replace ambient potion particles. |
| Protection | Safe zone, consent, amethyst, ward, forcefield, proxy, time lock, collision | Each target is revalidated before damage, movement or teleport. Sacrificial forcefields absorb an entire admitted hit even when it exceeds remaining integrity. |
| Target identity | Players, player-like test actors, mobs and unique custom names | Player-only semantics use the compatibility interface where meaningful; named lookup is indexed and refuses ambiguity. |

## Forms and bodies

The lifecycle policy covers physical players, Light/Dark realm avatars, astral avatars, teleport markers, possession controllers, dreamwalk controllers, hidden Shadow sessions and revealed Shadow bodies. Sources are none, innate, crystal, spell, Shadow Sword and Heavenly Partisan. Events are owner death, avatar fatality, body fatality, vessel fatality, source loss, power loss, energy exhaustion, suppression, expiry, unavailable target, invalid dimension, logout, server stop and manual end.

- A fatal hit to an astral, teleport or mindscape avatar recalls the mind to the recorded physical body, removes its ticket/proxy exactly once, then performs ordinary death and respawn there.
- A fatal hit to the frozen physical body follows the same recall-then-death sequence.
- A controlled vessel dying does not kill its controller. It returns the controller and invokes particle-hidden Divine Wrath: bounded non-lethal damage, energy loss, Weakness, Slowness and Darkness with a celestial fracture ceremony.
- A revealed Shadow is a globally visible, skin-matched, unequipped mortal mannequin. Killing it collapses only the current manifestation. Player-keyed diagnostic and lore memory survives, and a qualifying wielder may call Shadow again.
- Disconnect, respawn, dimension failure and server stop release runtime-only sessions, indexes, modifiers, client apparitions and chunk tickets.

## Travel and dimensions

Destinations pass world-border, collision, support, hazard, ward, dimensional-anchor, safe-zone and realm-law validation. Unloaded destinations use bounded temporary tickets and are revalidated after loading.

- Inside Light or Dark mindscapes, same-mindscape relocation is allowed by teleports, devices and powers.
- No player-controlled teleport, crystal, gateway, command or projection route may leave a Light/Dark avatar directly. The player must use the recorded body-return route and meet that realm's departure rank/alignment rule.
- Fatal soul recall returns before death even when ordinary departure is locked. The separate administrator recovery route exists for corrupt saves or broken destinations.
- Light/Dark crystals create vulnerable body proxies, enter their respective realms after bounded chunk loading, and return through body policy when used again.
- Middleworld, astral projection, teleport marking, dreamwalking and possession share proxy ownership but retain their distinct target and termination rules.

## Physical magic

Persistent fields, projectiles, beams, force blocks, wards and realm matter publish spatially indexed presences. Ordinary casts resolve the 2,080-pair policy before payment and commit residue only after gameplay success.

- Crossing Sunfire Energy Beam and Void Beam ray capsules terminate at their first intersection. A no-grief pressure blast damages nearby admitted bodies, visual lightning marks both casters, and a short celestial ring plays once under a per-tick collision cap and pair cooldown.
- Projectiles, fields and bodies independently check safe zones, consent, amethyst, powered wards, personal forcefields, time locks and solid geometry. One protection cannot be bypassed by chaining through another entity.
- Pure Light and Darkness contact starts a catastrophic, budgeted annihilation wave that destroys only those two force blocks. Amethyst and protected realm landmarks contain ordinary spread.
- Celestial Ruin persists its countdown and coordinates, progressively loads the finite crater area, emits the sky-beam/whiteout/ringing ceremony, destroys terrain under work budgets and applies distance-scaled entity damage beyond the crater.
- Time Freeze owns the server freeze only if it acquired it. External `/tick freeze` state is never unconditionally released, and artifact ownership reconciliation still runs while world ticks are frozen.

## World, progression and artifacts

Living-force spread, auras, Eclipse Scars, Whiteout/Dark Eclipse events, invasions, containment ceremonies, Heralds, landmarks and Celestial Ruin persist only the identifiers and coordinates required to resume safely. Chunk/entity scans use spatial indexes, rotating queues and hard tick caps rather than whole-world iteration.

The Shadow Sword exposes routed innate/crystal actions plus exactly three unique invocations: Call the Hollowed, Blight Ground and Nightfall Dominion. The Heavenly Partisan uses its curated Light roster. Artifact attunements, soulstones, ritual implements, vitality relics, transmutation, travel relics, the flute, archaeological reagents and ordinary weapons have their purpose/acquisition status in the main README; deliberately deferred crystal recipes remain absent.

## Resolution order for compound situations

1. Authenticate player, held source, selection, rank/alignment and testing permission.
2. Resolve the authoritative action ID and cast-source scaling policy.
3. Validate body/form state and mindscape travel law.
4. Resolve target identity, consent override artifact, range, sight and loaded-world safety.
5. Apply safe-zone, amethyst, ward, forcefield, proxy and time-lock counterplay.
6. Resolve nearby magic presences or physical ray collision.
7. Commit energy/cooldown only after the action can begin; channeled actions revalidate at release.
8. Apply bounded mechanics and semantic presentation.
9. Register only source-owned residue, toggles and cleanup hooks.
10. On death, loss, suppression, disconnect or shutdown, execute the lifecycle matrix outcome exactly once.

Unrelated systems deliberately coexist. For example, holding amethyst does not erase an unrelated vanilla effect, a failed teleport does not consume a Miniportal charge, and losing the Shadow Sword cannot remove an independently activated innate toggle.
