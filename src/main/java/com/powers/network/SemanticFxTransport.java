package com.powers.network;

import com.powers.diagnostics.ServerRuntimeMetrics;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Owns bounded, ordered, byte-accounted delivery of semantic presentation cues. */
public final class SemanticFxTransport {
	private static final Map<MinecraftServer,
			Map<UUID, SemanticFxBatchAccumulator<MagicFxPackets.BatchEntry>>> PENDING =
			new WeakHashMap<>();
	private static final Map<MinecraftServer, MutableMetrics> METRICS = new WeakHashMap<>();
	private static final int PLAY_CUSTOM_PAYLOAD_PACKET_ID = resolveCustomPayloadPacketId();

	private SemanticFxTransport() {
	}

	private record Channel(Object connection, String dimension) {
	}

	private static final class MutableMetrics {
		private long immediatePackets;
		private long batchPackets;
		private long batchedEntries;
		private long fallbackPackets;
		private long staleEntriesDropped;

		private MagicFxPackets.TransportSnapshot snapshot() {
			return new MagicFxPackets.TransportSnapshot(immediatePackets, batchPackets,
					batchedEntries, fallbackPackets, staleEntriesDropped);
		}
	}

	static void deliver(ServerPlayer observer, MagicFxPackets.BatchEntry entry, int encodedBytes) {
		MinecraftServer server = observer.level().getServer();
		var byObserver = PENDING.computeIfAbsent(server, ignored -> new HashMap<>());
		var accumulator = byObserver.computeIfAbsent(observer.getUUID(),
				ignored -> new SemanticFxBatchAccumulator<>());
		var offer = accumulator.offer(server.getTickCount(), channel(observer), entry, encodedBytes);
		if (!offer.before().entries().isEmpty()) {
			if (offer.rollover() == SemanticFxBatchAccumulator.Rollover.CHANNEL) {
				metrics(server).staleEntriesDropped += offer.before().entries().size();
			} else {
				sendDrain(observer, offer.before());
			}
		}
		if (offer.delivery() == SemanticFxBatchAccumulator.Delivery.IMMEDIATE
				&& sendIndividual(observer, entry)) {
			metrics(server).immediatePackets++;
		}
	}

	/** Flushes each current connection's final same-tick tail. */
	public static void flush(MinecraftServer server) {
		Map<UUID, SemanticFxBatchAccumulator<MagicFxPackets.BatchEntry>> pending = PENDING.remove(server);
		if (pending == null) return;
		for (Map.Entry<UUID, SemanticFxBatchAccumulator<MagicFxPackets.BatchEntry>> observer
				: pending.entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(observer.getKey());
			var drain = observer.getValue().drain();
			if (drain.entries().isEmpty()) continue;
			if (player == null || !channel(player).equals(drain.channel())) {
				metrics(server).staleEntriesDropped += drain.entries().size();
				continue;
			}
			sendDrain(player, drain);
		}
	}

	public static void clear() {
		PENDING.clear();
		METRICS.clear();
	}

	public static void resetMetrics(MinecraftServer server) {
		METRICS.remove(server);
	}

	public static MagicFxPackets.TransportSnapshot snapshot(MinecraftServer server) {
		MutableMetrics metrics = METRICS.get(server);
		return metrics == null ? new MagicFxPackets.TransportSnapshot(0, 0, 0, 0, 0)
				: metrics.snapshot();
	}

	static MagicFxPackets.TransportPlan plan(List<MagicFxPackets.BatchEntry> entries,
			boolean batchSupported, int compressionThreshold) {
		List<MagicFxPackets.BatchEntry> ordered = List.copyOf(entries);
		if (ordered.isEmpty()) return new MagicFxPackets.TransportPlan(ordered, false, 0, 0);
		List<byte[]> individualFrames = ordered.stream().map(SemanticFxTransport::encodedFrame).toList();
		byte[] batchFrame = encodedFrame(new MagicFxPackets.SemanticFxBatchPayload(ordered));
		var decision = SemanticFxWirePolicy.decide(individualFrames, batchFrame, compressionThreshold);
		return new MagicFxPackets.TransportPlan(ordered, batchSupported && decision.batch(),
				decision.individualWireBytes(), decision.batchWireBytes());
	}

	static byte[] encodedFrame(MagicFxPackets.BatchEntry entry) {
		if (entry.magic() != null) return encodedFrame(entry.magic());
		if (entry.beam() != null) return encodedFrame(entry.beam());
		return encodedFrame(entry.shape());
	}

	static byte[] encodedFrame(CustomPacketPayload payload) {
		var raw = Unpooled.buffer();
		try {
			var buffer = new RegistryFriendlyByteBuf(raw, RegistryAccess.EMPTY);
			buffer.writeVarInt(PLAY_CUSTOM_PAYLOAD_PACKET_ID);
			buffer.writeIdentifier(payload.type().id());
			if (payload instanceof MagicFxPackets.MagicFxPayload magic) {
				MagicFxPackets.MagicFxPayload.encode(buffer, magic);
			} else if (payload instanceof MagicFxPackets.BeamFxPayload beam) {
				MagicFxPackets.BeamFxPayload.encode(buffer, beam);
			} else if (payload instanceof MagicFxPackets.ShapeFxPayload shape) {
				MagicFxPackets.ShapeFxPayload.encode(buffer, shape);
			} else if (payload instanceof MagicFxPackets.SemanticFxBatchPayload batch) {
				MagicFxPackets.SemanticFxBatchPayload.encode(buffer, batch);
			} else {
				throw new IllegalArgumentException("Unsupported semantic FX payload");
			}
			byte[] bytes = new byte[raw.readableBytes()];
			raw.getBytes(raw.readerIndex(), bytes);
			return bytes;
		} finally {
			raw.release();
		}
	}

	static int playCustomPayloadPacketId() {
		return PLAY_CUSTOM_PAYLOAD_PACKET_ID;
	}

	private static int resolveCustomPayloadPacketId() {
		int[] resolved = {-1};
		GameProtocols.CLIENTBOUND_TEMPLATE.details().listPackets((type, networkId) -> {
			if (type == CommonPacketTypes.CLIENTBOUND_CUSTOM_PAYLOAD) resolved[0] = networkId;
		});
		if (resolved[0] < 0) throw new IllegalStateException("Clientbound custom-payload packet is absent");
		return resolved[0];
	}

	private static void sendDrain(ServerPlayer observer,
			SemanticFxBatchAccumulator.Drain<MagicFxPackets.BatchEntry> drain) {
		MinecraftServer server = observer.level().getServer();
		MagicFxPackets.TransportPlan plan = plan(drain.entries(),
				ServerPlayNetworking.canSend(observer, MagicFxPackets.SemanticFxBatchPayload.TYPE),
				server.getCompressionThreshold());
		MutableMetrics metrics = metrics(server);
		if (plan.batch()) {
			ServerPlayNetworking.send(observer,
					new MagicFxPackets.SemanticFxBatchPayload(plan.entries()));
			ServerRuntimeMetrics.recordPacket(server, server.getTickCount());
			metrics.batchPackets++;
			metrics.batchedEntries += plan.entries().size();
			return;
		}
		for (MagicFxPackets.BatchEntry entry : plan.entries()) {
			if (sendIndividual(observer, entry)) metrics.fallbackPackets++;
		}
	}

	private static boolean sendIndividual(ServerPlayer observer, MagicFxPackets.BatchEntry entry) {
		if (entry.magic() != null
				&& ServerPlayNetworking.canSend(observer, MagicFxPackets.MagicFxPayload.TYPE)) {
			ServerPlayNetworking.send(observer, entry.magic());
		} else if (entry.beam() != null
				&& ServerPlayNetworking.canSend(observer, MagicFxPackets.BeamFxPayload.TYPE)) {
			ServerPlayNetworking.send(observer, entry.beam());
		} else if (entry.shape() != null
				&& ServerPlayNetworking.canSend(observer, MagicFxPackets.ShapeFxPayload.TYPE)) {
			ServerPlayNetworking.send(observer, entry.shape());
		} else {
			return false;
		}
		MinecraftServer server = observer.level().getServer();
		ServerRuntimeMetrics.recordPacket(server, server.getTickCount());
		return true;
	}

	private static Channel channel(ServerPlayer player) {
		return new Channel(player.connection, player.level().dimension().identifier().toString());
	}

	private static MutableMetrics metrics(MinecraftServer server) {
		return METRICS.computeIfAbsent(server, ignored -> new MutableMetrics());
	}
}
