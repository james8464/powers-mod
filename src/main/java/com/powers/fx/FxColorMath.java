package com.powers.fx;

/** Pure colour calculations shared by magical particle renderers. */
public final class FxColorMath {
	private FxColorMath() {
	}

	/** Returns an RGB colour from the repeating six-segment rainbow wheel. */
	public static int rainbow(int tick, int step) {
		float hue = (float) Math.floorMod((long) tick * step, 360L) / 60.0f;
		float x = 1.0f - Math.abs(hue % 2.0f - 1.0f);
		float r;
		float g;
		float b;
		if (hue < 1) {
			r = 1.0f;
			g = x;
			b = 0.0f;
		} else if (hue < 2) {
			r = x;
			g = 1.0f;
			b = 0.0f;
		} else if (hue < 3) {
			r = 0.0f;
			g = 1.0f;
			b = x;
		} else if (hue < 4) {
			r = 0.0f;
			g = x;
			b = 1.0f;
		} else if (hue < 5) {
			r = x;
			g = 0.0f;
			b = 1.0f;
		} else {
			r = 1.0f;
			g = 0.0f;
			b = x;
		}
		return ((int) (r * 255.0f) << 16) | ((int) (g * 255.0f) << 8) | (int) (b * 255.0f);
	}
}
