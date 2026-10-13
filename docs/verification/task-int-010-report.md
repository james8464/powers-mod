# INT-010 Data-Driven Allegiance Verification Report

## Status

`STAGED — fresh independent review pending; not integrated or pushed.`

The isolated `int-010-allegiance` lane is based on exact clean main commit
`8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`. Protected main and `origin/main`
were not changed.

## Implemented boundary

- One immutable allegiance matrix names player, Shadow, artifact, force, boss,
  food, party, and amethyst roles and resolves same, opposed, neutral, party,
  and amethyst relationships.
- A server-data reload listener accepts exactly one complete policy document,
  validates it before publication, and atomically retains the last valid
  snapshot when a replacement is rejected.
- Player-shaped targeting, artifact guardians, Living Force affinity, Herald
  targeting, Concord eligibility, and Darkness food handling consume the shared
  published policy instead of maintaining independent opposition rules.
- The default datapack policy preserves existing behavior: same allegiance and
  party members are allied, opposed factions are hostile, neutral actors remain
  neutral, and amethyst suppression wins over party membership.

## Focused and live verification

| Proof | Result | SHA-256 |
|---|---|---|
| Focused allegiance/consumer JUnit plus GameTest compilation | GREEN in 12s | `880b0ca5797bd504fad502fe0ddcee42107fd606b09332f6570737341d1e3804` |
| Registered datapack-allegiance GameTest | **1/1 required passed** in 16s | `f1d323585631311fffe1f88bd68173db1434714327b035320ee7d3b532669037` |
| Source-quality repair verification | GREEN in 32s | `6b889200f5916204125f9b21214f0993177f874ae0eaeedc0dfdd3842724af16` |

Retained logs:

- `/tmp/powers-int010-focused-green-20260909.log`
- `/tmp/powers-int010-live-green-20260909.log`
- `/tmp/powers-int010-source-quality-green-20260909.log`

The live test proves the registered server-data listener published a nonzero
revision and exercises every authored role, including party precedence and
amethyst suppression.

## Deterministic branch gate

The first gate stopped immediately on the deliberately strict stale-audit
check. After regenerating the exact source manifest, the second gate exposed a
missing package contract for the new allegiance package (1,841/1,842 tests
passed). Adding `package-info.java` and regenerating the audit made the exact
Java 25 command GREEN:

`./gradlew check compileGametestJava -x runGameTest --rerun-tasks --no-daemon --console=plain`

The final run completed in 2m 2s. Mechanical XML aggregation found **427 suites,
1,842 tests, 0 failures, 0 errors, and 0 skipped**; Python reported **240/240
passed**. Resource validation, generated documentation, source/non-item/visual
audits, example-extension compilation, and GameTest compilation also passed.

Final log: `/tmp/powers-int010-deterministic-green-retry2-20260909.log`

SHA-256: `3fb4f12179bca5efa897d48c554b5c3a2d626adee0e2f4c87262508a5111919f`

## Integration boundary

- No plan checkbox or backlog row is closed by this staged lane.
- No merge, protected-main mutation, push, or publication was performed.
- A fresh independent review must return READY before integration.
