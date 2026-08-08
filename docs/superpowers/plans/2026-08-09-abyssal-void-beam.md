# Abyssal Void Beam Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the instant first-target Void Beam with a server-owned charged penetrating ray, explicit light/ward/amethyst counterplay, and a bounded interactive void scar.

**Architecture:** `VoidBeamRules` owns deterministic timing, penetration, falloff, target selection, and segment/sphere geometry. `VoidBeamAbility` owns one short charge per caster and resolves authoritative hitscan release; `VoidScarManager` owns bounded aftermath state. `SpellFieldManager` exposes only the nearest hostile ward-ray intercept, while `PowerFx` renders every semantic phase through existing budgeted particles and sounds.

**Tech Stack:** Java 25, Minecraft Java 26.2 Mojang mappings, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, JUnit 5, Gradle/Loom.

## Global Constraints

- Keep all gameplay mutation on the logical server and derive aim, owner, rank, dimension, and timing from server state.
- Do not add client-selected action IDs, new packets, persistent entities, terrain mutation, recipes, or new external dependencies.
- Preserve the ordinary energy/cooldown/collision transaction; interrupted committed charges are not refunded.
- Hard-cap penetration at five, active scars at 128, scar pulse targets at 16, radius at four blocks, and duration at 160 ticks.
- Respect amethyst, Pure Light, safe zones, Sanctuary, Kinetic Ward, personal forcefields, lifecycle cleanup, and the shared FX budget.

---

### Task 1: Pure Void Beam rules

**Files:**
- Create: `src/main/java/com/powers/power/abilities/VoidBeamRules.java`
- Create: `src/test/java/com/powers/power/abilities/VoidBeamRulesTest.java`

**Interfaces:**
- Produces: `chargeRemaining(long,long)`, `penetrationLimit(boolean,boolean)`, `damageMultiplier(int)`, `selectPenetrations(Collection<RayCandidate<T>>,double,int)`, `segmentSphereEntry(...)`, `shouldRenderScar(int)`, `shouldPulseScar(int)`, `scarRadius(double)`, `scarDuration(int,boolean)`, and `Counterplay`.

- [ ] **Step 1: Write timing, penetration, and falloff tests**

```java
@Test void chargeOpensOnlyAtTheTwelfthTick() {
    assertEquals(1, VoidBeamRules.chargeRemaining(100, 111));
    assertEquals(0, VoidBeamRules.chargeRemaining(100, 112));
}

@Test void rankVariantsAddFinitePenetration() {
    assertEquals(3, VoidBeamRules.penetrationLimit(false, false));
    assertEquals(4, VoidBeamRules.penetrationLimit(true, false));
    assertEquals(5, VoidBeamRules.penetrationLimit(true, true));
}

@Test void penetrationDamageFallsAndNeverBecomesMalformed() {
    assertEquals(1.0, VoidBeamRules.damageMultiplier(0));
    assertEquals(0.72, VoidBeamRules.damageMultiplier(1));
    assertEquals(0.52, VoidBeamRules.damageMultiplier(2));
    assertEquals(0.0, VoidBeamRules.damageMultiplier(-1));
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.power.abilities.VoidBeamRulesTest`

Expected: compilation fails because `VoidBeamRules` does not exist.

- [ ] **Step 3: Implement the minimal timing and falloff rules**

```java
public static int chargeRemaining(long startedAt, long now) {
    long releaseAt = startedAt > Long.MAX_VALUE - 12L ? Long.MAX_VALUE : startedAt + 12L;
    return (int) Math.min(12L, Math.max(0L, releaseAt - Math.max(0L, now)));
}

public static int penetrationLimit(boolean empoweredImpact, boolean ancientMastery) {
    return Math.min(5, 3 + (empoweredImpact ? 1 : 0) + (ancientMastery ? 1 : 0));
}

public static double damageMultiplier(int penetrationIndex) {
    if (penetrationIndex < 0) return 0.0;
    return penetrationIndex == 0 ? 1.0 : penetrationIndex == 1 ? 0.72 : 0.52;
}
```

- [ ] **Step 4: Add target ordering, ward geometry, and scar-bound tests**

```java
@Test void candidatesAreNearestFirstDeduplicatedAndCapped() {
    var selected = VoidBeamRules.selectPenetrations(List.of(
            new VoidBeamRules.RayCandidate<>("far", 8),
            new VoidBeamRules.RayCandidate<>("near", 2),
            new VoidBeamRules.RayCandidate<>("near", 3),
            new VoidBeamRules.RayCandidate<>("outside", 12)), 10, 2);
    assertEquals(List.of("near", "far"), selected.stream().map(VoidBeamRules.RayCandidate::target).toList());
}

@Test void segmentSphereReturnsTheNearestEntryDistance() {
    assertEquals(4.0, VoidBeamRules.segmentSphereEntry(0, 0, 0, 10, 0, 0, 5, 0, 0, 1), 1.0E-6);
    assertTrue(Double.isNaN(VoidBeamRules.segmentSphereEntry(0, 0, 0, 2, 0, 0, 5, 0, 0, 1)));
}

@Test void scarCadenceAndBoundsAreHardCapped() {
    assertTrue(VoidBeamRules.shouldRenderScar(5));
    assertTrue(VoidBeamRules.shouldPulseScar(10));
    assertFalse(VoidBeamRules.shouldPulseScar(0));
    assertEquals(4.0, VoidBeamRules.scarRadius(99), 0.0);
    assertEquals(160, VoidBeamRules.scarDuration(999, true));
}
```

- [ ] **Step 5: Run focused tests, implement the remaining pure methods, and verify GREEN**

Run the same focused Gradle command. Implement stable distance sorting with `LinkedHashSet` deduplication, the quadratic segment/sphere entry formula, five-/ten-tick positive-age cadence, finite radius clamp `1..4`, duration clamp `20..160`, and the enum values `NONE`, `LIGHT`, `AMETHYST`, `KINETIC_WARD`, `SANCTUARY`, `FORCEFIELD`, `SAFE_ZONE`.

- [ ] **Step 6: Commit the pure contract**

```bash
git add src/main/java/com/powers/power/abilities/VoidBeamRules.java src/test/java/com/powers/power/abilities/VoidBeamRulesTest.java
git commit -m "feat: define abyssal beam rules"
```

---

### Task 2: Path ward interception and removable impact presence

**Files:**
- Modify: `src/main/java/com/powers/spell/SpellFieldManager.java`
- Modify: `src/main/java/com/powers/magic/runtime/MagicRuntime.java`
- Modify: `src/test/java/com/powers/magic/runtime/MagicRuntimeTest.java`

**Interfaces:**
- Consumes: `VoidBeamRules.segmentSphereEntry(...)` and `VoidBeamRules.Counterplay`.
- Produces: `SpellFieldManager.firstHarmfulRayIntercept(ServerLevel,UUID,Vec3,Vec3)` returning `Optional<RayWardHit>`; `MagicRuntime.removePresence(MagicPresenceId)`.

- [ ] **Step 1: Add a failing real-runtime removal test**

```java
@Test void explicitlyRemovedImpactPresenceCannotCollideAgain() {
    MagicPresence presence = presence("00000000-0000-0000-0000-000000000099", "void_beam", 200);
    runtime.registerPresence(presence);
    assertTrue(runtime.removePresence(presence.id()));
    assertFalse(runtime.removePresence(presence.id()));
    assertTrue(runtime.previewCast(cast("starfall", 20)).reactions().isEmpty());
}
```

- [ ] **Step 2: Run the focused runtime test and verify RED**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.magic.runtime.MagicRuntimeTest`

Expected: compilation fails because `removePresence` is absent.

- [ ] **Step 3: Implement removal and ward interception**

`MagicRuntime.removePresence` delegates to the owned `ActiveMagicIndex.remove`. `SpellFieldManager` ignores expired, same-owner, other-dimension, and non-ward fields; it tests `KINETIC_WARD` and `SANCTUARY` spheres with the pure segment formula and returns only the nearest entry point, distance, and matching counterplay kind.

- [ ] **Step 4: Run focused runtime and pure tests and verify GREEN**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.powers.magic.runtime.MagicRuntimeTest --tests com.powers.power.abilities.VoidBeamRulesTest`

- [ ] **Step 5: Commit the interception boundary**

```bash
git add src/main/java/com/powers/spell/SpellFieldManager.java src/main/java/com/powers/magic/runtime/MagicRuntime.java src/test/java/com/powers/magic/runtime/MagicRuntimeTest.java
git commit -m "feat: intercept abyssal rays with wards"
```

---

### Task 3: Server-authoritative charge, release, and scar

**Files:**
- Replace: `src/main/java/com/powers/power/abilities/VoidBeamAbility.java`
- Create: `src/main/java/com/powers/power/abilities/VoidScarManager.java`
- Modify: `src/main/java/com/powers/fx/PowerFx.java`
- Modify: `src/main/java/com/powers/PowersMod.java`

**Interfaces:**
- Consumes: Task 1 rules, Task 2 ward query/removal, `PowerScalingService`, `PowerProtection`, `AmethystDampening`, `MagicShieldManager`, and `ServerMagicCasts` lifecycle.
- Produces: `VoidBeamAbility.tickAll(MinecraftServer)`, `clear(UUID)`, `clearAll()`; `VoidScarManager.create(...)`, `tickAll(MinecraftServer)`, `clear(UUID)`, `clearAll()`.
- Produces: `PowerFx.voidBeamCharge`, `voidBeamRelease`, `voidBeamPenetration`, `voidBeamCountered`, `voidScarPulse`, and `voidScarCollapse`.

- [ ] **Step 1: Start one snapshotted charge from `activate`**

Capture dimension, start tick, scaled 48-block range, 8 base damage, 100-tick Wither, 2.75 scar radius, 80-tick scar duration, pulse strength, penetration limit, and rank flags. Reject duplicate owner state; otherwise emit the opening charge cue and return `true` so the existing packet transaction performs payment, commit, and cooldown exactly once.

- [ ] **Step 2: Advance and validate charge state from the server tick**

Each tick, remove state for missing/dead casters, dimension changes, lost power, or amethyst suppression. Emit a contracting charge cue while `chargeRemaining > 0`; at zero remove the map entry before releasing so callbacks cannot duplicate the cast.

- [ ] **Step 3: Resolve the finite hitscan release**

Use the nearest block hit and `firstHarmfulRayIntercept` to bound the segment. Intersect living bounding boxes in the finite ray envelope, convert them to `RayCandidate<LivingEntity>`, and consume the ordered selection. Stop on amethyst, sanctuary, safe zone, active forcefield, failed server damage, field intercept, special block, or the penetration cap. Apply owner-attributed damage and Wither only after `hurtServer` succeeds.

- [ ] **Step 4: Implement bounded scar ownership and pulses**

Create at most 128 scars. Register an impact-position `void_beam` `MagicPresence`; store its ID, dimension, owner, centre, created/expiry ticks, radius, pulse damage, Wither tier, and rank presentation. Every positive fifth tick emits visuals; every positive tenth tick affects at most the nearest 16 permitted non-owner living entities. Remove the presence on expiry, unload, owner cleanup, or shutdown.

- [ ] **Step 5: Add all semantic FX methods**

Use existing `ECLIPSE`, `RIBBON`, `FRACTURE`, reverse-portal, soul, coloured rune/ring/spiral helpers, and authored dark/amethyst/light/ward sounds. Scale density only through bounded booleans/charge beats; distinguish counter types by geometry as well as colour.

- [ ] **Step 6: Wire every lifecycle edge**

Have the public `VoidBeamAbility` lifecycle methods delegate to the package-private scar manager, then call only that façade after respawn/disconnect, during server stop, and from `END_SERVER_TICK`. Keep source imports explicit and maintain the entry point below the Java-audit size ceiling.

- [ ] **Step 7: Compile server and client source sets**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew compileJava compileClientJava`

Expected: both tasks succeed with no dedicated-server client-class references.

- [ ] **Step 8: Commit gameplay runtime**

```bash
git add src/main/java/com/powers/power/abilities/VoidBeamAbility.java src/main/java/com/powers/power/abilities/VoidScarManager.java src/main/java/com/powers/fx/PowerFx.java src/main/java/com/powers/PowersMod.java
git commit -m "feat: tear open abyssal void scars"
```

---

### Task 4: Authored presentation, lore, and release gates

**Files:**
- Modify: `src/main/resources/assets/powers/lang/en_us.json`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Regenerate: `docs/quality/code-audit.md`
- Regenerate: `docs/quality/non-item-assets.md`

- [ ] **Step 1: Update player-facing copy and system documentation**

Change the power description to the charged penetrating/scar behavior, add three concise Void Beam lore lines, document payment/counterplay/penetration/scar/rank behavior in README, and add one changelog bullet. Do not add a recipe.

- [ ] **Step 2: Regenerate and check quality manifests**

Run:

```bash
python3 scripts/audit_java_sources.py
python3 scripts/audit_non_item_assets.py
python3 scripts/audit_java_sources.py --check
python3 scripts/audit_non_item_assets.py --check
```

- [ ] **Step 3: Run the full clean release build**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew clean check build`

Expected: all tests, JSON/assets, interaction matrix, source audits, and JAR tasks succeed.

- [ ] **Step 4: Run the dedicated-server lifecycle smoke**

Temporarily set ignored `run/server.properties` `enable-rcon=false`, start `runServer --args='nogui --universe <mktemp-dir> --port 0'`, wait for `Done`, send `stop`, require exit 0 and all dimensions saved, then restore `enable-rcon=true` with `apply_patch`.

- [ ] **Step 5: Review and commit the finished tranche**

Run `git diff --check`, review every changed file, require a clean focused test/build result, then commit:

```bash
git add src/main/resources/assets/powers/lang/en_us.json README.md CHANGELOG.md docs/quality
git commit -m "feat: unveil the abyssal beam ceremony"
```
