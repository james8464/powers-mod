package com.powers.companion;

import java.util.Optional;

/** Single-slot foreground task controller with exact reservation cleanup. */
public final class ShadowTaskController {
	private Active active;
	private record Active(ShadowTask task, ShadowTaskPlan plan, int stepIndex) { }

	public ShadowTask.Result submit(ShadowTask task) {
		if (active != null) return result(ShadowTask.State.REJECTED, "busy", active, 0);
		active = new Active(task, ShadowTaskPlan.forTask(task), 0);
		return result(ShadowTask.State.RUNNING, "accepted", active, 0);
	}

	public ShadowTask.Result tick(long now) {
		if (active == null) return new ShadowTask.Result(ShadowTask.State.COMPLETED,
				"idle", "", 0);
		if (now < active.task().deadline()) return result(ShadowTask.State.RUNNING, "running", active, 0);
		Active expired = active;
		active = null;
		return result(ShadowTask.State.FAILED, "timeout", expired, expired.task().reservedEnergy());
	}

	/** Advances only to the next authored stage and never executes gameplay by itself. */
	public ShadowTask.Result advance() {
		if (active == null) return new ShadowTask.Result(ShadowTask.State.REJECTED,
				"idle", "", 0);
		int next = Math.min(active.stepIndex() + 1, active.plan().steps().size() - 1);
		active = new Active(active.task(), active.plan(), next);
		return result(ShadowTask.State.RUNNING, "advanced", active, 0);
	}

	public ShadowTask.Result complete(String reason) {
		if (active == null) return new ShadowTask.Result(ShadowTask.State.REJECTED,
				"idle", "", 0);
		Active completed = active;
		active = null;
		return result(ShadowTask.State.COMPLETED, reason, completed, completed.task().reservedEnergy());
	}

	public ShadowTask.Result fail(String reason) {
		if (active == null) return new ShadowTask.Result(ShadowTask.State.REJECTED,
				"idle", "", 0);
		Active failed = active;
		active = null;
		return result(ShadowTask.State.FAILED, reason, failed, failed.task().reservedEnergy());
	}

	public ShadowTask.Result cancel(String reason) {
		if (active == null) return new ShadowTask.Result(ShadowTask.State.REJECTED,
				"idle", "", 0);
		Active cancelled = active;
		active = null;
		return result(ShadowTask.State.CANCELLED, reason, cancelled, cancelled.task().reservedEnergy());
	}

	public Optional<ShadowTask> active() {
		return active == null ? Optional.empty() : Optional.of(active.task());
	}

	public int reservedEnergy() { return active == null ? 0 : active.task().reservedEnergy(); }

	/** Current privacy-safe goal/step snapshot for replies and operator diagnostics. */
	public ShadowTaskPlan.Progress progress() {
		return active == null
				? new ShadowTaskPlan.Progress("idle", "idle", 0, 0,
						ShadowTaskPlan.Prerequisite.NONE, ShadowTaskPlan.Cost.NONE,
						ShadowTaskPlan.Rollback.NONE)
				: active.plan().progress(active.stepIndex());
	}

	private static ShadowTask.Result result(ShadowTask.State state, String reason,
			Active active, int released) {
		ShadowTaskPlan.Progress progress = active.plan().progress(active.stepIndex());
		return new ShadowTask.Result(state, reason, active.task().summary(), released,
				progress.stepId(), progress.rollback());
	}
}
