package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersItems;
import com.powers.PowersWeapons;
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

/** Real selected Yellow/relic routes preserve fixed scale and finite encounter support. */
public final class BossSizeShiftGameTests {
	@GameTest
	@SuppressWarnings("removal")
	public void innateArtifactSizeMorphChangesCasterReachNearEveryBoss(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		var origin = helper.absolutePos(new BlockPos(4, 3, 4));
		caster.setPos(origin.getX() + 0.5, 100, origin.getZ() + 0.5);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		var data = PlayerPowers.get(caster);
		data.setDarknessLevel(caster, 1);
		data.forceRestoreEnergy();
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
		var bosses = java.util.List.of(
				helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(7, 3, 4)),
				helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(4, 3, 7)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(1, 3, 4)));
		for (var boss : bosses) {
			boss.setNoAi(true);
			boss.setPos(boss.getX(), 100, boss.getZ());
		}
		var health = bosses.stream().map(Mob::getHealth).toList();
		var reach = net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE;
		double initialReach = caster.getAttributeValue(reach);
		var alignment = ArtifactAlignment.DARKNESS;
		int option = com.powers.power.abilities.SizeMorphRules.scales().indexOf(2.0);
		helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, "innate/size_shift", option),
				"Artifact refused Size Morph selection");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment) == AbilityActivationService.Result.ACTIVATED
				&& Math.abs(caster.getScale() - 2) < 0.0001 && caster.getAttributeValue(reach) > initialReach,
				"Artifact Size Morph did not apply the selected scale and reach");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment) == AbilityActivationService.Result.ACTIVATED
				&& Math.abs(caster.getScale() - 1) < 0.0001 && caster.getAttributeValue(reach) == initialReach,
				"Artifact Size Morph did not restore scale and reach");
		helper.assertTrue(bosses.stream().map(Mob::getHealth).toList().equals(health),
				"Size Morph changed boss health");
		helper.succeed();
	}

	@GameTest(maxTicks = 600)
	public void yellowSupportsLightHeraldEncounter(GameTestHelper helper) { shift(helper, PowersEntities.LIGHT_HERALD, false); }
	@GameTest(maxTicks = 600)
	public void yellowSupportsDarkHeraldEncounter(GameTestHelper helper) { shift(helper, PowersEntities.DARK_HERALD, false); }
	@GameTest(maxTicks = 600)
	public void yellowSupportsFirstVesselEncounter(GameTestHelper helper) { shift(helper, PowersEntities.FIRST_VESSEL, false); }
	@GameTest(maxTicks = 600)
	public void artifactSupportsLightHeraldEncounter(GameTestHelper helper) { shift(helper, PowersEntities.LIGHT_HERALD, true); }
	@GameTest(maxTicks = 600)
	public void artifactSupportsDarkHeraldEncounter(GameTestHelper helper) { shift(helper, PowersEntities.DARK_HERALD, true); }
	@GameTest(maxTicks = 600)
	public void artifactSupportsFirstVesselEncounter(GameTestHelper helper) { shift(helper, PowersEntities.FIRST_VESSEL, true); }

	@SuppressWarnings("removal")
	private static void shift(GameTestHelper helper, EntityType<? extends Mob> type, boolean artifact) {
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		var origin = helper.absolutePos(new BlockPos(4, 3, 4));
		caster.setPos(origin.getX() + 0.5, 100, origin.getZ() + 0.5);
		caster.setNoGravity(true);
		var data = PlayerPowers.get(caster);
		if (artifact) {
			caster.addTag(SkillSystem.DARKNESS_TAG);
			data.setDarknessLevel(caster, 1);
			caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
		} else caster.setItemInHand(InteractionHand.MAIN_HAND, PowersItems.YELLOW_CRYSTAL.getDefaultInstance());
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(6, 3, 4));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setPos(caster.getX() + 1.5, 100, caster.getZ());
		float health = boss.getHealth();
		int energy = data.energy();
		var ability = artifact ? ArtifactWeaponManager.action(ArtifactAlignment.DARKNESS, "crystal/size_shift").ability()
				: CrystalPowerRegistry.get(PowersItems.YELLOW_CRYSTAL);
		if (artifact) {
			helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS, "crystal/size_shift", -1),
					"Artifact selection failed");
			helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
					== AbilityActivationService.Result.ACTIVATED, "Artifact Size Shift refused");
		} else helper.assertTrue(CrystalPowerRegistry.tryActivate(caster, PowersItems.YELLOW_CRYSTAL), "Yellow activation refused");
		helper.assertTrue(Math.abs(caster.getScale() - 0.0625) < 0.0001,
				"Fixed small body scale was not applied through the selected route");
		helper.assertTrue(data.energy() < energy && ActivationCooldowns.remainingTicks(caster, ability) == 600,
				"Size Shift bypassed payment or its authored cooldown");
		helper.assertTrue(boss.getDeltaMovement().horizontalDistanceSqr() > 0,
				"Transformation did not produce its nearby encounter shockwave");
		int duration = artifact ? 560 : 400;
		helper.runAfterDelay(duration - 5, () -> helper.assertTrue(Math.abs(caster.getScale() - 0.0625) < 0.0001,
				"Transformation ended before its route-specific lease"));
		helper.runAfterDelay(duration + 5, () -> {
			helper.assertTrue(Math.abs(caster.getScale() - 1.0) < 0.0001,
					"Expired transformation retained the altered hitbox");
			helper.assertTrue(boss.isAlive() && boss.getHealth() == health,
					"Support transformation unexpectedly damaged the encounter boss");
			helper.succeed();
		});
	}
}
