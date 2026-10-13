package com.powers.progression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** Bounded server-thread encounter ledger with deterministic one-shot resolution. */
public final class BossContributionLedger {
	private static final int MAX_ENCOUNTERS = 64;
	private static final int MAX_TRACKED_PER_ENCOUNTER = 64;
	private static final int MAX_RESOLVED_ENCOUNTERS = 128;
	private final LinkedHashMap<UUID, Encounter> encounters = new LinkedHashMap<>();
	private final LinkedHashSet<UUID> resolvedEncounters = new LinkedHashSet<>();

	public boolean noteDamage(UUID encounter, UUID player, double amount, long tick) {
		return note(encounter, player, amount, 0.0, 0, tick);
	}

	public boolean noteSupport(UUID encounter, UUID player, double amount, long tick) {
		return note(encounter, player, 0.0, amount, 0, tick);
	}

	public boolean noteControl(UUID encounter, UUID player, int ticks, long tick) {
		return note(encounter, player, 0.0, 0.0, Math.max(0, ticks), tick);
	}

	public boolean hasActivity(UUID encounter, UUID player) {
		Encounter state = encounters.get(encounter);
		return state != null && state.contributors.containsKey(player);
	}

	public List<UUID> resolve(UUID encounter, long tick, Predicate<UUID> active) {
		if (encounter == null || resolvedEncounters.contains(encounter)) return List.of();
		Encounter state = encounters.remove(encounter);
		if (state == null || active == null) return List.of();
		rememberResolved(encounter);
		List<UUID> result = new ArrayList<>();
		state.contributors.entrySet().stream()
				.filter(entry -> entry.getValue().qualifies())
				.filter(entry -> BossContributionRules.recent(entry.getValue().lastTick, tick))
				.map(Map.Entry::getKey)
				.filter(active)
				.sorted()
				.limit(BossContributionRules.MAX_CONTRIBUTORS)
				.forEach(result::add);
		return List.copyOf(result);
	}

	public int activeEncounters() {
		return encounters.size();
	}

	public void clear() {
		encounters.clear();
		resolvedEncounters.clear();
	}

	private boolean note(UUID encounter, UUID player, double damage, double support,
			int controlTicks, long tick) {
		if (encounter == null || player == null || tick < 0 || resolvedEncounters.contains(encounter)
				|| (!BossContributionRules.validAmount(damage)
				&& !BossContributionRules.validAmount(support) && controlTicks <= 0)) return false;
		Encounter state = encounter(encounter);
		Contribution current = state.contributors.get(player);
		if (current == null && state.contributors.size() >= MAX_TRACKED_PER_ENCOUNTER) return false;
		if (current == null) current = new Contribution();
		current.damage = saturating(current.damage, damage);
		current.support = saturating(current.support, support);
		long totalControl = (long) current.controlTicks + controlTicks;
		current.controlTicks = (int) Math.min(Integer.MAX_VALUE, totalControl);
		current.lastTick = Math.max(current.lastTick, tick);
		state.contributors.put(player, current);
		return current.qualifies();
	}

	private Encounter encounter(UUID id) {
		Encounter state = encounters.get(id);
		if (state != null) return state;
		if (encounters.size() >= MAX_ENCOUNTERS) {
			var iterator = encounters.keySet().iterator();
			if (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
			}
		}
		state = new Encounter();
		encounters.put(id, state);
		return state;
	}

	private void rememberResolved(UUID id) {
		if (resolvedEncounters.size() >= MAX_RESOLVED_ENCOUNTERS) {
			var iterator = resolvedEncounters.iterator();
			if (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
			}
		}
		resolvedEncounters.add(id);
	}

	private static double saturating(double current, double addition) {
		if (!BossContributionRules.validAmount(addition)) return current;
		double result = current + addition;
		return Double.isFinite(result) ? result : Double.MAX_VALUE;
	}

	private static final class Encounter {
		private final Map<UUID, Contribution> contributors = new HashMap<>();
	}

	private static final class Contribution {
		private double damage;
		private double support;
		private int controlTicks;
		private long lastTick = -1;

		private boolean qualifies() {
			return BossContributionRules.qualifies(damage, support, controlTicks);
		}
	}
}
