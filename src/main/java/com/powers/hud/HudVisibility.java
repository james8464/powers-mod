package com.powers.hud;

/** Pure visibility policy shared by rendering and screenshot contracts. */
public final class HudVisibility {
	private HudVisibility() {
	}

	/** Energy mirrors survival hunger: it is absent when the HUD or survival bars are absent. */
	public static boolean energy(boolean hasPlayer, boolean spectator, boolean hideGui) {
		return hasPlayer && !spectator && !hideGui;
	}
}
