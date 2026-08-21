package com.powers.network;

import com.powers.PowersMod;
import com.powers.power.abilities.VesselPossessionAbility;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Bounded input and lifecycle packets for an authenticated Vessel Possession session. */
public final class VesselControlPackets {
	private static final VesselControlSequence INPUT_SEQUENCES = new VesselControlSequence();
	private VesselControlPackets() {
	}

	public record StatePayload(boolean active) implements CustomPacketPayload {
		public static final Type<StatePayload> TYPE = new Type<>(PowersMod.id("vessel_control_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.BOOL, StatePayload::active, StatePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Authenticated owner request to leave the currently controlled remote form. */
	public record ReleasePayload() implements CustomPacketPayload {
		public static final Type<ReleasePayload> TYPE =
				new Type<>(PowersMod.id("vessel_control_release"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ReleasePayload> STREAM_CODEC =
				StreamCodec.of((buffer, payload) -> { }, buffer -> new ReleasePayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record InputPayload(long sequence, float forward, float strafe, boolean jump, boolean crouch,
			float yaw, float pitch, int hotbarSlot, int attackEntityId)
			implements CustomPacketPayload {
		public static final Type<InputPayload> TYPE = new Type<>(PowersMod.id("vessel_control_input"));
		public static final StreamCodec<RegistryFriendlyByteBuf, InputPayload> STREAM_CODEC =
				StreamCodec.of(InputPayload::encode, InputPayload::decode);

		private static void encode(RegistryFriendlyByteBuf buffer, InputPayload input) {
			buffer.writeVarLong(input.sequence);
			buffer.writeFloat(input.forward);
			buffer.writeFloat(input.strafe);
			buffer.writeBoolean(input.jump);
			buffer.writeBoolean(input.crouch);
			buffer.writeFloat(input.yaw);
			buffer.writeFloat(input.pitch);
			buffer.writeVarInt(input.hotbarSlot);
			buffer.writeVarInt(input.attackEntityId + 1);
		}

		private static InputPayload decode(RegistryFriendlyByteBuf buffer) {
			return new InputPayload(buffer.readVarLong(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(),
					buffer.readBoolean(), buffer.readFloat(), buffer.readFloat(),
					buffer.readVarInt(), buffer.readVarInt() - 1);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(InputPayload.TYPE, InputPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ReleasePayload.TYPE, ReleasePayload.STREAM_CODEC);
		PowersPlayNetworking.registerReceiver(InputPayload.TYPE, (payload, owner) -> {
					if (INPUT_SEQUENCES.accept(owner.getUUID(), payload.sequence())
							&& PacketRateLimiter.allow(owner, PacketRateLimiter.Lane.VESSEL_CONTROL)) {
						VesselPossessionAbility.applyControl(owner, payload);
					}
				});
		PowersPlayNetworking.registerReceiver(ReleasePayload.TYPE, (payload, player) -> {
					if (PacketRateLimiter.allow(player, PacketRateLimiter.Lane.VESSEL_CONTROL)) {
						releaseControlledSession(player);
					}
				});
	}

	/** Production packet entry point retained separately for live GameTest coverage. */
	public static boolean releaseControlledSession(ServerPlayer owner) {
		if (owner != null) INPUT_SEQUENCES.clear(owner.getUUID());
		return VesselPossessionAbility.releaseControlledSession(owner);
	}

	public static void sendState(ServerPlayer owner, boolean active) {
		if (owner != null) INPUT_SEQUENCES.clear(owner.getUUID());
		if (owner != null && ServerPlayNetworking.canSend(owner, StatePayload.TYPE)) {
			PowersPlayNetworking.send(owner, new StatePayload(active));
		}
	}

	public static void clearSequences() {
		INPUT_SEQUENCES.clearAll();
	}
}
