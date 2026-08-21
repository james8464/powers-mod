package com.powers.entity;

import com.powers.PowersEntities;
import com.powers.PowersMod;
import com.powers.ai.PerceptionQueryProfile;
import com.powers.ai.PerceptionSnapshotRules;
import com.powers.ai.PerceptionSnapshotService;
import com.powers.companion.ShadowCompanionData;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.companion.combat.ShadowCombatController;
import com.powers.companion.combat.ShadowPowerAction;
import com.powers.companion.combat.ShadowRequestRange;
import com.powers.item.artifact.ArtifactAlignment;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Live correctness and inspection proof for the shared AI perception boundary. */
public final class PerceptionSnapshotGameTests {
	private static final EntityTypeTest<Entity, LivingEntity> LIVING =
			EntityTypeTest.forClass(LivingEntity.class);

	@GameTest(maxTicks = 20, padding = 128)
	@SuppressWarnings("removal")
	public void mixedProductionQueriesBeatTheIdenticalLegacyWork(GameTestHelper helper) {
		Cluster cluster = cluster(helper, 24);
		helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(4, 2, 4))
				.setPos(cluster.owner().position());
		cluster.shadow().setEnergy(0);
		int legacyInspections = 0;
		int actualInspections = 0;
		long queries = 0;
		long cacheHits = 0;
		for (int sample = 0; sample < 4; sample++) {
			spreadMixedActors(cluster, sample);
			legacyInspections += legacyMixedInspections(cluster);
			PerceptionSnapshotService.clear();
			cluster.shadow().setTarget(null);
			cluster.darkness().setTarget(null);
			cluster.light().setTarget(null);
			int tick = cluster.level().getServer().getTickCount() + sample * 10;
			ShadowCombatController.tick(cluster.level(), cluster.shadow(), cluster.owner(), tick, tick,
					ShadowRequestRange.AUTO, "");
			acquireAtScheduledPhase(cluster.darkness());
			acquireAtScheduledPhase(cluster.light());
			GuardianAlignmentField.pulse(cluster.level(), cluster.darkness(), ArtifactAlignment.DARKNESS);
			GuardianAlignmentField.pulse(cluster.level(), cluster.light(), ArtifactAlignment.LIGHT);
			var sampleDiagnostics = PerceptionSnapshotService.diagnostics();
			actualInspections += (int) sampleDiagnostics.inspections();
			queries += sampleDiagnostics.queries();
			cacheHits += sampleDiagnostics.cacheHits();
		}

		var reduction = new PerceptionSnapshotRules.Reduction(legacyInspections,
				actualInspections, legacyInspections == 0 ? 0.0
				: 1.0 - actualInspections / (double) legacyInspections);
		PowersMod.LOGGER.info("PERF-012 identical mixed AI: queries={}, cacheHits={}, "
					+ "inspections={}, legacyInspections={}, reductionPercent={}",
				queries, cacheHits, actualInspections,
				legacyInspections, Math.round(reduction.fraction() * 10_000.0) / 100.0);
		helper.assertTrue(queries >= 20,
				"Mixed production consumers did not issue their observations");
		helper.assertTrue(reduction.fraction() >= 0.30,
				"Identical mixed-AI work reduced inspections by less than 30%: "
						+ reduction.fraction());

		PerceptionSnapshotService.clear();
		List<java.util.UUID> first = ids(cluster.level(), cluster.shadow().position());
		PerceptionSnapshotService.clear();
		List<java.util.UUID> recaptured = ids(cluster.level(), cluster.shadow().position());
		helper.assertTrue(first.equals(recaptured),
				"A bounded recapture changed deterministic entity order");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void longFiringLanesRejectRangedOffense(GameTestHelper helper) {
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		owner.setPos(center);
		ShadowCompanionEntity shadow = helper.spawn(PowersEntities.SHADOW_COMPANION,
				new BlockPos(4, 2, 4));
		shadow.configure(owner, ShadowCompanionData.defaults());

		LivingEntity horizontal = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		horizontal.setPos(center.add(110.0, 0.0, 0.0));
		assertUnsafeLaneRejectsRangedOffense(helper, owner, shadow, horizontal, center, 1_850);
		assertUnsafeLaneRejectsRangedOffense(helper, owner, shadow, horizontal, center, 4);
		horizontal.discard();

		LivingEntity vertical = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		vertical.setPos(center.add(0.0, 60.0, 0.0));
		assertUnsafeLaneRejectsRangedOffense(helper, owner, shadow, vertical, center, 4);
		vertical.discard();

		DarknessCreature ally = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(4, 2, 4));
		ally.setPos(center.add(2.0, 0.0, 0.0));
		LivingEntity shortTarget = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		shortTarget.setPos(center.add(8.0, 0.0, 0.0));
		assertUnsafeLaneRejectsRangedOffense(helper, owner, shadow, shortTarget, center, 4);
		shortTarget.discard();
		ally.discard();
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void solitaryAndSplitCellFieldsRetainTheirInspectionCaps(GameTestHelper helper) {
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		DarknessCreature first = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(4, 2, 4));
		first.setPos(center);
		for (int index = 0; index < 32; index++) {
			helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4)).setPos(center);
		}
		PerceptionSnapshotService.clear();
		GuardianAlignmentField.pulse(helper.getLevel(), first, ArtifactAlignment.DARKNESS);
		helper.assertTrue(PerceptionSnapshotService.diagnostics().inspections() <= 16,
				"One ordinary field exceeded its former 16-body inspection cap");

		DarknessCreature separated = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(4, 2, 4));
		separated.setPos(center.add(40.0, 0.0, 0.0));
		PerceptionSnapshotService.clear();
		GuardianAlignmentField.pulse(helper.getLevel(), first, ArtifactAlignment.DARKNESS);
		GuardianAlignmentField.pulse(helper.getLevel(), separated, ArtifactAlignment.DARKNESS);
		helper.assertTrue(PerceptionSnapshotService.diagnostics().inspections() <= 32,
				"Two split-cell fields exceeded their combined former caps");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void saturatedOuterSnapshotCannotAnswerASparseInnerQuery(GameTestHelper helper) {
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		for (int index = 0; index < 16; index++) {
			helper.spawn(EntityTypes.ZOMBIE, new BlockPos(12, 2, 4));
		}
		LivingEntity inner = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(4, 2, 4));

		PerceptionSnapshotService.clear();
		PerceptionSnapshotService.observe(helper.getLevel(), center, 12.0, 6.0, 16,
				ignored -> true, PerceptionQueryProfile.GUARDIAN_FIELD);
		long outerInspections = PerceptionSnapshotService.diagnostics().inspections();
		var result = PerceptionSnapshotService.observe(helper.getLevel(), center, 1.0, 2.0, 16,
				fact -> fact.entityId().equals(inner.getUUID()),
				PerceptionQueryProfile.GUARDIAN_FIELD);
		helper.assertTrue(PerceptionSnapshotService.diagnostics().inspections() > outerInspections,
				"A saturated outer snapshot was unsafely reused for a contained inner query");
		helper.assertTrue(result.stream().anyMatch(fact -> fact.entityId().equals(inner.getUUID())),
				"The sparse inner entity disappeared behind the outer query's bounded prefix");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void offLaneAlliesCannotHideAFriendlyFireRisk(GameTestHelper helper) {
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		Vec3 from = center.add(-4.0, 1.5, 0.0);
		Vec3 to = center.add(4.0, 1.5, 0.0);
		for (int index = 0; index < 16; index++) {
			DarknessCreature offLane = helper.spawn(PowersEntities.DARKNESS_CREATURE,
					new BlockPos(4, 2, 4));
			offLane.setPos(center.add(0.0, 0.0, 4.0));
		}
		DarknessCreature inLane = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(4, 2, 4));
		inLane.setPos(center);

		PerceptionSnapshotService.clear();
		var result = PerceptionSnapshotService.observe(helper.getLevel(),
				new AABB(from, to).inflate(1.5), center, 16,
				fact -> fact.darknessAligned()
						&& PerceptionSnapshotRules.withinSegmentLane(fact, from, to, 1.5),
				PerceptionQueryProfile.ALLY_LANE);
		helper.assertTrue(result.stream().anyMatch(fact -> fact.entityId().equals(inLane.getUUID())),
				"Off-lane allies consumed the firing-lane inspection budget");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void invalidGuardianTargetsAreClearedWithoutAReplacement(GameTestHelper helper) {
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		DarknessCreature guardian = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(4, 2, 4));
		LivingEntity target = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		guardian.setPos(center);
		target.setPos(center.add(1.0, 0.0, 0.0));

		guardian.setTarget(target);
		target.addTag(com.powers.player.SkillSystem.DARKNESS_TAG);
		acquireAtScheduledPhase(guardian);
		helper.assertTrue(guardian.getTarget() == null,
				"A newly allied target remained assigned when no replacement existed");

		target.removeTag(com.powers.player.SkillSystem.DARKNESS_TAG);
		target.setPos(center.add(100.0, 0.0, 0.0));
		guardian.setTarget(target);
		acquireAtScheduledPhase(guardian);
		helper.assertTrue(guardian.getTarget() == null,
				"An out-of-range target remained assigned when no replacement existed");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void alliedCrowdsAndCylinderDistractorsCannotStarveValidTargets(GameTestHelper helper) {
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		DarknessCreature guardian = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(4, 2, 4));
		guardian.setPos(center);
		for (int index = 0; index < 70; index++) {
			helper.spawn(PowersEntities.DARKNESS_CREATURE, new BlockPos(4, 2, 4))
					.setPos(center.add(0.1, 0.0, 0.1));
		}
		LivingEntity enemy = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		enemy.setPos(center);

		PerceptionSnapshotService.clear();
		acquireAtScheduledPhase(guardian);
		helper.assertTrue(guardian.getTarget() == enemy,
				"More than 64 aligned bodies starved a valid opposed target");
		LivingEntity nearerEnemy = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		nearerEnemy.setPos(center.add(0.1, 0.0, 0.0));
		acquireAtScheduledPhase(guardian);
		helper.assertTrue(guardian.getTarget() == enemy,
				"A valid current target was replaced by periodic reacquisition");

		helper.getLevel().getEntitiesOfClass(DarknessCreature.class,
				AABB.ofSize(center, 32.0, 32.0, 32.0)).stream()
				.filter(entity -> entity != guardian).forEach(Entity::discard);
		nearerEnemy.discard();
		for (int index = 0; index < 16; index++) {
			DarknessCreature distractor = helper.spawn(PowersEntities.DARKNESS_CREATURE,
					new BlockPos(4, 2, 4));
			distractor.setPos(center.add(1.0, 5.99, 0.0));
		}
		PerceptionSnapshotService.clear();
		guardian.setTarget(null);
		acquireAtScheduledPhase(guardian);
		float before = enemy.getHealth();
		GuardianAlignmentField.pulse(helper.getLevel(), guardian, ArtifactAlignment.DARKNESS);
		helper.assertTrue(!enemy.isAlive() || enemy.getHealth() < before,
				"Out-of-sphere cylinder distractors consumed the field result limit");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	private static Cluster cluster(GameTestHelper helper, int zombies) {
		// The 48-block authored query must not count neighbouring parallel GameTests.
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 250, 4)));
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		owner.setPos(center);
		ShadowCompanionEntity shadow = helper.spawn(PowersEntities.SHADOW_COMPANION,
				new BlockPos(4, 2, 4));
		shadow.configure(owner, ShadowCompanionData.defaults());
		shadow.setPos(center);
		DarknessCreature darkness = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(4, 2, 4));
		RadiantSentinel light = helper.spawn(PowersEntities.RADIANT_SENTINEL,
				new BlockPos(4, 2, 4));
		darkness.setPos(center);
		light.setPos(center);
		List<LivingEntity> targets = new ArrayList<>(zombies);
		for (int index = 0; index < zombies; index++) {
			LivingEntity target = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
			target.setPos(center);
			targets.add(target);
		}
		return new Cluster(helper.getLevel(), owner, shadow, darkness, light, List.copyOf(targets));
	}

	private static boolean acquireAtScheduledPhase(AbstractPlayerLikeMob guardian) {
		guardian.tickCount = Math.floorMod(guardian.getUUID().hashCode(), 5);
		GuardianPerceptionTargetGoal goal = new GuardianPerceptionTargetGoal(guardian);
		if (!goal.canUse()) return false;
		goal.start();
		return true;
	}

	private static int legacyTargetInspections(ServerLevel level,
			AbstractPlayerLikeMob guardian) {
		double range = Math.min(48.0,
				guardian.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
		return legacyInspections(level, guardian.getBoundingBox().inflate(range, 4.0, range), 256);
	}

	private static int legacyMixedInspections(Cluster cluster) {
		LivingEntity shadowTarget = cluster.zombies().stream()
				.min(java.util.Comparator.comparingDouble(entity ->
						entity.distanceToSqr(cluster.owner()))).orElseThrow();
		int total = legacyInspections(cluster.level(),
				AABB.ofSize(cluster.owner().position(), 48.0, 24.0, 48.0), 64);
		total += legacyInspections(cluster.level(),
				new AABB(cluster.shadow().getEyePosition(), shadowTarget.getEyePosition())
						.inflate(1.5), 16);
		total += legacyInspections(cluster.level(),
				AABB.ofSize(cluster.darkness().position(), 12.0, 12.0, 12.0), 16);
		total += legacyInspections(cluster.level(),
				AABB.ofSize(cluster.light().position(), 12.0, 12.0, 12.0), 16);
		total += legacyTargetInspections(cluster.level(), cluster.darkness());
		return total + legacyTargetInspections(cluster.level(), cluster.light());
	}

	private static void spreadMixedActors(Cluster cluster, int sample) {
		Vec3 center = cluster.owner().position();
		cluster.shadow().setPos(center.add(-1.5 + sample * 0.15, 0.0, 0.5));
		cluster.darkness().setPos(center.add(2.0 + sample * 0.2, 0.0, 1.0));
		cluster.light().setPos(center.add(-2.0 - sample * 0.2, 0.0, -1.0));
		for (int index = 0; index < cluster.zombies().size(); index++) {
			double x = index % 6 - 2.5 + sample * 0.08;
			double z = index / 6 - 1.5 - sample * 0.06;
			cluster.zombies().get(index).setPos(center.add(x, 0.0, z));
		}
	}

	private static void assertUnsafeLaneRejectsRangedOffense(GameTestHelper helper,
			ServerPlayer owner, ShadowCompanionEntity shadow, LivingEntity target, Vec3 origin,
			int energy) {
		ShadowCombatController.clear();
		PerceptionSnapshotService.clear();
		shadow.setPos(origin);
		shadow.setEnergy(energy);
		shadow.setTarget(target);
		var result = ShadowCombatController.tick(helper.getLevel(), shadow, owner, 0, 0,
				ShadowRequestRange.FAR, "");
		helper.assertTrue(result.decision() != null,
				"Unsafe-lane combat did not produce a safe movement decision");
		helper.assertTrue(result.decision().action() == null
					|| result.decision().action().range() != ShadowPowerAction.RangeMode.FAR
					|| result.decision().action().intent() != ShadowPowerAction.Intent.OFFENSE,
				"A lane-sensitive ranged attack remained eligible in an unsafe firing lane");
	}

	private static int legacyInspections(ServerLevel level, AABB bounds, int limit) {
		List<LivingEntity> found = new ArrayList<>(limit);
		level.getEntities(LIVING, bounds, ignored -> true, found, limit);
		return found.size();
	}

	private static List<java.util.UUID> ids(ServerLevel level, Vec3 center) {
		return PerceptionSnapshotService.observe(level, center, 24.0, 12.0, 64,
				ignored -> true, PerceptionQueryProfile.SHADOW_TARGET).stream()
				.map(observation -> observation.entityId()).toList();
	}

	private record Cluster(ServerLevel level, ServerPlayer owner, ShadowCompanionEntity shadow,
			DarknessCreature darkness, RadiantSentinel light, List<LivingEntity> zombies) { }
}
