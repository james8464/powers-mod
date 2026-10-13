# SHD-010 verification status

Status: **implementation staged; full gate and independent review pending.**

The isolated branch is based on exact protected head
`8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`. Shadow stores at most 16
normalized owner-local named places. Remembering the same name updates rather
than expands the bounded set; owners can list and explicitly delete entries.
Navigation rejects other dimensions, unloaded chunks, and targets beyond 128
blocks before asking vanilla pathfinding to move. It never creates a chunk
ticket or loads a chunk.

Focused Java 25 tests for the codec, cap, update/delete semantics, schema
compatibility, admission rules, and commands pass. The production-path GameTest
passes 1/1 and proves owner isolation, a real loaded path and movement, explicit
deletion, and zero forced chunks. Earlier live diagnostics exposed an invalid
fixture whose body and target were placed half a block inside its floor; the
final test uses valid bottom-center positions and waits for genuine grounding.
No production admission rule was weakened.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-shd010-red-20260909.log` | `93e66a0d6ac914a9288f607c11961426c0974cd6036afc0efb000a851331b334` |
| `/tmp/powers-shd010-focused-green-final2-20260909.log` | `673a8c78158f9bacc99816bfd154c5b42ec0bb507324c22ebd12fb814fbc7018` |
| `/tmp/powers-shd010-exact-live-final8-20260909.log` | `6bb6d8f833553677298e6fd85d0fabd50c811e4bc3887feffba028127b8ffe52` |

No full-suite, review, merge, push, or closure claim is made by this report.
