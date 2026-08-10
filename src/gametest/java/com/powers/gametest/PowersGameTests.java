package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.entity.DarknessCreature;
import com.powers.player.SkillSystem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/** Small runtime suite for mechanics that pure unit tests cannot exercise. */
public final class PowersGameTests {
	public PowersGameTests() {
	}

	@GameTest
	public void darknessCreatureHonorsRuntimeFactionTags(GameTestHelper helper) {
		DarknessCreature creature = helper.spawn(PowersEntities.DARKNESS_CREATURE, new BlockPos(1, 1, 1));
		Player target = helper.makeMockPlayer(GameType.SURVIVAL);
		target.addTag(SkillSystem.DARKNESS_TAG);
		helper.assertFalse(creature.canAttack(target), "Darkness guardians attacked their own faction");
		target.removeTag(SkillSystem.DARKNESS_TAG);
		helper.assertTrue(creature.canAttack(target), "Darkness guardians ignored a non-dark target");
		helper.succeed();
	}

}
