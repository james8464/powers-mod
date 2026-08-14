# NET-007 scoped power-policy evidence

## Exact implementation

- Commit: `3b5d739` (`feat: resolve world and dimension power policies`)
- Date: 2026-08-14
- Platform: macOS, Java 25, Minecraft/Fabric 26.2 development runtime
- Scope: configuration schema, policy resolution, protection consumers, diagnostics, and observation targets

## Accepted behaviour

- Every overridable field resolves independently through global, exact world name, then namespaced dimension; omitted values inherit.
- Each effective value retains its origin for `/powers diagnose`.
- Maps are deterministic and bounded to 128 valid entries per scope. Exact bounded Unicode world names are supported; dimension IDs are canonical namespaced keys.
- Safe-zone and external protection-adapter denials remain absolute.
- Named-mob locator, remote viewing, and Blood Reading use the same observation policy as players; player and owned-participant consent still applies.
- Older compiled adapters that fail on the new observation action are denied safely rather than aborting server work.
- Schema v1–v3 data migrates to schema v4 with empty overrides and no effective-policy change.

## Test-first and review evidence

Failing tests were observed before implementation for the missing resolver contract, JSON precedence, external-adapter precedence, runtime cache, bounded dimension IDs, exact Unicode/whitespace world identity, old exhaustive-switch compatibility, and the production GameTest seam. Independent review identified the mob-observation bypass, adapter linkage failure, runtime-wiring proof gap, and world-key mismatch; each was reproduced and fixed before final re-review.

## Verification

| Check | Result |
| --- | --- |
| JVM suite | 1,474 passed; 0 failed |
| Fabric server GameTests | 103 passed; 0 failed |
| Focused live policy integration | Parsed runtime config, resolver cache, safe zone, registered deny adapter, named mob, and diagnose source all passed |
| Resource validation | Passed |
| Magic/item/rank documentation verification | Passed |
| Python validation suite | 27 passed |
| Java source audit | Regenerated and passed against the implementation tree |
| Independent review | No remaining actionable P1/P2 findings |

Commands and concise results are recorded in [verification-summary.txt](verification-summary.txt).

## Limits

Overrides intentionally cover only existing world-sensitive boolean policy. Operator permissions, work budgets, progression, and recipe availability are not per-world settings. Protection adapters remain one unanimous fail-closed chain; no override can weaken their denial.
