# INT-011 verification status

Status: **repair staged; complete JUnit and exact filtered live verification green;
independent review pending.**

The implementation defines one typed order—forcefield, Soul Link, then final
health—and uses it in both ward and mirror calculations. A sacrificial ward
accounts separately for the complete blocked hit and the finite integrity it
spent. Soul Link accounts separately for requested, capped-forwarded, and
actual mirrored health removed. Total health accounting is captured around the
authoritative `LivingEntity.hurtServer` return boundary, so it reports net
before/after health removed after armor and on lethal hits. The bounded
aggregate is visible through `powers diagnose` and is cleared with server-owned
ability state.

## Test-driven record

- RED: the first focused test compile failed because `LayeredDamageResolver`
  and `LayeredDamageLedger` did not exist.
- GREEN: the new resolver and ledger tests passed after the numerical contract
  and bounded aggregate were implemented.
- RED: the diagnostic-line test failed until the declared
  `forcefield>soul_link>health` order was exposed.
- GREEN: the resolver/ledger, `MagicShieldManager`, `SoulLinkMath`, and Hearth
  Sanctuary suites pass, and the new production-path GameTest compiles.
- RED (live): the first focused run exposed invalid test setup: direct ability
  activation recorded an innate cast, so lifecycle revalidation removed the
  crystal link. After using the real crystal source, a second run exposed
  vanilla same-tick post-hit immunity between the test's independent wounds.
- GREEN (live): the test now identifies the cast as crystal-sourced and resets
  vanilla hit immunity only between its two independent stimuli. The exact
  filtered GameTest ran once and passed under Java 25.
- RED (repair): `SourceQualityTest` rejected the missing damage-package
  documentation. The new ledger contract tests then failed to compile against
  the old `healthDamage`/`mirrorHealthDamage` names and callback-shaped API.
- GREEN (repair): package documentation is present; the ledger exposes
  `actualHealthRemoved` and `mirrorActualHealthRemoved`; and a registered
  `LivingEntity` return hook records the exact before/after health transition.
- RED (live repair): the first armor fixture used `generic` damage, which is in
  Minecraft 26.2's armor-bypass tag. The corrected hostile-attack fixture then
  demonstrated a positive health loss strictly below its 8.0 incoming damage.
- GREEN (live repair): the exact filtered test passed with one test selected.
  It asserts that the aggregate increases by exactly the armor-reduced health
  delta and then by exactly the lethal target's remaining 3.0 health.

The live GameTest exercises an overkill warded wound, a warded mirrored wound,
an unshielded mirrored wound, and a following production crystal tick with no
recursive bounce. It also checks the aggregate blocked-damage, integrity-spend,
forwarded-damage, mirrored actual-health removal, armor-reduced actual-health
removal, and lethal actual-health removal. The former
`recursiveMirrorsSuppressed` counter was removed: baseline presence did not
prove that a recursive wound had been excluded, while the behavioral no-bounce
assertion directly proves the required outcome.

## Repair verification

- `./gradlew test`: 1,845 tests across 427 suites; zero failures, errors, or
  skips. This includes the complete `SourceQualityTest` and new production
  boundary source coverage from server lifecycle through the crystal registry
  to Soul Link.
- Exact filtered live GameTest: one selected, one passed under Java 25 and
  Minecraft 26.2.
- `compileJava`, `compileGametestJava`, `auditJavaSources`,
  `auditNonItemAssets`, `verifyMagicDocs`, `verifyItemDocs`, `verifyRankDocs`,
  and `validatePowerResources`: passed.

No unfiltered GameTest, stage checkbox, backlog removal, integration, merge,
push, or final acceptance is claimed here.
