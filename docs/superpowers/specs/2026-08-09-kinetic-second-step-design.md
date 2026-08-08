# Kinetic Second Step Design

## Goal

Finish Speed Burst as an authoritative kinetic dash with collision-safe movement, a readable afterimage wake, a bounded endpoint shockwave, and one genuine Motion-rank Second Step.

## Existing gap

`SpeedBurstAbility` currently adds velocity without marking the server player for a velocity update. Its `second_step` branch emits only a rune, while the existing power-quality specification promises an afterimage trail, collision-safe impact wave, and motion-rank recovery. The HUD exposes only the ordinary cooldown, so it cannot communicate a legal follow-up cast.

## Decisions

- Speed Burst remains physical movement rather than a teleport. The server derives one finite dash vector from the player's look direction at the existing base strength of 2.2, caps its vertical component to -0.35 through 0.80, samples the moved player bounding box at twelve ordered points along the first-tick path, and scales the impulse to the last collision-free fraction. A Second Step receives a bounded 1.15 multiplier.
- The server sets the player's velocity authoritatively and marks it for synchronization. Slow Falling remains the recovery safeguard; the dash also resets excessive accumulated fall distance.
- One bounded runtime trace follows each caster for at most eight ticks. It emits a cyan-white afterimage ribbon between observed positions. A predicted obstruction, actual collision, trace expiry, death, disconnect, or dimension change ends the trace exactly once.
- Trace completion emits a radius-3 kinetic shockwave at the last valid position. It deals 4 base power damage and applies at most 1.35 outward impulse, both rank-scaled. It damages only living entities allowed by harm policy, moves only entities allowed by forced-movement policy, ignores amethyst-dampened targets, and processes the twelve nearest valid living targets. It never mutates terrain.
- A player with the `second_step` variant receives one follow-up window after the first dash. It opens two server ticks after release and closes at tick 50, using `now >= opensAt && now < expiresAt`. The ordinary rank-scaled seven-second base cooldown is armed immediately and remains persistent. During the window, only Speed Burst may bypass that cooldown once; the follow-up still pays its normal energy, passes amethyst/time-freeze and collision preparation, and restarts the full cooldown from its own release.
- Follow-up state is runtime-only and owner-scoped. Disconnect, respawn, death, rank loss, dimension change, shutdown, or use consumes/clears it. Because the full cooldown remains stored from the first cast, reconnecting cannot shorten recovery.
- The normal power-state payload gains three slot-aligned reactivation tick values. Non-reactivating powers always publish zero. The Speed Burst slot publishes its remaining Second Step window so the existing deduplicated state channel remains authoritative.
- The HUD renders an available follow-up with alternating cyan/gold cooldown runes, a paired centre diamond, and the translatable `II` marker. The ordinary cooldown seconds remain hidden while this stronger legal action is available.
- First release, follow-up awakening, wake, and impact use distinct layered particles, rotating runes, and sounds through `PowerFx`, retaining the global particle budget and universal cast ceremony.

## Components and data flow

`SpeedBurstRules` owns pure finite-vector normalization, vertical caps, collision-sample selection, and Second Step window decisions. `SpeedBurstAbility` owns per-player traces and follow-up windows on the server thread. `Ability` exposes default cooldown-bypass and reactivation-query hooks so the packet handler remains ability-agnostic. `PowersPackets` consults those hooks before rejecting a cooldown and includes slot-aligned reactivation ticks in `PowerStatePayload`.

The client stores and decrements the synchronized window in `ClientPowerState`. `PowerHudRenderer` selects reactivation geometry and colour through pure `HudMath` functions. No client packet may select a variant, alter a cooldown, or invent a follow-up.

## Safety and lifecycle boundaries

- All vectors, multipliers, positions, and time calculations reject or clamp non-finite and negative values.
- Collision sampling stops at the first blocked body-volume sample rather than accepting a later sample beyond a thin wall.
- At most one trace and one follow-up window exist per player; replacement concludes the old trace before installing the new one.
- Shockwaves sort by distance and affect at most twelve valid living targets.
- Re-entry is denied before the window opens, at and after expiry, without Motion mastery, or after the one follow-up is consumed.
- State cleanup is wired into respawn, disconnect, and server-stop lifecycle paths.
- Client list values are immutable, slot-aligned, non-negative, and reset on disconnect.

## Verification

JUnit first proves vector caps, first-obstruction collision fractions, inclusive/exclusive Second Step boundaries, one-use classification, payload round trips and source alias isolation, and HUD reactivation colours. Focused tests then cover all affected pure contracts. Final gates are both repository audits, a clean `check build`, and a fresh dedicated-server startup and graceful shutdown.
