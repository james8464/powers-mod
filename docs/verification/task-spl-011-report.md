# SPL-011 Purification Circle modes verification

Date: 2026-09-09

Status: **STAGED — independent review pending; not integrated or pushed.**

## Scope

- Selects exactly one mode from a retained offhand POWERS catalyst: ordinary cleanse, Soul Link severance, or corruption relief.
- Keeps the spell energy-only: its fixed 20-energy transaction remains authoritative and no catalyst is consumed.
- Defaults empty, unknown, and third-party catalyst IDs to ordinary cleanse.
- Caps each cast at the caster plus 255 inspected nearby living entities.
- Treats Exhaustion and dimensional anchors as corruption relief while never removing alignment/progression state or Amethyst Poisoning.

## Test-first evidence

- Initial focused RED: `/tmp/powers-spl011-red-20260909.log`
  - SHA-256: `30e8475c3b89d0cdb7cc54edab25f22e5367125b01278fd3a9da3e9e463298f8`
  - Failed for the deliberately missing mode rules and purification service.
- Final focused GREEN: `/tmp/powers-spl011-focused-green-final-20260909.log`
  - SHA-256: `719e307657b822342305b51779ccb95fe16055daca0d410857517a033b24886c`
  - Catalyst mapping, third-party fallback, exclusive effect policy, Amethyst immunity, and non-consumption wiring passed.
- Final exact registered live proof: `/tmp/powers-spl011-exact-live-final2-20260909.log`
  - SHA-256: `d395b57fba706ebd5a31347d35dd8e1e7fdedc5c1811448bcc67374984578eec`
  - Exactly 1/1 required GameTest passed.
  - The real ritual reserved 20 energy, retained its blood catalyst, cleared its anchor, preserved ordinary Wither in corruption-relief mode, and preserved Amethyst Poisoning across cleanse and relief.
- Two diagnostic live failures are retained as failures: the first exposed missing explicit caster processing; the second sampled net energy after ordinary regeneration. The production service and unchanged semantic assertions were corrected before the final proof.

## Deterministic branch gate

Java 25 and the disposable `/tmp` Python verification environment were used.

- Log: `/tmp/powers-spl011-deterministic-green-20260909.log`
- SHA-256: `3d293d385ba3d4e8f08b14c754223b3fa1fa51ed5c3171abd6790f5abe080fb2`
- 1,841 JUnit tests: 0 failures, 0 errors, 0 skipped (426 XML suites).
- 240 Python tests passed.
- Resource validation, exact Java/non-item/VFX audits, item/magic/rank documentation checks, and all common/client/GameTest compilation passed.
- `git diff --check` passed.

## Integration boundary

- Base commit: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.
- No broad/unfiltered main gate was run.
- No plan checkbox or backlog entry was closed.
- No merge, protected-main mutation, or push was performed.
- A fresh independent review remains mandatory before this unit can be considered READY.
