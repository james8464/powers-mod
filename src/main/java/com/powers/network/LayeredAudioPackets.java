package com.powers.network;

import com.powers.PowersMod;
import com.powers.audio.LayeredAudioCue;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Strict bounded wire contract for one server-authored semantic audio event. */
public final class LayeredAudioPackets {
	public static final int MAX_DIMENSION_UTF8_BYTES = 128;
	public static final double MAX_WORLD_COORDINATE = 30_000_000.0;

	private LayeredAudioPackets() {
	}

	public record Payload(long eventId, LayeredAudioCue cue, Identifier dimension,
			double x, double y, double z, float gain, float pitch, long emittedGameTime)
			implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(PowersMod.id("layered_audio"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC =
				StreamCodec.of(Payload::encode, Payload::decode);

		public Payload {
			Objects.requireNonNull(cue, "cue");
			Objects.requireNonNull(dimension, "dimension");
			if (eventId < 0 || emittedGameTime < 0 || !validDimension(dimension)
					|| !validPosition(x, y, z)
					|| !Float.isFinite(gain) || gain < 0.01F || gain > 4.0F
					|| !Float.isFinite(pitch) || pitch < 0.25F || pitch > 4.0F) {
				throw new IllegalArgumentException("Invalid layered audio payload");
			}
		}

		private static void encode(RegistryFriendlyByteBuf buffer, Payload payload) {
			buffer.writeVarLong(payload.eventId);
			buffer.writeByte(payload.cue.networkId());
			buffer.writeUtf(payload.dimension.toString(), MAX_DIMENSION_UTF8_BYTES);
			buffer.writeDouble(payload.x);
			buffer.writeDouble(payload.y);
			buffer.writeDouble(payload.z);
			buffer.writeFloat(payload.gain);
			buffer.writeFloat(payload.pitch);
			buffer.writeVarLong(payload.emittedGameTime);
		}

		private static Payload decode(RegistryFriendlyByteBuf buffer) {
			long eventId = buffer.readVarLong();
			int cueId = buffer.readUnsignedByte();
			LayeredAudioCue cue = LayeredAudioCue.fromNetworkId(cueId)
					.orElseThrow(() -> new IllegalArgumentException("Unknown layered audio cue " + cueId));
			Identifier dimension = Identifier.tryParse(buffer.readUtf(MAX_DIMENSION_UTF8_BYTES));
			return new Payload(eventId, cue, dimension, buffer.readDouble(), buffer.readDouble(),
					buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readVarLong());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.STREAM_CODEC);
	}

	private static boolean validDimension(Identifier dimension) {
		return dimension.toString().getBytes(StandardCharsets.UTF_8).length <= MAX_DIMENSION_UTF8_BYTES;
	}

	private static boolean validPosition(double x, double y, double z) {
		return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
				&& Math.abs(x) <= MAX_WORLD_COORDINATE
				&& Math.abs(y) <= MAX_WORLD_COORDINATE
				&& Math.abs(z) <= MAX_WORLD_COORDINATE;
	}
}
