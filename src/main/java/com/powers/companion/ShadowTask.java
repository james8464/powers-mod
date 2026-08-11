package com.powers.companion;

/** Save-safe description of one foreground task; it deliberately contains no entity handles. */
public record ShadowTask(ShadowRequest.Kind kind, String subject, int count, long startedAt,
		long deadline, int reservedEnergy) {
	public static final int MAX_SUMMARY_LENGTH = 192;

	public enum State { RUNNING, COMPLETED, FAILED, CANCELLED, REJECTED }
	public record Result(State state, String reason, String summary, int releasedEnergy) {
		public Result {
			reason = bounded(reason, 64);
			summary = bounded(summary, MAX_SUMMARY_LENGTH);
			releasedEnergy = Math.max(0, releasedEnergy);
		}
		public boolean accepted() { return state != State.REJECTED; }
	}

	public ShadowTask {
		kind = kind == null ? ShadowRequest.Kind.CONVERSE : kind;
		subject = bounded(subject, 128);
		count = Math.clamp(count, 1, 64);
		startedAt = Math.max(0L, startedAt);
		deadline = Math.max(startedAt + 1L, deadline);
		reservedEnergy = Math.max(0, reservedEnergy);
	}

	public static ShadowTask create(ShadowRequest.Kind kind, String subject, int count,
			long startedAt, long deadline, int reservedEnergy) {
		return new ShadowTask(kind, subject, count, startedAt, deadline, reservedEnergy);
	}

	public String summary() {
		return bounded(kind.name().toLowerCase() + ":" + subject + ":" + count, MAX_SUMMARY_LENGTH);
	}

	private static String bounded(String value, int maximum) {
		if (value == null) return "";
		String safe = value.replaceAll("[^a-zA-Z0-9_:. /-]", "").strip();
		return safe.substring(0, Math.min(maximum, safe.length()));
	}
}
