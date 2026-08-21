# Non-item asset audit

This historical integrity-only manifest covers every tracked POWERS namespace asset except item definitions, models, and textures. A digest or successful decode proves identity/integrity only; contact sheets are not renderer proof and carry no automatic visual verdict. JSON/reference, animation, alpha, sound, and translation contracts are enforced separately by `validate_resources.py`.

| Asset | Group | SHA-256 | Review | Evidence |
|---|---|---|---|---|
| `blockstates/amethyst_ward.json` | blockstates | `4c823ebbcfa9` | integrity | JSON decoded; references are covered by strict resource validation. |
| `blockstates/arcane_crucible.json` | blockstates | `29dec835b131` | integrity | JSON decoded; references are covered by strict resource validation. |
| `blockstates/darkness.json` | blockstates | `9a3e8bb90dd6` | integrity | JSON decoded; references are covered by strict resource validation. |
| `blockstates/pure_light.json` | blockstates | `c4375fe29f91` | integrity | JSON decoded; references are covered by strict resource validation. |
| `icon.png` | icon.png | `00ea24abeb49` | integrity | PNG 128×128, alpha; decoded for structural/pixel evidence only. |
| `lang/en_us.json` | lang | `6b83f7784d05` | integrity | JSON decoded; references are covered by strict resource validation. |
| `models/block/amethyst_ward.json` | models | `ec18f4c15589` | integrity | JSON decoded; references are covered by strict resource validation. |
| `models/block/amethyst_ward_powered.json` | models | `e82316d2c639` | integrity | JSON decoded; references are covered by strict resource validation. |
| `models/block/arcane_crucible.json` | models | `7590e3936646` | integrity | JSON decoded; references are covered by strict resource validation. |
| `models/block/arcane_crucible_active.json` | models | `5474925830ba` | integrity | JSON decoded; references are covered by strict resource validation. |
| `models/block/darkness.json` | models | `9ac7f87cddf9` | integrity | JSON decoded; references are covered by strict resource validation. |
| `models/block/pure_light.json` | models | `52939669700e` | integrity | JSON decoded; references are covered by strict resource validation. |
| `particles/eclipse.json` | particles | `e9b7e1861661` | integrity | JSON decoded; references are covered by strict resource validation. |
| `particles/fracture.json` | particles | `913af2001863` | integrity | JSON decoded; references are covered by strict resource validation. |
| `particles/glyph.json` | particles | `465b271539a2` | integrity | JSON decoded; references are covered by strict resource validation. |
| `particles/mote.json` | particles | `a2e8a3d7dae1` | integrity | JSON decoded; references are covered by strict resource validation. |
| `particles/ribbon.json` | particles | `61978b52277c` | integrity | JSON decoded; references are covered by strict resource validation. |
| `particles/root.json` | particles | `e09411967ede` | integrity | JSON decoded; references are covered by strict resource validation. |
| `particles/shard.json` | particles | `aabb8e3d848a` | integrity | JSON decoded; references are covered by strict resource validation. |
| `particles/spark.json` | particles | `ab031656cf84` | integrity | JSON decoded; references are covered by strict resource validation. |
| `sounds.json` | sounds.json | `2b1400f53c95` | integrity | JSON decoded; references are covered by strict resource validation. |
| `sounds/magic/amethyst_fracture.ogg` | sounds | `cf4b766784c8` | integrity | Ogg/Vorbis, 1 channel(s), 10547 bytes; stream decoded. |
| `sounds/magic/beam_ring.ogg` | sounds | `153bbff46127` | integrity | Ogg/Vorbis, 1 channel(s), 10618 bytes; stream decoded. |
| `sounds/magic/boss_impact_ring.ogg` | sounds | `38431cc37124` | integrity | Ogg/Vorbis, 1 channel(s), 14859 bytes; stream decoded. |
| `sounds/magic/celestial_ring.ogg` | sounds | `7d17c163117c` | integrity | Ogg/Vorbis, 1 channel(s), 29935 bytes; stream decoded. |
| `sounds/magic/crystal_resonate.ogg` | sounds | `c60c2c2d529f` | integrity | Ogg/Vorbis, 1 channel(s), 11263 bytes; stream decoded. |
| `sounds/magic/dark_whisper.ogg` | sounds | `928def201581` | integrity | Ogg/Vorbis, 1 channel(s), 14442 bytes; stream decoded. |
| `sounds/magic/interaction_clash.ogg` | sounds | `7705651a85ef` | integrity | Ogg/Vorbis, 1 channel(s), 11348 bytes; stream decoded. |
| `sounds/magic/light_chorus.ogg` | sounds | `1a3d2a9cc636` | integrity | Ogg/Vorbis, 1 channel(s), 11555 bytes; stream decoded. |
| `sounds/magic/rank_awaken.ogg` | sounds | `2ecb05ec2ac3` | integrity | Ogg/Vorbis, 1 channel(s), 12854 bytes; stream decoded. |
| `sounds/magic/rift_close.ogg` | sounds | `bd9d9a613fbe` | integrity | Ogg/Vorbis, 1 channel(s), 11390 bytes; stream decoded. |
| `sounds/magic/rift_open.ogg` | sounds | `8df0b971bbce` | integrity | Ogg/Vorbis, 1 channel(s), 13849 bytes; stream decoded. |
| `sounds/magic/rune_hum.ogg` | sounds | `118dc8302b2f` | integrity | Ogg/Vorbis, 1 channel(s), 11289 bytes; stream decoded. |
| `sounds/magic/soul_tether.ogg` | sounds | `bf4aa1de0fee` | integrity | Ogg/Vorbis, 1 channel(s), 12692 bytes; stream decoded. |
| `sounds/magic/time_release.ogg` | sounds | `2d60851b092a` | integrity | Ogg/Vorbis, 1 channel(s), 10848 bytes; stream decoded. |
| `sounds/magic/time_suspend.ogg` | sounds | `6c90a77a5ed5` | integrity | Ogg/Vorbis, 1 channel(s), 13316 bytes; stream decoded. |
| `sounds/magic/ward_impact.ogg` | sounds | `b25b0ca05e77` | integrity | Ogg/Vorbis, 1 channel(s), 9816 bytes; stream decoded. |
| `textures/block/amethyst_ward_side_off.png` | textures | `27b6fca2856e` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/block/amethyst_ward_side_on.png` | textures | `c0628c5b5ada` | integrity | PNG 16×64, alpha; decoded for structural/pixel evidence only. |
| `textures/block/amethyst_ward_side_on.png.mcmeta` | textures | `4d3bcabd8318` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/block/amethyst_ward_up_off.png` | textures | `8d70c7ba7502` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/block/amethyst_ward_up_on.png` | textures | `b76c898fcf41` | integrity | PNG 16×64, alpha; decoded for structural/pixel evidence only. |
| `textures/block/amethyst_ward_up_on.png.mcmeta` | textures | `eace6bdad2fa` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/block/darkness.png` | textures | `2e0a9a2d5f28` | integrity | PNG 16×16, opaque; decoded for structural/pixel evidence only. |
| `textures/block/device_brooding_forge_2.png` | textures | `311f0c6401b7` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/block/device_brooding_forge_side_off.png` | textures | `27b6fca2856e` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/block/device_brooding_forge_side_on.png` | textures | `c0628c5b5ada` | integrity | PNG 16×64, alpha; decoded for structural/pixel evidence only. |
| `textures/block/device_brooding_forge_side_on.png.mcmeta` | textures | `4d3bcabd8318` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/block/device_brooding_forge_up_off.png` | textures | `8d70c7ba7502` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/block/device_brooding_forge_up_on.png` | textures | `b76c898fcf41` | integrity | PNG 16×64, alpha; decoded for structural/pixel evidence only. |
| `textures/block/device_brooding_forge_up_on.png.mcmeta` | textures | `eace6bdad2fa` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/block/pure_light.png` | textures | `430c4a11827d` | integrity | PNG 16×16, opaque; decoded for structural/pixel evidence only. |
| `textures/entity/dark_herald.png` | textures | `e7622c91eed5` | integrity | PNG 64×64, alpha; decoded for structural/pixel evidence only. |
| `textures/entity/darkness_player.png` | textures | `e0c5949f45e1` | integrity | PNG 64×64, alpha; decoded for structural/pixel evidence only. |
| `textures/entity/first_vessel.png` | textures | `83245b548a10` | integrity | PNG 64×64, alpha; decoded for structural/pixel evidence only. |
| `textures/entity/light_herald.png` | textures | `d35fbe1f2294` | integrity | PNG 64×64, alpha; decoded for structural/pixel evidence only. |
| `textures/entity/radiant_sentinel.png` | textures | `2b3a8fdd3e0a` | integrity | PNG 64×64, alpha; decoded for structural/pixel evidence only. |
| `textures/entity/test_actor.png` | textures | `a36cd91fd2f8` | integrity | PNG 64×64, alpha; decoded for structural/pixel evidence only. |
| `textures/gui/advancements/backgrounds/radiant_path.png` | textures | `7d027877c130` | integrity | PNG 256×256, alpha; decoded for structural/pixel evidence only. |
| `textures/gui/advancements/backgrounds/shadow_path.png` | textures | `bdc8a02990ac` | integrity | PNG 256×256, alpha; decoded for structural/pixel evidence only. |
| `textures/gui/energy_symbols.png` | textures | `62435ce12695` | integrity | PNG 27×45, alpha; decoded for structural/pixel evidence only. |
| `textures/gui/locator_panel.png` | textures | `3255f1e09f0c` | integrity | PNG 240×224, alpha; decoded for structural/pixel evidence only. |
| `textures/gui/power_slot.png` | textures | `0126f9ebefca` | integrity | PNG 30×30, alpha; decoded for structural/pixel evidence only. |
| `textures/gui/power_slot_active.png` | textures | `69c00eb92a5f` | integrity | PNG 30×30, alpha; decoded for structural/pixel evidence only. |
| `textures/gui/rank_maze/dark_panel.png` | textures | `b12bd1b0e041` | integrity | PNG 512×256, alpha; decoded for structural/pixel evidence only. |
| `textures/gui/rank_maze/light_panel.png` | textures | `9ad24a6b8c59` | integrity | PNG 512×256, alpha; decoded for structural/pixel evidence only. |
| `textures/gui/teleport_panel.png` | textures | `ea40205aceab` | integrity | PNG 256×192, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood.png` | textures | `3bd128556e35` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_all.png` | textures | `7a533f89614c` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_east.png` | textures | `72f879a5824f` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_eastwest.png` | textures | `fc078dd7f0f8` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_noeast.png` | textures | `e5a7d812de5e` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_nonorth.png` | textures | `a87201c2cee5` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_north.png` | textures | `22fb27a03f7a` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_northeast.png` | textures | `925044227036` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_northsouth.png` | textures | `6e531b43b150` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_northwest.png` | textures | `88ee0aa0ae99` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_nosouth.png` | textures | `20a2f33c1b6f` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_nowest.png` | textures | `ec27a70052ef` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_side_0.png` | textures | `0e91c231073d` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_side_1.png` | textures | `03c7252e17e3` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_side_2.png` | textures | `35a38aa1d34b` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_side_3.png` | textures | `8b1f99ce1f86` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_south.png` | textures | `f7a9dd72a403` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_southeast.png` | textures | `20f8e1764f33` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_southwest.png` | textures | `bc84bed319cd` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_blood_west.png` | textures | `86e4d98ec280` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_0.png` | textures | `2b8a3e9f9d44` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_1.png` | textures | `62729e8f9895` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_10.png` | textures | `d6583881767b` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_11.png` | textures | `31b9c082caf4` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_12.png` | textures | `b9ee8cf92f17` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_13.png` | textures | `061c7099ca96` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_14.png` | textures | `681104ac888a` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_15.png` | textures | `ef74c94c283b` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_2.png` | textures | `970147095448` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_3.png` | textures | `53a8430122f5` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_4.png` | textures | `597e8a253cef` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_5.png` | textures | `50ec96163e86` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_6.png` | textures | `07f44819ea04` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_7.png` | textures | `29d3562b4402` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_8.png` | textures | `c7f4e42c2dc9` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/blocks/overlay_rune_9.png` | textures | `d4a7ac2f6c3d` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_blue_goo.png` | textures | `5b748518876f` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_blue_goo.png.mcmeta` | textures | `d859a74124fb` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_decaying_gel.png` | textures | `4cd251f6c032` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_decaying_gel.png.mcmeta` | textures | `d17dc69777a4` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_evil.png` | textures | `70e387625325` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_evil.png.mcmeta` | textures | `d17dc69777a4` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_gas_beige.png` | textures | `e61a6bcbbd07` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_gas_beige.png.mcmeta` | textures | `d859a74124fb` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_gas_dark.png` | textures | `3613ee2cae63` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_gas_dark.png.mcmeta` | textures | `d859a74124fb` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_green_goo.png` | textures | `9f76d5d0ba5d` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_green_goo.png.mcmeta` | textures | `eace6bdad2fa` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_molten.png` | textures | `f11da8abda6f` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_molten.png.mcmeta` | textures | `da6b19c34c62` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_molten_colorshifting.png` | textures | `6f56f7943018` | integrity | PNG 16×256, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_molten_colorshifting.png.mcmeta` | textures | `cc861f52feae` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_molten_wax.png` | textures | `12ed1f21b511` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_molten_wax.png.mcmeta` | textures | `bc96b0012ee6` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_red_mud.png` | textures | `08bf6ee7058b` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_red_mud.png.mcmeta` | textures | `bc96b0012ee6` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_roiling_plasma.png` | textures | `b30bf89ffed2` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_roiling_plasma.png.mcmeta` | textures | `bc96b0012ee6` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_bluish.png` | textures | `8f7f67a66d19` | integrity | PNG 16×256, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_strange_bluish.png.mcmeta` | textures | `1f1aa9014126` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_fiery.png` | textures | `dd620cab3e58` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_strange_fiery.png.mcmeta` | textures | `d859a74124fb` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_gray.png` | textures | `26e868bfc01f` | integrity | PNG 16×256, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_strange_gray.png.mcmeta` | textures | `1f1aa9014126` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_greenish.png` | textures | `cae6b2f4c513` | integrity | PNG 16×256, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_strange_greenish.png.mcmeta` | textures | `1f1aa9014126` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_mixture.png` | textures | `37020a7941c6` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_strange_mixture.png.mcmeta` | textures | `f40611ccb615` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_pale.png` | textures | `209a7f5406e6` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_strange_pale.png.mcmeta` | textures | `d859a74124fb` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_pale_2.png` | textures | `986c91b5c388` | integrity | PNG 16×480, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_strange_pale_2.png.mcmeta` | textures | `d859a74124fb` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_reddish.png` | textures | `c2228445f3bd` | integrity | PNG 16×256, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/fluids/fluid_strange_reddish.png.mcmeta` | textures | `1f1aa9014126` | integrity | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/gui/hud_icons_abyssal.png` | textures | `cbb8efc9e97c` | integrity | PNG 144×9, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/gui/hud_icons_celestial.png` | textures | `31dd34a9bd8b` | integrity | PNG 144×9, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/gui/hud_icons_deep.png` | textures | `13718c66633c` | integrity | PNG 144×9, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/gui/hud_icons_infernal.png` | textures | `44ece5016b08` | integrity | PNG 144×9, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/gui/hud_icons_unknown.png` | textures | `5b003c894486` | integrity | PNG 144×9, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/gui/hud_icons_wild.png` | textures | `41d54ff79832` | integrity | PNG 144×9, alpha; decoded for structural/pixel evidence only. |
| `textures/imported/gui/hud_potions.png` | textures | `94e64c6ac314` | integrity | PNG 126×18, alpha; decoded for structural/pixel evidence only. |
| `textures/mob_effect/amethyst_poisoning.png` | textures | `a4563dff8845` | integrity | PNG 18×18, alpha; decoded for structural/pixel evidence only. |
| `textures/mob_effect/exhaustion.png` | textures | `6c9d2a266fff` | integrity | PNG 18×18, alpha; decoded for structural/pixel evidence only. |
| `textures/particle/eclipse.png` | textures | `faf6c2a192ce` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/particle/fracture.png` | textures | `ab6fc3ba35ba` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/particle/glyph.png` | textures | `5fbe65c24fd2` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/particle/mote.png` | textures | `17a1b5af67ad` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/particle/ribbon.png` | textures | `9fdc931e942a` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/particle/root.png` | textures | `4f9b7b298446` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/particle/shard.png` | textures | `dac778faad4c` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
| `textures/particle/spark.png` | textures | `d311c0129375` | integrity | PNG 16×16, alpha; decoded for structural/pixel evidence only. |
