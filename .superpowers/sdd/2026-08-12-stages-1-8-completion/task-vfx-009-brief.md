# VFX-009 — Ancient-white Light Realm sky

## Status and locked scope

Implement a dedicated client-only Light Realm sky presentation while retaining the existing pure-white resource and render-state fallback. This unit changes no dimension data, gameplay, networking, server authority, save data, travel, weather, or resource-pack selection. VFX-011 remains open and its accepted evidence is not rewritten by this task.

The renderer must remain recognisable as an infinite field of white ancient magic: a stable white sky disc, several large geometric halo/rune silhouettes, restrained warm-white/gold tonal separation, and no sun, moon, stars, horizon void, or ordinary day/night tint. The geometry must remain impressive at long view distances without obscuring terrain readability.

## Chosen architecture

Use the existing vanilla `SkyRenderer` boundary that already supplies the proven static-white Sodium-compatible fallback. `LightRealmSkyMixin` continues to force the authoritative extracted sky state to white and stores one immutable client frame profile on that `SkyRenderer` instance. A second narrow injection after the vanilla white disc asks a dedicated `LightRealmSkyRenderer` to draw untextured procedural geometry using a built-in translucent position-colour pipeline and GPU buffers.

This is preferred over a `LevelRenderer` replacement because Sodium is more likely to replace or reorder the outer world renderer. It is preferred over a core shader or texture skybox because those are fragile under shader/resource packs and would make missing assets fatal. Geometry uses a built-in translucent position-colour pipeline; Minecraft 26.2's opaque `RenderPipelines.SKY` is explicitly unsuitable for alpha layers. No Sodium class is referenced and no compatibility-mod detection changes behaviour.

The enhanced renderer is additive and fail-closed. If its pipeline/buffer boundary is unavailable or one render attempt fails, a session circuit breaker stops enhanced draws, logs once, and leaves the already-rendered static white disc intact. Resource reload/reset may construct a fresh `SkyRenderer`, naturally rebuilding owned buffers. `close()` releases every owned buffer.

## Pure profile contract

Add server-safe pure records/rules so selection and accessibility are deterministic without a GPU:

- Non-Light-Realm dimensions resolve `NONE` and preserve vanilla state.
- Light Realm with an unavailable/disabled enhanced boundary resolves `STATIC_WHITE`.
- Ordinary Light Realm rendering resolves `ANCIENT_WHITE` with a stable white base, four shape-distinct halo/rune layers, bounded alpha, slow camera-independent angular drift, and no texture/custom-shader dependency.
- Vanilla reduced-motion detection resolves `ANCIENT_WHITE_REDUCED`: the same large silhouette and palette, two static layers, zero angular velocity, no pulse, and lower aggregate contrast.
- All profiles forbid sun, moon, stars, dark disc, precipitation tint, gameplay callbacks, texture identifiers, custom shader resources, and server state.
- Animation derives only from extracted client world time plus partial tick. It never mutates level time and remains finite under malformed inputs.

## Rendering and compatibility invariants

- Geometry is prebuilt once per `SkyRenderer`, not allocated per frame.
- Each layer is an untextured bounded ring/radial-rune mesh using built-in position/color-compatible rendering; resource packs cannot replace it.
- Shapes remain centred on the camera and use view rotation only where the vanilla sky matrix already does, so translation cannot reveal a seam.
- The base remains `0xFFFFFFFF` at every render distance and under weather/resource reload.
- Reduced motion changes presentation only; it never changes realm mechanics, visibility rules, fog distances, or timing.
- Dedicated-server class loading succeeds because all Minecraft client/GPU references remain in the client source set/mixin client list.
- Sodium compatibility is proven with the pinned real runtime after the protected QA-006 soak; until then compile/pure contracts are not described as visual proof.

## Acceptance

1. Pure tests prove mode selection, exact white fallback, layer/palette/alpha bounds, finite time handling, reduced-motion immobility, and zero texture/custom-shader dependencies.
2. Source/reachability tests prove the narrow `SkyRenderer` extraction/draw/close boundary and reject `LevelRenderer`, Sodium, shader-pack, server, or resource-pack coupling.
3. Client compilation and dedicated-server resource/classloading gates pass before live testing.
4. After QA-006 releases the host, real client captures cover normal/reduced motion, render distances, clear/rain command state, resource reload, and fallback mode. The pinned Sodium client repeats normal/reduced/fallback coverage.
5. Manual review confirms no black void, celestial bodies, seams, clipping, excessive glare, or terrain/HUD readability loss. Evidence records exact options, hashes, runtime, and visual verdicts; no synthetic image is called renderer proof.
