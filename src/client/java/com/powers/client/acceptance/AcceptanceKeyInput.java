package com.powers.client.acceptance;

import net.minecraft.client.KeyMapping;

/** Replays held and click-style vanilla key semantics for development acceptance clients. */
final class AcceptanceKeyInput {
	private AcceptanceKeyInput() {
	}

	static void apply(KeyMapping key, String name, boolean down) {
		if (down && name.equals("advancements")) {
			KeyMapping.click(key.getDefaultKey());
		}
		key.setDown(down);
	}
}
