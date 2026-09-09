# CRY-006 — bounded Creativity Manifestation blueprints

Status: implementation staged on an isolated branch; aggregate Java 25 gate and independent review remain required before integration.

## Implemented behavior

- Creativity Manifestation now reads an immutable, atomically published `creation_blueprints` datapack catalogue. Crouch-use while looking straight up cycles a persistent stable blueprint key without trapping the Orange Crystal's existing outer mode selection.
- Explicit blueprints accept only approved inert construction materials, at most 256 unique blocks, a 16-block span per axis, and offsets within the same bounded origin envelope.
- Vanilla blueprints accept only two reviewed small-house resource IDs. Their dimensions, block count, and material cardinality are checked at use time; command/spawner-style blocks are rejected and structure markers are stripped.
- Every target must be loaded, inside build/world-border bounds, empty, and approved by the shared protection adapters before the first write. A single denial leaves the entire blueprint untouched.
- Vanilla template blocks are copied as states only. Source entities and all block-entity NBT, including loot-table metadata, are never copied.
- The built-in catalogue retains the legacy orange chamber as the default and adds the approved plains and desert shelters.

## Evidence retained outside the repository

- RED proof: `/tmp/powers-cry006-red-20260909.log` — SHA-256 `7a3da7fe0f66b16feda5ae93c120c7d4e2a0d5ce623c92f2a6abcccbbbc4dc99`.
- Focused JUnit plus source/resource audits GREEN: `/tmp/powers-cry006-focused-green-final-20260909.log` — SHA-256 `aaeaaef7b5f3f7054b2dc13d91f7d563fbc58a617524daeb249f10dcb23de7d0`.
- Live atomic preflight, persistent selection, both approved vanilla templates, entity stripping, and loot-NBT stripping GameTest GREEN: `/tmp/powers-cry006-live-final-20260909.log` — SHA-256 `bfdc2b80cc6035532367652563dcfd29177d3075a3041a4ff552435279d3697f`.

## Remaining gates

- Run the complete unchanged Java 25 gate when this isolated commit is selected for integration.
- Obtain a fresh independent READY review; no self-approval substitutes for that review.
- Integrate only through the protected main sequencing rules.
