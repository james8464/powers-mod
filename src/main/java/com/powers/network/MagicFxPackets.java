package com.powers.network;

import com.powers.PowersMod;
import com.powers.magic.fx.MagicFxEvent;
import com.powers.magic.fx.MagicFxKind;
import com.powers.magic.fx.MagicFxService;
import com.powers.fx.BeamFxStyle;
import com.powers.diagnostics.ServerRuntimeMetrics;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/** Owns the compact clientbound protocol for semantic magic presentation. */
public final class MagicFxPackets {
	private static final Map<ServerLevel, MagicFxService> SERVICES = new WeakHashMap<>();

	private MagicFxPackets() {
	}

	/** Compact cast or interaction cue; clients generate deterministic geometry locally. */
	public record MagicFxPayload(MagicFxKind kind, long eventId, String motif, String sound,
			double x, double y, double z, int primaryColor, int secondaryColor,
			int glyphSeed, int intensity, int genericBeatCount) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<MagicFxPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("magic_fx"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MagicFxPayload> STREAM_CODEC =
				StreamCodec.of(MagicFxPayload::encode, MagicFxPayload::decode);

		private static void encode(RegistryFriendlyByteBuf buffer, MagicFxPayload payload) {
			buffer.writeVarInt(payload.kind.networkId());
			buffer.writeVarLong(payload.eventId);
			ByteBufCodecs.STRING_UTF8.encode(buffer, payload.motif);
			ByteBufCodecs.STRING_UTF8.encode(buffer, payload.sound);
			buffer.writeDouble(payload.x);
			buffer.writeDouble(payload.y);
			buffer.writeDouble(payload.z);
			buffer.writeInt(payload.primaryColor);
			buffer.writeInt(payload.secondaryColor);
			buffer.writeInt(payload.glyphSeed);
			buffer.writeVarInt(payload.intensity);
			buffer.writeVarInt(payload.genericBeatCount);
		}

		private static MagicFxPayload decode(RegistryFriendlyByteBuf buffer) {
			return new MagicFxPayload(MagicFxKind.fromNetworkId(buffer.readVarInt()), buffer.readVarLong(),
					ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
					buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readInt(),
					buffer.readInt(), buffer.readInt(), buffer.readVarInt(), buffer.readVarInt());
		}

		public MagicFxPayload(MagicFxEvent event) {
			this(event.kind(), event.eventId(), event.motif(), event.sound(), event.x(), event.y(), event.z(),
					event.primaryColor(), event.secondaryColor(), event.glyphSeed(), event.intensity(),
					event.genericBeatCount());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** One compact line cue; the client creates every point locally. */
	public record BeamFxPayload(long eventId, BeamFxStyle style,
			double fromX, double fromY, double fromZ, double toX, double toY, double toZ,
			int count, int color) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<BeamFxPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("beam_fx"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BeamFxPayload> STREAM_CODEC =
				StreamCodec.of(BeamFxPayload::encode, BeamFxPayload::decode);

		public BeamFxPayload {
			java.util.Objects.requireNonNull(style, "style");
			if (!Double.isFinite(fromX) || !Double.isFinite(fromY) || !Double.isFinite(fromZ)
					|| !Double.isFinite(toX) || !Double.isFinite(toY) || !Double.isFinite(toZ)) {
				throw new IllegalArgumentException("Beam endpoints must be finite");
			}
			count = Math.clamp(count, 1, 64);
			color &= 0xFFFFFF;
		}

		private static void encode(RegistryFriendlyByteBuf buffer, BeamFxPayload payload) {
			buffer.writeVarLong(payload.eventId);
			buffer.writeVarInt(payload.style.networkId());
			buffer.writeDouble(payload.fromX);
			buffer.writeDouble(payload.fromY);
			buffer.writeDouble(payload.fromZ);
			buffer.writeDouble(payload.toX);
			buffer.writeDouble(payload.toY);
			buffer.writeDouble(payload.toZ);
			buffer.writeVarInt(payload.count);
			buffer.writeInt(payload.color);
		}

		private static BeamFxPayload decode(RegistryFriendlyByteBuf buffer) {
			return new BeamFxPayload(buffer.readVarLong(), BeamFxStyle.fromNetworkId(buffer.readVarInt()),
					buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
					buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
					buffer.readVarInt(), buffer.readInt());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(MagicFxPayload.TYPE, MagicFxPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BeamFxPayload.TYPE, BeamFxPayload.STREAM_CODEC);
	}

	/** Sends an already budgeted beam only to its intended observer. */
	public static void sendBeam(ServerPlayer observer, BeamFxPayload payload) {
		if (ServerPlayNetworking.canSend(observer, BeamFxPayload.TYPE)) {
			ServerPlayNetworking.send(observer, payload);
			ServerRuntimeMetrics.recordPacket(observer.level().getServer(),
					observer.level().getServer().getTickCount());
		}
	}

	/** Sends one semantic cue to nearby clients without shipping particle arrays. */
	public static void broadcast(ServerLevel level, MagicFxEvent event) {
		MagicFxService service = SERVICES.computeIfAbsent(level,
				serverLevel -> new MagicFxService(cue -> send(serverLevel, cue)));
		String key = event.kind() + "@" + event.eventId() + "@" + event.motif()
				+ "@" + event.x() + ":" + event.y() + ":" + event.z();
		service.emit(key, event);
	}

	/** Clears weak transport state explicitly at the normal server lifecycle edge. */
	public static void clear() {
		SERVICES.clear();
	}

	private static void send(ServerLevel level, MagicFxEvent event) {
		MagicFxPayload payload = new MagicFxPayload(event);
		for (ServerPlayer observer : level.players()) {
			if (observer.position().distanceToSqr(event.x(), event.y(), event.z()) > 128.0 * 128.0) continue;
			if (ServerPlayNetworking.canSend(observer, MagicFxPayload.TYPE)) {
				ServerPlayNetworking.send(observer, payload);
				ServerRuntimeMetrics.recordPacket(level.getServer(), level.getServer().getTickCount());
			}
		}
	}
}
