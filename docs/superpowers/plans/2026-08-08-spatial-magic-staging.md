# Spatial Magic Staging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Place ground rituals at the caster's feet and keep vertical magical sigils readable from every client viewpoint.

**Architecture:** Pure orientation types and geometry transforms extend the existing immutable FX frames. The client applies one local transform per already-budgeted point using the observer-relative angle; the semantic packet and server remain unchanged.

**Tech Stack:** Java 25, Minecraft 26.2 Mojang mappings, Fabric Loader/API, JUnit 6, Gradle Loom.

## Global Constraints

- Preserve every existing packet field and server/client particle cap.
- Add no assets, entities, persistent state, or server-side particle work.
- Apply reduced-motion motif replacement before resolving automatic orientation.
- Reject non-finite transforms and frame offsets before rendering.

---

### Task 1: Pure orientation and transform contracts

**Files:**
- Create: `src/main/java/com/powers/magic/fx/FxOrientation.java`
- Modify: `src/main/java/com/powers/fx/FxGeometry.java`
- Create: `src/test/java/com/powers/fx/FxOrientationTest.java`

**Interfaces:**
- Produces: `FxOrientation.resolve(FxMotif)`.
- Produces: `FxGeometry.transform(Point, FxOrientation, double angleRadians)`.

- [ ] **Step 1: Write failing literal transform and automatic-resolution tests**

```java
Point point = new Point(1.0, 2.0, 3.0);
assertEquals(new Point(1.0, 3.0, 2.0), transform(point, GROUND, 0.0));
assertPointEquals(new Point(-3.0, 2.0, 1.0), transform(point, BILLBOARD, Math.PI / 2));
assertEquals(BILLBOARD, AUTO.resolve(FxMotif.GLYPH));
assertEquals(NATIVE, AUTO.resolve(FxMotif.RING));
assertThrows(IllegalArgumentException.class,
        () -> transform(point, BILLBOARD, Double.NaN));
```

- [ ] **Step 2: Run `./gradlew test --tests 'com.powers.fx.FxOrientationTest'` and confirm missing APIs fail**

- [ ] **Step 3: Implement the enum and distance-preserving transforms with finite-input validation**

- [ ] **Step 4: Re-run focused and complete tests, regenerate `docs/quality/code-audit.md`, and commit**

### Task 2: Choreography placement metadata

**Files:**
- Modify: `src/main/java/com/powers/magic/fx/FxFrame.java`
- Modify: `src/main/java/com/powers/magic/fx/FxChoreography.java`
- Modify: `src/test/java/com/powers/magic/fx/FxChoreographyTest.java`

**Interfaces:**
- Extends: `FxFrame` with `double verticalOffset` and `FxOrientation orientation`.
- Preserves: `FxChoreography.frame(...)` and `finished(...)` call signatures.

- [ ] **Step 1: Write failing expectations for ground anticipation, body-centred impact, rising aftermath, interaction centring, and non-finite offset rejection**

```java
FxFrame anticipation = frame(CAST, 0, false);
assertEquals(FxOrientation.GROUND, anticipation.orientation());
assertEquals(-0.92, anticipation.verticalOffset());
assertEquals(FxOrientation.AUTO, frame(CAST, 7, false).orientation());
assertTrue(frame(CAST, 13, false).verticalOffset() > 0.0);
assertEquals(0.0, frame(INTERACTION, 8, false).verticalOffset());
```

- [ ] **Step 2: Run the focused choreography test and confirm the missing frame fields fail**

- [ ] **Step 3: Add validated metadata to every cast and interaction frame without changing timing, budgets, scales, or reduced-motion clamps**

- [ ] **Step 4: Re-run focused and complete tests, regenerate the Java audit, and commit**

### Task 3: Client staging and release gate

**Files:**
- Modify: `src/client/java/com/powers/client/fx/ClientMagicFx.java`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Regenerate: `docs/quality/code-audit.md`

**Interfaces:**
- Consumes: `frame.orientation().resolve(motif)`, observer-relative Y rotation, `FxGeometry.transform`, and `frame.verticalOffset()`.

- [ ] **Step 1: Compile the client against the extended frame contract and retain the failing visual-placement contract until integration is added**

- [ ] **Step 2: Resolve reduced-motion motif, resolve orientation, compute `atan2(-(viewer.x-origin.x), viewer.z-origin.z)`, transform each scaled point, and add the frame offset to world Y**

- [ ] **Step 3: Document ground rituals and viewer-readable vertical sigils**

- [ ] **Step 4: Run `python3 scripts/audit_java_sources.py` and `./gradlew clean check build` with Java 25**

- [ ] **Step 5: Start a fresh temporary-universe dedicated server, reach `Done`, send `stop`, verify exit 0, commit all outputs, and leave `main` clean**
