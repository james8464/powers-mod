# Magic Kernel and Source Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the typed 63-action catalogue, exhaustive 2,016-pair resolver, bounded runtime presence index, cast integration, and enforceable source documentation standard.

**Architecture:** Immutable definitions and a pure resolver live under `com.powers.magic`; runtime indexing and orchestration live under `com.powers.magic.runtime`. Existing innate, crystal, spell, and amethyst entry points adapt to the kernel without changing stored IDs or player attachments.

**Tech Stack:** Java 25 records/sealed types, Fabric 0.19.3, Minecraft 26.2, JUnit 6, Gradle, Python resource/document generators.

## Global Constraints

- Preserve Minecraft 26.2, Fabric Loader, Java 25, server authority, and private-multiplayer safety.
- Do not add recipes for crystals or deliberately unreleased progression artifacts.
- Keep mindscape and projection bodies vulnerable.
- Preserve existing player data and migrate rank data without loss.
- Use professional contract/invariant/ownership comments rather than line narration.
- All client fields remain untrusted and are revalidated on the server.
- Every commit must build on the sole `main` branch.

---

### Task 1: Canonical Action Types and Catalogue

**Files:**
- Create: `src/main/java/com/powers/magic/MagicOrigin.java`
- Create: `src/main/java/com/powers/magic/MagicAspect.java`
- Create: `src/main/java/com/powers/magic/MagicDelivery.java`
- Create: `src/main/java/com/powers/magic/MagicIntent.java`
- Create: `src/main/java/com/powers/magic/MagicActionId.java`
- Create: `src/main/java/com/powers/magic/MagicSignature.java`
- Create: `src/main/java/com/powers/magic/MagicActionDefinition.java`
- Create: `src/main/java/com/powers/magic/MagicActionCatalogue.java`
- Create: `src/main/java/com/powers/magic/package-info.java`
- Test: `src/test/java/com/powers/magic/MagicActionCatalogueTest.java`

**Interfaces:**
- Produces: `MagicActionCatalogue.defaults()`, `definition(MagicActionId)`, `definitions()`, and the 63 stable IDs consumed by every later task.

- [ ] **Step 1: Write the failing catalogue contract test**

```java
@Test
void defaultsContainEverySupportedActionExactlyOnce() {
    MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();
    assertEquals(63, catalogue.definitions().size());
    assertEquals(63, catalogue.definitions().stream().map(MagicActionDefinition::id).distinct().count());
    assertEquals(27, catalogue.byOrigin(MagicOrigin.INNATE).size());
    assertEquals(13, catalogue.byOrigin(MagicOrigin.CRYSTAL).size());
    assertEquals(20, catalogue.byOrigin(MagicOrigin.SPELL).size());
    assertEquals(3, catalogue.byOrigin(MagicOrigin.AMETHYST).size());
    assertTrue(catalogue.definitions().stream().allMatch(MagicActionDefinition::isComplete));
}
```

- [ ] **Step 2: Run the test and verify the missing package failure**

Run: `./gradlew test --tests com.powers.magic.MagicActionCatalogueTest`

Expected: compilation fails because `com.powers.magic` does not exist.

- [ ] **Step 3: Implement immutable types and the complete catalogue**

```java
public record MagicActionDefinition(
        MagicActionId id, MagicOrigin origin, Set<MagicAspect> aspects,
        MagicDelivery delivery, MagicIntent intent, int basePotency,
        double baseRange, int baseDurationTicks, int baseEnergy,
        int baseCooldownTicks, int residueTicks, int priority,
        MagicSignature signature) {
    public MagicActionDefinition {
        Objects.requireNonNull(id);
        aspects = Set.copyOf(aspects);
        if (aspects.isEmpty() || basePotency < 0 || baseRange < 0 || baseDurationTicks < 0
                || baseEnergy < 0 || baseCooldownTicks < 0 || residueTicks < 0) {
            throw new IllegalArgumentException("Invalid action definition: " + id);
        }
    }
    public boolean isComplete() {
        return delivery != null && intent != null && signature != null;
    }
}
```

Use the current registry IDs verbatim. Crystal action IDs are the distinct values created by `CrystalPowerRegistry`; spell IDs are the twenty `SpellDefinition.id()` values; amethyst IDs are `amethyst_item`, `amethyst_block`, and `amethyst_ward`.

- [ ] **Step 4: Run catalogue and full unit tests**

Run: `./gradlew test --tests com.powers.magic.MagicActionCatalogueTest && ./gradlew test`

Expected: both commands pass; existing 47 tests remain green.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/powers/magic src/test/java/com/powers/magic/MagicActionCatalogueTest.java
git commit -m "feat: define canonical magic action catalogue"
```

### Task 2: Exhaustive Interaction Resolver

**Files:**
- Create: `src/main/java/com/powers/magic/InteractionOutcome.java`
- Create: `src/main/java/com/powers/magic/ActionPair.java`
- Create: `src/main/java/com/powers/magic/ResolvedPair.java`
- Create: `src/main/java/com/powers/magic/InteractionCue.java`
- Create: `src/main/java/com/powers/magic/InteractionResolution.java`
- Create: `src/main/java/com/powers/magic/InteractionContext.java`
- Create: `src/main/java/com/powers/magic/MagicInteractionRule.java`
- Create: `src/main/java/com/powers/magic/MagicInteractionResolver.java`
- Test: `src/test/java/com/powers/magic/MagicInteractionResolverTest.java`

**Interfaces:**
- Consumes: `MagicActionDefinition` and `MagicActionCatalogue.defaults()`.
- Produces: `resolve(MagicActionDefinition first, MagicActionDefinition second, InteractionContext context)` and `allPairs()`.

- [ ] **Step 1: Write failing exhaustive and exceptional-reaction tests**

```java
@Test
void everyUnorderedPairIncludingSelfHasMechanicsAndPresentation() {
    MagicInteractionResolver resolver = MagicInteractionResolver.defaults(MagicActionCatalogue.defaults());
    assertEquals(2016, resolver.allPairs().size());
    assertTrue(resolver.allPairs().stream().allMatch(pair ->
            pair.resolution().outcome() != null && pair.resolution().cue().isComplete()));
}

@Test
void flameAndFrostTransformIntoSteamSymmetrically() {
    assertEquals(InteractionOutcome.TRANSFORM,
            resolve("fireball", "frost_nova").outcome());
    assertEquals("steam", resolve("frost_nova", "fireball").cue().motif());
}
```

- [ ] **Step 2: Run the tests and verify missing resolver types**

Run: `./gradlew test --tests com.powers.magic.MagicInteractionResolverTest`

Expected: compilation fails on `MagicInteractionResolver`.

- [ ] **Step 3: Implement ordered priority and complete outcome data**

```java
public InteractionResolution resolve(MagicActionDefinition a, MagicActionDefinition b,
        InteractionContext context) {
    ActionPair key = ActionPair.canonical(a.id(), b.id());
    MagicInteractionRule exact = exactRules.get(key);
    if (exact != null) return exact.resolve(a, b, context);
    return suppression(a, b, context)
            .or(() -> opposition(a, b, context))
            .or(() -> resonance(a, b, context))
            .or(() -> deliveryIntent(a, b, context))
            .orElseGet(() -> coexistence(a, b));
}
```

Encode the high-impact reaction families from the design as named rules. The fallback cue combines both `MagicSignature` palettes and glyph seeds and uses the `harmonic_weave` motif.

- [ ] **Step 4: Test rule order, same-action collisions, symmetry declarations, and all 2,016 pairs**

Run: `./gradlew test --tests com.powers.magic.MagicInteractionResolverTest`

Expected: all resolver tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/powers/magic src/test/java/com/powers/magic/MagicInteractionResolverTest.java
git commit -m "feat: resolve every magic interaction pair"
```

### Task 3: Bounded Active-Magic Spatial Index

**Files:**
- Create: `src/main/java/com/powers/magic/runtime/MagicPresenceId.java`
- Create: `src/main/java/com/powers/magic/runtime/MagicPresence.java`
- Create: `src/main/java/com/powers/magic/runtime/PresenceAnchor.java`
- Create: `src/main/java/com/powers/magic/runtime/ActiveMagicIndex.java`
- Create: `src/main/java/com/powers/magic/runtime/package-info.java`
- Test: `src/test/java/com/powers/magic/runtime/ActiveMagicIndexTest.java`

**Interfaces:**
- Consumes: `MagicActionId`.
- Produces: `register`, `move`, `nearby`, `removeOwner`, `remove`, `expire`, and `clear`.

- [ ] **Step 1: Write failing locality and lifecycle tests**

```java
@Test
void nearbyOnlyReturnsIntersectingLivePresences() {
    ActiveMagicIndex index = new ActiveMagicIndex(16);
    index.register(presence("near", 4, 64, 4, 8, 100));
    index.register(presence("far", 96, 64, 96, 8, 100));
    assertEquals(Set.of("near"), ids(index.nearby(dimension, new Vec3(0, 64, 0), 16, 50)));
    assertTrue(index.nearby(dimension, new Vec3(0, 64, 0), 16, 101).isEmpty());
}

@Test
void ownerAndServerCleanupCannotLeaveResidues() {
    index.removeOwner(owner);
    assertEquals(0, index.size());
    index.clear();
    assertEquals(0, index.cellCount());
}
```

- [ ] **Step 2: Run and verify missing runtime package**

Run: `./gradlew test --tests com.powers.magic.runtime.ActiveMagicIndexTest`

Expected: compilation fails on `ActiveMagicIndex`.

- [ ] **Step 3: Implement cell membership and reverse ownership indexes**

```java
public final class ActiveMagicIndex {
    private final int cellSize;
    private final Map<CellKey, Set<MagicPresenceId>> cells = new HashMap<>();
    private final Map<MagicPresenceId, MagicPresence> presences = new HashMap<>();
    private final Map<UUID, Set<MagicPresenceId>> byOwner = new HashMap<>();
    // Registration records every intersected cell; move/remove updates all three maps atomically on the server thread.
}
```

Reject non-finite positions, negative radius/duration, duplicate IDs, and cross-dimension moves without explicit replacement.

- [ ] **Step 4: Run focused and complete tests**

Run: `./gradlew test --tests com.powers.magic.runtime.ActiveMagicIndexTest && ./gradlew test`

Expected: all tests pass with no leaked cells.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/powers/magic/runtime src/test/java/com/powers/magic/runtime
git commit -m "feat: track active magic in bounded spatial cells"
```

### Task 4: Shared Cast and Reaction Coordinator

**Files:**
- Create: `src/main/java/com/powers/magic/runtime/MagicCastContext.java`
- Create: `src/main/java/com/powers/magic/runtime/CastAdjustment.java`
- Create: `src/main/java/com/powers/magic/runtime/MagicRuntime.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `src/main/java/com/powers/power/crystals/CrystalPowerRegistry.java`
- Modify: `src/main/java/com/powers/spell/SpellCastingManager.java`
- Modify: `src/main/java/com/powers/power/AmethystDampening.java`
- Modify: `src/main/java/com/powers/PowersMod.java`
- Test: `src/test/java/com/powers/magic/runtime/MagicRuntimeTest.java`

**Interfaces:**
- Consumes: catalogue, resolver, index, existing cast validation and protection policies.
- Produces: `MagicRuntime.beforeCast`, `commitCast`, `registerPresence`, `tick`, `clearPlayer`, and `clearServer`.

- [ ] **Step 1: Write failing transaction and deduplication tests**

```java
@Test
void cancelledInteractionNeverCommitsCostCooldownOrPresence() {
    CastAdjustment adjustment = runtime.beforeCast(travelInsideAnchorContext());
    assertFalse(adjustment.allowed());
    assertEquals(InteractionOutcome.CANCEL, adjustment.resolutions().getFirst().outcome());
    assertEquals(0, index.size());
}

@Test
void repeatedPairInSameCellAndTickEmitsOneCue() {
    runtime.beforeCast(overlappingFireAndFrost());
    runtime.beforeCast(overlappingFireAndFrost());
    assertEquals(1, cueSink.events().size());
}
```

- [ ] **Step 2: Run and verify missing coordinator**

Run: `./gradlew test --tests com.powers.magic.runtime.MagicRuntimeTest`

Expected: compilation fails on `MagicRuntime`.

- [ ] **Step 3: Implement the server-thread transaction boundary**

```java
public CastAdjustment beforeCast(MagicCastContext cast) {
    requireServerThread(cast.server());
    List<InteractionResolution> resolutions = index.nearby(
            cast.dimension(), cast.origin(), cast.queryRadius(), cast.gameTime()).stream()
            .map(presence -> resolver.resolve(cast.definition(), catalogue.definition(presence.action()), cast.context()))
            .toList();
    return CastAdjustment.combine(resolutions);
}
```

`commitCast` registers one residue/presence only after the existing ability reports success. `clearPlayer` and `clearServer` delegate to the reverse ownership indexes.

- [ ] **Step 4: Integrate all four origins without trusting client IDs**

Innate casts derive the action from the resolved server slot; crystals derive it from the actual held item and selected server mode; spells derive it from the held grimoire and selected server spell; amethyst definitions are synthesized from server-side scan results. Blocked pre-casts refund reserved resources and do not start cooldowns.

- [ ] **Step 5: Run transaction, packet, crystal, spell, and full tests**

Run: `./gradlew test --tests 'com.powers.magic.runtime.*' --tests 'com.powers.network.*' --tests 'com.powers.power.crystals.*' --tests 'com.powers.spell.*' && ./gradlew test`

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/powers/magic/runtime src/main/java/com/powers/network/PowersPackets.java src/main/java/com/powers/power/crystals/CrystalPowerRegistry.java src/main/java/com/powers/spell/SpellCastingManager.java src/main/java/com/powers/power/AmethystDampening.java src/main/java/com/powers/PowersMod.java src/test/java/com/powers/magic/runtime
git commit -m "feat: coordinate interactions across every cast origin"
```

### Task 5: Source Documentation and Responsibility Audit

**Files:**
- Create: `src/test/java/com/powers/quality/SourceQualityTest.java`
- Create: `src/test/java/com/powers/quality/SourceAudit.java`
- Create: `scripts/audit_java_sources.py`
- Create: `docs/quality/code-audit.md`
- Create/modify: `package-info.java` in every Java package
- Modify: all Java source files identified by the audit
- Modify: `src/main/java/com/powers/PowersMod.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `src/main/java/com/powers/player/PlayerPowers.java`
- Modify: `src/main/java/com/powers/power/abilities/TeleportAbility.java`
- Test: `src/test/java/com/powers/quality/SourceQualityTest.java`

**Interfaces:**
- Consumes: all tracked Java paths.
- Produces: deterministic Markdown audit and a testable professional-documentation baseline.

- [ ] **Step 1: Write the failing source-quality test**

```java
@Test
void everyTrackedJavaFileIsAuditedAndPublicTypesAreDocumented() throws IOException {
    SourceAudit audit = SourceAudit.scan(projectRoot());
    assertEquals(audit.trackedJavaFiles(), audit.documentedAuditFiles());
    assertTrue(audit.undocumentedPublicTypes().isEmpty(), audit.report());
    assertTrue(audit.unfinishedMarkers().isEmpty(), audit.report());
    assertTrue(audit.debugWrites().isEmpty(), audit.report());
    assertTrue(audit.wildcardImports().isEmpty(), audit.report());
}
```

- [ ] **Step 2: Run and capture the exact baseline failures**

Run: `./gradlew test --tests com.powers.quality.SourceQualityTest`

Expected: failure lists every undocumented public type/package and any source shortcut.

- [ ] **Step 3: Generate `code-audit.md` and document every source contract**

The audit row schema is:

```text
| Path | Lines | Responsibility | Public contract | Ownership/lifecycle | Thread/authority | Findings | Resolution |
```

Add package Javadocs and contract comments. Keep standard overrides comment-free where the inherited contract is sufficient.

- [ ] **Step 4: Split the four mixed-responsibility classes along existing boundaries**

Move tick/lifecycle orchestration out of `PowersMod`, payload-family handlers out of `PowersPackets`, attachment operations behind focused nested/service facades while preserving `PlayerPowers.get`, and marking/storm travel out of `TeleportAbility`. Do not rename persistent attachment identifiers, packet IDs, command IDs, or power IDs.

- [ ] **Step 5: Run source audit, compiler warnings, and all tests**

Run: `python3 scripts/audit_java_sources.py --check && ./gradlew clean test`

Expected: every tracked Java file has one audit row, no policy violations remain, and all tests pass under `-Werror`.

- [ ] **Step 6: Commit**

```bash
git add scripts/audit_java_sources.py docs/quality/code-audit.md src/main/java src/client/java src/test/java/com/powers/quality
git commit -m "refactor: document and separate source responsibilities"
```

### Task 6: Generated Catalogue and Interaction Evidence

**Files:**
- Create: `src/main/java/com/powers/magic/MagicDocumentation.java`
- Create: `scripts/generate_magic_docs.py`
- Create: `docs/interactions/action-catalogue.md`
- Create: `docs/interactions/interaction-rules.md`
- Create: `docs/interactions/interaction-matrix.csv`
- Modify: `build.gradle`
- Test: `src/test/java/com/powers/magic/MagicDocumentationTest.java`

**Interfaces:**
- Consumes: a deterministic JSON export from `MagicActionCatalogue` and `MagicInteractionResolver`.
- Produces: three committed documents and Gradle `verifyMagicDocs` drift check.

- [ ] **Step 1: Write the failing documentation drift test**

```java
@Test
void committedMatrixMatchesResolver() throws IOException {
    List<String> rows = Files.readAllLines(projectRoot().resolve("docs/interactions/interaction-matrix.csv"));
    assertEquals(2017, rows.size());
    assertEquals("first,second,outcome,motif,mechanics", rows.getFirst());
    assertEquals(MagicDocumentation.renderMatrix(), String.join("\n", rows) + "\n");
}
```

- [ ] **Step 2: Run and verify the missing documents**

Run: `./gradlew test --tests com.powers.magic.MagicDocumentationTest`

Expected: failure because the matrix does not exist.

- [ ] **Step 3: Implement deterministic generation and write all documents**

Sort action IDs lexicographically and pair each index with itself and every later index. Quote CSV mechanics using RFC 4180 escaping. The rules document describes resolver priority and every named exceptional family.

- [ ] **Step 4: Add drift verification to `check` and run it**

```groovy
tasks.register("verifyMagicDocs", Exec) {
    commandLine "python3", "scripts/generate_magic_docs.py", "--check"
}
tasks.named("check") { dependsOn("verifyMagicDocs") }
```

Run: `python3 scripts/generate_magic_docs.py && ./gradlew check`

Expected: matrix contains one header plus exactly 2,016 data rows and `check` passes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/powers/magic/MagicDocumentation.java scripts/generate_magic_docs.py docs/interactions build.gradle src/test/java/com/powers/magic/MagicDocumentationTest.java
git commit -m "docs: enumerate every magic interaction"
```
