# PERF-010 + UX-004 virtual catalogue evidence

Initial implementation commit: `a5fe966fbf2974c1eb47b639ec5cb72979fae9bb`

Accepted review-correction commit: `eb17a3a82a42d852916e3970710c880337f717dc`

## Accepted visual evidence

- `catalogue-production-1280x720-gui2.png` — production screen, 1280×720, GUI scale 2.
- `catalogue-production-1280x720-gui3.png` — compact/high-scale production screen, 1280×720, GUI scale 3.

```text
072999ce93c2c2b71909ebc064240240a499ed30e32d5e2ac22f272e0f15ead9  catalogue-production-1280x720-gui2.png
914ab763e9d0b494fe38fad7d2a489970f219b39cbabc92e4d993a1f7f57a63c  catalogue-production-1280x720-gui3.png
```

Both captures were manually inspected: title and summary do not overlap; global search leaves every category tab unselected; rows, selected action, bind control, and all eight favourites remain readable. Superseded frames are quarantined under `rejected/` and are not acceptance evidence.

## Proven contracts

- The real production screen holds 10,000 synthetic actions with a constant widget/allocation count through wheel scroll, search, filtering, and revision refresh.
- Global search exposes an unbound action from the default surface; result click plus numbered favourite click is the two-interaction bind path.
- Selection, scroll position, GUI focus, and narration remain coherent across column-major scrolling and revision refresh.
- Recents decode with an eight-entry allocation bound.
- Registered packet handlers prove unbound/stale/rate-limited denial and authoritative bind acknowledgement before commit.

## Final gates

```text
./gradlew runGameTest --no-daemon
127/127 required GameTests passed in 51.50 s

./gradlew check --rerun-tasks --no-daemon
BUILD SUCCESSFUL in 1m 43s
embedded GameTests: 127/127 in 48.29 s
JVM tests: 1,589 passed
Python tests: 45 passed
resource/source/asset/access-widener/generated-doc checks: passed

./gradlew runClientGameTest --no-daemon
BUILD SUCCESSFUL in 36s
```

The previous 129-test count changed deliberately when three shared-identity authority fixtures were replaced by one stronger sequential registered-path orchestrator. No QA-005 capture, QA-006 process/worktree, or protected cache was modified.
