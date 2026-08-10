package com.powers;

import com.powers.forge.ArcaneCrucibleMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Custom server-authenticated container menu registrations. */
public final class PowersMenus {
	public static final ExtendedMenuType<ArcaneCrucibleMenu, BlockPos> ARCANE_CRUCIBLE = Registry.register(
			BuiltInRegistries.MENU, PowersMod.id("arcane_crucible"),
			new ExtendedMenuType<>(ArcaneCrucibleMenu::new, BlockPos.STREAM_CODEC));

	private PowersMenus() {
	}

	public static void initialize() {
	}
}
