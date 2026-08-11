# Save migration notes — 1.0.2

Back up an existing world before first launch. POWERS migrations preserve valid progression and deliberately avoid manufacturing recipe-gated story items.

- Retired innate selections—Cozy Campfire, Frost Nova, Elemental Blast, Ground Slam, and Shadow Step—reconcile deterministically to valid, distinct, allegiance-compatible powers. Earned Light/Dark rank depth is preserved.
- Legacy Shadow Sword summon aliases become `unique/call_hollowed`; spread aliases become `unique/blight_ground`; destructive retired rites become Nightfall Dominion only at Darkness rank 10 and otherwise Call the Hollowed. Invalid or duplicate favourite slots are reconciled to the stable eight-entry combat loadout.
- The old Infected Rainbow Crystal registry item remains an inert, hidden missing-save alias. Darkness now changes the ordinary Rainbow Crystal's held appearance without mutating the stack.
- The removed Green Space-Time and obsolete Portal Rift actions do not survive as selectable abilities. Green retains Life Bloom; Indigo owns the stable Middleworld journey.
- Existing Miniportals without charge data initialize safely within the new zero-to-two charge component contract. Rebinding stores exact dimension and coordinates; a dropped amethyst shard restores both charges.
- Configuration schema v1 rewrites atomically to v2. Only the obsolete generated `allowTerrainDamage: false` default becomes `true`; an administrator's explicit v2 opt-out remains unchanged.
- Realm-return and body states are revalidated at use. Corrupt/missing destinations use bounded recovery and operator diagnostics; ordinary players cannot use migration as a Light/Dark departure bypass.

After migration, run `/powers diagnose` and `/powers testing reset`. If a player is trapped by corrupt historic realm data, an operator may use the explicit administrative travel/recovery route.
