package com.powers.hud;

import java.util.List;

/** Pure scaled-window layout shared by HUD renderers and bounds tests. */
public record HudLayout(Rect energy, List<Rect> powerSlots) {
	public HudLayout {
		powerSlots = List.copyOf(powerSlots);
	}

	public static HudLayout forScreen(int width, int height) {
		int safeWidth = Math.max(1, width);
		int safeHeight = Math.max(1, height);
		Rect energy = new Rect(Math.max(0, (safeWidth - 172) / 2), Math.max(0, safeHeight - 51),
				Math.min(172, safeWidth), Math.min(22, safeHeight));
		int slotWidth = Math.min(36, safeWidth);
		int x = Math.max(0, safeWidth - 42);
		return new HudLayout(energy, List.of(
				new Rect(x, Math.max(0, safeHeight - 60), slotWidth, Math.min(36, safeHeight)),
				new Rect(x, Math.max(0, safeHeight - 100), slotWidth, Math.min(36, safeHeight)),
				new Rect(x, Math.max(0, safeHeight - 140), slotWidth, Math.min(36, safeHeight))));
	}

	public List<Rect> elements() {
		return java.util.stream.Stream.concat(java.util.stream.Stream.of(energy), powerSlots.stream()).toList();
	}

	public record Rect(int x, int y, int width, int height) {
		public int right() { return x + width; }
		public int bottom() { return y + height; }
	}
}
