package com.powers.companion;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowCompanionDataTest {
	@Test
	void defaultsAndMutationsClampPersistentGameplayState() {
		var defaults = ShadowCompanionData.defaults();
		assertEquals(1_850, defaults.energy());
		assertEquals(ShadowStance.FOLLOW, defaults.stance());
		assertFalse(defaults.revealed());
		assertTrue(defaults.bodyUuid().isEmpty());

		UUID body = UUID.fromString("2d4277a8-1716-4a35-bd28-1ba94484f9d4");
		var changed = defaults.withEnergy(9_999).withStance(null)
				.withRevealed(true).withBodyId(body).withRecallReadyAt(-40L);
		assertEquals(1_850, changed.energy());
		assertEquals(ShadowStance.FOLLOW, changed.stance());
		assertTrue(changed.revealed());
		assertEquals(body, changed.bodyUuid().orElseThrow());
		assertEquals(0L, changed.recallReadyAt());
	}

	@Test
	void codecRoundTripsOnlySanitizedSaveSafeValues() {
		UUID body = UUID.fromString("2d4277a8-1716-4a35-bd28-1ba94484f9d4");
		var original = ShadowCompanionData.defaults().withEnergy(417)
				.withStance(ShadowStance.GUARD).withRevealed(true)
				.withBodyId(body).withRecallReadyAt(12_345L);
		var encoded = ShadowCompanionData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		var decoded = ShadowCompanionData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		assertEquals(original, decoded);
	}
}
