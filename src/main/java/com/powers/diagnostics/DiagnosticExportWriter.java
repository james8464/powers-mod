package com.powers.diagnostics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atomically replaces one bounded diagnostic snapshot beneath the world root. */
public final class DiagnosticExportWriter {
	public static final String RELATIVE_PATH = "powers/diagnostics/latest.json";

	public record Result(boolean success, String relativePath, String failureReason) { }

	private DiagnosticExportWriter() {
	}

	public static Result write(Path worldRoot, DiagnosticExport export) {
		Path output = worldRoot.resolve(RELATIVE_PATH);
		Path temporary = output.resolveSibling("latest.json.tmp");
		try {
			Files.createDirectories(output.getParent());
			Files.writeString(temporary, DiagnosticExportJson.toJson(export), StandardCharsets.UTF_8);
			Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
			return new Result(true, RELATIVE_PATH, "");
		} catch (IOException error) {
			try {
				Files.deleteIfExists(temporary);
			} catch (IOException ignored) {
				// Preserve the original bounded failure result.
			}
			return new Result(false, RELATIVE_PATH, "atomic_write_failed");
		}
	}
}
