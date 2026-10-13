# SHD-014 verification status

Status: **implementation staged; full gate and independent review pending.**

The isolated branch is based on exact protected head
`8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`. Owner danger, nearby
intrusion, and amethyst suppression are critical warnings with a fixed
100-tick per-kind repeat limit. Combat engage/disengage chatter is disabled in
critical-only mode and bounded to 200 ticks in normal mode or 80 ticks in
verbose mode. Hidden Shadow output is private to its owner; revealed output
uses the existing global delivery path. The runtime observes only session facts
already held by the server and introduces no world scan.

Focused Java 25 tests for priority, cooldowns, mode persistence, diagnostics,
and parsing pass. The production-path GameTest passes 1/1 and proves critical
warnings survive critical-only mode, verbose combat-state output, owner-private
hidden delivery, revealed global delivery, and a real indexed amethyst
suppression source. Failed live diagnostics used non-due distributed-session
ticks and then a synthetic effect that production correctly cleared; neither is
represented as acceptance evidence.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-shd014-red-20260909.log` | `bd82d6f125ca7c0721aca9b3ddf32a5ac5f4c93228bf0ad21fed0bfcd56f4faa` |
| `/tmp/powers-shd014-focused-green-final3-20260909.log` | `27f874a2704d956a3e6a475a4f5941cab7a11ee2dc7d190e1ba198e574d725c4` |
| `/tmp/powers-shd014-exact-live-final4-20260909.log` | `bd7db05c6a3eee362b0354622ec29c2b98e034cbbc3b57999b2d4a40d1441582` |

No full-suite, review, merge, push, or closure claim is made by this report.
