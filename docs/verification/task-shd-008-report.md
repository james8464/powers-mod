# SHD-008 verification status

Status: **implementation staged; quiet full gate and independent review pending.**

The isolated branch is based on exact protected head
`8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`. Shadow exposes an
inspectable encounter role derived from server-held facts, preserves absolute
eligibility/protection/amethyst/energy rules, prevents area attacks from
damaging Darkness allies, and applies explicit per-pulse planning and execution
work budgets. Bounded diagnostics expose the chosen role and budget decisions
without player identity or private content.

Focused Java 25 planner, budget, and diagnostics tests pass. The filtered
production-path GameTest passes 1/1 and proves live role selection plus ally-safe
area damage. The deterministic affected suite also passes.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-shd008-red-20260909.log` | `b61fe21c86cfd782ee52acf787a89e42b90d8fdab1f92070846878354b6ff263` |
| `/tmp/powers-shd008-focused-green-final3-20260909.log` | `27f874a2704d956a3e6a475a4f5941cab7a11ee2dc7d190e1ba198e574d725c4` |
| `/tmp/powers-shd008-exact-live-final2-20260909.log` | `89d69acfeec11df6863f39272fce45690338b7c996ac2c8fa8fa897c45387d31` |

One attempted full check under severe host contention failed three unchanged
timing-sensitive GameTests (realm-crystal travel, packet-fault convergence, and
synthetic-dimension cleanup). It stopped before later gates and is retained only
as a failed diagnostic at
`/tmp/powers-shd008-deterministic-green-20260909.log` (despite the historical
filename), SHA-256
`9ba77528bf6005af8da441fcd4f1e1aab962ac95e49d85a02ace8c4cbc618dbf`.
It is not represented as acceptance evidence.

No full-suite, review, merge, push, or closure claim is made by this report.
