# VFX-007 layered magical audio design

## Goal

Replace the single-file presentation of every POWERS semantic sound cue with authored near, mid,
and far layers that remain directional, fall off predictably, soften behind opaque blocks, expose a
subtitle, and cannot combine into a clipping-prone burst. Celestial Ruin also receives a genuinely
lower-frequency reduced-tinnitus alternative.

VFX-007 is presentation-only. It does not change cast success, cost, timing, damage, protection,
tracking, persistence, or world state. UX-009 still owns an in-game comfort-options screen and the
broader flash, shake, FOV, and beam-opacity controls. VFX-012 still owns music and ambient loops.

## Finite scope

The catalogue is the 16 semantic cues registered by `PowersSounds`: the 14-cue bank that existed
when the backlog row was written plus the later VFX-008 `beam_ring` and `boss_impact_ring` cues.
Vanilla sounds used by unrelated mechanics are not silently replaced.

Every cue has one immutable network ID, subtitle key, mixing group, range profile, and tinnitus
classification. Each cue provides near, mid, and far mono Vorbis assets. `celestial_ring` additionally
provides reduced near, mid, and far assets. Existing base OGG files remain immutable source masters
and compatibility resources; production playback resolves only through the layered catalogue.

## Chosen architecture

### Alternatives considered

1. **Bounded semantic packet plus listener-side classification — chosen.** The server emits one
   validated semantic event to eligible same-dimension players. Each client selects its layer from
   camera distance, performs its own obstruction ray, applies comfort policy, and admits the result
   through one mixer. This gives each listener correct direction, distance, wall state, and settings
   without trusting the client with gameplay authority.
2. **Three server-side `playSound` calls.** Vanilla attenuation could fade three simultaneous files,
   but bands overlap, walls do not affect the mix, subtitles can triple, and bursts clip easily.
3. **One pre-mixed file per cue.** This is small but cannot provide listener-specific distance,
   occlusion, or reduced-tinnitus behavior and therefore does not satisfy the backlog row.

### Shared catalogue and pure rules

`LayeredAudioCue`, `LayeredAudioProfile`, `LayeredAudioLayer`, and `LayeredAudioRules` form a small
common package. They provide stable bounded IDs and pure calculations for:

- range classification using profile-specific near/mid/far thresholds;
- rejection outside the far radius or in a different dimension;
- obstruction behavior: advance one layer toward the softer far asset and multiply gain by 0.45;
- finite pitch and gain clamping;
- reduced-tinnitus asset selection for the Celestial cue only; and
- headroom scaling for concurrent cues in the same mixing group.

Three range profiles avoid pretending a quiet rune and a world-scale catastrophe have the same
reach: intimate uses 8/28/72 blocks, standard uses 12/48/128, and world uses 20/96/256. Thresholds
are inclusive at their inner edge, and anything beyond the final radius is silent.

### Server emission

`LayeredAudioService.emit` accepts a server level, origin, catalogue cue, finite base gain, and finite
pitch only after the owning gameplay operation has committed. It assigns a monotonic bounded event
ID and sends `LayeredAudioPayload` only to connected, payload-capable players in the same dimension
and within the cue's maximum radius. The payload contains the event ID, cue ID, exact dimension,
origin, gain, pitch, and emission game time. Decode and construction reject unknown IDs, non-finite
numbers, invalid dimensions, out-of-world coordinates, excessive gain/pitch, and future or stale
times.

Existing POWERS semantic call sites migrate from direct `PowerFx.sound` or local UI playback to this
service only at their already-committed presentation boundary. `EventAudioPackets` folds into this
one vocabulary. There is no duplicate vanilla fallback because POWERS is required on both sides;
unsupported clients simply receive no new packet.

### Client classification and mixing

The client rechecks dimension and age, deduplicates the event ID in a 256-entry ledger, measures from
the active camera, and performs one bounded block clip from camera to origin. A miss is unobstructed;
an opaque collision is obstructed. It then resolves exactly one positional layer and submits it to
`ClientLayeredAudioMixer`.

The mixer uses the normal POWERS/player sound category, retains directionality, and never plays all
three layers together. It admits at most eight POWERS semantic sounds globally and four per mixing
group in a four-tick burst window. Exact duplicate cue/origin cells coalesce. Concurrent admitted
sounds receive inverse-square-root headroom scaling, with final gain capped at 0.90. Dropped and
coalesced counts are diagnostic metrics, not gameplay state. Dimension change, disconnect, and
resource reload clear the ledger and mixer bookkeeping.

### Comfort configuration

`config/powers-client.json` gains a narrowly scoped `reducedTinnitus` boolean, defaulting to false.
Malformed or absent files fall back safely and log once without crashing. The file is read at client
initialization and resource reload. When enabled, only Celestial Ruin selects the reduced assets;
event timing and subtitle identity remain unchanged. This file-level switch makes the support usable
now while leaving the eventual narrated GUI to UX-009.

### Assets and subtitles

A deterministic generator reads the 16 committed mono masters and writes 51 versioned layers: 48
ordinary near/mid/far assets plus three reduced Celestial assets. Near retains the authored transient
under a conservative limiter; mid softens the high shelf and transient; far applies a stronger
low-pass and lower integrated gain. Reduced Celestial removes the piercing high partials and keeps a
short low-mid warning contour, rather than merely lowering the same tinnitus sample.

Every playable layer in `sounds.json` names the same localized subtitle for its semantic cue. The 16
English strings describe the event rather than its distance, so a subtitle does not reveal hidden
range information and reduced-tinnitus mode does not change meaning.

## Failure handling and bounds

- Unknown, malformed, stale, future, cross-dimension, or out-of-range payloads are ignored and
  counted; they never fall back to an arbitrary sound.
- Missing layered resources log once and resolve to the matching committed base master at restrained
  gain. A missing reduced asset falls back to silence for the tinnitus layer, not the piercing master.
- Client obstruction is advisory presentation state only. It cannot suppress server mechanics or
  alter which player was eligible to receive the event.
- Packet fan-out is bounded by the same-dimension player list, one payload per listener per committed
  event, with no chunk tickets, entity scans, persistence, or per-tick server task.
- Audio ledgers are fixed-size and reset on lifecycle boundaries; event IDs cannot grow collections.

## Verification and evidence

Implementation follows TDD. Pure JUnit tests cover the 16-entry catalogue, stable network IDs,
threshold boundaries, obstruction, comfort selection, gain/pitch clamps, headroom, coalescing,
ledger eviction, lifecycle resets, malformed packets, and missing-resource fallback. Source-boundary
tests prove all semantic hooks are post-commit and that legacy direct semantic playback is gone.
Fabric GameTests prove same-dimension/range recipient selection, payload count bounds, reconnect,
dimension change, and no gameplay mutation.

Python validation covers all 51 generated assets and all `sounds.json`/language references. Every
asset must be mono Vorbis at 44.1 kHz, finite, free of clipped samples, and at or below 0.707 peak.
Effective RMS must decrease near to mid to far after policy gain. Far spectral centroid must be at
least 20% below near. Reduced Celestial high-band energy from 4–12 kHz must be at least 70% below its
ordinary peer while retaining measurable low-mid warning energy.

A production client evidence run exercises all 16 cues at near, mid, and far distances, a solid-wall
case for every cue, a concurrent burst, subtitles, resource reload, reconnect, dimension change, and
ordinary/reduced Celestial comparison. The client writes bounded semantic audit rows naming the
chosen layer, measured distance, obstruction decision, gain, admission result, subtitle key, and
comfort state. Subtitle screenshots and deterministic waveform/spectrogram summaries accompany the
audit; no microphone recording is represented as source-faithful proof. The package is bound to the
exact implementation SHA, checksum-verified, privacy-scanned, independently reviewed, and accepted
only after the literal Java 25 full `check --rerun-tasks` gate passes on the finalized head.

## Acceptance

VFX-007 is complete only when all 16 production cues use the layered path; 51 generated assets and
16 subtitles validate; near/mid/far loudness is monotonic; obstruction is visibly recorded as a
softer/farther mix; burst policy remains inside its admission and headroom bounds; reduced Celestial
materially removes high-band tinnitus content; all automated and production-client evidence is
green; independent review returns READY; the branch is fast-forwarded to `main`; the literal full
gate passes again on merged `main`; `origin/main` matches; and every POWERS worktree is clean.
