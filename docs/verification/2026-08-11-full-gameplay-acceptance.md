# Full gameplay acceptance — 2026-08-11/12

## Tested build

- Campaign base: `acb9909a67c9b757af85c0a4654de7fbd5f2f496`
- Tested code and evidence commit: `335d0541a65711d11b29268af6c70174c60f621b`
- Mod: POWERS 1.0.2; Minecraft Java 26.2; Fabric Loader 0.19.3;
  Fabric API 0.156.0+26.2; Java 25.
- All worlds were isolated under Gradle `build/run` or `build/restart-soak`.
  Ordinary saves were not opened or modified.

## What “every interaction” can prove

The finite authoritative register contains **429 identities**: 23 innate powers,
12 spells, 11 crystal actions, 56 artifact actions, 8 custom entities, 14
cross-cutting systems, 262 registered items, 11 custom screens, and 32 commands.
The collision kernel contains **2,080/2,080** unordered action pairs and the
lifecycle table contains **672/672** rows. Those sets are exhaustively checked.

World seeds, arbitrary third-party mod combinations, hardware/driver behaviour,
network schedules, and player input sequences are infinite. They cannot honestly
be called exhaustively tested. The campaign therefore combines exhaustive pure
matrices with live Minecraft tests, a rendered integrated client, isolated
dedicated-server restarts, hostile-input fuzzing, and 10/50/100-player synthetic
loads. The generated [manual checklist](manual-acceptance-checklist.md) deliberately
does not mislabel registry or screenshot contracts as human gameplay approval.

## Final evidence

| Lane | Evidence | Result |
| --- | --- | --- |
| Full verification | `clean check verifyScreenshots saveMigrationCorpus syntheticSoak --rerun-tasks` | PASS in 1m03s |
| JVM suite | 1,366 tests | 1,366 passed; 0 failed/skipped |
| Live server GameTests | 69 tests | 69 passed; 21.33s final clean run |
| Save migration corpus | 20 cases | 20 passed |
| Screenshot/layout contracts | 17 cases plus deterministic visual goldens | 17 passed |
| Synthetic multiplayer | 10/50/100-player scenarios and cleanup case | 2 suites passed within budgets |
| Python generators/validators | 6 fixture suites | 6 passed |
| Resources/docs/audits | JSON, models, textures, sounds, translations, recipes, item/rank/magic docs, Java and non-item manifests | PASS |
| Magic collision matrix | all 64 canonical actions | 2,080/2,080 symmetric, non-empty, budgeted resolutions |
| Lifecycle matrix | death, drop, logout, source loss, mind/body and toggle combinations | 672/672 decisions |
| Hostile input | every serverbound codec plus 10,000 Shadow Unicode/control samples | 6,656 malformed frames and all parser samples rejected/bounded without crash |
| Real rendered client | `runClientGameTest` | PASS; 19 screenshots, integrated server, resource/model bake and clean shutdown |
| Mindscape crystals | actual Dark and Light Crystal activation, realm dimension change, body session, rendered realm, administrative body recovery | PASS for both crystals |
| Dedicated server | isolated `runServer` boot | PASS; all six dimensions loaded/saved; clean stop |
| Restart soak | two clean boots of the same isolated world | PASS; zero forced chunks, proxies, travel loads, or celestial events after each cycle |

Rendered evidence is preserved at
[`docs/verification/evidence/2026-08-11-full-gameplay-acceptance`](evidence/2026-08-11-full-gameplay-acceptance/). It includes the actual Light
and Dark realms; empty/half/full-Darkness energy HUD states; sevenfold Rainbow
selector; Light/Dark rank mazes; Shadow wheel/library at two sizes; teleport,
locator, Grimoire, power option, reservoir and Crucible screens. The ten energy
symbols are directly above and horizontally aligned with the hunger symbols.
The Light Realm screenshot has the authored white sky.

## Live gameplay exercised

- Actual authoritative power pipelines cover suppression, transaction, payment,
  cooldown, presence, cleanup and representative immediate effects across damage,
  projectile, beam, terrain, toggle,
  movement, forcefield, possession, astral/body, summon, control, healing,
  information and travel families. The final added probe covers Size Shift,
  Starfall, Thunderclap, Speed Burst, Telekinesis, Super Speed, Breezy Bash,
  Invisibility, Gravity Displacement, Ice Manipulation and Double Health.
- Lightning through the Shadow Sword creates a visible lightning entity.
  Fireball impact, Celestial Ruin versus the First Vessel, vulnerable body
  proxies, named player-like actors, vessel possession, realm confinement,
  distant chunk travel, frozen projectiles and catastrophic-spell persistence
  have live GameTests.
- Every registered power, spell, crystal and artifact route resolves on a live
  server. All 253 innate rank profiles (23 powers × ranks 0–10) are validated;
  artifact source scaling, rank-10 Darkness cooldown removal and selection
  migration are deterministic tests.
- All eight custom entity identities, including Shadow, are in the acceptance
  catalogue. Existing live tests exercise entity construction, targeting,
  player-like naming, combat, drops/factions or lifecycle as applicable.
- The integrated client parses and requires positive results from each sampled
  testing/operator command, then checks assigned-slot persistence and testing-limit
  cleanup. It creates and
  clears the test arena/actor, previews/cancels Heavenfall, exercises diagnostics,
  consent, slots, path listing, Shadow learning reset and configuration reload.
- Status-effect construction is centralized. Production-created power effects
  use `PowerStatusEffects.hidden`, which sets `showParticles=false`; custom dust,
  rune, beam and semantic FX remain independent of vanilla potion particles.

## Defects reproduced and closed

1. **Dedicated-server false green.** `runServer` could stop at the EULA gate.
   A failing launcher contract preceded isolated EULA/properties seeding; a real
   dedicated boot and two restart cycles now prove the path.
2. **Rainbow was not sevenfold.** The catalogue exposed six modes. A failing
   catalogue test preceded the Middleworld seventh mode and responsive layout.
3. **Combat wheel became dense at large GUI scales.** Actual screenshots exposed
   label/status crowding. Responsive elliptical geometry, compact label policy,
   hover naming and tests now keep the eight slots usable.
4. **Locator leaked translation keys.** The rendered client exposed missing
   strings. A source-to-language contract and localized, shorter prompts close it.
5. **Arcane Crucible labels lacked contrast.** Rendered inspection led to explicit
   high-contrast labels.
6. **Acceptance register omitted Shadow.** A failing catalogue test preceded the
   eighth entity entry.
7. **Evidence register overstated proof.** Registry/item/screenshot existence had
   been called gameplay success. Focused RED/GREEN tests now distinguish contract,
   automated behaviour and genuinely pending manual approval.
8. **Client evidence captured loading screens.** The harness now waits for the
   destination screen to close and 20 rendered ticks before HUD/realm screenshots.
9. **Live action probe used the wrong toggle entry point and shared movement
   state.** The failure was reproduced; each action now resets position/motion and
   toggles through the authored activation path. All 69 GameTests pass afterward.

## Observed non-mod/environment warnings

- Fabric's test client reports an illegal default anisotropic-filter value of 0,
  offline Realms/profile authentication, and Apple OpenGL shader-driver warnings.
  Resource reload, model bake, integrated server, screenshots and shutdown still
  completed. These did not originate from POWERS gameplay code.
- Dense parallel GameTest batches briefly reported server-behind warnings. The
  deterministic 10/50/100-player budget suite and clean-state diagnostics pass;
  production server sizing and third-party modpacks still require deployment
  profiling on the target host.

## Honest residual manual risk

Finite coverage that remains contract/rule-based rather than a distinct live
end-to-end cast includes: deliberately freezing the GameTest server with Time
Freeze; selecting and executing every one of the 56 artifact routes separately;
walking every one of the 68 authored structure pieces; opening naturally
generated copies of every injected loot table; and completing all 20 quest rows
with real multiplayer telemetry. Pure Light and Darkness spread now have separate
live tests; their affinity auras and the Light/Dark clash remain exhaustively
rule-tested, while powered-amethyst containment is exercised physically. The restart
soak proves clean persistence and leak-free recovery of an empty saved world; it
does not deliberately persist an active Time Freeze or irreversible Heavenfall
through the stop boundary. These are evidence gaps, not observed failures, and
remain visible rather than being promoted to a false manual pass.

No automated campaign can certify subjective sound volume, motion comfort,
particle beauty, every possible third-party boss/claim mod, every GPU/GUI-scale,
or every arbitrary world seed. The generated checklist retains those human
judgement rows. In particular, a server owner should still perform a final
headphones-safe audio pass, multiplayer PvP play session, and target-modpack boss
fight before a public release. No known deterministic crash, failed finite
interaction row, stale resource, leaked runtime ticket, or reproducible gameplay
failure remains in the tested configuration.
