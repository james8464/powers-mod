package com.powers.magic.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * One-shot cast transaction with deterministic phase ordering and LIFO
 * compensation. A failing phase is compensated too, because it may have
 * mutated state immediately before returning false or throwing.
 */
public final class CastTransaction {
	public enum Phase { VALIDATION, COST, EFFECT, COOLDOWN, PRESENCE }

	public record Result(boolean committed, Phase failedPhase, RuntimeException cause) {
	}

	private record Step(Phase phase, BooleanSupplier apply, Runnable rollback) {
	}

	private final List<Step> steps = new ArrayList<>(Phase.values().length);
	private boolean executed;

	public CastTransaction stage(Phase phase, BooleanSupplier apply, Runnable rollback) {
		if (executed) throw new IllegalStateException("Cast transaction already executed");
		Objects.requireNonNull(phase, "phase");
		Objects.requireNonNull(apply, "apply");
		Objects.requireNonNull(rollback, "rollback");
		if (!steps.isEmpty() && steps.getLast().phase().ordinal() >= phase.ordinal()) {
			throw new IllegalArgumentException("Cast phases must be unique and ordered: " + phase);
		}
		steps.add(new Step(phase, apply, rollback));
		return this;
	}

	public Result execute() {
		if (executed) throw new IllegalStateException("Cast transaction already executed");
		executed = true;
		List<Step> attempted = new ArrayList<>(steps.size());
		for (Step step : steps) {
			attempted.add(step);
			try {
				if (step.apply().getAsBoolean()) continue;
				RuntimeException rejected = new IllegalStateException(
						"Cast phase rejected: " + step.phase());
				rollback(attempted, rejected);
				return new Result(false, step.phase(), rejected);
			} catch (RuntimeException failure) {
				rollback(attempted, failure);
				return new Result(false, step.phase(), failure);
			}
		}
		return new Result(true, null, null);
	}

	private static void rollback(List<Step> attempted, RuntimeException failure) {
		for (int index = attempted.size() - 1; index >= 0; index--) {
			try {
				attempted.get(index).rollback().run();
			} catch (RuntimeException rollbackFailure) {
				failure.addSuppressed(rollbackFailure);
			}
		}
	}
}
