package com.powers.item;

import com.powers.PowersItems;
import net.minecraft.world.item.ItemStack;

/** One definition of the irreplaceable drops that mobs and commands may not remove. */
public final class ProtectedMagicDropRules {
	private ProtectedMagicDropRules() {
	}

	public static boolean isProtected(ItemStack stack) {
		return stack != null && isProtectedCategory(PowersItems.isCrystal(stack),
				stack.getItem() instanceof MythicArtifactItem);
	}

	static boolean isProtectedCategory(boolean crystal, boolean mythicArtifact) {
		return crystal || mythicArtifact;
	}
}
