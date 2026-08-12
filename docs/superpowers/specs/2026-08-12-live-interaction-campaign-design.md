# Live Interaction Campaign Design

## Goal

Exercise as much of POWERS' multiplayer gameplay interaction surface as practical inside real Minecraft server and client runtimes, with evidence that records observable effects rather than registry presence alone.

## Chosen approach

Use a hybrid campaign:

1. Fabric GameTests create distinct `ServerPlayer` identities backed by embedded network connections. This is the authoritative layer for combat, energy, cooldown, lifecycle, targeting, Shadow, and world mutation.
2. A production-adapter matrix drives all 2,080 unordered magic-action pairs through `ServerMagicCasts`, asserting reaction routing, blocking/commit behaviour, outcome totals, and cleanup.
3. A physical matrix maps real delivery types to projectile, beam, field, force-block, and body handles. Every supported pair reaches `PhysicalMagicPresences`, resolves once inside its collision window, and releases all state.
4. A Shadow campaign starts from owner chat, not the parser in isolation. Every innate and three sword-unique actions must be named, accepted, paid for, executed against a live player or mob where relevant, and cleaned up. Separate owners prove body, stance, visibility, memory, and death isolation.
5. Existing client GameTests, dedicated boot, synthetic 10/50/100-player soak, resource checks, and the complete JUnit suite remain final gates.

## Rejected alternatives

- Pure unit matrices are fast but do not prove Minecraft entity, world, networking, or lifecycle integration.
- Several full graphical clients are useful for subjective visual/input review but are slow, difficult to synchronize, and weaker for authoritative state assertions. One real client suite plus embedded connected players gives stronger repeatable evidence.

## Evidence boundary

Passing automation proves the code paths and assertions it executes. It does not claim subjective visual quality, compatibility with every third-party mod, internet-backed account behaviour, or a human's signed playthrough. Any discovered gameplay defect receives a focused regression reproduction before its production fix.

## Safety and performance

- Matrix cases use isolated test coordinates and bounded participant counts.
- Each case owns and removes its presence/entity state.
- Global systems such as Time Freeze are stopped immediately after their case.
- Test effects use the production packet/particle budgets; no test disables workload protection.
- Catastrophic rituals and uncontrolled force spread are not multiplied across the exhaustive matrix.
