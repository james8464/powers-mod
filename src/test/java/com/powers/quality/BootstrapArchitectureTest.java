package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the Fabric entrypoint a stable facade instead of a gameplay god class. */
class BootstrapArchitectureTest {
	private static final Path ENTRYPOINT = Path.of("src/main/java/com/powers/PowersMod.java");

	@Test
	void fabricEntrypointDelegatesRegistrationAndLifecycleWiring() throws IOException {
		String source = Files.readString(Path.of(System.getProperty("user.dir")).resolve(ENTRYPOINT));
		assertTrue(source.contains("PowersBootstrap.initialize()"),
				"content registration belongs to PowersBootstrap");
		assertTrue(source.contains("PowersServerLifecycle.initialize()"),
				"server event wiring belongs to PowersServerLifecycle");
		for (String eventApi : List.of("ServerTickEvents", "ServerLifecycleEvents",
				"ServerPlayConnectionEvents", "ServerPlayerEvents", "ServerMessageEvents")) {
			assertFalse(source.contains(eventApi), () -> eventApi + " leaked into the Fabric facade");
		}
	}

	@Test
	void fabricEntrypointStaysWithinFacadeSize() throws IOException {
		Path source = Path.of(System.getProperty("user.dir")).resolve(ENTRYPOINT);
		long lines;
		try (var stream = Files.lines(source)) {
			lines = stream.count();
		}
		assertTrue(lines <= 140, () -> "PowersMod is " + lines + " lines; facade limit is 140");
	}
}
