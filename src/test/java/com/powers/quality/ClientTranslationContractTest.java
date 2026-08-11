package com.powers.quality;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents literal client labels from silently rendering as raw translation keys. */
class ClientTranslationContractTest {
	private static final Pattern LITERAL = Pattern.compile(
			"Component\\.translatable(?:WithFallback)?\\(\\\"([^\\\"]+)\\\"\\s*[,)]");

	@Test
	void everyLiteralClientTranslationKeyExists() throws Exception {
		Path root = Path.of(System.getProperty("user.dir"));
		var language = JsonParser.parseString(Files.readString(root.resolve(
				"src/main/resources/assets/powers/lang/en_us.json"))).getAsJsonObject();
		var missing = new TreeSet<String>();
		try (var files = Files.walk(root.resolve("src/client/java"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				var matcher = LITERAL.matcher(Files.readString(file));
				while (matcher.find()) {
					String key = matcher.group(1);
					if (!language.has(key)) missing.add(key);
				}
			}
		}
		assertTrue(missing.isEmpty(), () -> "Missing client translations: " + missing);
	}
}
