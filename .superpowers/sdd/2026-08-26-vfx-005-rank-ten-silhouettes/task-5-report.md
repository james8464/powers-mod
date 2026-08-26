# Task 5 report — real-client gallery and deterministic verifier

## Status

Complete. Task 5 initially landed in `caf72e9454b77edd003321de0112b7d125c13ecb` after
Task 4's distant-width correction `0842fe0fa4089f3e446043d26b181be277b06362`.
The review-fix commit is `bf32ddd0cf2dcc89427e8e5c2f82a14dc6fed4d3`. The final literal gallery traverses the
production server hook, payload, registered receiver/manager, and production world renderer for
all 56 rows without changing the 96-block acceptance distance or any existing threshold.

## Review-fix TDD

The review regressions were written first. The RED run exposed invalid default processed metadata
and missing verifier rejection paths:

```text
python3 -m unittest scripts.tests.test_verify_vfx005_captures
json.decoder.JSONDecodeError: Illegal trailing comma before array end
FAILED (failures/errors across the new profile, inventory, format, overlay, and body tests)
```

The suite now has 19 tests. It executes Gradle resource processing for both the default profile and
`-Pvfx005ClientOnly`, decodes the resulting JSON, and asserts the exact client GameTest entrypoints.
It also covers extra screenshots, noncanonical IDs/paths/metadata/epochs, spoofed PNG content,
outside-ROI lifecycle overlays, outside-ROI wall leakage, advancement toasts, and near-body
obstruction in addition to the original gallery/outline/crosshair/wall cases.

Final GREEN:

```text
python3 -m unittest scripts.tests.test_verify_vfx005_captures
...................
Ran 19 tests in 26.026s
OK
```

The first retained pre-fix gallery correctly failed the hardened verifier:

```text
ValueError: toast or overlay present: vfx005-baseline-size_shift
```

Visual inspection proved that frame had a clean sky: the first detector compared opposite sides of
Minecraft's horizontal sky gradient. The final detector instead rejects high-contrast toast edges
inside the reserved top-right sky region. The deterministic wall scene legitimately occupies that
region, so its two frames use the stronger full-frame pixel equality gate. No toast/overlay is
ignored or tolerated in an acceptance frame.

## Production route and deterministic caster body

Each event temporarily moves the connected authoritative rank-10 `ServerPlayer` to the manifest
distance, calls `RankTenSilhouetteService.afterSuccessfulInnateCast`, restores the observer, and
waits for the exact production manager entry before the registered production renderer is captured.
The fixture never calls a renderer or geometry helper directly.

For the 8-block near row, the integrated server additionally spawns a real, server-synchronized,
player-shaped `POWER_TEST_ACTOR` at the exact event/caster origin `(observer.x, observer.y,
observer.z + 8)`. It is deterministic (no AI, no gravity, zero velocity, invulnerable, persistent,
hidden name) and is removed after capture. Thus an actual body remains at the semantic origin while
the observer returns to the camera. The verifier protects the crosshair and separately requires at
least 128 dark body pixels in the fixed body ROI; the final capture retains 5,908.

The fixture fixes 1280x720, GUI scale 2, FOV 70, yaw 0/pitch -5, render distance 12, noon, clear
weather, stable background, and the recorded particle/reduced-motion settings. It clears settled
advancement toasts and hides chat before capture, including after dimension and reconnect setup.
Resource reload, Overworld/Nether boundary, and saved-world reconnect remain real lifecycle paths.

## Exact inventory and verifier

The manifest must equal a canonical ordered 56-row structure: baseline; all 23 far-normal rows with
`particles=all`; all 23 far-reduced rows with `particles=minimal`; both darkness alignment variants
with `particles=all`; near, wall baseline/event, minimal-particle, reload, dimension, and reconnect.
IDs, safe unique filenames, distances, flags, revisions, and exact lifecycle epochs are fixed.
The screenshot directory must contain exactly those 56 regular files, and Pillow must decode each
as an actual PNG of exactly 1280x720.

The verifier rejects blank/duplicate masks, reduced mismatch, crosshair intrusion, near-body
obstruction, top-right toasts, lifecycle changes anywhere outside the silhouette ROI, and wall
changes anywhere in the full frame. Its output is sorted deterministic JSON with a SHA-256 for every
image.

Final actual metrics:

```text
rowCount=56
farNormalCount=23
farReducedCount=23
pairwiseComparisons=253
minimumReducedOutlineJaccard=1.0
minimumLifecycleOutlineJaccard=1.0
maximumLifecycleBackgroundDriftPixels=180
nearBodyVisiblePixels=5908
wallLeakagePixels=0
crosshairIntrusionPixels=0
```

## Final commands and retained output

Clean literal gallery:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
  ./gradlew runClientGameTest -Pvfx005ClientOnly --rerun-tasks --no-daemon --console=plain
BUILD SUCCESSFUL in 3m 14s
14 actionable tasks: 14 executed
```

Final verifier and hygiene:

```text
python3 -m unittest scripts.tests.test_verify_vfx005_captures
python3 scripts/verify_vfx005_captures.py \
  --manifest build/run/clientGameTest/vfx005-manifest.jsonl \
  --screenshots build/run/clientGameTest/screenshots \
  --output build/vfx005-verification.json
python3 scripts/audit_java_sources.py
python3 scripts/audit_java_sources.py --check
git diff --check
```

All exit 0. Retained paths:

- `build/run/clientGameTest/vfx005-manifest.jsonl` — 56 canonical JSONL rows;
- `build/run/clientGameTest/screenshots` — exactly 56 decoded 1280x720 PNGs;
- `build/run/clientGameTest/logs/latest.log` — final client transcript;
- `build/vfx005-verification.json` — sorted verifier result and 56 frame hashes.

Visual inspection covered baseline and early far rows, normal/reduced flight and forcefield,
darkness variants, near flight with the visible body, both wall frames, minimal particles, reload,
dimension, and reconnect. No advancement toast, chat, or lifecycle overlay remains.

## Review fix round 2

The second review identified three remaining fail-open cases: uniform non-lifecycle overlays,
sub-threshold wall differences, and a generic dark-pixel body gate. Three regressions were added
first and all failed because the prior verifier raised no error:

```text
python3 -m unittest \
  scripts.tests.test_verify_vfx005_captures.Vfx005CaptureVerifierTest.test_uniform_full_frame_tint_on_alignment_variant_is_rejected \
  scripts.tests.test_verify_vfx005_captures.Vfx005CaptureVerifierTest.test_one_channel_wall_leakage_is_rejected \
  scripts.tests.test_verify_vfx005_captures.Vfx005CaptureVerifierTest.test_partial_near_body_occlusion_is_rejected
FFF
Ran 3 tests in 5.882s
FAILED (failures=3)
```

The final suite is 22 tests:

```text
......................
Ran 22 tests in 27.416s
OK
```

### Exact formulas

For every row, the verifier computes the max-channel RGB absolute difference against the canonical
baseline outside the only allowed foreground rectangle. Pixels whose max-channel delta is at least
12 are counted; more than 256 rejects the frame. Baseline is checked reciprocally against the first
normal row so it is not self-referential. Allowed rectangles are silhouette `(430,220)-(850,590)`,
near silhouette/body `(430,220)-(850,610)`, and wall `(240,0)-(1040,600)`. The uniform alignment
tint regression changes the full frame by 20 and is rejected by this all-category background gate.
The final maximum across all 56 rows is 176; the lifecycle-only maximum is 144.

Wall event and wall baseline then receive an independent exact decoded-RGB comparison: every pixel
with any channel delta of at least 1 is counted, and the required count is zero. The one-channel
`+1` regression is rejected. The retained pair is exactly identical and shares SHA-256
`d826d481bd1f5fc35100b4e7878bf26e4177cb0e2ad39c70bb7113707f10354f`.

Body identity uses the fixed interior ROI `(620,385)-(660,510)` and an exact rendered palette
derived from `test_actor.png` under the deterministic light:

```text
(15,17,21) (16,17,21) (24,26,32) (24,26,33) (24,27,33)
(25,27,33) (25,27,34) (37,41,51) (38,42,52) (39,43,53) (51,56,69)
```

The reviewed reference footprint is 4,704 palette pixels. Acceptance requires at least 90% pixel
retention, 113 of 125 occupied rows, and 36 of 40 occupied columns. The partial 24x80 obstruction
regression fails while the final real body records 4,704 pixels, retention 1.0, 125 rows, and 40
columns. The separate protected-crosshair gate remains zero.

### Real-client rerun and artifacts

The hardened verifier correctly rejected the previous gallery before any fixture adjustment:

```text
ValueError: background mismatch: vfx005-far_normal-time_shift has 27344 changed pixels
```

Pixel evidence showed the client sky/fog interpolation was still converging after fixed server
noon. The fixture now waits 500 ticks through that bounded client transition on initial load and
reconnect. The first clean rerun captured and verified all 56 frames but hit a Fabric client-GameTest
shutdown phaser deadlock after the final capture. A live thread dump showed render blocked in
`IntegratedServer.halt`, server and test threads blocked in `ThreadingImpl.enterPhase`; only the
owned process was stopped. The clean retry exited normally:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
  ./gradlew runClientGameTest -Pvfx005ClientOnly --rerun-tasks --no-daemon --console=plain
BUILD SUCCESSFUL in 3m 49s
14 actionable tasks: 14 executed
```

Final verifier metrics are 56 rows, 23 normal, 23 reduced, 253 comparisons, reduced/lifecycle
Jaccard 1.0, all-row background maximum 176, lifecycle background maximum 144, exact wall delta 0,
and crosshair intrusion 0. Representative baseline, alignment, near body, wall pair, and lifecycle
frames were visually inspected with no toast, flash, tint, or overlay.

Both required verifier artifacts were generated independently from the retained frames and match:

```text
1bd72e4f7e67e57f8828cbf5ad76358817d1493964016544c31504bbc9e54ff4  build/vfx005-verification.json
1bd72e4f7e67e57f8828cbf5ad76358817d1493964016544c31504bbc9e54ff4  /tmp/vfx005-capture-verification.json
```

Selected retained frame hashes:

```text
baseline          e957346536e1da5877f0c2767a1aebaa3934242918fa2f8a43a4756beaa53ec6
alignment flight  47becb82163dae68a02e10c8e4221c2b75b67ade02d09d8bf56fa428182b469f
near flight       5db5865b255be9cfc886adf2507528ce44d1ddbb2efc15c0a9f3c0cf58666184
wall pair         d826d481bd1f5fc35100b4e7878bf26e4177cb0e2ad39c70bb7113707f10354f
```

Round-2 fix commit: `2208d7ed4082f2e833f28e6c85e6145a4a27cd3c`.

## Review fix round 3

The final review found that the general far-row foreground allowance was still substantially wider
than the deterministic silhouette, alignment variants lacked an outline-pair gate, and opening an
image as RGB erased source-mode and alpha evidence. Regressions were written first. The prior
verifier did not reject an alignment mismatch, an overlay placed inside the old broad ROI, an RGB
PNG, a transparent hidden-RGB pixel, or an alpha-only wall change; the valid-result test also lacked
the required alignment metric. The exact 256-pixel accepted, 257-pixel rejected, and reciprocal
baseline-direction cases were added as boundary characterizations and already matched the strict
`> 256` contract.

GREEN is the complete 30-test suite:

```text
python3 -m unittest scripts.tests.test_verify_vfx005_captures -v
Ran 30 tests in 22.905s
OK
```

### Tight envelope, pairing, and alpha formulas

The retained far silhouettes have a measured half-open union of
`[628,401,652,425)`. Normal, reduced, alignment, baseline, and lifecycle rows now receive only the
four-pixel-margin envelope `[624,397,656,429)`. Every max-channel RGB difference of at least 12
outside that envelope counts toward the all-row background limit; 256 is accepted and 257 is
rejected. Baseline remains checked reciprocally against the canonical normal size-shift anchor.
Near and wall rows retain their separately reviewed body/world envelopes.

Both darkness alignment variants are paired to the same-power radiant normal mask. Their Jaccard
intersection-over-union must be at least 0.82, matching the reduced/lifecycle outline contract. The
retained flight and forcefield alignment scores are both 1.0, so
`minimumAlignmentOutlineJaccard=1.0`. A synthetic compact, nonblank but wrong alignment mask now
fails, and a title-style rectangle at `(500,280)-(560,330)`—inside the former allowance but outside
the true envelope—fails the background gate.

Every capture must decode as PNG, have source mode exactly RGBA, be 1280x720, and have alpha extrema
exactly `(255,255)` before any RGB comparison. Thus an RGB-mode PNG, transparent hidden RGB, and an
alpha-only wall delta fail closed instead of disappearing during conversion. All 56 retained
captures are RGBA with `(255,255)` alpha extrema. Exact wall equality and its one-channel RGB
regression remain independently enforced.

### Retained evidence re-verification

No Java, fixture, manifest, or screenshot input changed in round 3, so the literal gallery was not
rerun. Both required verifier artifacts were freshly regenerated from the clean retained 56-row
gallery:

```text
python3 scripts/verify_vfx005_captures.py \
  --screenshots build/run/clientGameTest/screenshots \
  --manifest build/run/clientGameTest/vfx005-manifest.jsonl \
  --output build/vfx005-verification.json
python3 scripts/verify_vfx005_captures.py \
  --screenshots build/run/clientGameTest/screenshots \
  --manifest build/run/clientGameTest/vfx005-manifest.jsonl \
  --output /tmp/vfx005-capture-verification.json
```

The schema-2 metrics are 56 rows, 23 normal, 23 reduced, 253 pairwise comparisons,
reduced/alignment/lifecycle minimum Jaccard 1.0, all-row maximum background drift 176, lifecycle
maximum 144, body identity 4,704, retention 1.0, 125 rows, 40 columns, wall leakage 0, and
crosshair intrusion 0. The artifacts are byte-identical:

```text
5771be8b457e53f08ba4e414c29df4bcf7ee776c5e33d8c5954fcc89f9493f58  build/vfx005-verification.json
5771be8b457e53f08ba4e414c29df4bcf7ee776c5e33d8c5954fcc89f9493f58  /tmp/vfx005-capture-verification.json
```

The retained frame hashes remain unchanged: baseline `e9573465…ec6`, alignment flight
`47becb82…69f`, near flight `5db5865b…184`, and the identical wall pair `d826d481…354f`.

## Review fix round 4

The round-3 report had treated inclusive pixel y=424 as if the half-open bottom were 424. Direct
threshold-mask inspection proves that normal and reduced `plant_healing_acceleration`, normal and
reduced `double_health`, and post-reconnect `double_health` each contain two silhouette pixels at
y=424. The actual half-open union is therefore `[628,401,652,425)`. The prior envelope bottom 428
provided only the three empty rows y=425–427; the corrected bottom 429 provides the intended four
rows y=425–428.

The regression was written before the coordinate change. A paired normal/reduced synthetic profile
whose foreground spans y=424–428 remained accepted, but the reported normal mask contained only 42
pixels instead of the independently derived 43, proving y=428 was excluded. After changing only the
half-open bottom from 428 to 429, that test records all 43 pixels. A separate 288-pixel overlay
starting immediately outside at y=429 is rejected by the background gate, so the test does not
derive either boundary from the verifier constant.

```text
python3 -m unittest \
  scripts.tests.test_verify_vfx005_captures.Vfx005CaptureVerifierTest.test_far_foreground_includes_four_pixel_margin_below_y424 \
  scripts.tests.test_verify_vfx005_captures.Vfx005CaptureVerifierTest.test_overlay_immediately_below_far_envelope_is_rejected -v

RED: AssertionError: 43 != 42
GREEN: Ran 2 tests in 1.356s; OK

python3 -m unittest scripts.tests.test_verify_vfx005_captures -v
Ran 32 tests in 37.008s
OK
```

No Java, gallery fixture, manifest, or screenshot changed, so the literal gallery was not rerun.
Both verifier executions over the retained 56 captures pass with the same metrics, and because the
retained silhouette pixels were already inside the old bound, the regenerated JSON remains
byte-identical at SHA-256
`5771be8b457e53f08ba4e414c29df4bcf7ee776c5e33d8c5954fcc89f9493f58` for both
`build/vfx005-verification.json` and `/tmp/vfx005-capture-verification.json`.

## Concerns

The Fabric shutdown deadlock was transient and did not recur on the clean accepted run. Round 4 is
verifier-only and found no product, fixture, or retained-evidence concern.
