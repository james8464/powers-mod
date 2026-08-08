package com.powers.power.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reference counts overlapping freeze casters without storing Minecraft objects. */
public final class OwnedFreezeIndex {
	private final Map<UUID, Set<UUID>> owners = new HashMap<>();

	/** Claims an entity and returns true only for its first owner. */
	public boolean claim(UUID entity, UUID owner) {
		Set<UUID> current = owners.computeIfAbsent(entity, ignored -> new HashSet<>());
		boolean first = current.isEmpty();
		current.add(owner);
		return first;
	}

	/** Releases one owner and returns true only when the entity now has no owners. */
	public boolean release(UUID entity, UUID owner) {
		Set<UUID> current = owners.get(entity);
		if (current == null || !current.remove(owner) || !current.isEmpty()) return false;
		owners.remove(entity);
		return true;
	}

	public boolean isClaimed(UUID entity) {
		return owners.containsKey(entity);
	}

	public void clear() {
		owners.clear();
	}
}
