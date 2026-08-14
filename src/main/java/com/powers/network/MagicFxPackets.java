package com.powers.network;

import com.powers.PowersMod;
import com.powers.magic.fx.MagicFxEvent;
import com.powers.magic.fx.MagicFxKind;
import com.powers.magic.fx.MagicFxService;
import com.powers.fx.BeamFxStyle;
import com.powers.fx.ShapeFxKind;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/** Owns the compact clientbound protocol for semantic magic presentation. */
public final class MagicFxPackets {
	private static final Map<ServerLevel, MagicFxService> SERVICES = new WeakHashMap<>();
	private static final FxPacketCoalescer COALESCER = new FxPacketCoalescer(32_768);

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

		static void encode(RegistryFriendlyByteBuf buffer, MagicFxPayload payload) {
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

		static void encode(RegistryFriendlyByteBuf buffer, BeamFxPayload payload) {
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

		static void encode(RegistryFriendlyByteBuf buffer, ShapeFxPayload payload) {
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

	/** One ordered semantic cue inside a mixed same-tick batch. */
	public record BatchEntry(MagicFxPayload magic, BeamFxPayload beam, ShapeFxPayload shape) {
		private static final int MAGIC = 0;
		private static final int BEAM = 1;
		private static final int SHAPE = 2;

		public BatchEntry {
			int present = (magic == null ? 0 : 1) + (beam == null ? 0 : 1) + (shape == null ? 0 : 1);
			if (present != 1) throw new IllegalArgumentException("A batch entry must hold exactly one cue");
		}

		public static BatchEntry magic(MagicFxPayload payload) {
			return new BatchEntry(Objects.requireNonNull(payload), null, null);
		}

		public static BatchEntry beam(BeamFxPayload payload) {
			return new BatchEntry(null, Objects.requireNonNull(payload), null);
		}

		public static BatchEntry shape(ShapeFxPayload payload) {
			return new BatchEntry(null, null, Objects.requireNonNull(payload));
		}

		void encode(RegistryFriendlyByteBuf buffer) {
			if (magic != null) {
				buffer.writeByte(MAGIC);
				MagicFxPayload.encode(buffer, magic);
			} else if (beam != null) {
				buffer.writeByte(BEAM);
				BeamFxPayload.encode(buffer, beam);
			} else {
				buffer.writeByte(SHAPE);
				ShapeFxPayload.encode(buffer, shape);
			}
		}

		private static BatchEntry decode(RegistryFriendlyByteBuf buffer) {
			return switch (buffer.readUnsignedByte()) {
				case MAGIC -> magic(MagicFxPayload.decode(buffer));
				case BEAM -> beam(BeamFxPayload.decode(buffer));
				case SHAPE -> shape(ShapeFxPayload.decode(buffer));
				default -> throw new IllegalArgumentException("Unknown semantic FX batch entry");
			};
		}
	}

	/** A bounded ordered tail whose repeated semantic fields can use vanilla packet compression. */
	public record SemanticFxBatchPayload(List<BatchEntry> entries) implements CustomPacketPayload {
		private static final int MAX_ENTRIES = SemanticFxBatchAccumulator.DEFAULT_MAX_ENTRIES;
		public static final CustomPacketPayload.Type<SemanticFxBatchPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("semantic_fx_batch"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SemanticFxBatchPayload> STREAM_CODEC =
				StreamCodec.of(SemanticFxBatchPayload::encode, SemanticFxBatchPayload::decode);

		public SemanticFxBatchPayload {
			entries = List.copyOf(entries);
			if (entries.isEmpty() || entries.size() > MAX_ENTRIES) {
				throw new IllegalArgumentException("Semantic FX batch size is out of bounds");
			}
		}

		static void encode(RegistryFriendlyByteBuf buffer, SemanticFxBatchPayload payload) {
			buffer.writeVarInt(payload.entries.size());
			for (BatchEntry entry : payload.entries) entry.encode(buffer);
		}

		private static SemanticFxBatchPayload decode(RegistryFriendlyByteBuf buffer) {
			int count = buffer.readVarInt();
			if (count < 1 || count > MAX_ENTRIES) {
				throw new IllegalArgumentException("Semantic FX batch size is out of bounds");
			}
			List<BatchEntry> entries = new java.util.ArrayList<>(count);
			for (int index = 0; index < count; index++) entries.add(BatchEntry.decode(buffer));
			return new SemanticFxBatchPayload(entries);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Byte-accounted delivery choice for one ordered deferred tail. */
	public record TransportPlan(List<BatchEntry> entries, boolean batch,
			int individualWireBytes, int batchWireBytes) {
		public TransportPlan {
			entries = List.copyOf(entries);
		}
	}

	/** Actual packets sent by the semantic transport since its last explicit reset. */
	public record TransportSnapshot(long immediatePackets, long batchPackets,
			long batchedEntries, long fallbackPackets, long staleEntriesDropped) {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(MagicFxPayload.TYPE, MagicFxPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BeamFxPayload.TYPE, BeamFxPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ShapeFxPayload.TYPE, ShapeFxPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(
				SemanticFxBatchPayload.TYPE, SemanticFxBatchPayload.STREAM_CODEC);
	}

	/** Sends an already budgeted beam only to its intended observer. */
	public static void sendBeam(ServerPlayer observer, BeamFxPayload payload) {
		if (!ServerPlayNetworking.canSend(observer, BeamFxPayload.TYPE)) return;
		long tick = observer.level().getServer().getTickCount();
		int chunkX = ((int) Math.floor((payload.fromX() + payload.toX()) * 0.5)) >> 4;
		int chunkZ = ((int) Math.floor((payload.fromZ() + payload.toZ()) * 0.5)) >> 4;
		if (COALESCER.allow(tick, observer.getUUID(), observer.level().dimension().identifier().toString(),
				chunkX, chunkZ, "beam:" + payload.style().name(), "sustain",
				encodedBodyBytes(payload))) {
			deliver(observer, BatchEntry.beam(payload), encodedBodyBytes(payload));
		}
	}

	/** Sends an already budgeted semantic shape only to its intended observer. */
	public static void sendShape(ServerPlayer observer, ShapeFxPayload payload) {
		if (!ServerPlayNetworking.canSend(observer, ShapeFxPayload.TYPE)) return;
		long tick = observer.level().getServer().getTickCount();
		if (COALESCER.allow(tick, observer.getUUID(), observer.level().dimension().identifier().toString(),
				((int) Math.floor(payload.x())) >> 4, ((int) Math.floor(payload.z())) >> 4,
				"shape:" + payload.kind().name(), "sustain", encodedBodyBytes(payload))) {
			deliver(observer, BatchEntry.shape(payload), encodedBodyBytes(payload));
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
		SemanticFxTransport.clear();
		COALESCER.clear();
	}

	/** Flushes deferred same-tick tails after every authoritative magic owner has ticked. */
	public static void flush(MinecraftServer server) {
		SemanticFxTransport.flush(server);
	}

	/** Starts an isolated transport capture without disturbing payload or level caches. */
	public static void resetFxTrafficMetrics() {
		COALESCER.clear();
	}

	/** Resets only semantic transport counters for a bounded live acceptance capture. */
	public static void resetTransportMetrics(MinecraftServer server) {
		SemanticFxTransport.resetMetrics(server);
	}

	public static TransportSnapshot transportSnapshot(MinecraftServer server) {
		return SemanticFxTransport.snapshot(server);
	}

	public static FxPacketCoalescer.TrafficSnapshot fxTrafficSnapshot() {
		return COALESCER.trafficSnapshot();
	}

	private static void send(ServerLevel level, MagicFxEvent event) {
		MagicFxPayload payload = new MagicFxPayload(event);
		for (ServerPlayer observer : level.players()) {
			if (observer.position().distanceToSqr(event.x(), event.y(), event.z()) > 128.0 * 128.0) continue;
			if (!ServerPlayNetworking.canSend(observer, MagicFxPayload.TYPE)) continue;
			long tick = level.getServer().getTickCount();
			if (COALESCER.allow(tick, observer.getUUID(), level.dimension().identifier().toString(),
					((int) Math.floor(event.x())) >> 4, ((int) Math.floor(event.z())) >> 4,
					"magic:" + event.motif(), event.kind().name().toLowerCase(java.util.Locale.ROOT),
					encodedBodyBytes(payload))) {
				deliver(observer, BatchEntry.magic(payload), encodedBodyBytes(payload));
			}
		}
	}

	private static void deliver(ServerPlayer observer, BatchEntry entry, int encodedBytes) {
		SemanticFxTransport.deliver(observer, entry, encodedBytes);
	}

	static TransportPlan transportPlan(List<BatchEntry> entries, boolean batchSupported) {
		return SemanticFxTransport.plan(entries, batchSupported, 256);
	}

	static TransportPlan transportPlan(List<BatchEntry> entries, boolean batchSupported,
			int compressionThreshold) {
		return SemanticFxTransport.plan(entries, batchSupported, compressionThreshold);
	}

	static int encodedBodyBytes(MagicFxPayload payload) {
		return varIntBytes(payload.kind().networkId()) + varLongBytes(payload.eventId())
				+ stringBytes(payload.motif()) + stringBytes(payload.sound())
				+ Double.BYTES * 3 + Integer.BYTES * 3
				+ varIntBytes(payload.intensity()) + varIntBytes(payload.genericBeatCount());
	}

	static int encodedBodyBytes(BeamFxPayload payload) {
		return varLongBytes(payload.eventId()) + varIntBytes(payload.style().networkId())
				+ Double.BYTES * 6 + varIntBytes(payload.count()) + Integer.BYTES;
	}

	static int encodedBodyBytes(ShapeFxPayload payload) {
		return varLongBytes(payload.eventId()) + varIntBytes(payload.kind().networkId())
				+ Double.BYTES * 5 + varIntBytes(payload.count())
				+ Integer.BYTES + Double.BYTES;
	}

	private static int stringBytes(String value) {
		int bytes = value.getBytes(StandardCharsets.UTF_8).length;
		return varIntBytes(bytes) + bytes;
	}

	private static int varIntBytes(int value) {
		int bytes = 1;
		while ((value & ~0x7F) != 0) {
			bytes++;
			value >>>= 7;
		}
		return bytes;
	}

	private static int varLongBytes(long value) {
		int bytes = 1;
		while ((value & ~0x7FL) != 0L) {
			bytes++;
			value >>>= 7;
		}
		return bytes;
	}
}
