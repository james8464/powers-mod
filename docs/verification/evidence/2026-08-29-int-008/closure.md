# INT-008 final integration ledger

Status: READY for closure reconciliation; final recapture/integration PENDING.
Fresh independent reviewer `int008_review7` accepted implementation `aaff0b0f`
and evidence head `9b245015` with no Critical, Important, or Minor findings.
This is not final merge approval. No merge is claimed.

The post-repair unfiltered capture and literal Java 25 aggregate both pass on
clean implementation `aaff0b0f88312b66d232f6e1f4ef0741a8690928`:
167 GameTests, 1,836 JUnit tests across 424 byte-preserved XML suites, and
240 Python tests. The six temporal rows are byte-identical to the exact-SHA
capture. Ordered clean preflight/postflight receipts and the raw-XML inventory
digest are retained in `logs/aggregate-check.log`. The JUnit inventory digest is
`594d4f37feab9c9ed2811051ab3853c02624e13ba0f9b20a1fc92f391d10c5a6`.

The aggregate required unchanged retries for unrelated intermittent packet-loss
and Living Force assertions; those failed transcripts remain diagnostic only.
The successful run completed all 24 tasks without a GameTest lag warning.
Earlier ca1469d4/73d81c13 and 051d105f captures/reviews are historical, not
acceptance of this repaired implementation.

Three deterministic 437-file archives of the reviewed package match SHA-256
`e6bd35f53a629e78f0ac040b912d43d4fb729caf03a9e296a3c01c29d22b2dd8`.
The additional `9b245015` literal gate failed only an unchanged portal fixture
at tick 0 before JUnit; it is not a passing final-head gate. Review found no
established temporal cause. The failed diagnostic is retained separately.

Non-evidence closure documents are now reconciled before a mandatory new final
clean-SHA recapture. The retained aaff package is historical reviewed PENDING
evidence, not final acceptance of the documentation-reconciled head. Only
evidence-prefix changes may follow that final capture; final-head gate/review
and integration gates remain outstanding.

- [x] Repair the Void Scar presence-clock regression through observed RED/GREEN.
- [x] New clean-SHA unfiltered GameTest capture and literal aggregate with ordered receipts/raw XML.
- [x] Mechanically derived exact inventory/counts, deterministic packages, checksums/privacy, evidence commit/push.
- [x] Fresh independent READY review and non-evidence closure reconciliation before final recapture.
- [ ] Final clean-SHA recapture and evidence-only closure commit/push.
- [ ] Unchanged literal full gate on final evidence head and fresh independent READY review.
- [ ] Fast-forward main, unchanged merged-main literal full gate, then push origin/main.
- [ ] Prove main equals origin/main and every POWERS worktree is clean before INT-009.

All remaining ledger updates and retained receipts must stay within this evidence
directory. They do not authorize changing captured source or weakening gates.
