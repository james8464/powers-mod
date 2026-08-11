package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SemanticParticleAuditTest {
	@Test
	void genericMagicPlaceholdersCannotReturnToProductionVisuals() throws Exception {
		List<String> placeholders = List.of("ParticleTypes.END_ROD", "ParticleTypes.REVERSE_PORTAL",
				"ParticleTypes.SOUL,", "ParticleTypes.CLOUD,");
		for (String sourceSet : List.of("src/main/java", "src/client/java")) {
			Path root = Path.of(System.getProperty("user.dir"), sourceSet);
			try (var files = Files.walk(root)) {
				for (Path source : files.filter(path -> path.toString().endsWith(".java"))
						.filter(path -> !path.getFileName().toString().equals("BeamFxStyle.java")).toList()) {
					String text = Files.readString(source);
					for (String placeholder : placeholders) {
						assertFalse(text.contains(placeholder), source + " uses " + placeholder);
					}
				}
			}
		}
	}
}
