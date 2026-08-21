# VFX-011 — Exact-build asset and rendered-surface audit brief

## Scope

Replace integrity-only visual claims with one exact-build audit covering every POWERS texture, animation frame, model, item definition, spawn egg, custom display transform, entity skin, HUD surface, boss bar, and custom screen. Deterministic structural and pixel checks are release gates; real Minecraft client renders are the visual authority. VFX-009's enhanced Light Realm sky remains a separate open row, so VFX-011 may prove only the current static fallback.

## Architecture

- A dedicated `VfxAssetAudit` scans the complete `assets/powers` graph, resolves item/model/texture references and variables, validates finite geometry/UV/display data, inventories animation frames and model contexts, and publishes an immutable JSON manifest with full SHA-256 identities.
- A deterministic `VfxPixelAudit` decodes every PNG, separates every animation frame, builds alpha-correct mip levels, measures transparent-edge and cross-frame contamination, and produces paged light/dark/checker sheets. Explicit reviewed exceptions live in one bounded whitelist; decode success is never a visual PASS.
- The audit owns its generated filenames. `--check` rejects manifest drift, missing or stale pages, extra pages, wrong hashes, and missing review verdicts. Historical `asset-audit.md` remains historical and must stop making unsupported contact-sheet claims.
- A client-only visual gallery renders actual baked item models in every defined `ItemDisplayContext`, all spawn eggs in GUI/ground/first-person left-right/third-person left-right plus fixed/head when defined, actual entity models and owner-skin overlays, real HUD/boss bars, and every custom screen. Captures use exact options, fixed cameras, stable time/weather, GUI scales 1–4, mipmap levels 0–4, normal and reduced-motion modes, and bounded page IDs.
- Evidence records implementation commit/JAR hash, Minecraft/Fabric/Java/GPU, resource packs/mods, options, exact commands, logs, capture hashes, review verdicts, repairs, and limitations. Generated evidence supplements but never substitutes for client renders.

## Acceptance tests

1. RED then GREEN temporary-tree tests reject degenerate/out-of-range UVs, unresolved texture variables/cycles, non-finite or malformed display transforms, animation-frame mip bleed, transparent-edge halos, missing required views, stale/extra sheet pages, and manifest drift.
2. The checked manifest inventories all current item definitions/models/textures, block models/textures, entity skins, GUI/particle/effect textures, animation frames, spawn eggs, and every authored display context without auto-PASS wording.
3. Headless sheets show every frame at every applicable mip on light/dark/checker backgrounds and are deterministically hashed; review verdicts are explicit and source-specific.
4. Exact-build client captures prove actual humanoid/entity UVs, wide/slim Shadow and Echo owner skins with overlays, all spawn-egg views, every custom display transform, representative inherited/default model families, actual HUD/boss states, and all named custom screens at GUI scales 1–4 and relevant narrow/wide sizes.
5. HUD tests apply GUI scale to logical coordinates, distinguish every counted case, cover five energy rows and 0–20 half units, and do not claim reduced-motion paths they did not execute. Synthetic sky and raw-background/skin/egg images are not cited as renderer proof.
6. Focused fixtures, client compilation, client gallery, resource/source/asset/document audits, full required GameTests, and `check` pass before VFX-011 is removed from the backlog.

## Non-goals

- No VFX-009 enhanced sky implementation, shader replacement, or resource-pack compatibility claim.
- No arbitrary artistic transform-magnitude rejection when Minecraft's schema accepts the authored transform.
- No automatic visual PASS based only on decoding, hashing, resource reload, or absence of missing-texture logs.
- No gameplay, collision, authority, save, recipe, registered-ID, or server-side effect change.
