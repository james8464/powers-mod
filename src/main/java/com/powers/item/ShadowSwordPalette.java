package com.powers.item;

/** Deterministically corrupts an existing power colour into a dark sword palette. */
public final class ShadowSwordPalette {
	public record Palette(int primary, int secondary) {
	}

	private ShadowSwordPalette() {
	}

	public static Palette corrupt(int original) {
		return new Palette(blend(original, 0x240B31, 1, 6), blend(original, 0x681632, 1, 6));
	}

	private static int blend(int first, int second, int firstWeight, int secondWeight) {
		int divisor = firstWeight + secondWeight;
		int red = (((first >>> 16) & 0xFF) * firstWeight
				+ ((second >>> 16) & 0xFF) * secondWeight) / divisor;
		int green = (((first >>> 8) & 0xFF) * firstWeight
				+ ((second >>> 8) & 0xFF) * secondWeight) / divisor;
		int blue = ((first & 0xFF) * firstWeight + (second & 0xFF) * secondWeight) / divisor;
		return red << 16 | green << 8 | blue;
	}
}
