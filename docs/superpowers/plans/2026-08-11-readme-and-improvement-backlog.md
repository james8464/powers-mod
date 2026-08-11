# README and improvement-backlog implementation plan

**Goal:** Publish a concise but complete repository-derived manual and a separate exhaustive, actionable improvement backlog.

**Architecture:** Treat production registries/resources as authoritative, generated catalogues as exhaustive row-level appendices, and verification documents as evidence. Keep the README implemented-only; keep proposals in `docs/planning/IMPROVEMENT_BACKLOG.md`.

**Stack:** Markdown, Java/Fabric registries, JSON datapack/resources, existing Python audit generators, Gradle/JUnit/GameTest validation.

---

### Task 1: Build the content inventory

Inspect power, spell, crystal, artifact, item, block, entity, dimension, progression, command, configuration, UI, interaction, performance, and test sources. Reconcile counts with generated catalogues and release evidence.

### Task 2: Rewrite `README.md`

Replace the file in one pass. Use compact tables and explicit acquisition/status notes. Include every implemented system and link to generated per-ID/per-pair appendices for exhaustive detail.

### Task 3: Create `docs/planning/IMPROVEMENT_BACKLOG.md`

Record guarantees, enhancements, and expansions by subsystem. Give each item an ID, priority, rationale, and acceptance condition; distinguish evidence-backed defects from preventive audits and ideas.

### Task 4: Verify and publish

Regenerate magic/item/source/asset documentation as required, validate resources and links, run the full check suite, review the final diff, commit the documentation subsystem, push the branch, and leave a clean synchronized worktree.
