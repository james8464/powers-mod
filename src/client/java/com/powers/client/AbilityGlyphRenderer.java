package com.powers.client;

import com.powers.hud.AbilityGlyphStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws semantic ability sigils consistently across the HUD and artifact interfaces. */
public final class AbilityGlyphRenderer {
	private AbilityGlyphRenderer() {
	}

	public static void draw(GuiGraphicsExtractor graphics, String abilityId,
			int centerX, int centerY, int color) {
		switch (AbilityGlyphStyle.forAbility(abilityId)) {
			case EMPTY -> diamond(graphics, centerX, centerY, 2, 0xFF626874);
			case LIGHTNING -> {
				graphics.fill(centerX, centerY - 6, centerX + 2, centerY - 1, color);
				graphics.fill(centerX - 2, centerY - 1, centerX + 2, centerY + 2, color);
				graphics.fill(centerX - 2, centerY + 2, centerX, centerY + 7, color);
			}
			case FLAME -> {
				diamond(graphics, centerX, centerY + 2, 5, color);
				diamond(graphics, centerX + 1, centerY - 4, 3, color);
			}
			case AIR -> {
				for (int step = 0; step < 5; step++) {
					graphics.fill(centerX - 7 + step, centerY - 3 + step,
							centerX - 5 + step, centerY + 3 + step, color);
					graphics.fill(centerX + 5 - step, centerY - 3 + step,
							centerX + 7 - step, centerY + 3 + step, color);
				}
			}
			case TIME -> {
				graphics.fill(centerX - 5, centerY - 6, centerX + 6, centerY - 4, color);
				graphics.fill(centerX - 5, centerY + 4, centerX + 6, centerY + 6, color);
				for (int step = 0; step < 4; step++) {
					graphics.fill(centerX - 3 + step, centerY - 4 + step,
							centerX + 4 - step, centerY - 3 + step, color);
					graphics.fill(centerX - step, centerY + step,
							centerX + step + 1, centerY + step + 1, color);
				}
			}
			case FROST -> {
				graphics.fill(centerX, centerY - 7, centerX + 1, centerY + 8, color);
				graphics.fill(centerX - 7, centerY, centerX + 8, centerY + 1, color);
				for (int offset = -5; offset <= 5; offset++) {
					graphics.fill(centerX + offset, centerY + offset,
							centerX + offset + 1, centerY + offset + 1, color);
					graphics.fill(centerX + offset, centerY - offset,
							centerX + offset + 1, centerY - offset + 1, color);
				}
			}
			case SHADOW -> {
				diamond(graphics, centerX, centerY, 7, color);
				diamond(graphics, centerX + 3, centerY - 2, 6, 0xFF11131B);
			}
			case HEALING -> {
				graphics.fill(centerX - 2, centerY - 7, centerX + 3, centerY + 8, color);
				graphics.fill(centerX - 7, centerY - 2, centerX + 8, centerY + 3, color);
			}
			case SPEED -> {
				for (int arrow = -1; arrow <= 1; arrow++) {
					int y = centerY + arrow * 5;
					graphics.fill(centerX - 6, y, centerX + 5, y + 2, color);
					graphics.fill(centerX + 2, y - 2, centerX + 7, y + 4, color);
				}
			}
			case GENERIC -> {
				diamond(graphics, centerX, centerY, 6, color);
				diamond(graphics, centerX, centerY, 2, 0xFF11131B);
			}
		}
	}

	public static void diamond(GuiGraphicsExtractor graphics, int centerX, int centerY,
			int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = radius - Math.abs(dy);
			graphics.fill(centerX - half, centerY + dy,
					centerX + half + 1, centerY + dy + 1, color);
		}
	}
}
