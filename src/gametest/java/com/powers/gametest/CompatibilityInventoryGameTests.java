package com.powers.gametest;

import com.powers.PowersWeapons;
import com.powers.item.ArtifactInventoryRuntime;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Live assertions whose topology requires the pinned Inventory Extended jar. */
public final class CompatibilityInventoryGameTests {
	// 26.2 includes the 36 carried slots plus armor, offhand, body and saddle entries.
	private static final int VANILLA_INVENTORY_SIZE = 43;
	private static final int ADDED_SLOT_COUNT = 27;
	private static final int FIRST_ADDED_SLOT = 36;

	@GameTest
	@SuppressWarnings("removal")
	public void addedTopLevelSlotAuthorizesAndRemovalRevokesArtifactState(GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		player.addTag(SkillSystem.DARKNESS_TAG);
		helper.assertTrue(player.getInventory().getContainerSize()
				== VANILLA_INVENTORY_SIZE + ADDED_SLOT_COUNT,
				"Pinned Inventory Extended did not expose exactly 27 additional top-level slots");
		player.getInventory().setItem(FIRST_ADDED_SLOT,
				PowersWeapons.weapon("lycanbane").getDefaultInstance());
		helper.assertTrue(ArtifactWeaponManager.carries(player, ArtifactAlignment.DARKNESS),
				"Artifact in an added top-level slot was not recognized as carried");
		var flight = ArtifactWeaponManager.actions(ArtifactAlignment.DARKNESS).stream()
				.filter(action -> action.definition().key().equals("innate/flight"))
				.findFirst().orElseThrow();
		String key = ArtifactWeaponManager.toggleKey(flight);
		var powers = PlayerPowers.get(player);
		helper.assertTrue(flight.ability().activateToggleOn(player, powers),
				"Added-slot artifact flight setup failed");
		powers.setToggleActive(player, key, true);
		player.getInventory().setItem(FIRST_ADDED_SLOT, ItemStack.EMPTY);
		ArtifactInventoryRuntime.reconcileOwnership(player);
		helper.assertFalse(ArtifactWeaponManager.carries(player, ArtifactAlignment.DARKNESS),
				"Removed added-slot artifact remained authorized");
		helper.assertFalse(powers.isToggleActive(key),
				"Added-slot removal did not revoke its routed toggle");
		helper.assertTrue(powers.flightSnapshot() == -1,
				"Added-slot removal did not clean up routed flight state");
		helper.succeed();
	}
}
