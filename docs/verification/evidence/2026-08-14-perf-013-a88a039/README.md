# PERF-013 semantic-FX distance LOD evidence

## Exact build

- Commit: `a88a039a63c060a0e99ecaed679440e533522f8a`
- Date: 2026-08-14
- Platform: macOS, Java 25, Minecraft/Fabric 26.2 development runtime
- Scope: beams, runes, spirals, generic semantic magic, Herald and First Vessel ceremonies, and Celestial Ruin

## Accepted behaviour

- Near observers receive full authored semantic density.
- Mid observers retain the full silhouette at reduced density.
- Far observers retain event diameter, shape, signature palette, timing, flash, and restrained audio; ordinary local effects remain range-bounded.
- Distance LOD changes presentation only. Damage, collision, protection, lifecycle, and spell state remain server authoritative.
- The server marks only observer-authorised distant cues as always visible, including geometry that straddles Minecraft's ordinary particle-distance boundary.
- Celestial Ruin retains a 100-block-diameter, 264-block-tall column and reaches observers out to 6,000 blocks. Near/mid/far ringing gains are bounded, and simultaneous Ruins keep independent four-entry client ringing state.
- Adaptive FX reduction cannot remove the minimum recognisable samples for a rare event family.

## Test-first defects closed

The implementation was driven through failing tests for absent LOD policy, column-density rules, large-radius viewer lookup, event audio payloads, distance-override payload flags, Celestial audio gain, and boundary geometry. Independent review then identified and verified fixes for client particle culling beyond 32 blocks, unattenuated far Ruin audio, a false-positive client particle assertion, combined radial/vertical shape extent, and overlapping Ruin audio state.

## Verification

| Check | Result |
| --- | --- |
| JVM suite | 1,462 passed; 0 failed, 0 errors, 0 skipped |
| Fabric server GameTests | 101 passed; 0 failed |
| Rendered-client GameTests | Passed; real local/event runes rendered at 144/1,800 blocks, distant audio arrived, and two concurrent Ruin ring states remained active |
| Resource validation | Passed |
| Magic/item/rank documentation verification | Passed |
| Python validation suite | 27 passed |
| Java source audit | Passed against the exact implementation tree |
| Independent review | No remaining actionable P1/P2 findings |

Exact invocations and concise results are recorded in [verification-summary.txt](verification-summary.txt). Checksums for this evidence directory are in [SHA256SUMS](SHA256SUMS).

## Limits

LOD is intentionally semantic rather than view-frustum aware: server distance and work budgets authorise an event, while the client renderer performs ordinary frustum handling. Distant ordinary/local magic is capped at 256 blocks; only explicitly event-scale or catastrophic choreography reaches farther. This work does not alter physical collision or gameplay range.
