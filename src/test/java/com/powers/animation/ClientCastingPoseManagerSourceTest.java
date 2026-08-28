package com.powers.animation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientCastingPoseManagerSourceTest {
	private static final Path MANAGER = Path.of(
			"src/client/java/com/powers/client/animation/ClientCastingPoseManager.java");
	private static final Path CLIENT = Path.of("src/client/java/com/powers/client/PowersClient.java");

	@Test
	void receiverCapturesEpochBeforeEnqueueAndLifecycleResets() throws IOException {
		assertTrue(Files.exists(MANAGER), "Client casting-pose manager is not implemented");
		String manager = Files.readString(MANAGER);
		String client = Files.readString(CLIENT);
		assertTrue(client.contains("ClientCastingPoseManager.captureHandlerStamp(context.client())"));
		assertTrue(client.contains("ClientCastingPoseManager.handle(payload, captured)"));
		assertTrue(client.contains("ClientCastingPoseManager.resetConnectionEpoch();"));
		assertTrue(client.contains("ClientCastingPoseManager.tick(client);"));
		assertTrue(manager.contains("CastingPoseService.scopeType(entity.getClass())"));
		assertTrue(manager.contains("state.accept("));
	}
}
