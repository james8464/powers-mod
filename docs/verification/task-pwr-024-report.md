# PWR-024 verification status

Status: **implementation staged; focused verification green; independent review pending.**

This branch is intentionally stacked on the staged INT-007 shared thermal-table
commit `bb1c615798f269e1c0616d7ac08f5cf25b40cc3d`. Ice Manipulation now creates
vanilla frosted ice rather than permanent ordinary ice, so bridge terrain owns a
finite vanilla melt lifecycle without a POWERS queue, forced chunk loading, or
unbounded restoration index. Fireball consumes that temporary frost through the
same finite thermal table and retains the immutable Pure Light and Darkness
rules.

Successful Ice damage applies one transient, refreshable 25% armour and armour-
toughness fracture for 100 server ticks. The UUID-keyed owner never retains
entity objects, removes only its namespaced modifiers, and clears on successful
fire damage, natural expiry, death, player replacement/disconnect, and server
stop. Failed or forbidden damage cannot apply or consume the fracture.

## Test-driven record

- RED: the focused lifecycle and production-wiring tests failed to compile
  before `IceLifecycleRules` and `BrittleArmorManager` existed.
- GREEN: the pure lifecycle test proves the exact 100-tick duration, monotonic
  refresh, expiry boundary, -25% modifier, and fire/non-fire transition.
- GREEN (live): the exact production Ice test applied armour 20→15, cleared it
  to 20 after successful fire damage, reapplied it, and observed production
  ticking expire it back to 20 after 100 ticks (1/1). Log:
  `/tmp/powers-pwr024-brittle-expiry-exact-20260909.log`, SHA-256
  `f6e7534a3265c3bc07ea8ee2f8f8eecf3f74bf881b8530b475fd7468c4f99ff8`.
- GREEN (live): the exact Fireball production test melted frosted ice to stable
  water while preserving adjacent Pure Light and Darkness (1/1). Log:
  `/tmp/powers-pwr024-frost-fireball-exact-20260908.log`, SHA-256
  `2c754d8278e77bd8785c7d6e3f88a045380d1041deb8532965a7a01b247b8711`.
- GREEN (live): the exact hostile water/lava regression exercised Ice, Flight,
  and Inferno with stable bounded thermal results and no duplicate drops (1/1).
  Log: `/tmp/powers-pwr024-hostile-thermal-exact-20260908.log`, SHA-256
  `780bc3b0e4f248c31f7306d871f3eddc253b5f0cd3d6d83775a583f06497e0c5`.
- GREEN: Java 25 deterministic branch gate: 1,842 JUnit tests, GameTest
  compilation, resources, generated documentation, Java/non-item/VFX audits,
  and all 240 Python fixture tests. Log:
  `/tmp/powers-pwr024-deterministic-20260909.log`, SHA-256
  `4babcadc1cbc8ffa732eec2b09e08640cc75aba9fc0ff524f403e03ebfbff440`.

## Non-authoritative unfiltered attempt

The combined `check compileGametestJava` invocation unexpectedly included the
unfiltered server GameTest task. It stopped with two unrelated timing-sensitive
failures: API-preview parked-world expiry and LivingForce patrol manifestation.
The attempt is retained at `/tmp/powers-pwr024-check-20260908.log`, SHA-256
`f42780b883ed4df26384b5686d04b89b0cff7284de7e31304ff40f94fbce758c`;
it is not cited as acceptance evidence.

No aggregate main gate, stage checkbox, backlog removal, integration, merge,
push, or final acceptance is claimed. Fresh independent review is still
required; the available reviewer tasks reached the account usage limit.
