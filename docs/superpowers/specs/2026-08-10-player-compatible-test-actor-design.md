# Player-Compatible Test Actor Design

## Goal

Make the Power Test Actor a stable manual-test target for every power whose effect can meaningfully be applied to another living entity, including mechanics that normally distinguish players from ordinary mobs. The actor remains safe to spawn in single-player and dedicated-server worlds.

## Chosen architecture

The actor remains a normal `Mob`; it does not become a synthetic `ServerPlayer`. Fake players require a fabricated network connection, player-list registration, packet sinks, respawn handling, and logout cleanup. That would risk ghost tab-list entries, save corruption, and incompatibility with other mods merely to satisfy Java `instanceof` checks.

Instead, a focused player-test-target contract exposes:

- a persistent username;
- stable UUID identity;
- explicit always-consenting test semantics;
- eligibility for player-oriented target mechanics where no real client is required.

Central helpers decide whether a `LivingEntity` is a connected player, a compatible test actor, or an ordinary mob. Runtime systems consume that decision rather than adding scattered `instanceof PowerTestActor` shortcuts.

## Identity

- A newly spawned actor receives a deterministic default username derived from its UUID, using a valid Minecraft-style name no longer than 16 characters.
- The username is stored in entity save data and displayed as the actor's visible custom name.
- Renaming the actor with a name tag updates its testing username after normalization.
- Empty, malformed, or overlong saved names recover to the UUID-derived default.
- Named-target lookup refuses ambiguity when two loaded entities share a username.
- The actor does not appear in the tab list or claim a Mojang account/profile.

## Target compatibility

The actor is treated as a player-like target for mechanics that need target identity or player-specific counterplay:

- name-based lookup and locator results;
- Dreamwalking and Vessel Possession camera hosts;
- Dimensional Anchor application/removal;
- teleporting a selected subject and companion travel;
- shared forcefield membership and impact sacrifice;
- energy-drain player branch through a bounded simulated energy well;
- player-sensitive concealment, shield, amethyst, Time Stop, and projectile interactions where applicable.

Consent checks always allow the test actor outside configured safe zones. Safe zones, amethyst, forcefields, death/removal, dimension validity, line of sight, range, and all ordinary gameplay protections still apply so the actor remains useful for counterplay testing.

Mechanics that require an actual client, inventory, advancement tree, operator permission, chat receiver, or casting input are not fabricated on the actor. The human player remains the caster and uses the actor as the target.

## State and lifecycle

- Test-only target state is keyed by entity UUID and owned by a small manager.
- State is removed when the actor dies or is discarded and cleared when the server stops.
- Persistent identity lives with the entity; transient anchors, shields, simulated energy, and camera references do not outlive invalid entities.
- No forced chunks, fake connections, scheduled threads, or global entity scans are introduced.

## Manual usability

- The spawn egg creates an actor with a visible username immediately.
- Name-based screens and commands accept that username anywhere they already accept named living targets.
- Multiple actors receive distinct default usernames, enabling unambiguous multiplayer-style scenarios.
- The actor keeps player-scale rendering, equipment slots, movement, health, armour, knockback, and hostile-mob retaliation.

## Operator testing controls

Manual testing receives an operator-only, per-player session mode:

- `/powers testing on|off|status` changes or reports both bypasses;
- `/powers testing energy on|off` changes only energy payment and ongoing-drain bypass;
- `/powers testing cooldowns on|off` changes only ability, spell, crystal, artifact, and item recharge bypass;
- `/powers testing refill` restores the current energy well and clears saved ability/spell cooldowns;
- `/powers testing actor spawn [username]` creates a test actor at the executor with an optional normalized username.

Testing mode is runtime-only and clears on disconnect and server stop. It never changes advancement/rank, allegiance, permissions, protection, targeting, amethyst, Time Stop, casting validity, damage, or terrain rules. Bypasses are enforced at shared payment and cooldown boundaries rather than within individual abilities, and status changes are reported prominently so an operator cannot unknowingly continue ordinary survival play with testing enabled.

## Verification

Test-first coverage will prove:

- default usernames are valid, deterministic, distinct, and save-safe;
- renames normalize correctly and ambiguous names are rejected;
- player-oriented target classification includes only connected players and test actors;
- consent bypass applies only to the test actor and never bypasses safe zones;
- simulated target energy saturates and cleans up correctly;
- testing commands require the configured administrator permission;
- testing energy and cooldown bypasses are independent, central, runtime-only, and clean up on disconnect/server stop;
- live GameTests cover spawning/name visibility, named resolution, possession or Dreamwalking, Dimensional Anchor, forcefield overkill, energy drain, and teleport relocation;
- the full unit/GameTest/resource suite and dedicated-server boot remain green before the development client is launched.
