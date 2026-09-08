# INT-011 verification status

Status: **implementation staged; focused verification green; independent review pending.**

The implementation defines one typed order—forcefield, Soul Link, then final
health—and uses it in both ward and mirror calculations. A sacrificial ward
accounts separately for the complete blocked hit and the finite integrity it
spent. Soul Link accounts separately for requested, capped-forwarded, and
observed health damage, while its mirrored-health baseline remains the
recursion boundary. The bounded aggregate is visible through `powers diagnose`
and is cleared with server-owned ability state.

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

The new live GameTest exercises an overkill warded wound, a warded mirrored
wound, an unshielded mirrored wound, and a following tick with no recursive
bounce. It also checks the aggregate blocked-damage, integrity-spend,
forwarded-damage, health-damage, and recursion-guard values.

No unfiltered GameTest, full aggregate, stage checkbox, backlog removal,
integration, merge, push, or final acceptance is claimed here.
