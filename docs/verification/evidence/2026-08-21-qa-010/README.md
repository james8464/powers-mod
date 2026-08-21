# QA-010 hostile-environment evidence

Date: 2026-08-21
Platform: Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25

## Authoritative-owner coverage

| Action family | Production owner exercised | Hostile fixture |
|---|---|---|
| Travel / teleport | `AbilityActivationService.activateTeleport`, `TeleportAbility`, `TravelChunkLoader`, `TravelCohort` | deny adapter, real world border, exact void coordinate, mounted cohort, synthetic dimension |
| Destructive block work | `IceManipulationAbility`, `PowerProtection` | deny adapter and real water/lava |
| Movement / flight / size | `SizeMorphAbility`, `FlightAbility` | solid low ceiling, submerged player, mounted player, synthetic dimension |
| Projectiles / channels | `IceManipulationAbility` production ray/channel and `InfernoAbility` | protected beam corridor and submerged Inferno field |
| Fields / summons | `AlignedArtifactAbility`, `ArtifactGuardianSummons`, `InfernoAbility` | claim-denied Hollowed summon and bounded submerged field |
| Observation / mind | `DreamwalkingAbility`, `AstralProjectionAbility`, `BodyProxyManager` | claim denial and fatal vulnerable body over a void floor |
| Realm portals | `NetherPortalBlock.getPortalDestination`, `RealmPortalMixin`, `RealmPortalRules` | physically placed ordinary, Dark Realm, and Light Realm Nether portals |
| Delayed work | `PowersMod.scheduleDelayed`, `TravelChunkLoader` | exact asynchronous void travel and foreign-dimension callback cleanup |

The table claims shared authoritative boundaries, not individual catalogue-action coverage.

## Literal result matrix

| Environment | Production entrypoint | Expected and observed authoritative result | Payment / mutation | Cleanup | Exact GameTest ID |
|---|---|---|---|---|---|
| External claim deny | Ice, Time Shift, Dreamwalking, Call Hollowed, API presence | Every action failed before authority changed | 0 energy, 0 cooldown, 0 block/entity/presence mutations | adapter unregistered; owned host/player removed; owner-scoped summon revoke | `powers-gametest:hostile_environment_game_tests_claim_denial_precedes_destruction_travel_observation_summons_and_external_api` |
| Solid two-block ceiling | Size Shift activation and active 1x→2x selection | Enlargement rejected; 1x option/scale and active ownership remained consistent | 0 unsafe scale commits; no clipping | toggle explicitly stopped; override cleared | `powers-gametest:hostile_environment_game_tests_low_ceiling_rejects_enlargement_without_clipping_or_stale_scale` |
| Real bounded world border | Time Shift exact-coordinate input | rejected without nearest-safe substitution | 0 energy; 0 coordinate rewrites; 0 ticket delta | prior center and size restored | `powers-gametest:hostile_environment_game_tests_world_border_rejects_exact_coordinates_atomically_without_rewriting` |
| Air/void floor | asynchronous Time Shift plus Astral Projection | exact entered coordinate committed; fatal vulnerable-body damage ended the mind session | one accepted travel; no safety correction | astral/body state and ticket delta zero; gravity/override/player cleaned | `powers-gametest:hostile_environment_game_tests_void_coordinate_remains_exact_and_fatal_body_damage_clears_mind_and_travel_state` |
| Real water and lava | Ice ray, Flight tick, Inferno field | water→ice once; lava→obsidian once; submerged velocity finite; target damaged | one mutation per thermal block; bounded entity damage | Flight off; Inferno/override cleared | `powers-gametest:hostile_environment_game_tests_water_and_lava_use_real_thermal_blocks_and_submerged_movement_stays_finite` |
| Horse/player/zombie/chicken graph | `TravelCohort.capture/move` plus Flight | exactly three companions retained; principal cleanly dismounted; horse→zombie→chicken graph preserved | no duplicates or cross-level references | toggle stopped; every owned entity removed | `powers-gametest:hostile_environment_game_tests_mounted_nested_passengers_travel_without_duplication_or_cross_level_references` |
| Physical Nether portals | vanilla portal destination plus confinement mixin | ordinary portal legal; Dark/Light departures denied in place | 0 energy and 0 ticket delta on denial | player returned to Overworld and removed | `powers-gametest:hostile_environment_game_tests_nether_portal_is_legal_in_ordinary_world_and_denied_without_state_change_in_mindscape` |
| `qa010_hostile:synthetic` | vanilla transition, policy resolve, Flight/FX, scheduled callback | foreign key present in live registry; exact travel/action/callback succeeded | one action; one callback | toggle, delayed owner state, override, and player removed | `powers-gametest:hostile_environment_game_tests_synthetic_foreign_dimension_runs_policy_fx_travel_and_stable_delayed_cleanup` |

## RED / GREEN record

- RED: 2x activation under a bedrock ceiling clipped; focused GameTest failed. GREEN: collision-aware scale commit rejects and refreshes the authoritative hitbox.
- RED: active 1x→2x selection returned success and persisted an unsafe option. GREEN: selection restores its prior option/scale transactionally and failed active ticks normalize to 1x.
- RED: a real deny adapter allowed Ice to charge before its later per-block checks. GREEN: the ray preflights all prospective water, lava, and snow mutations before damage, FX, payment, or cooldown commit.
- RED by owner inspection/compound claim fixture: player-owned artifact guardians did not consult external ritual protection. GREEN: `ArtifactGuardianSummons` rejects the summon before entity creation.
- Test-resource RED: the first preset override changed vanilla flat terrain. GREEN: the final override copies the exact Minecraft 26.2 Overworld/Nether/End entries and only adds Dark Realm, Light Realm, and `qa010_hostile:synthetic`.
- Full-suite isolation REDs: global claim/border state overlapped QA-009, real realm fixtures overlapped Augury, and PERF-012 sampled neighbour fixtures. GREEN: setup offsets and 128-block PERF-012 padding isolate those owners.
- Full-suite fairness RED: a fixed first-64 invasion scan could starve later player anchors. GREEN: the production manager consumes a pure rotating window whose unit proof covers all 65 and 100 indexes in two pulses, never exceeds 64 anchors per pulse, and resets its lifecycle cursor explicitly.

## Verification

- Focused hostile batch: 8/8 required GameTests passed.
- Full live batch: 124/124 required GameTests passed.
- JVM unit/integration batch: 1,546 tests passed across 367 classes; Python validator batch: 44 tests passed.
- Server shutdown enumerated and saved `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`, `powers:dark_realm`, `powers:light_realm`, and `qa010_hostile:synthetic`, proving live registry presence rather than JSON-only inspection.

No direct-coordinate safety search, consent gate, new gameplay system, main-resource dimension, multi-client run, or high-load profile was added. The isolated QA-006 soak worktree and its runtime were not touched.
