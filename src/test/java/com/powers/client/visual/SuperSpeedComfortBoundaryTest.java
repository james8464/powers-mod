package com.powers.client.visual;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks Chronal Overdrive camera comfort to local presentation state. */
class SuperSpeedComfortBoundaryTest {
	@Test
	void cameraHookUsesOnlyOwnerLocalLifecycleAndVanillaAccessibility() throws IOException {
		String mixin = Files.readString(Path.of(
				"src/client/java/com/powers/mixin/SuperSpeedCameraMixin.java"));
		String clientState = Files.readString(Path.of(
				"src/client/java/com/powers/client/ClientSuperSpeedComfort.java"));
		String mixins = Files.readString(Path.of("src/main/resources/powers.mixins.json"));
		assertTrue(mixins.contains("SuperSpeedCameraMixin"));
		assertTrue(mixin.contains("ClientSuperSpeedComfort.active()"));
		assertTrue(mixin.contains("FxAccessibility.reducedMotion(client)"));
		assertTrue(mixin.contains("client.options.fovEffectScale().get()"));
		assertTrue(clientState.contains("remainingTicks") && clientState.contains("clear()"));
		assertFalse(mixin.contains("net.minecraft.server"));
		assertFalse(clientState.contains("setDeltaMovement") || clientState.contains("Attribute"));
	}
}
