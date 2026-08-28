package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.boss.FirstVesselCombat;
import com.powers.boss.FirstVesselPhase;
import com.powers.boss.FirstVesselPowerAction;
import com.powers.companion.ShadowCompanionData;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.companion.combat.ShadowPowerCatalogue;
import com.powers.companion.combat.ShadowPowerExecutor;
import com.powers.animation.CastingHand;
import com.powers.animation.CastingPose;
import com.powers.animation.CastingPoseEvent;
import com.powers.animation.CastingPoseService;
import com.powers.animation.CastingStyle;
import com.powers.entity.RadiantSentinel;
import com.powers.entity.FirstVessel;
import com.powers.entity.PowerTestActor;
import com.powers.entity.RealmHerald;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Live server-boundary tests for VFX-006 semantic pose state. */
public final class CastingPoseGameTests {
	@GameTest(maxTicks = 20)
	public void guardianCommittedProjectileEmitsProjectPose(GameTestHelper helper) {
		CastingPoseService.clearAll();
		RadiantSentinel guardian = helper.spawn(PowersEntities.RADIANT_SENTINEL,
				new BlockPos(2, 2, 2));
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(2, 2, 7));
		try {
			Method cast = guardian.getClass().getSuperclass().getDeclaredMethod(
					"castFireball", net.minecraft.server.level.ServerLevel.class, LivingEntity.class);
			cast.setAccessible(true);
			cast.invoke(guardian, helper.getLevel(), target);
			CastingPoseEvent event = CastingPoseService.current(guardian.getUUID(),
					helper.getLevel().getGameTime()).orElseThrow();
			helper.assertTrue(event.pose() == CastingPose.PROJECT
					&& event.style() == CastingStyle.RADIANT,
					"Committed guardian projectile did not emit a radiant project pose");
			helper.succeed();
		} catch (ReflectiveOperationException | java.util.NoSuchElementException failure) {
			helper.fail("Guardian production pose seam missing: " + failure.getMessage());
		}
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void shadowCommittedPowerEmitsMappedPose(GameTestHelper helper) {
		CastingPoseService.clearAll();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		ShadowCompanionEntity shadow = helper.spawn(PowersEntities.SHADOW_COMPANION,
				new BlockPos(2, 2, 2));
		shadow.configure(owner, ShadowCompanionData.defaults().withRevealed(true));
		var result = ShadowPowerExecutor.execute(helper.getLevel(), shadow, null,
				ShadowPowerCatalogue.find("flight"), new ShadowPowerExecutor.ExecutionContext(owner,
						false, helper.getLevel().getServer().getTickCount()));
		helper.assertTrue(result.success(), "Shadow fixture failed to commit flight");
		CastingPoseEvent event = CastingPoseService.current(shadow.getUUID(),
				helper.getLevel().getGameTime()).orElseThrow();
		helper.assertTrue(event.pose() == CastingPose.INVOKE
				&& event.style() == CastingStyle.SHADOW,
				"Committed Shadow power did not emit its mapped pose");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void uncommittedShadowPowerEmitsNoPose(GameTestHelper helper) {
		CastingPoseService.clearAll();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		ShadowCompanionEntity shadow = helper.spawn(PowersEntities.SHADOW_COMPANION,
				new BlockPos(2, 2, 2));
		shadow.configure(owner, ShadowCompanionData.defaults().withRevealed(true));
		shadow.setEnergy(0);
		var result = ShadowPowerExecutor.execute(helper.getLevel(), shadow, null,
				ShadowPowerCatalogue.find("flight"), new ShadowPowerExecutor.ExecutionContext(owner,
						false, helper.getLevel().getServer().getTickCount()));
		helper.assertFalse(result.success(), "Unaffordable Shadow fixture unexpectedly committed");
		helper.assertTrue(CastingPoseService.current(shadow.getUUID(),
				helper.getLevel().getGameTime()).isEmpty(),
				"Unaffordable Shadow action emitted presentation state");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void firstVesselCommittedActionEmitsMappedPose(GameTestHelper helper) {
		CastingPoseService.clearAll();
		FirstVessel vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(2, 2, 7));
		FirstVesselCombat.cast(helper.getLevel(), vessel, target,
				new FirstVesselPowerAction("fireball", FirstVesselPowerAction.Kind.PROJECTILE,
						20, 1, 0), FirstVesselPhase.AWAKENING);
		CastingPoseEvent event = CastingPoseService.current(vessel.getUUID(),
				helper.getLevel().getGameTime()).orElseThrow();
		helper.assertTrue(event.pose() == CastingPose.PROJECT
				&& event.style() == CastingStyle.FIRST_VESSEL,
				"Committed First Vessel action did not emit its mapped pose");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void heraldCommittedBeamEmitsRealmStyle(GameTestHelper helper) {
		CastingPoseService.clearAll();
		RealmHerald herald = helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(2, 2, 2));
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(2, 2, 7));
		target.addTag(com.powers.player.SkillSystem.DARKNESS_TAG);
		herald.setTarget(target);
		herald.tickCount = 80;
		try {
			Method ai = RealmHerald.class.getDeclaredMethod("customServerAiStep",
					net.minecraft.server.level.ServerLevel.class);
			ai.setAccessible(true);
			ai.invoke(herald, helper.getLevel());
			CastingPoseEvent event = CastingPoseService.current(herald.getUUID(),
					helper.getLevel().getGameTime()).orElseThrow();
			helper.assertTrue(event.pose() == CastingPose.CHANNEL
					&& event.style() == CastingStyle.HERALD_LIGHT,
					"Committed Herald beam did not emit its realm-style channel pose");
			helper.succeed();
		} catch (ReflectiveOperationException | java.util.NoSuchElementException failure) {
			helper.fail("Herald production pose seam missing: " + failure.getMessage());
		}
	}

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
