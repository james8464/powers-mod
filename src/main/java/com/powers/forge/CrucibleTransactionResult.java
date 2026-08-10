package com.powers.forge;

import net.minecraft.world.item.ItemStack;

/** Immutable prepare result; failed plans never carry mutations. */
public record CrucibleTransactionResult(boolean success, String reason,
		ItemStack result, ItemStack weaponAfter, ItemStack catalystAfter, boolean levelUp) {
	public static CrucibleTransactionResult fail(String reason) {
		return new CrucibleTransactionResult(false, reason, ItemStack.EMPTY,
				ItemStack.EMPTY, ItemStack.EMPTY, false);
	}
}
