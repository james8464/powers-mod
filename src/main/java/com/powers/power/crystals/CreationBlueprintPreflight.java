package com.powers.power.crystals;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Two-phase mutation helper: no write can occur until every target is accepted. */
public final class CreationBlueprintPreflight {
	public enum Result { DENIED, PLACED }

	private CreationBlueprintPreflight() { }

	public static <T> Result placeAtomically(List<T> targets, Predicate<T> mayPlace, Consumer<T> place) {
		if (targets.isEmpty() || targets.stream().anyMatch(mayPlace.negate())) return Result.DENIED;
		targets.forEach(place);
		return Result.PLACED;
	}
}
