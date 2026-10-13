# PWR-011 Thunderclap verification

Status: implementation, focused tests, and filtered live proof complete; exact full gate and independent review pending.

## Implemented behavior

- The existing server-authoritative forward cone retains rank-scaled damage, control, knockback, and bounded projectile deflection.
- Every authored rank keeps a positive hard terrain budget, still capped by the administrator's per-cast maximum.
- Thunderclap recognizes the data-driven `powers:thunderclap_fragile` block tag. Tagged glass and panes may break at every rank, but the tag bypasses only hardness: safe zones, claim adapters, block-entity policy, fluids, amethyst, portals, realm matter, and forbidden blocks remain authoritative.
- Five low terrain-dust bands now travel with the existing pressure rings.
- One low-volume directional echo is emitted at the forward reflection point only when bounded loaded-only probes also find a ceiling and at least one side wall. An open forward path stays silent.
- The environment probe performs at most forty bounded block samples and never requests or loads a chunk.

## Evidence

- `ThunderclapRulesTest` proves forward geometry, stable vertical-aim fallback, enclosure-gated echo, fragile-tag hardness behavior, and forbidden-block precedence.
- `CombatTerrainRulesTest` enumerates ranks 0–10 and proves every Thunderclap budget is positive and monotonic.
- The filtered Java 25 GameTest exercises all eleven ranks through the production terrain entrypoint, proves each mutation count remains within its rank budget, retains claimed glass and bedrock, and distinguishes enclosed from open echo geometry.
- The first live diagnostic exposed a non-empty generated corridor in the open-path fixture. The fixture now authors explicit air; production behavior was unchanged before the passing rerun.

| Evidence | SHA-256 |
| --- | --- |
| `/tmp/powers-pwr011-red-20260909.log` | `81e4bfa6970ae3377a297944589a7a7b4a4b5261ac8558c80d036081229ed43f` |
| `/tmp/powers-pwr011-focused-green-final2-20260909.log` | `4dabd6ee4108d1bdc2a6ec9aa87f543a46cf2e662ce100715f2905788914bd8f` |
| `/tmp/powers-pwr011-live-final2-20260909.log` | `66df56f6b4d2388903f72f162a04299ac5ba07c71995fd8100efcb1c0f2d1402` |

## Pending acceptance

Run the unchanged checkout through the exact Java 25 full gate, retain raw outputs, and obtain a fresh independent READY review before integration. This branch is not READY and must not be merged or pushed meanwhile.
