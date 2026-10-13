# SHD-016 local dialogue research

Status: implementation and deterministic tests complete; quiet-host empirical benchmark and independent review pending.

## Boundary

- The feature remains disabled by default.
- Only remote HTTPS or loopback HTTP endpoints are accepted.
- Remote HTTPS still requires the configured environment credential.
- Loopback Ollama may omit a credential; the transport then omits the authorization header.
- Provider responses remain sanitized prose. Deterministic POWERS code is the sole action, permission, targeting, recipe, and gameplay authority.

## Reproducible benchmark

`scripts/shd016_ollama_benchmark.py` is inert without `--run` and refuses non-loopback endpoints. Its fixed prompts contain no player identity, UUID, chat, coordinates, credential, or world state. The JSON result includes:

- min/p50/p95/max/mean latency;
- Ollama process CPU and resident memory before and after;
- per-case moderation, command-output, redaction-canary, and presentation audits;
- a stateless memory-isolation prompt; and
- an unreachable-loopback offline-fallback probe.

The locally installed candidates observed before benchmarking are `qwen3.5:9b` (6.6 GB) and `gemma4:12b` (7.6 GB). No model was downloaded or changed by this task.

## Deterministic evidence

- Provider tests prove credentialless loopback operation and credential-required hosted HTTPS.
- Transport tests prove the authorization header is omitted locally and retained for credentialed hosted use.
- Python tests prove localhost-only validation, deterministic latency summaries, output auditing, and absence of private canaries from benchmark prompts.
- Existing provider tests continue to cover timeouts, cooldowns, concurrency, prompt privacy, response bounds, and immutable offline/diagnostic authority.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-shd016-focused-java-green-final-20260909.log` | `673a8c78158f9bacc99816bfd154c5b42ec0bb507324c22ebd12fb814fbc7018` |
| `/tmp/powers-shd016-focused-python-green-final-20260909.log` | `e6ec182765cd965f2837d8eeaef5a64af48a104f245cd1a0b7281559194c57c1` |

## Pending acceptance

Run each candidate on a genuinely quiet host, retain the raw JSON, compare latency and CPU/RAM, and record a recommendation. Until that evidence and fresh independent review exist, SHD-016 is not READY and must not be integrated or pushed.
