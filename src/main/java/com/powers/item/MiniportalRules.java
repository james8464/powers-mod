package com.powers.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/** Pure charge and destination rules for the finite-use Miniportal. */
public final class MiniportalRules {
	public static final int MAX_CHARGES = 2;

	private MiniportalRules() {
	}

	public static int charges(Integer stored) {
		return stored == null ? MAX_CHARGES : Math.clamp(stored, 0, MAX_CHARGES);
	}

	public static boolean mayTravel(int charges, boolean sameDimension) {
		return sameDimension && charges > 0;
	}

	/** Prevents dropping, replacing, or externally editing a device mid-load. */
	public static boolean mayCommit(boolean samePlayer, boolean alive,
			boolean sameOrigin, boolean ownsDevice, int reservedCharges, int currentCharges) {
		return samePlayer && alive && sameOrigin && ownsDevice
				&& reservedCharges > 0 && reservedCharges == currentCharges;
	}

	public static int afterSuccessfulTravel(int charges) {
		return Math.max(0, Math.min(MAX_CHARGES, charges) - 1);
	}

	public static int afterRecharge() {
		return MAX_CHARGES;
	}

	/** Thirteen-pixel vanilla durability-bar width, rounded to show one half as seven. */
	public static int barWidth(int charges) {
		return Math.round(13.0F * Math.clamp(charges, 0, MAX_CHARGES) / MAX_CHARGES);
	}

	public static String anchorName(String requested, String fallback) {
		String normalized = requested == null ? "" : requested.strip();
		if (normalized.isEmpty()) normalized = fallback == null ? "Anchor" : fallback.strip();
		if (normalized.isEmpty()) normalized = "Anchor";
		return normalized.substring(0, Math.min(48, normalized.length()));
	}

	public static boolean chargedModel(int charges) {
		return charges > 0;
	}

	/** Selects exactly the first bound inventory anchor for preview and travel. */
	public static TravelAnchorData firstAnchor(Iterable<TravelAnchorData> anchors) {
		if (anchors == null) return null;
		for (TravelAnchorData anchor : anchors) if (anchor != null) return anchor;
		return null;
	}

	/** Keeps the empty-model flag derived from the authoritative charge component. */
	public static void applyVisual(ItemStack stack, int charges) {
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
				List.of(), List.of(!chargedModel(charges)), List.of(), List.of()));
	}
}
