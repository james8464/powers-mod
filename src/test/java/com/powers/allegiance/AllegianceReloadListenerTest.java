package com.powers.allegiance;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllegianceReloadListenerTest {
	@Test
	void registeredReloadPathPublishesValidPolicyAndRetainsPriorPolicyOnFailure() {
		AllegianceRegistry registry = new AllegianceRegistry();
		assertTrue(AllegianceReloadListener.reloadDocument(registry,
				JsonParser.parseString("""
						{"same":"ally","opposed":"hostile","neutral":"neutrality",
						 "party":"ally","amethyst":"suppressed"}
						""").getAsJsonObject()));
		assertEquals(1L, registry.snapshot().revision());
		assertFalse(AllegianceReloadListener.reloadDocument(registry,
				JsonParser.parseString("{\"opposed\":\"ally\"}").getAsJsonObject()));
		assertEquals(1L, registry.snapshot().revision());
	}
}
