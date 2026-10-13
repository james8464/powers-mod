# SPL-007 — Celestial Ruin atmosphere and distant scars

Status: implementation staged on isolated branch; aggregate Java 25 gate and independent review remain required before integration.

## Implemented behavior

- Celestial Ruin retains its authored 6,000-block quadratic damage and knockback falloff.
- The real client now renders a bounded post-detonation cloud displacement ring and locally falling ash for 600 ticks. Per-event density follows the existing near/mid/far LOD, and overlapping aftermath state is capped at four events.
- Distant terrain scars remain restricted to already-loaded chunks and the existing 2,048-sample server tick budget. Excavation depth now falls through 6/4/2/1-block bands toward the 6,000-block boundary.
- The existing flash, whiteout, ringing, persistence journal, world-border protection, terrain policy, and no-force-load behavior are unchanged.

## Evidence retained outside the repository

- RED unit proof: `/tmp/powers-spl007-red-20260909.log` — SHA-256 `da04c6a58d256036005faf0aeb21c7268c0352f506f99f8b1f6723ace60052bd`.
- Focused unit GREEN: `/tmp/powers-spl007-focused-green-20260909.log` — SHA-256 `7c81de4c86ec97dcfaac6cf531d95fd138800a0dc477a004821b9733d4d7088d`.
- Final affected JUnit/source-quality/resource GREEN: `/tmp/powers-spl007-focused-green-final-20260909.log` — SHA-256 `665c2ac6f7ed3e7a4fae5eccbd429d297a5092eba7dba83767d44ccaf02c04fa`.
- Real-client GREEN: `/tmp/powers-spl007-client-live-final-20260909.log` — SHA-256 `1adc17f9e3480671198f09efcbbaa91b6803b094c6d9d9cc8cb6fffbbd89179d`.
- Server GameTest GREEN: `/tmp/powers-spl007-live-final2-20260909.log` — SHA-256 `057bcdd01eca8bab9c90eb28d5ae68a1c7df60c46377bea66d9d70b973065069`.

Two retained server GameTest attempts exposed and corrected a test-only monotonicity assumption; neither failed production behavior. The final unique run is the authoritative server evidence above.

## Remaining gates

- Run the complete unchanged Java 25 gate when this isolated commit is selected for integration.
- Obtain a fresh independent READY review; no self-approval substitutes for that review.
- Integrate only through the protected main sequencing rules.
