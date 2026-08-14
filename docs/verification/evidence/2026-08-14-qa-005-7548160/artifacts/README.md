# Artifact live acceptance — `7548160`

This bundle is exact-build QA-005 evidence for all 56 actions advertised by
`ArtifactActionCatalogue`. Each lane used a real Fabric 26.2 client connected to a
dedicated Fabric server. The scripts use the authenticated `CommitPayload` route;
commands only prepare fixtures and query authoritative state. `client.log` records
every connected tick, payload, NBT/attribute query, and final zero-residue diagnostic.

## Lanes

| Directory | Coverage | Objective proof |
| --- | --- | --- |
| `dark-routed-innates` | All 23 Shadow Sword innate routes | Target health/motion, hidden effects, attributes, tick lease, body proxies, and cleanup |
| `targeted-routed-innates` | Breezy Bash, Gravity, Possession, Plant, and Speed Burst isolation | `+1.45` lift/`-2.5` slam, `+0.830769` repel, spectator + one proxy/ticket, crop-age predicate, and `+1.2012 Z` impulse |
| `dark-crystals-uniques` | All 11 crystal routes and exactly three sword-exclusive actions | Realm/dimension state, target state, structures, summons, terrain, apotheosis, and cleanup |
| `dark-creation-uniques` | Corrected Creativity and Nightfall isolation | Creation light/frame predicates, particle-free rank-10 buffs, hostile target death, and toggle cleanup |
| `blight-ground` | Corrected Blight Ground isolation | Centre and radius-edge `powers:darkness` predicates after the bounded terrain queue settles |
| `realm-return` | Light Crystal enter, command return, re-enter, artifact return | Both returns restore the Overworld and clear proxy/ticket state |
| `light-partisan` | All eight innate routes, three crystal routes, and eight dominions | Damage/motion, attributes, summons, terrain, fields, lethal interception, realms, and cleanup |
| `light-partisan-combat` | Artifact persistence during Flight and Energy Beam plus Lightning | Real survival motion without creative flags and measured actor health loss |

Every screenshot directory contains one automatic connection capture followed by the
named `SCREENSHOT` operations in its sibling `scenario.tsv`, in chronological filename
order. This pairing preserves the original pixels while making each capture's semantic
label reproducible. The acceptance ledger points at the exact lane log; each row's note
states its measured assertion and its rendered frame is resolved by the scenario order.

## Isolation correction

The first broad pass sampled Breezy Bash after landing and used daylight-vulnerable
zombies in an isolation probe. The server log proved those fixtures burned before the
assertion. The accepted `targeted-routed-innates` lane uses sunlight-immune husks,
samples both launch and slam phases, isolates long-lived fields by removing the weapon,
and aims the plant ray at the crop rather than its supporting block. No production
change was made from invalid fixture evidence.

The initial Creativity predicate assumed a horizontal ray coordinate instead of the
block actually selected by the production raycast. `dark-creation-uniques` reproduces
the proven straight-down chamber placement and records both glowstone and orange-frame
predicates. The initial Blight fixture teleported before constructing its support, so
gravity moved the caster below the asserted Y level while the platform was being built.
`blight-ground` constructs the platform first and proves both centre and edge conversion.
The accepted Nightfall target is an unaligned husk rather than a Darkness-capable test
actor, proving the hostile pressure kill independently from the hidden apotheosis buffs.

Final diagnostics for every lane report zero remaining proxies, travel loads, forced
chunks, artifact fields, physical rays, and leaked Shadow state.
