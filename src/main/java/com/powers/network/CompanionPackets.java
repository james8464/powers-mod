package com.powers.network;

import com.powers.PowersMod;
import com.powers.companion.PrivateCompanionManager;
import com.powers.diagnostics.ServerRuntimeMetrics;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Authenticated Shadow control and client-local apparition state. */
public final class CompanionPackets {
	private static final int MAX_STATE_PACKETS_PER_TICK = 4_096;
	private static final Map<net.minecraft.server.MinecraftServer, CompanionPacketBudget> BUDGETS =
			new WeakHashMap<>();
	private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
			(buf, value) -> {
				buf.writeLong(value.getMostSignificantBits());
				buf.writeLong(value.getLeastSignificantBits());
			}, buf -> new UUID(buf.readLong(), buf.readLong()));
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> DIMENSION_CODEC =
			ByteBufCodecs.stringUtf8(128);

	private CompanionPackets() {
	}

	/** A complete client state; inactive records remove the local apparition. */
	public record StatePayload(UUID ownerId, long sessionId, boolean active, boolean teleport,
			String dimension, double x, double y, double z, float yaw)
			implements CustomPacketPayload {
		public static final Type<StatePayload> TYPE = new Type<>(PowersMod.id("companion_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> STREAM_CODEC =
				StreamCodec.composite(
						UUID_CODEC, StatePayload::ownerId,
						ByteBufCodecs.LONG, StatePayload::sessionId,
						ByteBufCodecs.BOOL, StatePayload::active,
						ByteBufCodecs.BOOL, StatePayload::teleport,
						DIMENSION_CODEC, StatePayload::dimension,
						ByteBufCodecs.DOUBLE, StatePayload::x,
						ByteBufCodecs.DOUBLE, StatePayload::y,
						ByteBufCodecs.DOUBLE, StatePayload::z,
						ByteBufCodecs.FLOAT, StatePayload::yaw,
						StatePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** The owner toggles its own apparition; arbitrary session IDs are ignored. */
	public record InteractPayload(long sessionId) implements CustomPacketPayload {
		public static final Type<InteractPayload> TYPE = new Type<>(PowersMod.id("companion_interact"));
		public static final StreamCodec<RegistryFriendlyByteBuf, InteractPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.LONG, InteractPayload::sessionId,
						InteractPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Authenticated owner-only energy and relevance snapshot. */
	public record StatusPayload(UUID ownerId, boolean active, int energy, int maximumEnergy,
			String stance, boolean revealed, boolean suppressed, int recallTicks)
			implements CustomPacketPayload {
		public static final Type<StatusPayload> TYPE = new Type<>(PowersMod.id("companion_status"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StatusPayload> STREAM_CODEC =
				StreamCodec.of((buffer, payload) -> {
					UUID_CODEC.encode(buffer, payload.ownerId());
					buffer.writeBoolean(payload.active());
					buffer.writeVarInt(payload.energy());
					buffer.writeVarInt(payload.maximumEnergy());
					ByteBufCodecs.stringUtf8(16).encode(buffer, payload.stance());
					buffer.writeBoolean(payload.revealed());
					buffer.writeBoolean(payload.suppressed());
					buffer.writeVarInt(payload.recallTicks());
				}, buffer -> new StatusPayload(UUID_CODEC.decode(buffer), buffer.readBoolean(),
						buffer.readVarInt(), buffer.readVarInt(),
						ByteBufCodecs.stringUtf8(16).decode(buffer), buffer.readBoolean(),
						buffer.readBoolean(), buffer.readVarInt()));

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(StatusPayload.TYPE, StatusPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(InteractPayload.TYPE, InteractPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(InteractPayload.TYPE, (payload, context) ->
				ServerPlayCallback.execute(context, player -> {
					if (PacketRateLimiter.allow(player, PacketRateLimiter.Lane.COMPANION)) {
						PrivateCompanionManager.interact(player, payload.sessionId());
					}
				}));
	}

	public static void sendStatus(ServerPlayer owner, boolean active, int energy,
			String stance, boolean revealed, boolean suppressed, int recallTicks) {
		if (!ServerPlayNetworking.canSend(owner, StatusPayload.TYPE)) return;
		ServerPlayNetworking.send(owner, new StatusPayload(owner.getUUID(), active,
				Math.clamp(energy, 0, com.powers.companion.ShadowCompanionRules.MAX_ENERGY),
				com.powers.companion.ShadowCompanionRules.MAX_ENERGY,
				stance == null ? "follow" : stance, revealed, suppressed,
				Math.max(0, recallTicks)));
	}

	public static boolean sendState(ServerPlayer recipient, UUID ownerId, long sessionId,
			boolean active, boolean teleport, String dimension,
			double x, double y, double z, float yaw) {
		return sendState(recipient, ownerId, sessionId, active, teleport, dimension,
				x, y, z, yaw, false);
	}

	/** Removal packets bypass the ordinary budget so no stale apparition survives. */
	public static boolean sendCriticalState(ServerPlayer recipient, UUID ownerId, long sessionId,
			boolean active, boolean teleport, String dimension,
			double x, double y, double z, float yaw) {
		return sendState(recipient, ownerId, sessionId, active, teleport, dimension,
				x, y, z, yaw, true);
	}

	private static boolean sendState(ServerPlayer recipient, UUID ownerId, long sessionId,
			boolean active, boolean teleport, String dimension,
			double x, double y, double z, float yaw, boolean critical) {
		if (!ServerPlayNetworking.canSend(recipient, StatePayload.TYPE)) return false;
		var server = recipient.level().getServer();
		long tick = server.getTickCount();
		if (!critical && !BUDGETS.computeIfAbsent(server,
				ignored -> new CompanionPacketBudget(MAX_STATE_PACKETS_PER_TICK)).claim(tick)) {
			return false;
		}
		ServerPlayNetworking.send(recipient, new StatePayload(ownerId, sessionId, active,
				teleport, dimension, x, y, z, yaw));
		ServerRuntimeMetrics.recordPacket(server, tick);
		return true;
	}

	public static void clearBudgets() {
		BUDGETS.clear();
	}
}
