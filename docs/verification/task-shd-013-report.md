# SHD-013 verification report

## Scope

Make Shadow's subtle Darkness agenda stateful across conversations and body replacement while preserving truthful mechanics, recorded failures, and safety boundaries.

## Implementation

- `ShadowAgendaPhase` maps the existing bounded owner-local influence register to Whisper, Invitation, and Covenant phases.
- `ShadowAgendaVoice` appends phase-specific persuasion only after the verified reply; factual and safety text remains unchanged at the front of every answer.
- Nonblank owner dialogue advances the saved influence register by four points, clamped to its existing `0..100` bound. Blank and system-originated messages do not advance it.
- Conversation memory already travels with the owner rather than the Shadow body, so agenda state survives body death and replacement without a new persistence schema.
- JVM tests distinguish every phase, assert exact factual-prefix preservation and safety language, prove bounded advancement, and round-trip the state through persisted copies.
- A live GameTest advances eighteen conversations through all three phases and proves the exact guard-command fact remains the leading text at early, middle, and late checkpoints.

## Evidence

| Evidence | Result | SHA-256 |
|---|---|---|
| `/tmp/powers-shd013-red-20260909.log` | RED: the phase, voice, and memory APIs do not yet exist | `c37ce63508dcf3282de192f417ddc9ac0fc54680adf2902f23a1b2a1e9452143` |
| `/tmp/powers-shd013-focused-green-20260909.log` | GREEN: focused companion agenda and persistence tests | `719e307657b822342305b51779ccb95fe16055daca0d410857517a033b24886c` |
| `/tmp/powers-shd013-exact-live-20260909.log` | GREEN: exact isolated live agenda test, 1/1 | `fadccee5549b100b1b1429e9c1bc2f16e78428b9153548e171a53b58e24db477` |
| `/tmp/powers-shd013-deterministic-green-20260909.log` | Retained fail-fast diagnostic: Java source audit detected the intentionally stale generated inventory before any tests ran | `8fe174996881706382bc93ead04867199bac73668f17d2a988aebbe93807d5ba` |
| `/tmp/powers-shd013-deterministic-green-final-20260909.log` | GREEN: literal Java 25 `check --rerun-tasks`; 168/168 GameTests, 1,838 JUnit tests across 425 suites, 240 Python tests, and all audits/resource gates | `c982614d47ac02341b74e658f4e0aba621894242ce1675bfb895b75e37d0d948` |

## State

Implemented and verified on an isolated worktree based exactly on `8c7c50b74c9e2b2d09500340a4ca81ef02b4216a`. Independent review remains required before integration; this report does not self-approve the unit.
