package com.powers.animation;

import com.powers.companion.ShadowCompanionEntity;
import com.powers.entity.DarknessCreature;
import com.powers.entity.FirstVessel;
import com.powers.entity.RadiantSentinel;
import com.powers.entity.RealmHerald;
import com.powers.network.CastingPosePackets;
import com.powers.network.PowersPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Server boundary for scoped, tracking-only casting-pose delivery. */
public final class CastingPoseService {
	private static final CastingPoseLedger LEDGER = new CastingPoseLedger();

	private CastingPoseService() {
	}

	public static void initialize() {
		EntityTrackingEvents.START_TRACKING.register(CastingPoseService::trackingStarted);
	}

	public static boolean scopeType(Class<?> type) {
		return type == ShadowCompanionEntity.class || type == RadiantSentinel.class
				|| type == DarknessCreature.class || type == RealmHerald.class
				|| type == FirstVessel.class;
	}

	public static Optional<CastingPoseEvent> deliver(CastingPoseLedger ledger, RuntimeAccess runtime,
			CastingPose pose, CastingStyle style, CastingHand hand, int durationTicks) {
		if (!runtime.eligible()) return Optional.empty();
		Optional<CastingPoseEvent> offered = ledger.offer(runtime.entityId(), runtime.entityUuid(),
				pose, style, hand, runtime.gameTime(), durationTicks);
		if (offered.isEmpty()) return Optional.empty();
		CastingPosePackets.Payload payload = new CastingPosePackets.Payload(offered.orElseThrow());
		for (UUID observer : runtime.trackingObservers()) {
			if (runtime.canSend(observer)) runtime.sendGuarded(observer, payload);
		}
		return offered;
	}

	/** Starts one server-authored pose and sends it only to current tracking observers. */
	public static Optional<CastingPoseEvent> start(LivingEntity entity, CastingPose pose,
			CastingStyle style, CastingHand hand, int durationTicks) {
		if (!(entity.level() instanceof ServerLevel level)) return Optional.empty();
		return deliver(LEDGER, new MinecraftRuntime(entity, level), pose, style, hand, durationTicks);
	}

	/** Returns a current authoritative pose without retaining entity or level instances. */
	public static Optional<CastingPoseEvent> current(UUID entityUuid, long gameTime) {
		return LEDGER.snapshot(entityUuid, gameTime);
	}

	public static CastingPoseLedger.Metrics metrics() {
		return LEDGER.metrics();
	}

	/** Replaces an active channel with a one-tick terminal event, then lets tick cleanup remove it. */
	public static void clear(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			LEDGER.clear(entity.getUUID());
			return;
		}
		Optional<CastingPoseEvent> current = LEDGER.snapshot(entity.getUUID(), level.getGameTime());
		if (current.isEmpty()) {
			LEDGER.clear(entity.getUUID());
			return;
		}
		CastingPoseEvent event = current.orElseThrow();
		deliver(LEDGER, new MinecraftRuntime(entity, level), event.pose(), event.style(),
				event.hand(), 1);
	}

	/** Expires state and forgets identities that are no longer loaded and alive. */
	public static void tick(MinecraftServer server) {
		long gameTime = server.overworld().getGameTime();
		LEDGER.tick(gameTime, uuid -> alive(server, uuid));
	}

	public static void clearAll() {
		LEDGER.clear();
	}

	private static void trackingStarted(Entity entity, ServerPlayer observer) {
		if (!scopeType(entity.getClass()) || !(entity.level() instanceof ServerLevel level)) return;
		LEDGER.snapshot(entity.getUUID(), level.getGameTime()).ifPresent(event -> {
			CastingPosePackets.Payload payload = new CastingPosePackets.Payload(event);
			if (ServerPlayNetworking.canSend(observer, CastingPosePackets.Payload.TYPE)) {
				sendGuarded(entity, observer, payload);
			}
		});
	}

	private static boolean alive(MinecraftServer server, UUID uuid) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(uuid);
			if (entity != null) return entity.isAlive() && !entity.isRemoved();
		}
		return false;
	}

	private static void sendGuarded(Entity entity, ServerPlayer observer,
			CastingPosePackets.Payload payload) {
		MinecraftServer server = observer.level().getServer();
		UUID observerUuid = observer.getUUID();
		UUID entityUuid = entity.getUUID();
		int entityId = entity.getId();
		PowersPlayNetworking.sendGuarded(observer, payload, current -> {
			if (server.getPlayerList().getPlayer(observerUuid) != current) return false;
			Entity live = current.level().getEntity(entityId);
			return live != null && live.getUUID().equals(entityUuid)
					&& !live.isRemoved() && live.isAlive();
		}, () -> { }, failure -> { });
	}

	/** Narrow runtime boundary guarantees delivery cannot enumerate non-tracking players. */
	public interface RuntimeAccess {
		int entityId();
		UUID entityUuid();
		long gameTime();
		boolean eligible();
		List<UUID> trackingObservers();
		boolean canSend(UUID observer);
		void sendGuarded(UUID observer, CastingPosePackets.Payload payload);
	}

	private static final class MinecraftRuntime implements RuntimeAccess {
		private final LivingEntity entity;
		private final ServerLevel level;

		private MinecraftRuntime(LivingEntity entity, ServerLevel level) {
			this.entity = entity;
			this.level = level;
		}

		@Override
		public int entityId() {
			return entity.getId();
		}

		@Override
		public UUID entityUuid() {
			return entity.getUUID();
		}

		@Override
		public long gameTime() {
			return level.getGameTime();
		}

		@Override
		public boolean eligible() {
			return scopeType(entity.getClass()) && entity.level() == level
					&& entity.isAlive() && !entity.isRemoved();
		}

		@Override
		public List<UUID> trackingObservers() {
			return PlayerLookup.tracking(entity).stream().map(ServerPlayer::getUUID).toList();
		}

		@Override
		public boolean canSend(UUID observer) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(observer);
			return player != null && ServerPlayNetworking.canSend(player, CastingPosePackets.Payload.TYPE);
		}

		@Override
		public void sendGuarded(UUID observer, CastingPosePackets.Payload payload) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(observer);
			if (player != null) CastingPoseService.sendGuarded(entity, player, payload);
		}
	}
}
