# Arcane Crucible Design

**Date:** 2026-08-10  
**Status:** Approved

## Player Experience

The **Arcane Crucible** is a two-input block using the existing brooding-forge visual family, upgraded with authored active/off states and ancient runes. It accepts a base weapon in the left slot and one catalyst in the right. The centre panel previews valid transformations; the player chooses one and presses **Transmute**. No ordinary output slot exists, preventing shift-click and automation duplication races.

The server owns both inventories, builds the valid-choice list, revalidates it when the button is pressed, atomically consumes inputs, and inserts or drops exactly one result. Closing, disconnecting, breaking the block, hopper access, or concurrent viewers can never duplicate items.

## Block and Menu

- Block entity with two single-stack input slots and a version counter.
- Screen handler synchronizes compact choice IDs, catalyst progress, weapon XP/level, lightning unlock, and error reason.
- Screen uses the existing rune/forge palette and vanilla nine-slice/button/widget conventions.
- Comparator output reports a filled-slot/progress signal.
- Hoppers may insert into compatible inputs but cannot extract while an operation is being committed.
- Only one mutation lock exists per block entity; all transactions execute on the server thread.
- Breaking the block drops stored inputs using vanilla container semantics.

## Forgeability and Compatibility

Eligibility is data-driven through `#powers:arcane_crucible_base_weapons`. Built-in entries include vanilla swords, axes, bows, crossbows, and non-mythic weapons from `PowersWeapons`. Other mods can add their weapons with a datapack.

Hard exclusions override tags:

- Shadow Sword;
- Heavenly Partisan;
- items declaring the mythic-artifact component;
- stacks already prohibited by a compatibility callback;
- non-damageable or non-weapon stacks unless explicitly registered by an API extension.

The transmutation preserves safe vanilla and third-party data components, enchantments, custom name, lore, repair cost, and current damage ratio. It replaces only the item identity and the Crucible-owned component. If a target cannot preserve a required component, the choice is not offered.

## Catalyst Transactions

### Alignment conversion

- Darkness block + base weapon: choose any registered darkness-themed peer from the non-mythic weapon catalogue.
- Pure-light block + base weapon: choose any registered light-themed peer.
- Consumes one base and one block only after the result passes capacity and component-preservation checks.
- A converted weapon receives `CrucibleWeaponData(lineageId, alignment, starBound=false, xp=0, level=0)`.

### Animated-star binding

- Animated Artifact Star + a converted eligible weapon: consumes one star and binds lightning.
- The operation preserves item identity and sets `starBound=true`.
- Rebinding is rejected without consuming either stack.
- Star-bound lightning has no cooldown but costs normal/darkness energy according to alignment.

### Rune infusion

- A registered runestone + star-bound weapon: consumes one rune and awards configured weapon XP.
- Runes are naturally obtainable through documented loot/world content and craftable through ordinary materials. This does not create recipes for crystals or story artifacts.
- Rune XP defaults: common 25, uncommon 75, rare 225, ancient 675. Rune tags/data map determine the tier.
- Non-star-bound weapons reject rune infusion because the star is the XP conduit.

## Persistent Weapon Data

Use a registered, codec-backed data component rather than opaque custom NBT:

```text
CrucibleWeaponData {
  schemaVersion: 1,
  lineageId: ResourceLocation,
  alignment: DARKNESS | LIGHT,
  starBound: boolean,
  xp: long,
  level: int
}
```

Deserialization validates the identifier, clamps XP to `[0, Long.MAX_VALUE]`, recomputes level from XP, and rejects unknown alignment values. Packets never accept this data from a client.

## XP and Lightning Scaling

Level thresholds are exponential and overflow-safe:

```text
XP required to reach level L = min(Long.MAX_VALUE, 100 * 2^(L - 1)), L >= 1
```

Level is capped at 30. Excess XP remains saturated but cannot wrap. Lightning damage is intentionally boss-capable:

```text
base damage = 18
level bonus = 5 * level + floor(level^2 / 3)
alignment bonus = 12 when striking the opposed faction
final non-player damage cap = 1200
final player damage cap = 120
energy cost = 12 + ceil(level / 4)
```

Rank does not scale this forge-granted ability because it is an item power. It has no cooldown at any level. Energy reservation, line of sight, range, PvP policy, safe zone, amethyst, and per-tick cast rate still apply. Repeated input packets cannot cause multiple casts in one server tick.

## Visual and Audio Contract

- Idle: off forge texture, no particles.
- Valid recipe: slow contained rune orbit and quiet hum.
- Conversion: two-second `RITUAL` sequence; alignment light descends, weapon silhouette forms, one server transaction occurs at release.
- Star binding: a contained lightning strike and five-point star seal, with foreground particles culled near the camera.
- Rune infusion: rune colour travels into the weapon; level-up receives a stronger ring and sound.
- Weapon lightning: `MINIMAL`; the bolt and thunder are the presentation, with no duplicate generic cast cloud.

## Failure and Recovery

Every failure returns a concise action-bar message and leaves inputs untouched. Invalid/corrupt Crucible data disables the choice and logs a rate-limited diagnostic. Data pack reload rebuilds recipes safely. An operation interrupted before commit has no effect; after atomic commit it is complete and cannot be replayed.

## Acceptance Tests

- atomic convert/bind/infuse success and every invalid input combination;
- close, disconnect, block break, hopper, concurrent-viewer, full-inventory, and stale-choice races;
- component/enchantment/name/damage-ratio preservation;
- hard mythic exclusions even when incorrectly tagged;
- XP thresholds at levels 0, 1, 29, 30 and overflow boundaries;
- lightning energy, zero cooldown, damage caps, faction bonus, protection and packet-rate validation;
- model/texture/blockstate/menu/translation/recipe/tag validators and visual smoke.

