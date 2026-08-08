# Rank Variant Consequences Design

## Goal

Give the remaining `true_sight` and `dark_resurgence` rank variants visible, server-authoritative gameplay consequences without weakening multiplayer consent, amethyst counterplay, or bounded effects.

## True Sight

- Any completed Insight-branch node unlocks True Sight through the existing rank profile.
- During the consent-gated Soul Compass ritual, True Sight pierces the Light and Dark mindscape veils without requiring the caster to be the matching path at maximum legacy rank.
- It does not bypass locator consent, reveal an unselected player, remove concealment, or expose inventory data.
- A cyan-and-gold eye-shaped rune, vertical beam, echoing chime, and target halo distinguish the successful veil piercing from an ordinary locator cast.

## Dark Resurgence

- The Darkness path's Abyss identity strengthens energy restored by nearby Darkness blocks after normal rank regeneration scaling.
- A normal pulse becomes 1.5× stronger; at or below 25% energy it becomes 2× stronger, creating an emergency retreat objective without granting invulnerability.
- Amethyst dampening still prevents the entire pulse, and capacity clamping remains owned by `PlayerPowers`.
- Emergency pulses add an eclipse rune, soul-fire sparks, a rising spiral, and a restrained probabilistic bass cue.

## Verification

Pure rules cover threshold inclusivity, invalid values, overflow safety, and True Sight gate precedence. Runtime verification covers compilation, resources, the full test suite, and isolated dedicated-server startup/shutdown.
