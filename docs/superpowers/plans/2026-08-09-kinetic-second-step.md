# Kinetic Second Step Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish Speed Burst as a synchronized, collision-safe kinetic dash with an afterimage wake, endpoint shockwave, and one visible Motion-rank Second Step.

**Architecture:** Pure `SpeedBurstRules` derives bounded vectors, collision fractions, and follow-up timing. `SpeedBurstAbility` owns bounded server-thread traces/windows; generic ability hooks let the packet cooldown gate and synchronized HUD expose one legal reactivation without weakening the stored cooldown.

**Tech Stack:** Java 25, Minecraft 26.2 Mojang mappings, Fabric Loader/API, JUnit 6, Gradle Loom.

## Global Constraints

- Speed Burst remains physical movement, never a teleport.
- The normal persistent cooldown is armed on both first and follow-up casts.
- Every follow-up pays energy and passes suppression, freeze, collision, and interaction preparation normally.
- Shockwaves affect at most twelve living targets and never mutate terrain.
- Runtime owner state clears on respawn, disconnect, dimension change, death, rank loss, and server stop.
- All server and client particle work remains inside existing budgets.

---

### Task 1: Pure dash and Second Step rules

**Files:**
- Create: `src/main/java/com/powers/power/abilities/SpeedBurstRules.java`
- Create: `src/test/java/com/powers/power/abilities/SpeedBurstRulesTest.java`

**Interfaces:**
- Produces: `dashVector(Vec3, double, double, double)`, `lastSafeFraction(boolean...)`, `secondStepAvailable(long, long, long, boolean)`, and `secondStepRemaining(long, long, boolean)`.

- [ ] **Step 1: Write failing rule tests**

```java
assertEquals(new Vec3(1.32, 0.8, 0.0),
        SpeedBurstRules.dashVector(new Vec3(0.6, 0.8, 0.0), 2.2, -0.35, 0.8));
assertEquals(0.5, SpeedBurstRules.lastSafeFraction(true, true, false, true), 0.0001);
assertTrue(SpeedBurstRules.secondStepAvailable(102L, 150L, 102L, true));
assertFalse(SpeedBurstRules.secondStepAvailable(102L, 150L, 150L, true));
assertEquals(48, SpeedBurstRules.secondStepRemaining(150L, 102L, true));
```

Also assert null, zero, negative, NaN, positive/negative vertical caps, an immediately blocked sample, no samples, no mastery, pre-open time, and overflow-safe remaining time.

- [ ] **Step 2: Run the focused test and observe RED**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.power.abilities.SpeedBurstRulesTest'`

Expected: test compilation fails because `SpeedBurstRules` does not exist.

- [ ] **Step 3: Implement the pure rules**

```java
public static Vec3 dashVector(Vec3 look, double strength, double minimumY, double maximumY) {
    if (look == null || !finite(look) || !Double.isFinite(strength) || strength <= 0.0
            || !Double.isFinite(minimumY) || !Double.isFinite(maximumY) || minimumY > maximumY) {
        return Vec3.ZERO;
    }
    Vec3 normalized = look.normalize();
    return new Vec3(normalized.x * strength,
            Math.clamp(normalized.y * strength, minimumY, maximumY), normalized.z * strength);
}

public static double lastSafeFraction(boolean... clearSamples) {
    if (clearSamples == null || clearSamples.length == 0) return 0.0;
    int clear = 0;
    while (clear < clearSamples.length && clearSamples[clear]) clear++;
    return clear / (double) clearSamples.length;
}
```

Use `now >= opensAt && now < expiresAt` for availability and clamp remaining ticks to `0..Integer.MAX_VALUE`.

- [ ] **Step 4: Re-run the focused test and observe GREEN**

- [ ] **Step 5: Commit the pure contract**

```bash
git add src/main/java/com/powers/power/abilities/SpeedBurstRules.java src/test/java/com/powers/power/abilities/SpeedBurstRulesTest.java
git commit -m "feat: define kinetic dash rules"
```

### Task 2: Transactional reactivation and HUD state

**Files:**
- Modify: `src/main/java/com/powers/power/Ability.java`
- Modify: `src/main/java/com/powers/power/ActivationCooldowns.java`
- Create: `src/test/java/com/powers/power/ActivationCooldownsTest.java`
- Modify: `src/main/java/com/powers/network/PowerStatePayload.java`
- Modify: `src/test/java/com/powers/network/PowerStatePayloadTest.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `src/client/java/com/powers/client/ClientPowerState.java`
- Modify: `src/main/java/com/powers/hud/HudMath.java`
- Modify: `src/test/java/com/powers/hud/HudMathTest.java`
- Modify: `src/client/java/com/powers/client/PowerHudRenderer.java`

**Interfaces:**
- Consumes: future `Ability.mayReactivateDuringCooldown(...)` and `reactivationTicks(...)` overrides.
- Produces: generic cooldown bypass limited by `ActivationCooldowns.blocks(...)`; immutable slot-aligned `PowerStatePayload.reactivationTicks()`; cyan/gold `HudMath.secondStepRuneColor(...)`.

- [ ] **Step 1: Write failing cooldown, payload, and HUD tests**

```java
assertTrue(ActivationCooldowns.blocks(80, false));
assertFalse(ActivationCooldowns.blocks(80, true));
assertFalse(ActivationCooldowns.blocks(0, false));

PowerStatePayload payload = new PowerStatePayload(
        List.of("powers:speed_burst"), List.of(), List.of(80), List.of(140), List.of(42),
        200, 250, false, false, false, 0, List.of(), "", 0);
assertEquals(List.of(42), payload.reactivationTicks());

assertEquals(0xFFD7F8FF, HudMath.secondStepRuneColor(0, 0));
assertEquals(0xCCFFD166, HudMath.secondStepRuneColor(1, 0));
assertEquals(0xFFFFD166, HudMath.secondStepRuneColor(1, 4));
```

Extend the existing network round-trip and source-alias test with mutable reactivation ticks.

- [ ] **Step 2: Run the three focused test classes and observe RED**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.power.ActivationCooldownsTest' --tests 'com.powers.network.PowerStatePayloadTest' --tests 'com.powers.hud.HudMathTest'`

Expected: compilation fails on the missing gate, payload field, and HUD colour API.

- [ ] **Step 3: Add default ability hooks and the pure cooldown gate**

```java
public boolean mayReactivateDuringCooldown(ServerPlayer player,
        PlayerPowers.PlayerPowersData data, int remainingTicks) { return false; }

public int reactivationTicks(ServerPlayer player, PlayerPowers.PlayerPowersData data) { return 0; }

public static boolean blocks(int remainingTicks, boolean reactivationAllowed) {
    return remainingTicks > 0 && !reactivationAllowed;
}
```

In `PowersPackets.handleActivate`, compute the remaining cooldown once and reject only when `blocks(...)` is true. Do not move suppression, freeze, cast preparation, payment, execution, cooldown start, or commit out of their existing order.

- [ ] **Step 4: Extend synchronized payload and client mirror**

Add `List<Integer> reactivationTicks` after cooldown maximums in the record and codec, copy it in the compact constructor, and include it in the convenience constructor. `syncTo` emits exactly three non-negative values using each slot ability's hook. `ClientPowerState` copies, decrements, bounds-checks, and clears the values just like cooldowns.

- [ ] **Step 5: Render the legal Second Step state**

```java
public static int secondStepRuneColor(int rune, int tick) {
    int rgb = (rune & 1) == 0 ? 0xD7F8FF : 0xFFD166;
    int alpha = ((Math.floorDiv(tick, 4) + rune) & 1) == 0 ? 0xFF000000 : 0xCC000000;
    return alpha | rgb;
}
```

When a slot has positive reactivation ticks, render all twelve runes with this function, draw two small centre diamonds, show translatable `hud.powers.second_step` (`II`), and hide ordinary cooldown seconds.

- [ ] **Step 6: Re-run focused tests and observe GREEN**

- [ ] **Step 7: Commit the generic transaction/UI slice**

```bash
git add src/main/java/com/powers/power/Ability.java src/main/java/com/powers/power/ActivationCooldowns.java src/test/java/com/powers/power/ActivationCooldownsTest.java src/main/java/com/powers/network/PowerStatePayload.java src/test/java/com/powers/network/PowerStatePayloadTest.java src/main/java/com/powers/network/PowersPackets.java src/client/java/com/powers/client/ClientPowerState.java src/main/java/com/powers/hud/HudMath.java src/test/java/com/powers/hud/HudMathTest.java src/client/java/com/powers/client/PowerHudRenderer.java
git commit -m "feat: synchronize ranked power reactivation"
```

### Task 3: Server-owned kinetic trace, shockwave, and presentation

**Files:**
- Modify: `src/main/java/com/powers/power/abilities/SpeedBurstAbility.java`
- Modify: `src/main/java/com/powers/fx/PowerFx.java`
- Modify: `src/main/java/com/powers/PowersMod.java`
- Modify: `src/main/resources/assets/powers/lang/en_us.json`
- Modify: `README.md`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Consumes: Task 1 rules and Task 2 ability hooks.
- Produces: `SpeedBurstAbility.tickAll(MinecraftServer)`, `clear(UUID)`, and `clearAll()` lifecycle APIs; world-space speed wake, recovery, and impact FX.

- [ ] **Step 1: Implement owner-scoped state using only proven pure rules**

Add constants `BASE_STRENGTH = 2.2`, `SECOND_STEP_MULTIPLIER = 1.15`, `COLLISION_SAMPLES = 12`, `TRACE_TICKS = 8`, `SECOND_STEP_DELAY = 2`, `SECOND_STEP_WINDOW = 50`, `IMPACT_RADIUS = 3.0`, `MAX_IMPACT_TARGETS = 12`, `BASE_IMPACT_DAMAGE = 4.0F`, and `BASE_IMPACT_FORCE = 1.35`.

Store at most one immutable `DashTrace` and `SecondStepWindow` per UUID. Both records retain the originating dimension key; the trace also records whether collision prediction found an obstruction. The activation flow must:

```java
boolean followUp = consumeAvailableSecondStep(player, now);
double strength = BASE_STRENGTH * Math.min(1.35, scaling(player).rangeMultiplier())
        * (followUp ? SECOND_STEP_MULTIPLIER : 1.0);
Vec3 impulse = SpeedBurstRules.dashVector(player.getLookAngle(), strength, -0.35, 0.80);
double safeFraction = collisionFraction(level, player, impulse);
player.setDeltaMovement(impulse.scale(safeFraction));
player.hurtMarked = true;
player.fallDistance = 0.0F;
```

Conclude any replaced trace once, install the new trace, add Slow Falling, and create a new window only after a first cast with current `second_step` mastery.

- [ ] **Step 2: Implement bounded trace completion and shockwave**

`tickAll` removes dead, missing, dimension-changed, expired, or collided traces. Each live trace emits one wake from its last position to the current position and is replaced with decremented immutable state. Completion sorts nearby valid living entities by squared distance, limits to twelve, applies power damage only through `PowerProtection.mayHarm`, and pushes only through `PowerProtection.mayForceMove`; amethyst-dampened targets receive neither.

Set `hurtMarked` on moved targets. Use the normalized horizontal center-to-target direction with an upward component capped at `0.35`. Expired windows are evicted on the same server tick.

- [ ] **Step 3: Add bounded bespoke presentation**

Add `PowerFx.speedBurstRelease`, `speedBurstWake`, `secondStepReady`, and `speedBurstImpact`. Use cyan-white/gold entity-effect colours, electric sparks, clouds, enchanted motes, two counter-rotating runes, and layered rocket/sonic/anchor sounds. Every particle call must continue through `PowerFx.burst` or existing geometry helpers.

Add three lore variants under `ability.powers.speed_burst.second_step.*` and the HUD key `hud.powers.second_step`.

- [ ] **Step 4: Wire lifecycle and ticking**

Call `SpeedBurstAbility.clear(...)` during respawn and disconnect, `clearAll()` during server stop, and `tickAll(server)` once per end-server tick. Keep `PowersMod.java` below the source-audit line cap.

- [ ] **Step 5: Update player-facing documentation**

Describe collision prediction, the afterimage shockwave, persistent cooldown, paid Second Step, safe-zone/consent behavior, and the paired HUD state in `README.md` and `CHANGELOG.md`.

- [ ] **Step 6: Run focused tests and compile both source sets**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.power.abilities.SpeedBurstRulesTest' --tests 'com.powers.power.ActivationCooldownsTest' --tests 'com.powers.network.PowerStatePayloadTest' --tests 'com.powers.hud.HudMathTest' compileJava compileClientJava`

- [ ] **Step 7: Regenerate audits and commit the runtime slice**

Run: `python3 scripts/audit_java_sources.py` and `python3 scripts/audit_non_item_assets.py`.

```bash
git add CHANGELOG.md README.md docs/quality/code-audit.md docs/quality/asset-audit.md src/main/java/com/powers/power/abilities/SpeedBurstAbility.java src/main/java/com/powers/fx/PowerFx.java src/main/java/com/powers/PowersMod.java src/main/resources/assets/powers/lang/en_us.json
git commit -m "feat: unleash kinetic second steps"
```

### Task 4: Release verification

**Files:**
- Verify only; modify production files only if a failing gate reveals a tested defect.

**Interfaces:**
- Proves all prior interfaces compile and run together on Minecraft 26.2 dedicated-server code paths.

- [ ] **Step 1: Run repository audit checks**

Run `python3 scripts/audit_java_sources.py --check` and `python3 scripts/audit_non_item_assets.py --check`.

- [ ] **Step 2: Run the clean release gate**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew clean check build`

Expected: all fourteen tasks execute successfully, including production/client compilation, JUnit, resource validation, interaction-document drift, source audit, and asset audit.

- [ ] **Step 3: Smoke-test a fresh dedicated server**

Temporarily disable RCON in ignored `run/server.properties`, start `runServer` with a `mktemp -d` universe and port `0`, wait for `Done`, send `stop`, require exit code `0` and all six dimensions saved, then restore RCON exactly.

- [ ] **Step 4: Review and seal the tranche**

Run `git diff --check`, inspect the complete staged diff, verify `run/server.properties` is restored, commit any final verified audit outputs, and leave `git status --short` empty.
