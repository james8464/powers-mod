# Bounded P2 hardening design

Date: 2026-08-11  
Status: approved autonomously under the owner's instruction to prefer small, reliable improvements and avoid long feature rewrites

## Objective

Finish the low-risk part of the P2 backlog without weakening the release claims. The work must improve correctness, operability, performance evidence, or an already-shipped mechanic; it must not introduce a new progression system, large content surface, protocol redesign, or save-risky migration.

## P0/P1 boundary

The code and automated foundations for every P0/P1 entry have been audited. Eight entries remain open because their acceptance conditions require evidence that cannot honestly be synthesized by unit tests: real connected-player profiling, packet/allocation captures, multiplayer quest telemetry, first-person captures, a signed manual checklist, and the full restart soak. They remain in the backlog and acceptance ledger until that evidence exists.

## Approaches considered

1. Implement every P2 item shallowly. Rejected because incomplete stubs would enlarge the maintenance surface and contradict the acceptance conditions.
2. Deepen a few large features such as waypoint travel, flight physics, realm settlements, or bosses. Rejected for this pass because each needs extensive balance and playtesting.
3. Complete bounded hardening slices whose behavior can be proven deterministically. Selected because it produces release-quality gains without destabilising existing gameplay.

## Selected scope

### Gameplay correctness

- **COR-017:** centralise player-facing cooldown rounding and formatting so HUD, artifact catalogue, grimoires, Shadow explanations, and diagnostics agree to the tick.
- **PWR-009:** add a deliberate crouch-use release for Cinderheart fireballs while preserving the existing bounded charge, catch, deflection, ownership, and wake rules.
- **PWR-010:** extend the existing finite lightning chain to recognise water, copper/lightning-rod contact, and conductive armour through tags. Medium-specific damage/visual metadata must remain bounded by the existing node cap and must not summon harmful vanilla lightning.
- **PWR-018:** show Time Freeze drain and safe-duration forecast before activation, warn at high MSPT, and never silently substitute a low-TPS refusal for operator policy.

### Artifacts and acquisition

- **ART-010 / NET-005:** retain absolute safe-zone/policy denial, make Empyrean consent override conspicuous to the target, and emit structured operator audit events for overrides plus existing recovery, forced-travel, testing, and catastrophic ritual controls.
- **ART-017:** add one bounded Archivist/realm loot source for Wisdom Fruit and regenerate acquisition documentation. No deliberately recipe-less crystal/item receives a recipe.

### Operations and configuration

- **NET-006:** add `/powers diagnose export`, writing a bounded, schema-versioned, redacted JSON snapshot beneath the world directory. It contains aggregate counters and budget state, never chat, names, UUIDs, precise player coordinates, tokens, or remote content.
- **NET-008:** report every config clamp/default substitution on reload and expose the bounded summary in diagnostics. Runtime sanitisation remains authoritative and save-safe.

### Verification and maintenance

- **QA-011:** give the synthetic soak suite a separately attributed CI task and retain its tick, scan, packet, particle, and forced-chunk budgets.
- **QA-013:** validate README registry totals and local links against authoritative sources.
- **QA-014:** run generated documentation in check mode in CI and fail when the checkout would become dirty.
- **QA-018:** extend resource validation with deterministic local-reference and cycle checks for recipes, loot tables, and tags.

## Explicitly deferred

Waypoint databases, new flight physics, terrain-boring beams, new mob/boss variants, realm settlements, ritual networks, archaeology chains, new mastery systems, client comfort settings, catalogue virtualisation, replay infrastructure, and broad save-schema migrations remain backlog work. Partial implementations would not meet their acceptance conditions.

## Architecture

- Pure rule/format classes own cooldown presentation, Time Freeze forecasts, lightning conductance classification, config validation deltas, audit event sanitisation, and diagnostic export schema.
- Runtime adapters perform Minecraft lookups and side effects only after those rules decide the outcome.
- Tags define conductive blocks and armour for compatibility; unknown third-party content remains safely non-conductive unless opted in.
- Audit storage is a bounded in-memory aggregate plus the normal server log. Diagnostic export includes aggregate counts only.
- Generated documentation remains reproducible: generators compare expected bytes in check mode rather than treating a hash manifest as semantic proof.

## Failure and safety behaviour

- Missing/invalid config values are replaced with documented defaults and reported, not fatal.
- Diagnostic export creates its own narrow directory, writes atomically, and reports failure without exposing an absolute host path to ordinary players.
- Fireball release and lightning chaining revalidate owner, level, policy, and loaded-state constraints server-side.
- Loot additions use existing generated/validated tables and cannot make mythic artifacts or crystals craftable.
- All new visual work uses semantic bounded effects and respects reduced-motion/particle budgets.

## Verification

Each production behavior begins with a focused failing test, followed by focused and affected-suite passes. Completion requires clean full build, all unit/resource checks, dedicated-server boot, generated-doc check, clean Git tree, one branch, and a successful push of `main`. P0/P1 evidence-only entries are reported as open rather than claimed complete.
