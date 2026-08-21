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
	void shortFactualCommentIsAcceptedWithoutMandatorySignalWords() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				/** Owns the example invariant. */
				public final class Example {
					// The meter shows the player's current value on screen.
					void render() { }
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.genericComments().isEmpty(), result::summary);
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
	void apiContractsCoverMultilineConstructorsNestedInterfacesAndMeaningfulDocs() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Stable integration boundary. */
				public record ExampleApi(String id) {
					/** A comment that says nothing useful. */
					public ExampleApi {
					}
					public boolean
							allows(
								String value
							) { return true; }
					@FunctionalInterface
					public interface Decision {
						boolean decide(String value);
					}
					private static final class Internal {
						public void helper() { }
					}
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of(
				"src/main/java/com/example/api/v1/ExampleApi.java:5",
				"src/main/java/com/example/api/v1/ExampleApi.java:7",
				"src/main/java/com/example/api/v1/ExampleApi.java:13"),
				result.undocumentedPublicContracts());
	}

	@Test
	void overrideMayInheritItsContract() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Stable integration boundary. */
				public final class ExampleApi implements Runnable {
					@Override
					public void run() { }
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.undocumentedPublicContracts().isEmpty(), result::summary);
		assertTrue(result.undocumentedPublicTypes().isEmpty(), result::summary);
	}

	@Test
	void annotatedSameLinePublicTypeStillRequiresDocumentation() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				@Deprecated public final class Example { }
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/Example.java:2"),
				result.undocumentedPublicTypes());
	}

	@Test
	void onlyExactOverrideAnnotationsMayInheritAnApiContract() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Stable integration boundary. */
				public final class ExampleApi implements Runnable {
					@interface CustomOverride { }
					@CustomOverride
					public void missingContract() { }
					@java.lang.Override
					public void run() { }
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/api/v1/ExampleApi.java:5"),
				result.undocumentedPublicContracts());
	}

	@Test
	void packagePrivateNestedTypesDoNotCreatePublicApiContracts() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Stable integration boundary. */
				public final class ExampleApi {
					static final class InternalWorker {
						public void execute() { }
					}
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.undocumentedPublicContracts().isEmpty(), result::summary);
		assertTrue(result.undocumentedPublicTypes().isEmpty(), result::summary);
	}

	@Test
	void interfaceMemberTypesAreImplicitlyPublicApiSurfaces() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Stable integration boundary. */
				public interface ExampleApi {
					/** Executes a validated integration decision. */
					class Decision {
						public void execute() { }
					}
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/api/v1/ExampleApi.java:6"),
				result.undocumentedPublicContracts());
	}

	@Test
	void explicitPublicNestedApiTypesRequireTheirOwnMeaningfulContract() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Stable integration boundary. */
				public final class ExampleApi {
					public static final class Decision {
						/** Reports the validated decision outcome. */
						public boolean allowed() { return true; }
					}
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/api/v1/ExampleApi.java:4"),
				result.undocumentedPublicTypes());
	}

	@Test
	void implicitPublicInterfaceMemberTypesRequireTheirOwnMeaningfulContract() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Stable integration boundary. */
				public interface ExampleApi {
					/** Type. */
					class Decision {
						/** Reports the validated decision outcome. */
						public boolean allowed() { return true; }
					}
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/api/v1/ExampleApi.java:5"),
				result.undocumentedPublicTypes());
	}

	@Test
	void isolatedOutcomeVerbIsNotAMeaningfulApiContract() throws IOException {
		write("src/main/java/com/example/api/v1/ExampleApi.java", """
				package com.example.api.v1;
				/** Stable integration boundary. */
				public interface ExampleApi {
					/** Returns. */
					int state();
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/api/v1/ExampleApi.java:5"),
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
	void commentFindingsReportTheExactOffendingLineInsideMultilineComments() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				/** Owns the example invariant. */
				public final class Example {
					/* Context is retained here.
					 * TODO remove this branch.
					 */
					// Context is retained here.
					// This should never happen.
					void apply() { }
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/Example.java:5"), result.unfinishedMarkers());
		assertEquals(Set.of("src/main/java/com/example/Example.java:8"), result.misleadingComments());
	}

	@Test
	void mechanicalNarrationCannotBorrowWeakIntentTokens() throws IOException {
		write("src/main/java/com/example/Example.java", """
				package com.example;
				/** Owns the example invariant. */
				public final class Example {
					// Apply logic only when needed so it works.
					void apply() { }
					/** Process the value only when requested. */
					void process() { }
				}
				""");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of(
				"src/main/java/com/example/Example.java:4",
				"src/main/java/com/example/Example.java:6"), result.genericComments());
	}

	@Test
	void nearLimitSourceWithIndependentExternallyVisibleOwnersIsRejected() throws IOException {
		StringBuilder source = new StringBuilder("""
				package com.example;
				/** Primary responsibility. */
				public final class Example {
					public static final class IndependentWorker {
						public void execute() { }
					}
				}
				""");
		while (source.toString().lines().count() <= 350) source.append("\n");
		write("src/main/java/com/example/Example.java", source.toString());

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/Example.java (351 lines, 2 externally visible owners)"),
				result.mixedResponsibilityFiles());
	}

	@Test
	void nearLimitSourceMayKeepPrivateCoupledHelpers() throws IOException {
		StringBuilder source = new StringBuilder("""
				package com.example;
				/** Primary responsibility. */
				public final class Example {
					private static final class CoupledHelper {
						void execute() { }
					}
				}
				""");
		while (source.toString().lines().count() <= 350) source.append("\n");
		write("src/main/java/com/example/Example.java", source.toString());

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.mixedResponsibilityFiles().isEmpty(), result::summary);
	}

	@Test
	void nearLimitSourceMayKeepPackagePrivateTopLevelHelpers() throws IOException {
		StringBuilder source = new StringBuilder("""
				package com.example;
				/** Primary responsibility. */
				public final class Example { }
				final class PackageWorker {
					void execute() { }
				}
				""");
		while (source.toString().lines().count() <= 350) source.append("\n");
		write("src/main/java/com/example/Example.java", source.toString());

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.mixedResponsibilityFiles().isEmpty(), result::summary);
	}

	@Test
	void nearLimitSourceMayKeepPackagePrivateNestedBehavioralHelpers() throws IOException {
		StringBuilder source = new StringBuilder("""
				package com.example;
				/** Primary responsibility. */
				public final class Example {
					static final class CoupledWorker {
						void execute() { }
					}
				}
				""");
		while (source.toString().lines().count() <= 350) source.append("\n");
		write("src/main/java/com/example/Example.java", source.toString());

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.mixedResponsibilityFiles().isEmpty(), result::summary);
	}

	@Test
	void nearLimitSourceCountsImplicitlyPublicInterfaceOwners() throws IOException {
		StringBuilder source = new StringBuilder("""
				package com.example;
				/** Primary responsibility. */
				public interface Example {
					/** Executes a validated integration decision. */
					class IndependentWorker {
						void execute() { }
					}
				}
				""");
		while (source.toString().lines().count() <= 350) source.append("\n");
		write("src/main/java/com/example/Example.java", source.toString());

		SourceAudit.Result result = SourceAudit.scan(root);

		assertEquals(Set.of("src/main/java/com/example/Example.java (351 lines, 2 externally visible owners)"),
				result.mixedResponsibilityFiles());
	}

	@Test
	void nearLimitSourceMayExposeDataOnlyNestedRecords() throws IOException {
		StringBuilder source = new StringBuilder("""
				package com.example;
				/** Primary responsibility. */
				public final class Example {
					public record Snapshot(int value) {
						public Snapshot { if (value < 0) throw new IllegalArgumentException(); }
					}
				}
				""");
		while (source.toString().lines().count() <= 350) source.append("\n");
		write("src/main/java/com/example/Example.java", source.toString());

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.mixedResponsibilityFiles().isEmpty(), result::summary);
	}

	@Test
	void nearLimitSourceMayExposeConstructorOnlyDataClasses() throws IOException {
		StringBuilder source = new StringBuilder("""
				package com.example;
				/** Primary responsibility. */
				public final class Example {
					public static final class Snapshot {
						private final int value;
						public Snapshot(int value) { this.value = value; }
					}
				}
				""");
		while (source.toString().lines().count() <= 350) source.append("\n");
		write("src/main/java/com/example/Example.java", source.toString());

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.mixedResponsibilityFiles().isEmpty(), result::summary);
	}

	@Test
	void escapedDelimiterLikeSequenceDoesNotEndATextBlock() throws IOException {
		String delimiter = "\"\"\"";
		String escapedDelimiter = "\\\"" + "\"\"";
		write("src/main/java/com/example/Example.java",
				"package com.example;\n"
						+ "/** Owns the example invariant. */\n"
						+ "public final class Example {\n"
						+ "\tString text = " + delimiter + "\n"
						+ "\t\t" + escapedDelimiter + " // TODO remains text\n"
						+ "\t\t" + delimiter + ";\n"
						+ "}\n");

		SourceAudit.Result result = SourceAudit.scan(root);

		assertTrue(result.unfinishedMarkers().isEmpty(), result::summary);
	}

	private void write(String relative, String source) throws IOException {
		Path destination = root.resolve(relative);
		Files.createDirectories(destination.getParent());
		Files.writeString(destination, source);
	}
}
