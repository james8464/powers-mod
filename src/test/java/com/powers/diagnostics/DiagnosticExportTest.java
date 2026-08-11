package com.powers.diagnostics;

import com.powers.audit.OperatorAuditAction;
import com.powers.audit.OperatorAuditEvent;
import com.powers.audit.OperatorAuditLedger;
import com.powers.audit.OperatorAuditResult;
import com.powers.config.ConfigValidationReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticExportTest {
	@Test
	void schemaContainsOnlyVersionedBoundedAggregateData() {
		OperatorAuditLedger ledger = new OperatorAuditLedger();
		ledger.record(new OperatorAuditEvent(OperatorAuditAction.RECOVERY,
				OperatorAuditResult.SUCCESS, "SensitivePlayerName",
				"123e4567-e89b-12d3-a456-426614174001", "secret chat token /host/path"));
		RuntimeDiagnosticSnapshot runtime = new RuntimeDiagnosticSnapshot(
				3, 9, 4, 32, 2, 7, 120, 1, 4096, 512,
				2, 1, 1, 362, 18, 240, 88);
		ConfigValidationReport config = ConfigValidationReport.of(5, List.of(
				new ConfigValidationReport.Entry("wardRadius", ConfigValidationReport.Kind.CLAMPED)));

		String json = DiagnosticExportJson.toJson(DiagnosticExport.create(
				1234, runtime, ledger.snapshot(), config));

		assertTrue(json.contains("\"schemaVersion\": 1"));
		assertTrue(json.contains("\"serverTick\": 1234"));
		assertTrue(json.contains("\"action\": \"RECOVERY\""));
		assertFalse(json.contains("SensitivePlayerName"));
		assertFalse(json.contains("123e4567"));
		assertFalse(json.contains("secret chat"));
		assertFalse(json.contains("/host/path"));
		assertFalse(json.contains("coordinate"));
		assertFalse(json.contains("token"));
	}
}
