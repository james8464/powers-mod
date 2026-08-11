package com.powers.diagnostics;

import com.powers.audit.OperatorAuditAction;
import com.powers.audit.OperatorAuditResult;
import com.powers.audit.OperatorAuditSnapshot;
import com.powers.config.ConfigValidationReport;

import java.util.List;

/** Schema-versioned aggregate-only diagnostic document. */
public record DiagnosticExport(int schemaVersion, long serverTick, RuntimeDiagnosticSnapshot runtime,
		AuditAggregate operatorAudit, ConfigAggregate configValidation) {
	public static final int CURRENT_SCHEMA_VERSION = 1;

	public static DiagnosticExport create(long serverTick, RuntimeDiagnosticSnapshot runtime,
			OperatorAuditSnapshot audit, ConfigValidationReport config) {
		List<AuditCount> counts = audit.counts().stream()
				.map(value -> new AuditCount(value.action(), value.result(), value.count())).toList();
		return new DiagnosticExport(CURRENT_SCHEMA_VERSION, Math.max(0L, serverTick), runtime,
				new AuditAggregate(audit.total(), counts),
				new ConfigAggregate(config.revision(), config.adjustments(),
						config.entries().size(), config.dropped()));
	}

	public record AuditAggregate(long total, List<AuditCount> counts) {
		public AuditAggregate {
			total = Math.max(0L, total);
			counts = List.copyOf(counts);
		}
	}

	public record AuditCount(OperatorAuditAction action, OperatorAuditResult result, long count) {
		public AuditCount {
			count = Math.max(0L, count);
		}
	}

	public record ConfigAggregate(long revision, int adjustments, int retained, int dropped) { }
}
