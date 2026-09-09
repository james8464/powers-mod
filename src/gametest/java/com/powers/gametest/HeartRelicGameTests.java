package com.powers.gametest;

import com.powers.ImportedPackItems;
import com.powers.item.ImportedArtifactItem;
import com.powers.item.ImportedArtifactRuntime;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Live proof for aligned heart consumption and exclusive strongest-heart passives. */
public final class HeartRelicGameTests {
	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void eatingHonorsAlignmentAndConsumesExactlyOne(GameTestHelper helper) {
		ServerPlayer light = helper.makeMockServerPlayerInLevel();
		light.setGameMode(GameType.SURVIVAL);
		light.setHealth(light.getMaxHealth() - 10.0F);
		ItemStack living = ImportedPackItems.item(
				"imported_artifact_beating_heart").getDefaultInstance();
		light.setItemInHand(InteractionHand.MAIN_HAND, living);
		ImportedArtifactItem livingItem = (ImportedArtifactItem) living.getItem();
		helper.assertTrue(livingItem.use(light.level(), light, InteractionHand.MAIN_HAND)
				.consumesAction(), "Living Heart did not begin its eating animation for Light");
		livingItem.finishUsingItem(living, light.level(), light);
		helper.assertTrue(living.isEmpty(), "Living Heart was not consumed exactly once");
		helper.assertTrue(light.getHealth() > light.getMaxHealth() - 10.0F,
				"Living Heart did not apply its authored healing after eating");

		ServerPlayer darkness = helper.makeMockServerPlayerInLevel();
		darkness.setGameMode(GameType.SURVIVAL);
		darkness.addTag(SkillSystem.DARKNESS_TAG);
		ItemStack forbidden = ImportedPackItems.item(
				"imported_artifact_beating_heart").getDefaultInstance();
		darkness.setItemInHand(InteractionHand.MAIN_HAND, forbidden);
		helper.assertTrue(((ImportedArtifactItem) forbidden.getItem()).use(
				darkness.level(), darkness, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
				"Darkness began eating a Light-aligned heart");
		helper.assertTrue(forbidden.getCount() == 1, "Rejected heart use consumed an item");

		ItemStack ghoul = ImportedPackItems.item(
				"imported_artifact_ghoul_heart").getDefaultInstance();
		darkness.setItemInHand(InteractionHand.MAIN_HAND, ghoul);
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(darkness);
		powers.emptyEnergy();
		ImportedArtifactItem ghoulItem = (ImportedArtifactItem) ghoul.getItem();
		helper.assertTrue(ghoulItem.use(darkness.level(), darkness, InteractionHand.MAIN_HAND)
				.consumesAction(), "Ghoul Heart did not begin eating for Darkness");
		ghoulItem.finishUsingItem(ghoul, darkness.level(), darkness);
		helper.assertTrue(ghoul.isEmpty() && powers.energy() == 30,
				"Ghoul Heart did not consume once and restore exactly 30 energy");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void strongestCarriedHeartExclusivelyOwnsPassive(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		for (String id : java.util.List.of(
				"imported_artifact_beating_heart", "imported_artifact_ghoul_heart",
				"imported_artifact_woodheart", "imported_artifact_heart_mechanism",
				"imported_artifact_bloodstone")) {
			player.getInventory().add(ImportedPackItems.item(id).getDefaultInstance());
		}
		player.removeAllEffects();
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(player);
		powers.emptyEnergy();
		ImportedArtifactRuntime.tickPlayer(player, 20);
		helper.assertTrue(player.hasEffect(MobEffects.RESISTANCE),
				"The strongest Bloodstone did not own the sole heart passive");
		helper.assertFalse(player.hasEffect(MobEffects.REGENERATION),
				"A weaker living heart stacked regeneration under Bloodstone");
		helper.assertFalse(player.hasEffect(MobEffects.ABSORPTION),
				"A weaker clockwork heart stacked absorption under Bloodstone");
		helper.assertTrue(powers.energy() == 0,
				"A weaker ghoul heart stacked energy restoration under Bloodstone");
		helper.succeed();
	}
}
