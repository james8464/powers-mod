package com.powers.gametest;

import com.powers.ImportedPackItems;
import com.powers.item.ImportedArtifactRuntime;
import com.powers.player.PlayerPowers;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

/** Live inventory proof for bounded strongest-per-school relic attunements. */
public final class ArtifactAttunementGameTests {
	@GameTest
	@SuppressWarnings("removal")
	public void strongestRecoveryAndWoundedSchoolsApplyOnce(GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		player.getInventory().add(ImportedPackItems.item(
				"imported_artifact_plain_copper_ring").getDefaultInstance());
		player.getInventory().add(ImportedPackItems.item(
				"imported_artifact_corroded_copper_ring").getDefaultInstance());
		player.getInventory().add(ImportedPackItems.item(
				"imported_artifact_plain_copper_ring").getDefaultInstance());
		player.getInventory().add(ImportedPackItems.item(
				"imported_artifact_diamond_ring").getDefaultInstance());
		player.getInventory().add(ImportedPackItems.item(
				"imported_artifact_emerald_ring").getDefaultInstance());
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(player);
		powers.emptyEnergy();
		player.setHealth(player.getMaxHealth() * 0.5F);

		ImportedArtifactRuntime.tickPlayer(player, 20);

		helper.assertTrue(powers.energy() == 2,
				"Recovery school stacked weaker or duplicate rings: " + powers.energy());
		helper.assertTrue(player.hasEffect(MobEffects.RESISTANCE)
				&& player.getEffect(MobEffects.RESISTANCE).getAmplifier() == 1,
				"Warding school did not apply bounded Resistance II");
		helper.assertTrue(player.hasEffect(MobEffects.REGENERATION)
				&& player.getEffect(MobEffects.REGENERATION).getAmplifier() == 0,
				"Verdancy school did not apply bounded Regeneration I");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void duplicateChannelingAmuletsRecoverOnlyTwoEnergy(GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		player.getInventory().add(ImportedPackItems.item(
				"imported_artifact_amulet").getDefaultInstance());
		player.getInventory().add(ImportedPackItems.item(
				"imported_artifact_amulet").getDefaultInstance());
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(player);
		powers.emptyEnergy();
		powers.regenerateEnergy(powers.energyCapacity() / 2);
		int before = powers.energy();
		player.setItemInHand(InteractionHand.MAIN_HAND, Items.APPLE.getDefaultInstance());
		player.startUsingItem(InteractionHand.MAIN_HAND);

		ImportedArtifactRuntime.tickPlayer(player, 20);

		helper.assertTrue(player.isUsingItem(), "Fixture did not establish the channeling condition");
		helper.assertTrue(powers.energy() == before + 2,
				"Duplicate Channeling amulets stacked beyond +2 energy: " + powers.energy());
		helper.succeed();
	}
}
