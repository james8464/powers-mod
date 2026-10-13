package com.powers.spell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BloodReadingPrivacyContractTest {
	@Test
	void consentPrecedesDiagnosisAndHistoryContainsNoPrivateIdentity() throws Exception {
		String effects = Files.readString(Path.of("src/main/java/com/powers/spell/SpellEffects.java"));
		int consent = effects.indexOf("PowerProtection.mayLocate(caster, target)");
		int report = effects.indexOf("BloodReadingReport.create(target)");
		assertTrue(consent >= 0 && report > consent);
		String history = Files.readString(Path.of(
				"src/main/java/com/powers/spell/BloodReadingHistoryService.java"));
		assertFalse(history.contains("DamageSource"));
		assertFalse(history.contains("getDisplayName"));
		assertFalse(history.contains("blockPosition"));
		assertFalse(history.contains("getArmorSlots"));
		assertFalse(history.contains("getItemBySlot"));
	}
}
