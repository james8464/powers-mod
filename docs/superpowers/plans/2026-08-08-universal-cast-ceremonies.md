# Universal Arcane Cast Ceremonies Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every successful player-cast action a distinctive, bounded, four-beat audiovisual ceremony derived from its canonical magic signature.

**Architecture:** Pure presentation and choreography policies turn action definitions into safe sound/intensity profiles and timed render frames. The successful server commit boundary emits one typed semantic event; the Fabric client expands it into deterministic geometry while existing bespoke ability effects remain intact.

**Tech Stack:** Java 25, Minecraft 26.2 Mojang mappings, Fabric Loader/API, JUnit 6, Gradle Loom.

## Global Constraints

- Do not add crafting recipes for crystals or other progression items.
- Emit cast ceremonies only after a successful authoritative server commit.
- Preserve the existing 128-block observer radius, 96-point frame cap, 32-event client queue, and 256-ID client deduplication cap.
- Respect vanilla screen-effect scale and replace moving geometry under reduced motion.
- Use only registered server-selected sound cues and compact semantic packets.
- Preserve existing bespoke ability, realm-force, and collision effects.

---

### Task 1: Typed events and presentation profiles

**Files:**
- Create: `src/main/java/com/powers/magic/fx/MagicFxKind.java`
- Create: `src/main/java/com/powers/magic/fx/MagicCastPresentation.java`
- Modify: `src/main/java/com/powers/magic/fx/MagicFxEvent.java`
- Modify: `src/test/java/com/powers/magic/fx/MagicFxServiceTest.java`
- Create: `src/test/java/com/powers/magic/fx/MagicCastPresentationTest.java`

**Interfaces:**
- Produces: `MagicFxKind.networkId()` and `MagicFxKind.fromNetworkId(int)`.
- Produces: `MagicCastPresentation.forAction(MagicActionDefinition)` with `soundCue()` and bounded `intensity()`.
- Produces: `MagicFxEvent.cast(...)`; preserves `MagicFxEvent.interaction(...)`.

- [ ] **Step 1: Write failing event-kind and catalogue-profile tests**

```java
MagicFxEvent cast = MagicFxEvent.cast(7L, "time", "time_suspend", 1, 2, 3,
        0x68E0D5, 0xD7F8FF, 42, 3);
assertEquals(MagicFxKind.CAST, cast.kind());
assertThrows(IllegalArgumentException.class, () -> MagicFxKind.fromNetworkId(99));

for (MagicActionDefinition action : MagicActionCatalogue.defaults().definitions()) {
    MagicCastPresentation profile = MagicCastPresentation.forAction(action);
    assertTrue(profile.intensity() >= 1 && profile.intensity() <= 5);
    assertTrue(AUTHORED_CUES.contains(profile.soundCue()));
}
```

- [ ] **Step 2: Run the focused tests and confirm missing APIs fail**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.magic.fx.MagicFxServiceTest' --tests 'com.powers.magic.fx.MagicCastPresentationTest'`

Expected: compilation fails because `MagicFxKind`, `MagicCastPresentation`, and `MagicFxEvent.cast` do not exist.

- [ ] **Step 3: Implement the minimal validated event kind and pure profile policy**

```java
public enum MagicFxKind {
    CAST(0), INTERACTION(1);

    public static MagicFxKind fromNetworkId(int id) {
        return switch (id) {
            case 0 -> CAST;
            case 1 -> INTERACTION;
            default -> throw new IllegalArgumentException("Unknown magic FX kind: " + id);
        };
    }
}
```

The profile selects only `rune_hum`, `crystal_resonate`, `amethyst_fracture`, `time_suspend`, `rift_open`, `soul_tether`, `light_chorus`, `dark_whisper`, or `ward_impact`, using the priority defined in the design specification.

- [ ] **Step 4: Re-run both focused tests until green, then run the complete unit suite**

- [ ] **Step 5: Commit the typed semantic event slice**

### Task 2: Pure four-beat choreography and physical scaling

**Files:**
- Create: `src/main/java/com/powers/magic/fx/FxFrame.java`
- Create: `src/main/java/com/powers/magic/fx/FxChoreography.java`
- Create: `src/test/java/com/powers/magic/fx/FxChoreographyTest.java`
- Modify: `src/main/java/com/powers/fx/FxGeometry.java`
- Modify: `src/test/java/com/powers/fx/FxGeometryTest.java`

**Interfaces:**
- Produces: `FxChoreography.frame(MagicFxKind, int age, boolean reducedMotion)` returning `Optional<FxFrame>`.
- Produces: `FxChoreography.finished(MagicFxKind, int age)`.
- Produces: `FxGeometry.scale(Point, double)` for finite bounded local-space expansion.

- [ ] **Step 1: Write failing literal timing, accessibility, and geometry tests**

```java
assertEquals(FxBeat.ANTICIPATION, frame(CAST, 0, false).orElseThrow().beat());
assertTrue(frame(CAST, 1, false).isEmpty());
assertEquals(FxBeat.RELEASE, frame(CAST, 3, false).orElseThrow().beat());
assertEquals(FxBeat.IMPACT, frame(CAST, 7, false).orElseThrow().beat());
assertEquals(FxBeat.AFTERMATH, frame(CAST, 13, false).orElseThrow().beat());
assertTrue(finished(CAST, 17));
assertTrue(frame(CAST, 7, true).orElseThrow().geometryScale()
        < frame(CAST, 7, false).orElseThrow().geometryScale());
assertEquals(new FxGeometry.Point(2.0, -4.0, 6.0),
        FxGeometry.scale(new FxGeometry.Point(1.0, -2.0, 3.0), 2.0));
```

- [ ] **Step 2: Run the focused tests and confirm the missing choreography fails**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.magic.fx.FxChoreographyTest' --tests 'com.powers.fx.FxGeometryTest'`

- [ ] **Step 3: Implement immutable frames, distinct cast/interaction schedules, reduced-motion clamps, and validated point scaling**

`FxFrame` validates finite non-negative budget, geometry, and velocity scales. Cast beats occur at ages 0/3/7/13 and finish at 17; interaction beats occur at 0/4/8/15 and finish at 18. Reduced motion replaces moving override motifs with `RING` or `GLYPH`, caps geometry at `0.85`, and caps velocity at `0.25`.

- [ ] **Step 4: Re-run focused tests and the complete unit suite until green**

- [ ] **Step 5: Commit the pure choreography slice**

### Task 3: Server commit, compact protocol, and client renderer

**Files:**
- Modify: `src/main/java/com/powers/network/MagicFxPackets.java`
- Modify: `src/main/java/com/powers/magic/runtime/ServerMagicCasts.java`
- Modify: `src/main/java/com/powers/magic/runtime/PreparedMagicCast.java`
- Modify: `src/client/java/com/powers/client/fx/ClientMagicFx.java`

**Interfaces:**
- Consumes: `MagicFxKind`, `MagicCastPresentation`, `FxChoreography`, `FxGeometry.scale`, and the action's `MagicSignature`.
- Produces: a payload whose first field is a bounded kind ID and a successful-commit broadcast/audio side effect.

- [ ] **Step 1: Add a failing payload-conversion test to `MagicFxServiceTest`**

```java
MagicFxEvent event = MagicFxEvent.cast(17L, "light", "light_chorus",
        1.0, 2.0, 3.0, 0xFFF2B0, 0xFFFFFF, 13, 4);
MagicFxPackets.MagicFxPayload payload = new MagicFxPackets.MagicFxPayload(event);
assertEquals(MagicFxKind.CAST, payload.kind());
```

The production change caught by this test is dropping the semantic event kind while converting an event into its network payload.

- [ ] **Step 2: Run the focused test and confirm the missing payload accessor fails**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests 'com.powers.magic.fx.MagicFxServiceTest'`

Expected: compilation fails because the old payload does not expose `kind()`.

- [ ] **Step 3: Wire the typed payload and successful server cast ceremony**

```java
MagicPresenceId presenceId = MagicRuntime.global().commitCast(completed, prepared.adjustment());
MagicCastPresentation presentation = MagicCastPresentation.forAction(completed.definition());
MagicSignature signature = completed.definition().signature();
MagicFxPackets.broadcast(level, MagicFxEvent.cast(eventId, signature.motif(),
        presentation.soundCue(), anchor.x(), anchor.y(), anchor.z(),
        signature.primaryColor(), signature.secondaryColor(), signature.glyphSeed(),
        presentation.intensity()));
PowerFx.sound(level, anchor, PowersSounds.forCue(presentation.soundCue()), volume, pitch);
return presenceId;
```

Encode `MagicFxKind` with `ByteBufCodecs.VAR_INT.map(MagicFxKind::fromNetworkId, MagicFxKind::networkId)`, include kind in the payload constructor and deduplication key, and preserve all existing fields.

- [ ] **Step 4: Replace hard-coded client ages with `FxChoreography`; scale point coordinates and velocities by each frame; keep queue, distance, budget, tint, sprite, and reset safeguards**

- [ ] **Step 5: Run focused tests, `compileJava`, `compileClientJava`, and the full unit suite; fix mapping/type errors without weakening validation**

- [ ] **Step 6: Commit the integrated cast ceremony**

### Task 4: Documentation, audit, and release verification

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Regenerate: `docs/quality/code-audit.md`

**Interfaces:**
- Produces: user-facing cast-presentation documentation and an exact production-source manifest.

- [ ] **Step 1: Document universal cast ceremonies, signature-driven sounds, accessibility behaviour, and unchanged bespoke impacts**

- [ ] **Step 2: Regenerate the production Java audit before invoking `check`**

Run: `python3 scripts/audit_java_sources.py`

- [ ] **Step 3: Run the complete release gate**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew clean check build`

- [ ] **Step 4: Start an isolated dedicated server, verify POWERS initializes without errors, issue `stop`, and confirm clean shutdown**

- [ ] **Step 5: Inspect `git diff --check`, commit all verified outputs to `main`, and leave the worktree clean**
