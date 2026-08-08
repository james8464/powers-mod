package com.powers.progression;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable capped aggregation of every completed rank node. Global values
 * and action/aspect-scoped values remain separate until a cast asks for both.
 */
public record RankProfile(Map<RankPerkType, Double> globalValues,
		Map<String, Map<RankPerkType, Double>> scopedValues,
		Map<String, Double> branchWeights, String focus) {
	public static final RankProfile EMPTY = new RankProfile(Map.of(), Map.of(), Map.of(), "");

	public RankProfile {
		globalValues = Map.copyOf(globalValues);
		scopedValues = scopedValues.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
				Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
		branchWeights = Map.copyOf(branchWeights);
		focus = Objects.requireNonNullElse(focus, "");
	}

	/** Returns the capped global contribution for a mechanical dimension. */
	public double value(RankPerkType type) {
		return globalValues.getOrDefault(type, 0.0);
	}

	/** Returns the capped global plus exact action/aspect contribution. */
	public double value(RankPerkType type, String actionOrAspect) {
		return Math.min(type.cap(), value(type) + scopedValue(type, actionOrAspect));
	}

	/** Returns only the exact action/aspect contribution, excluding global perks. */
	public double scopedValue(RankPerkType type, String actionOrAspect) {
		return scopedValues.getOrDefault(Objects.requireNonNullElse(actionOrAspect, "").toLowerCase(), Map.of())
				.getOrDefault(type, 0.0);
	}

	/** Weighted number of completed nodes in a branch; focused nodes count 1.5. */
	public double branchWeight(String branch) {
		return branchWeights.getOrDefault(branch, 0.0);
	}
}
