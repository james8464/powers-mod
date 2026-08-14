package com.powers.gametest;

import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.ActionRegistryReloadListener;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.network.ActionSubmissionService;
import com.powers.player.ArtifactSelectionState;
import com.powers.player.PlayerPowers;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.spell.SpellCastingManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** Live production-owner coverage for NET-010 reload, stale submission, casts, and migration. */
public final class ActionRegistryReloadGameTests {
	@GameTest(maxTicks = 200)
	public void successfulAndFailedReloadAreAtomic(GameTestHelper helper) {
		helper.runAfterDelay(10, () -> {
			var catalogue = MagicRuntime.catalogue();
			long before = catalogue.snapshot().revision();
			var server = helper.getLevel().getServer();
			server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((ignored, failure) ->
				server.execute(() -> {
					if (failure != null) throw new AssertionError("Registered Fabric reload failed", failure);
					var accepted = catalogue.snapshot();
					helper.assertTrue(accepted.revision() == before + 1L,
							"Registered listener did not publish exactly one revision");
					helper.assertTrue(accepted.definition(new MagicActionId("example_resonant_field")) != null,
							"NET-009 external action was omitted from the resource-reloaded snapshot");
					helper.assertTrue("fireball".equals(accepted.resolve("net010_pack_fire").value()),
							"Real datapack alias did not pass through the registered parser/apply path");
					verifyRegisteredFailedReload(helper, server, catalogue, accepted);
				}));
		});
	}

	private static void verifyRegisteredFailedReload(GameTestHelper helper,
			net.minecraft.server.MinecraftServer server,
			com.powers.magic.MagicActionCatalogue catalogue,
			com.powers.magic.ActionRegistrySnapshot accepted) {
		try {
			var resource = ActionRegistryReloadGameTests.class.getClassLoader()
					.getResource("data/powers/powers_actions/net010_live.json");
			if (resource == null || !"file".equals(resource.getProtocol())) {
				throw new AssertionError("Live GameTest action resource is not writable");
			}
			Path path = Path.of(resource.toURI());
			String valid = Files.readString(path);
			Files.writeString(path,
					"{\"aliases\":{\"net010_cycle_a\":\"net010_cycle_b\","
							+ "\"net010_cycle_b\":\"net010_cycle_a\"}}");
			server.reloadResources(server.getPackRepository().getSelectedIds())
					.whenComplete((ignored, failure) -> {
						Throwable restoreFailure = null;
						try {
							Files.writeString(path, valid);
						} catch (java.io.IOException error) {
							restoreFailure = error;
						}
						Throwable finalRestoreFailure = restoreFailure;
						server.execute(() -> {
							if (finalRestoreFailure != null) {
								throw new AssertionError("Could not restore live GameTest resource",
										finalRestoreFailure);
							}
							helper.assertTrue(failure != null,
									"Registered Fabric reload future accepted a cyclic resource");
							helper.assertTrue(catalogue.snapshot() == accepted
									&& catalogue.snapshot().revision() == accepted.revision(),
									"Failed registered reload changed snapshot identity or revision");
							helper.succeed();
						});
					});
		} catch (java.io.IOException | java.net.URISyntaxException error) {
			throw new AssertionError("Could not prepare invalid live action resource", error);
		}
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
	public void activeCastSnapshotSurvivesReloadAndCompletesAuthoredEffect(GameTestHelper helper) {
		var catalogue = MagicRuntime.catalogue();
		var player = helper.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		player.snapTo(origin, 0.0F, 0.0F);
		var target = helper.spawn(com.powers.PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		target.setNoAi(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_deep").getDefaultInstance());
		SpellCastingManager.use(player, "book_grimoire_deep");
		helper.assertTrue(SpellCastingManager.isChanneling(player.getUUID()),
				"Real Dimensional Anchor channel did not start");
		var captured = SpellCastingManager.activeRegistrySnapshot(player.getUUID());
		helper.assertTrue(captured != null && ActionRegistryReloadListener.reloadDocuments(catalogue,
				List.of(document("cast", "{\"aliases\":{\"net010_retired_anchor\":\"dimensional_anchor\"}}"))),
				"Valid reload was rejected");
		helper.assertTrue(SpellCastingManager.activeRegistrySnapshot(player.getUUID()) == captured
				&& captured.revision() < catalogue.snapshot().revision(),
				"Live channel replaced its captured registry snapshot");
		helper.runAfterDelay(60, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(player.getUUID()),
					"Captured channel did not reach one terminal completion");
			helper.assertTrue(DimensionalAnchorAbility.isAnchored(target),
					"Captured channel did not execute its authored completion effect");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 30, padding = 128)
	@SuppressWarnings("removal")
	public void invalidContinuationCancelsExactlyOnceWithoutCompletion(GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		player.snapTo(origin, 0.0F, 0.0F);
		var target = helper.spawn(com.powers.PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		target.setNoAi(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_deep").getDefaultInstance());
		int before = PlayerPowers.get(player).energy();
		SpellCastingManager.use(player, "book_grimoire_deep");
		helper.assertTrue(SpellCastingManager.isChanneling(player.getUUID()), "Channel did not begin");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.runAfterDelay(4, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(player.getUUID()),
					"Owner loss did not cancel the channel");
			helper.assertTrue(PlayerPowers.get(player).energy() == before - 11,
					"Cancellation did not retain exactly one half-payment");
			helper.assertFalse(DimensionalAnchorAbility.isAnchored(target),
					"Cancelled channel executed its authored effect");
			int cancelledEnergy = PlayerPowers.get(player).energy();
			helper.runAfterDelay(5, () -> {
				helper.assertTrue(PlayerPowers.get(player).energy() == cancelledEnergy,
						"Invalid continuation was cancelled more than once");
				helper.assertFalse(DimensionalAnchorAbility.isAnchored(target),
						"Cancelled channel completed later");
				helper.succeed();
			});
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
