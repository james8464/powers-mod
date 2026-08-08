package com.powers.progression;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Pure aggregator that turns unlocked maze nodes into a finite rank profile. */
public final class RankProfileService {
	private static final double FOCUS_MULTIPLIER = 1.5;

	/** Aggregates known completed nodes; stale save identifiers are ignored safely. */
	public RankProfile profile(RankGraph graph, RankProgress progress) {
		Map<RankPerkType, Double> global = new EnumMap<>(RankPerkType.class);
		Map<String, Map<RankPerkType, Double>> scoped = new HashMap<>();
		Map<String, Double> branches = new HashMap<>();
		for (String nodeId : progress.completed()) {
			RankNode node = graph.node(nodeId);
			if (node == null) continue;
			double focusMultiplier = node.id().equals(progress.focus()) ? FOCUS_MULTIPLIER : 1.0;
			branches.merge(node.branch(), focusMultiplier, Double::sum);
			for (RankPerk perk : node.perks()) {
				Map<RankPerkType, Double> destination = perk.actionOrAspect().isEmpty()
						? global : scoped.computeIfAbsent(perk.actionOrAspect(), ignored -> new EnumMap<>(RankPerkType.class));
				destination.compute(perk.type(), (ignored, existing) -> Math.min(perk.type().cap(),
						(existing == null ? 0.0 : existing) + perk.amount() * focusMultiplier));
			}
		}
		return new RankProfile(global, scoped, branches, progress.focus());
	}
}
