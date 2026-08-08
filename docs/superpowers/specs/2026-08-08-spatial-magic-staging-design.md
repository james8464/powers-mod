# Spatial Magic Staging Design

**Date:** 2026-08-08

**Target:** Fabric, Minecraft Java Edition 26.2, Java 25

**Status:** Approved through the user's standing authorization to make autonomous design decisions

## Purpose

Make semantic magic geometry occupy an intentional place and plane. Cast anticipation must form beneath the caster rather than through their torso, and vertical glyphs must remain readable to every observer instead of disappearing edge-on.

## Approaches Considered

- **Native geometry only:** preserves current shapes but leaves vertical glyphs readable from only two directions.
- **Billboard every motif:** guarantees visibility but makes ground rings, roots, spirals, and three-dimensional fractures rotate unnaturally with each viewer.
- **Semantic frame orientation — selected:** frames request ground, billboard, native, or motif-aware placement. The client applies the transform locally using its own view direction, so the server packet remains unchanged.

## Model

`FxOrientation` defines `AUTO`, `NATIVE`, `GROUND`, and `BILLBOARD`. `AUTO` resolves glyph, eclipse, and fork motifs to billboard orientation; all other motifs remain native. `FxFrame` gains a finite vertical offset and orientation alongside its existing budget, radius, and velocity scales.

Cast anticipation uses a ground-oriented glyph at `-0.92` blocks relative to the existing body-centred event anchor. Release and impact use motif-aware orientation at body height. Aftermath remains motif-aware and drifts slightly upward. Interaction events remain centred on their collision point and use motif-aware orientation.

`FxGeometry.transform` performs pure rotations:

- native returns the point unchanged;
- ground maps the local vertical plane onto X/Z while preserving thickness as height;
- billboard rotates around world Y by the observer-relative angle.

Rotations preserve finite coordinates and point distance from the local origin. Every client independently faces billboard geometry toward itself; this does not change authoritative state or reveal information.

## Performance and Accessibility

Transforms are constant-time arithmetic inside the existing hard 96-point frame budget. No additional particles, packets, ticking state, entities, assets, or server work are added. Reduced-motion motif replacement occurs before orientation resolution, so its static rings and glyphs use the correct plane.

## Verification

Pure tests cover literal ground and billboard transforms, distance preservation, automatic motif orientation, frame offsets, invalid offset rejection, and unchanged timing/accessibility contracts. Release verification remains the Java audit, `clean check build`, and an isolated dedicated-server startup/shutdown smoke test.
