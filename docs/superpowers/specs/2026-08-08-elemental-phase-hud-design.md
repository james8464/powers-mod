# Elemental Phase HUD Design

**Date:** 2026-08-08

**Status:** Approved through the user's standing design authority

## Problem and Choice

The server persists Elemental Blast's primed phase but omits it from `PowerStatePayload`, so players cannot know which force will fire before spending energy. A post-cast chat message is too late, while client prediction diverges after failures and reconnects. The selected design synchronizes the normalized server phase and renders it procedurally in the existing medallion.

## Presentation

An Elemental Blast slot adopts the primed phase colour and appends a localized Flame, Frost, Storm, or Earth label. Four small runes inside the medallion show the complete cycle; inactive runes remain translucent in their own colours and the primed rune pulses between opaque and bright states. The existing cooldown ring, key label, layout, and universal world ceremony remain unchanged.

The payload value is server-only, participates in existing state deduplication, updates after successful phase advancement, and normalizes through `ElementalPhase` on the client. Failed casts do not advance or resync a false phase.

## Performance and Verification

The UI adds four tiny fills only when an Elemental Blast slot is visible. Pure HUD math tests cover phase colours, active/inactive alpha, pulse timing, and malformed indices. Payload, client reset, resource translations, full build, and server startup are release-gated.
