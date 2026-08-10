package com.powers.player;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankNameFormatterTest {
	@Test
	void prependsTheSyncedTitleWithoutFlatteningTheVanillaName() {
		Component result = RankNameFormatter.decorate(
				Component.literal("[Origin] "), Component.literal("James"));

		assertEquals("[Origin] James", result.getString());
	}

	@Test
	void emptyPrefixLeavesTheVanillaDisplayNameAlone() {
		Component name = Component.literal("James");

		assertEquals(name, RankNameFormatter.decorate(Component.empty(), name));
	}
}
