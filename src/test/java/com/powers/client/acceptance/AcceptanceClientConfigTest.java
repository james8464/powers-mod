package com.powers.client.acceptance;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptanceClientConfigTest {
	@Test
	void harnessRequiresDevelopmentModeAndAnExplicitRoleAndServer() {
		Map<String, String> properties = Map.of(
				"powers.qa.role", "Caster",
				"powers.qa.server", "localhost:25565",
				"powers.qa.script", "caster.tsv");
		AcceptanceClientConfig enabled = AcceptanceClientConfig.resolve(true, properties::get);

		assertTrue(enabled.enabled());
		assertEquals("Caster", enabled.role());
		assertEquals("localhost:25565", enabled.server());
		assertEquals("caster.tsv", enabled.script());
		assertFalse(AcceptanceClientConfig.resolve(false, properties::get).enabled());
		assertFalse(AcceptanceClientConfig.resolve(true, key -> null).enabled());
	}
}
