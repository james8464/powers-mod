package com.powers.power.abilities;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InvisibilityToggleAbilityTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void ownedInvisibilityIsInfiniteAmplifier255AndCompletelyHidden() {
		var effect = InvisibilityToggleAbility.ownedEffect();

		assertEquals(Integer.MAX_VALUE, effect.getDuration());
		assertEquals(255, effect.getAmplifier());
		assertFalse(effect.isVisible());
		assertFalse(effect.showIcon());
	}
}
