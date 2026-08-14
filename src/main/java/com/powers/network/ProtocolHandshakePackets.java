package com.powers.network;

import com.powers.PowersMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.FabricServerConfigurationPacketListenerImpl;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Rejects missing or incompatible POWERS clients during configuration, before
 * Minecraft starts accepting any of the mod's play-stage packets.
 */
public final class ProtocolHandshakePackets {
	private static final int TIMEOUT_TICKS = 200;
	private static final Map<ServerConfigurationPacketListenerImpl, HandshakeTask> PENDING =
			new ConcurrentHashMap<>();

	private ProtocolHandshakePackets() {
	}

	public record Challenge(int protocol, String modVersion) implements CustomPacketPayload {
		public static final Type<Challenge> TYPE = new Type<>(PowersMod.id("protocol_challenge"));
		public static final StreamCodec<FriendlyByteBuf, Challenge> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Challenge::protocol,
				ByteBufCodecs.stringUtf8(ProtocolHandshakeRules.MAX_VERSION_LENGTH), Challenge::modVersion,
				Challenge::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record Response(int protocol, String modVersion) implements CustomPacketPayload {
		public static final Type<Response> TYPE = new Type<>(PowersMod.id("protocol_response"));
		public static final StreamCodec<FriendlyByteBuf, Response> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Response::protocol,
				ByteBufCodecs.stringUtf8(ProtocolHandshakeRules.MAX_VERSION_LENGTH), Response::modVersion,
				Response::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundConfiguration().register(Challenge.TYPE, Challenge.STREAM_CODEC);
		PayloadTypeRegistry.serverboundConfiguration().register(Response.TYPE, Response.STREAM_CODEC);
		ServerConfigurationNetworking.registerGlobalReceiver(Response.TYPE,
				ProtocolHandshakePackets::handleResponse);
		ServerConfigurationConnectionEvents.CONFIGURE.register(ProtocolHandshakePackets::begin);
		ServerConfigurationConnectionEvents.DISCONNECT.register((listener, server) -> PENDING.remove(listener));
	}

	public static String modVersion() {
		return FabricLoader.getInstance().getModContainer(PowersMod.MOD_ID)
				.orElseThrow(() -> new IllegalStateException("POWERS mod metadata is unavailable"))
				.getMetadata().getVersion().getFriendlyString();
	}

	private static void begin(ServerConfigurationPacketListenerImpl listener,
			net.minecraft.server.MinecraftServer server) {
		if (!ServerConfigurationNetworking.canSend(listener, Challenge.TYPE)) {
			listener.disconnect(Component.literal(
					"This server requires the same POWERS mod version as the server ("
							+ modVersion() + ")."));
			return;
		}
		HandshakeTask task = new HandshakeTask(listener);
		PENDING.put(listener, task);
		((FabricServerConfigurationPacketListenerImpl) listener).addTask(task);
	}

	private static void handleResponse(Response payload, ServerConfigurationNetworking.Context context) {
		ServerConfigurationPacketListenerImpl listener = context.packetListener();
		long epoch = com.powers.util.ServerCallbackGate.capture(context.server());
		com.powers.util.ServerCallbackGate.execute(epoch, server -> {
			HandshakeTask task = PENDING.remove(listener);
			if (task == null) {
				listener.disconnect(Component.literal("Unexpected POWERS configuration response."));
				return;
			}
			ProtocolHandshakeRules.Result result = ProtocolHandshakeRules.validate(
					ProtocolHandshakeRules.CURRENT_PROTOCOL, payload.protocol(), payload.modVersion());
			if (!result.accepted()) {
				listener.disconnect(Component.literal(result.message()));
				return;
			}
			((FabricServerConfigurationPacketListenerImpl) listener).completeTask(HandshakeTask.TYPE);
		});
	}

	private static final class HandshakeTask implements ConfigurationTask {
		private static final Type TYPE = new Type(PowersMod.MOD_ID + ":protocol_handshake");
		private final ServerConfigurationPacketListenerImpl listener;
		private int age;

		private HandshakeTask(ServerConfigurationPacketListenerImpl listener) {
			this.listener = listener;
		}

		@Override
		public void start(Consumer<Packet<?>> sender) {
			sender.accept(ServerConfigurationNetworking.createClientboundPacket(new Challenge(
					ProtocolHandshakeRules.CURRENT_PROTOCOL, modVersion())));
		}

		@Override
		public boolean tick() {
			if (++age < TIMEOUT_TICKS) return false;
			PENDING.remove(listener, this);
			listener.disconnect(Component.literal(
					"POWERS protocol handshake timed out. Install the same POWERS version as the server."));
			return false;
		}

		@Override
		public Type type() {
			return TYPE;
		}
	}
}
