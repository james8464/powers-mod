# Rank-Responsive Cast Presence Design

**Date:** 2026-08-08

**Target:** Fabric, Minecraft Java Edition 26.2, Java 25

**Status:** Approved through the user's standing authorization to make autonomous design decisions

## Purpose

Make progression visible whenever a player casts. A newly awakened player and an ancient title-holder must retain the same power identity while differing clearly in magical presence.

## Approaches

- Rank-specific packet layers offer more geometry but expand protocol and budget splitting.
- Per-power rank assets offer maximum variation but multiply the asset matrix.
- **Bounded signature amplification — selected:** existing semantic intensity already controls geometry radius, particle budget, velocity, volume, and pitch. Rank safely feeds that one tested field with no packet or asset changes.

## Rules

`MagicCastPresentation.forAction` gains a mastery-aware overload accepting legacy depth and unlocked variants. Depth 0–3 adds no intensity, 4–7 adds one, and 8–10 adds two. The `ancient_mastery` title variant adds one more. Inputs clamp to the supported 0–10 depth, and final intensity remains within 1–5.

The server derives depth and variants from the existing authoritative progression services only after a successful cast. Failed casts remain silent. Base origin/delivery/potency intensity remains the floor, so crystals and major rituals still feel intrinsically stronger; rank can never bypass particle, sound, distance, or reduced-motion bounds.

## Verification

Pure tests cover thresholds, ancient mastery, invalid-depth clamping, all catalogue actions, and hard intensity bounds. The release gate remains Java audit, `clean check build`, and an isolated dedicated-server smoke test.
