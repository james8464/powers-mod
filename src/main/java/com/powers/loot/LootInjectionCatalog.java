package com.powers.loot;

import com.powers.PowersWeapons;

import java.util.ArrayList;
import java.util.List;

/** Additive drop catalogue; none of these entries replaces a vanilla table. */
public final class LootInjectionCatalog {
	private LootInjectionCatalog() {
	}

	public static List<LootDropGroup> groups() {
		List<LootDropGroup> groups = new ArrayList<>(List.of(
				group("minecraft:chests/simple_dungeon", 0.22f, "imported_artifact_runestone_inert"),
				group("minecraft:chests/abandoned_mineshaft", 0.14f, "imported_artifact_runestone_dark_tiny"),
				group("minecraft:chests/ancient_city", 0.10f, "imported_artifact_runestone_frigid"),
				group("minecraft:chests/stronghold_library", 0.12f,
						"imported_artifact_runestone_dark_small"),
				group("minecraft:chests/bastion_treasure", 0.08f,
						"imported_artifact_runestone_dark_medium"),
				group("minecraft:chests/end_city_treasure", 0.04f,
						"imported_artifact_runestone_dark_inscribed_large"),
				group("minecraft:chests/desert_pyramid", 0.10f,
						"imported_artifact_lodestone", "imported_blood_salts_2"),
				group("minecraft:chests/jungle_temple", 0.08f,
						"imported_artifact_flute", "imported_artifact_trilobite_fossil"),
				group("minecraft:chests/woodland_mansion", 0.045f,
						"imported_artifact_philosopherstone", "imported_artifact_ritualdagger"),
				group("minecraft:chests/ruined_portal", 0.07f,
						"imported_device_miniportal", "imported_artifact_malignember"),
				group("minecraft:chests/nether_bridge", 0.06f,
						"imported_artifact_soulmatrix", "imported_magic_essence_blood_dust"),
				group("minecraft:chests/buried_treasure", 0.12f,
						"imported_magic_essence_soul_dust", "imported_magic_essence_sacred_dust",
						"imported_artifact_blackpearl"),
				group("minecraft:blocks/birch_leaves", 0.025f, "imported_food_silver_pear"),
				group("minecraft:blocks/cherry_leaves", 0.025f, "imported_food_pantao"),
				group("minecraft:blocks/jungle_leaves", 0.04f, "imported_food_fig", "imported_food_grapes",
						"imported_food_coconut_normal", "imported_food_coconut_opened", "imported_food_coconut_straw"),
				group("minecraft:blocks/melon", 0.35f, "imported_food_slice_cantaloupe", "imported_food_slice_honeydew",
						"imported_food_slice_hornedmelon", "imported_food_slice_wintermelon"),
				group("minecraft:blocks/oak_leaves", 0.025f, "imported_food_apple_green", "imported_food_apple_wormy",
						"imported_food_apple_wormy_2"),
				group("minecraft:blocks/pumpkin", 0.35f, "imported_food_slice_squash"),
				group("minecraft:entities/cod", 0.45f, "imported_food_fish_fillet_raw"),
				group("minecraft:entities/cow", 0.65f, "imported_food_slab_beef_raw"),
				group("minecraft:entities/fox", 0.25f, "imported_food_billberry", "imported_food_blackberry",
						"imported_food_blueberries", "imported_food_cranberries", "imported_food_fisherberries",
						"imported_food_mulberries", "imported_food_prickleberries", "imported_food_raspberries",
						"imported_food_strawberries", "imported_food_sunberries"),
				group("minecraft:entities/horse", 0.65f, "imported_food_slab_cheval_raw"),
				group("minecraft:entities/husk", 0.35f, "imported_food_jerky", "imported_food_slab_beef_salted",
						"imported_food_slab_cheval_salted", "imported_food_slab_pork_salted"),
				group("minecraft:entities/mooshroom", 0.65f, "imported_food_slab_mooshroom_raw"),
				group("minecraft:entities/pig", 0.65f, "imported_food_slab_pork_raw", "imported_food_bacon_raw",
						"imported_food_sausage_raw"),
				group("minecraft:entities/salmon", 0.45f, "imported_food_salmon_fillet_raw"),
				group("minecraft:entities/slime", 0.15f, "imported_food_muckroot"),
				group("minecraft:entities/villager", 0.65f, "imported_artifact_beating_heart", "imported_food_tomato",
						"imported_food_lettuce", "imported_food_cabbage", "imported_food_onion", "imported_food_garlic",
						"imported_food_leek", "imported_food_radish", "imported_food_spinach", "imported_food_pepper",
						"imported_food_beet", "imported_food_beans", "imported_food_chickpeas", "imported_food_lentils",
						"imported_food_bread_big", "imported_food_mungbean", "imported_food_uradbean",
						"imported_food_redbeans", "imported_food_sweetpod", "imported_food_stew_sweetpod")));
		groups.addAll(WeaponLootCatalog.groups(PowersWeapons.ordinaryWeaponIds()));
		return List.copyOf(groups);
	}

	private static LootDropGroup group(String table, float chance, String... items) {
		return new LootDropGroup(table, chance, 1, 1, List.of(items));
	}
}
