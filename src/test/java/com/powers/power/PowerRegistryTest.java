package com.powers.power;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerRegistryTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void assignableRosterPromotesSizeMorphingAndRemovesSlowWorldRuntime() {
		PowerRegistry.initialize();
		Set<String> ids = PowerRegistry.getAssignable().stream()
				.map(power -> power.id().getPath())
				.collect(Collectors.toSet());

		assertTrue(ids.contains("size_shift"));
		assertFalse(ids.contains("slow_world"));
		assertNull(PowerRegistry.get("slow_world"));
	}
}
