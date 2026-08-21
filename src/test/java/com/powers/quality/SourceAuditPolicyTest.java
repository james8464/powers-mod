package com.powers.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceAuditPolicyTest {
	@TempDir Path root;

	@Test
	void genericMechanicalNarrationIsRejectedWithItsLine() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				/** Owns the example invariant. */
				public final class Example {
					void apply() {
						// Apply logic
					}
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/Example.java:5"), result.genericComments());
	}

	@Test
	void shortDescriptiveNarrationIsRejectedEvenWithoutAnImperativeVerb() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				/** Owns the example invariant. */
				public final class Example {
					// The meter shows the player's current value on screen.
					void render() { }
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/Example.java:4"), result.genericComments());
	}

	@Test
	void adjacentInlineCommentsAreAuditedIndependently() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				/** Owns the example invariant. */
				public final class Example {
					int first = 1; // First value
					int second = 2; // Second value
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of(
				"src/main/java/com/example/Example.java:4",
				"src/main/java/com/example/Example.java:5"), result.genericComments());
	}

	@Test
	void intentAndInvariantCommentsRemainAccepted() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				/** Owns the example invariant. */
				public final class Example {
					void apply() {
						// Validate before payment so a denied action cannot consume energy.
					}
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.genericComments().isEmpty(), result::summary);
		assertTrue(result.misleadingComments().isEmpty(), result::summary);
	}

	@Test
	void staleMarkersAreReadFromCommentsButNotStringLiterals() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				/** Owns the example invariant. */
				public final class Example {
					String diagnostic = "TODO is valid player text";
					// FIXME replace the temporary branch
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/Example.java:5"), result.unfinishedMarkers());
	}

	@Test
	void unsupportedCertaintyClaimsAreRejected() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				/** Owns the example invariant. */
				public final class Example {
					// This should never happen.
					void apply() { }
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/Example.java:4"), result.misleadingComments());
	}

	@Test
	void callablePublicApiMembersRequireContracts() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Public integration boundary. */
				public interface ExampleApi {
					boolean mutate(String value);
					/** Reports state without mutation. */
					int state();
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/api/v1/ExampleApi.java:4"),
				result.undocumentedPublicContracts());
	}

	@Test
	void inlinePublicApiMethodsStillRequireContracts() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Public integration boundary. */
				public final class ExampleApi {
					public static ExampleApi global() { return null; }
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/api/v1/ExampleApi.java:4"),
				result.undocumentedPublicContracts());
	}

	@Test
	void nearLimitSourceWithIndependentTopLevelTypesIsRejected() throws IOException {
		StringBuilder source = new StringBuilder("""
				package com.example;
				/** Primary responsibility. */
				public final class Example { }
				final class SeparateOwner { }
				""");
		while (source.toString().lines().count() <= 350) source.append("\n");
		write("src/main/java/com/example/Example.java", source.toString());

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/Example.java (351 lines, 2 top-level types)"),
				result.mixedResponsibilityFiles());
	}

	private void write(String relative, String source) throws IOException {
		Path destination = root.resolve(relative);
		Files.createDirectories(destination.getParent());
		Files.writeString(destination, source);
	}
}
