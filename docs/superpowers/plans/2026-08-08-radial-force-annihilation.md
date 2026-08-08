# Radial Force Annihilation Implementation Plan

1. Add a regression proving the cursor starts at the epicentre and never moves to a smaller radial shell.
2. Replace cube-slice traversal with stateful integer-shell traversal without whole-sphere precomputation.
3. Derive the visible wave radius from the processed cursor frontier.
4. Run the Java audit, unit suite, resource validation, clean build, and isolated dedicated-server smoke.
5. Review and commit only the radial-annihilation slice.
