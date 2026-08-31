# INT-008 final integration ledger

Status: Final recapture passed; final evidence-head gate/review and integration PENDING.
Fresh independent reviewer `int008_review7` accepted implementation `aaff0b0f`
and evidence head `9b245015` with no Critical, Important, or Minor findings.
This is not final merge approval. No merge is claimed.

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

Only evidence-prefix changes may follow this final capture. This package remains
PENDING until the actual final evidence head passes its unchanged literal gate
and fresh independent READY review. Main integration is not yet claimed.

- [x] Repair the Void Scar presence-clock regression through observed RED/GREEN.
- [x] New clean-SHA unfiltered GameTest capture and literal aggregate with ordered receipts/raw XML.
- [x] Mechanically derived exact inventory/counts, deterministic packages, checksums/privacy, evidence commit/push.
- [x] Fresh independent READY review and non-evidence closure reconciliation before final recapture.
- [x] Final clean-SHA unfiltered recapture and literal aggregate with ordered receipts/raw XML.
- [ ] Final evidence-only closure commit/push.
- [ ] Unchanged literal full gate on final evidence head and fresh independent READY review.
- [ ] Fast-forward main, unchanged merged-main literal full gate, then push origin/main.
- [ ] Prove main equals origin/main and every POWERS worktree is clean before INT-009.

All remaining ledger updates and retained receipts must stay within this evidence
directory. They do not authorize changing captured source or weakening gates.
