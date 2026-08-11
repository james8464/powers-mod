package com.powers.audit;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Fixed-size in-memory aggregation for privileged events. */
public final class OperatorAuditLedger {
	private final EnumMap<OperatorAuditAction, EnumMap<OperatorAuditResult, Long>> counts =
			new EnumMap<>(OperatorAuditAction.class);
	private long total;

	public OperatorAuditLedger() {
		for (OperatorAuditAction action : OperatorAuditAction.values()) {
			EnumMap<OperatorAuditResult, Long> results = new EnumMap<>(OperatorAuditResult.class);
			for (OperatorAuditResult result : OperatorAuditResult.values()) results.put(result, 0L);
			counts.put(action, results);
		}
	}

	public synchronized void record(OperatorAuditEvent event) {
		EnumMap<OperatorAuditResult, Long> results = counts.get(event.action());
		results.put(event.result(), increment(results.get(event.result())));
		total = increment(total);
	}

	public synchronized OperatorAuditSnapshot snapshot() {
		List<OperatorAuditSnapshot.Count> copy = new ArrayList<>(
				OperatorAuditAction.values().length * OperatorAuditResult.values().length);
		for (OperatorAuditAction action : OperatorAuditAction.values()) {
			for (OperatorAuditResult result : OperatorAuditResult.values()) {
				copy.add(new OperatorAuditSnapshot.Count(action, result, counts.get(action).get(result)));
			}
		}
		return new OperatorAuditSnapshot(total, copy);
	}

	public synchronized void clear() {
		for (var results : counts.values()) {
			for (OperatorAuditResult result : OperatorAuditResult.values()) results.put(result, 0L);
		}
		total = 0L;
	}

	private static long increment(long value) {
		return value == Long.MAX_VALUE ? value : value + 1L;
	}
}
