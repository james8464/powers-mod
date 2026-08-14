# PERF-016 fair catastrophic block-work evidence

## Accepted build

- Implementation commit: `4f85042` (`perf: isolate catastrophic block-work lanes`)
- Date: 2026-08-14
- Minecraft/Fabric: Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2
- Runtime: OpenJDK 25 from `/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`

## Measurement gate and result

The pre-change opt-in live benchmark started 30 powered containment ceremonies in the Overworld and one ordinary ceremony in the Nether. The global first-level scheduler failed at tick 102 with `Catastrophic work starved another dimension for over 100 ticks`, crossing the locked implementation gate.

The accepted implementation uses one hard 256-slot cap, splits it across active dimensions under the current unanimous protection-policy snapshot, and rotates both lane and ceremony work. Request ingestion is O(1) and de-duplicated. Ceremony validation and at most one block inspection occur only after a task receives a bounded slot, so an ordinary tick never scans every active ceremony.

On the same live scene, the Nether ceremony progressed after one tick. A registered deny adapter then kept its protected Darkness block unchanged, proving that the scheduler does not bypass protection policy. The benchmark is opt-in through `-PpowersPerf016Benchmark`; it cannot pollute the ordinary GameTest registry.

The protection integration is deliberately described as one unanimous policy-chain snapshot rather than concurrent individual providers: every block query must consult the full ordered provider chain, so inventing separate provider ownership would make denial unsafe.

## Verification

Commands executed from the repository root:

```text
./gradlew test --tests com.powers.force.BlockWorkBudgetTest --tests com.powers.util.BoundedRoundRobinQueueTest --tests com.powers.protection.PowerProtectionAdaptersTest runGameTest -PpowersPerf016Benchmark --rerun-tasks --no-daemon
./gradlew runGameTest --rerun-tasks --no-daemon
./gradlew test validatePowerResources verifyMagicDocs verifyItemDocs verifyRankDocs testPythonScripts auditJavaSources auditNonItemAssets --rerun-tasks --no-daemon
```

Results:

- 1,438 JVM tests passed; 0 failed, errored, or skipped.
- 27 Python verification tests passed.
- All 100 ordinary required Fabric GameTests passed.
- The isolated PERF-016 Fabric GameTest passed with 30 catastrophic ceremonies, one-tick cross-dimension progress, and claim denial intact.
- Resource, generated magic/item/rank documentation, Java source audit, and non-item asset audit passed.
- Long-horizon deterministic tests cover 10,000 lanes and 10,000 recurring tasks with a 256-slot cap; every entry receives work within `ceil(count / capacity)` ticks.
- Independent read-only review ended with no remaining P1/P2 findings after eager heartbeat validation, all-task scans, rotation gaps, and provider-policy wording were corrected.

`game-test-key-lines.log` preserves the acceptance markers. `SHA256SUMS` covers this bundle's stable evidence files.
