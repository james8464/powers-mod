package com.powers.player;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

	@Test
	void disabledCompatibilityPathReturnsOriginalTeamOrNicknameComponent() {
		Component styledName = Component.literal("[Team] Nick").withStyle(net.minecraft.ChatFormatting.AQUA);
		assertSame(styledName, RankNameFormatter.decorate(false,
				Component.literal("[Rank] "), styledName));
	}

	@Test
	void decorationDoesNotMutateSignedMessageContent() {
		String signedContent = "hello world";
		RankNameFormatter.decorate(true, Component.literal("[Rank] "), Component.literal("Nick"));
		assertEquals("hello world", signedContent);
	}
}
