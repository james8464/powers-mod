package com.powers.animation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Pure connection/world-epoch state for latency-correct client casting poses. */
public final class ClientCastingPoseState {
	public static final int MAX_ENTRIES = 128;
	private final Map<UUID, CastingPoseEvent> entries = new HashMap<>();
	private final LinkedHashMap<UUID, Long> sequences = new LinkedHashMap<>();
	private final Set<UUID> terminalTombstones = new HashSet<>();
	private WorldIdentity identity;

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
		if (event.startGameTime() > gameTime + CastingPoseRules.MAX_FUTURE_SKEW_TICKS) return false;
		long previous = sequences.getOrDefault(event.entityUuid(), 0L);
		if (event.sequence() <= previous) return false;
		if (!event.terminal() && gameTime >= event.endGameTime()) return false;
		purgeExpired(gameTime);
		ensureCapacity(event.entityUuid());
		sequences.put(event.entityUuid(), event.sequence());
		if (event.terminal()) {
			entries.remove(event.entityUuid());
			terminalTombstones.add(event.entityUuid());
		} else {
			terminalTombstones.remove(event.entityUuid());
			entries.put(event.entityUuid(), event);
		}
		return true;
	}

	public Optional<Resolved> resolve(UUID entityUuid, long gameTime) {
		CastingPoseEvent event = entries.get(entityUuid);
		if (event == null || gameTime >= event.endGameTime()) {
			removeNonTerminal(entityUuid);
			return Optional.empty();
		}
		return Optional.of(new Resolved(event, CastingPoseRules.progress(gameTime, event)));
	}

	public void reset(WorldIdentity world) {
		identity = Objects.requireNonNull(world, "world");
		entries.clear();
		sequences.clear();
		terminalTombstones.clear();
	}

	public void tick(long gameTime) {
		purgeExpired(gameTime);
	}

	public Map<UUID, CastingPoseEvent> entries() {
		return Map.copyOf(entries);
	}

	public int trackedIdentities() {
		return sequences.size();
	}

	public record Wire(int entityId, UUID entityUuid, long sequence, CastingPose pose,
			CastingStyle style, CastingHand hand, long startGameTime, int durationTicks,
			boolean terminal) {
		public Wire {
			new CastingPoseEvent(entityId, entityUuid, sequence, pose, style, hand,
					startGameTime, durationTicks, terminal);
		}

		public CastingPoseEvent event() {
			return new CastingPoseEvent(entityId, entityUuid, sequence, pose, style, hand,
					startGameTime, durationTicks, terminal);
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

	private void purgeExpired(long gameTime) {
		for (UUID uuid : Set.copyOf(entries.keySet())) {
			if (gameTime >= entries.get(uuid).endGameTime()) removeNonTerminal(uuid);
		}
	}

	private void removeNonTerminal(UUID uuid) {
		entries.remove(uuid);
		if (!terminalTombstones.contains(uuid)) sequences.remove(uuid);
	}

	private void ensureCapacity(UUID incoming) {
		if (sequences.containsKey(incoming) || sequences.size() < MAX_ENTRIES) return;
		UUID evict = sequences.keySet().iterator().next();
		sequences.remove(evict);
		entries.remove(evict);
		terminalTombstones.remove(evict);
	}
}
