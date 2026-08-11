# README and improvement-backlog design

## Objective

Replace `README.md` with a repository-derived player, administrator, and developer manual that documents every implemented gameplay family without presenting proposals as shipped features. Add a separate exhaustive improvement backlog linked from the README.

## Sources of truth

The rewrite is derived, in descending authority, from production registries and policies, datapack/resource definitions, generated item/action/system catalogues, automated tests, and the latest release evidence. Legacy README wording is not treated as proof.

## README structure

1. Identity, scope, requirements, installation, controls, and first-join flow.
2. Energy, cast sources, allegiance, rank maze, quests, titles, and scaling.
3. Complete catalogues for innate powers, grimoires/spells, crystals, mythic artifacts, relic families, runestones, weapons, foods, blocks, entities, bosses, dimensions, world systems, interfaces, visuals, interactions, counterplay, consent, and lifecycle behavior.
4. Complete command/configuration reference, acquisition rules, compatibility, performance architecture, persistence/migration, testing, and troubleshooting.
5. Links to generated row-level catalogues where hundreds of individual IDs or thousands of interaction rows would make the primary manual less usable; their totals and guarantees remain stated in the README.

## Improvement backlog structure

Recommendations are grouped by correctness, performance, gameplay depth, progression, powers, spells, crystals, artifacts/items, realms/world generation, bosses/entities, Shadow, UI/accessibility, visual/audio presentation, multiplayer, compatibility/API, observability, testing, release engineering, and lore. Every entry is labelled as a guarantee, enhancement, or expansion and includes priority, rationale, and an acceptance condition. Suspected risks are not described as confirmed bugs without evidence.

## Validation

Regenerate the repository's semantic and asset audits, run documentation/resource validation and the full test suite, inspect all links and headings, then commit and push only a clean verified tree.
