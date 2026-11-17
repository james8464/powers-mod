package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.animation.CastingPoseService;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Exercises the actual vanilla melee entrypoint, not just catalogue metadata. */
public final class FirstVesselTelegraphGameTests {
	@GameTest(maxTicks = 100)
	public void liveMeleeGoalHonorsWarningAndReleases(GameTestHelper helper) {
		var vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
		var target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(3, 2, 2));
		vessel.setNoGravity(true);
		vessel.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);
		target.setNoAi(true);
		target.setNoGravity(true);
		vessel.setTarget(target);
		float health = target.getHealth();
		var sawMelee = new java.util.concurrent.atomic.AtomicBoolean();
		long startGameTime = helper.getLevel().getGameTime();
		var trace = new java.util.ArrayList<String>();
		for (int tick = 1; tick < 60; tick++) {
			helper.runAtTickTime(tick, () -> {
				var damage = target.getLastDamageSource();
				var pose = CastingPoseService.current(vessel.getUUID(), helper.getLevel().getGameTime());
				if (vessel.tickCount % 5 == 0) {
					trace.add(vessel.tickCount + ":" + target.getHealth() + ":" + pose
							+ ":" + vessel.isWithinMeleeAttackRange(target));
				}
				if (damage != null && damage.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)) {
					sawMelee.set(true);
				}
			});
		}
		for (int tick = 1; tick < 20; tick++) {
			helper.runAtTickTime(tick, () -> helper.assertTrue(target.getHealth() == health,
					"AI melee bypassed its counter window"));
		}
		helper.runAtTickTime(60, () -> {
			helper.assertTrue(target.getHealth() < health, "AI melee was starved by catalogue planning");
			helper.assertTrue(sawMelee.get(),
					"Catalogue damage cannot stand in for the live melee goal; source="
							+ target.getLastDamageSource() + " health=" + target.getHealth()
							+ " vessel=" + vessel.position() + " target=" + target.position()
							+ " age=" + vessel.tickCount + " selected=" + vessel.getTarget()
							+ " range=" + vessel.isWithinMeleeAttackRange(target)
							+ " sight=" + vessel.hasLineOfSight(target)
							+ " startGameTime=" + startGameTime + " trace=" + trace);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void changedMeleeTargetNeedsItsOwnWarning(GameTestHelper helper) {
		var vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
		var first = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(3, 2, 2));
		var second = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 2, 3));
		vessel.setNoAi(true);
		vessel.setNoGravity(true);
		first.setNoAi(true);
		first.setNoGravity(true);
		second.setNoAi(true);
		second.setNoGravity(true);
		vessel.setTarget(first);
		vessel.doHurtTarget(helper.getLevel(), first);
		float health = second.getHealth();
		helper.runAtTickTime(30, () -> {
			vessel.setTarget(second);
			helper.assertTrue(!vessel.doHurtTarget(helper.getLevel(), second),
					"A different target inherited the old melee warning");
			helper.assertTrue(second.getHealth() == health, "New target was hit without warning");
		});
		helper.runAtTickTime(60, () -> {
			helper.assertTrue(vessel.doHurtTarget(helper.getLevel(), second),
					"New target's completed melee warning never released");
			helper.assertTrue(second.getHealth() < health, "Second target took no warned damage");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void spellWindupCannotBeBypassedByMelee(GameTestHelper helper) throws Exception {
		var vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
		var target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(3, 2, 2));
		vessel.setNoAi(true);
		vessel.setNoGravity(true);
		target.setNoAi(true);
		target.setNoGravity(true);
		vessel.setTarget(target);
		float health = target.getHealth();
		var begin = com.powers.entity.FirstVessel.class.getDeclaredMethod("beginTechniqueTelegraph",
				net.minecraft.server.level.ServerLevel.class, String.class);
		begin.setAccessible(true);
		begin.invoke(vessel, helper.getLevel(), "world_suture");
		for (int tick = 0; tick < 40; tick++) {
			helper.runAtTickTime(tick, () -> {
				helper.assertTrue(!vessel.doHurtTarget(helper.getLevel(), target),
						"Melee bypassed an active technique warning");
				helper.assertTrue(target.getHealth() == health, "Melee damage overlapped spell windup");
			});
		}
		helper.runAtTickTime(40, () -> {
			CastingPoseService.clear(vessel);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void meleeCannotDamageBeforeItsWarningWindow(GameTestHelper helper) {
		var vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 2, 2));
		var target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(3, 2, 2));
		vessel.setNoAi(true);
		vessel.setNoGravity(true);
		target.setNoAi(true);
		target.setNoGravity(true);
		vessel.setTarget(target);
		float health = target.getHealth();
		helper.assertTrue(!vessel.doHurtTarget(helper.getLevel(), target),
				"First Vessel melee bypassed its warning on the first attack");
		helper.assertTrue(target.getHealth() == health, "Melee damaged the target before warning");
		for (int tick = 1; tick < 20; tick++) {
			helper.runAtTickTime(tick, () -> {
				vessel.doHurtTarget(helper.getLevel(), target);
				helper.assertTrue(target.getHealth() == health, "Melee damaged during its counter window");
			});
		}
		helper.runAtTickTime(40, () -> {
			helper.assertTrue(vessel.doHurtTarget(helper.getLevel(), target),
					"Warned melee never released against its original nearby target");
			helper.assertTrue(target.getHealth() < health, "Warned melee did not damage its target");
			CastingPoseService.clear(vessel);
			helper.succeed();
		});
	}
}
