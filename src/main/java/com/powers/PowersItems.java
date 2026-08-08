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

import java.util.LinkedHashMap;
import java.util.Map;

/** Registers progression crystals and exposes their stable lookup table. */
public final class PowersItems {
	private static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "ingredients"));

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

	private static Item colorCrystal(String name) {
		Item item = ModItemIds.register(ModItemIds.create(name), CrystalItem::new, crystalProperties());
		COLOR_CRYSTALS.put(name, item);
		return item;
	}

	// crystals are fire resistant so a dropped one never burns in lava
	private static Item.Properties crystalProperties() {
		return new Item.Properties().stacksTo(1).fireResistant();
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
	}
}
