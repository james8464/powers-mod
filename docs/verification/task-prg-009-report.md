# PRG-009 consumable runestones verification

Date: 2026-09-09

Status: **STAGED — independent review pending; not integrated or pushed.**

## Scope

- Raises every runestone stack limit from 16 to 64.
- Removes runestone cooldowns so another rune can be used immediately.
- Consumes exactly one rune only after its energy restoration succeeds.
- Leaves the stack untouched when restoration fails because the reserve is full.
- Preserves the authored per-tier energy values and all existing exhaustion behavior.

## Test-first evidence

- Initial focused RED: `/tmp/powers-prg009-red-20260909.log`
  - SHA-256: `b05d08a2e511189c63eee94363b78fb1f5ce6faf96563992bfec3f4e4f6c340a`
  - Both new contract tests failed against the released stack-16, reusable, cooldown-bound behavior.
- Final focused GREEN: `/tmp/powers-prg009-focused-green-final-20260909.log`
  - SHA-256: `a97badec2d0557eba7176041715fe3449c2c5112fd766eb4925b55fe913e46d3`
  - The source contract, tier rules, and GameTest compilation passed.
- Final exact registered live proof: `/tmp/powers-prg009-exact-live-20260909.log`
  - SHA-256: `5e2ece5b524f21c9d60176e24458ba7f95b20adca65e5562b97564cff83f45ab`
  - Exactly 1/1 required GameTest passed.
  - A full reserve retained both runes; two immediately consecutive successful restores each added 40 energy and consumed one rune, with no cooldown.
- One focused diagnostic failure is retained as a failure: constructing an unregistered raw item was not a valid Minecraft 26 unit fixture. Runtime stack capacity remains covered by the registered live test, while the focused source-boundary check avoids registry bootstrapping.

## Deterministic branch gate

Java 25 and the disposable `/tmp` Python verification environment were used.

- Log: `/tmp/powers-prg009-deterministic-green-20260909.log`
- SHA-256: `edc2557dc944ebe0da4a707f552eb5eb675b4412441468ab8698b6069d3b2402`
- 1,837 JUnit tests: 0 failures, 0 errors, 0 skipped (425 XML suites).
- 240 Python tests passed.
- Resource validation, exact Java/non-item/VFX audits, item/magic/rank documentation checks, and all common/client/GameTest compilation passed.
- `git diff --check` passed.

## Integration boundary

- Base commit: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.
- No broad/unfiltered main gate was run.
- No plan checkbox or backlog entry was closed.
- No merge, protected-main mutation, or push was performed.
- A fresh independent review remains mandatory before this unit can be considered READY.
