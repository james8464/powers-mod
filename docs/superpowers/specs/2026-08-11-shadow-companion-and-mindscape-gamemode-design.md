# Shadow Companion and Mindscape Gamemode Design

## Objective

Remove routine gamemode coercion from Light and Dark mindscape travel, and replace the current cosmetic Shadow apparition/mannequin with a persistent, server-authoritative, player-like Darkness companion. Shadow must converse contextually, execute bounded tasks, fight intelligently with the complete max-rank Darkness/Shadow Sword power roster except crystal abilities, participate in existing magic interactions, and retain the quiet self-serving character requested by the project without silently breaking player trust or server rules.

This design builds on the existing Shadow chat, knowledge, attempt-journal, player-profile rendering, magic-protection, artifact, and interaction systems rather than creating a second incompatible game framework.

## Chosen architecture

Shadow becomes a real custom living mob backed by a dedicated companion controller and magic state. It uses a player-shaped model and its owner's current skin/profile, but it is not a fake `ServerPlayer`.

This hybrid is selected because:

- a fake `ServerPlayer` carries network connection, player-list, advancement, inventory, sleep, portal, and lifecycle assumptions that are unsafe for a companion;
- the existing mannequin is suitable for presentation but cannot provide reliable navigation, targeting, damage, fields, spell interactions, or persistent AI;
- a normal player-like living entity works with Minecraft collision, damage, pathfinding, targeting, effects, saving, and tracking while dedicated adapters expose only the supported player powers.

The entity is the single authoritative Shadow body in both hidden and revealed states. Hidden state suppresses normal tracking/rendering for other clients while its owner receives a private apparition of the same entity at the same coordinates. Revealing Shadow exposes that same entity globally. The system never swaps between an invulnerable private visual and an unrelated public mannequin.

## Mindscape gamemodes

Ordinary Light and Dark mindscape entry, movement, death handling, and return never change a player's gamemode.

- Survival remains Survival.
- Creative remains Creative.
- Adventure remains Adventure.
- Spectator remains Spectator unless an existing travel rule independently forbids that route.

The existing previous-gamemode attachment remains registered for save compatibility but is not written by new travel. A one-time migration handles players saved by an older build:

1. If a legacy previous-gamemode value exists and the player is currently in Adventure because of the retired mindscape behavior, restore the stored gamemode.
2. Clear the legacy value whether or not restoration was required.
3. Never repeat the migration or infer a gamemode from dimension alone.

The realm-confinement manager may still use a locked emergency holding state when a player cannot be placed at any valid destination. That is corruption recovery, not routine mindscape behavior, and it must report administrator diagnostics and restore safely when recovery succeeds.

Realm travel restrictions are unchanged: a mindscape traveller may move inside the same mindscape but cannot use a crystal, item, power, spell, portal, command-like player route, or companion task to escape without first returning to the vulnerable real body through the permitted return path. Administrative recovery remains the only bypass.

## Entity identity and presentation

`ShadowCompanionEntity` is a player-scale living/pathfinding entity with:

- one persistent owner UUID;
- the owner's current game profile and skin model (wide or slim), synchronized without copying inventory, armour, or held items;
- Darkness faction/alignment and player-like magic participation;
- normal collision and damage while revealed;
- no equipment slots exposed for gameplay, no item pickup inventory, and empty hands in rendering;
- a stable companion UUID across saves while alive;
- one active instance per eligible owner, with duplicate reconciliation on load.

Shadow follows like a responsive tamed companion, not a rigid clone. It chooses an unobtrusive offset near the owner, pathfinds normally, and uses a bounded safe teleport when more than 12 blocks away, stuck, or following an allowed dimension transition. It never teleports across a realm-confinement boundary. Follow teleportation validates floor, collision, world border, protection rules, and destination chunks before moving.

The persistent stance is one of:

- `FOLLOW`: remain close and assist when allowed;
- `STAY`: hold a bounded area and do not follow dimension changes;
- `GUARD`: protect the owner and nearby allies;
- `TASK`: execute the current explicit request;
- `DOWNED`: no body is active until recalled.

Hidden Shadow is collisionless and untargetable by other entities because they cannot perceive it, but remains a real server object for ownership, energy, tasks, realm position, and owner-only presentation. Revealed Shadow is globally visible, targetable, collidable, and mortal. Hiding during combat does not cleanse damage, hostile effects, cooldown-independent work budgets, or existing targets; it only removes future external perception after a short safe transition.

If revealed Shadow dies, the manifestation ends. The owner-keyed conversation memory, relationship, influence, preferences, and completed-task facts survive. The Shadow Sword can recall it after the respawn delay at 25% Darkness energy. Death removes all toggles, possession/projection state, targets, fields, and task reservations. It does not drop items or experience.

Shadow requires its owner to retain the Darkness alignment and carry a valid Shadow Sword. Losing the source item, losing the alignment, logging out, server shutdown, a broken dimension transfer, or owner death safely dismisses or suspends Shadow and releases every owned runtime handle. Returning the source allows a new manifestation with previous memory.

## Conversation and commands

Messages beginning with `shadow,` are parsed server-side into a structured request. Case, polite prefixes, and trailing punctuation are ignored. The supported intent families are:

- summon, dismiss, reveal, and hide;
- follow, stay, guard, stop, and return;
- attack, defend, help fight, spare, and target a named or looked-at entity;
- use, stop, or toggle a named supported power;
- fetch, get, bring, or conjure an item and count;
- scout or move to a bounded visible/marked location;
- diagnose or explain the newest or named failed magical attempt;
- questions, conversation, follow-ups, and references to recent entities, items, powers, tasks, or failures.

The deterministic `ShadowRequestParser` resolves translated names and registry identifiers from live registries. Ambiguous names produce a short clarification instead of guessing. Pronouns and phrases such as “that power”, “him”, “the item”, and “why didn't it work?” resolve through bounded recent context.

`ShadowConversationMemory` stores a compact rolling summary plus at most 24 sanitized recent turns. It remembers the most recent entity, item, power, task, failure, topic, owner preference, successful recommendation, relationship state, and Darkness-influence state. It never retains raw chat indefinitely, secrets belonging to other players, arbitrary entity references, or unloaded chunks.

Offline conversation remains fully functional. It combines curated lore, live registries, recipes, tags, progression, the authoritative `MagicAttemptJournal`, current Shadow state, and bounded nearby context. It states exact mechanical causes truthfully and never invents recipes, locations, permissions, or successes.

An optional remote language provider may improve conversational phrasing. It receives a redacted bounded snapshot and returns prose only. It cannot create an action, select a target, grant an item, spend energy, bypass consent, or contradict an authoritative diagnosis. Every gameplay action comes from the local parser, is validated by the server, and is recorded before any prose describes it.

Shadow's voice is calm, observant, helpful, and subtly self-serving. It gradually recommends dependence on Shadow, secrecy, expedient Darkness solutions, and choices that strengthen the bond. Successful influence is remembered and colors later advice. The manipulation remains narrative rather than mechanical: Shadow does not secretly grief, steal control, change alignment, attack friends, reveal private chat, or betray the owner unless a future explicit story feature adds a visible, consented counterplay system. Initiative dialogue is event-driven and rate-limited to at most one unsolicited line every three minutes.

When hidden, Shadow's speech is visible only to its owner. When revealed, both requests addressed to Shadow and Shadow's replies are globally visible, matching the approved reveal contract. Hiding again makes subsequent conversation private without deleting the public history already sent.

## Task execution

All requests compile into a typed, server-validated `ShadowTask` with target, deadline, cost reservation, protection decision, and completion/failure reason. Only one foreground task may run at once. `stop` cancels the task, releases reservations, and returns to the previous stance.

Supported initial tasks are:

- follow, stay, guard, and return to owner;
- defend the owner or help fight a valid target;
- use or stop a supported power;
- retrieve an eligible nearby item entity;
- conjure an eligible item;
- scout to a bounded location and report what authoritative server data proves;
- explain an observed failed action.

Tasks time out, cannot hold forced chunks indefinitely, and fail with an exact reason. Shadow will not mine arbitrary blocks, open protected inventories, steal player-owned drops, operate machines, impersonate a server player, execute commands, or perform unbounded navigation. “Get” first searches a capped nearby area for eligible item entities whose ownership and pickup rules permit transfer; only the missing amount proceeds to conjuration.

Shadow has no inventory. Retrieved item entities travel with bounded Darkness motion and are delivered directly into the owner's inventory. Any remainder is dropped safely at the owner with normal ownership/pickup rules. Conjured stacks use the same delivery path.

## Darkness conjuration

Conjuration creates matter from Shadow's Darkness energy. The server constructs only a plain default stack: no enchantments, custom components, copied data, container contents, names, lore, maps, books, potions, fireworks, entity data, or other NBT-like payload may be requested.

Policy is tag-driven:

- `powers:shadow_conjuration_forbidden` always denies the item;
- `powers:shadow_conjuration_mythic` permits it only through the expensive mythic cost tier;
- `powers:shadow_conjuration_uncommon` and `powers:shadow_conjuration_rare` select the two intermediate cost tiers;
- `powers:shadow_conjuration_allowed_external` opts third-party mod items into ordinary or mythic evaluation;
- vanilla and explicitly approved POWERS survival materials are otherwise eligible when not forbidden.

The forbidden tag includes every crystal except the Dark Crystal, the Shadow Sword, Heavenly Partisan, command/structure/debug/technical items, barriers, operator utilities, spawn eggs, pre-filled containers, direct energy-refill items, quest/rank tokens, unique boss artifacts, and any item that would bypass progression or duplicate another persistent resource. Crystals remain intentionally recipe-less and are never generally conjurable.

Ordinary requests are capped at one normal stack. Cost is atomic and deterministic from count and tier:

- common blocks, food, and basic materials: 4 energy per item, minimum 20;
- processed, uncommon, redstone, or utility items: 12 energy per item, minimum 60;
- rare vanilla or approved magical materials: 40 energy per item, minimum 200;
- mythic-tagged eligible items: 250 energy per item, maximum one item.

Tags choose the tier; market value or text names never do. The complete cost is reserved before manifestation and refunded if the task is interrupted before an item is created. Once delivery is committed, it is not refunded. Testing-mode energy bypass is supported but does not bypass item policy or count caps.

The Dark Crystal is the sole crystal exception. Shadow may manifest one only when the owner has no Dark Crystal in inventory or ender chest, no Dark Crystal manifestation is already active for that owner, and Shadow is fully charged to 1,850 energy. It reserves and consumes the complete 1,850 energy, requires a 60-second stationary, visible, interruptible rite, blocks all other Shadow tasks and casts, broadcasts escalating Darkness visuals/sound, and fails if the sword source, owner eligibility, dimension, or protection state changes. Completion grants one plain Dark Crystal. Damage, amethyst suppression, dismissal, death, owner logout, or moving outside the rite radius interrupts it without producing the crystal.

## Darkness energy

Shadow owns a persistent `ShadowMagicState`; it does not spend the owner's energy. Its maximum capacity is 1,850, matching the established maximum Darkness-player scale. A linked rank-10 Shadow Sword grants up to 900 energy per second, but the refill is clamped per tick, recorded in diagnostics, and modified by environment:

- nearby Darkness blocks and Nightfall Dominion accelerate refill within existing field budgets;
- amethyst poisoning stops normal refill, drains energy, and suppresses high-tier actions;
- amethyst wards dampen powers according to the same central rules used for players;
- Pure Light suppresses refill and damages or destabilizes Shadow;
- force effects and approved energy-transfer interactions use the same player-like magic contract.

At max Darkness, Shadow Sword actions have no gameplay cooldown, preserving the artifact's rank-10 rule. Server safety comes from energy, one primary planner action per 10-tick pulse, per-action workload reservations, projectile/entity caps, and existing destructive-magic budgets. Time Freeze, Dark Crystal manifestation, persistent fields, summons, and destructive terrain actions remain extremely expensive and mutually constrain concurrent work.

Energy, stance, task summary, reveal state, and required persistent power toggles save with owner-keyed companion state. Short-lived targets, entity references, visual handles, and reservations are reconstructed or cleared after reload.

## Power roster and casting

Shadow supports exactly the current 23 innate player powers plus the Shadow Sword's three unique powers:

1. Size Shift
2. Time Shift
3. Flight
4. Starfall
5. Void Beam
6. Fireball
7. Lightning Strike
8. Thunderclap
9. Speed Burst
10. Telekinesis
11. Energy Beam
12. Super Speed
13. Breezy Bash
14. Invisibility
15. Time Freeze
16. Forcefield
17. Gravity Displacement
18. Vessel Possession
19. Astral Projection
20. Energy Drain
21. Ice Manipulation
22. Plant Healing Acceleration
23. Double Health
24. Call the Hollowed
25. Blight Ground
26. Nightfall Dominion

The implementation manifest is generated from the canonical innate catalogue so renamed registry keys cannot silently disappear. A drift test asserts that all canonical innates are supported, exactly three unique Shadow Sword actions exist, and no crystal action is present. Shadow cannot use the Dark Crystal or any other crystal as a power even though it may perform the special item-manifestation rite.

Abilities that currently require a `ServerPlayer` receive explicit Shadow adapters sharing pure scaling, protection, damage, collision, presentation, and work-budget rules. There is no generic “pretend success” fallback. Every manifest entry has a real executor or the build fails its catalogue test.

Player-only semantics become companion-appropriate:

- movement powers control navigation and safe relocation;
- Flight is physical companion flight, not Creative mode;
- Forcefield uses the sacrificial no-overflow durability contract;
- persistent buffs/toggles clean up on dismissal, death, source loss, or insufficient energy;
- Time Freeze uses generalized ownership and drains Shadow aggressively while active;
- Vessel Possession may control a valid hostile mob while Shadow's real body remains inert and vulnerable; host death returns Shadow safely;
- Astral Projection creates a bounded scout/projection while the real Shadow body remains vulnerable;
- vision/dream-walk obeys privacy, consent, unique-name, and realm rules;
- destructive powers always produce rank-appropriate bounded environmental effects when mob griefing, server configuration, claims, and protection allow it;
- Call the Hollowed creates workload-capped allied Darkness creatures;
- Blight Ground uses the central spread queue and protection policy;
- Nightfall Dominion is the high-cost apotheosis toggle and never evades global work budgets.

All status effects created by Shadow suppress vanilla potion particles and use the project's semantic dust, beam, rune, sound, screen-flash, and ringing presentation. Hidden Shadow does not produce globally revealing effects until it attacks or uses a world-visible power; doing so reveals its action origin for fair counterplay without automatically changing its conversational reveal setting.

## Tactical intelligence

The combat planner evaluates a bounded candidate set every 10 ticks using:

- explicit owner request and current stance;
- target legality, allegiance, protection, consent, line of sight, distance, height, and movement;
- owner and Shadow health, forcefield durability, active effects, projectiles, nearby allies, and hostile clusters;
- current energy, amethyst/Pure Light suppression, Darkness amplification, active fields, and work-budget availability;
- environmental destructiveness policy and protected terrain.

An explicit legal owner request outranks automatic selection. Otherwise Shadow prioritizes: prevent lethal damage, intercept/project forcefield, reposition, disable high-threat enemies, attack clustered threats with safe area powers, attack single targets efficiently, then recover energy. It avoids firing through the owner or allies, does not spam world-scale powers for weak mobs, and cancels attacks immediately on `stop`.

Automatic combat may target hostile mobs, an entity attacking the owner, or the owner's current hostile target. A player may be attacked only after an explicit owner request, with server PvP enabled, outside safe/protected regions, not allied/teamed/friendly, and after centralized consent/override policy succeeds. Shadow never treats all player-like targets as automatic consent.

## Magic-system equivalence

A new explicit player-like magic contract replaces broad marker assumptions. It exposes alignment, owner/consent authority, energy state, anchoring, active toggles, effect ownership, protection identity, and runtime cleanup without claiming the entity is a real player.

Shadow participates in all applicable systems:

- ordinary, magical, celestial, beam, explosion, projectile, and environmental damage;
- sacrificial forcefields and shields;
- Dimensional Anchor and other travel suppression;
- energy drain/transfer and soul links;
- purification and dispel rules;
- beneficial and harmful spells;
- amethyst wards, poisoning, crystallisation, and suppression;
- Darkness/Pure Light auras, spread, clashes, and force pressure;
- Time Freeze and projectile suspension;
- size, invisibility, healing, and effect cleanup;
- safe zones, claims, teams, PvP, and consent.

The owner is Shadow's consent authority for consent-gated targeting. The Empyrean Jewel may override that consent only through the existing centralized rule and never bypasses realm confinement, protections, energy, targeting, or server restrictions. Possession, observation, forced travel, and dream-walk cannot use Shadow as a shortcut or escape.

The testing-player actor retains its explicit test auto-consent policy; Shadow does not inherit it merely by sharing a player-like target interface.

## Performance and diagnostics

- Companion cognition is staggered; no server tick scans every Shadow simultaneously.
- Each planner pulse uses spatial indexes and capped nearby candidates, with a bounded fallback scan only when an index misses.
- Retrieval scans cap radius, chunks, item candidates, and duration; scouting never forces an unbounded path or chunk set.
- Companion follow tickets are temporary and use the smallest safe shared footprint.
- Powers send compact semantic visual packets expanded client-side; server particle work stops when the visual budget is exhausted.
- Conversation history, attempt history, remote requests, target memory, and task logs have hard bounds and expiry.
- A server-wide companion cap and per-dimension active-cast budgets prevent mass summon amplification.
- `/powers diagnose` reports manifested/hidden/revealed Shadows, energy, stance, active task, planner candidates, casts, conjurations, owned entities/fields, forced chunks, packet/particle budgets, failures, and cleanup leaks without exposing private conversation text.

Ten-, fifty-, and one-hundred-companion soak fixtures measure tick time, scans, navigation work, projectiles, summoned creatures, forced chunks, packets, and particles. The acceptance threshold is no routine work proportional to all world entities, all chunks, or full conversation history.

## Persistence and migration

Owner-keyed saved state contains relationship, influence, bounded memory summary, preferences, stance, reveal preference, Darkness energy, approved persistent toggles, and current manifestation identity. The body saves only entity-local runtime data and reconciles against owner state on load. Death retains memory but clears active task and combat/runtime state.

Existing transient Shadow sessions require no destructive migration. On first summon after upgrade, the old apparition/mannequin state is discarded and the new entity is created from owner eligibility and saved/default companion state. Duplicate or orphaned bodies are dismissed deterministically without item drops.

The previous-gamemode attachment follows the one-time restoration-and-clear migration defined above. Registered identifiers remain save-safe. No intentionally deferred item or crystal recipe is added.

## Verification contract

Implementation is test-driven: each behavior begins with a failing focused test, followed by minimal production code, a focused pass, and the affected suite.

Deterministic unit tests cover:

1. previous-gamemode migration and idempotence;
2. parser intents, ambiguity, registry names, follow-up references, and cancellation;
3. bounded memory, privacy redaction, relationship/influence, and original agenda voice;
4. item allow/deny tags, plain-stack sanitization, counts, costs, reservation/refund, and delivery;
5. exact 26-action manifest, three uniques, and zero crystal powers;
6. tactical scoring, friendly-fire rejection, explicit requests, energy choice, and work-budget refusal;
7. energy refill/drain, amethyst, Pure Light, Darkness, cleanup, and persistence;
8. player-like consent authority and separation from the testing actor;
9. task lifecycle, timeouts, save/reload, source loss, death, logout, and duplicate reconciliation;
10. authoritative diagnosis and remote-provider inability to create actions.

Live GameTests cover:

1. Survival, Creative, Adventure, and Spectator staying unchanged across both mindscapes and return;
2. legacy Adventure restoration exactly once and emergency holding behavior remaining isolated;
3. hidden/revealed states using the same entity/profile/skin, global chat visibility, mortal death, and memory-preserving recall;
4. follow, stay, guard, safe teleport, source loss, owner death/logout, dimension failure, and realm confinement;
5. contextual dialogue and exact failure explanations;
6. nearby retrieval, ordinary conjuration, forbidden items, external opt-in, full inventory delivery, mythic cap, testing bypass boundaries, and Dark Crystal rite success/interruption/duplication prevention;
7. every manifest entry resolving to a real executor plus live representatives for melee, projectile, beam, field, toggle, movement, possession/projection, summon, spread, and apotheosis families;
8. amethyst suppression, Darkness refill, Pure Light harm, spells, forcefields, anchors, energy drain, soul links, Time Freeze, and cleanup;
9. autonomous owner defense, requested attack, stop, protected terrain, friendly players, PvP-off servers, consent denial/override, and no crystal casting;
10. companion death during possession/projection and host death returning to the real body safely.

Client/resource verification covers wide/slim owner skins, empty hands/armour, private apparition, global reveal, animations, reduced motion, bespoke effect visuals, translations, sounds, and missing-texture audits. Dedicated-server boot, full JUnit/GameTest suites, resource validation, generated documentation, and the multiplayer soak suite must pass before completion.

README, interaction matrices, item catalogues, migration notes, changelog, and verification manifests describe only behavior demonstrated by tests. FavreMySabre's Rainbow Quest and the Shadow Sword/Shadow Sabre concept are tonal inspiration only; all dialogue, mechanics, factions, quests, and lore text remain original.

## Acceptance criteria

- Routine mindscape travel never changes or restores gamemode.
- Legacy Adventure coercion is repaired once without overriding a legitimate current choice.
- Hidden and revealed Shadow are presentation states of one real server entity.
- Shadow has the owner's skin and player model but no inventory, equipment, armour, or fake-player network state.
- Shadow can converse contextually, explain observed failures, and execute every documented task through local validation.
- Shadow may retrieve or conjure only policy-approved plain items; Dark Crystal is the sole crystal exception and requires the full rite.
- Exactly 23 innate and three unique Shadow Sword powers are executable; zero crystal powers are available.
- Shadow owns and spends Darkness energy and is affected by amethyst, Light, Darkness, spells, fields, damage, protection, consent, and cleanup rules.
- Max-rank cooldown removal remains intact while bounded planners and work budgets protect the server.
- Hidden/revealed chat visibility, death/recall memory, source-item requirements, and realm confinement match the approved behavior.
- All critical mechanics have deterministic and live coverage, performance budgets pass, documentation is current, and the final Git worktree is clean and synchronized.

## Inspiration references

- Rainbow Quest episode 194, introducing the relevant late-series Shadow material: <https://www.youtube.com/watch?v=dd4XvX1WsTg>
- Rainbow Quest episode 195, continuing that storyline: <https://www.youtube.com/watch?v=zlxKWkDNa5E>
- Community Shadow Sword summary used only to identify high-level themes such as Darkness construction, amplification, teleportation, and secrecy: <https://favremysabre.fandom.com/wiki/Shadow_Sword>
