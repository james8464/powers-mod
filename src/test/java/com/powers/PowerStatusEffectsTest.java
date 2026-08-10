package com.powers;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerStatusEffectsTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void hiddenEffectKeepsItsHudIconWithoutEntityParticles() {
		MobEffectInstance effect = PowerStatusEffects.hidden(
				MobEffects.SPEED, 40, 1, true, true);

		assertFalse(effect.isVisible());
		assertTrue(effect.showIcon());
	}

	@Test
	void internalMovementEffectCanHideItsShortLivedIcon() {
		MobEffectInstance effect = PowerStatusEffects.hidden(
				MobEffects.SLOWNESS, 4, 0, true, false);

		assertFalse(effect.isVisible());
		assertFalse(effect.showIcon());
	}
}
