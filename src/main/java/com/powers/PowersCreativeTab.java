package com.powers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Dedicated inventory page for every item and block registered by POWERS. */
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
						PowersItems.COLOR_CRYSTALS.values().forEach(output::accept);
						output.accept(PowersItems.LIGHT_CRYSTAL);
						output.accept(PowersItems.DARK_CRYSTAL);
						output.accept(PowersItems.REVERSE_RAINBOW_CRYSTAL);
						output.accept(PowersItems.INFECTED_RAINBOW_CRYSTAL);
						output.accept(PowersBlocks.DARKNESS);
						output.accept(PowersBlocks.PURE_LIGHT);
						output.accept(PowersBlocks.AMETHYST_WARD);
						PowersWeapons.WEAPONS.values().forEach(output::accept);
					})
					.build());

	private PowersCreativeTab() {
	}

	public static void initialize() {
		// Ensures the tab is registered after all item and block registries exist.
	}
}
