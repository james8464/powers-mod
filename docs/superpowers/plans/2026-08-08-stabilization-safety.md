# Stabilization and Multiplayer Safety Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the existing POWERS feature set compile, testable, deterministic, persistent, and safe on a multiplayer server.

**Architecture:** Introduce small pure policy helpers around the existing Fabric entrypoints, then route abilities and packets through those helpers. Persist state by absolute server-tick deadlines and give every mutable effect an owner so cleanup cannot remove unrelated state.

**Tech Stack:** Java 25, Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Loom 1.17.19, Gradle 9.5.1, JUnit Jupiter.

## Global Constraints

- Do not add recipes for crystals or other custom progression artifacts that intentionally lack them.
- Preserve all pre-existing working-tree edits and staged deletions.
- Treat every C2S payload as untrusted.
- Projection bodies remain vulnerable.
- Destructive powers default to no terrain or block-entity destruction.
- Use original names and assets; do not copy another property's assets or exact spells.

---

### Task 1: Reproducible build and test harness

**Files:**
- Modify: `gradle.properties`
- Modify: `build.gradle`
- Modify: `test.sh`
- Create: `src/test/java/com/powers/BuildBaselineTest.java`

**Interfaces:**
- Produces: JUnit Platform test task and `validatePowerResources` coverage for `.json` and `.mcmeta`.

- [ ] Add JUnit Jupiter to `testImplementation`, call `useJUnitPlatform()`, pin Loom to `1.17.19`, exclude `**/.DS_Store`, and make `check` depend on the expanded validator.
- [ ] Add a baseline test that asserts `PowersMod.MOD_ID.equals("powers")`.
- [ ] Run `./gradlew test`; confirm RED from the existing `getDayTime()` compilation error.
- [ ] Replace `getDayTime()` with the mapped 26.2 clock API and correct `JAVA_HOME` to `/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.
- [ ] Run `./gradlew test validatePowerResources`; require exit 0.
- [ ] Commit with `git commit -m "build: restore reproducible 26.2 verification" -- gradle.properties build.gradle test.sh src/test/java/com/powers/BuildBaselineTest.java src/main/java/com/powers/PowersMod.java`.

### Task 2: Food and targeting regression fixes

**Files:**
- Modify: `src/main/java/com/powers/mixin/FoodConsumeMixin.java`
- Modify: `src/main/java/com/powers/player/FoodAffinity.java`
- Modify: `src/main/java/com/powers/power/PowerTargeting.java`
- Modify: `src/main/java/com/powers/power/crystals/LightCrystalAbility.java`
- Modify: `src/main/java/com/powers/power/crystals/DarkCrystalAbility.java`
- Create: `src/test/java/com/powers/power/PowerTargetingMathTest.java`
- Create: `src/main/resources/data/powers/tags/item/food_normal.json`
- Create: `src/main/resources/data/powers/tags/item/food_abnormal.json`
- Create: `src/main/resources/data/powers/tags/item/food_neutral.json`

**Interfaces:**
- Produces: `PowerTargeting.nearest(HitResult block, EntityHitResult entity)` and tag-backed food classification.

- [ ] Write tests asserting an entity at squared distance 25 wins over a block at squared distance 36, the block wins at 16 versus 25, and a 48-block ray supplies max squared distance `2304.0`.
- [ ] Run the targeting test and confirm it fails because the helpers do not exist.
- [ ] Implement squared range and nearest-hit comparison; make both realm crystals use `PowerTargeting.findLivingTarget`.
- [ ] Change food classification to full identifiers/tags and modify the mixin so it changes nutrition/effects without cancelling vanilla stack consumption, listeners, criteria or game events.
- [ ] Add a GameTest/manual assertion that a stack of two becomes one and normal consumable effects still fire for a darkness player.
- [ ] Run focused tests, then `./gradlew test`.
- [ ] Commit with `git commit -m "fix: restore food consumption and accurate targeting" -- src/main/java/com/powers/mixin/FoodConsumeMixin.java src/main/java/com/powers/player/FoodAffinity.java src/main/java/com/powers/power/PowerTargeting.java src/main/java/com/powers/power/crystals/LightCrystalAbility.java src/main/java/com/powers/power/crystals/DarkCrystalAbility.java src/main/resources/data/powers/tags/item/food_normal.json src/main/resources/data/powers/tags/item/food_abnormal.json src/main/resources/data/powers/tags/item/food_neutral.json src/test/java/com/powers/power/PowerTargetingMathTest.java`.

### Task 3: Dedicated power damage type

**Files:**
- Modify: `src/main/java/com/powers/power/PowerDamage.java`
- Modify: offensive ability classes using vanilla damage sources
- Create: `src/main/resources/data/powers/damage_type/power_magic.json`
- Create: `src/main/resources/data/powers/tags/damage_type/power_damage.json`
- Create: `src/test/java/com/powers/power/PowerDamagePolicyTest.java`

**Interfaces:**
- Produces: `PowerDamage.source(ServerPlayer)` and `PowerDamage.isPowerDamage(DamageSource)` matching only `#powers:power_damage`.

- [ ] Write a policy test whose table marks `powers:power_magic` true and vanilla magic, indirect magic and freeze false.
- [ ] Confirm RED under the current broad predicate.
- [ ] Register/resolve the custom damage type and migrate Fireball, Lightning, Starfall, Frost Nova and punishments that are truly ability damage.
- [ ] Verify amethyst blocks custom power damage but not powder snow, potions, void or `/kill`.
- [ ] Run tests and commit only damage-related files.

### Task 4: Server configuration, consent, and protection policy

**Files:**
- Create: `src/main/java/com/powers/config/PowersConfig.java`
- Create: `src/main/java/com/powers/config/PowersConfigLoader.java`
- Create: `src/main/java/com/powers/protection/PowerProtection.java`
- Create: `src/main/java/com/powers/protection/ProtectionDecision.java`
- Create: `src/test/java/com/powers/config/PowersConfigTest.java`
- Modify: `src/main/java/com/powers/command/PowerCommand.java`

**Interfaces:**
- Produces: immutable config; `PowerProtection.mayAffectBlock`, `mayForceMove`, `mayLocate`, and `isSafeZone`.

- [ ] Write tests for safe defaults: terrain damage false, block-entity damage false, self-reroll false, persistent cooldown true, bodies vulnerable true, locator consent true and particle cap positive.
- [ ] Confirm RED because config classes are absent.
- [ ] Implement atomic JSON load/save under `config/powers.json`, retaining the last valid config on malformed reload.
- [ ] Add `/powers consent teleport|locator|companion <allow|deny>` and op-only `/powers reload`.
- [ ] Require permission level 2 for reroll unless `allowSelfReroll` is true.
- [ ] Route direct block mutation and forced-player movement through `PowerProtection`; never destroy block entities under defaults.
- [ ] Run tests and commit configuration/protection files.

### Task 5: Safe cast, packet authentication, and destination resolution

**Files:**
- Create: `src/main/java/com/powers/power/cast/CastContext.java`
- Create: `src/main/java/com/powers/power/cast/CastFailure.java`
- Create: `src/main/java/com/powers/power/cast/CastPolicy.java`
- Create: `src/main/java/com/powers/power/travel/SafeDestinationResolver.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `src/main/java/com/powers/power/abilities/TeleportAbility.java`
- Modify: `src/main/java/com/powers/item/CelestialGrimoireItem.java`
- Create: `src/test/java/com/powers/power/travel/SafeDestinationPolicyTest.java`

**Interfaces:**
- Produces: `SafeDestinationResolver.validate(ServerPlayer subject, ServerLevel target, Vec3 requested, TravelKind kind)` and authenticated cast contexts.

- [ ] Write policy tests covering maxY exclusivity, real world border, unloaded chunks, full collision box, fluids/hazards, wards, anchors, Middleworld, Dark Realm rank, safe zones and target consent.
- [ ] Confirm RED because the resolver is absent.
- [ ] Implement pure checks first, then Minecraft adapters; never generate a remote chunk solely to validate a client request.
- [ ] Make locator payloads valid only while the Celestial Grimoire is held and an expiring server nonce/session is active; rate-limit all input payloads.
- [ ] Revalidate destinations and participants at delayed execution time and refund only casts that never committed.
- [ ] Remove direct client authority over target dimension/item/ability identity.
- [ ] Run tests and commit cast/travel/network files.

### Task 6: Persistent cooldowns, anchors, and owned effects

**Files:**
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`
- Modify: `src/main/java/com/powers/power/ActivationCooldowns.java`
- Modify: `src/main/java/com/powers/power/abilities/DimensionalAnchorAbility.java`
- Create: `src/main/java/com/powers/power/state/OwnedPowerState.java`
- Modify: toggle/passive abilities
- Create: `src/test/java/com/powers/power/state/CooldownStateTest.java`

**Interfaces:**
- Produces: persisted absolute deadlines and owner-scoped cleanup operations.

- [ ] Write serialization tests proving cooldown and anchor deadlines survive reconstruction while expired values disappear.
- [ ] Confirm RED under static-map state.
- [ ] Persist deadlines in attachments and copy them through respawn according to death rules.
- [ ] Replace unconditional effect removal with owner UUID/modifier IDs and restore prior flight, invisibility, game mode and attributes only when POWERS changed them.
- [ ] Make `setSlots` deactivate every old toggle instance before replacing slots, then reactivate nothing implicitly.
- [ ] Verify relog cannot reset cooldown/anchor and reroll cannot leave invisible/flying/health-boosted ghosts.
- [ ] Run tests and commit state-related files.

### Task 7: Existing ability correctness sweep

**Files:**
- Modify: all files under `src/main/java/com/powers/power/abilities/`
- Modify: all bound files under `src/main/java/com/powers/power/crystals/`
- Create: `src/test/java/com/powers/power/AbilityArithmeticTest.java`

**Interfaces:**
- Consumes: cast policy, safe destination resolver, owned state and custom damage.
- Produces: consistent energy, duration, cooldown, damage and range semantics.

- [ ] Add arithmetic tests for Cozy Campfire exactly 200 ticks, Energy Drain reaching zero, scaled miss endpoints and mode-cycling with zero energy cost.
- [ ] Confirm failures under current implementations.
- [ ] Fix Cozy Campfire scheduling, Energy Drain remainder/sync, Plant Healing hit position, Shadow Step collision, Ice endpoint/range, Slow World ally filtering and crystal mode selection.
- [ ] Replace Ground Slam/Ice/Creativity block edits with protection-aware helpers and valid drops.
- [ ] Bound Space-Time to a configurable radius, add nonzero cooldown, reference-count frozen ownership, restore original state once, and remove misleading full-world claims.
- [ ] Fix Soul Link by updating all post-mirror health snapshots in the same tick and tagging mirrored damage to prevent re-entry.
- [ ] Make clone/projectile cleanup survive restart or make spawned entities non-persistent with bounded lifetime.
- [ ] Run all tests and commit in coherent ability groups.

### Task 8: Performance, chat, loot compatibility, and documentation

**Files:**
- Modify: `src/main/java/com/powers/PowersMod.java`
- Modify: `src/main/java/com/powers/power/AmethystDampening.java`
- Modify: `src/main/java/com/powers/fx/PowerFx.java`
- Delete after event migration: `src/main/resources/data/minecraft/loot_table/**`
- Modify: `README.md`

**Interfaces:**
- Produces: cached amethyst checks, deadline queue, dirty-state sync and additive loot injection.

- [ ] Add tests for deterministic delayed-task ordering and cache invalidation pure helpers.
- [ ] Replace cloned tick lists with iterators/priority queues and full-state periodic sync with dirty revision checks.
- [ ] Cache amethyst results by player block/chunk and invalidate on movement, inventory change and ward/block events.
- [ ] Replace vanilla loot-table overrides with `LootTableEvents.MODIFY` additions.
- [ ] Stop rebroadcasting signed chat as system messages; use player display/decorating support without cancelling messages.
- [ ] Remove stale translation typo and unused resource files proven unreachable; exclude `.DS_Store`.
- [ ] Rewrite README to match actual commands, persistence, damage, progression and intentional missing recipes.
- [ ] Run `./gradlew clean build validatePowerResources` and dedicated-server smoke startup.
- [ ] Commit compatibility/performance/docs changes.

### Task 9: Stabilization acceptance audit

**Files:**
- Create: `docs/verification/stabilization-audit.md`

**Interfaces:**
- Produces: requirement-to-evidence matrix for all original audit findings.

- [ ] Map every original issue to a source change and a test/manual command.
- [ ] Run the complete unit/GameTest/resource suite, client compile and server startup.
- [ ] Record exact commands and exit codes; leave any unproven item open.
- [ ] Commit the evidence document only after all stabilization items pass.
