# QA-005 Dimensional Anchor evidence

- Build: `d2de1a989097b0b1f8ca0e4af1d4b73327da5d1a`
- Runtime: Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Java 25
- Participants: two real rendered Fabric clients (`AnchorCaster`, `AnchorTarget`), a persisted player-like test actor, and one dedicated Fabric server
- Route: production Deep Grimoire selection followed by vanilla main-hand use

The target client enabled teleport consent and both clients occupied the prepared ray line. A persisted test actor was also present at the same target point and won the server raycast. The completed channel produced the caster-side authoritative message `Test_a304de4a is now bound to overworld.` This proves the spell’s supported player-like-target state path; this run is not cited as proof that the real-player consent target was selected.

Both clients disconnected cleanly and the dedicated server saved all six dimensions before stopping. The caster screenshot, both client logs, server log, exact scripts, and checksums are retained beside this report.
