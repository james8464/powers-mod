# QA-005 rendered-client evidence

- Tested commit: `e1c656af20bdb706c9204d24ca01cb54e59814bf`
- Runtime: Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25, Apple M3 Pro OpenGL/Metal
- Command: `./gradlew runClientGameTest --no-daemon`
- Result: pass; the integrated server loaded all six dimensions, both realm crystals completed live body-proxy journeys, every command returned a positive result and cleaned its test state, and all 19 named captures were produced.

Human visual inspection passed every retained capture for legibility and state identity. In particular, the ten energy symbols remain directly aligned above Hunger; Light/Dark rank panels use the correct artwork and preserve complete readable node names; standard and compact combat wheels keep all eight slots distinct; the Light Realm sky is white; and arrival lightning surrounds rather than intersects the first-person camera. The Dark Realm remains intentionally near-black and the Light Realm intentionally high-key.

The log's offline-development authentication `401` messages and Minecraft's restored default anisotropic-filter warning are environmental startup messages; neither originates in POWERS nor changes the successful test result. This evidence covers only the named rendered-client rows and is not presented as complete QA-005 proof.
