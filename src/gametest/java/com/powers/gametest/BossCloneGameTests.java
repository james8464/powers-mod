package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersItems;
import com.powers.PowersWeapons;
import com.powers.entity.EchoClone;
import com.powers.entity.FirstVessel;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.crystals.CrystalPowerRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Live summon AI and lifetime checks through real selected crystal/relic routes. */
public final class BossCloneGameTests {
	@GameTest(maxTicks = 1800)
	public void orangeClonesFightLightHerald(GameTestHelper helper) { clones(helper, PowersEntities.LIGHT_HERALD, false); }
	@GameTest(maxTicks = 1800)
	public void orangeClonesFightDarkHerald(GameTestHelper helper) { clones(helper, PowersEntities.DARK_HERALD, false); }
	@GameTest(maxTicks = 1800)
	public void orangeClonesFightFirstVessel(GameTestHelper helper) { clones(helper, PowersEntities.FIRST_VESSEL, false); }
	@GameTest(maxTicks = 1800)
	public void artifactClonesFightLightHerald(GameTestHelper helper) { clones(helper, PowersEntities.LIGHT_HERALD, true); }
	@GameTest(maxTicks = 1800)
	public void artifactClonesRespectAlliedDarkHerald(GameTestHelper helper) { clones(helper, PowersEntities.DARK_HERALD, true); }
	@GameTest(maxTicks = 1800)
	public void artifactClonesFightFirstVessel(GameTestHelper helper) { clones(helper, PowersEntities.FIRST_VESSEL, true); }
	@GameTest(maxTicks = 100)
	public void orangeClonesRetireAfterOwnerDeath(GameTestHelper helper) { clones(helper, PowersEntities.DARK_HERALD, false, true); }
	@GameTest(maxTicks = 100)
	public void artifactClonesRetireAfterOwnerDeath(GameTestHelper helper) { clones(helper, PowersEntities.DARK_HERALD, true, true); }

	private static void clones(GameTestHelper helper, EntityType<? extends Mob> type, boolean artifact) {
		clones(helper, type, artifact, false);
	}

	private static void clones(GameTestHelper helper, EntityType<? extends Mob> type, boolean artifact, boolean ownerDeath) {
		var origin = helper.absolutePos(new BlockPos(8, 2, 8));
		var center = new net.minecraft.world.level.ChunkPos(origin.getX() >> 4, origin.getZ() >> 4);
		for (int x = center.x() - 1; x <= center.x() + 1; x++) for (int z = center.z() - 1; z <= center.z() + 1; z++) {
			helper.getLevel().setChunkForced(x, z, true);
		}
		helper.runAfterDelay(20, () -> castClones(helper, type, artifact, center, ownerDeath));
	}

	@SuppressWarnings("removal")
	private static void castClones(GameTestHelper helper, EntityType<? extends Mob> type, boolean artifact,
			net.minecraft.world.level.ChunkPos center, boolean ownerDeath) {
		for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) {
			helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			for (int y = 2; y <= 8; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
		}
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		var origin = helper.absolutePos(new BlockPos(8, 2, 8));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var data = PlayerPowers.get(caster);
		if (artifact || type == PowersEntities.LIGHT_HERALD) caster.addTag(SkillSystem.DARKNESS_TAG);
		if (artifact) {
			data.setDarknessLevel(caster, 1);
			caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
		} else caster.setItemInHand(InteractionHand.MAIN_HAND, PowersItems.ORANGE_CRYSTAL.getDefaultInstance());
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(8, 2, 12));
		boss.setNoAi(true);
		float before = vitality(boss);
		int energy = data.energy();
		var ability = artifact ? ArtifactWeaponManager.action(ArtifactAlignment.DARKNESS, "crystal/clone_swarm").ability()
				: CrystalPowerRegistry.get(PowersItems.ORANGE_CRYSTAL);
		if (artifact) {
			helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS, "crystal/clone_swarm", -1),
					"Artifact selection failed");
			helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
					== AbilityActivationService.Result.ACTIVATED, "Artifact Clone Swarm failed");
		} else helper.assertTrue(CrystalPowerRegistry.tryActivate(caster, PowersItems.ORANGE_CRYSTAL), "Orange Crystal failed");
		var echoes = helper.getLevel().getEntitiesOfClass(EchoClone.class, caster.getBoundingBox().inflate(5));
		helper.assertTrue(echoes.size() == 3 && echoes.stream().allMatch(echo ->
				echo.ownerProfile().id().equals(caster.getUUID())), "Cast did not create exactly three owner echoes");
		helper.assertTrue(echoes.stream().allMatch(echo -> echo.getMaxHealth() == (artifact ? 200F : 80F)),
				"Echo health ignored the explicit artifact potency or leaked rank into crystals");
		helper.assertTrue(data.energy() < energy && ActivationCooldowns.remainingTicks(caster, ability) == 1800,
				"Clone Swarm bypassed payment/cooldown");
		if (ownerDeath) {
			helper.runAfterDelay(20, () -> {
				helper.assertTrue(echoes.stream().allMatch(EchoClone::isAlive), "Echoes disappeared before owner death");
				// Exercise the cleanup boundary after death, independently of mock-player damage admission.
				caster.setHealth(0);
				caster.die(caster.damageSources().genericKill());
				helper.assertTrue(!caster.isAlive(), "Owner death fixture did not kill the owner");
			});
			helper.runAfterDelay(40, () -> {
				helper.assertTrue(echoes.stream().allMatch(EchoClone::isRemoved), "Dead owner's echoes retained active entities");
				for (int x = center.x() - 1; x <= center.x() + 1; x++) for (int z = center.z() - 1; z <= center.z() + 1; z++) {
					helper.getLevel().setChunkForced(x, z, false);
				}
				helper.succeed();
			});
			return;
		}
		boolean ally = artifact && type == PowersEntities.DARK_HERALD;
		helper.runAfterDelay(120, () -> helper.assertTrue(ally ? vitality(boss) == before : vitality(boss) < before,
				"Clone engagement: " + vitality(boss) + "/" + before + " bossPos=" + boss.position()
						+ " owner=" + caster.isAlive() + ":" + caster.position()
						+ " echoes=" + echoes.stream().map(echo -> "alive=" + echo.isAlive()
						+ " hp=" + echo.getHealth() + " target=" + echo.getTarget() + " pos=" + echo.position()
						+ " damage=" + echo.getLastDamageSource()).toList()));
		int lifetime = artifact ? 1680 : 1200;
		helper.runAfterDelay(lifetime - 10, () -> helper.assertTrue(echoes.stream().allMatch(EchoClone::isAlive),
				"Echoes vanished early: ownerAlive=" + caster.isAlive() + " ownerPos=" + caster.position()
						+ " echoes=" + echoes.stream().map(echo -> "age=" + echo.tickCount
						+ " alive=" + echo.isAlive() + " removed=" + echo.isRemoved() + " hp=" + echo.getHealth()
						+ " damage=" + echo.getLastDamageSource() + " pos=" + echo.position()).toList()));
		helper.runAfterDelay(lifetime + 5, () -> {
			helper.assertTrue(echoes.stream().allMatch(EchoClone::isRemoved), "Expired echoes retained entities: "
					+ echoes.stream().map(echo -> "age=" + echo.tickCount + " alive=" + echo.isAlive()
					+ " pos=" + echo.position()).toList());
			for (int x = center.x() - 1; x <= center.x() + 1; x++) for (int z = center.z() - 1; z <= center.z() + 1; z++) {
				helper.getLevel().setChunkForced(x, z, false);
			}
			helper.succeed();
		});
	}

	private static float vitality(Mob boss) {
		return boss instanceof FirstVessel vessel ? vessel.effectiveHealth() : boss.getHealth();
	}
}
