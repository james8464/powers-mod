package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.animation.CastingHand;
import com.powers.animation.CastingPose;
import com.powers.animation.CastingPoseEvent;
import com.powers.animation.CastingPoseService;
import com.powers.animation.CastingStyle;
import com.powers.entity.RadiantSentinel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Live server-boundary tests for VFX-006 semantic pose state. */
public final class CastingPoseGameTests {
	@GameTest(maxTicks = 20)
	public void scopedEntityStartUsesAuthoritativeLevelTime(GameTestHelper helper) {
		RadiantSentinel guardian = helper.spawn(PowersEntities.RADIANT_SENTINEL,
				new BlockPos(2, 2, 2));
		try {
			Method start = CastingPoseService.class.getMethod("start", LivingEntity.class,
					CastingPose.class, CastingStyle.class, CastingHand.class, int.class);
			Method current = CastingPoseService.class.getMethod("current", UUID.class, long.class);
			start.invoke(null, guardian, CastingPose.PROJECT, CastingStyle.RADIANT,
					CastingHand.RIGHT, 20);
			@SuppressWarnings("unchecked")
			Optional<CastingPoseEvent> event = (Optional<CastingPoseEvent>) current.invoke(null,
					guardian.getUUID(), helper.getLevel().getGameTime());
			helper.assertTrue(event.isPresent(), "Scoped entity pose was not retained");
			helper.assertTrue(event.orElseThrow().startGameTime() == helper.getLevel().getGameTime(),
					"Pose start did not use authoritative level time");
			helper.succeed();
		} catch (ReflectiveOperationException missing) {
			helper.fail("CastingPoseService live boundary is not implemented: " + missing.getMessage());
		}
	}

	@GameTest(maxTicks = 20)
	public void excludedTestActorCannotStartPose(GameTestHelper helper) {
		var actor = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 2, 2));
		try {
			Method start = CastingPoseService.class.getMethod("start", LivingEntity.class,
					CastingPose.class, CastingStyle.class, CastingHand.class, int.class);
			Method current = CastingPoseService.class.getMethod("current", UUID.class, long.class);
			start.invoke(null, actor, CastingPose.PROJECT, CastingStyle.RADIANT,
					CastingHand.RIGHT, 20);
			@SuppressWarnings("unchecked")
			Optional<CastingPoseEvent> event = (Optional<CastingPoseEvent>) current.invoke(null,
					actor.getUUID(), helper.getLevel().getGameTime());
			helper.assertTrue(event.isEmpty(), "Excluded test actor received a casting pose");
			helper.succeed();
		} catch (ReflectiveOperationException missing) {
			helper.fail("CastingPoseService live boundary is not implemented: " + missing.getMessage());
		}
	}
}
