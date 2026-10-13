# SDD ledger — plan: docs/superpowers/plans/2026-08-12-stages-1-8-completion.md

## Recovery, 2026-10-13

- Existing Stage 4–8 implementation commits are combined in bfbb3824; do not reimplement them from checkbox state. Unchecked acceptance remains open.
- Candidate full check at bfbb3824 failed on Dark Crystal travel, after 115 preceding GameTests. JUnit/Python were not reached. Raw evidence: common Git directory `codex-backups/stages-candidate-rjj18l`.
- 48983d8c adds First Vessel vanilla-melee warning: observed RED, four focused GameTests GREEN, 1,982 JUnit GREEN. Broader MOB-007 acceptance remains open.
- 743ef4ee fixes test-server pacing for cold Dark/Light travel, not production deadlines. Both isolated tests passed with initial destination chunk unloaded. Full gate remains outstanding.
- Ruling: continue independent implementation in the existing isolated acceptance worktree, per the user's later instruction to implement the remaining plan without waiting for one stage. Keep main unchanged; no acceptance, integration, or release claims from focused evidence.
- Exploratory remaining 106 GameTests on exact clean candidate 743ef4ee use session 93858, logs `tail-743ef4ee-7mvm0pps`. Queue parent PID 93921 temporarily stopped between cases for focused WRLD-015 regression; resume with CONT when focused testing ends.
- WRLD-015 review: radius-only lookup duplicates displaced Archivists. First regression attempt failed in setup because origin chunk entities were not tracked yet; not RED evidence. Second attempt explicitly holds the test fixture chunk and allows normal-rate startup before invoking the production boundary.
- WRLD-015 second regression reproduced duplicate at tick 30. Saved outpost identity plus existing entity lifecycle hooks repairs it. Three focused live tests and all 1,984 JUnit tests pass; source audit passes. Full candidate/legacy-save acceptance remains open. Evidence: `docs/verification/task-wrld-015-report.md`.
- Queue parent 93921 resumed with CONT after focused tests; candidate stays unchanged at 743ef4ee for its exploratory tail run.
