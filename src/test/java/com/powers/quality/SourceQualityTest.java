package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

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
}
