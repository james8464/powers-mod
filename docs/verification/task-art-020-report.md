# ART-020 top-level mythic ownership verification

Date: 2026-09-09

Status: **STAGED — independent review pending; not integrated or pushed.**

## Scope

- Resolves mythic ownership only from directly exposed player-inventory slots.
- Supports genuine mod-added top-level slots, including all 27 pinned Inventory Extended entries.
- Never traverses shulkers, bundles, vanilla container components, or unknown mod container components.
- Keeps hand-held activation, alignment authorization, sustain checks, passive effects, and ownership reconciliation on the same fail-closed predicate.
- Documents the supported and deliberately rejected container boundaries.

## Test-first evidence

- Initial focused RED: `/tmp/powers-art020-red-20260909.log`
  - SHA-256: `abc028b81070d69ca564ece5e0217ccc0370082ee0ff36f33d3c019149171909`
  - Compilation failed because the new explicit ownership policy did not yet exist.
- Final focused GREEN: `/tmp/powers-art020-focused-green-final-20260909.log`
  - SHA-256: `a97badec2d0557eba7176041715fe3449c2c5112fd766eb4925b55fe913e46d3`
  - Direct-slot, opaque-carrier, invalid-input, and GameTest compilation checks passed.
- Exact registered nested-container proof: `/tmp/powers-art020-exact-live-20260909.log`
  - SHA-256: `e526fd94c0476bc8e0933a08f489347894cd81734e13b89b917169e68b1bfb0e`
  - Exactly 1/1 required GameTest passed.
  - A Shadow Sword inside a shulker granted no ownership despite correct Darkness alignment; the same registered sword directly in inventory did.
- Exact pinned compatibility proof: `/tmp/powers-art020-compatibility-live-20260909.log`
  - SHA-256: `e5739ed8ea441eea0a2c73109a1dfa29746e35e87d23e71bfc2e79f4203e0c17`
  - Exactly 1/1 required Inventory Extended GameTest passed under the hash-verified complete server profile.
  - Slot 36 authorized the artifact; removal revoked ownership and cleaned its routed flight state.
- One focused diagnostic failure is retained as a failure: raw Minecraft item-stack fixtures require registry bootstrap. The slot-walk kernel moved to a registry-free unit seam, while the unchanged semantic nested-container assertion runs against registered items in GameTest.

## Deterministic branch gate

Java 25 and the disposable `/tmp` Python verification environment were used.

- Log: `/tmp/powers-art020-deterministic-green-20260909.log`
- SHA-256: `5251f3eaefad281ecf894e41399f5003143657aceaab99ab6b253f012fdc845f`
- 1,839 JUnit tests: 0 failures, 0 errors, 0 skipped (425 XML suites).
- 240 Python tests passed.
- Resource validation, exact Java/non-item/VFX audits, item/magic/rank documentation checks, and all common/client/GameTest compilation passed.
- `git diff --check` passed.

## Integration boundary

- Base commit: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.
- No broad/unfiltered main gate was run.
- No plan checkbox or backlog entry was closed.
- No merge, protected-main mutation, or push was performed.
- A fresh independent review remains mandatory before this unit can be considered READY.
