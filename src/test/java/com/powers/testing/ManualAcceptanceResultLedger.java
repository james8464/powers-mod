package com.powers.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict, evidence-backed results imported by the generated manual checklist. */
final class ManualAcceptanceResultLedger {
	static final String FILE = "docs/verification/manual-acceptance-results.tsv";

	private ManualAcceptanceResultLedger() {
	}

	static Map<Key, Result> load(Path root) throws IOException {
		Path file = root.resolve(FILE);
		if (!Files.exists(file)) return Map.of();
		Map<Key, Result> results = parse(Files.readAllLines(file));
		Path normalizedRoot = root.toAbsolutePath().normalize();
		for (Result result : results.values()) {
			Path evidence = normalizedRoot.resolve(result.evidence()).normalize();
			if (!evidence.startsWith(normalizedRoot) || !Files.isRegularFile(evidence)) {
				throw new IllegalArgumentException("Missing or unsafe acceptance evidence: "
						+ result.evidence());
			}
		}
		return results;
	}

	static Map<Key, Result> parse(List<String> lines) {
		Map<Key, Result> results = new LinkedHashMap<>();
		for (int index = 0; index < lines.size(); index++) {
			String line = lines.get(index);
			if (line.isBlank() || line.startsWith("#") || line.startsWith("family\t")) continue;
			String[] cells = line.split("\t", -1);
			if (cells.length != 6) throw new IllegalArgumentException(
					"Acceptance result line " + (index + 1) + " must contain six tab-separated cells");
			Key key = new Key(required(cells[0], "family"), required(cells[1], "identity"));
			String outcome = required(cells[2], "outcome");
			if (!outcome.equals("PASS") && !outcome.equals("FAIL")) {
				throw new IllegalArgumentException("Unsupported acceptance outcome: " + outcome);
			}
			Result result = new Result(outcome, required(cells[3], "build"),
					required(cells[4], "evidence"), cells[5].strip());
			if (results.putIfAbsent(key, result) != null) {
				throw new IllegalArgumentException("Duplicate acceptance result: " + key);
			}
		}
		return Map.copyOf(results);
	}

	private static String required(String value, String field) {
		String normalized = value.strip();
		if (normalized.isEmpty()) throw new IllegalArgumentException(
				"Acceptance " + field + " cannot be empty");
		return normalized;
	}

	record Key(String family, String identity) {
		Key {
			family = required(family, "family");
			identity = required(identity, "identity");
		}
	}

	record Result(String outcome, String build, String evidence, String notes) {
		String displayStatus() {
			return outcome + " (" + build + ")";
		}
	}
}
