package com.powers.audit;

import java.util.List;

/** Immutable aggregate with one fixed entry for every action/result pair. */
public record OperatorAuditSnapshot(long total, List<Count> counts) {
	public OperatorAuditSnapshot {
		total = Math.max(0L, total);
		counts = List.copyOf(counts);
	}

	public long count(OperatorAuditAction action, OperatorAuditResult result) {
		return counts.stream().filter(value -> value.action() == action && value.result() == result)
				.mapToLong(Count::count).findFirst().orElse(0L);
	}

	public record Count(OperatorAuditAction action, OperatorAuditResult result, long count) {
		public Count {
			count = Math.max(0L, count);
		}
	}
}
