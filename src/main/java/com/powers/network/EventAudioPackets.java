package com.powers.network;

import com.powers.PowersMod;
import com.powers.PowersSounds;
import com.powers.fx.FxLodTier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

import java.util.Objects;

/** Bounded vocabulary for the quiet non-positional layer of distant world-scale events. */
public final class EventAudioPackets {
	public enum Cue {
		LIGHT_HERALD(0),
		DARK_EVENT(1);

		private final int networkId;

		Cue(int networkId) {
			this.networkId = networkId;
		}

		public SoundEvent sound() {
			return this == LIGHT_HERALD ? PowersSounds.LIGHT_CHORUS : PowersSounds.DARK_WHISPER;
		}

		public float volume(FxLodTier tier) {
			return switch (tier) {
				case NEAR -> 0.75F;
				case MID -> 0.55F;
				case FAR -> 0.28F;
				case HIDDEN -> 0.0F;
			};
		}

		private static Cue fromNetworkId(int id) {
			return id == LIGHT_HERALD.networkId ? LIGHT_HERALD : DARK_EVENT;
		}
	}

	public record Payload(Cue cue, FxLodTier lod, float pitch) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(PowersMod.id("event_audio"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC =
				StreamCodec.of(Payload::encode, Payload::decode);

		public Payload {
			Objects.requireNonNull(cue, "cue");
			Objects.requireNonNull(lod, "lod");
			if (lod == FxLodTier.HIDDEN) {
				throw new IllegalArgumentException("Hidden event audio must not be sent");
			}
			pitch = Float.isFinite(pitch) ? Math.clamp(pitch, 0.25F, 4.0F) : 1.0F;
		}

		private static void encode(RegistryFriendlyByteBuf buffer, Payload payload) {
			buffer.writeByte(payload.cue.networkId);
			buffer.writeByte(payload.lod.networkId());
			buffer.writeFloat(payload.pitch);
		}

		private static Payload decode(RegistryFriendlyByteBuf buffer) {
			return new Payload(Cue.fromNetworkId(buffer.readUnsignedByte()),
					FxLodTier.fromNetworkId(buffer.readUnsignedByte()), buffer.readFloat());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private EventAudioPackets() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.STREAM_CODEC);
	}

	public static boolean send(ServerPlayer player, Cue cue, FxLodTier tier, float pitch) {
		if (!ServerPlayNetworking.canSend(player, Payload.TYPE)) return false;
		ServerPlayNetworking.send(player, new Payload(cue, tier, pitch));
		return true;
	}
}
