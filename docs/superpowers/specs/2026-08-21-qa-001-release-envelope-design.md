# QA-001 Exact-Build Release Envelope Design

## Status and scope

This design prepares the `QA-001` release envelope without closing it. `QA-001`
remains open until the exact final Stage 8 release commit has passed every final
acceptance gate, is present on `origin/main`, and its JAR and envelope have
verifiable GitHub artifact attestations.

The envelope is release governance, not a replacement for gameplay evidence.
It validates and binds existing automated, live, visual, multiplayer,
compatibility, profiling, migration, and soak evidence to one immutable commit.

## Chosen architecture

Use a manifest-first local verifier plus a manually dispatched GitHub Actions
attestation job.

1. `config/release/qa-001-gates.json` is the static gate catalogue. It names
   every required final command/evidence family and the validator that proves
   it. It contains no run result and no secret.
2. `scripts/release_gate.py` executes one declared automated gate. It captures
   combined output, exit status, exact argument vector, allowlisted environment,
   start/end time, duration, commit, log size, and SHA-256 into an atomic JSON
   receipt. It never invokes a shell and never records arbitrary environment
   variables.
3. `scripts/release_envelope.py` validates the gate catalogue, command receipts,
   evidence manifest, repository state, plan/backlog state, final artifacts, and
   typed evidence. It emits canonical `release-envelope.json`, a human-readable
   `release-envelope.md`, and `SHA256SUMS` only when all requirements pass.
4. `.github/workflows/release-envelope.yml` is manual-only. It checks out the
   explicitly supplied final SHA, proves that SHA is `main`, runs the exact
   automated release commands through `release_gate.py`, builds/verifies the
   envelope, and attests the runtime JAR, sources JAR, JSON envelope, and Markdown
   report with `actions/attest@v4`.

CI-only generation was rejected because it would make local reproduction and
preflight validation weak. A document-only report was rejected because prose
cannot bind artifacts, logs, counts, and evidence bytes.

## Repository and identity contract

Final mode requires all of the following:

- branch is exactly `main`;
- working tree and index are clean, including untracked files except an explicit
  deny-by-default release-output directory;
- `HEAD` equals the requested release SHA and `origin/main` after a fresh fetch;
- the selected Stage 1–8 ledger has no open checkbox;
- `QA-001` is absent from `docs/planning/IMPROVEMENT_BACKLOG.md`;
- all other branches are absent locally and remotely;
- every receipt and evidence record names the same full 40-character commit;
- runtime and sources JAR version/filename match Gradle project metadata;
- no release tag is required or created by the workflow.

The final commit therefore contains the completed plan/evidence and removes the
QA-001 backlog row. GitHub attests that exact commit after it is pushed. A tag may
be created only after `gh attestation verify` succeeds against the downloaded
subjects. No post-attestation commit is permitted.

## Gate catalogue

The static catalogue is schema-versioned and rejects duplicate IDs, unknown
validators, undeclared files, mutable URLs, shell strings, and missing commands.
It includes these final families:

- the literal Gradle final command from the selected completion plan;
- complete Fabric server and integrated-client GameTests;
- dedicated-server reload/save/restart proof;
- compatibility and packet-fault campaigns;
- four-client/manual acceptance and the regenerated QA-005 checklist;
- final 10/50/100-player profiles and final 24-hour restart soak;
- migration corpus, synthetic soak, mutation, screenshot, and visual-golden
  results;
- asset, sound, resource, item, magic, rank, migration, source-quality, and
  exact-audit manifests;
- GitHub Actions results and unresolved limitations.

The catalogue may be extended as later accepted units add a new mandatory final
gate. Removing or weakening an existing gate is a schema migration requiring an
explicit test and review.

## Evidence manifest

The final evidence manifest is committed on the final release commit and uses
schema 1. Each row contains:

- stable gate/evidence ID and kind;
- repository-relative path;
- full SHA-256 and byte size;
- full implementation commit;
- producer command or fixture identity;
- typed result fields, such as test totals, client count, duration, cycle count,
  or visual review decision;
- explicit limitations where the acceptance permits a limitation.

Paths must be regular files below the repository or the owned release-output
directory. Symlinks, hard-link aliases for writable outputs, absolute paths,
`..`, device files, sockets, and missing files are rejected. Files are opened
without following symlinks and rehashed after packaging. Generated output uses
exclusive temporary files, `fsync`, and atomic replacement.

## Typed validation

The verifier does not treat a file hash as proof of its contents.

- JUnit XML totals are parsed and must have zero failures/errors/skips where the
  gate requires no skips.
- Fabric logs must contain the exact required-test total and success marker and
  no failed required test.
- restart-soak JSON must be the final schema, `passed=true`, have exactly the
  requested cycles, have at least 86,400 accepted seconds, contain no failure,
  and name the release commit.
- profiling reports must name 10/50/100 real-client runs, 1,800 seconds each,
  and the release commit.
- visual/manual evidence must have explicit digest-bound review decisions;
  contact sheets or screenshots without retained source bytes cannot pass.
- compatibility receipts must name exact pinned artifact hashes and the release
  commit.
- command receipts must exit zero and match the catalogue's literal argument
  vector; arbitrary `command` strings are not accepted.
- limitations are included verbatim in both envelope formats and cannot be
  silently dropped.

## Canonical outputs

`release-envelope.json` uses UTF-8, sorted keys, deterministic array ordering,
and a trailing newline. It records:

- repository, branch, full commit, version, Minecraft/Fabric/Java versions;
- creation time and CI run identity as metadata outside deterministic evidence
  ordering;
- every gate, command receipt, typed count, evidence digest, artifact digest,
  and limitation;
- the exact commands needed to reproduce local verification;
- the required post-build `gh attestation verify` commands.

`release-envelope.md` is rendered only from the validated JSON object. It never
parses raw logs independently. `SHA256SUMS` covers both envelopes, both JARs, and
all packaged receipts.

The envelope is not self-referential: GitHub's attestation URL is an external
result. The attested JSON and Markdown report bind all release evidence, while
the workflow summary records attestation IDs/URLs and the subsequent CLI
verification output.

## GitHub Actions contract

The workflow is `workflow_dispatch` only until final release policy explicitly
changes. Inputs include the full release SHA and committed evidence-manifest
path. The job uses Java 25, Gradle's official setup action, and read-only source
checkout. Permissions are minimal:

```yaml
permissions:
  contents: read
  id-token: write
  attestations: write
```

After local verification and artifact creation:

```yaml
- uses: actions/attest@v4
  with:
    subject-path: |
      build/libs/powers-*.jar
      build/release-envelope/release-envelope.json
      build/release-envelope/release-envelope.md
    create-storage-record: false
```

The workflow uploads the envelope bundle as an ordinary workflow artifact too,
but upload success is not attestation proof. Final operator verification uses:

```text
gh attestation verify <subject> --repo james8464/powers-mod
```

Each subject must verify against the exact repository and digest before tagging.

## Failure handling and security

All validators fail closed with a nonzero exit and a concise path/gate-specific
message. They never rewrite source evidence, delete inputs, invoke a shell,
download arbitrary content, or read secrets. Receipt environment capture is an
allowlist limited to Java/Gradle/runtime version identifiers and GitHub run IDs.
Potential credentials, home paths, bearer tokens, UUIDs, public IP addresses,
and unowned absolute paths are rejected from packaged text evidence.

An interrupted run leaves no accepted envelope. Temporary output is ignored and
owned by one explicit release directory. Re-running with the same validated
inputs produces byte-identical deterministic evidence sections and digests.

## Test strategy

Fixture-driven Python tests establish RED before implementation and exercise the
real scripts in temporary Git repositories/directories. Required cases include:

- happy-path canonical generation and repeat determinism;
- dirty tree, wrong branch/SHA/remote, extra branch, and open plan/backlog rows;
- missing, duplicate, stale, malformed, path-escaping, symlinked, and changed
  evidence;
- nonzero or mismatched command receipts and forbidden environment fields;
- failed/skipped JUnit tests and false/mismatched Fabric totals;
- 23:59:59 soak, failed cycle, wrong commit, and incomplete cycle count;
- missing final real-client profiles or insufficient duration;
- raw visual bytes/decision mismatch and unacknowledged limitations;
- artifact filename/version/hash mismatch;
- atomic-output interruption and rehash mismatch;
- workflow parsing that proves manual-only triggering, exact SHA checkout,
  minimal permissions, `actions/attest@v4`, all four subjects, and no tag or
  release mutation.

The current main CI receives a lightweight verifier test only. The expensive
release-envelope workflow runs solely on explicit final dispatch.

## Closure rule

Landing this infrastructure does not check the QA-001 row. QA-001 closes only
on the final Stage 8 release commit after:

1. every selected ledger and final-acceptance checkbox is checked;
2. all final evidence is regenerated for that commit;
3. the final commit is pushed to `origin/main` and Actions is green;
4. the manual envelope workflow succeeds for that exact SHA;
5. every subject passes `gh attestation verify`;
6. local/remote SHA equality, single-branch state, and clean worktree are
   independently confirmed.
