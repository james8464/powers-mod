package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceQualityTest {
	@Test
	void everyProductionJavaFileIsAuditedAndPublicContractsAreDocumented() throws IOException {
		SourceAudit.Result audit = SourceAudit.scan(Path.of(System.getProperty("user.dir")));

		assertEquals(audit.productionFiles(), audit.manifestFiles(), audit::summary);
		assertTrue(audit.undocumentedPublicTypes().isEmpty(), audit::summary);
		assertTrue(audit.missingPackageDocumentation().isEmpty(), audit::summary);
	}

	@Test
	void productionSourcesContainNoUnfinishedDebugOrWildcardShortcuts() throws IOException {
		SourceAudit.Result audit = SourceAudit.scan(Path.of(System.getProperty("user.dir")));

		assertTrue(audit.unfinishedMarkers().isEmpty(), audit::summary);
		assertTrue(audit.debugWrites().isEmpty(), audit::summary);
		assertTrue(audit.wildcardImports().isEmpty(), audit::summary);
	}

	@Test
	void responsibilityClassesStayWithinTheReviewedSizeBoundary() throws IOException {
		SourceAudit.Result audit = SourceAudit.scan(Path.of(System.getProperty("user.dir")));

		assertTrue(audit.oversizedFiles().isEmpty(), audit::summary);
	}

	@Test
	void playerTravelPathsNeverSynchronouslyLoadOrGenerateDestinationChunks() throws IOException {
		Path root = Path.of(System.getProperty("user.dir"));
		for (String relative : List.of(
				"src/main/java/com/powers/mind/BodyProxyManager.java",
				"src/main/java/com/powers/realm/RealmConfinementManager.java",
				"src/main/java/com/powers/power/crystals/MiddleworldAbility.java")) {
			String source = Files.readString(root.resolve(relative));
			org.junit.jupiter.api.Assertions.assertFalse(source.contains(".getChunk("), relative);
		}
	}
}
