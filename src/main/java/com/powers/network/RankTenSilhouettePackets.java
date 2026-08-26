package com.powers.network;

import com.powers.PowersMod;
import com.powers.fx.ClientRankTenSilhouetteState;
import com.powers.fx.RankTenSilhouetteGeometry;
import com.powers.fx.RankTenSilhouetteProfile;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Owns the compact validated clientbound rank-ten silhouette contract. */
public final class RankTenSilhouettePackets {
	public static final int MAX_DIMENSION_UTF8_BYTES = 128;
	private static final UUID ZERO_UUID = new UUID(0L, 0L);

	private RankTenSilhouettePackets() {
	}

	/** One semantic event; profile geometry is expanded only by the receiving client. */
	public record Payload(long eventId, int profileId, UUID caster, String dimension,
			double x, double y, double z, float yaw, float pitch, int alignmentId,
			int visualSeed, int lifetimeTicks) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(PowersMod.id("rank_ten_silhouette"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC =
				StreamCodec.of(Payload::encode, Payload::decode);

		public Payload {
			ClientRankTenSilhouetteState.Wire wire = new ClientRankTenSilhouetteState.Wire(eventId,
					profileId, caster, dimension, x, y, z, yaw, pitch, alignmentId,
					visualSeed, lifetimeTicks);
			if (!valid(wire)) throw new IllegalArgumentException("invalid rank-ten silhouette payload");
		}

		public ClientRankTenSilhouetteState.Wire wire() {
			return new ClientRankTenSilhouetteState.Wire(eventId, profileId, caster, dimension,
					x, y, z, yaw, pitch, alignmentId, visualSeed, lifetimeTicks);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		private static void encode(RegistryFriendlyByteBuf buffer, Payload payload) {
			buffer.writeVarLong(payload.eventId);
			buffer.writeVarInt(payload.profileId);
			buffer.writeUUID(payload.caster);
			buffer.writeUtf(payload.dimension, MAX_DIMENSION_UTF8_BYTES);
			buffer.writeDouble(payload.x);
			buffer.writeDouble(payload.y);
			buffer.writeDouble(payload.z);
			buffer.writeFloat(payload.yaw);
			buffer.writeFloat(payload.pitch);
			buffer.writeVarInt(payload.alignmentId);
			buffer.writeInt(payload.visualSeed);
			buffer.writeVarInt(payload.lifetimeTicks);
		}

		private static Payload decode(RegistryFriendlyByteBuf buffer) {
			return new Payload(buffer.readVarLong(), buffer.readVarInt(), buffer.readUUID(),
					buffer.readUtf(MAX_DIMENSION_UTF8_BYTES), buffer.readDouble(), buffer.readDouble(),
					buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(),
					buffer.readInt(), buffer.readVarInt());
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.STREAM_CODEC);
	}

	private static boolean valid(ClientRankTenSilhouetteState.Wire wire) {
		return wire.eventId() > 0 && RankTenSilhouetteProfile.fromNetworkId(wire.profileId()).isPresent()
				&& wire.caster() != null && !ZERO_UUID.equals(wire.caster())
				&& validDimension(wire.dimension())
				&& validCoordinates(wire.x(), wire.y(), wire.z())
				&& Float.isFinite(wire.yaw()) && Float.isFinite(wire.pitch())
				&& (wire.alignmentId() == 0 || wire.alignmentId() == 1)
				&& wire.lifetimeTicks() >= ClientRankTenSilhouetteState.MIN_LIFETIME_TICKS
				&& wire.lifetimeTicks() <= ClientRankTenSilhouetteState.MAX_LIFETIME_TICKS;
	}

	private static boolean validDimension(String value) {
		return value != null && !value.isBlank() && Identifier.tryParse(value) != null
				&& value.getBytes(StandardCharsets.UTF_8).length <= MAX_DIMENSION_UTF8_BYTES;
	}

	private static boolean validCoordinates(double x, double y, double z) {
		return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
				&& Math.abs(x) <= RankTenSilhouetteGeometry.MAX_WORLD_COORDINATE
				&& Math.abs(y) <= RankTenSilhouetteGeometry.MAX_WORLD_COORDINATE
				&& Math.abs(z) <= RankTenSilhouetteGeometry.MAX_WORLD_COORDINATE;
	}
}
