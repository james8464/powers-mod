package com.powers.network;

import com.powers.PowersMod;
import com.powers.companion.PrivateCompanionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Authenticated Shadow control and client-local apparition state. */
public final class CompanionPackets {
	private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
			(buf, value) -> {
				buf.writeLong(value.getMostSignificantBits());
				buf.writeLong(value.getLeastSignificantBits());
			}, buf -> new UUID(buf.readLong(), buf.readLong()));
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> DIMENSION_CODEC =
			ByteBufCodecs.stringUtf8(128);

	private CompanionPackets() {
	}

	/** A complete client state; inactive records remove the local apparition. */
	public record StatePayload(UUID ownerId, long sessionId, boolean active, boolean teleport,
			String dimension, double x, double y, double z, float yaw)
			implements CustomPacketPayload {
		public static final Type<StatePayload> TYPE = new Type<>(PowersMod.id("companion_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> STREAM_CODEC =
				StreamCodec.composite(
						UUID_CODEC, StatePayload::ownerId,
						ByteBufCodecs.LONG, StatePayload::sessionId,
						ByteBufCodecs.BOOL, StatePayload::active,
						ByteBufCodecs.BOOL, StatePayload::teleport,
						DIMENSION_CODEC, StatePayload::dimension,
						ByteBufCodecs.DOUBLE, StatePayload::x,
						ByteBufCodecs.DOUBLE, StatePayload::y,
						ByteBufCodecs.DOUBLE, StatePayload::z,
						ByteBufCodecs.FLOAT, StatePayload::yaw,
						StatePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** The owner toggles its own apparition; arbitrary session IDs are ignored. */
	public record InteractPayload(long sessionId) implements CustomPacketPayload {
		public static final Type<InteractPayload> TYPE = new Type<>(PowersMod.id("companion_interact"));
		public static final StreamCodec<RegistryFriendlyByteBuf, InteractPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.LONG, InteractPayload::sessionId,
						InteractPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(InteractPayload.TYPE, InteractPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(InteractPayload.TYPE, (payload, context) ->
				context.server().execute(() -> {
					ServerPlayer player = context.player();
					if (PacketRateLimiter.allow(player, PacketRateLimiter.Lane.COMPANION)) {
						PrivateCompanionManager.interact(player, payload.sessionId());
					}
				}));
	}

	public static void sendState(ServerPlayer recipient, UUID ownerId, long sessionId,
			boolean active, boolean teleport, String dimension,
			double x, double y, double z, float yaw) {
		if (!ServerPlayNetworking.canSend(recipient, StatePayload.TYPE)) return;
		ServerPlayNetworking.send(recipient, new StatePayload(ownerId, sessionId, active,
				teleport, dimension, x, y, z, yaw));
	}
}
