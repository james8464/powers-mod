package com.powers.gametest;

import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.ActionSubmissionValidation;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.player.ArtifactSelectionState;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Map;
import java.util.List;

/** Live production-owner coverage for NET-010 reload, stale submission, casts, and migration. */
public final class ActionRegistryReloadGameTests {
	@GameTest
	public void successfulAndFailedReloadAreAtomic(GameTestHelper helper) {
		var catalogue = MagicRuntime.catalogue();
		long before = catalogue.snapshot().revision();
		helper.assertTrue(catalogue.reloadAliases(Map.of("net010_old_fire", "fireball")),
				"Valid reload was rejected");
		var accepted = catalogue.snapshot();
		helper.assertTrue(accepted.revision() == before + 1L, "Revision did not increase once");
		helper.assertTrue(!catalogue.reloadAliases(Map.of("net010_a", "net010_b", "net010_b", "net010_a")),
				"Cyclic reload was accepted");
		helper.assertTrue(catalogue.snapshot() == accepted, "Failed reload partially published");
		helper.succeed();
	}

	@GameTest
	public void staleActionSubmissionRequiresExactlyOneRefresh(GameTestHelper helper) {
		var snapshot = MagicRuntime.catalogue().snapshot();
		int refreshes = ActionSubmissionValidation.validate(snapshot,
				snapshot.revision() - 1L, "fireball") == ActionSubmissionValidation.REFRESH ? 1 : 0;
		helper.assertTrue(refreshes == 1, "Stale action did not select exactly one refresh branch");
		helper.succeed();
	}

	@GameTest
	public void activeCastSnapshotSurvivesReload(GameTestHelper helper) {
		var catalogue = MagicRuntime.catalogue();
		var captured = catalogue.snapshot();
		var definition = captured.definition(new MagicActionId("fireball"));
		helper.assertTrue(catalogue.reloadAliases(Map.of("net010_retired_fire", "fireball")),
				"Valid reload was rejected");
		helper.assertTrue(captured.revision() < catalogue.snapshot().revision()
				&& captured.definition(new MagicActionId("fireball")) == definition,
				"Captured cast definition changed across reload");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void savedArtifactKeyMigratesThroughActualOwner(GameTestHelper helper) {
		var catalogue = MagicRuntime.catalogue();
		helper.assertTrue(catalogue.reloadAliases(Map.of("net010_retired_fire", "fireball")),
				"Alias reload was rejected");
		var player = helper.makeMockServerPlayerInLevel();
		com.powers.player.PlayerPowers.get(player).setDarknessLevel(player, 1);
		helper.assertTrue(ArtifactSelectionState.select(player, ArtifactAlignment.DARKNESS,
				"innate/net010_retired_fire"), "Owner rejected a resolvable retired key");
		helper.assertTrue("innate/fireball".equals(
				ArtifactSelectionState.selected(player, ArtifactAlignment.DARKNESS)),
				"Owner did not persist the canonical key");
		helper.assertTrue(ArtifactSelectionState.bindFavourite(player, ArtifactAlignment.DARKNESS,
				0, "innate/net010_retired_fire"), "Favourite owner rejected a resolvable retired key");
		helper.assertTrue("innate/fireball".equals(
				ArtifactSelectionState.favourites(player, ArtifactAlignment.DARKNESS).getFirst()),
				"Favourite owner did not persist and deduplicate the canonical key");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void savedSpellAndCrystalKeysMigrateThroughPlayerPowers(GameTestHelper helper) {
		var catalogue = MagicRuntime.catalogue();
		helper.assertTrue(catalogue.reloadAliases(Map.of(
				"net010_old_augury", "augury", "net010_old_inferno", "inferno")),
				"Player selection alias reload was rejected");
		var data = com.powers.player.PlayerPowers.get(helper.makeMockServerPlayerInLevel());
		data.setSelectedSpellKey("book_grimoire_celestial", "net010_old_augury");
		data.setSelectedCrystalModeKey("rainbow_crystal", "net010_old_inferno");
		helper.assertTrue(data.selectedSpell("book_grimoire_celestial",
				List.of("soul_compass", "augury")) == 1,
				"Spell owner did not migrate its saved action key");
		helper.assertTrue(data.selectedCrystalMode("rainbow_crystal",
				List.of("inferno", "life_bloom")) == 0,
				"Crystal owner did not migrate its saved action key");
		helper.succeed();
	}
}
