# Development decision history

POWERS was built through a series of test-first design increments between 8 and 11 August 2026. The superseded working plans and design drafts were removed from the release tree once their implemented contracts were captured by source, tests, README, generated catalogues, and verification reports. Their exact text remains available in Git history through commit `ebac214` and its ancestors.

## Decisions retained in the implementation

- Server-authoritative casting, energy, cooldowns, travel, progression, damage, and persistent world changes.
- Vulnerable physical bodies for mindscape, projection, possession, and remote-view sessions.
- Explicit `CastContext` source/scaling policy so innate rank scaling cannot leak into crystals or spells.
- A 73-action collision kernel with deterministic pair outcomes and compact semantic effect packets.
- Ten vanilla-style energy symbols above hunger, plus a radial artifact wheel and alignment-specific rank maze panels.
- Spatial indices, rotating queues, fixed candidate caps, ticket deadlines, and diagnostic counters for multiplayer safety.
- Exactly three Shadow Sword-exclusive invocations, while routed powers retain corrupted presentation.
- Living Darkness/Pure Light spread, auras, opposed-force clashes, amethyst containment, realm factions, and herald bosses.
- Offline Shadow dialogue and lore knowledge attached to the globally revealable player-skin companion instead of the vanilla Knowledge Book.
- Save-safe hidden aliases for legacy identifiers and intentionally deferred crafting recipes.

## Where current truth lives

- Player/operator guide: `README.md`
- Item purpose and acquisition: `docs/gameplay/item-catalogue.md`
- Innate rank transformations: `docs/gameplay/innate-levels.md`
- Magic actions and collisions: `docs/interactions/`
- Exact source and asset manifests: `docs/quality/`
- Release and migration evidence: `docs/verification/`
- Current maintenance design and execution plan: `docs/superpowers/`

Git remains the authoritative archive for abandoned alternatives and intermediate implementation notes; they are not runtime or release documentation.
