# Non-item asset audit

This exhaustive manifest covers every tracked POWERS namespace asset except new-item definitions, models, and textures, which the requested pass explicitly excludes. A digest proves file identity only. PNGs are decoded into contact sheets for visual review; JSON/reference, animation, alpha, sound, and translation contracts are enforced separately by `validate_resources.py`.

| Asset | Group | SHA-256 | Review | Evidence |
|---|---|---|---|---|
| `blockstates/amethyst_ward.json` | blockstates | `4c823ebbcfa9` | pass | JSON decoded; references are covered by strict resource validation. |
| `blockstates/arcane_crucible.json` | blockstates | `29dec835b131` | pass | JSON decoded; references are covered by strict resource validation. |
| `blockstates/dark_memory_obelisk.json` | blockstates | `23f6c99a807e` | pass | JSON decoded; references are covered by strict resource validation. |
| `blockstates/darkness.json` | blockstates | `9a3e8bb90dd6` | pass | JSON decoded; references are covered by strict resource validation. |
| `blockstates/light_memory_obelisk.json` | blockstates | `62981c2f148e` | pass | JSON decoded; references are covered by strict resource validation. |
| `blockstates/pure_light.json` | blockstates | `c4375fe29f91` | pass | JSON decoded; references are covered by strict resource validation. |
| `icon.png` | icon.png | `00ea24abeb49` | pass | PNG 128×128, alpha; reviewed in contact sheet. |
| `lang/en_us.json` | lang | `229e0a30da61` | pass | JSON decoded; references are covered by strict resource validation. |
| `models/block/amethyst_ward.json` | models | `ec18f4c15589` | pass | JSON decoded; references are covered by strict resource validation. |
| `models/block/amethyst_ward_powered.json` | models | `e82316d2c639` | pass | JSON decoded; references are covered by strict resource validation. |
| `models/block/arcane_crucible.json` | models | `7590e3936646` | pass | JSON decoded; references are covered by strict resource validation. |
| `models/block/arcane_crucible_active.json` | models | `5474925830ba` | pass | JSON decoded; references are covered by strict resource validation. |
| `models/block/dark_memory_obelisk.json` | models | `5061ea4c6337` | pass | JSON decoded; references are covered by strict resource validation. |
| `models/block/darkness.json` | models | `9ac7f87cddf9` | pass | JSON decoded; references are covered by strict resource validation. |
| `models/block/light_memory_obelisk.json` | models | `8f76171ed38c` | pass | JSON decoded; references are covered by strict resource validation. |
| `models/block/pure_light.json` | models | `52939669700e` | pass | JSON decoded; references are covered by strict resource validation. |
| `particles/eclipse.json` | particles | `e9b7e1861661` | pass | JSON decoded; references are covered by strict resource validation. |
| `particles/fracture.json` | particles | `913af2001863` | pass | JSON decoded; references are covered by strict resource validation. |
| `particles/glyph.json` | particles | `465b271539a2` | pass | JSON decoded; references are covered by strict resource validation. |
| `particles/mote.json` | particles | `a2e8a3d7dae1` | pass | JSON decoded; references are covered by strict resource validation. |
| `particles/ribbon.json` | particles | `61978b52277c` | pass | JSON decoded; references are covered by strict resource validation. |
| `particles/root.json` | particles | `e09411967ede` | pass | JSON decoded; references are covered by strict resource validation. |
| `particles/shard.json` | particles | `aabb8e3d848a` | pass | JSON decoded; references are covered by strict resource validation. |
| `particles/spark.json` | particles | `ab031656cf84` | pass | JSON decoded; references are covered by strict resource validation. |
| `sounds.json` | sounds.json | `dae444973962` | pass | JSON decoded; references are covered by strict resource validation. |
| `sounds/magic/amethyst_fracture.ogg` | sounds | `cf4b766784c8` | pass | Ogg/Vorbis, 1 channel(s), 10547 bytes; normalized original cue. |
| `sounds/magic/celestial_ring.ogg` | sounds | `fd55e5e760c5` | pass | Ogg/Vorbis, 1 channel(s), 30025 bytes; normalized original cue. |
| `sounds/magic/crystal_resonate.ogg` | sounds | `c60c2c2d529f` | pass | Ogg/Vorbis, 1 channel(s), 11263 bytes; normalized original cue. |
| `sounds/magic/dark_whisper.ogg` | sounds | `928def201581` | pass | Ogg/Vorbis, 1 channel(s), 14442 bytes; normalized original cue. |
| `sounds/magic/interaction_clash.ogg` | sounds | `7705651a85ef` | pass | Ogg/Vorbis, 1 channel(s), 11348 bytes; normalized original cue. |
| `sounds/magic/light_chorus.ogg` | sounds | `1a3d2a9cc636` | pass | Ogg/Vorbis, 1 channel(s), 11555 bytes; normalized original cue. |
| `sounds/magic/rank_awaken.ogg` | sounds | `2ecb05ec2ac3` | pass | Ogg/Vorbis, 1 channel(s), 12854 bytes; normalized original cue. |
| `sounds/magic/rift_close.ogg` | sounds | `bd9d9a613fbe` | pass | Ogg/Vorbis, 1 channel(s), 11390 bytes; normalized original cue. |
| `sounds/magic/rift_open.ogg` | sounds | `8df0b971bbce` | pass | Ogg/Vorbis, 1 channel(s), 13849 bytes; normalized original cue. |
| `sounds/magic/rune_hum.ogg` | sounds | `118dc8302b2f` | pass | Ogg/Vorbis, 1 channel(s), 11289 bytes; normalized original cue. |
| `sounds/magic/soul_tether.ogg` | sounds | `bf4aa1de0fee` | pass | Ogg/Vorbis, 1 channel(s), 12692 bytes; normalized original cue. |
| `sounds/magic/time_release.ogg` | sounds | `2d60851b092a` | pass | Ogg/Vorbis, 1 channel(s), 10848 bytes; normalized original cue. |
| `sounds/magic/time_suspend.ogg` | sounds | `6c90a77a5ed5` | pass | Ogg/Vorbis, 1 channel(s), 13316 bytes; normalized original cue. |
| `sounds/magic/ward_impact.ogg` | sounds | `b25b0ca05e77` | pass | Ogg/Vorbis, 1 channel(s), 9816 bytes; normalized original cue. |
| `textures/block/amethyst_ward_side_off.png` | textures | `27b6fca2856e` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/block/amethyst_ward_side_on.png` | textures | `c0628c5b5ada` | pass | PNG 16×64, alpha; reviewed in contact sheet. |
| `textures/block/amethyst_ward_side_on.png.mcmeta` | textures | `4d3bcabd8318` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/block/amethyst_ward_up_off.png` | textures | `8d70c7ba7502` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/block/amethyst_ward_up_on.png` | textures | `b76c898fcf41` | pass | PNG 16×64, alpha; reviewed in contact sheet. |
| `textures/block/amethyst_ward_up_on.png.mcmeta` | textures | `eace6bdad2fa` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/block/darkness.png` | textures | `2e0a9a2d5f28` | pass | PNG 16×16, opaque; reviewed in contact sheet. |
| `textures/block/device_brooding_forge_2.png` | textures | `311f0c6401b7` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/block/device_brooding_forge_side_off.png` | textures | `27b6fca2856e` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/block/device_brooding_forge_side_on.png` | textures | `c0628c5b5ada` | pass | PNG 16×64, alpha; reviewed in contact sheet. |
| `textures/block/device_brooding_forge_side_on.png.mcmeta` | textures | `4d3bcabd8318` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/block/device_brooding_forge_up_off.png` | textures | `8d70c7ba7502` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/block/device_brooding_forge_up_on.png` | textures | `b76c898fcf41` | pass | PNG 16×64, alpha; reviewed in contact sheet. |
| `textures/block/device_brooding_forge_up_on.png.mcmeta` | textures | `eace6bdad2fa` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/block/pure_light.png` | textures | `430c4a11827d` | pass | PNG 16×16, opaque; reviewed in contact sheet. |
| `textures/entity/dark_herald.png` | textures | `e7622c91eed5` | pass | PNG 64×64, alpha; reviewed in contact sheet. |
| `textures/entity/darkness_player.png` | textures | `e0c5949f45e1` | pass | PNG 64×64, alpha; reviewed in contact sheet. |
| `textures/entity/first_vessel.png` | textures | `83245b548a10` | pass | PNG 64×64, alpha; reviewed in contact sheet. |
| `textures/entity/light_herald.png` | textures | `d35fbe1f2294` | pass | PNG 64×64, alpha; reviewed in contact sheet. |
| `textures/entity/radiant_sentinel.png` | textures | `2b3a8fdd3e0a` | pass | PNG 64×64, alpha; reviewed in contact sheet. |
| `textures/entity/test_actor.png` | textures | `a36cd91fd2f8` | pass | PNG 64×64, alpha; reviewed in contact sheet. |
| `textures/gui/advancements/backgrounds/radiant_path.png` | textures | `7d027877c130` | pass | PNG 256×256, alpha; reviewed in contact sheet. |
| `textures/gui/advancements/backgrounds/shadow_path.png` | textures | `bdc8a02990ac` | pass | PNG 256×256, alpha; reviewed in contact sheet. |
| `textures/gui/energy_symbols.png` | textures | `62435ce12695` | pass | PNG 27×45, alpha; reviewed in contact sheet. |
| `textures/gui/locator_panel.png` | textures | `3255f1e09f0c` | pass | PNG 240×224, alpha; reviewed in contact sheet. |
| `textures/gui/power_slot.png` | textures | `0126f9ebefca` | pass | PNG 30×30, alpha; reviewed in contact sheet. |
| `textures/gui/power_slot_active.png` | textures | `69c00eb92a5f` | pass | PNG 30×30, alpha; reviewed in contact sheet. |
| `textures/gui/rank_maze/dark_panel.png` | textures | `b12bd1b0e041` | pass | PNG 512×256, alpha; reviewed in contact sheet. |
| `textures/gui/rank_maze/light_panel.png` | textures | `9ad24a6b8c59` | pass | PNG 512×256, alpha; reviewed in contact sheet. |
| `textures/gui/teleport_panel.png` | textures | `ea40205aceab` | pass | PNG 256×192, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood.png` | textures | `3bd128556e35` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_all.png` | textures | `7a533f89614c` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_east.png` | textures | `72f879a5824f` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_eastwest.png` | textures | `fc078dd7f0f8` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_noeast.png` | textures | `e5a7d812de5e` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_nonorth.png` | textures | `a87201c2cee5` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_north.png` | textures | `22fb27a03f7a` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_northeast.png` | textures | `925044227036` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_northsouth.png` | textures | `6e531b43b150` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_northwest.png` | textures | `88ee0aa0ae99` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_nosouth.png` | textures | `20a2f33c1b6f` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_nowest.png` | textures | `ec27a70052ef` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_side_0.png` | textures | `0e91c231073d` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_side_1.png` | textures | `03c7252e17e3` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_side_2.png` | textures | `35a38aa1d34b` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_side_3.png` | textures | `8b1f99ce1f86` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_south.png` | textures | `f7a9dd72a403` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_southeast.png` | textures | `20f8e1764f33` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_southwest.png` | textures | `bc84bed319cd` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_blood_west.png` | textures | `86e4d98ec280` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_0.png` | textures | `2b8a3e9f9d44` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_1.png` | textures | `62729e8f9895` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_10.png` | textures | `d6583881767b` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_11.png` | textures | `31b9c082caf4` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_12.png` | textures | `b9ee8cf92f17` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_13.png` | textures | `061c7099ca96` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_14.png` | textures | `681104ac888a` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_15.png` | textures | `ef74c94c283b` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_2.png` | textures | `970147095448` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_3.png` | textures | `53a8430122f5` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_4.png` | textures | `597e8a253cef` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_5.png` | textures | `50ec96163e86` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_6.png` | textures | `07f44819ea04` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_7.png` | textures | `29d3562b4402` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_8.png` | textures | `c7f4e42c2dc9` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/blocks/overlay_rune_9.png` | textures | `d4a7ac2f6c3d` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_blue_goo.png` | textures | `5b748518876f` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_blue_goo.png.mcmeta` | textures | `d859a74124fb` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_decaying_gel.png` | textures | `4cd251f6c032` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_decaying_gel.png.mcmeta` | textures | `d17dc69777a4` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_evil.png` | textures | `70e387625325` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_evil.png.mcmeta` | textures | `d17dc69777a4` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_gas_beige.png` | textures | `e61a6bcbbd07` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_gas_beige.png.mcmeta` | textures | `d859a74124fb` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_gas_dark.png` | textures | `3613ee2cae63` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_gas_dark.png.mcmeta` | textures | `d859a74124fb` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_green_goo.png` | textures | `9f76d5d0ba5d` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_green_goo.png.mcmeta` | textures | `eace6bdad2fa` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_molten.png` | textures | `f11da8abda6f` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_molten.png.mcmeta` | textures | `da6b19c34c62` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_molten_colorshifting.png` | textures | `6f56f7943018` | pass | PNG 16×256, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_molten_colorshifting.png.mcmeta` | textures | `cc861f52feae` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_molten_wax.png` | textures | `12ed1f21b511` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_molten_wax.png.mcmeta` | textures | `bc96b0012ee6` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_red_mud.png` | textures | `08bf6ee7058b` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_red_mud.png.mcmeta` | textures | `bc96b0012ee6` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_roiling_plasma.png` | textures | `b30bf89ffed2` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_roiling_plasma.png.mcmeta` | textures | `bc96b0012ee6` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_bluish.png` | textures | `8f7f67a66d19` | pass | PNG 16×256, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_strange_bluish.png.mcmeta` | textures | `1f1aa9014126` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_fiery.png` | textures | `dd620cab3e58` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_strange_fiery.png.mcmeta` | textures | `d859a74124fb` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_gray.png` | textures | `26e868bfc01f` | pass | PNG 16×256, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_strange_gray.png.mcmeta` | textures | `1f1aa9014126` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_greenish.png` | textures | `cae6b2f4c513` | pass | PNG 16×256, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_strange_greenish.png.mcmeta` | textures | `1f1aa9014126` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_mixture.png` | textures | `37020a7941c6` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_strange_mixture.png.mcmeta` | textures | `f40611ccb615` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_pale.png` | textures | `209a7f5406e6` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_strange_pale.png.mcmeta` | textures | `d859a74124fb` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_pale_2.png` | textures | `986c91b5c388` | pass | PNG 16×480, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_strange_pale_2.png.mcmeta` | textures | `d859a74124fb` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/fluids/fluid_strange_reddish.png` | textures | `c2228445f3bd` | pass | PNG 16×256, alpha; reviewed in contact sheet. |
| `textures/imported/fluids/fluid_strange_reddish.png.mcmeta` | textures | `1f1aa9014126` | pass | JSON decoded; references are covered by strict resource validation. |
| `textures/imported/gui/hud_icons_abyssal.png` | textures | `cbb8efc9e97c` | pass | PNG 144×9, alpha; reviewed in contact sheet. |
| `textures/imported/gui/hud_icons_celestial.png` | textures | `31dd34a9bd8b` | pass | PNG 144×9, alpha; reviewed in contact sheet. |
| `textures/imported/gui/hud_icons_deep.png` | textures | `13718c66633c` | pass | PNG 144×9, alpha; reviewed in contact sheet. |
| `textures/imported/gui/hud_icons_infernal.png` | textures | `44ece5016b08` | pass | PNG 144×9, alpha; reviewed in contact sheet. |
| `textures/imported/gui/hud_icons_unknown.png` | textures | `5b003c894486` | pass | PNG 144×9, alpha; reviewed in contact sheet. |
| `textures/imported/gui/hud_icons_wild.png` | textures | `41d54ff79832` | pass | PNG 144×9, alpha; reviewed in contact sheet. |
| `textures/imported/gui/hud_potions.png` | textures | `94e64c6ac314` | pass | PNG 126×18, alpha; reviewed in contact sheet. |
| `textures/mob_effect/amethyst_poisoning.png` | textures | `a4563dff8845` | pass | PNG 18×18, alpha; reviewed in contact sheet. |
| `textures/mob_effect/exhaustion.png` | textures | `6c9d2a266fff` | pass | PNG 18×18, alpha; reviewed in contact sheet. |
| `textures/particle/eclipse.png` | textures | `faf6c2a192ce` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/particle/fracture.png` | textures | `ab6fc3ba35ba` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/particle/glyph.png` | textures | `5fbe65c24fd2` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/particle/mote.png` | textures | `17a1b5af67ad` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/particle/ribbon.png` | textures | `9fdc931e942a` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/particle/root.png` | textures | `4f9b7b298446` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/particle/shard.png` | textures | `dac778faad4c` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
| `textures/particle/spark.png` | textures | `d311c0129375` | pass | PNG 16×16, alpha; reviewed in contact sheet. |
