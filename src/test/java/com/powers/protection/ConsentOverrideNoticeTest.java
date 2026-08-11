package com.powers.protection;

import com.powers.util.PowerMessages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsentOverrideNoticeTest {
	@Test
	void targetNoticeIsPersistentAndNamesKindAndSurcharge() {
		ConsentOverrideNotice notice = ConsentOverrideNotice.forTarget(ConsentKind.DREAMWALK);

		assertEquals(PowerMessages.Delivery.CHAT, notice.delivery());
		assertEquals("artifact.powers.empyrean.overridden", notice.translationKey());
		assertEquals("dreamwalk", notice.kind());
		assertEquals(ConsentOverrideRules.OVERRIDE_ENERGY_SURCHARGE, notice.energySurcharge());
	}
}
