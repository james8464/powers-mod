package com.powers.network;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.powers.PowersMod;
import com.powers.mind.BodyProxyManager;
import com.powers.mind.BodySnapshot;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/** Bounded clientbound transport for exact frozen-body render snapshots. */
public final class BodyProxyPackets {
	public static final int MAX_SNAPSHOT_CHARS = 8_192;

	private BodyProxyPackets() {
	}

	/** Empty JSON removes the entity ID from the client snapshot cache. */
	public record BodySnapshotPayload(int entityId, String snapshotJson)
			implements CustomPacketPayload {
		public static final Type<BodySnapshotPayload> TYPE =
				new Type<>(PowersMod.id("body_snapshot"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BodySnapshotPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_INT, BodySnapshotPayload::entityId,
						ByteBufCodecs.STRING_UTF8, BodySnapshotPayload::snapshotJson,
						BodySnapshotPayload::new);

		public BodySnapshotPayload {
			if (entityId < 0) throw new IllegalArgumentException("Invalid body entity ID");
			if (snapshotJson == null || snapshotJson.length() > MAX_SNAPSHOT_CHARS) {
				throw new IllegalArgumentException("Invalid body snapshot payload");
			}
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(
				BodySnapshotPayload.TYPE, BodySnapshotPayload.STREAM_CODEC);
		EntityTrackingEvents.START_TRACKING.register((entity, observer) -> {
			BodySnapshot snapshot = BodyProxyManager.snapshotFor(entity.getUUID());
			if (snapshot != null) send(observer, entity.getId(), snapshot);
		});
	}

	/** Encodes through the same validated codec used by unit and client parsing. */
	public static String encode(BodySnapshot snapshot) {
		String json = BodySnapshot.CODEC.encodeStart(JsonOps.INSTANCE, snapshot).getOrThrow().toString();
		if (json.length() > MAX_SNAPSHOT_CHARS) throw new IllegalArgumentException("Body snapshot is too large");
		return json;
	}

	/** Rejects malformed or oversized snapshots without mutating client state. */
	public static Optional<BodySnapshot> decode(String json) {
		if (json == null || json.isEmpty() || json.length() > MAX_SNAPSHOT_CHARS) return Optional.empty();
		try {
			return BodySnapshot.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result();
		} catch (RuntimeException ignored) {
			return Optional.empty();
		}
	}

	public static void send(ServerPlayer observer, int entityId, BodySnapshot snapshot) {
		if (ServerPlayNetworking.canSend(observer, BodySnapshotPayload.TYPE)) {
			PowersPlayNetworking.send(observer, new BodySnapshotPayload(entityId, encode(snapshot)));
		}
	}

	/** Sends an initial frame to players already tracking a newly registered proxy. */
	public static void sendToTracking(Entity body, BodySnapshot snapshot) {
		for (ServerPlayer observer : PlayerLookup.tracking(body)) {
			send(observer, body.getId(), snapshot);
		}
	}

	/** Removes a discarded proxy snapshot from every client that tracked it. */
	public static void remove(Entity body) {
		BodySnapshotPayload payload = new BodySnapshotPayload(body.getId(), "");
		for (ServerPlayer observer : PlayerLookup.tracking(body)) {
			if (ServerPlayNetworking.canSend(observer, BodySnapshotPayload.TYPE)) {
				PowersPlayNetworking.send(observer, payload);
			}
		}
	}
}
