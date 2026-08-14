package com.powers.gametest;

import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.ActionRegistryReloadListener;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.network.ActionSubmissionService;
import com.powers.player.ArtifactSelectionState;
import com.powers.player.PlayerPowers;
import com.powers.spell.SpellCastingManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Live production-owner coverage for NET-010 reload, stale submission, casts, and migration. */
public final class ActionRegistryReloadGameTests {
	@GameTest
	public void successfulAndFailedReloadAreAtomic(GameTestHelper helper) {
		var catalogue = MagicRuntime.catalogue();
		long before = catalogue.snapshot().revision();
		helper.assertTrue(ActionRegistryReloadListener.reloadDocuments(catalogue, List.of(document(
				"success", "{\"aliases\":{\"net010_old_fire\":\"fireball\","
						+ "\"net010_old_extension\":\"example_resonant_field\"}}"))),
				"Valid reload was rejected");
		var accepted = catalogue.snapshot();
		helper.assertTrue(accepted.revision() == before + 1L, "Revision did not increase once");
		helper.assertTrue(accepted.definition(new MagicActionId("example_resonant_field")) != null
				&& accepted.resolve("net010_old_extension").value().equals("example_resonant_field"),
				"NET-009 external action was omitted from the reloaded snapshot");
		helper.assertTrue(!ActionRegistryReloadListener.reloadDocuments(catalogue, List.of(document(
				"failed", "{\"aliases\":{\"net010_a\":\"net010_b\",\"net010_b\":\"net010_a\"}}"))),
				"Cyclic reload was accepted");
		helper.assertTrue(catalogue.snapshot() == accepted, "Failed reload partially published");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void staleActionSubmissionRequiresExactlyOneRefresh(GameTestHelper helper) {
		var snapshot = MagicRuntime.catalogue().snapshot();
		var player = helper.makeMockServerPlayerInLevel();
		int energy = PlayerPowers.get(player).energy();
		AtomicInteger refreshes = new AtomicInteger();
		AtomicInteger limiter = new AtomicInteger();
		AtomicInteger mutation = new AtomicInteger();
		var result = ActionSubmissionService.submit(snapshot,
				new ActionSubmissionService.Request(snapshot.revision() - 1L, "fireball"),
				() -> true, refreshes::incrementAndGet,
				() -> { limiter.incrementAndGet(); return true; }, mutation::incrementAndGet);
		helper.assertTrue(result == ActionSubmissionService.Result.REFRESHED
				&& refreshes.get() == 1 && limiter.get() == 0 && mutation.get() == 0,
				"Production submission service did not emit one side-effect-free refresh");
		helper.assertTrue(PlayerPowers.get(player).energy() == energy,
				"Rejected production submission charged the player");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal")
	public void activeCastSnapshotSurvivesReload(GameTestHelper helper) {
		var catalogue = MagicRuntime.catalogue();
		var player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_celestial").getDefaultInstance());
		PlayerPowers.get(player).setSelectedSpell("book_grimoire_celestial", 1);
		SpellCastingManager.use(player, "book_grimoire_celestial");
		helper.assertTrue(SpellCastingManager.isChanneling(player.getUUID()),
				"Real Augury channel did not start");
		var captured = SpellCastingManager.activeRegistrySnapshot(player.getUUID());
		helper.assertTrue(captured != null && ActionRegistryReloadListener.reloadDocuments(catalogue,
				List.of(document("cast", "{\"aliases\":{\"net010_retired_augury\":\"augury\"}}"))),
				"Valid reload was rejected");
		helper.assertTrue(SpellCastingManager.activeRegistrySnapshot(player.getUUID()) == captured
				&& captured.revision() < catalogue.snapshot().revision(),
				"Live channel replaced its captured registry snapshot");
		helper.runAfterDelay(30, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(player.getUUID()),
					"Captured channel did not reach one terminal completion");
			helper.assertTrue(PlayerPowers.get(player).cooldownReadyAt("spell:augury")
						> player.level().getGameTime(), "Captured channel did not commit under its original definition");
			helper.succeed();
		});
	}

	@GameTest
	@SuppressWarnings("removal")
	public void savedArtifactKeyMigratesThroughActualOwner(GameTestHelper helper) {
		var catalogue = MagicRuntime.catalogue();
		helper.assertTrue(ActionRegistryReloadListener.reloadDocuments(catalogue, List.of(document(
				"owners", "{\"aliases\":{"
						+ "\"innate/net010_retired_fire\":\"innate/fireball\","
						+ "\"crystal/net010_retired_inferno\":\"crystal/inferno\","
						+ "\"unique/net010_retired_call\":\"unique/call_hollowed\","
						+ "\"dominion/net010_retired_host\":\"dominion/host_heaven\"}}"))),
				"Alias reload was rejected");
		var player = helper.makeMockServerPlayerInLevel();
		PlayerPowers.get(player).setDarknessLevel(player, 10);
		PlayerPowers.get(player).setSkillLevel(player, 10);
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
		helper.assertTrue(ArtifactSelectionState.select(player, ArtifactAlignment.DARKNESS,
				"crystal/net010_retired_inferno") && "crystal/inferno".equals(
				ArtifactSelectionState.selected(player, ArtifactAlignment.DARKNESS)),
				"Crystal-qualified owner migration lost its namespace");
		helper.assertTrue(ArtifactSelectionState.select(player, ArtifactAlignment.DARKNESS,
				"unique/net010_retired_call") && "unique/call_hollowed".equals(
				ArtifactSelectionState.selected(player, ArtifactAlignment.DARKNESS)),
				"Unique-qualified owner migration lost its namespace");
		helper.assertTrue(ArtifactSelectionState.select(player, ArtifactAlignment.LIGHT,
				"dominion/net010_retired_host") && "dominion/host_heaven".equals(
				ArtifactSelectionState.selected(player, ArtifactAlignment.LIGHT)),
				"Dominion-qualified owner migration lost its namespace");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void savedSpellAndCrystalKeysMigrateThroughPlayerPowers(GameTestHelper helper) {
		var catalogue = MagicRuntime.catalogue();
		helper.assertTrue(ActionRegistryReloadListener.reloadDocuments(catalogue, List.of(document(
				"player_owners", "{\"aliases\":{\"net010_old_augury\":\"augury\","
						+ "\"net010_old_inferno\":\"inferno\"}}"))),
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

	private static ActionRegistryReloadListener.Document document(String id, String json) {
		return new ActionRegistryReloadListener.Document(id,
				com.google.gson.JsonParser.parseString(json).getAsJsonObject());
	}
}
