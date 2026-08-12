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
	private static final Pattern NUMBERED_MESSAGE = Pattern.compile(
			"PowerMessages\\.(?:send|sendImportant)\\(\\s*[^,]+,\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*(\\d+)");

	@Test
	void everyLiteralClientTranslationKeyExists() throws Exception {
		Path root = Path.of(System.getProperty("user.dir"));
		var language = language(root);
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

	@Test
	void everyLiteralServerTranslationKeyExists() throws Exception {
		Path root = Path.of(System.getProperty("user.dir"));
		var language = language(root);
		var missing = new TreeSet<String>();
		try (var files = Files.walk(root.resolve("src/main/java"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				var matcher = LITERAL.matcher(Files.readString(file));
				while (matcher.find()) {
					String key = matcher.group(1);
					if (!language.has(key)) missing.add(key);
				}
			}
		}
		assertTrue(missing.isEmpty(), () -> "Missing server translations: " + missing);
	}

	@Test
	void everyNumberedPowerMessageHasEveryAdvertisedVariant() throws Exception {
		Path root = Path.of(System.getProperty("user.dir"));
		var language = language(root);
		var missing = new TreeSet<String>();
		try (var files = Files.walk(root.resolve("src/main/java"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				var matcher = NUMBERED_MESSAGE.matcher(Files.readString(file));
				while (matcher.find()) {
					for (int index = 1; index <= Integer.parseInt(matcher.group(2)); index++) {
						String key = matcher.group(1) + "." + index;
						if (!language.has(key)) missing.add(key);
					}
				}
			}
		}
		assertTrue(missing.isEmpty(), () -> "Missing numbered message translations: " + missing);
	}

	@Test
	void cartographerScreenTeachesTheAcceptedQueryGrammar() throws Exception {
		Path root = Path.of(System.getProperty("user.dir"));
		var language = language(root);
		String help = language.get("screen.powers.locator.world_help_1").getAsString();
		String hint = language.get("screen.powers.locator.world_hint").getAsString();
		assertTrue(help.contains("biome <id>"), help);
		assertTrue(help.contains("structure <id>"), help);
		assertTrue(help.contains("landmark <name>"), help);
		assertTrue(hint.length() <= 22, "Locator hint exceeds its 148px edit box: " + hint);
	}

	private static com.google.gson.JsonObject language(Path root) throws Exception {
		return JsonParser.parseString(Files.readString(root.resolve(
				"src/main/resources/assets/powers/lang/en_us.json"))).getAsJsonObject();
	}
}
