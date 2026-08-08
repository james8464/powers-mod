# Living Realm Forces Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bounded spreading darkness and pure light, darkness affinity effects, and a staged catastrophic mutual-annihilation clash.

**Architecture:** Random-ticking block subclasses delegate to a server-owned manager backed by a chunk index. Pure rules calculate affinities and blast falloff; active clash waves mutate only loaded force blocks under a per-tick work budget.

**Tech Stack:** Java 25, Minecraft 26.2 Mojang mappings, Fabric Loader/API, JUnit 6, Gradle Loom.

## Global Constraints

- Do not add crafting recipes for crystals or other progression items.
- Never force-load chunks or execute client-authoritative world mutations.
- Respect safe zones for spreading and hostile entity effects.
- Preserve the global particle budget and the 450-line source responsibility limit.
- A clash removes only darkness and pure-light blocks; it does not run a literal vanilla power-100 terrain explosion.

---

### Task 1: Pure rules and sanitized policy

**Files:**
- Create: `src/test/java/com/powers/force/LivingForceRulesTest.java`
- Create: `src/main/java/com/powers/force/LivingForceKind.java`
- Create: `src/main/java/com/powers/force/LivingForceRules.java`
- Modify: `src/main/java/com/powers/config/PowersConfig.java`
- Modify: `src/main/java/com/powers/config/PowersConfigLoader.java`
- Modify: `src/test/java/com/powers/config/PowersConfigTest.java`

**Interfaces:**
- Produces: `LivingForceRules.affinity(boolean, LivingForceKind)`, `opposes(...)`, `insideSphere(...)`, and `clashDamage(double, double, double)`.
- Produces: sanitized `PowersConfig.LivingForces` policy.

- [ ] **Step 1: Write failing rule and configuration tests**

```java
assertEquals(Affinity.REFILL, affinity(true, DARKNESS));
assertEquals(Affinity.WITHER, affinity(false, DARKNESS));
assertTrue(opposes(DARKNESS, PURE_LIGHT));
assertEquals(100.0, clashDamage(0.0, 48.0, 100.0));
assertEquals(0.0, clashDamage(48.0, 48.0, 100.0));
assertTrue(PowersConfig.defaults().livingForces().spreadingEnabled());
```

- [ ] **Step 2: Run the focused tests and confirm missing production APIs fail**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.force.LivingForceRulesTest' --tests 'com.powers.config.PowersConfigTest'`

- [ ] **Step 3: Implement the smallest immutable rules and sanitized defaults that pass**

```java
public enum Affinity { NONE, WITHER, REFILL }

public static double clashDamage(double distance, double radius, double peak) {
    return distance >= radius ? 0.0 : peak * Math.pow(1.0 - distance / radius, 2.0);
}
```

- [ ] **Step 4: Re-run focused tests until green**

- [ ] **Step 5: Commit the rule and policy slice**

### Task 2: Spatial index and block spreading

**Files:**
- Create: `src/test/java/com/powers/force/LivingForceIndexTest.java`
- Create: `src/main/java/com/powers/force/LivingForceIndex.java`
- Create: `src/main/java/com/powers/force/LivingForceBlock.java`
- Create: `src/main/java/com/powers/force/LivingForceManager.java`
- Create: `src/main/java/com/powers/force/package-info.java`
- Modify: `src/main/java/com/powers/PowersBlocks.java`
- Create: `src/main/resources/data/powers/tags/block/living_force_immune.json`

**Interfaces:**
- Consumes: `PowersConfig.LivingForces`, `LivingForceKind`, and `LivingForceRules`.
- Produces: `LivingForceManager.initialize()`, `tick(MinecraftServer)`, `clearAll()`, `spread(...)`, `register(...)`, and `unregister(...)`.

- [ ] **Step 1: Write failing index tests for add/remove/chunk eviction/nearest range**

```java
index.add(BlockPos.asLong(1, 64, 1), DARKNESS);
assertTrue(index.hasWithin(0.0, 64.0, 0.0, 8.0, DARKNESS));
index.removeChunk(ChunkPos.pack(0, 0));
assertFalse(index.hasWithin(0.0, 64.0, 0.0, 8.0, DARKNESS));
```

- [ ] **Step 2: Run the focused index test and confirm the missing type fails**

- [ ] **Step 3: Implement the chunk-bucket index, random-tick block delegation, chunk lifecycle hooks, replacement policy, and low-intensity spread cues**

```java
if (state.isAir() || !state.getFluidState().isEmpty() || state.is(FORCE_SPREAD_IMMUNE)
        || state.getDestroySpeed(level, target) < 0 || level.getBlockEntity(target) != null) return;
level.setBlock(target, kind.block().defaultBlockState(), Block.UPDATE_ALL);
```

- [ ] **Step 4: Run index tests, compile production sources, and fix mapping errors**

- [ ] **Step 5: Commit the spreading slice**

### Task 3: Aura and catastrophic clash wave

**Files:**
- Create: `src/test/java/com/powers/force/ForceClashWaveTest.java`
- Create: `src/main/java/com/powers/force/ForceClashWave.java`
- Modify: `src/main/java/com/powers/force/LivingForceManager.java`
- Modify: `src/main/java/com/powers/fx/PowerFx.java`
- Modify: `src/main/java/com/powers/PowersMod.java`

**Interfaces:**
- Consumes: indexed darkness positions, pure blast rules, player energy/rank scaling, safe zones, amethyst dampening, and particle budgets.
- Produces: bounded `ForceClashWave.tick(int)` and once-per-second entity affinity updates.

- [ ] **Step 1: Write a failing cursor test proving a wave visits each in-sphere coordinate once and respects the work budget**

```java
ForceClashWave.Cursor cursor = new ForceClashWave.Cursor(2);
assertEquals(5, cursor.take(5).size());
assertTrue(cursor.take(1000).stream().allMatch(offset -> offset.distanceSquared() <= 4));
assertTrue(cursor.finished());
```

- [ ] **Step 2: Run the focused test and confirm the missing cursor fails**

- [ ] **Step 3: Implement aura outcomes, stale-index cleanup, clash deduplication, staged force removal, damage, knockback, and audiovisual beats**

```java
double damage = LivingForceRules.clashDamage(entity.distanceTo(center), radius, 100.0);
if (damage > 0.0 && !PowerProtection.isSafeZone(level, entity.position())) {
    entity.hurtServer(level, entity.damageSources().magic(), (float) damage);
}
```

- [ ] **Step 4: Run force tests and the complete unit suite**

- [ ] **Step 5: Commit the aura/clash slice**

### Task 4: Exhaustive interactions, documentation, and release verification

**Files:**
- Modify: `src/main/java/com/powers/magic/MagicOrigin.java`
- Modify: `src/main/java/com/powers/magic/MagicActionCatalogue.java`
- Modify: `src/main/java/com/powers/magic/MagicInteractionResolver.java`
- Modify: `src/main/java/com/powers/magic/MagicDocumentation.java`
- Modify: `src/test/java/com/powers/magic/MagicActionCatalogueTest.java`
- Modify: `src/test/java/com/powers/magic/MagicDocumentationTest.java`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Regenerate: `docs/interactions/action-catalogue.md`
- Regenerate: `docs/interactions/interaction-rules.md`
- Regenerate: `docs/interactions/interaction-matrix.csv`
- Regenerate: `docs/quality/code-audit.md`

**Interfaces:**
- Produces: 65 canonical actions and 2,145 unordered deterministic interactions including exact realm-force annihilation.

- [ ] **Step 1: Write failing catalogue and resolver expectations for the two realm actions and exact annihilation pair**

```java
assertEquals(2, catalogue.byOrigin(MagicOrigin.REALM).size());
assertEquals(InteractionOutcome.CANCEL, resolver.resolve(darkness, light, DEFAULT).outcome());
```

- [ ] **Step 2: Run the focused magic tests and confirm the new expectations fail**

- [ ] **Step 3: Register realm actions, exact collision semantics, and update human documentation**

- [ ] **Step 4: Regenerate interaction and source-audit documents**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew generateMagicDocs && python3 scripts/audit_java_sources.py`

- [ ] **Step 5: Run `check`, `build`, and a dedicated-server startup/shutdown smoke test; then commit all verified outputs**
