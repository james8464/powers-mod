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

/** Owner-addressed companion state and authenticated interaction payloads. */
public final class CompanionPackets {
	private static final int MAX_DIALOGUE_LENGTH = 256;

	private CompanionPackets() {
	}

	/** A complete client state; inactive records remove the local apparition. */
	public record StatePayload(long sessionId, boolean active, boolean teleport,
			double x, double y, double z, float yaw, String dialogue)
			implements CustomPacketPayload {
		public static final Type<StatePayload> TYPE = new Type<>(PowersMod.id("companion_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.LONG, StatePayload::sessionId,
						ByteBufCodecs.BOOL, StatePayload::active,
						ByteBufCodecs.BOOL, StatePayload::teleport,
						ByteBufCodecs.DOUBLE, StatePayload::x,
						ByteBufCodecs.DOUBLE, StatePayload::y,
						ByteBufCodecs.DOUBLE, StatePayload::z,
						ByteBufCodecs.FLOAT, StatePayload::yaw,
						ByteBufCodecs.STRING_UTF8, StatePayload::dialogue,
						StatePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** The owner asks to speak to the exact private session they can see. */
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

	public static void sendState(ServerPlayer owner, long sessionId, boolean active,
			boolean teleport, double x, double y, double z, float yaw, String dialogue) {
		String safeDialogue = dialogue == null ? "" : dialogue.substring(
				0, Math.min(MAX_DIALOGUE_LENGTH, dialogue.length()));
		ServerPlayNetworking.send(owner, new StatePayload(sessionId, active, teleport,
				x, y, z, yaw, safeDialogue));
	}
}
