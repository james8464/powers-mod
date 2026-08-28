package com.powers.animation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CastingPoseRendererSourceTest {
	@Test
	void onlyOwnedPlayerLikeRenderersApplyPostVanillaPoseDeltas() throws IOException {
		String humanoid = Files.readString(Path.of(
				"src/client/java/com/powers/client/animation/CastingHumanoidModel.java"));
		String player = Files.readString(Path.of(
				"src/client/java/com/powers/client/animation/CastingPlayerModel.java"));
		String mobs = Files.readString(Path.of(
				"src/client/java/com/powers/client/PlayerLikeMobRenderer.java"));
		String shadow = Files.readString(Path.of(
				"src/client/java/com/powers/client/ShadowCompanionRenderer.java"));
		assertTrue(humanoid.indexOf("super.setupAnim(state)") < humanoid.indexOf("apply("));
		assertTrue(player.indexOf("super.setupAnim(state)") < player.indexOf("apply("));
		assertTrue(mobs.contains("ClientCastingPoseManager.resolve(entity)"));
		assertTrue(shadow.contains("ClientCastingPoseManager.resolve(entity)"));
		assertTrue(mobs.contains("FxAccessibility.reducedMotion(Minecraft.getInstance())"));
		assertTrue(shadow.contains("FxAccessibility.reducedMotion(Minecraft.getInstance())"));
		assertTrue(mobs.contains("CastingPoseLocomotion.scale("));
		assertTrue(shadow.contains("CastingPoseLocomotion.scale("));
	}
}
