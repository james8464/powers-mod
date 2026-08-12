package com.powers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Its own creative inventory page holding every item and block POWERS adds. */
public final class PowersCreativeTab {
	public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(
			BuiltInRegistries.CREATIVE_MODE_TAB.key(), PowersMod.id("powers"));
	public static final CreativeModeTab TAB = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			KEY,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.powers"))
					.icon(() -> new ItemStack(PowersItems.RAINBOW_CRYSTAL))
					.displayItems((parameters, output) -> {
						output.accept(PowersItems.RAINBOW_CRYSTAL);
						PowersItems.colorCrystals().values().forEach(output::accept);
						output.accept(PowersItems.LIGHT_CRYSTAL);
						output.accept(PowersItems.DARK_CRYSTAL);
						output.accept(PowersItems.DARKNESS_CREATURE_SPAWN_EGG);
						output.accept(PowersItems.POWER_TEST_ACTOR_SPAWN_EGG);
						output.accept(PowersItems.RADIANT_SENTINEL_SPAWN_EGG);
						output.accept(PowersItems.FIRST_VESSEL_SPAWN_EGG);
						output.accept(PowersItems.DARK_HERALD_SPAWN_EGG);
						output.accept(PowersItems.LIGHT_HERALD_SPAWN_EGG);
						output.accept(PowersBlocks.DARKNESS);
						output.accept(PowersBlocks.PURE_LIGHT);
						output.accept(PowersBlocks.AMETHYST_WARD);
						output.accept(PowersBlocks.ARCANE_CRUCIBLE);
						PowersWeapons.weapons().values().forEach(output::accept);
						ImportedPackItems.items().entrySet().stream()
								.filter(entry -> !ImportedItemRules.isHiddenCompatibilityItem(entry.getKey()))
								.map(java.util.Map.Entry::getValue).forEach(output::accept);
					})
					.build());

	private PowersCreativeTab() {
	}

	public static void initialize() {
		// the tab can only list items after they exist, so this runs after every registry
	}
}
