# Telekinesis Release Implementation Plan

1. Add failing tests for outward impulse and empty-cast resolution.
2. Implement reusable finite force-vector rules.
3. Split collision preview from reaction emission so allowed reactions commit only after successful gameplay.
4. Count moved entities and reflected projectiles; refund empty casts through the existing transaction.
5. Strengthen directional success cues and add localized failure lore.
6. Regenerate audits, run release verification and server smoke, review, and commit.
