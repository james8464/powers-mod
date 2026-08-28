package com.powers.animation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

/** Pure connection/world-epoch state for latency-correct client casting poses. */
public final class ClientCastingPoseState {
	public static final int MAX_ENTRIES = 128;
	private final Map<UUID, CastingPoseEvent> entries = new HashMap<>();
	private WorldIdentity identity;

	public ClientCastingPoseState() {
	}

	public boolean accept(Wire wire, HandlerStamp captured, WorldIdentity world,
			EntityIdentity entity, long gameTime) {
		Objects.requireNonNull(wire, "wire");
		Objects.requireNonNull(captured, "captured");
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(entity, "entity");
		if (gameTime < 0 || captured.connectionEpoch() != world.connectionEpoch()
				|| captured.worldEpoch() != world.worldEpoch()) return false;
		if (identity == null) identity = world;
		if (!identity.equals(world) || !entity.scoped() || entity.entityId() != wire.entityId()
				|| !entity.entityUuid().equals(wire.entityUuid())) return false;
		CastingPoseEvent event;
		try {
			event = wire.event();
		} catch (RuntimeException invalid) {
			return false;
		}
		if (event.startGameTime() > gameTime + CastingPoseRules.MAX_FUTURE_SKEW_TICKS
				|| !CastingPoseRules.active(gameTime, event)) return false;
		CastingPoseEvent existing = entries.get(event.entityUuid());
		if (existing != null && event.sequence() <= existing.sequence()) return false;
		entries.entrySet().removeIf(entry -> !CastingPoseRules.active(gameTime, entry.getValue()));
		if (existing == null && entries.size() >= MAX_ENTRIES) {
			UUID evict = entries.entrySet().stream().min(Comparator
					.comparingLong((Map.Entry<UUID, CastingPoseEvent> entry) -> finish(entry.getValue()))
					.thenComparing(entry -> entry.getKey().toString()))
					.map(Map.Entry::getKey).orElse(null);
			if (evict != null) entries.remove(evict);
		}
		entries.put(event.entityUuid(), event);
		return true;
	}

	public Optional<Resolved> resolve(UUID entityUuid, long gameTime) {
		CastingPoseEvent event = entries.get(entityUuid);
		if (event == null || !CastingPoseRules.active(gameTime, event)) {
			entries.remove(entityUuid);
			return Optional.empty();
		}
		return Optional.of(new Resolved(event, CastingPoseRules.progress(gameTime, event)));
	}

	public void reset(WorldIdentity world) {
		identity = Objects.requireNonNull(world, "world");
		entries.clear();
	}

	public void tick(long gameTime) {
		entries.entrySet().removeIf(entry -> !CastingPoseRules.active(gameTime, entry.getValue()));
	}

	public Map<UUID, CastingPoseEvent> entries() {
		return Map.copyOf(entries);
	}

	public record Wire(int entityId, UUID entityUuid, long sequence, CastingPose pose,
			CastingStyle style, CastingHand hand, long startGameTime, int durationTicks) {
		public Wire {
			new CastingPoseEvent(entityId, entityUuid, sequence, pose, style, hand,
					startGameTime, durationTicks);
		}

		public CastingPoseEvent event() {
			return new CastingPoseEvent(entityId, entityUuid, sequence, pose, style, hand,
					startGameTime, durationTicks);
		}
	}

	public record HandlerStamp(long connectionEpoch, long worldEpoch) {
		public HandlerStamp {
			if (connectionEpoch < 0 || worldEpoch < 0) throw new IllegalArgumentException("epoch");
		}
	}

	public record WorldIdentity(long connectionEpoch, long worldEpoch) {
		public WorldIdentity {
			if (connectionEpoch < 0 || worldEpoch < 0) throw new IllegalArgumentException("epoch");
		}
	}

	public record EntityIdentity(int entityId, UUID entityUuid, boolean scoped) {
		public EntityIdentity {
			if (entityId < 0 || entityUuid == null) throw new IllegalArgumentException("entity");
		}
	}

	public record Resolved(CastingPoseEvent event, double progress) {
		public Resolved {
			Objects.requireNonNull(event, "event");
			if (!Double.isFinite(progress) || progress < 0 || progress > 1) {
				throw new IllegalArgumentException("progress");
			}
		}
	}

	private static long finish(CastingPoseEvent event) {
		return event.startGameTime() + event.durationTicks();
	}
}
