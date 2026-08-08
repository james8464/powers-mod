# Radial Force Annihilation Design

## Goal

Make a Darkness–Pure Light clash erase realm matter from the epicentre outward in the same order as its expanding magical corona, while retaining strict per-tick work bounds and loaded-chunk safety.

## Invariants

- The centre is the first visited coordinate; every later coordinate belongs to the same or a larger integer radial shell.
- Every integer coordinate inside the configured sphere is visited exactly once, including the boundary, and cube corners outside the sphere are never visited.
- A caller-provided batch limit remains a hard upper bound; no offsets are precomputed for the whole sphere.
- The displayed wave radius follows the traversal frontier, so visuals never claim that unprocessed outer matter has already been erased.
- Existing clash damage, safe-zone handling, loaded-chunk checks, force-only removal, and particle budgets remain unchanged.

## Implementation

`ForceClashWave.Cursor` traverses successive shells `s`, emitting coordinates whose squared distance is greater than `(s - 1)^2` and at most `s^2`. It retains only scalar cursor state and returns at most the requested batch. `ForceClashWave` performs the batch first, then renders the current frontier radius.
