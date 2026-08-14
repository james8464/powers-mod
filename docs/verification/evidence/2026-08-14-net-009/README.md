# NET-009 verification evidence

Verified on 2026-08-14 with Java 25 against Minecraft/Fabric 26.2.

## TDD record

- RED: `POWERS_TEST_RUN_ID=net009-red ./gradlew test --tests com.powers.api.v1.PowersApiV1Test --no-daemon`
  failed in `compileTestJava` with 50 missing `com.powers.api.v1` symbols before the API existed.
- Focused GREEN: `POWERS_TEST_RUN_ID=net009-final-focused2 ./gradlew test --tests com.powers.api.v1.PowersApiV1Test --no-daemon`
  passed 6/6 compatibility-contract tests.
- An intermediate full GameTest run exposed three built-in-catalogue assertions that included the
  newly discovered external action. Those assertions now distinguish built-in from extension origin.

## Final proof

`POWERS_TEST_RUN_ID=net009-commit-check ./gradlew check --no-daemon` passed:

- 1,480 JVM tests with 0 failures, 0 errors, and 0 skipped;
- all 104 required Fabric GameTests, including `ApiCompatibilityGameTests` loaded through the
  production `powers:v1` Fabric entrypoint;
- common, client, independent example-extension, and GameTest compilation;
- Java source/non-item audits, Python tests (27/27), resource validation, access-widener validation,
  and item/magic/rank documentation verification.

The example GameTest proves the production action catalogue, server-authoritative cast context,
shared protection chain, physical-presence owner, and lifecycle-start hook. API bytecode scanning also
confirms that the public v1 surface has no `net.minecraft.client` linkage.

No save codec or network protocol changed; no migration artifact is required.

## Review fix round 1

- RED: `POWERS_TEST_RUN_ID=net009-review1-red ./gradlew test --tests com.powers.api.v1.PowersApiV1Test compileGametestJava --no-daemon`
  failed at compilation on the intentionally changed context-bound presence API, lifecycle boundary,
  and registration-limit contract before the production fixes existed.
- Focused GREEN: `POWERS_TEST_RUN_ID=net009-review1-green4 ./gradlew test --tests com.powers.api.v1.PowersApiV1Test compileGametestJava --no-daemon`
  passed 9/9 API unit tests and compiled the production-entrypoint GameTest.
- Live GREEN: `POWERS_TEST_RUN_ID=net009-review1-live2 ./gradlew runGameTest --no-daemon`
  passed all 104 required GameTests. The API GameTest rejects forged, synthetic, removed,
  protected, out-of-range, replayed, cooldown, and over-budget requests, and proves bounded accepted
  registration pays energy and reaches the canonical collision runtime.
- Final aggregate attempt: `POWERS_TEST_RUN_ID=net009-review1-check ./gradlew check --no-daemon`
  reached the complete 104-test live suite after its preceding checks, then failed only the unrelated
  pre-existing `FxCoalescingGameTests.eventScaleLodReachesNearMidAndFarObservers` timing assertion.
  Seven immediately preceding complete 104-test runs, including `net009-review1-live2`, passed.

The review fix also proves that entrypoint discovery performs no callback beyond 256 candidates,
over-limit action/protection/hook extensions roll back transactionally, `SERVER_STARTED` observes the
actual Fabric started boundary, and cast authority is an opaque one-shot token bound to the exact live
player-list instance and current server epoch. Registration families and live API presences have both
per-extension and per-epoch hard caps.
