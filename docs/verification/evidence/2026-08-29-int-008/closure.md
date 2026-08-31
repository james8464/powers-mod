# INT-008 final integration ledger

Status: NOT READY. Final independent review of `73d81c13` found an Important
Void Scar presence-clock regression, since repaired through observed RED/GREEN.
The retained ca1469d4 package is historical PENDING evidence, not acceptance of
the repair. The new full suite requires 167 GameTests. No merge is claimed.

The initial independent review accepted implementation
`051d105f52b845f8f399995211050b627d03f113` and evidence commit
`a6fd807515529115d96e5eef011ff36ea8b54084` for closure reconciliation with no
new findings. Non-evidence closure documentation is reconciled before final
capture. The final capture and literal aggregate now both pass on clean
`ca1469d40a261288bae891317dac000e6d4a482e`: 166 GameTests, 1,836 JUnit tests
across 424 byte-preserved XML suites, and 240 Python tests. Ordered checkout
receipts and the raw-XML inventory digest are retained in `logs/aggregate-check.log`.
The aggregate required unchanged retries for unrelated intermittent GameTests;
only the complete successful run is acceptance evidence. Earlier transcripts
remain retained as diagnostics, not passing evidence.

- [x] Repair the Void Scar presence-clock regression through observed RED/GREEN.
- [ ] New clean-SHA unfiltered GameTest capture and literal aggregate with ordered receipts/raw XML.
- [ ] Mechanically derived exact inventory/counts, deterministic packages, checksums/privacy, evidence commit/push.
- [ ] Unchanged literal full gate on final evidence head and fresh independent READY review.
- [ ] Fast-forward main, unchanged merged-main literal full gate, then push origin/main.
- [ ] Prove main equals origin/main and every POWERS worktree is clean before INT-009.

All remaining ledger updates and retained receipts must stay within this evidence
directory. They do not authorize changing captured source or weakening gates.
