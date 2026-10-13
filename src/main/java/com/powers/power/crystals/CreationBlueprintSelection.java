package com.powers.power.crystals;

import java.util.List;

/** Stable-key selection rules that reconcile cleanly across datapack reloads. */
public final class CreationBlueprintSelection {
	private CreationBlueprintSelection() { }

	public static boolean isSelectionGesture(boolean crouching, float pitch) {
		return crouching && pitch < -70.0F;
	}

	public static String next(String selected, List<String> available) {
		if (available.isEmpty()) return "";
		int current = available.indexOf(selected);
		return current < 0 ? available.getFirst() : available.get((current + 1) % available.size());
	}
}
