# INT-008 final integration ledger

Status: INT-008 integrated and pushed on `main` at exact reviewed head
`922873d85ff77ee4a02473492b1b92eb6f8b0a64`. The resulting evidence-only
integration-receipt head remains subject to its own unchanged full gate and
fresh independent review before INT-009 starts.

Fresh independent reviewer `int008_review7` accepted implementation `aaff0b0f`
and evidence head `9b245015` with no Critical, Important, or Minor findings.
Fresh clean-context reviewer `int008_pass_review9` subsequently accepted exact
evidence head `922873d8` with no findings after independently checking the
production diff, retained evidence, prior failure uncertainty, and exact-head
gate receipts. See `review-evidence-head-922873d8.md`.

After that review, non-evidence closure documents were reconciled without any
gameplay/build/script changes. The final unfiltered capture and literal Java 25
aggregate both pass on clean SHA `100efb814308a70aff228a877c2f9aca5cb9e550`:
167 GameTests, 1,836 JUnit tests across 424 byte-preserved XML suites, and
240 Python tests. The six temporal rows are byte-identical to the exact-SHA
capture. Ordered clean preflight/postflight receipts and the raw-XML inventory
digest are retained in `logs/aggregate-check.log`. The JUnit inventory digest is
`1cbc259e97100e7a7ada20f1f409b0a2f46597614bb72bb3cbd4529cfbe27ff0`.

This final-SHA capture and aggregate each passed their first unchanged attempt,
without a GameTest lag warning. The aggregate executed all 24 tasks in 3m 12s.
Earlier aaff0b0f, ca1469d4/73d81c13 and 051d105f captures/reviews are retained as
historical evidence, not substitutes for these final-SHA receipts.

Three deterministic 437-file archives of the earlier reviewed aaff0b0f package match SHA-256
`e6bd35f53a629e78f0ac040b912d43d4fb729caf03a9e296a3c01c29d22b2dd8`.
The additional `9b245015` literal gate failed only an unchanged portal fixture
at tick 0 before JUnit; it is not a passing final-head gate. Review found no
established temporal cause. The failed diagnostic is retained separately.

## Observed exact evidence-head acceptance

Clean evidence head `5fa0de1a5944a3e3f31589014db32ea8c3ba3dc6` was committed
and pushed, then passed the unchanged literal Java 25 full gate on retry8:
167 GameTests, 1,836 JUnit tests, 240 Python tests, 24 executed tasks, and
BUILD SUCCESSFUL in 3m 7s. No lag warning was recorded. The original raw log
SHA-256 is `fd0f00c370f708c1c8427b6a07d6dc49ce519f5777bf1e645e95f84a4a4cf43d`.
Its privacy-redacted transcript and all 424 byte-identical raw XML suites are
retained under `logs/final-head-5fa0de1a/`. Ordered clean-SHA pre/post receipts
bind that gate to `5fa0de1a`; the XML inventory digest is
`a8b17b694093bc054867e4f912b90c698c8e581206166c0be56f3dc225dfe364`.
The literal aggregate's six diagnostic temporal rows use the default zero SHA;
they are not substituted for the separate exact-100efb81 capture rows.

Fresh independent `int008_final_review8` verified the new log hash, receipts,
all 424 XML hashes and 1,836 actual testcases with no failures/errors/skips.
It returned READY for exact clean `5fa0de1a`, with no Critical, Important or
Minor findings. See `review-final-head-5fa0de1a.md`. Its prior independent audit
also checked 524 immutable-base source entries, six exact capture rows,
inventories/checksums/privacy, three deterministic 437-file PENDING archives
(`7073d0c88561c0de1b7dc180bead39a069b03bf4d1b1d6c492e82fe5f0cd990a`),
and 22 focused verifier/package tests.

The preceding eight failed attempts remain diagnostics, not acceptance. They
include patrol scan-order sensitivity, packet payment/loss/health assertions,
foreign-dimension return bounds, reload timeout, and occasional server stalls.
The source-backed patrol scan mechanism is strongly correlated with batch order
and retained players, but exact runtime index/pulse traces were not captured;
packet-health cause remains unlocalized. No demonstrated temporal clock-state
leak was found. The green retry does not erase those failures or establish
that they were unrelated to INT-008. No acceptance gate or test was weakened.

Only evidence-prefix changes may follow the final capture. PASS now records
the captured implementation and exact reviewed `5fa0de1a` acceptance. This
receipt-only update creates a later head which is NOT covered by that gate:
it must pass its own unchanged literal full gate and fresh independent review
before fast-forward integration. At that stage, main integration was not yet
claimed; the subsequent observed closure is recorded below.

## Observed integration closure

Evidence-only receipt head `922873d85ff77ee4a02473492b1b92eb6f8b0a64`
passed its unchanged literal Java 25 full gate and fresh independent
`int008_pass_review9` review before integration. Local `main` was then
fast-forwarded to that exact head while all 11 POWERS worktrees were clean.

The third unchanged merged-main retry passed all 167 GameTests, all six
INT-008 temporal rows, 1,836 JUnit tests across 424 raw XML suites, 240 Python
tests, and all 24 tasks. Ordered clean-SHA preflight/postflight receipts bind
the run to `922873d8`; the recomputed JUnit inventory digest is
`c794655c8864a620f9d9f25ee8798d36ea0f51a4ab47301379b63b6b6965aa82`.
The original transcript SHA-256 is
`197a4cec003f8fc274a8708ef0a99e8af6945d340b9794725cbc80c3554fc7a5`;
the private-prefix-only redacted retained transcript SHA-256 is
`33cbf99881656fc0edeb95e6db4f9108aded800cde6ac3d55332a726be8147d3`.
The transcript and byte-identical XML are under `logs/merged-main-922873d8/`.

BUILD SUCCESSFUL reported 1h 5m 45s and the raw transcript contains a large
host wall-clock jump. This receipt does not describe the host as stable for the
whole run. The three earlier merged-main failures and their diagnostic
uncertainty remain preserved; the green run does not establish that those
failures were unrelated to INT-008.

After the green merged-main gate, `origin/main` was pushed and a fresh fetch
proved local `main`, `HEAD`, `origin/main`, and
`origin/int-008-temporal-leases` all equal `922873d8`; all 11 worktrees were
clean. This evidence-only integration receipt necessarily creates a later head,
which must pass its own unchanged full gate and fresh independent review before
it is treated as verified or INT-009 starts.

- [x] Repair the Void Scar presence-clock regression through observed RED/GREEN.
- [x] New clean-SHA unfiltered GameTest capture and literal aggregate with ordered receipts/raw XML.
- [x] Mechanically derived exact inventory/counts, deterministic packages, checksums/privacy, evidence commit/push.
- [x] Fresh independent READY review and non-evidence closure reconciliation before final recapture.
- [x] Final clean-SHA unfiltered recapture and literal aggregate with ordered receipts/raw XML.
- [x] Final PENDING evidence-only commit/push at `5fa0de1a`.
- [x] Unchanged literal full gate on exact `5fa0de1a` and fresh independent READY review.
- [x] PASS/receipt-only closure commit/push, its own unchanged full gate and fresh READY review.
- [x] Fast-forward main, unchanged merged-main literal full gate, then push origin/main.
- [x] Prove main equals origin/main and every POWERS worktree is clean before INT-009.

All remaining ledger updates and retained receipts must stay within this evidence
directory. They do not authorize changing captured source or weakening gates.
