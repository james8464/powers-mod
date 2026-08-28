package com.powers.network;

import com.powers.PowersMod;
import com.powers.animation.CastingHand;
import com.powers.animation.CastingPose;
import com.powers.animation.CastingPoseEvent;
import com.powers.animation.CastingStyle;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Compact validated clientbound transport for server-authored entity poses. */
public final class CastingPosePackets {
	private CastingPosePackets() {
	}

	public record Payload(int entityId, UUID entityUuid, long sequence,
			int poseId, int styleId, int handId, long startGameTime, int durationTicks)
			implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(PowersMod.id("casting_pose"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC =
				StreamCodec.of(Payload::encode, Payload::decode);

		public Payload {
			new CastingPoseEvent(entityId, entityUuid, sequence,
					pose(poseId), style(styleId), hand(handId), startGameTime, durationTicks);
		}

		public Payload(CastingPoseEvent event) {
			this(event.entityId(), event.entityUuid(), event.sequence(), event.pose().networkId(),
					event.style().networkId(), event.hand().networkId(), event.startGameTime(),
					event.durationTicks());
		}

		public CastingPoseEvent event() {
			return new CastingPoseEvent(entityId, entityUuid, sequence, pose(poseId), style(styleId),
					hand(handId), startGameTime, durationTicks);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		private static void encode(RegistryFriendlyByteBuf buffer, Payload payload) {
			buffer.writeVarInt(payload.entityId);
			buffer.writeUUID(payload.entityUuid);
			buffer.writeVarLong(payload.sequence);
			buffer.writeVarInt(payload.poseId);
			buffer.writeVarInt(payload.styleId);
			buffer.writeVarInt(payload.handId);
			buffer.writeVarLong(payload.startGameTime);
			buffer.writeVarInt(payload.durationTicks);
		}

		private static Payload decode(RegistryFriendlyByteBuf buffer) {
			return new Payload(buffer.readVarInt(), buffer.readUUID(), buffer.readVarLong(),
					buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
					buffer.readVarLong(), buffer.readVarInt());
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.STREAM_CODEC);
	}

	private static CastingPose pose(int id) {
		return CastingPose.fromNetworkId(id)
				.orElseThrow(() -> new IllegalArgumentException("unknown pose ID"));
	}

	private static CastingStyle style(int id) {
		return CastingStyle.fromNetworkId(id)
				.orElseThrow(() -> new IllegalArgumentException("unknown style ID"));
	}

	private static CastingHand hand(int id) {
		return CastingHand.fromNetworkId(id)
				.orElseThrow(() -> new IllegalArgumentException("unknown hand ID"));
	}
}
