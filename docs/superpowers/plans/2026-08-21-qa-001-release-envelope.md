# QA-001 Exact-Build Release Envelope Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a fail-closed, reproducible release verifier and manual GitHub attestation workflow that can bind every final Stage 1–8 result to one exact `main` commit without closing `QA-001` early.

**Architecture:** A checked-in JSON catalogue declares literal automated commands and typed evidence requirements. Small Python owners validate paths, receipts, evidence, repository state, and artifacts; a runner executes catalogue argument vectors without a shell; an envelope builder emits canonical JSON, Markdown, and checksums only after all validation succeeds. A manual-only GitHub workflow checks out an explicit final SHA, reruns the catalogue, builds the envelope, and attests the two JARs plus both envelope reports.

**Tech Stack:** Python 3 standard library, fixture-driven `unittest`, Git CLI, Gradle/Fabric Loom, GitHub Actions YAML, `actions/attest@v4`, Java 25.

**Spec:** `docs/superpowers/specs/2026-08-21-qa-001-release-envelope-design.md`

## Global Constraints

- Work directly on `main`, but stage only explicit QA-001 paths and commit each accepted task cohesively.
- Keep `QA-001` unchecked and backlog-active until the exact final Stage 8 commit passes remote attestation verification.
- Do not run live GameTests, clients, servers, profiles, or the release workflow while the protected QA-006 soak is active.
- Use test-first development: record the focused RED, make the smallest implementation GREEN, then run affected Python/static gates.
- Use argument vectors with `shell=False`; never accept a command string or interpolate a shell fragment.
- Treat repository/evidence paths as hostile. Reject absolute paths, `..`, symlinks, devices, sockets, writable hard-link aliases, and files outside the owned root.
- Write owned outputs via exclusive temporary files, `fsync`, atomic replacement, and destination rehash.
- Capture only the environment allowlist defined by the catalogue. Never package arbitrary environment variables, credentials, home paths, bearer tokens, UUIDs, public IPs, or unowned absolute paths.
- A hash proves byte identity, not acceptance. Each evidence family must pass its typed validator.
- A committed file cannot embed the hash of the commit that contains it. The committed evidence index and commit-bearing source evidence therefore use only the literal `@HEAD`; final verification resolves it after proving `HEAD` and emits only the full 40-character SHA. Receipts never use the token.
- Do not tag, create a GitHub release, push, remove backlog rows, or check the QA-001 ledger as part of infrastructure implementation.

---

## File map

| File | Responsibility |
|---|---|
| `config/release/qa-001-gates.json` | Static schema-1 gate catalogue: literal argv, environment allowlist, evidence families, artifact rules, and validator IDs. |
| `scripts/release_contract.py` | Shared immutable models, strict JSON/schema parsing, safe path opening/hashing, privacy checks, and atomic output primitives. |
| `scripts/release_gate.py` | Executes one declared automated gate and writes its atomic command receipt. |
| `scripts/release_evidence.py` | Typed validators for JUnit, Fabric logs, soak/profile/compatibility/manual/visual evidence. |
| `scripts/release_envelope.py` | Validates repository state, receipts, evidence, plan/backlog, JARs, then renders deterministic envelope outputs. |
| `scripts/tests/test_release_contract.py` | Catalogue/schema, safe-path, privacy, and atomic-write fixtures. |
| `scripts/tests/test_release_gate.py` | Exact argv, environment, receipt, failure, and interruption fixtures. |
| `scripts/tests/test_release_evidence.py` | Typed evidence positive/negative corpus. |
| `scripts/tests/test_release_envelope.py` | Temporary-Git-repository, artifact, determinism, and final-mode fixtures. |
| `scripts/tests/test_release_workflow.py` | Parses the workflow as text/structured YAML subset and locks its security/release contract. |
| `.github/workflows/release-envelope.yml` | Manual exact-SHA final gate and GitHub artifact attestations. |
| `docs/verification/task-qa-001-report.md` | Infrastructure RED/GREEN record and explicit still-open status; final run section remains pending until Stage 8. |

## Task 1 — Lock the catalogue and safe shared contract

**Files:**

- Create: `config/release/qa-001-gates.json`
- Create: `scripts/release_contract.py`
- Create: `scripts/tests/test_release_contract.py`

- [x] Add tests that import `scripts/release_contract.py` by path and expect:
  - `ReleaseContractError`;
  - immutable `Gate`, `GateCatalogue`, `CommandReceipt`, and `EvidenceRow` models;
  - `load_catalogue(path)` and `load_evidence_manifest(path)`;
  - `safe_regular_file(root, relative)` and `sha256_file(path)`;
  - `write_bytes_atomic(path, data)` and canonical `write_json_atomic(path, value)`.
- [x] Add catalogue fixtures rejecting malformed schema versions, unknown keys/validators, duplicate IDs, empty or non-string argv, command strings, undeclared evidence kinds, mutable URLs, absolute/parent paths, and environment names outside the allowlist.
- [x] Add filesystem fixtures rejecting symlinked roots/children, FIFOs/devices/directories, out-of-root resolution, and writable multi-link inputs; prove rejected inputs and external hard-link targets remain unchanged.
- [x] Add atomic-output fixtures proving deterministic sorted UTF-8 JSON with a trailing newline, distinct replacement inode, external hard-link preservation, temporary-file cleanup after simulated replacement failure, and post-write rehash.
- [x] Add privacy fixtures rejecting credential-shaped text, bearer tokens, home paths, UUIDs, public IPs, and unowned absolute paths while allowing repository-relative paths and documented non-secret hashes.
- [x] Run the focused test and record the missing-module/API RED:

  ```bash
  python3 -B -m unittest scripts.tests.test_release_contract -v
  ```
- [x] Implement only the shared contract required by the tests, using `os.open`/`dir_fd` and no-follow flags for owned files.
- [x] Populate the schema-1 catalogue with these literal automated gate IDs and argv vectors:
  - `final-gradle`: `./gradlew clean check pitest verifyScreenshots verifyVisualGoldens saveMigrationCorpus syntheticSoak --rerun-tasks --no-daemon`;
  - `server-gametests`: `./gradlew runGameTest --rerun-tasks --no-daemon --console=plain`;
  - `client-gametests`: `./gradlew runClientGameTest --rerun-tasks --no-daemon --console=plain`;
  - `compatibility-gametests`: `./gradlew runCompatibilityGameTest --rerun-tasks --no-daemon --console=plain`;
  - `dedicated-server-smoke`: `python3 -B scripts/server_smoke.py`.
- [x] Declare typed evidence families for QA-005, QA-006, PERF-001, compatibility, packet faults, migration, visuals, assets/resources/docs/source audits, four-client acceptance, GitHub CI, and limitations.
- [x] Re-run the focused test GREEN and inspect `git diff --check`.
- [x] Commit only Task 1 paths with message `test(release): lock QA-001 release contract`.

## Task 2 — Produce exact command receipts

**Files:**

- Create: `scripts/release_gate.py`
- Create: `scripts/tests/test_release_gate.py`

- [x] Write CLI fixtures around a temporary catalogue and executable fixture that prove:
  - only a declared gate ID can execute;
  - the exact argv list is passed with `shell=False` in the repository root;
  - only `JAVA_HOME`, `JAVA_VERSION`, `GRADLE_USER_HOME`, `POWERS_TEST_RUN_ID`, `GITHUB_RUN_ID`, `GITHUB_RUN_ATTEMPT`, and `GITHUB_SHA` may be captured when present;
  - stdout/stderr are combined into an owned log file without loading an unbounded log into memory;
  - receipt schema records gate ID, full commit, argv, allowlisted environment, UTC start/end, monotonic duration, exit code, log path/size/SHA-256, and catalogue SHA-256;
  - nonzero exit, signal termination, mismatched `HEAD`, oversized output, changed log bytes, and atomic-write failure cannot yield an accepted receipt.
- [x] Add a deterministic `--dry-run` that validates and prints canonical argv without executing it; ensure dry-run cannot create an accepted receipt.
- [x] Observe the missing-script RED with:

  ```bash
  python3 -B -m unittest scripts.tests.test_release_gate -v
  ```
- [x] Implement `run_gate(catalogue, gate_id, receipt_dir, repo_root, expected_sha, environment)` and the CLI. Stream output to an exclusive owned log, hash it from the same descriptor, then atomically write the receipt only after process completion.
- [x] Return nonzero and leave a diagnostic failure receipt outside the accepted receipt namespace when the command fails; never overwrite a prior accepted receipt for another commit.
- [x] Re-run Tasks 1–2 tests GREEN and run all Python release tests discovered so far.
- [x] Commit only Task 2 paths with message `feat(release): capture exact gate receipts`.

## Task 3 — Validate evidence semantically

**Files:**

- Create: `scripts/release_evidence.py`
- Create: `scripts/tests/test_release_evidence.py`

- [x] Define `validate_evidence(row, path, expected_sha) -> dict[str, object]` and register closed validator IDs rather than dynamic imports.
- [x] Add JUnit XML fixtures for exact totals and zero failures/errors/skips; reject malformed XML, missing suites, skipped tests, and count inconsistencies.
- [x] Add Fabric log fixtures requiring a declared exact required-test total, completion marker, and no failed required test or server error; reject stale commit/count claims.
- [x] Add restart-soak schema fixtures requiring `passed=true`, empty failure, exact planned cycle count, at least 86,400 accepted seconds, accepted disconnect/recovery predicates for every boundary, zero server errors, and the exact commit. Include 23:59:59, failed-cycle, missing-cycle, stale-report, and wrong-commit REDs.
- [x] Add profile fixtures requiring distinct 10/50/100 real-client runs, 1,800 seconds each, exact commit, bounded error fields, and no embedded-actor substitution.
- [x] Add compatibility fixtures requiring every pinned artifact ID/version/file size/SHA-256, exact stack identity, accepted test totals, and explicit LIMITED rows where permitted.
- [x] Add manual/visual fixtures requiring retained raw bytes, source-byte SHA-256, explicit digest-bound reviewer decision, exact runtime metadata, and no pending/heuristic decision. Contact-sheet-only or reconstructed-metadata rows must not pass.
- [x] Add generic JSON/text-manifest validators for packet faults, migrations, assets, sounds, resources, docs, source audits, four-client acceptance, and GitHub CI; each must assert its typed result and exact commit rather than only its file digest.
- [x] Add limitation fixtures proving every accepted limitation is explicit, stable-ID keyed, nonblank, and returned verbatim to the envelope.
- [x] Observe the missing-validator RED:

  ```bash
  python3 -B -m unittest scripts.tests.test_release_evidence -v
  ```
- [x] Implement the closed validator registry with bounded file sizes/counts and path-specific error messages.
- [x] Re-run Tasks 1–3 tests GREEN and run `python3 -B -m unittest discover -s scripts/tests -p 'test_release_*.py' -v`.
- [x] Commit only Task 3 paths with message `feat(release): validate typed acceptance evidence`.

## Task 4 — Build a canonical exact-commit envelope

**Files:**

- Create: `scripts/release_envelope.py`
- Create: `scripts/tests/test_release_envelope.py`

- [ ] Build temporary Git repository fixtures with a bare `origin` and test `validate_repository(repo_root, expected_sha, final_mode)` against:
  - correct/incorrect branch, HEAD, fresh `origin/main`, dirty index/worktree, untracked files, missing remote, extra local/remote branches, and shallow/ambiguous SHA;
  - the one owned ignored output root, allowed only when its contents are exact generated outputs;
  - open plan checkboxes, present QA-001 backlog row, invalid evidence tokens, and evidence values that do not resolve to the verified commit.
- [ ] Split modes explicitly:
  - `preflight` validates infrastructure and inputs but must report `accepted=false` and cannot emit a release envelope;
  - `final` requires every selected ledger checkbox checked and QA-001 absent from the backlog.
- [ ] Add receipt fixtures rejecting a missing gate, duplicate receipt, nonzero exit, argv/catalogue mismatch, unknown environment key, stale commit, changed log, or non-regular log.
- [ ] Add artifact fixtures using `gradle.properties` and ZIP/JAR manifests; require exactly `powers-<mod_version>.jar` and `powers-<mod_version>-sources.jar`, nonempty regular files, exact expected names/version, and destination rehash after packaging.
- [ ] Add determinism fixtures proving two builds from identical validated inputs produce byte-identical deterministic evidence sections, gate/evidence ordering, Markdown, and `SHA256SUMS`; creation/run metadata must be supplied explicitly and cannot affect evidence ordering.
- [ ] Add interruption/rehash fixtures proving no accepted partial output and no manifest acceptance after a source byte changes.
- [ ] Observe the missing-builder RED:

  ```bash
  python3 -B -m unittest scripts.tests.test_release_envelope -v
  ```
- [ ] Implement:
  - `validate_repository(...)`;
  - `validate_receipts(...)`;
  - `validate_artifacts(...)`;
  - `build_envelope(...)`;
  - Markdown rendering exclusively from the validated JSON object;
  - `SHA256SUMS` over JSON, Markdown, both JARs, and packaged receipts.
- [ ] Add CLI arguments `--repo-root`, `--expected-sha`, `--catalogue`, `--evidence`, `--receipts`, `--runtime-jar`, `--sources-jar`, `--output`, `--mode`, `--created-at`, `--github-run-id`, and `--github-run-attempt`.
- [ ] Include literal reproduction commands and four `gh attestation verify <subject> --repo james8464/powers-mod` commands in both envelope formats.
- [ ] Re-run all focused release tests GREEN and run `git diff --check`.
- [ ] Commit only Task 4 paths with message `feat(release): build canonical QA-001 envelope`.

## Task 5 — Lock the manual attestation workflow

**Files:**

- Create: `.github/workflows/release-envelope.yml`
- Create: `scripts/tests/test_release_workflow.py`

- [ ] Write a workflow contract test that rejects any trigger other than `workflow_dispatch`, a non-40-hex input, default-branch checkout, tag/release creation, write permissions beyond attestations/id-token, mutable evidence paths, shell-composed release commands, or a missing final verifier step.
- [ ] Require these exact properties:
  - inputs `release_sha` and `evidence_manifest`;
  - checkout `ref: ${{ inputs.release_sha }}` with persisted credentials disabled;
  - proof that input SHA equals checked-out `HEAD` and fetched `origin/main`;
  - Java 25 via `actions/setup-java@v5` and Gradle setup via `gradle/actions/setup-gradle@v4`;
  - every automated gate invoked by `release_gate.py`, followed by `release_envelope.py --mode final`;
  - permissions exactly `contents: read`, `id-token: write`, `attestations: write`;
  - `actions/attest@v4` with `create-storage-record: false` and explicit runtime JAR, sources JAR, JSON envelope, and Markdown envelope subject paths;
  - ordinary upload of the bundle for retrieval, without treating upload as proof;
  - no `git push`, tag, GitHub release, package publish, or arbitrary download step.
- [ ] Observe the absent-workflow RED:

  ```bash
  python3 -B -m unittest scripts.tests.test_release_workflow -v
  ```
- [ ] Implement the minimal workflow and keep it manual-only.
- [ ] Re-run all `test_release_*.py` tests GREEN and run workflow whitespace/privacy checks.
- [ ] Commit only Task 5 paths with message `ci(release): attest exact QA-001 envelope`.

## Task 6 — Integrate lightweight verification and document open status

**Files:**

- Modify only if required: `build.gradle`
- Create: `docs/verification/task-qa-001-report.md`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/superpowers/plans/2026-08-21-qa-001-release-envelope.md`

- [ ] Confirm `testPythonScripts` discovers every `scripts/tests/test_release_*.py`; avoid a redundant Gradle task if discovery already proves this.
- [ ] Add a lightweight catalogue/preflight validation invocation to ordinary CI only if the Python suite does not already cover it. Do not add any live or final-mode gate to push/PR CI.
- [ ] Record every observed RED and focused GREEN command, implementation commit, changed files, known limitation, and the explicit statement: `QA-001 infrastructure implemented; final envelope not yet accepted`.
- [ ] Document operator flow in README/changelog: local preflight, final evidence manifest, manual workflow dispatch, artifact download, four `gh attestation verify` commands, then and only then optional tagging.
- [ ] Keep the QA-001 plan row unchecked and its backlog row present. Add a regression assertion for both open-state facts if the docs validator can own it without circular final-mode logic.
- [ ] Run while QA-006 remains active:

  ```bash
  python3 -B -m unittest discover -s scripts/tests -p 'test_release_*.py' -v
  ./gradlew testPythonScripts --rerun-tasks --no-daemon
  git diff --check
  ```
- [ ] After QA-006 completes and the host is idle, run the ordinary affected aggregate once; do not run the manual final workflow or claim final acceptance:

  ```bash
  ./gradlew check --rerun-tasks --no-daemon
  ```
- [ ] Request independent code review of the bounded infrastructure diff, address all P1/P2 findings test-first, and repeat focused verification.
- [ ] Commit only the Task 6 documentation/integration paths with message `docs(release): prepare QA-001 operator flow`.

## Task 7 — Final Stage 8 closure procedure (deferred, do not execute now)

- [ ] Complete and accept every remaining Stage 1–8 work unit in strict ledger order.
- [ ] Regenerate the final QA-005 evidence, PERF-001 10/50/100 profiles, QA-006 24-hour soak, compatibility, packet-fault, visual, migration, audit, multiplayer, server, and client evidence for the candidate tree, using exact `@HEAD` only where committed evidence has a commit field.
- [ ] Commit the final schema-1 `@HEAD` evidence index, check every final-acceptance row, and remove QA-001 from the backlog in one release-candidate commit.
- [ ] Execute every catalogue gate via `release_gate.py` at that exact clean SHA and build both JARs.
- [ ] Push only that SHA to `origin/main`; fetch and prove equality plus single-branch state.
- [ ] Dispatch `.github/workflows/release-envelope.yml` with the full SHA and committed evidence-manifest path.
- [ ] Download all four attested subjects and execute each generated `gh attestation verify ... --repo james8464/powers-mod` command.
- [ ] Confirm Actions green, subject digests exact, worktree clean, and no post-attestation commit.
- [ ] Only after those proofs, mark `QA-001` accepted and optionally create the release tag. If any proof fails, keep QA-001 open, repair on a new commit, regenerate all commit-bound evidence, and repeat.
