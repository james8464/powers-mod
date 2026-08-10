package com.powers.hud;

import java.util.List;

/**
 * Pure scaled-window layout shared by HUD renderers and bounds tests.
 *
 * <p>The ten-symbol energy row uses the exact footprint of the hunger row and
 * sits one vanilla row above it. Extra player hearts remain on the opposite
 * side of the hotbar, so Double Health cannot cover either resource.</p>
 */
public record HudLayout(Rect energy, List<Rect> powerSlots) {
	public static final int ENERGY_WIDTH = 81;
	public static final int ENERGY_HEIGHT = 9;
	public static final int POWER_SLOT_SIZE = 30;
	private static final int POWER_SLOT_GAP = 4;
	private static final int EDGE_MARGIN = 4;
	private static final int HUNGER_LEFT_OFFSET = 10;
	private static final int ENERGY_BOTTOM_OFFSET = 49;
	private static final int CONDITIONAL_ROW_STEP = 10;

	public HudLayout {
		powerSlots = List.copyOf(powerSlots);
	}

	/** Creates a hunger-aligned energy row and right-edge power rail. */
	public static HudLayout forScreen(int width, int height) {
		return forScreen(width, height, 0, 0);
	}

	/**
	 * Creates the layout while reserving vanilla air and mount-health rows on
	 * the hunger side. Values are row counts, not raw icon counts.
	 */
	public static HudLayout forScreen(int width, int height, int airRows, int mountRows) {
		int safeWidth = Math.max(1, width);
		int safeHeight = Math.max(1, height);
		int energyWidth = Math.min(ENERGY_WIDTH, safeWidth);
		int energyHeight = Math.min(ENERGY_HEIGHT, safeHeight);
		int hungerX = safeWidth / 2 + HUNGER_LEFT_OFFSET;
		int energyX = Math.max(0, Math.min(hungerX, safeWidth - energyWidth));
		int conditionalRows = Math.clamp(Math.max(0, airRows) + Math.max(0, mountRows), 0, 4);
		int energyY = Math.max(0, safeHeight - ENERGY_BOTTOM_OFFSET
				- conditionalRows * CONDITIONAL_ROW_STEP);
		Rect energy = new Rect(energyX, energyY,
				energyWidth, energyHeight);

		int slotSize = Math.min(POWER_SLOT_SIZE, Math.min(safeWidth, safeHeight));
		int railHeight = slotSize * 3 + POWER_SLOT_GAP * 2;
		int railX = Math.max(0, safeWidth - EDGE_MARGIN - slotSize);
		int railTop = Math.max(0, safeHeight - 26 - railHeight);
		return new HudLayout(energy, List.of(
				new Rect(railX, railTop, slotSize, slotSize),
				new Rect(railX, railTop + slotSize + POWER_SLOT_GAP, slotSize, slotSize),
				new Rect(railX, railTop + (slotSize + POWER_SLOT_GAP) * 2, slotSize, slotSize)));
	}

	/** Right-to-left x coordinate for one of the ten vanilla-stride symbols. */
	public static int energySymbolX(Rect energy, int symbol) {
		int index = Math.clamp(symbol, 0, 9);
		return energy.right() - ENERGY_HEIGHT - index * 8;
	}

	/** Bounds of the vanilla nine-slot bar, useful for deterministic collision tests. */
	public static Rect vanillaHotbar(int width, int height) {
		int safeWidth = Math.max(1, width);
		int safeHeight = Math.max(1, height);
		int hotbarWidth = Math.min(182, safeWidth);
		int hotbarHeight = Math.min(22, safeHeight);
		return new Rect(Math.max(0, (safeWidth - hotbarWidth) / 2),
				Math.max(0, safeHeight - hotbarHeight), hotbarWidth, hotbarHeight);
	}

	public List<Rect> elements() {
		return java.util.stream.Stream.concat(java.util.stream.Stream.of(energy), powerSlots.stream()).toList();
	}

	public record Rect(int x, int y, int width, int height) {
		public int right() { return x + width; }
		public int bottom() { return y + height; }

		public boolean intersects(Rect other) {
			return x < other.right() && right() > other.x
					&& y < other.bottom() && bottom() > other.y;
		}
	}
}
