package com.powers.player;

import com.powers.PowersItems;
import com.powers.PowersWeapons;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.network.ActionSubmissionService;
import com.powers.network.CrystalSelectorPackets;
import com.powers.network.PacketRateLimiter;
import com.powers.network.ShadowSwordPackets;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Registered packet and transport-boundary coverage for NET-010 rejection ordering. */
@SuppressWarnings("removal")
public final class ActionSubmissionPacketGameTests {
	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void staleCycleDoesNotMigrateRawSelectionOrConsumeArtifactLane(GameTestHelper helper) {
		ServerPlayer player = authorizedShadowOwner(helper);
		var catalogue = MagicRuntime.catalogue();
		catalogue.reloadAliases(java.util.Map.of("innate/net010_old_fire", "innate/fireball"));
		long stale = catalogue.snapshot().revision() - 1L;
		player.setAttached(PlayerPowerAttachments.SHADOW_SWORD_SELECTION, "innate/net010_old_fire");
		int energy = PlayerPowers.get(player).energy();
		long cooldown = player.level().getGameTime() + 100L;
		PlayerPowers.get(player).setCooldown("powers:fireball", cooldown);
		List<Object> payloads = captureClientboundPayloads(player);
		PacketRateLimiter.clearGlobal();

		player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(
				new ShadowSwordPackets.CyclePayload(stale, "darkness", "innate/fireball", 1)));
		helper.runAfterDelay(2, () -> {
			helper.assertTrue("innate/net010_old_fire".equals(player.getAttachedOrElse(
					PlayerPowerAttachments.SHADOW_SWORD_SELECTION, "")),
					"Stale cycle migrated the raw saved selection before validation");
			helper.assertTrue(PlayerPowers.get(player).energy() == energy,
					"Stale cycle changed energy");
			helper.assertTrue(PlayerPowers.get(player).cooldownReadyAt("powers:fireball") == cooldown,
					"Stale cycle changed cooldown state");
			assertLaneUntouched(helper, player, PacketRateLimiter.Lane.ARTIFACT);
			assertOneRefresh(helper, payloads, "artifact");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void staleTeleportDoesNotMigrateRawSelectionOrConsumeTravelLane(GameTestHelper helper) {
		ServerPlayer player = authorizedShadowOwner(helper);
		var catalogue = MagicRuntime.catalogue();
		catalogue.reloadAliases(java.util.Map.of("innate/net010_old_time", "innate/time_shift"));
		long stale = catalogue.snapshot().revision() - 1L;
		player.setAttached(PlayerPowerAttachments.SHADOW_SWORD_SELECTION, "innate/net010_old_time");
		var before = player.position();
		int energy = PlayerPowers.get(player).energy();
		long cooldown = player.level().getGameTime() + 100L;
		PlayerPowers.get(player).setCooldown("powers:time_shift", cooldown);
		List<Object> payloads = captureClientboundPayloads(player);
		PacketRateLimiter.clearGlobal();

		player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(
				new ShadowSwordPackets.TeleportPayload(stale, "darkness", "innate/time_shift",
						player.getX() + 8.0, player.getY(), player.getZ(), player.level().dimension(), "")));
		helper.runAfterDelay(2, () -> {
			helper.assertTrue("innate/net010_old_time".equals(player.getAttachedOrElse(
					PlayerPowerAttachments.SHADOW_SWORD_SELECTION, "")),
					"Stale teleport migrated the raw saved selection before validation");
			helper.assertTrue(player.position().equals(before) && PlayerPowers.get(player).energy() == energy,
					"Stale teleport changed action state or energy");
			helper.assertTrue(PlayerPowers.get(player).cooldownReadyAt("powers:time_shift") == cooldown,
					"Stale teleport changed cooldown state");
			assertLaneUntouched(helper, player, PacketRateLimiter.Lane.TRAVEL);
			assertOneRefresh(helper, payloads, "artifact");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void crystalOwnerLossSendsOnlyExplicitCrystalInvalidation(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, PowersItems.RAINBOW_CRYSTAL.getDefaultInstance());
		long revision = MagicRuntime.catalogue().snapshot().revision();
		player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
		List<Object> payloads = captureClientboundPayloads(player);

		player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(
				new CrystalSelectorPackets.SelectPayload(revision, "inferno")));
		helper.runAfterDelay(2, () -> {
			assertOneRefresh(helper, payloads, "crystal");
			helper.succeed();
		});
	}

	private static ServerPlayer authorizedShadowOwner(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
		PlayerPowers.get(player).setDarknessLevel(player, 10);
		return player;
	}

	private static void assertLaneUntouched(GameTestHelper helper, ServerPlayer player,
			PacketRateLimiter.Lane lane) {
		for (int request = 0; request < lane.limit(); request++) {
			helper.assertTrue(PacketRateLimiter.allow(player, lane), "Rejected packet consumed limiter capacity");
		}
		helper.assertFalse(PacketRateLimiter.allow(player, lane), "Limiter did not enforce its authored capacity");
	}

	private static void assertOneRefresh(GameTestHelper helper, List<Object> payloads, String surface) {
		List<ActionSubmissionService.RefreshPayload> refreshes = payloads.stream()
				.filter(ActionSubmissionService.RefreshPayload.class::isInstance)
				.map(ActionSubmissionService.RefreshPayload.class::cast).toList();
		helper.assertTrue(payloads.size() == 1 && refreshes.size() == 1
				&& surface.equals(refreshes.getFirst().surface()),
				"Rejected packet did not send exactly one " + surface + " invalidation: " + payloads);
	}

	private static List<Object> captureClientboundPayloads(ServerPlayer player) {
		try {
			Field listenerConnection = player.connection.getClass().getSuperclass()
					.getDeclaredField("connection");
			listenerConnection.setAccessible(true);
			Connection connection = (Connection) listenerConnection.get(player.connection);
			Field channelField = Connection.class.getDeclaredField("channel");
			channelField.setAccessible(true);
			io.netty.channel.Channel channel = (io.netty.channel.Channel) channelField.get(connection);
			List<Object> payloads = new ArrayList<>();
			channel.pipeline().addLast("net010_capture_" + System.identityHashCode(payloads),
					new ChannelDuplexHandler() {
						@Override
						public void write(ChannelHandlerContext context, Object message, ChannelPromise promise)
								throws Exception {
							if (message instanceof ClientboundCustomPayloadPacket custom) {
								payloads.add(custom.payload());
							}
							super.write(context, message, promise);
						}
					});
			return payloads;
		} catch (ReflectiveOperationException error) {
			throw new AssertionError("Could not observe the real clientbound connection", error);
		}
	}
}
