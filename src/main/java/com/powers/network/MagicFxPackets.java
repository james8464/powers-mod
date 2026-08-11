package com.powers.network;

import com.powers.PowersMod;
import com.powers.magic.fx.MagicFxEvent;
import com.powers.magic.fx.MagicFxKind;
import com.powers.magic.fx.MagicFxService;
import com.powers.fx.BeamFxStyle;
import com.powers.fx.ShapeFxKind;
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
	private static final FxPacketCoalescer COALESCER = new FxPacketCoalescer(32_768);
	private static final FxPayloadPool PAYLOADS = new FxPayloadPool(1_024);

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

	/** One budgeted circle, rune, or spiral; every point is expanded by its recipient. */
	public record ShapeFxPayload(long eventId, ShapeFxKind kind,
			double x, double y, double z, double radius, double height,
			int count, int color, double phase) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ShapeFxPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("shape_fx"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ShapeFxPayload> STREAM_CODEC =
				StreamCodec.of(ShapeFxPayload::encode, ShapeFxPayload::decode);

		public ShapeFxPayload {
			java.util.Objects.requireNonNull(kind, "kind");
			if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
					|| !Double.isFinite(radius) || !Double.isFinite(height)
					|| !Double.isFinite(phase)) {
				throw new IllegalArgumentException("Shape geometry must be finite");
			}
			radius = Math.clamp(radius, 0.0, 256.0);
			height = Math.clamp(height, -256.0, 256.0);
			count = Math.clamp(count, 1, 640);
			color &= 0xFFFFFF;
		}

		private static void encode(RegistryFriendlyByteBuf buffer, ShapeFxPayload payload) {
			buffer.writeVarLong(payload.eventId);
			buffer.writeVarInt(payload.kind.networkId());
			buffer.writeDouble(payload.x);
			buffer.writeDouble(payload.y);
			buffer.writeDouble(payload.z);
			buffer.writeDouble(payload.radius);
			buffer.writeDouble(payload.height);
			buffer.writeVarInt(payload.count);
			buffer.writeInt(payload.color);
			buffer.writeDouble(payload.phase);
		}

		private static ShapeFxPayload decode(RegistryFriendlyByteBuf buffer) {
			return new ShapeFxPayload(buffer.readVarLong(),
					ShapeFxKind.fromNetworkId(buffer.readVarInt()),
					buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
					buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(),
					buffer.readInt(), buffer.readDouble());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(MagicFxPayload.TYPE, MagicFxPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BeamFxPayload.TYPE, BeamFxPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ShapeFxPayload.TYPE, ShapeFxPayload.STREAM_CODEC);
	}

	/** Sends an already budgeted beam only to its intended observer. */
	public static void sendBeam(ServerPlayer observer, BeamFxPayload payload) {
		long tick = observer.level().getServer().getTickCount();
		int chunkX = ((int) Math.floor((payload.fromX() + payload.toX()) * 0.5)) >> 4;
		int chunkZ = ((int) Math.floor((payload.fromZ() + payload.toZ()) * 0.5)) >> 4;
		if (COALESCER.allow(tick, observer.getUUID(), observer.level().dimension().identifier().toString(),
				chunkX, chunkZ, "beam:" + payload.style().name())
				&& ServerPlayNetworking.canSend(observer, BeamFxPayload.TYPE)) {
			ServerPlayNetworking.send(observer, payload);
			ServerRuntimeMetrics.recordPacket(observer.level().getServer(), tick);
		}
	}

	/** Sends an already budgeted semantic shape only to its intended observer. */
	public static void sendShape(ServerPlayer observer, ShapeFxPayload payload) {
		long tick = observer.level().getServer().getTickCount();
		if (COALESCER.allow(tick, observer.getUUID(), observer.level().dimension().identifier().toString(),
				((int) Math.floor(payload.x())) >> 4, ((int) Math.floor(payload.z())) >> 4,
				"shape:" + payload.kind().name())
				&& ServerPlayNetworking.canSend(observer, ShapeFxPayload.TYPE)) {
			ServerPlayNetworking.send(observer, payload);
			ServerRuntimeMetrics.recordPacket(observer.level().getServer(), tick);
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
		COALESCER.clear();
		PAYLOADS.clear();
	}

	/** Canonicalizes a short-lived immutable payload for repeated observer sends. */
	public static <T extends CustomPacketPayload> T pooled(T payload) {
		return PAYLOADS.intern(payload);
	}

	private static void send(ServerLevel level, MagicFxEvent event) {
		MagicFxPayload payload = new MagicFxPayload(event);
		for (ServerPlayer observer : level.players()) {
			if (observer.position().distanceToSqr(event.x(), event.y(), event.z()) > 128.0 * 128.0) continue;
			long tick = level.getServer().getTickCount();
			if (COALESCER.allow(tick, observer.getUUID(), level.dimension().identifier().toString(),
					((int) Math.floor(event.x())) >> 4, ((int) Math.floor(event.z())) >> 4,
					"magic:" + event.kind().name() + ":" + event.motif())
					&& ServerPlayNetworking.canSend(observer, MagicFxPayload.TYPE)) {
				ServerPlayNetworking.send(observer, payload);
				ServerRuntimeMetrics.recordPacket(level.getServer(), tick);
			}
		}
	}
}
