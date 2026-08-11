package com.powers.hud;

/** Sanitized client preference for HUD placement; reset preserves the vanilla default. */
public record HudPlacement(Anchor anchor, int horizontalMargin, int verticalMargin,
		int powerRailMargin) {
	public enum Anchor {
		HUNGER,
		TOP_LEFT,
		TOP_RIGHT
	}

	public HudPlacement {
		anchor = anchor == null ? Anchor.HUNGER : anchor;
		horizontalMargin = Math.clamp(horizontalMargin, -128, 128);
		verticalMargin = Math.clamp(verticalMargin, -128, 128);
		powerRailMargin = Math.clamp(powerRailMargin, 0, 128);
	}

	/** Exact legacy placement: ten symbols above hunger and a four-pixel right rail margin. */
	public static HudPlacement defaults() {
		return new HudPlacement(Anchor.HUNGER, 0, 0, 4);
	}
}
