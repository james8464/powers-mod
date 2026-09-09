package com.powers.spell;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CartographerEnhancementIntegrationContractTest {
	@Test
	void locatorUsesPersistentJournalProgressionGateAndBoundedBreadcrumbs() throws IOException {
		Path root = Path.of("src/main/java/com/powers");
		String packets = Files.readString(root.resolve("network/LocatorSpellPackets.java"));
		String attachments = Files.readString(root.resolve("player/PlayerPowerAttachments.java"));
		assertTrue(packets.contains("CartographerJournalStore.find"));
		assertTrue(packets.contains("CartographerJournalStore.discover"));
		assertTrue(packets.contains("CartographerBreadcrumbs.schedule"));
		assertTrue(packets.contains("mayReveal"));
		assertTrue(attachments.contains("CARTOGRAPHER_SITES"));
		assertTrue(attachments.contains("persistent(CartographerSite.CODEC.listOf()).copyOnDeath()"));
	}
}
