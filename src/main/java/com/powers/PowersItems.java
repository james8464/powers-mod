package com.powers;

import com.powers.item.CrystalItem;
import com.powers.item.RainbowCrystalItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registers progression crystals and exposes their stable lookup table. */
public final class PowersItems {
	private static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "ingredients"));
	private static final ResourceKey<CreativeModeTab> SPAWN_EGGS_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "spawn_eggs"));

	// all seven color crystals fuse into the rainbow crystal
	public static final ResourceKey<Item> RAINBOW_CRYSTAL_KEY = ModItemIds.create("rainbow_crystal");
	public static final Item RAINBOW_CRYSTAL = ModItemIds.register(
			RAINBOW_CRYSTAL_KEY,
			RainbowCrystalItem::new,
			crystalProperties());

	// the seven chromatic crystals, keyed by id; all seven fuse into the rainbow crystal
	public static final Map<String, Item> COLOR_CRYSTALS = new LinkedHashMap<>();
	public static final Item RED_CRYSTAL = colorCrystal("red_crystal");
	public static final Item ORANGE_CRYSTAL = colorCrystal("orange_crystal");
	public static final Item YELLOW_CRYSTAL = colorCrystal("yellow_crystal");
	public static final Item GREEN_CRYSTAL = colorCrystal("green_crystal");
	public static final Item BLUE_CRYSTAL = colorCrystal("blue_crystal");
	public static final Item INDIGO_CRYSTAL = colorCrystal("indigo_crystal");
	public static final Item VIOLET_CRYSTAL = colorCrystal("violet_crystal");

	public static final ResourceKey<Item> LIGHT_CRYSTAL_KEY = ModItemIds.create("light_crystal");
	public static final Item LIGHT_CRYSTAL = ModItemIds.register(LIGHT_CRYSTAL_KEY, CrystalItem::new, crystalProperties());
	public static final ResourceKey<Item> DARK_CRYSTAL_KEY = ModItemIds.create("dark_crystal");
	public static final Item DARK_CRYSTAL = ModItemIds.register(DARK_CRYSTAL_KEY, CrystalItem::new, crystalProperties());
	public static final ResourceKey<Item> INFECTED_RAINBOW_CRYSTAL_KEY = ModItemIds.create("infected_rainbow_crystal");
	public static final Item INFECTED_RAINBOW_CRYSTAL = ModItemIds.register(INFECTED_RAINBOW_CRYSTAL_KEY, CrystalItem::new, crystalProperties());
	public static final Item DARKNESS_CREATURE_SPAWN_EGG = spawnEgg(
			"darkness_creature_spawn_egg", PowersEntities.DARKNESS_CREATURE);
	public static final Item POWER_TEST_ACTOR_SPAWN_EGG = spawnEgg(
			"power_test_actor_spawn_egg", PowersEntities.POWER_TEST_ACTOR);
	public static final Item RADIANT_SENTINEL_SPAWN_EGG = spawnEgg(
			"radiant_sentinel_spawn_egg", PowersEntities.RADIANT_SENTINEL);
	public static final Item FIRST_VESSEL_SPAWN_EGG = spawnEgg(
			"first_vessel_spawn_egg", PowersEntities.FIRST_VESSEL);

	private static Item colorCrystal(String name) {
		Item item = ModItemIds.register(ModItemIds.create(name), CrystalItem::new, crystalProperties());
		COLOR_CRYSTALS.put(name, item);
		return item;
	}

	// crystals are fire resistant so a dropped one never burns in lava
	private static Item.Properties crystalProperties() {
		return new Item.Properties().stacksTo(1).fireResistant();
	}

	/** Registers an operator testing egg bound to one exact custom mob type. */
	private static Item spawnEgg(String name, net.minecraft.world.entity.EntityType<?> entityType) {
		return ModItemIds.register(ModItemIds.create(name), SpawnEggItem::new,
				new Item.Properties().spawnEgg(entityType));
	}

	/** True for any of the mod's crystals; these never despawn, burn, or get picked up by mobs. */
	public static boolean isCrystal(ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof CrystalItem || item instanceof RainbowCrystalItem;
	}

	private PowersItems() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(INGREDIENTS_TAB)
				.register((creativeTab) -> {
					creativeTab.accept(RAINBOW_CRYSTAL);
					for (Item crystal : COLOR_CRYSTALS.values()) {
						creativeTab.accept(crystal);
					}
					creativeTab.accept(LIGHT_CRYSTAL);
					creativeTab.accept(DARK_CRYSTAL);
					creativeTab.accept(INFECTED_RAINBOW_CRYSTAL);
				});
		CreativeModeTabEvents.modifyOutputEvent(SPAWN_EGGS_TAB)
				.register(output -> {
					output.accept(DARKNESS_CREATURE_SPAWN_EGG);
					output.accept(POWER_TEST_ACTOR_SPAWN_EGG);
					output.accept(RADIANT_SENTINEL_SPAWN_EGG);
					output.accept(FIRST_VESSEL_SPAWN_EGG);
				});
	}
}
