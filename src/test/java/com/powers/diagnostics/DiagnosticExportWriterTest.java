package com.powers.diagnostics;

import com.powers.audit.OperatorAuditLedger;
import com.powers.config.ConfigValidationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticExportWriterTest {
	@TempDir
	Path world;

	@Test
	void writeAtomicallyReplacesOneBoundedWorldOwnedSnapshot() throws Exception {
		RuntimeDiagnosticSnapshot firstRuntime = new RuntimeDiagnosticSnapshot(
				1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17);
		DiagnosticExport first = DiagnosticExport.create(10, firstRuntime,
				new OperatorAuditLedger().snapshot(), ConfigValidationReport.empty());
		DiagnosticExportWriter.Result firstResult = DiagnosticExportWriter.write(world, first);

		assertTrue(firstResult.success());
		assertEquals("powers/diagnostics/latest.json", firstResult.relativePath());
		Path output = world.resolve(firstResult.relativePath());
		assertTrue(Files.isRegularFile(output));
		assertFalse(Files.exists(output.resolveSibling("latest.json.tmp")));

		RuntimeDiagnosticSnapshot secondRuntime = new RuntimeDiagnosticSnapshot(
				2, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17);
		DiagnosticExport second = DiagnosticExport.create(11, secondRuntime,
				new OperatorAuditLedger().snapshot(), ConfigValidationReport.empty());
		assertTrue(DiagnosticExportWriter.write(world, second).success());
		String json = Files.readString(output);
		assertTrue(json.contains("\"serverTick\": 11"));
		assertFalse(json.contains("\"serverTick\": 10"));
	}
}
