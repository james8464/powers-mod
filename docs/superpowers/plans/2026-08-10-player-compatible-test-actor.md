# Player-Compatible Test Actor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Power Test Actor a persistent username and explicit player-target semantics, add safe operator testing bypass commands, prove the behavior, and launch the development client for manual testing.

**Architecture:** Keep the actor as a saved mob and express player-like target behavior through focused identity, classification, and transient-state services. Put testing overrides at the shared cooldown/energy boundaries so individual powers remain unaware of test mode. Preserve all protection and lifecycle rules.

**Tech Stack:** Java 25, Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Gradle/Loom 1.17.19, JUnit 6, Fabric GameTest.

## Global Constraints

- Do not construct or register a fake `ServerPlayer` or network connection.
- The actor may simulate target-side player mechanics, but never client input, inventory, permissions, chat, ranks, or advancement state.
- Testing bypasses require the configured administrator permission and are session-only.
- Testing bypasses never skip targeting, protection, amethyst, Time Stop, terrain, realm, or cast-validity checks.
- Follow strict RED-GREEN-REFACTOR for every production behavior.

---

### Task 1: Persistent test identity and classification

**Files:**
- Create: `src/main/java/com/powers/entity/TestActorIdentity.java`
- Create: `src/main/java/com/powers/entity/PlayerLikeTarget.java`
- Modify: `src/main/java/com/powers/entity/PowerTestActor.java`
- Test: `src/test/java/com/powers/entity/TestActorIdentityTest.java`
- Test: `src/test/java/com/powers/entity/PlayerLikeTargetTest.java`

**Interfaces:**
- Produces: `TestActorIdentity.defaultUsername(UUID)`, `normalize(String, UUID)`, `PlayerLikeTarget.isCompatible(LivingEntity)`, `username(LivingEntity)`, and `alwaysConsents(LivingEntity)`.
- The actor persists `PowersTestUsername` through `addAdditionalSaveData`/`readAdditionalSaveData`, exposes it through `testingUsername()`, and synchronizes it with the visible custom name.

- [ ] **Step 1: Write failing pure tests**

Prove UUID-derived names are deterministic, distinct, match `[A-Za-z0-9_]{1,16}`, explicit names are trimmed/normalized, invalid values recover to the default, ordinary mobs are not player-like, and the actor contract is the only always-consenting target.

- [ ] **Step 2: Run RED**

Run: `./gradlew test --tests 'com.powers.entity.TestActorIdentityTest' --tests 'com.powers.entity.PlayerLikeTargetTest'`

Expected: compilation fails because the two production types do not exist.

- [ ] **Step 3: Implement the minimal identity contract**

Use `Test_<8 lowercase UUID hex digits>` as the deterministic fallback. Normalize explicit names by trimming, replacing non-name characters with `_`, truncating to 16, and falling back when blank. `PlayerLikeTarget` recognizes `ServerPlayer` and `PowerTestActor`, but `alwaysConsents` returns true only for the actor.

```java
public interface PlayerLikeTarget {
	String testingUsername();
}

public static String defaultUsername(UUID id);
public static String normalize(String requested, UUID id);
public static boolean isCompatible(LivingEntity target);
public static boolean alwaysConsents(LivingEntity target);
public static String username(LivingEntity target);
```

- [ ] **Step 4: Persist and display the actor username**

Initialize after entity construction/spawn, override the custom-name update path so name tags become normalized testing usernames, and recover corrupt save data without throwing.

- [ ] **Step 5: Run GREEN and commit**

Run the focused tests and `./gradlew compileJava compileClientJava`, then commit with `feat: give test actors stable player identity`.

### Task 2: Player-oriented target state

**Files:**
- Create: `src/main/java/com/powers/entity/TestActorPowerState.java`
- Modify: `src/main/java/com/powers/power/abilities/DimensionalAnchorAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/EnergyDrainAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/ForcefieldAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/BreezyBashAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/EnergyBeamRayResolver.java`
- Modify: `src/main/java/com/powers/power/abilities/FireballImpactResolver.java`
- Modify: `src/main/java/com/powers/power/abilities/GravityDisplacementAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/GroundSlamImpactResolver.java`
- Modify: `src/main/java/com/powers/power/abilities/LightningStrikeImpactResolver.java`
- Modify: `src/main/java/com/powers/power/abilities/SpeedBurstAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/StarfallImpactResolver.java`
- Modify: `src/main/java/com/powers/power/abilities/SuperSpeedAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/TelekinesisAbility.java`
- Modify: `src/main/java/com/powers/power/abilities/VoidBeamAbility.java`
- Test: `src/test/java/com/powers/entity/TestActorPowerStateTest.java`
- Test: focused anchor, energy-drain, and forcefield rule tests

**Interfaces:**
- Produces: bounded simulated energy (`energy`, `drain`, `empty`, `restore`), transient dimensional anchor (`anchor`, `anchorDimension`, `clearAnchor`), `clear(UUID)`, and `clearAll()`.
- Existing `MagicShieldManager` remains the one UUID-keyed shield owner.

- [ ] **Step 1: Write RED tests for transient state**

Prove energy starts full, saturates between zero and its fixed test capacity, anchor expiry is exact, malformed dimensions clear safely, and cleanup removes all state.

- [ ] **Step 2: Run RED and implement the minimal manager**

Run the focused tests, confirm missing-symbol failure, then implement a server-thread-owned UUID map with no world scans or persistence.

```java
public static int energy(LivingEntity target);
public static int drain(LivingEntity target, int requested);
public static void empty(LivingEntity target);
public static void restore(LivingEntity target);
public static void anchor(LivingEntity target, ResourceKey<Level> dimension, long expiresAt);
public static ResourceKey<Level> anchorDimension(LivingEntity target, long currentTick);
public static void clear(UUID targetId);
public static void clearAll();
```

- [ ] **Step 3: Generalize target-side mechanics**

Accept `LivingEntity` in anchor application/query helpers; use real `PlayerPowers` for `ServerPlayer`, `TestActorPowerState` for the actor, and reject ordinary mobs where a player-like target is required. Route Energy Drain's player branch through the same classification and simulated pool. Share Forcefield with nearby compatible targets and make UUID-keyed shield checks apply to the actor in impact resolvers.

- [ ] **Step 4: Run focused GREEN tests and commit**

Run state, anchor, energy-drain, shield, and affected resolver tests; commit with `feat: simulate player target state on test actors`.

### Task 3: Named targeting, consent, and travel

**Files:**
- Modify: `src/main/java/com/powers/network/NamedLivingTargetIndex.java`
- Modify: `src/main/java/com/powers/protection/PowerProtection.java`
- Modify: `src/main/java/com/powers/network/LocatorSpellPackets.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `src/main/java/com/powers/power/abilities/TeleportAbility.java`
- Modify: `src/main/java/com/powers/power/travel/SafeDestinationResolver.java`
- Test: `src/test/java/com/powers/network/NamedTargetRulesTest.java`
- Test: `src/test/java/com/powers/protection/PlayerLikeConsentTest.java`
- Test: `src/test/java/com/powers/power/abilities/TestActorTravelRulesTest.java`

**Interfaces:**
- Named target resolution consumes `PlayerLikeTarget.username` and retains the existing ambiguity refusal.
- Travel validation gains a `LivingEntity` subject path while retaining realm rules only for real players and retaining bounds, chunks, collision, wards, safe zones, anchors, and anti-portal fields for both.

- [ ] **Step 1: Write RED tests**

Prove actor usernames resolve uniquely, duplicate names are ambiguous, actor consent is allowed only outside safe zones, and actor travel still rejects anchors, hazards, collisions, unloaded chunks, and protected destinations.

- [ ] **Step 2: Run RED and implement named/consent integration**

Track compatible actors by testing username even without a name tag. Keep real-player consent unchanged; only the actor supplies always-consent semantics.

```java
public static boolean mayTargetPlayerLike(ServerPlayer caster, LivingEntity target,
		boolean requireConsent, PlayerPowers.ConsentKind kind);
public static NamedTargetRules.Resolution<LivingEntity> findNamedTarget(
		MinecraftServer server, String requestedName);
```

- [ ] **Step 3: Implement actor targeting and relocation**

Let name-based locator and teleport subject lookup return a `LivingEntity`. Real players keep body/consent/player-list behavior; actors use direct entity teleport after the same destination and delayed-lifecycle revalidation. Marking through another entity's eyes remains caster-owned and accepts the actor as its target.

- [ ] **Step 4: Run focused GREEN tests and commit**

Run named-target, protection, travel, Dreamwalking, possession, anchor, and packet validation suites; commit with `feat: target test actors by username`.

### Task 4: Operator testing mode

**Files:**
- Create: `src/main/java/com/powers/testing/TestingOverrides.java`
- Create: `src/main/java/com/powers/testing/TestingOverrideRules.java`
- Modify: `src/main/java/com/powers/command/PowerCommand.java`
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`
- Modify: `src/main/java/com/powers/player/PlayerEnergyStorage.java`
- Modify: `src/main/java/com/powers/power/ActivationCooldowns.java`
- Modify: `src/main/java/com/powers/spell/SpellCastingManager.java`
- Modify: item cooldown call sites used by runes/artifacts
- Modify: `src/main/java/com/powers/PowersMod.java`
- Modify: `src/main/resources/assets/powers/lang/en_us.json`
- Test: `src/test/java/com/powers/testing/TestingOverrideRulesTest.java`
- Test: `src/test/java/com/powers/testing/TestingOverridesTest.java`
- Test: command registration/permission tests

**Interfaces:**
- Produces: immutable `TestingOverrides.State(boolean energyBypass, boolean cooldownBypass)`, `state(UUID)`, `setEnergy`, `setCooldowns`, `enableAll`, `disableAll`, `forget`, and `clear`.
- Shared payment/cooldown code queries the state by caster UUID.

- [ ] **Step 1: Write RED tests**

Prove bypasses are independent, default off, malformed changes cannot enable another player, cleanup is exact, energy payment succeeds without mutation only when enabled, and cooldown remaining/start behave as zero/no-op only when enabled.

- [ ] **Step 2: Run RED and implement the session registry**

Use a server-thread-owned UUID map. Remove default/off states instead of retaining empty entries.

```java
public record State(boolean energyBypass, boolean cooldownBypass) {
	public static final State DISABLED = new State(false, false);
}

public static State state(UUID playerId);
public static void setEnergy(UUID playerId, boolean enabled);
public static void setCooldowns(UUID playerId, boolean enabled);
public static void enableAll(UUID playerId);
public static void disableAll(UUID playerId);
public static void forget(UUID playerId);
public static void clear();
```

- [ ] **Step 3: Wire central boundaries**

Make `PlayerEnergyStorage.consume/drain` and ability/spell/item payment paths honor energy bypass for `ServerPlayer`; make `ActivationCooldowns`, spell cooldown checks/writes, crystal/artifact shared paths, and relevant vanilla item cooldown wrappers honor cooldown bypass. Do not bypass MagicUseGate or protection.

- [ ] **Step 4: Register operator commands**

Add the exact command tree from the design. Reuse `PowerCommand.isAdmin`, require a player executor for per-player state, sync the HUD after changes, make `refill` restore energy and clear saved/vanilla cooldowns, and spawn the actor at a collision-safe position with an optional word username.

```text
/powers testing on
/powers testing off
/powers testing status
/powers testing energy on|off
/powers testing cooldowns on|off
/powers testing refill
/powers testing actor spawn [username]
```

- [ ] **Step 5: Wire cleanup, run GREEN, and commit**

Call `TestingOverrides.forget(player.getUUID())` on disconnect and `clear()` on server stop. Run focused command, energy, cooldown, spell, crystal, artifact, and lifecycle tests; commit with `feat: add operator power testing mode`.

### Task 5: Live proof, documentation, and launch

**Files:**
- Modify: `src/gametest/java/com/powers/gametest/PowersGameTests.java`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Regenerate: `docs/quality/code-audit.md`

**Interfaces:**
- GameTests exercise real entity registration/world behavior; no source-text assertions.

- [ ] **Step 1: Add failing live GameTests**

Spawn two actors and prove distinct visible usernames; resolve one by name; apply an anchor; raise and sacrifice a shield against overkill; drain simulated energy; relocate one actor; start and end possession/Dreamwalking without stranding the caster.

- [ ] **Step 2: Run RED, finish only missing integration, then run GREEN**

Run `./gradlew runGameTest`, confirm each new test initially catches missing integration, implement the minimum missing bridge, and rerun until all live tests pass.

- [ ] **Step 3: Remove duplicate crystal ticking discovered during inspection**

Extract the per-tick runtime list into one focused coordinator testable by invocation count, prove one server tick invokes `CrystalPowerRegistry.tick` exactly once, then remove the duplicate call from `PowersMod`.

- [ ] **Step 4: Update player documentation and audit manifests**

Document actor usernames, target compatibility and limitations, every testing subcommand, operator/session safety, and how to leave testing mode. Regenerate Java/non-item manifests and generated magic documentation.

- [ ] **Step 5: Run final verification**

Run `./gradlew clean build --no-daemon`, `./gradlew javadoc`, both audit scripts with `--check`, `git diff --check`, and a dedicated-server boot/clean stop. Record the JAR checksum.

- [ ] **Step 6: Commit, push, and launch Minecraft**

Commit with `test: prove player-compatible test actor`, push `codex/powers-finalisation`, then run `./gradlew runClient` in a persistent PTY. Wait until the client log reports the title/menu ready state and leave the process running for manual testing.
