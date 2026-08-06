package com.powers;

import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import com.powers.item.GrimoireItem;
import com.powers.item.RuneItem;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registers the useful item textures imported from unused-textures-master. */
public final class ImportedPackItems {
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final String[] TEXTURES = """
		artifact_ammolite,artifact_amulet,artifact_beating_heart,artifact_blackpearl,artifact_bloodstone,artifact_bonefigurine,artifact_bound_runestone_active_1,artifact_bound_runestone_active_2,artifact_bound_runestone_active_3,artifact_bound_runestone_inert_1,artifact_bound_runestone_inert_2,artifact_bound_runestone_inert_3,artifact_bowl,artifact_bullion,artifact_coins,artifact_corroded_copper_ring,artifact_dark_bone_figurine,artifact_diamond_ring,artifact_dripping_orb_1,artifact_dripping_orb_2,artifact_emerald_ring,artifact_emperyeanjewel,artifact_flute,artifact_ghoul_heart,artifact_heart_mechanism,artifact_lodestone,artifact_malignember,artifact_oddstone,artifact_philosopherstone,artifact_plain_copper_ring,artifact_ritualdagger,artifact_runestone_back,artifact_runestone_dark_inscribed_large,artifact_runestone_dark_inscribed_medium,artifact_runestone_dark_inscribed_small,artifact_runestone_dark_inscribed_tiny,artifact_runestone_dark_large,artifact_runestone_dark_medium,artifact_runestone_dark_small,artifact_runestone_dark_tiny,artifact_runestone_frigid,artifact_runestone_inert,artifact_runestone_overlay_0,artifact_runestone_overlay_1,artifact_runestone_overlay_10,artifact_runestone_overlay_2,artifact_runestone_overlay_3,artifact_runestone_overlay_4,artifact_runestone_overlay_5,artifact_runestone_overlay_6,artifact_runestone_overlay_7,artifact_runestone_overlay_8,artifact_runestone_overlay_9,artifact_smallpot,artifact_soulmatrix,artifact_soulstone.large,artifact_soulstone.large_inert,artifact_soulstone.medium,artifact_soulstone.medium_inert,artifact_soulstone.small,artifact_soulstone.small_inert,artifact_star,artifact_star_animated,artifact_trilobite_fossil,artifact_trilobitefossil,artifact_woodheart,blood_salts_2,book_grimoire_abyssal,book_grimoire_blight,book_grimoire_celestial,book_grimoire_deep,book_grimoire_infernal,book_grimoire_recolor,book_grimoire_recolor_overlay_abyssal,book_grimoire_recolor_overlay_blight,book_grimoire_recolor_overlay_celestial,book_grimoire_recolor_overlay_deep,book_grimoire_recolor_overlay_infernal,book_grimoire_recolor_overlay_wild,book_grimoire_wild,book_page_written,book_tattered,device_miniportal,device_miniportal_active,food_apple_green,food_apple_wormy,food_apple_wormy_2,food_bacon_cooked,food_bacon_raw,food_beans,food_beet,food_billberry,food_blackberry,food_blueberries,food_bread_big,food_bryony,food_butter,food_cabbage,food_chickpeas,food_coconut_normal,food_coconut_opened,food_coconut_straw,food_cornkernels,food_cranberries,food_dough,food_fig,food_fish_fillet_cooked,food_fish_fillet_raw,food_fish_fillet_smoked,food_fisherberries,food_garlic,food_grapes,food_hardtack,food_jerky,food_leek,food_lentils,food_lettuce,food_muckroot,food_mulberries,food_mungbean,food_onion,food_pantao,food_pepper,food_pile_rice,food_prickleberries,food_radish,food_raspberries,food_redbeans,food_salmon_fillet_cooked,food_salmon_fillet_raw,food_salmon_fillet_smoked,food_sausage_cooked,food_sausage_raw,food_silver_pear,food_slab_beef_cooked,food_slab_beef_raw,food_slab_beef_salted,food_slab_cheval_cooked,food_slab_cheval_raw,food_slab_cheval_salted,food_slab_mooshroom_cooked,food_slab_mooshroom_raw,food_slab_pork_cooked,food_slab_pork_raw,food_slab_pork_salted,food_slice_cantaloupe,food_slice_eggplant,food_slice_honeydew,food_slice_hornedmelon,food_slice_squash,food_slice_wintermelon,food_spices,food_spinach,food_stew_sweetpod,food_strawberries,food_sunberries,food_sweetpod,food_tomatillos,food_tomato,food_uradbean,food_wisdomfruit,magic_essence_blood_dust,magic_essence_sacred_dust,magic_essence_soul_dust
		""".split(",");

	private ImportedPackItems() {
	}

	public static void initialize() {
		for (String rawTexture : TEXTURES) {
			String texture = rawTexture.trim();
			if (texture.isEmpty()) continue;
			String id = "imported_" + texture.replace('.', '_');
			Item.Properties properties = new Item.Properties().stacksTo(64);
			java.util.function.Function<Item.Properties, Item> factory = Item::new;
			if (texture.startsWith("food_")) {
				boolean cooked = texture.contains("cooked") || texture.contains("smoked") || texture.contains("stew");
				properties.food(new FoodProperties.Builder()
						.nutrition(cooked ? 6 : 4)
						.saturationModifier(cooked ? 0.6f : 0.3f)
						.build());
			} else if (texture.startsWith("book_grimoire")) {
				factory = props -> new GrimoireItem(props, texture);
			} else if (texture.contains("runestone") || texture.contains("rune")) {
				factory = RuneItem::new;
			}
			ITEMS.put(id, ModItemIds.register(ModItemIds.create(id), factory, properties));
		}
	}
}
