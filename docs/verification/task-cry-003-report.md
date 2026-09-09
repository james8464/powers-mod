# CRY-003 — persistent crystal attunement discovery

Status: implementation staged on an isolated branch; aggregate Java 25 gate and independent review remain required before integration.

## Implemented behavior

- A new copy-on-death persistent attachment records canonical discovered mode IDs per player and crystal.
- New players see every Rainbow selector control as a numbered unknown silhouette. Unknown controls remain mouse-, keyboard-, and focus-selectable, so discovery never creates a control lockout.
- A mode is revealed only after its underlying action activates successfully. Repeated use is idempotent and saved order follows the current canonical catalogue.
- Legacy index/key selections migrate one-way into exactly one discovered canonical mode. Retired IDs resolve through the action alias catalogue; malformed and duplicate entries are dropped.
- The authenticated selector payload now carries the server-authoritative discovered subset, bounded to the same eight-mode transport ceiling.

## Evidence retained outside the repository

- RED proof: `/tmp/powers-cry003-red-20260909.log` — SHA-256 `8c629d8b32d69f5973db0cf339137bfbb0d7cba15eaf3e33366b814d9102c2ca`.
- Focused JUnit GREEN: `/tmp/powers-cry003-focused-green-final-20260909.log` — SHA-256 `7c81de4c86ec97dcfaac6cf531d95fd138800a0dc477a004821b9733d4d7088d`.
- Final affected JUnit/source-quality/resource GREEN: `/tmp/powers-cry003-affected-green-final2-20260909.log` — SHA-256 `665c2ac6f7ed3e7a4fae5eccbd429d297a5092eba7dba83767d44ccaf02c04fa`.
- Server migration/discovery GameTest GREEN: `/tmp/powers-cry003-live-final-20260909.log` — SHA-256 `59758294c5b8ccdd2c5614d410e71ea6ea93737ee513771b79d5847682ea8ab9`.
- Real-client unknown-selector render GREEN: `/tmp/powers-cry003-client-live-20260909.log` — SHA-256 `c0c4b78124ad10e60e6d4ac5ceb6d40fbbe6b34d73be6aab1e363964b300c35b`.

## Remaining gates

- Run the complete unchanged Java 25 gate when this isolated commit is selected for integration.
- Obtain a fresh independent READY review; no self-approval substitutes for that review.
- Integrate only through the protected main sequencing rules.
