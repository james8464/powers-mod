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
| Fields / summons | `AlignedArtifactAbility`, `ShadowPowerExecutor`, `ArtifactGuardianSummons`, `InfernoAbility` | claim-denied player and owner-directed Shadow summons; bounded submerged field |
| Observation / mind | `DreamwalkingAbility`, `AstralProjectionAbility`, `BodyProxyManager` | claim denial and fatal vulnerable body over a void floor |
| Realm portals | `NetherPortalBlock.getPortalDestination`, `RealmPortalMixin`, `RealmPortalRules` | physically placed ordinary, Dark Realm, and Light Realm Nether portals |
| Delayed work | `PowersMod.scheduleDelayed`, `TravelChunkLoader` | exact asynchronous void travel/death cleanup and a foreign-dimension callback across resource reload |

The table claims shared authoritative boundaries, not individual catalogue-action coverage.

## Literal result matrix

| Environment | Production entrypoint | Expected and observed authoritative result | Payment / mutation | Cleanup | Exact GameTest ID |
|---|---|---|---|---|---|
| External claim deny | Ice, Time Shift, Dreamwalking, player/Shadow Call Hollowed, API presence | Every action failed before authority changed; the authoritative Shadow owner was consulted | 0 player/Shadow energy, 0 cooldown/cast metric, 0 block/entity/presence mutations | adapter unregistered; owned host/player removed; owner-scoped summon revoke | `powers-gametest:hostile_environment_game_tests_claim_denial_precedes_destruction_travel_observation_summons_and_external_api` |
| Solid two-block ceiling | Size Shift activation and active 1x→2x selection | Enlargement rejected; 1x option/scale and active ownership remained consistent | 0 unsafe scale commits; no clipping | toggle explicitly stopped; override cleared | `powers-gametest:hostile_environment_game_tests_low_ceiling_rejects_enlargement_without_clipping_or_stale_scale` |
| Real bounded world border | Time Shift exact-coordinate input | rejected without nearest-safe substitution | 0 energy; 0 coordinate rewrites; 0 ticket delta | prior center and size restored | `powers-gametest:hostile_environment_game_tests_world_border_rejects_exact_coordinates_atomically_without_rewriting` |
| Air/void floor | asynchronous Time Shift, Flight, Astral Projection, vanilla `checkBelowWorld` | exact entered coordinate committed; lethal vanilla below-world dispatch ended the projected avatar lifecycle | one accepted travel; no safety correction | astral/body/toggle/owned callback/ready lease cleared | `powers-gametest:hostile_environment_game_tests_void_coordinate_remains_exact_and_fatal_body_damage_clears_mind_and_travel_state` |
| Immutable Ice plan | Ice entity ray plus real water with air above | original-state mutation plan authorized once, then entity damage and water→ice committed without a post-mutation snow query | one deduplicated fluid mutation; one entity hit | adapter/player/target removed | `powers-gametest:hostile_environment_game_tests_ice_authorizes_and_applies_one_immutable_mutation_plan_before_entity_damage` |
| Real water and lava | Ice ray, Flight tick, Inferno field | distinct water→ice and lava→obsidian states remained stable after ten ticks; submerged velocity finite; one explicit Inferno tick damaged once | two stable thermal results; no item-drop delta; no damage after Inferno clear | Flight off; Inferno/override/players/target cleared | `powers-gametest:hostile_environment_game_tests_water_and_lava_use_real_thermal_blocks_and_submerged_movement_stays_finite` |
| Horse/player/zombie/chicken graph | `AbilityActivationService.activateTeleport`, `TeleportAbility`, `TravelChunkLoader`, `TravelCohort`, Flight | production delayed travel moved exactly three companions once and deterministically detached every vehicle edge | no duplicates/cross-level references; exact relationship cardinality zero after travel | ready lease/toggle/storm/tasks/entities cleared in failure-safe cleanup | `powers-gametest:hostile_environment_game_tests_mounted_nested_passengers_travel_without_duplication_or_cross_level_references` |
| Physical Nether portals | real portal block, returned ordinary transition, confinement mixin, `BodyProxyManager` | ordinary transition applied; unqualified Dark/Light portal departures denied without changing live mind/body sessions; qualified explicit body return completed before the next realm | 0 energy and 0 ticket delta on denial | both proxy bodies removed; player returned to Overworld and removed | `powers-gametest:hostile_environment_game_tests_nether_portal_is_legal_in_ordinary_world_and_denied_without_state_change_in_mindscape` |
| `qa010_hostile:synthetic` | POWERS Time Shift in/out, policy resolve, Flight FX, owned callback, server resource reload | foreign key present; exact round trip, observable FX delivery, callback against stable level/player identities, and post-reload policy rebind succeeded | two accepted travel commits; one action; one callback | zero owner tickets/tasks; toggle/override/body/player cleared | `powers-gametest:hostile_environment_game_tests_synthetic_foreign_dimension_runs_policy_fx_travel_and_stable_delayed_cleanup` |

## RED / GREEN record

- RED: 2x activation under a bedrock ceiling clipped; focused GameTest failed. GREEN: collision-aware scale commit rejects and refreshes the authoritative hitbox.
- RED: active 1x→2x selection returned success and persisted an unsafe option. GREEN: selection restores its prior option/scale transactionally and failed active ticks normalize to 1x.
- RED: a fluid sampled twice was first mutated, then re-read as solid; denial of its newly inferred snow position happened only after entity damage. GREEN: one immutable, deduplicated original-state plan is fully authorized before damage and applied without re-reading.
- RED: an owner-directed real Shadow `call_hollowed` bypassed the deny adapter because the non-player body discarded its authoritative `ServerPlayer` owner. GREEN: `ShadowPowerExecutor` passes the owner actor to `ArtifactGuardianSummons`; denial changes no Shadow energy, cast metric, or guardian index.
- Test-resource RED: the first preset override changed vanilla flat terrain. GREEN: the final override copies the exact Minecraft 26.2 Overworld/Nether/End entries and only adds Dark Realm, Light Realm, and `qa010_hostile:synthetic`.
- Full-suite isolation REDs: global claim/border state overlapped QA-009, real realm fixtures overlapped Augury, and PERF-012 sampled neighbour fixtures. GREEN: setup offsets and 128-block PERF-012 padding isolate those owners.
- Near-cap fairness RED: consuming one anchor advanced by 64 and skipped 63 unvisited anchors. GREEN: the O(64) integer cursor advances by actual visited count; unit coverage proves 65/100 round robin, early-break resumption, bounded representative churn, and reset without live clients.
- Integrated mount RED: nested relations varied with entity iteration/teleport order. GREEN: the production cohort prevalidates a bounded travelling set, then deterministically detaches its exact passenger graph before moving it.
- Vanilla void RED: fatal projected-avatar cleanup retained Flight ownership, owned callbacks, and a ready lease. GREEN: fatal Astral cleanup cancels pending/ready travel work and delayed tasks and clears toggle ownership before proxy disposal/death continuation.

## Verification

- Focused hostile batch: 9/9 required GameTests passed.
- Final focused hostile batch: 9/9 required GameTests passed in 3.35s.
- Final aggregate live batch: 125/125 required GameTests passed in 51.44s.
- Final aggregate `check`: 1,548 JVM tests across 367 classes and 44 Python tests passed; resource, item-doc, magic-doc, rank-doc, Java-source-audit, and non-item-asset validators passed.
- Server shutdown enumerated and saved `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`, `powers:dark_realm`, `powers:light_realm`, and `qa010_hostile:synthetic`, proving live registry presence rather than JSON-only inspection.

No direct-coordinate safety search, consent gate, new gameplay system, main-resource dimension, multi-client run, or high-load profile was added. The isolated QA-006 soak worktree and its runtime were not touched.
