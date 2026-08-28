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
import com.powers.entity.DarknessCreature;
import com.powers.entity.FirstVessel;
import com.powers.entity.PowerTestActor;
import com.powers.entity.RealmHerald;
import com.powers.player.PlayerPowers;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.protection.PowerProtectionAdapters;
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
	public void darknessGuardianCommittedLightningEmitsDarkProjectPose(GameTestHelper helper) {
		CastingPoseService.clearAll();
		DarknessCreature guardian = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(2, 2, 2));
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(2, 2, 7));
		try {
			Method cast = guardian.getClass().getSuperclass().getDeclaredMethod(
					"castLightning", net.minecraft.server.level.ServerLevel.class, LivingEntity.class);
			cast.setAccessible(true);
			cast.invoke(guardian, helper.getLevel(), target);
			assertPose(helper, guardian, CastingPose.PROJECT, CastingStyle.DARKNESS,
					"Dark guardian lightning");
			helper.succeed();
		} catch (ReflectiveOperationException | java.util.NoSuchElementException failure) {
			helper.fail("Dark guardian production pose seam missing: " + failure.getMessage());
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
	public void uncommittedFirstVesselMobilityEmitsNoPose(GameTestHelper helper) {
		CastingPoseService.clearAll();
		FirstVessel vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(2, 2, 2));
		boolean committed = FirstVesselCombat.cast(helper.getLevel(), vessel, target,
				new FirstVesselPowerAction("speed_burst", FirstVesselPowerAction.Kind.MOBILITY,
						20, 1, 0), FirstVesselPhase.AWAKENING);
		helper.assertFalse(committed, "Zero-distance mobility unexpectedly committed");
		helper.assertTrue(CastingPoseService.current(vessel.getUUID(),
				helper.getLevel().getGameTime()).isEmpty(),
				"Uncommitted First Vessel mobility emitted a pose");
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
	public void darkHeraldCommittedBeamEmitsDarkRealmStyle(GameTestHelper helper) {
		CastingPoseService.clearAll();
		RealmHerald herald = helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(2, 2, 2));
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(2, 2, 7));
		herald.setTarget(target);
		herald.tickCount = 80;
		try {
			Method ai = RealmHerald.class.getDeclaredMethod("customServerAiStep",
					net.minecraft.server.level.ServerLevel.class);
			ai.setAccessible(true);
			ai.invoke(herald, helper.getLevel());
			assertPose(helper, herald, CastingPose.CHANNEL, CastingStyle.HERALD_DARK,
					"Dark Herald beam");
			helper.succeed();
		} catch (ReflectiveOperationException | java.util.NoSuchElementException failure) {
			helper.fail("Dark Herald production pose seam missing: " + failure.getMessage());
		}
	}

	@GameTest(maxTicks = 20)
	public void firstVesselWorldSutureAndFirmamentOwnReleaseHooks(GameTestHelper helper) {
		CastingPoseService.clearAll();
		FirstVessel suture = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
		FirstVesselCombat.worldSuture(helper.getLevel(), suture);
		assertPose(helper, suture, CastingPose.RELEASE, CastingStyle.FIRST_VESSEL,
				"World Suture");
		FirstVessel firmament = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(8, 2, 2));
		FirstVesselCombat.lastFirmament(helper.getLevel(), firmament);
		assertPose(helper, firmament, CastingPose.RELEASE, CastingStyle.FIRST_VESSEL,
				"Last Firmament");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void firstVesselStolenAndDeckCastsUseCommittedProductionSeams(GameTestHelper helper) {
		CastingPoseService.clearAll();
		ServerPlayer target = helper.makeMockServerPlayerInLevel();
		target.teleportTo(8.5, 2.0, 8.5);
		PlayerPowers.get(target).setSlots(target, java.util.List.of(
				"powers:flight", "powers:fireball", "powers:forcefield"));
		FirstVessel stolen = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
		FirstVessel deck = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 10));
		try {
			Method stolenCast = FirstVessel.class.getDeclaredMethod("castStolenPower",
					net.minecraft.server.level.ServerLevel.class, ServerPlayer.class);
			stolenCast.setAccessible(true);
			helper.assertTrue((boolean) stolenCast.invoke(stolen, helper.getLevel(), target),
					"Stolen-power production seam did not commit");
			helper.assertTrue(CastingPoseService.current(stolen.getUUID(),
					helper.getLevel().getGameTime()).isPresent(), "Stolen cast emitted no pose");
			Method deckCast = FirstVessel.class.getDeclaredMethod("castFromDeck",
					net.minecraft.server.level.ServerLevel.class, LivingEntity.class);
			deckCast.setAccessible(true);
			deck.setHealth(deck.getMaxHealth() * 0.5F);
			helper.assertTrue((boolean) deckCast.invoke(deck, helper.getLevel(), target),
					"Deck production seam did not commit");
			helper.assertTrue(CastingPoseService.current(deck.getUUID(),
					helper.getLevel().getGameTime()).isPresent(), "Deck cast emitted no pose");
			helper.succeed();
		} catch (ReflectiveOperationException failure) {
			helper.fail("First Vessel production cast seam missing: " + failure.getMessage());
		}
	}

	@GameTest(maxTicks = 20)
	public void protectedFirstVesselCancellationEmitsNoPose(GameTestHelper helper) {
		CastingPoseService.clearAll();
		String adapter = "vfx006_pose_deny";
		helper.assertTrue(PowerProtectionAdapters.register(adapter, 20_000, query -> false),
				"Could not install protection fixture");
		try {
			FirstVessel vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
			PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
					new BlockPos(2, 2, 7));
			boolean committed = FirstVesselCombat.cast(helper.getLevel(), vessel, target,
					new FirstVesselPowerAction("fireball", FirstVesselPowerAction.Kind.PROJECTILE,
							20, 1, 0), FirstVesselPhase.AWAKENING);
			helper.assertFalse(committed, "Protected action unexpectedly committed");
			helper.assertTrue(CastingPoseService.current(vessel.getUUID(),
					helper.getLevel().getGameTime()).isEmpty(),
					"Protected cancellation emitted a pose");
			helper.succeed();
		} finally {
			PowerProtectionAdapters.unregister(adapter);
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

	@GameTest(maxTicks = 20)
	public void reconstitutionCompletionAndInterruptionClearChannels(GameTestHelper helper) {
		CastingPoseService.clearAll();
		FirstVessel completed = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
		FirstVessel interrupted = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(8, 2, 2));
		try {
			Method begin = FirstVessel.class.getDeclaredMethod("beginReconstitution",
					net.minecraft.server.level.ServerLevel.class);
			Method tick = FirstVessel.class.getDeclaredMethod("tickReconstitution",
					net.minecraft.server.level.ServerLevel.class);
			begin.setAccessible(true);
			tick.setAccessible(true);
			begin.invoke(completed, helper.getLevel());
			for (int index = 0; index < 120; index++) tick.invoke(completed, helper.getLevel());
			helper.assertTrue(CastingPoseService.current(completed.getUUID(),
					helper.getLevel().getGameTime()).isEmpty(),
					"Completed Reconstitution retained its channel");
			begin.invoke(interrupted, helper.getLevel());
			var damage = FirstVessel.class.getDeclaredField("reconstitutionDamage");
			damage.setAccessible(true);
			damage.setFloat(interrupted, Float.MAX_VALUE);
			tick.invoke(interrupted, helper.getLevel());
			helper.assertTrue(CastingPoseService.current(interrupted.getUUID(),
					helper.getLevel().getGameTime()).isEmpty(),
					"Interrupted Reconstitution retained its channel");
			helper.succeed();
		} catch (ReflectiveOperationException failure) {
			helper.fail("Reconstitution production seam missing: " + failure.getMessage());
		}
	}

	@GameTest(environment = "powers:casting_pose_isolated", maxTicks = 20)
	public void lateSnapshotPreservesClockExpiryRemovesStateAndUsesZeroTravelTickets(
			GameTestHelper helper) {
		CastingPoseService.clearAll();
		int tickets = TravelChunkLoader.pendingRequestCount();
		RadiantSentinel guardian = helper.spawn(PowersEntities.RADIANT_SENTINEL,
				new BlockPos(2, 2, 2));
		long start = helper.getLevel().getGameTime();
		CastingPoseService.start(guardian, CastingPose.CHANNEL, CastingStyle.RADIANT,
				CastingHand.BOTH, 6).orElseThrow();
		helper.runAfterDelay(3, () -> {
			CastingPoseEvent late = CastingPoseService.current(guardian.getUUID(),
					helper.getLevel().getGameTime()).orElseThrow();
			helper.assertTrue(late.startGameTime() == start,
					"Late snapshot restarted the authoritative clock");
			helper.assertTrue(TravelChunkLoader.pendingRequestCount() == tickets,
					"Casting pose created a travel ticket");
		});
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(CastingPoseService.current(guardian.getUUID(),
					helper.getLevel().getGameTime()).isEmpty(), "Expired pose remained active");
			helper.assertTrue(TravelChunkLoader.pendingRequestCount() == tickets,
					"Casting pose lifecycle leaked a travel ticket");
			helper.succeed();
		});
	}

	private static void assertPose(GameTestHelper helper, LivingEntity entity,
			CastingPose pose, CastingStyle style, String action) {
		CastingPoseEvent event = CastingPoseService.current(entity.getUUID(),
				helper.getLevel().getGameTime()).orElseThrow();
		helper.assertTrue(event.pose() == pose && event.style() == style,
				action + " emitted " + event.pose() + '/' + event.style());
	}
}
