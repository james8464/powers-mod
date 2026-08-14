# COR-020 delayed-callback ownership evidence

Status: **accepted** on production commit `c3fc5b1f623f5764b71eafe825901da300ed90dd`.

## Guarantees

- Every multi-tick magic task has an inspectable subject UUID, dimension ID, deadline, cancellation-owner UUID, and purpose.
- Travel, realm-crystal, body-return, locator, storm, punishment, and soak continuations re-resolve live entities and levels only on the active server thread.
- Remote Shadow/First Vessel responses and asynchronous chunk futures re-enter only through the lifecycle epoch that created them.
- Queued serverbound play packets retain UUID/entity ID/dimension/epoch identity instead of the Fabric player context.
- Logout and respawn cancel the owner's callbacks; replacement requests settle exactly once; missing dimensions no-op; server stop invalidates queued remote work and clears all tasks/tickets.
- `/powers diagnose` exposes bounded active delayed-task identities without callback contents.

## Verification

The implementation followed observed RED tests for the missing descriptor/cancellation API, lifecycle epoch gate, and retained server/network context. The accepted source then passed:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew test validatePowerResources auditJavaSources auditNonItemAssets \
  verifyMagicDocs verifyItemDocs verifyRankDocs testPythonScripts \
  --rerun-tasks --no-daemon

JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew runGameTest --no-daemon
```

Results: 1,421/1,421 JVM tests, 27/27 Python tests, and 88/88 required Fabric GameTests passed. Resource, exact-source, non-item asset, magic-doc, item-doc, and rank-doc verification also passed. The live suite includes owner cancellation and unavailable-dimension callbacks through the production scheduler; existing travel, realm, body, possession, artifact, and interaction tests provide affected regression coverage.

`game-test-key-lines.log` is extracted from the accepted live server log. `SHA256SUMS` authenticates it; this README is excluded because it contains the checksum reference.
