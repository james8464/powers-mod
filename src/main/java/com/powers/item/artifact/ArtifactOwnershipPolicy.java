package com.powers.item.artifact;

import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Fail-closed mythic ownership scan over direct, exposed player-inventory slots only. */
public final class ArtifactOwnershipPolicy {
	private ArtifactOwnershipPolicy() {
	}

	/**
	 * Accepts direct entries exposed by the player inventory, including mod-added
	 * top-level slots. It intentionally never asks carrier items, data components,
	 * or foreign container APIs for nested contents.
	 */
	public static boolean carriesTopLevel(Container inventory, Predicate<ItemStack> matches) {
		if (inventory == null || matches == null) return false;
		return anyDirect(inventory.getContainerSize(), inventory::getItem,
				stack -> stack != null && !stack.isEmpty() && matches.test(stack));
	}

	static <T> boolean anyDirect(int size, IntFunction<T> entryAt, Predicate<T> matches) {
		if (size <= 0 || entryAt == null || matches == null) return false;
		for (int slot = 0; slot < size; slot++) {
			T entry = entryAt.apply(slot);
			if (entry != null && matches.test(entry)) return true;
		}
		return false;
	}
}
