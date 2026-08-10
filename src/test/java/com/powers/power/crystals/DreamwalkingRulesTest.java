package com.powers.power.crystals;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreamwalkingRulesTest {
	private static ResourceKey<Level> dimension(String path) {
		return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("powers", path));
	}

	@Test
	void remoteCameraRequiresMindTravelAcrossDimensions() {
		ResourceKey<Level> light = dimension("light_realm");
		assertFalse(DreamwalkingRules.mustTravel(light, light));
		assertTrue(DreamwalkingRules.mustTravel(light, dimension("dark_realm")));
	}
}
