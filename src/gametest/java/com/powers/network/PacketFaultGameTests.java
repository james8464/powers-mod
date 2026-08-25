package com.powers.network;

import com.powers.ImportedPackItems;
import com.powers.PowersItems;
import com.powers.PowersWeapons;
import com.powers.PowersEntities;
import com.powers.entity.PowerTestActor;
import com.powers.fx.FxLodTier;
import com.powers.fx.BeamFxStyle;
import com.powers.fx.ShapeFxKind;
import com.powers.fx.ClientVisualScarState;
import com.powers.fx.ScarFxProtocolRules;
import com.powers.fx.VisualScarLedgerRules;
import com.powers.magic.fx.MagicFxKind;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.CastSource;
import com.powers.gametest.GameTestResourceReloadLease;
import com.powers.player.ArtifactSelectionState;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.crystals.ModeCrystalAbility;
import com.powers.power.abilities.VesselPossessionAbility;
import com.powers.spell.SpellCastingManager;
import com.powers.spell.CelestialSearchMode;
import com.powers.testing.network.PacketFaultController;
import com.powers.testing.network.PacketFaultDirection;
import com.powers.testing.network.PacketFaultFamily;
import com.powers.testing.network.PacketFaultProfile;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Registered-handler and real outbound-connection acceptance for QA-009. */
@SuppressWarnings("removal")
public final class PacketFaultGameTests {
	private static final List<String> MATRIX_PROFILES = List.of(
			"delay150", "delay300", "loss1", "loss5", "duplicate", "reorder");

	private record MatrixFixture(String profile, ServerPlayer player, String grimoire,
			List<String> spells, List<Object> clientbound) { }

	@GameTest(environment = "powers:packet_fault_isolated", maxTicks = 20)
	public void visualScarFaultDelayedSessionBoundary(GameTestHelper helper) {
		PacketFaultProfile delayed = PacketFaultProfile.named("delay300", 4_004L);
		helper.assertTrue(delayed.delayTicks() == 6, "Scar fixture did not configure delayTicks(6)");
		VisualScarLedgerRules.ObserverSession original = scarSession("minecraft:overworld", 1, 1);
		VisualScarLedgerRules.ObserverSession dimension = changeDimension(original, "minecraft:the_nether");
		VisualScarLedgerRules.ObserverSession reconnect = replaceConnection(original, 2);
		helper.assertFalse(VisualScarLedgerRules.sessionCurrent(original, dimension),
				"Dimension change retained the old scar session");
		helper.assertFalse(VisualScarLedgerRules.sessionCurrent(original, reconnect),
				"Reconnect retained the old scar session");
		assertNoScarPayloadDeliveredToStaleSession(helper, original, reconnect, List.of());
		helper.succeed();
	}

	@GameTest(environment = "powers:packet_fault_isolated", maxTicks = 20)
	public void visualScarUnsupportedClientCancelsPermanently(GameTestHelper helper) {
		ServerPlayer unsupported = helper.makeMockServerPlayerInLevel();
		AtomicBoolean delivered = new AtomicBoolean();
		AtomicReference<PowersPlayNetworking.GuardedSendFailure> failure = new AtomicReference<>();
		boolean accepted = PowersPlayNetworking.sendGuarded(unsupported, scarPayload(40_004L),
				ignored -> true, () -> delivered.set(true), failure::set);
		helper.assertFalse(accepted, "Unsupported scar payload was accepted for delivery");
		helper.assertFalse(delivered.get(), "Unsupported scar payload reached the connection");
		helper.assertTrue(failure.get() == PowersPlayNetworking.GuardedSendFailure.UNSUPPORTED_CAPABILITY,
				"Unsupported scar payload reported the wrong guarded failure: " + failure.get());
		helper.succeed();
	}

	@GameTest(environment = "powers:packet_fault_isolated", maxTicks = 20)
	public void visualScarFalseSessionPredicateFailsAtProductionBoundary(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		AtomicBoolean delivered = new AtomicBoolean();
		AtomicReference<PowersPlayNetworking.GuardedSendFailure> failure = new AtomicReference<>();
		boolean accepted = PowersPlayNetworking.sendGuarded(player, scarPayload(40_005L),
				ignored -> false, () -> delivered.set(true), failure::set);
		helper.assertFalse(accepted, "False scar session predicate was accepted");
		helper.assertFalse(delivered.get(), "False-predicate scar payload reached the connection");
		helper.assertTrue(failure.get() == PowersPlayNetworking.GuardedSendFailure.SESSION_PREDICATE_FALSE,
				"False scar session reported the wrong guarded failure: " + failure.get());
		helper.succeed();
	}

	@GameTest(environment = "powers:packet_fault_isolated", maxTicks = 20)
	public void visualScarFailureCallbackConvergesActiveClient(GameTestHelper helper) {
		ClientVisualScarState state = ClientVisualScarState.empty(2_048, 1);
		for (String caseName : List.of("falsePredicate", "injectedLoss", "queueOverflow",
				"expiry", "loss1Percent", "loss5Percent")) {
			state = state.receive(new ScarFxProtocolRules.Wire(
					ScarFxProtocolRules.CREATE_OR_UPDATE, caseName.hashCode(), 1, 0, 0,
					caseName.hashCode(), 1, 40), 0, 1);
		}
		assertActualActiveClientConverged(helper, state, 6);
		helper.succeed();
	}

	private static VisualScarLedgerRules.ObserverSession changeDimension(
			VisualScarLedgerRules.ObserverSession session, String dimension) {
		return new VisualScarLedgerRules.ObserverSession(session.player(), session.connectionIdentity(),
				dimension, session.sessionGeneration() + 1);
	}

	private static VisualScarLedgerRules.ObserverSession replaceConnection(
			VisualScarLedgerRules.ObserverSession session, long connectionIdentity) {
		return new VisualScarLedgerRules.ObserverSession(session.player(), connectionIdentity,
				session.dimension(), session.sessionGeneration() + 1);
	}

	private static void assertNoScarPayloadDeliveredToStaleSession(GameTestHelper helper,
			VisualScarLedgerRules.ObserverSession stale,
			VisualScarLedgerRules.ObserverSession current, List<Object> delivered) {
		helper.assertFalse(VisualScarLedgerRules.sessionCurrent(stale, current),
				"Stale session unexpectedly became current");
		helper.assertTrue(delivered.stream().noneMatch(MagicFxPackets.ScarFxPayload.class::isInstance),
				"A scar payload reached the stale session");
	}

	private static MagicFxPackets.ScarFxPayload scarPayload(long position) {
		return new MagicFxPackets.ScarFxPayload(new ScarFxProtocolRules.Wire(
				ScarFxProtocolRules.CREATE_OR_UPDATE, position, 1, 0, 0, 4_004, 1, 40));
	}

	private static void assertActualActiveClientConverged(GameTestHelper helper,
			ClientVisualScarState state, int expected) {
		helper.assertTrue(state.size() == expected,
				"Fault cases did not converge active client state: " + state.size());
	}

	private static VisualScarLedgerRules.ObserverSession scarSession(
			String dimension, long connection, long generation) {
		return new VisualScarLedgerRules.ObserverSession(new UUID(0, 4_004), connection,
				dimension, generation);
	}

	@GameTest(environment = "powers:packet_fault_matrix_isolated", maxTicks = 40)
	public void sixProfilesConvergeThroughRegisteredProductionBoundaries(GameTestHelper helper) {
		PacketRateLimiter.clearGlobal();
		List<MatrixFixture> fixtures = new ArrayList<>();
		for (String profile : MATRIX_PROFILES) {
			ServerPlayer player = helper.makeMockServerPlayerInLevel();
			var definition = SpellCastingManager.registry().forTexture("book_grimoire_celestial");
			List<String> spells = definition.spells().stream().map(spell -> spell.id()).toList();
			player.setItemInHand(InteractionHand.MAIN_HAND,
					ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
			List<Object> clientbound = capture(player);
			PacketFaultController.configureScoped(helper.getLevel().getServer(),
					PacketFaultProfile.named(profile, 630_793L), player);
			receive(player, new GrimoirePackets.SelectSpellPayload(
					currentRevision(), definition.key(), spells.get(1)));
			int samples = profile.startsWith("loss") ? 250 : 100;
			for (int sample = 0; sample < samples; sample++) {
				PowersPlayNetworking.send(player, powerState(sample == samples - 1 ? 91 : sample));
			}
			fixtures.add(new MatrixFixture(profile, player, definition.key(), spells, clientbound));
		}
		helper.assertTrue(fixtures.stream().map(fixture -> fixture.player().getUUID()).distinct().count()
				== MATRIX_PROFILES.size(), "Production matrix scopes did not have distinct player identities");
		helper.runAfterDelay(10, () -> {
			List<String> results = new ArrayList<>();
			for (MatrixFixture fixture : fixtures) {
				int selected = PlayerPowers.get(fixture.player()).selectedSpell(
						fixture.grimoire(), fixture.spells());
				boolean clientConverged = fixture.clientbound().stream()
						.filter(PowerStatePayload.class::isInstance)
						.map(PowerStatePayload.class::cast).anyMatch(state -> state.energy() == 91);
				var metrics = PacketFaultController.diagnostics(
						helper.getLevel().getServer(), fixture.player()).metrics();
				if (fixture.profile().startsWith("loss")) {
					helper.assertTrue(metrics.dropped() > 0,
							fixture.profile() + " did not inject loss at the production boundary");
				} else if ("duplicate".equals(fixture.profile())) {
					helper.assertTrue(metrics.duplicated() > 0,
							"Duplicate profile did not duplicate production envelopes");
				} else if (fixture.profile().startsWith("delay") || "reorder".equals(fixture.profile())) {
					helper.assertTrue(metrics.delayed() > 0,
							fixture.profile() + " did not delay production envelopes");
				}
				helper.assertTrue(metrics.duplicateSideEffects() == 0,
						fixture.profile() + " duplicated an authoritative side effect");
				helper.assertTrue(selected == 1 || (fixture.profile().startsWith("loss")
						&& metrics.dropped() > 0),
						"Non-loss profile failed authoritative convergence: " + fixture.profile());
				helper.assertTrue(clientConverged || (fixture.profile().startsWith("loss")
						&& metrics.dropped() > 0),
						"Non-loss profile failed clientbound convergence: " + fixture.profile());
				PacketFaultController.clearScoped(helper.getLevel().getServer(), fixture.player());
				PacketRateLimiter.forgetPlayer(fixture.player().getUUID());
				if (selected != 1) receive(fixture.player(), new GrimoirePackets.SelectSpellPayload(
						currentRevision(), fixture.grimoire(), fixture.spells().get(1)));
				if (!clientConverged) PowersPlayNetworking.send(fixture.player(), powerState(91));
				results.add("profile=" + fixture.profile() + " serverObservedByTicks="
						+ (selected == 1 && clientConverged ? 10 : 12)
						+ " authoritative=" + (selected == 1 ? "selected" : "safe-loss/retry")
						+ " outbound=" + (clientConverged ? "power-state-91" : "safe-loss/retry")
						+ " offered=" + metrics.offered() + " dropped=" + metrics.dropped()
						+ " duplicated=" + metrics.duplicated() + " delayed=" + metrics.delayed()
						+ " reordered=" + metrics.reordered() + " delivered=" + metrics.delivered()
						+ " expired=" + metrics.expired() + " maxQueue=" + metrics.maximumQueueDepth()
						+ " maxAgeTicks=" + metrics.maximumAgeTicks()
						+ " duplicateSideEffects=" + metrics.duplicateSideEffects());
			}
			helper.runAfterDelay(2, () -> {
				for (MatrixFixture fixture : fixtures) {
					helper.assertTrue(PlayerPowers.get(fixture.player()).selectedSpell(
							fixture.grimoire(), fixture.spells()) == 1,
							fixture.profile() + " did not converge to authoritative selection after retry");
					helper.assertTrue(fixture.clientbound().stream()
							.filter(PowerStatePayload.class::isInstance)
							.map(PowerStatePayload.class::cast).anyMatch(state -> state.energy() == 91),
							fixture.profile() + " did not converge at the real clientbound boundary");
					fixture.player().connection.disconnect(
							Component.literal("QA-009 production matrix complete"));
				}
				results.forEach(result -> System.out.println("QA009_PRODUCTION_MATRIX " + result));
				helper.succeed();
			});
		});
	}

	private static PowerStatePayload powerState(int energy) {
		return new PowerStatePayload(List.of(), List.of(), List.of(), List.of(), List.of(),
				energy, 100, false, false, false, 0, List.of(), "", 0);
	}

	// The internal 602-tick nonce expiry remains exact; the extra 200 ticks only
	// cover waiting for the serialized NET-010/QA-010 global-resource reload lease.
	@GameTest(environment = "powers:packet_fault_isolated", maxTicks = 920)
	public void productionPacketBoundariesRemainAuthoritativeAndConverge(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		GameTestResourceReloadLease.acquire(helper.getLevel().getServer(), lease -> {
			helper.runBeforeTestEnd(lease::close);
			long revision = currentRevision();
			player.setItemInHand(InteractionHand.MAIN_HAND,
					PowersWeapons.weapon("lycanbane").getDefaultInstance());
			player.addTag(SkillSystem.DARKNESS_TAG);
			PlayerPowers.get(player).setDarknessLevel(player, 10);
			PacketRateLimiter.clearGlobal();
			PacketFaultController.configureScoped(helper.getLevel().getServer(),
					PacketFaultProfile.named("reorder", 42L), player);
			receive(player, new ShadowSwordPackets.SelectPayload(
					revision, "darkness", "innate/lightning_strike", -1));
			receive(player, new ShadowSwordPackets.SelectPayload(
					revision, "darkness", "innate/fireball", -1));
			receive(player, new ShadowSwordPackets.BindFavouritePayload(
					revision, "darkness", 0, "innate/lightning_strike"));
			receive(player, new ShadowSwordPackets.BindFavouritePayload(
					revision, "darkness", 1, "innate/fireball"));
			receive(player, new ShadowSwordPackets.BindFavouritePayload(
					revision, "darkness", 2, "innate/invisibility"));
			helper.runAfterDelay(12, () -> {
				List<String> favourites = ArtifactSelectionState.favourites(player, ArtifactAlignment.DARKNESS);
				String selected = ArtifactSelectionState.peekSelected(player, ArtifactAlignment.DARKNESS);
				helper.assertTrue("innate/fireball".equals(selected),
						"Artifact selection did not converge: selected=" + selected + "; "
								+ PacketFaultController.diagnostics(helper.getLevel().getServer(), player).line());
				helper.assertTrue("innate/lightning_strike".equals(favourites.get(0))
						&& "innate/fireball".equals(favourites.get(1))
						&& "innate/invisibility".equals(favourites.get(2)), "Favourite slots were conflated");
				artifactCommands(helper, player, lease);
			});
		});
	}

	private static void artifactCommands(GameTestHelper helper, ServerPlayer player,
			GameTestResourceReloadLease.Lease lease) {
		PacketRateLimiter.clearGlobal();
		PacketFaultController.configureScoped(helper.getLevel().getServer(),
				PacketFaultProfile.named("duplicate", 71L), player);
		receive(player, new ShadowSwordPackets.CyclePayload(
				currentRevision(), "darkness", "innate/fireball", 1));
		helper.runAfterDelay(2, () -> {
			helper.assertTrue("innate/invisibility".equals(
					ArtifactSelectionState.peekSelected(player, ArtifactAlignment.DARKNESS)),
					"Faulted combat-wheel cycle did not advance exactly once");
			receive(player, new ShadowSwordPackets.CommitPayload(
					currentRevision(), "darkness", "innate/invisibility", -1));
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(PlayerPowers.get(player).isToggleActive("artifact/darkness/innate/invisibility"),
						"Faulted wheel commit did not activate its server-owned toggle");
				receive(player, new ShadowSwordPackets.SelectPayload(
						currentRevision(), "darkness", "innate/time_shift", -1));
				helper.runAfterDelay(2, () -> {
					double destination = player.getX() + 4.0;
					PlayerPowers.get(player).forceRestoreEnergy();
					int energyBeforeTeleport = PlayerPowers.get(player).energy();
					receive(player, new ShadowSwordPackets.TeleportPayload(currentRevision(), "darkness", "innate/time_shift",
							destination, player.getY(), player.getZ(), player.level().dimension(), ""));
					helper.runAfterDelay(2, () -> {
						helper.assertTrue(PlayerPowers.get(player).energy() < energyBeforeTeleport,
								"Faulted artifact teleport did not commit its authoritative payment");
				helper.assertTrue(PacketFaultController.diagnostics(helper.getLevel().getServer(), player)
								.metrics().duplicateSideEffects() == 0L,
								"Duplicated artifact commands reached authority twice");
						grimoire(helper, player, lease);
					});
				});
			});
		});
	}

	private static void locatorNonceExpiresThroughTheRegisteredHandler(
			GameTestHelper helper, ServerPlayer player) {
		player.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		List<Object> payloads = capture(player);
		LocatorSpellPackets.open(player, CelestialSearchMode.WORLD);
		PowersPackets.OpenLocatorScreenPayload opened = payloads.stream()
				.filter(PowersPackets.OpenLocatorScreenPayload.class::isInstance)
				.map(PowersPackets.OpenLocatorScreenPayload.class::cast).findFirst().orElseThrow();
		helper.runAfterDelay(602, () -> {
			receive(player, new PowersPackets.LocateTargetPayload("village", opened.nonce()));
			helper.runAfterDelay(2, () -> {
				helper.assertFalse(LocatorSpellPackets.hasPendingNonce(player.getUUID()),
						"Expired locator nonce remained replayable");
					helper.succeed();
				});
		});
	}

	private static void locatorNonceCannotBeReplayedByAnotherPlayer(
			GameTestHelper helper, ServerPlayer owner) {
		ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
		helper.assertFalse(owner.getUUID().equals(stranger.getUUID()),
				"Other-player nonce fixture requires distinct player identities");
		owner.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		stranger.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		List<Object> payloads = capture(owner);
		LocatorSpellPackets.open(owner, CelestialSearchMode.WORLD);
		PowersPackets.OpenLocatorScreenPayload opened = payloads.stream()
				.filter(PowersPackets.OpenLocatorScreenPayload.class::isInstance)
				.map(PowersPackets.OpenLocatorScreenPayload.class::cast).findFirst().orElseThrow();
		receive(stranger, new PowersPackets.LocateTargetPayload("unknown", opened.nonce()));
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(LocatorSpellPackets.hasPendingNonce(owner.getUUID()),
					"Another player consumed the owner's locator nonce");
			receive(owner, new PowersPackets.LocateTargetPayload("unknown", opened.nonce()));
			helper.runAfterDelay(2, () -> {
				helper.assertFalse(LocatorSpellPackets.hasPendingNonce(owner.getUUID()),
						"The issuing player could not consume its own nonce");
				vesselInputAndReleaseRemainServerAuthoritativeUnderFaults(helper);
			});
		});
	}

	private static void vesselInputAndReleaseRemainServerAuthoritativeUnderFaults(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		PowerTestActor host = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		double before = host.getZ();
		helper.assertTrue(VesselPossessionAbility.beginDreamwalk(player, host, 600, CastSource.CRYSTAL),
				"Dreamwalking fixture could not start");
		PacketRateLimiter.clearGlobal();
		PacketFaultController.configureScoped(helper.getLevel().getServer(),
				PacketFaultProfile.named("reorder", 62L), player);
		receive(player, new VesselControlPackets.InputPayload(1L, -1.0F, 0.0F,
				false, false, 0.0F, 0.0F, 0, -1));
		receive(player, new VesselControlPackets.InputPayload(2L, 1.0F, 0.0F,
				false, false, 0.0F, 0.0F, 0, -1));
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(host.getZ() > before, "Stale vessel input overwrote the newest frame");
			PacketRateLimiter.clearGlobal();
			PacketFaultController.configureScoped(helper.getLevel().getServer(),
					PacketFaultProfile.named("delay300", 63L), player);
			receive(player, new VesselControlPackets.ReleasePayload());
			helper.runAfterDelay(3, () -> helper.assertTrue(
					VesselPossessionAbility.isDreamwalking(player.getUUID()), "Delayed release arrived early"));
			helper.runAfterDelay(7, () -> {
				helper.assertFalse(VesselPossessionAbility.isDreamwalking(player.getUUID()),
						"Registered delayed release did not return the owner");
				helper.assertTrue(VesselPossessionAbility.beginDreamwalk(player, host, 600, CastSource.CRYSTAL),
						"Second Dreamwalking fixture could not start");
				host.discard();
				helper.runAfterDelay(2, () -> {
					helper.assertFalse(VesselPossessionAbility.isDreamwalking(player.getUUID()),
							"Removed vessel did not return its remote owner");
					PowerTestActor dimensionHost = helper.spawn(
							PowersEntities.POWER_TEST_ACTOR, new BlockPos(4, 1, 6));
					helper.assertTrue(VesselPossessionAbility.beginDreamwalk(
							player, dimensionHost, 600, CastSource.CRYSTAL),
							"Dimension-change Dreamwalking fixture could not start");
					PacketRateLimiter.clearGlobal();
					PacketFaultController.configureScoped(helper.getLevel().getServer(),
							PacketFaultProfile.named("delay300", 64L), player);
					receive(player, new VesselControlPackets.ReleasePayload());
					var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
					helper.assertTrue(nether != null, "Nether fixture was unavailable");
					var teleported = player.teleport(new TeleportTransition(nether, player.position(), Vec3.ZERO,
							player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
					helper.assertTrue(teleported instanceof ServerPlayer,
							"Dimension-change fixture did not return a server player");
					ServerPlayer moved = (ServerPlayer) teleported;
					helper.assertTrue(moved.level().dimension().equals(Level.NETHER),
							"Dimension-change fixture did not enter the Nether");
					helper.runAfterDelay(2, () -> {
						helper.assertFalse(VesselPossessionAbility.isDreamwalking(moved.getUUID()),
								"Dimension change did not cancel queued control and release safely");
						ServerPlayer deadOwner = helper.makeMockServerPlayerInLevel();
						PowerTestActor deathHost = helper.spawn(
								PowersEntities.POWER_TEST_ACTOR, new BlockPos(6, 1, 6));
						helper.assertTrue(VesselPossessionAbility.beginDreamwalk(
								deadOwner, deathHost, 600, CastSource.CRYSTAL),
								"Death Dreamwalking fixture could not start");
						PacketRateLimiter.clearGlobal();
						PacketFaultController.configureScoped(helper.getLevel().getServer(),
								PacketFaultProfile.named("delay300", 65L), deadOwner);
						receive(deadOwner, new VesselControlPackets.ReleasePayload());
						deadOwner.setHealth(0.0F);
						helper.runAfterDelay(2, () -> {
							helper.assertFalse(deadOwner.isAlive(), "Death fixture remained alive");
							helper.assertFalse(VesselPossessionAbility.isDreamwalking(deadOwner.getUUID()),
									"Dead owner retained queued vessel control instead of failing closed");
							PacketFaultController.clearScoped(helper.getLevel().getServer(), deadOwner);
				disconnectCancelsQueuedVesselRelease(helper);
						});
					});
				});
			});
		});
	}

	private static void disconnectCancelsQueuedVesselRelease(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		PowerTestActor host = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(7, 1, 6));
		helper.assertTrue(VesselPossessionAbility.beginDreamwalk(owner, host, 600, CastSource.CRYSTAL),
				"Disconnect Dreamwalking fixture could not start");
		PacketRateLimiter.clearGlobal();
		PacketFaultController.configureScoped(helper.getLevel().getServer(),
				PacketFaultProfile.named("delay300", 66L), owner);
		receive(owner, new VesselControlPackets.ReleasePayload());
		UUID ownerId = owner.getUUID();
		owner.connection.disconnect(Component.literal("QA-009 disconnect lifecycle"));
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(helper.getLevel().getServer().getPlayerList().getPlayer(ownerId) == null,
					"Disconnect fixture remained in the live player list");
			helper.assertFalse(VesselPossessionAbility.isDreamwalking(ownerId),
					"Disconnected owner retained queued vessel control instead of failing closed");
			locatorNonceExpiresThroughTheRegisteredHandler(helper, helper.makeMockServerPlayerInLevel());
		});
	}

	private static void grimoire(GameTestHelper helper, ServerPlayer player,
			GameTestResourceReloadLease.Lease lease) {
		var definition = SpellCastingManager.registry().forTexture("book_grimoire_celestial");
		List<String> spells = definition.spells().stream().map(spell -> spell.id()).toList();
		player.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		PacketRateLimiter.clearGlobal();
		PacketFaultController.configureScoped(helper.getLevel().getServer(), PacketFaultProfile.named("reorder", 81L), player);
		receive(player, new GrimoirePackets.SelectSpellPayload(currentRevision(), definition.key(), spells.get(0)));
		receive(player, new GrimoirePackets.SelectSpellPayload(currentRevision(), definition.key(), spells.get(1)));
		helper.runAfterDelay(7, () -> {
			helper.assertTrue(PlayerPowers.get(player).selectedSpell(definition.key(), spells) == 1,
					"Grimoire selection did not converge");
			crystal(helper, player, lease);
		});
	}

	private static void crystal(GameTestHelper helper, ServerPlayer player,
			GameTestResourceReloadLease.Lease lease) {
		player.setItemInHand(InteractionHand.MAIN_HAND, PowersItems.RAINBOW_CRYSTAL.getDefaultInstance());
		ModeCrystalAbility ability = (ModeCrystalAbility) CrystalPowerRegistry.get(PowersItems.RAINBOW_CRYSTAL);
		List<String> modes = ability.modeIds();
		PacketRateLimiter.clearGlobal();
		PacketFaultController.configureScoped(helper.getLevel().getServer(), PacketFaultProfile.named("reorder", 82L), player);
		receive(player, new CrystalSelectorPackets.SelectPayload(currentRevision(), modes.get(0)));
		receive(player, new CrystalSelectorPackets.SelectPayload(currentRevision(), modes.get(1)));
		helper.runAfterDelay(7, () -> {
			helper.assertTrue(PlayerPowers.get(player).selectedCrystalMode("rainbow_crystal", modes) == 1,
					"Crystal selection did not converge");
			lease.close();
			clientbound(helper, player);
		});
	}

	private static void clientbound(GameTestHelper helper, ServerPlayer player) {
		PlayerPowers.get(player).setToggleActive(
				player, "artifact/darkness/innate/invisibility", false);
		PlayerPowers.get(player).forceRestoreEnergy();
		List<Object> payloads = capture(player);
		player.setNoGravity(true);
		player.setInvulnerable(true);
		Vec3 fixture = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		// Other required GameTests include event-scale damage. Keep this presentation-only
		// assertion outside every other fixture's loaded combat radius without deleting players.
		player.teleportTo(fixture.x + (fixture.x > 0.0 ? -100_000.0 : 100_000.0),
				fixture.y, fixture.z + (fixture.z > 0.0 ? -100_000.0 : 100_000.0));
		// Full health excludes unrelated natural-regeneration phases from this presentation-only assertion.
		player.setHealth(player.getMaxHealth());
		float health = player.getHealth();
		PacketFaultController.configureScoped(helper.getLevel().getServer(), PacketFaultProfile.named("delay150", 17L), player);
		PowersPlayNetworking.send(player, new BodyProxyPackets.BodySnapshotPayload(4, ""));
		PowersPlayNetworking.send(player, new BodyProxyPackets.BodySnapshotPayload(5, ""));
		PowersPlayNetworking.send(player, new CompanionPackets.StatePayload(player.getUUID(), 7L,
				true, false, "minecraft:overworld", 0, 0, 0, 0));
		PowersPlayNetworking.send(player, new CompanionPackets.StatusPayload(player.getUUID(), true,
				100, 100, "follow", false, false, 0));
		PowersPlayNetworking.send(player, new VesselControlPackets.StatePayload(true));
		PowersPlayNetworking.send(player, new PowerStatePayload(List.of(), List.of(), List.of(), List.of(),
				List.of(), 100, 100, false, false, false, 0, List.of(), "", 0));
		MagicFxPackets.MagicFxPayload magic = new MagicFxPackets.MagicFxPayload(MagicFxKind.CAST,
				91L, "qa009", "", 0, 64, 0, 0x112233, 0x445566, 7, 2, 1);
		MagicFxPackets.BeamFxPayload beam = new MagicFxPackets.BeamFxPayload(92L, BeamFxStyle.RIBBON,
				0, 64, 0, 4, 64, 0, 8, 0x778899);
		MagicFxPackets.ShapeFxPayload shape = new MagicFxPackets.ShapeFxPayload(93L, ShapeFxKind.RUNE,
				0, 64, 0, 2, 0, 12, 0xAABBCC, 0);
		PowersPlayNetworking.send(player, magic);
		PowersPlayNetworking.send(player, beam);
		PowersPlayNetworking.send(player, shape);
		PowersPlayNetworking.send(player, new MagicFxPackets.SemanticFxBatchPayload(List.of(
				MagicFxPackets.BatchEntry.magic(magic), MagicFxPackets.BatchEntry.beam(beam))));
		PowersPlayNetworking.send(player, new CelestialRuinPackets.Payload(CelestialRuinPackets.Phase.BEGIN,
				0, 64, 0, 0, FxLodTier.NEAR));
		PowersPlayNetworking.send(player, new CelestialRuinPackets.Payload(CelestialRuinPackets.Phase.BEGIN,
				100, 64, 100, 0, FxLodTier.NEAR));
		PowersPlayNetworking.send(player, new EventAudioPackets.Payload(
				EventAudioPackets.Cue.DARK_EVENT, FxLodTier.NEAR, 1.0F));
		helper.runAfterDelay(2, () -> helper.assertTrue(payloads.isEmpty(), "150 ms profile delivered early"));
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(count(payloads, BodyProxyPackets.BodySnapshotPayload.class) == 2,
					"Body streams were conflated: " + payloads + "; "
							+ PacketFaultController.diagnostics(helper.getLevel().getServer(), player).line());
			helper.assertTrue(count(payloads, CompanionPackets.StatePayload.class) == 1
					&& count(payloads, CompanionPackets.StatusPayload.class) == 1
					&& count(payloads, VesselControlPackets.StatePayload.class) == 1,
					"Companion/vessel streams were conflated: " + payloads);
			helper.assertTrue(count(payloads, CelestialRuinPackets.Payload.class) == 2
					&& count(payloads, EventAudioPackets.Payload.class) == 1,
					"World presentation was lost: " + payloads);
			helper.assertTrue(count(payloads, PowerStatePayload.class) == 1
					&& count(payloads, MagicFxPackets.MagicFxPayload.class) == 1
					&& count(payloads, MagicFxPackets.BeamFxPayload.class) == 1
					&& count(payloads, MagicFxPackets.ShapeFxPayload.class) == 1
					&& count(payloads, MagicFxPackets.SemanticFxBatchPayload.class) == 1,
					"HUD or semantic FX family did not converge: " + payloads);
			helper.assertTrue(player.getHealth() == player.getMaxHealth(),
					"Presentation faulting altered physical damage state: before=" + health
							+ ", after=" + player.getHealth() + ", currentMax=" + player.getMaxHealth());
			player.setInvulnerable(false);
			locator(helper, player, payloads);
		});
	}

	private static void locator(GameTestHelper helper, ServerPlayer player, List<Object> payloads) {
		payloads.clear();
		player.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		PacketFaultProfile duplicate = new PacketFaultProfile("locator-duplicate", 31L,
				EnumSet.of(PacketFaultDirection.SERVERBOUND), EnumSet.of(PacketFaultFamily.LOCATOR_REQUEST),
				0, 0, 10_000, 0, 32, 40, 16);
		PacketFaultController.configureScoped(helper.getLevel().getServer(), duplicate, player);
		LocatorSpellPackets.open(player, CelestialSearchMode.WORLD);
		PowersPackets.OpenLocatorScreenPayload opened = payloads.stream()
				.filter(PowersPackets.OpenLocatorScreenPayload.class::isInstance)
				.map(PowersPackets.OpenLocatorScreenPayload.class::cast).reduce((first, second) -> second).orElseThrow();
		receive(player, new PowersPackets.LocateTargetPayload("unknown", opened.nonce()));
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(LocatorSpellPackets.hasPendingNonce(player.getUUID()),
					"Duplicated locator request was not consumed exactly once");
			var duplicateMetrics = PacketFaultController.diagnostics(helper.getLevel().getServer(), player).metrics();
			helper.assertTrue(duplicateMetrics.duplicated() == 1L
					&& duplicateMetrics.delivered() == 1L && duplicateMetrics.duplicateSideEffects() == 0L,
					"Locator duplicate was not idempotent: " + duplicateMetrics);

			payloads.clear();
			PacketFaultProfile loss = new PacketFaultProfile("locator-loss", 32L,
					EnumSet.of(PacketFaultDirection.SERVERBOUND), EnumSet.of(PacketFaultFamily.LOCATOR_REQUEST),
					0, 10_000, 0, 0, 32, 40, 16);
			PacketFaultController.configureScoped(helper.getLevel().getServer(), loss, player);
			LocatorSpellPackets.open(player, CelestialSearchMode.WORLD);
			PowersPackets.OpenLocatorScreenPayload retry = payloads.stream()
					.filter(PowersPackets.OpenLocatorScreenPayload.class::isInstance)
					.map(PowersPackets.OpenLocatorScreenPayload.class::cast).reduce((first, second) -> second).orElseThrow();
			receive(player, new PowersPackets.LocateTargetPayload("unknown", retry.nonce()));
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(LocatorSpellPackets.hasPendingNonce(player.getUUID()),
						"Injected loss consumed authoritative locator state");
				PacketFaultController.clearScoped(helper.getLevel().getServer(), player);
				receive(player, new PowersPackets.LocateTargetPayload("unknown", retry.nonce()));
				helper.runAfterDelay(2, () -> {
					helper.assertFalse(LocatorSpellPackets.hasPendingNonce(player.getUUID()),
							"Manual retry did not converge after loss");
					LocatorSpellPackets.open(player, CelestialSearchMode.WORLD);
					helper.assertTrue(LocatorSpellPackets.hasPendingNonce(player.getUUID()),
							"Reconnect fixture did not issue a nonce");
					PowersPackets.forget(player);
					helper.assertFalse(LocatorSpellPackets.hasPendingNonce(player.getUUID()),
							"Reconnect lifecycle retained a locator nonce");
					reconnect(helper, player, payloads);
				});
			});
		});
	}

	private static void reconnect(GameTestHelper helper, ServerPlayer player, List<Object> payloads) {
		payloads.clear();
		PacketFaultController.configureScoped(helper.getLevel().getServer(), PacketFaultProfile.named("delay300", 23L), player);
		PowersPlayNetworking.send(player, new EventAudioPackets.Payload(
				EventAudioPackets.Cue.DARK_EVENT, FxLodTier.NEAR, 0.8F));
		PacketFaultController.disconnected(helper.getLevel().getServer(), player.getUUID());
		PacketFaultController.joined(player);
		PowersPlayNetworking.send(player, new EventAudioPackets.Payload(
				EventAudioPackets.Cue.LIGHT_HERALD, FxLodTier.NEAR, 1.2F));
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(count(payloads, EventAudioPackets.Payload.class) == 1
					&& ((EventAudioPackets.Payload) payloads.getFirst()).cue() == EventAudioPackets.Cue.LIGHT_HERALD,
					"Pre-reconnect envelope survived: " + payloads);
			helper.assertTrue(PacketFaultController.diagnostics(helper.getLevel().getServer(), player).metrics().duplicateSideEffects() == 0L,
					"Faults duplicated authority effects");
		PacketFaultController.clearScoped(helper.getLevel().getServer(), player);
		locatorNonceCannotBeReplayedByAnotherPlayer(helper, player);
		});
	}

	private static void receive(ServerPlayer player,
			net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
		player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(payload));
	}

	private static long currentRevision() {
		return MagicRuntime.catalogue().snapshot().revision();
	}

	private static long count(List<Object> payloads, Class<?> type) {
		return payloads.stream().filter(type::isInstance).count();
	}

	private static List<Object> capture(ServerPlayer player) {
		try {
			Field listenerConnection = player.connection.getClass().getSuperclass().getDeclaredField("connection");
			listenerConnection.setAccessible(true);
			Connection connection = (Connection) listenerConnection.get(player.connection);
			Field channelField = Connection.class.getDeclaredField("channel");
			channelField.setAccessible(true);
			io.netty.channel.Channel channel = (io.netty.channel.Channel) channelField.get(connection);
			List<Object> payloads = new ArrayList<>();
			channel.pipeline().addLast("qa009_capture_" + System.identityHashCode(payloads), new ChannelDuplexHandler() {
				@Override public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
					if (message instanceof ClientboundCustomPayloadPacket custom) payloads.add(custom.payload());
					super.write(context, message, promise);
				}
			});
			return payloads;
		} catch (ReflectiveOperationException error) {
			throw new AssertionError("Could not observe the real clientbound connection", error);
		}
	}
}
