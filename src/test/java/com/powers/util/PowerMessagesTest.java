package com.powers.util;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerMessagesTest {
	@Test
	void commonFeedbackUsesOnlyTheOverlaySink() {
		Component message = Component.literal("Cast");
		List<Component> overlay = new ArrayList<>();
		List<Component> chat = new ArrayList<>();

		PowerMessages.dispatch(PowerMessages.Delivery.OVERLAY, message,
				overlay::add, chat::add);

		assertEquals(List.of(message), overlay);
		assertEquals(List.of(), chat);
	}

	@Test
	void importantFeedbackUsesOnlyTheChatSink() {
		Component message = Component.literal("Rank advanced");
		List<Component> overlay = new ArrayList<>();
		List<Component> chat = new ArrayList<>();

		PowerMessages.dispatch(PowerMessages.Delivery.CHAT, message,
				overlay::add, chat::add);

		assertEquals(List.of(), overlay);
		assertEquals(List.of(message), chat);
	}
}
