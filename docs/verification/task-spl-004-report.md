# SPL-004 Augury forecast verification

Base revision: `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`.

Status: **STAGED — fresh independent rereview pending; not integrated or pushed.**

## Implemented contract

- Augury reads living-force spread fronts only through `LivingForceManager`'s
  authoritative in-memory index. Each force observation is capped at 128 frontier
  entries and 512 inspected entries within 96 blocks, skips buckets that contain
  only the opposite force, and labels a capped result as a sample.
- Every force result says that it covers loaded chunks only. A negative observation
  never claims that unloaded chunks are empty, and the query uses non-generating
  chunk-presence checks before reading a block.
- Dark Eclipse and Whiteout forecasts reuse `RealmEventRules` for event phase and
  pressure severity. They report the upcoming or active event, its predicted
  severity tier, and the next phase boundary.
- A ritual window is always provisional. An active realm event or an observed
  loaded spread front marks it hazardous; otherwise a known realm schedule bounds
  the provisional window, while other dimensions report an unknown duration.

## Test-first evidence

The deterministic Augury tests were added before production changes. The first
valid Java 25 run was:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew test --tests com.powers.spell.AuguryReportTest \
  --no-daemon --max-workers=2 --console=plain
```

It failed at `:compileTestJava` with 21 expected `cannot find symbol` errors for
the absent `RealmForecast`, `RitualWindow`, `RitualSafety`, `Uncertainty`,
`LoadedFrontForecast`, `realmForecast(...)`, and `ritualWindow(...)` contracts.
The preceding attempt without `JAVA_HOME` did not reach compilation and is not
counted as RED evidence.

After the minimal implementation, the focused Augury JUnit class passed. The
production-entrypoint GameTest
`powers-gametest:powers_game_tests_augury_completes_as_an_unranked_practical_ritual`
passed after observing a real loaded Darkness frontier, classifying its ritual
window as hazardous, completing the real grimoire channel, and proving a distant
unknown chunk inside the 96-block forecast horizon remained unloaded.

## Independent-review repair

Review found that the original 129-result cap bounded matches rather than index
entries inspected. Before the repair, two deterministic index tests were added.
The Java 25 run of `LivingForceIndexTest` failed at `:compileTestJava` with four
expected `cannot find symbol` errors for the absent `Sample` result and
`sampleWithin(...)` API. The implementation now counts every inspected entry,
stops at 512, skips an entire chunk bucket when its existing per-kind counter
proves the requested force absent, and propagates budget exhaustion as explicit
sample uncertainty. A follow-up diagnostic assertion then produced a behavioral
RED: `forecastSamplingStopsAtTheEntryInspectionBudget()` expected one recorded
query but observed zero. The minimal repair now records the query and exactly ten
charged candidates in that fixture. Both new tests pass. The live no-load fixture was also moved
from 640 blocks away to 80 blocks away, inside the actual Augury query radius.

## Second independent-rereview repair

Rereview found that an exhausted sample with zero observed matches still opened a
provisional ritual window and used the definitive no-front presentation. Focused
tests were added first. The first Java 25 RED failed at `:compileTestJava` with
the two expected missing symbols `INDETERMINATE_SAMPLE` and
`frontTranslationKey(...)`. A separate resource RED then ran two presentation
tests and failed one because both incomplete-sample translations were absent.
The minimal implementation now refuses to forecast a safe window from an empty
truncated sample, selects the incomplete-sample message before the ordinary empty
message, and tells the caster that no front was found within the bounded sample
while loaded frontier entries remain uninspected.

The final post-rereview focused verification passed 107 JUnit tests across 32 selected suites
covering spells, force rules/indexes, realm-event rules, translation integrity,
and source quality. Resource validation, GameTest compilation, and the regenerated
exact-source audit also passed.

## Final serialized verification

The registered production-entrypoint GameTest was recaptured and passed **1/1 required** in 16s:

`/tmp/powers-spl004-live-green-20260909.log`

SHA-256: `99608497d2884c9ab3e7325296efc80835767e054bb92900028403686caaa070`

The exact Java 25 branch command was then run serially:

`./gradlew check compileGametestJava -x runGameTest --rerun-tasks --no-daemon --console=plain`

The first attempt stopped on the strict stale non-item-asset manifest. After regenerating the
non-item and VFX manifests for the changed language resource, the unchanged command completed
**GREEN** in 2m 2s. Mechanical XML aggregation found **425 suites, 1,845 tests, 0 failures,
0 errors, and 0 skipped**; Python reported **240/240 passed**. Resource validation, generated
documentation, source/non-item/visual audits, example-extension compilation, and GameTest
compilation all passed.

Final log: `/tmp/powers-spl004-deterministic-green-retry1-20260909.log`

SHA-256: `edf5808e474582b6013bd5d42443b5ef95e55f9507e8368237cb8da98ffe3079`

The unfiltered Fabric GameTest suite and protected-main gate were not run. No plan checkbox or
backlog row was closed, and no merge, push, or publication was performed. Fresh independent
rereview must return READY before integration.
