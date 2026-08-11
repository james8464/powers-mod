# Rendered client evidence

These 19 PNGs were emitted by Fabric's real client GameTest runtime on
2026-08-12 using Minecraft 26.2, Fabric Loader 0.19.3, Fabric API
0.156.0+26.2, Java 25, and POWERS 1.0.2.

Command:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
./gradlew runClientGameTest --no-daemon --console=plain
```

The harness creates an isolated integrated-server world, performs actual Dark
and Light Crystal journeys with body-session checks and recovery, captures the
HUD and all principal custom screens at standard/compact sizes, requires positive
operator-command results, verifies post-command state, then closes and saves the
world cleanly. The images are committed so visual conclusions remain reviewable
after `build/` is cleaned.
