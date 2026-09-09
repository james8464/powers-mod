# WRLD-008 + MOB-006 staged verification

Status: **STAGED** on the isolated `wrld-008-mob-006-herald-courts` branch. This report does not claim integration, push, or release readiness.

## Implemented contract

- Existing completed Herald Court landmarks remain the only spawn authority, and the existing per-realm twenty-minute defeat cooldown remains persistent.
- Each Herald owns three health-derived phases: Verdict, Ascendant, and Last Covenant. Phase, bounded participant scaling, and the absolute next-summon time persist with the entity.
- Combat pressure samples at most eight nearby opposed players and is capped at 96 beam damage.
- Court summons have a six-entity hard ceiling, a 200-tick cadence, absolute finite lifetimes, and loaded victory cleanup. The temporal ceiling is five even if a crowded entity query degrades phase-cap detection.
- Reversible court hazards use short-lived Slowness plus Darkness or Glowing. They mutate no terrain, expire within twenty ticks of the final scheduled pulse, respect safe zones, Sanctuary, and amethyst dampening, and cannot lock a recovery exit.
- Existing guaranteed Shadow Sword and Heavenly Partisan loot tables remain unconditional and now have dedicated acceptance coverage.

## Focused evidence

- Java 25 unit coverage passes for all phase thresholds, corrupt-state normalization, participant/damage/summon/radius caps, temporal summon ceiling, hazard expiry, recovery-exit invariance, faction/respawn rules, and both guaranteed mythic loot tables.
- The focused live GameTest passes for an actual Light Herald phase transition, finite status hazard, one production guardian summon, and loaded summon cleanup on defeat.
- Both pre-existing Light and Dark Herald committed-beam GameTests pass unchanged, preserving the established 80-tick opening cadence and casting-pose hooks.

## Deterministic branch gate

- Java 25 deterministic branch gate: **GREEN** — `/tmp/powers-wrld008-mob006-deterministic-green-retry1-20260909.log`, SHA-256 `c861ce9c28a3e177706737b81d38a26fb6f8278fe06146db6d8c96b1876e924d`.
- It completed the JUnit suite, resources, Java/source, non-item, and VFX asset audits, canonical documentation checks, 240 Python tests, and GameTest compilation in 2m01s.
- The aggregate live suite is intentionally not used as release evidence for this isolated unit: its known unrelated timing failures remain a separate protected-main investigation. This report therefore makes no integration or release-ready claim.
