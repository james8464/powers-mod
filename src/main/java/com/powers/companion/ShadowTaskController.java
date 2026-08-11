package com.powers.companion;

import java.util.Optional;

/** Single-slot foreground task controller with exact reservation cleanup. */
public final class ShadowTaskController {
	private ShadowTask active;

	public ShadowTask.Result submit(ShadowTask task) {
		if (active != null) return result(ShadowTask.State.REJECTED, "busy", active, 0);
		active = task;
		return result(ShadowTask.State.RUNNING, "accepted", task, 0);
	}

	public ShadowTask.Result tick(long now) {
		if (active == null) return new ShadowTask.Result(ShadowTask.State.COMPLETED,
				"idle", "", 0);
		if (now < active.deadline()) return result(ShadowTask.State.RUNNING, "running", active, 0);
		ShadowTask expired = active;
		active = null;
		return result(ShadowTask.State.FAILED, "timeout", expired, expired.reservedEnergy());
	}

	public ShadowTask.Result complete(String reason) {
		if (active == null) return new ShadowTask.Result(ShadowTask.State.REJECTED,
				"idle", "", 0);
		ShadowTask completed = active;
		active = null;
		return result(ShadowTask.State.COMPLETED, reason, completed, completed.reservedEnergy());
	}

	public ShadowTask.Result fail(String reason) {
		if (active == null) return new ShadowTask.Result(ShadowTask.State.REJECTED,
				"idle", "", 0);
		ShadowTask failed = active;
		active = null;
		return result(ShadowTask.State.FAILED, reason, failed, failed.reservedEnergy());
	}

	public ShadowTask.Result cancel(String reason) {
		if (active == null) return new ShadowTask.Result(ShadowTask.State.REJECTED,
				"idle", "", 0);
		ShadowTask cancelled = active;
		active = null;
		return result(ShadowTask.State.CANCELLED, reason, cancelled, cancelled.reservedEnergy());
	}

	public Optional<ShadowTask> active() { return Optional.ofNullable(active); }
	public int reservedEnergy() { return active == null ? 0 : active.reservedEnergy(); }

	private static ShadowTask.Result result(ShadowTask.State state, String reason,
			ShadowTask task, int released) {
		return new ShadowTask.Result(state, reason, task.summary(), released);
	}
}
