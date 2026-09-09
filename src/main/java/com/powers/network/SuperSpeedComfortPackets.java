package com.powers.network;

import com.powers.PowersMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Owner-only lifecycle signal for local Super Speed camera comfort. */
public final class SuperSpeedComfortPackets {
	private static final int MAX_DURATION_TICKS = 2400;

	private SuperSpeedComfortPackets() {
	}

	public record Payload(boolean active, int remainingTicks) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(PowersMod.id("super_speed_comfort"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC =
				StreamCodec.of(Payload::encode, Payload::decode);

		public Payload {
			if (remainingTicks < 0 || remainingTicks > MAX_DURATION_TICKS
					|| active == (remainingTicks == 0)) {
				throw new IllegalArgumentException("Invalid Super Speed comfort payload");
			}
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		private static void encode(RegistryFriendlyByteBuf buffer, Payload payload) {
			buffer.writeBoolean(payload.active);
			buffer.writeVarInt(payload.remainingTicks);
		}

		private static Payload decode(RegistryFriendlyByteBuf buffer) {
			return new Payload(buffer.readBoolean(), buffer.readVarInt());
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.STREAM_CODEC);
	}

	public static void send(ServerPlayer player, boolean active, int remainingTicks) {
		if (player != null && ServerPlayNetworking.canSend(player, Payload.TYPE)) {
			PowersPlayNetworking.send(player, new Payload(active, active ? remainingTicks : 0));
		}
	}
}
