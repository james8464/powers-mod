# Live multiplayer interaction campaign — 2026-08-12

## Scope

This campaign extends the finite interaction proof with real Minecraft server objects and distinct
embedded player connections. It does not claim that infinite world seeds, third-party mods, network
schedules, hardware, or arbitrary human input can be exhaustively enumerated.

## Live scenario inventory

| Campaign | Live scenarios | Production path asserted |
| --- | ---: | --- |
| Ordered magic pair adapter | 4,096 | Two distinct connected players, `ServerMagicCasts.prepare`, reaction ownership, caller-order blocking, commit, and cleanup for every 64×64 orientation. |
| Physical collision families | 1,069 | Real projectile/entity bodies or fixed beam/field/force-block handles, spatial movement, single collision resolution, repeat-window suppression, and cleanup. |
| Shadow complete arsenal | 26 | Player chat → bounded parser → task controller → executor for all 23 innate and three sword-exclusive actions; energy, Time Freeze, toggle stop, effects, entities, terrain, and cleanup. |
| Concurrent Shadow owners | 3 owners | Independent bodies, identities, memories, stances, combat ranges, targets, amethyst suppression, Sanctuary counterplay, hide/reveal, and one-body death isolation. |
| Busy global Shadow dialogue | 3 speakers | Speaker-labelled public history, two simultaneous owner-local focuses, unprefixed natural follow-ups, non-consumed public chat, bystander silence, and persistent-turn isolation. |
| Group realm return regression | Player + mob + Shadow | Outbound crystal cohort, vulnerable body, tracked mob, persistent Shadow rebind, return cohort, and eventual entity-manager registration at the recorded origin. |

The reviewed unordered outcome totals remain 884 coexist, 533 resonate, 261 dampen, 144 contest,
139 cancel, 94 consume, 16 destabilize, 6 amplify, and 3 transform. Both caller orientations are now
executed live, for 4,096 production-adapter invocations in total.

## Defects found by the campaign

1. Shadow's three exclusive sword invocations were executable internally but absent from spoken-name
   resolution. They now use the same bounded chat route as innate powers.
2. Stopping Shadow's Double Health left Absorption and Resistance behind. Toggle cleanup now removes
   both effects.
3. The group-mob return assertion used one fixed observation tick even though cross-dimensional entity
   registration is asynchronous. The live test now asserts the same outcome until its bounded deadline,
   retaining exact stranded-entity diagnostics on failure.
4. Ordinary public chat was ignored by Shadow. A bounded global context plus owner-local dialogue focus
   now supports human-like follow-ups without allowing unprefixed gameplay commands.
5. Two exhaustive tests declared padding outside Minecraft 26.2's synchronized `0..128` range. The
   dedicated client lane exposed the invalid registry payload; both declarations now use 128.
6. The realm-cohort fixture could be collision-pushed at the shared landing site by other concurrent
   GameTests. Its test mob is now non-physical, preserving the production travel path while removing
   unrelated fixture interference; the full 76-test batch then passed.
7. The Time Shift fixture could be collision-ejected from its deliberately solid exact destination
   after a correct teleport. Its actor is now non-physical so the assertion measures raw destination
   preservation rather than later vanilla collision resolution.

## Final verification

| Gate | Result |
| --- | --- |
| JVM/unit and contract suite | 1,371 passed, 0 failed. |
| Dedicated live GameTests | 76 passed, 0 failed in 36.53 seconds; the focused Time Shift regression also passed three consecutive fresh runs. |
| Client GameTests | Passed after a real Minecraft 26.2 client, OpenGL renderer, resource reload, integrated server, GUI, commands, and clean shutdown. |
| Interaction coverage inside live suite | 4,096 ordered semantic casts, 1,069 supported physical collision pairs, 26 Shadow powers, three simultaneous Shadow owners, and three-speaker overlapping chat. |
| Resources and generated docs | `validatePowerResources`, `verifyMagicDocs`, `verifyItemDocs`, and `verifyRankDocs` passed. |
| Visual gates | `verifyScreenshots`, `verifyVisualGoldens`, and `auditNonItemAssets` passed. |
| Performance/validator gates | `syntheticSoak` and all seven Python validation tests passed. |
| Final repository gate | Clean `./test.sh check` passed in 1 minute 11 seconds, including source/asset audits, all 76 live tests, all JVM/Python tests, resources, generated docs, and access-widener validation. |

The client log contains platform shader/anisotropic warnings from the Apple OpenGL compatibility
layer and an offline Realms authentication warning from the Fabric test identity. Neither failed a
test, resource reload, world load, or shutdown.

## Execution notes

The high-volume GameTests intentionally advance simulated server ticks faster than wall-clock 20 TPS;
Minecraft may emit a server-behind warning during the matrix. Per-test work is still capped at two
semantic pairs and one physical pair per simulated tick. Production tick budgets remain separately
covered by the 10/50/100-player synthetic soak.

Rendered multi-client GUI input is not substituted for these authoritative server assertions. The
fixtures use distinct random UUIDs, embedded connections, the live player list, real levels/entities,
and actual server lifecycle code. Client rendering and screenshot coverage remain a separate lane.
