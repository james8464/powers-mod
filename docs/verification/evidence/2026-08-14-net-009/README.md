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
