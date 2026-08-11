package com.powers.companion;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Immutable bounded intent plan; descriptions never grant gameplay authority. */
public record ShadowTaskPlan(String goal, List<Step> steps) {
	public static final int MAX_STEPS = 4;

	/** Server fact that must be revalidated by the executor before this step. */
	public enum Prerequisite {
		OWNER_ELIGIBLE, TARGET_AUTHENTICATED, ITEM_ALLOWLISTED, DROP_LOADED,
		POWER_ENTITY_SAFE, ENERGY_AVAILABLE, PATH_LOADED, NONE
	}

	/** Authoritative subsystem that owns payment; the plan never spends resources itself. */
	public enum Cost {
		NONE, POWER_RUNTIME, CONJURATION_RUNTIME, PATHFINDING_BUDGET
	}

	/** Exact compensating action if execution stops during a step. */
	public enum Rollback {
		NONE, RELEASE_ITEM_CLAIM, REFUND_CONJURATION, CLEAR_POWER_TOGGLE,
		DISENGAGE_TARGET, STOP_NAVIGATION
	}

	public record Step(String id, Prerequisite prerequisite, Cost cost, Rollback rollback) {
		public Step {
			id = bounded(id);
			if (id.isBlank() || prerequisite == null || cost == null || rollback == null) {
				throw new IllegalArgumentException("A Shadow plan step must be explicit");
			}
		}
	}

	public record Progress(String goal, String stepId, int stepIndex, int totalSteps,
			Prerequisite prerequisite, Cost cost, Rollback rollback) {
	}

	private static final Set<ShadowRequest.Kind> EXECUTABLE = Set.copyOf(EnumSet.of(
			ShadowRequest.Kind.ATTACK, ShadowRequest.Kind.DEFEND,
			ShadowRequest.Kind.USE_POWER, ShadowRequest.Kind.STOP_POWER,
			ShadowRequest.Kind.GET_ITEM, ShadowRequest.Kind.CONJURE_ITEM,
			ShadowRequest.Kind.SCOUT, ShadowRequest.Kind.RANGE_PREFERENCE));

	public ShadowTaskPlan {
		goal = bounded(goal);
		steps = List.copyOf(steps);
		if (goal.isBlank() || steps.isEmpty() || steps.size() > MAX_STEPS) {
			throw new IllegalArgumentException("Shadow plan must have one bounded goal and 1..4 steps");
		}
	}

	public static Set<ShadowRequest.Kind> executableKinds() {
		return EXECUTABLE;
	}

	/** Converts a parsed request into descriptive execution stages without authorizing any stage. */
	public static ShadowTaskPlan forTask(ShadowTask task) {
		return switch (task.kind()) {
			case ATTACK, DEFEND -> plan("engage_target",
					step("validate_target", Prerequisite.TARGET_AUTHENTICATED, Cost.NONE,
							Rollback.NONE),
					step("engage", Prerequisite.POWER_ENTITY_SAFE, Cost.POWER_RUNTIME,
							Rollback.DISENGAGE_TARGET));
			case USE_POWER -> plan("cast_power",
					step("validate_power", Prerequisite.POWER_ENTITY_SAFE, Cost.NONE,
							Rollback.NONE),
					step("cast", Prerequisite.ENERGY_AVAILABLE, Cost.POWER_RUNTIME,
							Rollback.CLEAR_POWER_TOGGLE));
			case STOP_POWER -> plan("stop_power",
					step("resolve_toggle", Prerequisite.POWER_ENTITY_SAFE, Cost.NONE,
							Rollback.NONE));
			case GET_ITEM -> plan("retrieve_loaded_drop",
					step("validate_item", Prerequisite.ITEM_ALLOWLISTED, Cost.NONE,
							Rollback.NONE),
					step("locate_drop", Prerequisite.DROP_LOADED, Cost.PATHFINDING_BUDGET,
							Rollback.RELEASE_ITEM_CLAIM),
					step("deliver_drop", Prerequisite.OWNER_ELIGIBLE, Cost.NONE,
							Rollback.RELEASE_ITEM_CLAIM));
			case CONJURE_ITEM -> plan("conjure_bounded_item",
					step("validate_item", Prerequisite.ITEM_ALLOWLISTED, Cost.NONE,
							Rollback.NONE),
					step("channel_darkness", Prerequisite.ENERGY_AVAILABLE,
							Cost.CONJURATION_RUNTIME, Rollback.REFUND_CONJURATION),
					step("deliver_conjuration", Prerequisite.OWNER_ELIGIBLE, Cost.NONE,
							Rollback.REFUND_CONJURATION));
			case SCOUT -> plan("scout_loaded_route",
					step("validate_route", Prerequisite.PATH_LOADED, Cost.NONE,
							Rollback.NONE),
					step("navigate", Prerequisite.OWNER_ELIGIBLE, Cost.PATHFINDING_BUDGET,
							Rollback.STOP_NAVIGATION));
			case RANGE_PREFERENCE -> plan("set_combat_range",
					step("validate_preference", Prerequisite.OWNER_ELIGIBLE, Cost.NONE,
							Rollback.NONE));
			default -> throw new IllegalArgumentException("Not an executable Shadow task: " + task.kind());
		};
	}

	public Progress progress(int index) {
		int selected = Math.clamp(index, 0, steps.size() - 1);
		Step step = steps.get(selected);
		return new Progress(goal, step.id(), selected + 1, steps.size(),
				step.prerequisite(), step.cost(), step.rollback());
	}

	private static ShadowTaskPlan plan(String goal, Step... steps) {
		return new ShadowTaskPlan(goal, List.of(steps));
	}

	private static Step step(String id, Prerequisite prerequisite, Cost cost, Rollback rollback) {
		return new Step(id, prerequisite, cost, rollback);
	}

	private static String bounded(String value) {
		String safe = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT)
				.replaceAll("[^a-z0-9_:-]", "_").replaceAll("_+", "_");
		return safe.substring(0, Math.min(64, safe.length()));
	}
}
