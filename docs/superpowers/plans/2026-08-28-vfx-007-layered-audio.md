# VFX-007 Layered Magical Audio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route all 16 POWERS semantic sound cues through safe positional near/mid/far layers with obstruction falloff, subtitles, bounded mixing, and reduced-tinnitus Celestial audio.

**Architecture:** A common immutable catalogue and pure rules define the only accepted cues and mix decisions. The server sends one bounded semantic payload to eligible listeners after existing gameplay commits; the client revalidates it, chooses one layer using camera distance and a single obstruction ray, applies comfort and headroom policy, then plays one positional sound. Deterministic asset generation, quantitative audio validation, GameTests, and real-client audit evidence close the task.

**Tech Stack:** Java 25, Minecraft 26.2, Fabric Loader/API networking and GameTests, JUnit 5, Python 3 with NumPy/SoundFile, OGG/Vorbis, Gradle 9.5.1.

**Spec:** `docs/superpowers/specs/2026-08-28-vfx-007-layered-audio-design.md`

## Global Constraints

- Cover exactly the 16 semantic cues registered by `PowersSounds`, including VFX-008's `beam_ring` and `boss_impact_ring`.
- Preserve gameplay success, cost, timing, damage, protection, tracking, persistence, and world state.
- Emit only after the owning gameplay operation has committed; no duplicate vanilla semantic playback.
- Use intimate 8/28/72, standard 12/48/128, and world 20/96/256 block thresholds.
- Obstruction advances one layer toward far and multiplies gain by 0.45.
- Cap playback at eight global sounds, four per group in four ticks, and final gain 0.90.
- Keep client event/deduplication state bounded to 256 entries and clear it on disconnect, dimension change, and resource reload.
- Generate 48 ordinary layers plus three reduced Celestial layers; all are mono 44.1 kHz Vorbis with peak at or below 0.707.
- Do not add an options screen, music, ambient loops, or change unrelated vanilla sound calls.

---

### Task 1: Catalogue, range, obstruction, and headroom rules

**Files:**
- Create: `src/main/java/com/powers/audio/LayeredAudioLayer.java`
- Create: `src/main/java/com/powers/audio/LayeredAudioProfile.java`
- Create: `src/main/java/com/powers/audio/LayeredAudioCue.java`
- Create: `src/main/java/com/powers/audio/LayeredAudioRules.java`
- Create: `src/main/java/com/powers/audio/package-info.java`
- Create: `src/test/java/com/powers/audio/LayeredAudioCatalogueTest.java`
- Create: `src/test/java/com/powers/audio/LayeredAudioRulesTest.java`

**Interfaces:**
- Consumes: no VFX-007 code.
- Produces: `LayeredAudioCue.fromNetworkId(int)` and `forSemanticName(String)`; `LayeredAudioRules.resolve(cue, distance, obstructed, reducedTinnitus, baseGain, concurrent)` returning `Optional<ResolvedLayer>`.

- [ ] **Step 1: Write catalogue tests that require 16 stable IDs and complete metadata**

```java
assertEquals(16, LayeredAudioCue.values().length);
for (int id = 0; id < 16; id++) {
    LayeredAudioCue cue = LayeredAudioCue.values()[id];
    assertEquals(id, cue.networkId());
    assertEquals(Optional.of(cue), LayeredAudioCue.fromNetworkId(id));
    assertFalse(cue.subtitleKey().isBlank());
    assertNotNull(cue.profile());
    assertNotNull(cue.group());
}
assertEquals(Optional.empty(), LayeredAudioCue.fromNetworkId(-1));
assertEquals(Optional.empty(), LayeredAudioCue.fromNetworkId(16));
```

- [ ] **Step 2: Write RED boundary tests for layer, obstruction, comfort, and headroom**

```java
assertEquals(NEAR, resolve(RUNE_HUM, 8.0, false).layer());
assertEquals(MID, resolve(RUNE_HUM, 8.0001, false).layer());
assertEquals(FAR, resolve(RUNE_HUM, 28.0001, false).layer());
assertTrue(LayeredAudioRules.resolve(RUNE_HUM, 72.0001, false, false, 1, 1).isEmpty());
assertEquals(MID, resolve(RUNE_HUM, 4.0, true).layer());
assertEquals(0.45F, resolve(RUNE_HUM, 4.0, true).obstructionGain());
assertTrue(resolve(CELESTIAL_RING, 4.0, false, true).reducedTinnitus());
assertFalse(resolve(BEAM_RING, 4.0, false, true).reducedTinnitus());
assertEquals(0.45F, LayeredAudioRules.headroom(4, 0.90F), 0.0001F);
```

- [ ] **Step 3: Run the focused tests and confirm RED**

Run: `./gradlew test --tests 'com.powers.audio.*' --no-daemon --console=plain`

Expected: compilation fails because the audio catalogue and rules do not exist.

- [ ] **Step 4: Implement the immutable catalogue and pure resolver**

```java
public static Optional<ResolvedLayer> resolve(LayeredAudioCue cue, double distance,
        boolean obstructed, boolean reducedTinnitus, float baseGain, int concurrent) {
    if (cue == null || !Double.isFinite(distance) || distance < 0.0F
            || !Float.isFinite(baseGain) || baseGain <= 0.0F) return Optional.empty();
    LayeredAudioLayer layer = cue.profile().layer(distance).orElse(null);
    if (layer == null) return Optional.empty();
    if (obstructed) layer = layer.softer();
    float obstructionGain = obstructed ? 0.45F : 1.0F;
    float gain = Math.min(0.90F, Math.clamp(baseGain, 0.0F, 4.0F)
            * obstructionGain / (float) Math.sqrt(Math.clamp(concurrent, 1, 8)));
    return Optional.of(new ResolvedLayer(layer, gain, obstructionGain,
            reducedTinnitus && cue == LayeredAudioCue.CELESTIAL_RING));
}
```

- [ ] **Step 5: Run tests, source audit, and commit**

Run: `./gradlew test --tests 'com.powers.audio.*' auditJavaSources --no-daemon --console=plain`

Expected: PASS.

```bash
git add src/main/java/com/powers/audio src/test/java/com/powers/audio
git commit -m "feat(audio): define bounded layered cue rules"
```

### Task 2: Deterministic layered assets, registrations, subtitles, and validation

**Files:**
- Create: `scripts/generate_layered_magic_sounds.py`
- Create: `scripts/validate_layered_audio.py`
- Create: `scripts/tests/test_validate_layered_audio.py`
- Modify: `scripts/validate_resources.py`
- Modify: `src/main/resources/assets/powers/sounds.json`
- Modify: `src/main/resources/assets/powers/lang/en_us.json`
- Create: `src/main/resources/assets/powers/sounds/magic/layered/*.ogg` (51 files)
- Modify: `src/main/java/com/powers/PowersSounds.java`
- Create: `src/test/java/com/powers/audio/LayeredAudioResourcesTest.java`

**Interfaces:**
- Consumes: `LayeredAudioCue.assetId(layer, reduced)` and the 16 committed base masters.
- Produces: 51 deterministic resources, 16 subtitle strings, `PowersSounds.layer(cue, layer, reduced)`, and JSON audio metrics.

- [ ] **Step 1: Write RED Python tests for inventory and quantitative acceptance**

```python
report = validate_layered_audio.validate(ROOT)
self.assertEqual(51, report["assetCount"])
self.assertEqual(0, len(report["errors"]))
for cue in report["cues"]:
    self.assertGreater(cue["effectiveRms"]["near"], cue["effectiveRms"]["mid"])
    self.assertGreater(cue["effectiveRms"]["mid"], cue["effectiveRms"]["far"])
    self.assertLessEqual(cue["peak"], 0.707)
    self.assertLessEqual(cue["farCentroid"], cue["nearCentroid"] * 0.80)
self.assertLessEqual(report["reducedCelestialHighBandRatio"], 0.30)
```

- [ ] **Step 2: Run the validator test and confirm RED**

Run: `python3 -m unittest scripts.tests.test_validate_layered_audio -v`

Expected: FAIL because the generator, validator, and 51 layered assets are absent.

- [ ] **Step 3: Implement deterministic DSP from committed masters**

```python
def layer(master, kind):
    signal = np.asarray(master, dtype=np.float64)
    if kind == "near":
        shaped, gain = one_pole_lowpass(signal, 15_000), 0.88
    elif kind == "mid":
        shaped, gain = one_pole_lowpass(signal, 7_000), 0.58
    else:
        shaped, gain = one_pole_lowpass(signal, 2_800), 0.32
    shaped = soften_transient(shaped, attack_ms={"near": 2, "mid": 8, "far": 18}[kind])
    return limit_peak(shaped * gain, 0.707).astype(np.float32)
```

Use deterministic seeds and fixed SoundFile Vorbis settings. Generate reduced Celestial from a low-mid contour derived from the master envelope, with the 4–12 kHz band removed rather than simple gain reduction.

- [ ] **Step 4: Register layered events and subtitles**

For each cue, add `cue.near`, `cue.mid`, and `cue.far` entries with the same `subtitle` key. Add three `celestial_ring.reduced.*` entries. Add exactly 16 `subtitles.powers.<cue>` English strings. Extend `PowersSounds` with a `LayeredSoundSet` map while preserving the existing base constants for compatibility and `fromSound` lookup.

- [ ] **Step 5: Extend strict resource validation**

Require exact layered entry/file inventory, mono 44.1 kHz Vorbis, translation coverage, no duplicate JSON keys, and the quantitative validator's zero-error report. Do not make generation part of ordinary Gradle builds; committed binaries must be reproducible by an explicit generator run.

- [ ] **Step 6: Generate twice and prove byte stability**

Run:

```bash
python3 scripts/generate_layered_magic_sounds.py
find src/main/resources/assets/powers/sounds/magic/layered -type f -print0 | sort -z | xargs -0 shasum -a 256 > /tmp/vfx007-a
python3 scripts/generate_layered_magic_sounds.py
find src/main/resources/assets/powers/sounds/magic/layered -type f -print0 | sort -z | xargs -0 shasum -a 256 > /tmp/vfx007-b
diff -u /tmp/vfx007-a /tmp/vfx007-b
```

Expected: no diff and 51 rows.

- [ ] **Step 7: Run resource tests and commit**

Run: `python3 -m unittest scripts.tests.test_validate_layered_audio -v && ./gradlew validatePowerResources test --tests com.powers.audio.LayeredAudioResourcesTest --no-daemon --console=plain`

Expected: PASS.

```bash
git add scripts/generate_layered_magic_sounds.py scripts/validate_layered_audio.py scripts/tests/test_validate_layered_audio.py scripts/validate_resources.py src/main/java/com/powers/PowersSounds.java src/main/resources/assets/powers src/test/java/com/powers/audio/LayeredAudioResourcesTest.java
git commit -m "feat(audio): author distance-layered magic bank"
```

### Task 3: Bounded semantic payload and server recipient service

**Files:**
- Create: `src/main/java/com/powers/network/LayeredAudioPackets.java`
- Create: `src/main/java/com/powers/audio/LayeredAudioService.java`
- Modify: `src/main/java/com/powers/network/PowersPackets.java`
- Modify: `src/main/java/com/powers/testing/network/PacketFaultFamilies.java`
- Modify: `src/main/java/com/powers/testing/network/PacketFaultStreams.java`
- Create: `src/test/java/com/powers/network/LayeredAudioPacketsTest.java`
- Create: `src/test/java/com/powers/audio/LayeredAudioServiceTest.java`
- Modify: `src/test/java/com/powers/testing/network/PacketFaultFamiliesTest.java`

**Interfaces:**
- Consumes: `LayeredAudioCue` and profile maximum range.
- Produces: `LayeredAudioPackets.Payload(long eventId, LayeredAudioCue cue, Identifier dimension, double x, double y, double z, float gain, float pitch, long emittedGameTime)` and `LayeredAudioService.emit(ServerLevel, Vec3, LayeredAudioCue, float, float)`.

- [ ] **Step 1: Write RED codec, validation, recipient, and packet-family tests**

Require round-trip equality, unknown cue refusal, finite world bounds, gain `[0.01,4.0]`, pitch `[0.25,4.0]`, no recipients across dimensions/outside far range, exactly one send for each eligible payload-capable listener, and stable fault-stream key from event ID.

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `./gradlew test --tests 'com.powers.network.LayeredAudioPacketsTest' --tests 'com.powers.audio.LayeredAudioServiceTest' --tests 'com.powers.testing.network.PacketFaultFamiliesTest' --no-daemon --console=plain`

Expected: compilation fails because payload/service types are absent.

- [ ] **Step 3: Implement strict payload construction and codec**

```java
public Payload {
    Objects.requireNonNull(cue, "cue");
    Objects.requireNonNull(dimension, "dimension");
    if (eventId < 0 || emittedGameTime < 0 || !finitePosition(x, y, z)
            || !Float.isFinite(gain) || gain < 0.01F || gain > 4.0F
            || !Float.isFinite(pitch) || pitch < 0.25F || pitch > 4.0F) {
        throw new IllegalArgumentException("Invalid layered audio payload");
    }
}
```

Decode cue IDs through `Optional`; malformed IDs throw the codec's normal decode exception rather than mapping to a fallback cue.

- [ ] **Step 4: Implement bounded fan-out and registration**

Use the level's player list, exact dimension equality, squared eye distance, `ServerPlayNetworking.canSend`, and `PowersPlayNetworking.send`. Event IDs combine server tick with a bounded per-tick sequence that resets each tick; no collection is allocated or persisted.

- [ ] **Step 5: Run focused tests and commit**

Run: `./gradlew test --tests 'com.powers.network.LayeredAudioPacketsTest' --tests 'com.powers.audio.LayeredAudioServiceTest' --tests 'com.powers.testing.network.PacketFaultFamiliesTest' --no-daemon --console=plain`

Expected: PASS.

```bash
git add src/main/java/com/powers/network/LayeredAudioPackets.java src/main/java/com/powers/audio/LayeredAudioService.java src/main/java/com/powers/network/PowersPackets.java src/main/java/com/powers/testing/network/PacketFaultFamilies.java src/main/java/com/powers/testing/network/PacketFaultStreams.java src/test/java/com/powers/network/LayeredAudioPacketsTest.java src/test/java/com/powers/audio/LayeredAudioServiceTest.java src/test/java/com/powers/testing/network/PacketFaultFamiliesTest.java
git commit -m "feat(audio): send bounded semantic sound events"
```

### Task 4: Client comfort config, payload ledger, obstruction, and mixer

**Files:**
- Create: `src/client/java/com/powers/client/audio/ClientAudioComfortConfig.java`
- Create: `src/client/java/com/powers/client/audio/ClientLayeredAudioState.java`
- Create: `src/client/java/com/powers/client/audio/ClientLayeredAudioMixer.java`
- Create: `src/client/java/com/powers/client/audio/PositionalLayeredSound.java`
- Create: `src/client/java/com/powers/client/audio/package-info.java`
- Modify: `src/client/java/com/powers/client/PowersClient.java`
- Create: `src/test/java/com/powers/client/audio/ClientAudioComfortConfigTest.java`
- Create: `src/test/java/com/powers/client/audio/ClientLayeredAudioStateTest.java`
- Create: `src/test/java/com/powers/client/audio/ClientLayeredAudioMixerSourceTest.java`

**Interfaces:**
- Consumes: `LayeredAudioPackets.Payload`, `LayeredAudioRules`, and `PowersSounds.layer`.
- Produces: `ClientLayeredAudioMixer.handle(payload)`, `resetConnectionEpoch()`, `reload()`, `metrics()`, and config `reducedTinnitus()`.

- [ ] **Step 1: Write RED config, bounded-ledger, burst, and lifecycle tests**

Test absent/malformed config defaults false; valid JSON enables only reduced tinnitus; duplicate and older event IDs are ignored; the 257th accepted ID evicts deterministically; same cue/origin cell coalesces within four ticks; fifth group and ninth global offers are dropped; resets clear IDs and counters.

- [ ] **Step 2: Write a RED source-boundary test for real positional playback**

Require one `SoundSource.PLAYERS` sound instance at payload coordinates, linear attenuation, exactly one selected event, one `ClipContext` obstruction query, and no `SimpleSoundInstance.forUI` or three-layer loop.

- [ ] **Step 3: Run focused tests and confirm RED**

Run: `./gradlew test --tests 'com.powers.client.audio.*' --no-daemon --console=plain`

Expected: compilation fails because client audio classes are absent.

- [ ] **Step 4: Implement config and pure client state**

Parse only `{"reducedTinnitus":true|false}` from `config/powers-client.json`; ignore unknown fields, cap file size at 4 KiB, and log malformed input once. Implement the 256-entry ID ledger with access-ordered `LinkedHashMap` and explicit eldest removal.

- [ ] **Step 5: Implement listener classification and one positional sound**

On the client thread: reject missing player/level, dimension mismatch, payload age outside `[0,100]`, or duplicate ID; measure camera-to-origin distance; perform one collider-only clip; resolve the cue/layer/gain; apply group admission; and play one `PositionalLayeredSound` using the matching registered layer event. The sound's coordinates remain the server origin and `relative=false`.

- [ ] **Step 6: Wire receiver and lifecycle resets**

Register the payload in `PowersClient`; reset on DISCONNECT and dimension transition; recreate config/state on resource reload. Preserve diagnostics as bounded scalar counters.

- [ ] **Step 7: Run focused tests and commit**

Run: `./gradlew test --tests 'com.powers.client.audio.*' auditJavaSources --no-daemon --console=plain`

Expected: PASS.

```bash
git add src/client/java/com/powers/client/audio src/client/java/com/powers/client/PowersClient.java src/test/java/com/powers/client/audio
git commit -m "feat(audio): mix listener-specific layered cues"
```

### Task 5: Production semantic-hook migration and Celestial comfort path

**Files:**
- Modify: `src/main/java/com/powers/fx/PowerFx.java`
- Modify: `src/main/java/com/powers/PowersSounds.java`
- Modify: `src/main/java/com/powers/fx/ServerFxTransport.java`
- Delete: `src/main/java/com/powers/network/EventAudioPackets.java`
- Delete: `src/client/java/com/powers/client/fx/ClientEventAudio.java`
- Modify: `src/client/java/com/powers/client/fx/ClientCelestialRuinFx.java`
- Modify: `src/main/java/com/powers/boss/FirstVesselRitual.java`
- Modify: `src/main/java/com/powers/realm/RealmHeraldManager.java`
- Modify: `src/test/java/com/powers/network/EventAudioPacketsTest.java` (replace with layered-payload assertions, then rename the class/file)
- Modify: `src/gametest/java/com/powers/network/PacketFaultGameTests.java`
- Modify: `src/gametest/java/com/powers/gametest/PowersClientGameTests.java`
- Create: `src/test/java/com/powers/audio/LayeredAudioProductionBoundaryTest.java`

**Interfaces:**
- Consumes: `PowersSounds.fromSound`, `LayeredAudioService.emit`, and client mixer local-Celestial entry.
- Produces: one central semantic routing boundary in `PowerFx.sound` and no direct production playback of a registered POWERS base cue.

- [ ] **Step 1: Write RED exhaustive source-boundary tests**

Scan main/client sources and require that every `PowersSounds` base event routed through `PowerFx.sound` reaches `LayeredAudioService`, legacy EventAudio types are absent, direct `player.playSound(PowersSounds.CELESTIAL_RING...)` is absent, vanilla `SoundEvents` calls remain vanilla, and all 16 catalogue cues have at least one production or acceptance emission path.

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `./gradlew test --tests com.powers.audio.LayeredAudioProductionBoundaryTest --no-daemon --console=plain`

Expected: FAIL on legacy direct semantic playback.

- [ ] **Step 3: Route registered semantic events centrally**

```java
public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
    Optional<LayeredAudioCue> cue = PowersSounds.fromSound(sound);
    if (cue.isPresent()) {
        LayeredAudioService.emit(level, pos, cue.get(), volume, pitch);
        return;
    }
    level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
}
```

Collapse `eventSound` into the same service and migrate its two semantic enum cases to catalogue cues. Do not alter call ordering around committed gameplay effects.

- [ ] **Step 4: Route Celestial ringing through the mixer comfort decision**

Replace both local base-ring calls with one mixer API that accepts the existing authoritative volume/pitch and selects ordinary or reduced layered Celestial assets. Retain current whiteout/ringing timing and strongest-event aggregation.

- [ ] **Step 5: Update packet-fault and client tests, run broad focused gate, and commit**

Run: `./gradlew test --tests 'com.powers.audio.*' --tests 'com.powers.network.*Audio*' --tests 'com.powers.client.audio.*' --tests 'com.powers.spell.CelestialRuinPresentationTest' --no-daemon --console=plain`

Expected: PASS.

```bash
git add -A src/main/java/com/powers src/client/java/com/powers src/test/java/com/powers src/gametest/java/com/powers
git commit -m "refactor(audio): route semantic cues through layered mixer"
```

### Task 6: GameTests and production-client acceptance instrumentation

**Files:**
- Create: `src/gametest/java/com/powers/gametest/LayeredAudioGameTests.java`
- Create: `src/gametest/java/com/powers/gametest/LayeredAudioClientAcceptance.java`
- Modify: `src/gametest/java/com/powers/gametest/PowersClientGameTests.java`
- Modify: `src/gametest/java/com/powers/network/PacketFaultGameTests.java`
- Modify: `src/client/java/com/powers/client/acceptance/AcceptanceClientScript.java`
- Modify: `src/client/java/com/powers/client/acceptance/AcceptanceClientAgent.java`
- Modify: corresponding acceptance parser tests

**Interfaces:**
- Consumes: production server service and client mixer metrics.
- Produces: live recipient/lifecycle GameTests and bounded JSONL audit rows for evidence capture.

- [ ] **Step 1: Write RED GameTests for recipients and lifecycle**

Cover exact near/mid/far/final-radius boundaries, outside radius, different dimension, unsupported capability, one payload per listener, packet delay/loss/duplication convergence, stale delivery rejection, reconnect, dimension transition, and zero changes to energy/health/world blocks.

- [ ] **Step 2: Add RED acceptance-script commands**

Add strict commands `audio_emit <cue> <distance> <wall|open>`, `audio_comfort <ordinary|reduced>`, and `audio_assert <layer> <admitted|dropped>`. Unknown cue/mode/layer or extra fields fail parsing.

- [ ] **Step 3: Implement audit rows and fixture**

Each admitted or rejected event writes cue, layer, distance, obstructed, effective gain, result, subtitle key, reduced-tinnitus flag, dimension, event ID, and implementation SHA. Bound one session to 128 rows and redact absolute paths/player identities.

- [ ] **Step 4: Run GameTests and focused parser tests**

Run: `./gradlew runGameTest test --tests 'com.powers.client.acceptance.*' --rerun-tasks --no-daemon --console=plain`

Expected: all required GameTests and focused JUnit tests pass without lag warnings.

- [ ] **Step 5: Commit**

```bash
git add src/gametest src/client/java/com/powers/client/acceptance src/test/java/com/powers/client/acceptance
git commit -m "test(audio): prove layered delivery and lifecycle"
```

### Task 7: Deterministic VFX-007 evidence package

**Files:**
- Create: `scripts/verify_vfx007_audio.py`
- Create: `scripts/package_vfx007_evidence.py`
- Create: `scripts/tests/test_verify_vfx007_audio.py`
- Create: `scripts/tests/test_package_vfx007_evidence.py`
- Create: `docs/verification/evidence/2026-08-28-vfx-007/` package contents

**Interfaces:**
- Consumes: 51 assets, metrics validator, client JSONL audit, subtitle screenshots, exact implementation SHA, and build logs.
- Produces: deterministic report, inventory, SHA256SUMS, privacy-clean archive, and evidence README.

- [ ] **Step 1: Write RED verifier/package tests**

Require 16×3 open rows, 16 wall rows, one burst set, ordinary/reduced Celestial rows, reload/reconnect/dimension lifecycle rows, all 16 subtitle keys, exact SHA equality, quantitative metrics, sorted inventory, checksum recomputation, archive byte determinism, and rejection of private paths or identities.

- [ ] **Step 2: Run tests and confirm RED**

Run: `python3 -m unittest scripts.tests.test_verify_vfx007_audio scripts.tests.test_package_vfx007_evidence -v`

Expected: FAIL because verifier/package scripts and evidence are absent.

- [ ] **Step 3: Implement verifier and deterministic packager**

Use strict schema version 1, duplicate-key rejection, exact finite cue/layer sets, normalized LF text, sorted POSIX paths, fixed archive mtimes/modes, and SHA-256 over every committed evidence file except the checksum file itself.

- [ ] **Step 4: Capture production-client evidence**

Run the exact implementation SHA with a real client and dedicated server. Exercise 48 open distance rows, 16 wall rows, capped burst, subtitles, resource reload, reconnect, dimension change, and ordinary/reduced Celestial. Retain audit logs, subtitle screenshots, audio metric report, spectrogram summary, and limitations stating that no microphone recording is used as source-faithful proof.

- [ ] **Step 5: Verify, package twice, compare, and privacy-scan**

Run:

```bash
python3 scripts/verify_vfx007_audio.py docs/verification/evidence/2026-08-28-vfx-007
python3 scripts/package_vfx007_evidence.py docs/verification/evidence/2026-08-28-vfx-007 --output /tmp/vfx007-a.tar.gz
python3 scripts/package_vfx007_evidence.py docs/verification/evidence/2026-08-28-vfx-007 --output /tmp/vfx007-b.tar.gz
cmp /tmp/vfx007-a.tar.gz /tmp/vfx007-b.tar.gz
rg -n '/Users/|\.worktrees|file://|james8464' docs/verification/evidence/2026-08-28-vfx-007 && exit 1 || true
```

Expected: verifier PASS, archives identical, privacy scan empty.

- [ ] **Step 6: Run focused tests and commit evidence**

Run: `python3 -m unittest scripts.tests.test_verify_vfx007_audio scripts.tests.test_package_vfx007_evidence scripts.tests.test_validate_layered_audio -v`

Expected: PASS.

```bash
git add scripts/verify_vfx007_audio.py scripts/package_vfx007_evidence.py scripts/tests docs/verification/evidence/2026-08-28-vfx-007
git commit -m "docs(audio): package layered sound acceptance"
```

### Task 8: Review, full verification, closure, merge, and push

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/planning/IMPROVEMENT_BACKLOG.md`
- Modify: `docs/superpowers/plans/2026-08-12-stages-1-8-completion.md`
- Modify: `docs/verification/evidence/2026-08-28-vfx-007/README.md`
- Modify: `docs/verification/evidence/2026-08-28-vfx-007/build-metadata.json`
- Modify: `docs/verification/evidence/2026-08-28-vfx-007/SHA256SUMS`

**Interfaces:**
- Consumes: exact implementation/evidence SHA and all task gates.
- Produces: independently reviewed VFX-007 closure integrated and pushed on `main`.

- [ ] **Step 1: Run the literal finalized-head aggregate**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew check --rerun-tasks --no-daemon --console=plain`

Expected: all GameTests, JUnit tests, Python tests, resource validation, and audits pass with `BUILD SUCCESSFUL`. Diagnose failures from evidence; do not filter the gate or alter unrelated workloads.

- [ ] **Step 2: Obtain independent code/evidence review**

Reviewer checks all spec requirements, catalogue completeness, commit-bound hooks, payload validation, bounded fan-out, obstruction and comfort behavior, mixer limits, quantitative audio metrics, client evidence, checksums, privacy, and claim truthfulness. Resolve every P0/P1/P2 finding, rerun the focused command from its owning task plus the literal full gate, and repeat review until READY.

- [ ] **Step 3: Reconcile public claims only after acceptance**

Mark VFX-007 complete in the stage plan, remove only the VFX-007 backlog row, and record exact test totals and implementation/evidence SHA in README, CHANGELOG, and evidence metadata. Regenerate checksums and rerun verifier/package/privacy gates.

- [ ] **Step 4: Commit and push closure**

```bash
git add README.md CHANGELOG.md docs/planning/IMPROVEMENT_BACKLOG.md docs/superpowers/plans/2026-08-12-stages-1-8-completion.md docs/verification/evidence/2026-08-28-vfx-007
git commit -m "docs(audio): close layered magical audio acceptance"
git push origin vfx-007-layered-audio
```

- [ ] **Step 5: Rerun literal gate on closure head and obtain final READY review**

Run the unchanged Java 25 literal command from Step 1. Require `BUILD SUCCESSFUL`, a clean branch synchronized with origin, and a fresh final READY review.

- [ ] **Step 6: Fast-forward, verify merged main, push, and prove hygiene**

```bash
git -C '/Users/james/Developer/Minecraft mods/POWERS' merge --ff-only vfx-007-layered-audio
git -C '/Users/james/Developer/Minecraft mods/POWERS' status --short
```

Run the literal full Gradle gate from the main checkout, then:

```bash
git -C '/Users/james/Developer/Minecraft mods/POWERS' push origin main
test "$(git -C '/Users/james/Developer/Minecraft mods/POWERS' rev-parse HEAD)" = "$(git -C '/Users/james/Developer/Minecraft mods/POWERS' rev-parse origin/main)"
git -C '/Users/james/Developer/Minecraft mods/POWERS' worktree list --porcelain
```

Inspect every listed worktree with `git status --porcelain`; all must be empty. Continue strict Stage 1–8 order with `INT-008` only after this proof.
