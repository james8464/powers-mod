# Playtest finalisation implementation plan

> This plan converts the 2026-08-09 hands-on playtest notes into testable release gates. Work is checkpointed on `main`; no phase is considered complete from compilation alone.

## 1. Presentation and feedback

- Hide entity particles for every mod-authored status-effect instance while retaining useful HUD icons.
- Route routine casts, toggles, cooldowns, selection changes, and failures to concise actionbar text; reserve chat for rare lore, consent, progression, danger, and irreversible events.
- Reduce near-camera density for Lightning Strike and Fireball while preserving distant silhouettes and semantic counterplay cues.
- Redesign the energy HUD around vanilla heart/armour/hunger geometry, including extra-heart row avoidance, darkness/amethyst/empty modes, and scalable resolution-safe anchors.
- Replace the three bottom-right rectangles with a compact vertical keyed power rail inspired by the supplied reference, using original POWERS art rather than copied Viltrumite assets.
- Add stronger server-wide Time Stop presentation and visually verify all GUI/effect assets.

## 2. Power mechanics

- Set Lightning Strike's base cooldown to zero while retaining its one-active-tribunal and payment protections.
- Promote Size Morphing to a selectable-scale player power; make deviation from normal size drive ongoing energy cost.
- Remove Slow World from the assignable roster.
- Replace Elemental Blast auto-cycling with explicit element selection.
- Make rank affect every innate power through bounded potency, duration, range, efficiency, capacity, or authored rank variants; never scale spells or crystals.
- Upgrade Time Stop to a real server-wide tick freeze with a wall-clock-safe release path, caster lifecycle recovery, multiplayer ownership rules, and rich cross-dimensional cues.
- Expand suitable player-only targeting to living mobs, including unique-name Remote Viewing and mob Vessel Possession.
- Add Thunderclap, improve momentum-based Flight, and introduce meaningful light-only and darkness-only innate powers.

## 3. Grimoires and catastrophic containment

- Exercise every spell effect, channel, payment, target, counterplay, cooldown, and interruption path; replace unclear or nonfunctional behavior.
- Add Celestial Annihilation: a persistent one-minute server-owned rite, 100-block-diameter pulsing sky beam, forced chunk ticket, safe restart persistence, evacuation warning, and a final blast at least twenty times the ordinary Light/Dark clash scale.
- The blast must eliminate runaway Darkness/Pure Light within its authored radius, remain bounded per tick, work after the caster leaves/unloads the area, and expose explicit server policy for unrelated terrain destruction.

## 4. Realms and travel

- Render the Light Realm sky pure white and keep both mindscapes visually coherent.
- Make memory landmarks functional, labelled, and fully documented.
- Enforce realm departure gates server-side: Dark Realm requires the darkness tag and Darkness level 5; Light Realm requires level 5 in either ladder.
- Repair Light/Dark crystal travel and preserve detached-body vulnerability.
- Let Teleport safely request bounded destination generation/tickets beyond client render distance; widen full dimension-identifier buttons.

## 5. Progression and advancements

- Show exactly one advancement track according to current affinity, with the correct custom background and working name prefix/focus title.
- Replace weak or incoherent quests with escalating, auditable challenges. Darkness progression should demand increasingly cruel choices, but avoid impossible vanilla predicates and keep server administrators able to tune totals.
- Give rune tiers distinct energy values and add natural loot/crafting acquisition without adding recipes for deliberately unfinished crystals.

## 6. Test actors, Darkness creatures, and Shadow Sword

- Add a controllable player-simulation test actor with player-shaped rendering, configurable attributes, movement, targetability, and power reactions.
- Add naturally spawning, completely black player-shaped Darkness creatures in the Dark Realm. They attack non-darkness living entities and use the same bounded Fireball and Lightning contracts as players.
- Rename Lycanbane to the bold dark-grey Shadow Sword. Non-darkness carriers suffer blindness/wither and summon lightning-born guardians; darkness carriers gain rapid energy affinity, guardian summoning, ground corruption, and a themed menu that can invoke every registered innate/crystal action under normal validation.

## 7. Performance, documentation, and release gates

- Replace unbounded entity/block/chunk scans with indexed, hard-budgeted, lifecycle-cleared work; audit packet trust and per-player tick costs.
- Explain every innate power, crystal action, spell, rank branch, realm landmark, rune, creature, item, counterplay, control, energy cost, and configuration option in `README.md`.
- For every checkpoint: add pure/unit tests first, run focused tests, source/resource audits, generated interaction-doc verification, `clean check build`, dedicated-server six-dimension smoke, and a targeted read-only review before committing.

