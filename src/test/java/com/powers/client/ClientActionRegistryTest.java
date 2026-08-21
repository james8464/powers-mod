package com.powers.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientActionRegistryTest {
	@AfterEach
	void reset() {
		ClientActionRegistry.reset();
	}

	@Test
	void reorderedOlderMenusCannotRegressRevisionOrArtifactSelection() {
		ClientActionRegistry.acceptArtifact(12L, "innate/fireball");
		ClientActionRegistry.acceptArtifact(11L, "innate/lightning_strike");
		assertEquals(12L, ClientActionRegistry.revision());
		assertEquals("innate/fireball", ClientActionRegistry.artifactActionKey());
	}
}
