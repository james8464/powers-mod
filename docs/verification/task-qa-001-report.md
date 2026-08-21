# QA-001 Exact-Build Release Envelope Report

## Status

`QA-001 infrastructure implemented; final envelope not yet accepted`.

`QA-001` remains **OPEN**. The release-envelope infrastructure is implemented,
but no final evidence index, final release commit, GitHub attestation, CLI
attestation verification, or release tag exists yet. The protected QA-006
24-hour run is still active, and the selected Stage 1–8 ledger remains open.

## Implemented boundary

- `config/release/qa-001-gates.json` declares seven literal no-shell commands,
  fourteen typed evidence families, a closed environment allowlist, and the two
  exact versioned JAR paths. The compatibility acquisition command accepts only
  the manifest's exact Modrinth host/project/version, byte size, and SHA-256.
- `scripts/release_gate.py` runs with a minimal child environment, streams one
  declared command into a held descriptor-relative bounded log, and publishes
  the accepted log/receipt through an owned `O_EXCL` namespace and no-replace
  links only for exit zero at the expected full commit.
- `scripts/release_evidence.py` validates exact JUnit/Fabric totals, the complete
  schema-3 restart soak, 10/50/100 real-client profiles, compatibility artifacts,
  retained digest-bound visual/manual bytes, migration/audit/network/client
  results, and explicit limitations.
- `scripts/release_envelope.py` queries remote heads directly, proves the locked
  72-item ledger and eight final statements, resolves the committed `@HEAD`
  evidence token, and validates control files, receipts/logs, raw review bytes,
  and JAR structure/hash from descriptor-backed snapshots. Every source identity
  is rechecked after deterministic JSON, Markdown, and checksums are emitted.
- `.github/workflows/release-envelope.yml` is manual-only, checks out an explicit
  40-character SHA, uses Java 25, invokes every catalogue gate, and calls
  `actions/attest@v4` for the runtime JAR, sources JAR, JSON envelope, and
  Markdown envelope with minimal permissions. It cannot push, tag, publish a
  package, or create a GitHub release.

The committed evidence index uses literal `@HEAD` because a file cannot contain
the hash of the commit that contains that file. The repository gate proves the
full SHA first; generated receipts and envelopes contain only that resolved SHA.

## TDD record

| Task | Observed RED | Focused GREEN |
|---|---|---|
| Contract/catalogue | `FileNotFoundError: scripts/release_contract.py` | 8 contract tests passed; real catalogue parsed. |
| Gate receipts | `FileNotFoundError: scripts/release_gate.py` | 15 combined tests passed under warnings-as-errors. |
| Typed evidence | `FileNotFoundError: scripts/release_evidence.py` | 24 combined tests passed; exact-byte TOCTOU repair retained GREEN. |
| Envelope | `FileNotFoundError: scripts/release_envelope.py` | 35 combined tests passed after Git symbolic-ref and isolated-fixture corrections. |
| Workflow | missing `.github/workflows/release-envelope.yml` | 41 release tests and Ruby YAML parsing passed. |
| `@HEAD` binding | committed index rejected `@HEAD` | exact-token resolution and mismatched-symbol rejection passed. |
| Path/privacy hardening | symlinked receipt directory and exact-byte private path escaped | descriptor-relative snapshots, closed output inventory, and exact log/receipt privacy regressions passed. |
| Independent review round 1 | clean deleted its live receipt path; fresh CI lacked compatibility bytes; separate-open inputs, recursive upload, truncated governance, overwrite, and refspec gaps remained | moved evidence outside `build`, added pinned acquisition, descriptor snapshots, exact inventory/governance, immutable receipts, and remote-head proof. |
| Independent review round 2 | hidden files were omitted; the cache parent could escape; non-QA selected backlog rows survived; receipt reservation raced; bare secrets/public IPv6 escaped privacy checks | dedicated REDs followed by held dirfd traversal, all-selected backlog reconciliation, explicit hidden upload, minimal child environment, IPv6 rejection, and no-replace receipt publication. |
| Independent review round 3 | a failed lock contender removed the live owner's lock | retained the owner descriptor/inode, unlinked only the exact owned lock, and proved second and third contenders remain rejected; independent final verdict **PASS**, no P1/P2. |

## Verification

- `PYTHONWARNINGS=error python3 -B -m unittest discover -s scripts/tests -p 'test_release_*.py' -q`
  — **59/59 passed**.
- `PYTHONWARNINGS=error python3 -B -m unittest scripts.tests.test_compatibility_harness -q`
  — **15/15 passed**.
- `PYTHONWARNINGS=error python3 -B -m unittest discover -s scripts/tests -p 'test_*.py' -q`
  — **122/122 passed**.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew testPythonScripts --rerun-tasks --no-daemon --console=plain`
  — **122/122 passed**, `BUILD SUCCESSFUL` in 17 seconds.
- `ruby -e 'require "yaml"; YAML.load_file(".github/workflows/release-envelope.yml")'`
  — workflow YAML parsed.
- `git diff --check` — passed for every accepted implementation set.

The first Gradle invocation exited before configuration because the interactive
shell had no `JAVA_HOME`; the unchanged command passed after selecting the
repository's locked Java 25 runtime. No live GameTest, client, server, profile,
compatibility, or final release workflow was started during this work.

## Implementation commits

- `20b98de` — release contract and catalogue.
- `d19ffad` — exact gate receipts.
- `7d1427b` — typed evidence validators.
- `468e53d` — canonical envelope and repository/artifact validation.
- `df9c1b7` — manual attestation workflow.
- `b8f4c95` — realizable `@HEAD` exact-commit binding correction.
- `@HEAD` — independent-review remediation and this open-status operator report;
  the token is resolved by the same clean-repository rule as other committed
  commit-bearing evidence.

## Remaining acceptance

1. Finish and accept QA-006, then run the deferred ordinary affected aggregate
   on an idle host.
2. Complete every remaining Stage 3–8 work unit and regenerate all final
   evidence for one candidate tree.
3. Commit the final `@HEAD` evidence index and final ledger/backlog state.
4. Run all seven catalogue commands at the exact clean final SHA and push only
   that SHA to `origin/main`.
5. Dispatch the manual workflow, download all four subjects, and run every
   generated `gh attestation verify ... --repo james8464/powers-mod` command.
6. Close QA-001 only if those proofs succeed with no post-attestation commit.

## Changed files

- `.github/workflows/release-envelope.yml`
- `config/release/qa-001-gates.json`
- `scripts/release_contract.py`
- `scripts/release_gate.py`
- `scripts/release_evidence.py`
- `scripts/release_envelope.py`
- `scripts/compatibility_harness.py`
- `scripts/tests/test_release_contract.py`
- `scripts/tests/test_release_gate.py`
- `scripts/tests/test_release_evidence.py`
- `scripts/tests/test_release_envelope.py`
- `scripts/tests/test_release_workflow.py`
- `scripts/tests/test_compatibility_harness.py`
- `docs/superpowers/specs/2026-08-21-qa-001-release-envelope-design.md`
- `docs/superpowers/plans/2026-08-21-qa-001-release-envelope.md`
- `docs/verification/task-qa-001-report.md`
- `README.md`
- `CHANGELOG.md`
