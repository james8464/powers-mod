package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillDeed;
import com.powers.player.SkillQuestTracker;
import com.powers.power.PowerDamage;
import com.powers.power.crystals.LifeBloomAbility;
import com.powers.progression.BossContributionManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Live once-per-encounter damage, support, control, and anti-duplicate proof. */
public final class BossContributionGameTests {
	@GameTest(maxTicks = 40, padding = 96)
	@SuppressWarnings("removal")
	public void heraldCreditsMeaningfulNearbyContributorsExactlyOnce(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos fixture = helper.absolutePos(new BlockPos(8, 3, 8));
		var boss = helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(8, 3, 8));
		boss.setPos(fixture.getX() + 0.5, 100.0, fixture.getZ() + 0.5);
		var damage = helper.makeMockServerPlayerInLevel();
		var support = helper.makeMockServerPlayerInLevel();
		var control = helper.makeMockServerPlayerInLevel();
		var idle = helper.makeMockServerPlayerInLevel();
		damage.setPos(boss.getX() + 2.0, 100.0, boss.getZ());
		support.setPos(boss.getX() + 3.0, 100.0, boss.getZ());
		control.setPos(boss.getX() + 4.0, 100.0, boss.getZ());
		idle.setPos(boss.getX() + 5.0, 100.0, boss.getZ());
		int damageBefore = SkillQuestTracker.deedCount(damage,
				com.powers.player.SkillDeed.BOSS_KILL);
		int supportBefore = SkillQuestTracker.deedCount(support,
				com.powers.player.SkillDeed.BOSS_KILL);
		int controlBefore = SkillQuestTracker.deedCount(control,
				com.powers.player.SkillDeed.BOSS_KILL);
		int idleBefore = SkillQuestTracker.deedCount(idle,
				com.powers.player.SkillDeed.BOSS_KILL);
		int damagePowerKillsBefore = SkillQuestTracker.deedCount(damage, SkillDeed.POWER_KILL);

		boss.hurtServer(level, PowerDamage.source(damage), 12.0F);
		damage.setHealth(10.0F);
		helper.assertTrue(new LifeBloomAbility().activate(support, PlayerPowers.get(support)),
				"Life Bloom support cast was rejected");
		BossContributionManager.recordControl(control, boss, 20);
		boss.hurtServer(level, PowerDamage.source(damage), 2_000.0F);

		helper.assertTrue(SkillQuestTracker.deedCount(damage,
				com.powers.player.SkillDeed.BOSS_KILL) == damageBefore + 1,
				"Damage contributor missed boss credit");
		helper.assertTrue(SkillQuestTracker.deedCount(support,
				com.powers.player.SkillDeed.BOSS_KILL) == supportBefore + 1,
				"Support contributor missed boss credit");
		helper.assertTrue(SkillQuestTracker.deedCount(control,
				com.powers.player.SkillDeed.BOSS_KILL) == controlBefore + 1,
				"Control contributor missed boss credit");
		helper.assertTrue(SkillQuestTracker.deedCount(idle,
				com.powers.player.SkillDeed.BOSS_KILL) == idleBefore,
				"Nearby idle player bypassed contribution rules");
		helper.assertTrue(SkillQuestTracker.deedCount(damage,
				SkillDeed.POWER_KILL) == damagePowerKillsBefore + 1,
				"Power finisher missed the existing power-kill credit");
		BossContributionManager.resolve(boss, PowerDamage.source(damage));
		helper.assertTrue(SkillQuestTracker.deedCount(damage,
				com.powers.player.SkillDeed.BOSS_KILL) == damageBefore + 1,
				"Encounter awarded duplicate boss credit");
		helper.assertTrue(SkillQuestTracker.deedCount(damage,
				SkillDeed.POWER_KILL) == damagePowerKillsBefore + 1,
				"Duplicate encounter resolution changed power-kill credit");
		helper.succeed();
	}

	@GameTest(maxTicks = 40, padding = 96)
	@SuppressWarnings("removal")
	public void firstVesselCreditsContributorAndFinisherExactlyOnce(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos fixture = helper.absolutePos(new BlockPos(8, 3, 8));
		var boss = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(8, 3, 8));
		boss.setPos(fixture.getX() + 0.5, 100.0, fixture.getZ() + 0.5);
		var contributor = helper.makeMockServerPlayerInLevel();
		var finisher = helper.makeMockServerPlayerInLevel();
		contributor.setPos(boss.getX() + 2.0, 100.0, boss.getZ());
		finisher.setPos(boss.getX() + 3.0, 100.0, boss.getZ());
		int contributorBefore = SkillQuestTracker.deedCount(contributor,
				com.powers.player.SkillDeed.BOSS_KILL);
		int finisherBefore = SkillQuestTracker.deedCount(finisher,
				com.powers.player.SkillDeed.BOSS_KILL);
		int contributorPowerKillsBefore = SkillQuestTracker.deedCount(contributor, SkillDeed.POWER_KILL);
		int finisherPowerKillsBefore = SkillQuestTracker.deedCount(finisher, SkillDeed.POWER_KILL);

		boss.hurtServer(level, PowerDamage.source(contributor), 20.0F);
		boss.hurtServer(level, PowerDamage.source(finisher), 50_000.0F);

		helper.assertTrue(SkillQuestTracker.deedCount(contributor,
				com.powers.player.SkillDeed.BOSS_KILL) == contributorBefore + 1,
				"First Vessel contributor missed boss credit");
		helper.assertTrue(SkillQuestTracker.deedCount(finisher,
				com.powers.player.SkillDeed.BOSS_KILL) == finisherBefore + 1,
				"First Vessel finisher missed boss credit");
		helper.assertTrue(SkillQuestTracker.deedCount(contributor,
				SkillDeed.POWER_KILL) == contributorPowerKillsBefore,
				"Non-finishing contributor received power-kill credit");
		helper.assertTrue(SkillQuestTracker.deedCount(finisher,
				SkillDeed.POWER_KILL) == finisherPowerKillsBefore + 1,
				"First Vessel finisher missed power-kill credit");
		BossContributionManager.resolve(boss, PowerDamage.source(finisher));
		helper.assertTrue(SkillQuestTracker.deedCount(finisher,
				com.powers.player.SkillDeed.BOSS_KILL) == finisherBefore + 1,
				"First Vessel encounter awarded duplicate credit");
		helper.assertTrue(SkillQuestTracker.deedCount(finisher,
				SkillDeed.POWER_KILL) == finisherPowerKillsBefore + 1,
				"Duplicate First Vessel resolution changed power-kill credit");
		helper.succeed();
	}
}
