package com.powers.network;

import com.powers.power.abilities.VesselPossessionAbility;
import com.powers.testing.network.PacketFaultController;
import com.powers.testing.network.PacketFaultFamilies;
import com.powers.testing.network.PacketFaultFamily;
import com.powers.testing.network.PacketFaultStreams;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Project-owned play transport seam; ordinary disabled traffic takes the direct Fabric path. */
public final class PowersPlayNetworking {
	@FunctionalInterface
	public interface Handler<T extends CustomPacketPayload> {
		void receive(T payload, ServerPlayer player);
	}

	private PowersPlayNetworking() {
	}

	public static <T extends CustomPacketPayload> void registerReceiver(
			CustomPacketPayload.Type<T> type, Handler<T> handler) {
		if (!PacketFaultFamilies.isProjectOwned(type)) {
			ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
					ServerPlayCallback.execute(context, player -> handler.receive(payload, player)));
			return;
		}
		PacketFaultFamily family = PacketFaultFamilies.classify(type);
		ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
				ServerPlayCallback.execute(context, player -> {
					if (!PacketFaultController.enabled(context.server())) {
						handler.receive(payload, player);
						return;
					}
					PacketFaultController.receive(player, family, PacketFaultStreams.key(payload), payload,
							(current, value) -> handler.receive(value, current),
							() -> failClosed(context.server(), player.getUUID(), family));
				}));
	}

	public static boolean send(ServerPlayer player, CustomPacketPayload payload) {
		return send(player, payload, () -> { });
	}

	public static boolean send(ServerPlayer player, CustomPacketPayload payload, Runnable failure) {
		if (!PacketFaultFamilies.isProjectOwned(payload.type())
				|| !PacketFaultController.enabled(player.level().getServer())) {
			ServerPlayNetworking.send(player, payload);
			return true;
		}
		return PacketFaultController.send(player, PacketFaultFamilies.classify(payload),
				PacketFaultStreams.key(payload), payload,
				(current, value) -> {
					ServerPlayNetworking.send(current, value);
					return true;
				}, failure);
	}

	/** Unfaulted invalidation used only after an injected request was intentionally lost. */
	static boolean sendCritical(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
		return true;
	}

	private static void failClosed(net.minecraft.server.MinecraftServer server,
			java.util.UUID owner, PacketFaultFamily family) {
		ServerPlayer player = server.getPlayerList().getPlayer(owner);
		if (player == null) return;
		switch (family) {
			case ARTIFACT_SELECTION, ARTIFACT_BINDING ->
					ActionSubmissionService.refreshCritical(player, "artifact");
			case GRIMOIRE_SELECTION -> ActionSubmissionService.refreshCritical(player, "grimoire");
			case CRYSTAL_SELECTION -> ActionSubmissionService.refreshCritical(player, "crystal");
			case VESSEL_RELEASE -> VesselPossessionAbility.releaseControlledSession(player);
			default -> { }
		}
	}
}
