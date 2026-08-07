package com.powers.player;

import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** classifies every edible item into what the darkness touched can stomach */
public final class FoodAffinity {
	/** ordinary prepared food, foul to the darkness touched */
	public static final String NORMAL = "normal";
	/** raw, rotten or strange food, a feast for the darkness touched */
	public static final String ABNORMAL = "abnormal";
	/** simple staples everyone can enjoy */
	public static final String NEUTRAL = "neutral";

	private static final Set<String> NORMAL_FOODS = Set.of(
			// vanilla dishes
			"mushroom_stew", "cooked_porkchop", "cooked_cod", "cooked_salmon", "cooked_beef",
			"cooked_chicken", "cooked_rabbit", "rabbit_stew", "cooked_mutton", "beetroot_soup",
			// imported dishes
			"imported_food_bacon_cooked", "imported_food_fish_fillet_cooked", "imported_food_fish_fillet_smoked",
			"imported_food_salmon_fillet_cooked", "imported_food_salmon_fillet_smoked", "imported_food_sausage_cooked",
			"imported_food_slab_beef_cooked", "imported_food_slab_cheval_cooked", "imported_food_slab_mooshroom_cooked",
			"imported_food_slab_pork_cooked", "imported_food_stew_sweetpod");

	private static final Set<String> ABNORMAL_FOODS = Set.of(
			// vanilla oddities
			"rotten_flesh", "spider_eye", "pufferfish", "pufferfish_bucket", "tropical_fish",
			"tropical_fish_bucket", "cod_bucket", "salmon_bucket", "cod", "salmon",
			"poisonous_potato", "chorus_fruit", "suspicious_stew", "porkchop", "beef",
			"chicken", "rabbit", "mutton",
			// imported raw, wormy and earthy fare
			"imported_food_apple_wormy", "imported_food_apple_wormy_2", "imported_food_bacon_raw",
			"imported_food_fish_fillet_raw", "imported_food_salmon_fillet_raw", "imported_food_sausage_raw",
			"imported_food_slab_beef_raw", "imported_food_slab_cheval_raw", "imported_food_slab_mooshroom_raw",
			"imported_food_slab_pork_raw", "imported_food_slab_beef_salted", "imported_food_slab_cheval_salted",
			"imported_food_slab_pork_salted", "imported_food_muckroot", "imported_food_jerky");

	private static final Set<String> NEUTRAL_FOODS = Set.of(
			// vanilla staples
			"bread", "apple", "carrot", "potato", "baked_potato", "golden_carrot",
			"golden_apple", "enchanted_golden_apple", "melon_slice", "sweet_berries", "glow_berries",
			"cookie", "dried_kelp", "beetroot", "honey_bottle", "pumpkin_pie",
			// imported produce and humble fare
			"imported_food_apple_green", "imported_food_beans", "imported_food_beet", "imported_food_billberry",
			"imported_food_blackberry", "imported_food_blueberries", "imported_food_bread_big", "imported_food_cabbage",
			"imported_food_chickpeas", "imported_food_coconut_normal", "imported_food_coconut_opened", "imported_food_coconut_straw",
			"imported_food_cranberries", "imported_food_fig", "imported_food_fisherberries",
			"imported_food_garlic", "imported_food_grapes", "imported_food_leek",
			"imported_food_lentils", "imported_food_lettuce", "imported_food_mulberries", "imported_food_mungbean",
			"imported_food_onion", "imported_food_pantao", "imported_food_pepper", "imported_food_prickleberries", "imported_food_radish",
			"imported_food_raspberries", "imported_food_redbeans",
			"imported_food_silver_pear", "imported_food_slice_cantaloupe", "imported_food_slice_honeydew",
			"imported_food_slice_hornedmelon", "imported_food_slice_squash", "imported_food_slice_wintermelon", "imported_food_spinach",
			"imported_food_strawberries", "imported_food_sunberries", "imported_food_sweetpod",
			"imported_food_tomato", "imported_food_uradbean", "imported_food_wisdomfruit");

	private FoodAffinity() {
	}

	/** how the darkness touched experiences this item, ordinary food by default */
	public static String of(ItemStack stack) {
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		if (NEUTRAL_FOODS.contains(id)) {
			return NEUTRAL;
		}
		if (ABNORMAL_FOODS.contains(id)) {
			return ABNORMAL;
		}
		return NORMAL;
	}
}
