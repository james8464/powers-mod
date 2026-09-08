package com.powers.power.damage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayeredDamageResolverTest {
	@Test
	void declaresOneForcefieldSoulLinkHealthOrder() {
		assertEquals(List.of(LayeredDamageResolver.Stage.FORCEFIELD,
				LayeredDamageResolver.Stage.SOUL_LINK,
				LayeredDamageResolver.Stage.HEALTH), LayeredDamageResolver.order());
	}

	@Test
	void overkillSacrificesOnlyExistingIntegrityAndBlocksTheWholeImpact() {
		var decision = LayeredDamageResolver.ward(50_000.0F, 40.0F, true);

		assertTrue(decision.present());
		assertTrue(decision.shattered());
		assertEquals(50_000.0F, decision.blockedDamage());
		assertEquals(40.0F, decision.integritySpent());
		assertEquals(0.0F, decision.remainingIntegrity());
	}

	@Test
	void absentWardPassesTheImpactWithoutInventingConsumption() {
		var decision = LayeredDamageResolver.ward(7.0F, 0.0F, false);

		assertFalse(decision.present());
		assertEquals(0.0F, decision.blockedDamage());
		assertEquals(0.0F, decision.integritySpent());
	}

	@Test
	void soulLinkCapsOneForwardingPassAndHealthUsesObservedDamage() {
		var mirror = LayeredDamageResolver.mirror(8.0F, 1.5, 10.0F);

		assertEquals(12.0F, mirror.requestedDamage());
		assertEquals(10.0F, mirror.forwardedDamage());
		assertEquals(4.0F, LayeredDamageResolver.health(20.0F, 16.0F));
		assertEquals(0.0F, LayeredDamageResolver.health(16.0F, 20.0F));
	}
}
