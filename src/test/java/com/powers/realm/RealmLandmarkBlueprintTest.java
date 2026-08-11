package com.powers.realm;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmLandmarkBlueprintTest {
	@Test
	void everyAuthoredLandmarkUsesDistinctBoundedTemplatePieces() {
		Path root = Path.of(System.getProperty("user.dir"));
		for (RealmKind kind : RealmKind.values()) {
			for (MemorySite site : RealmLayout.sites(kind)) {
				var pieces = RealmLandmarkTemplates.pieces(site.id());
				assertFalse(pieces.isEmpty(), site.id());
				assertTrue(pieces.stream().mapToInt(RealmLandmarkTemplates.Piece::blocks).sum()
						<= RealmLandmarkTemplates.MAX_SITE_BLOCKS, site.id());
				assertEquals(pieces.size(), new HashSet<>(pieces.stream()
						.map(RealmLandmarkTemplates.Piece::template).toList()).size());
				for (var piece : pieces) {
					Path resource = root.resolve("src/main/resources/data/powers/structure/")
							.resolve(piece.template().getPath() + ".nbt");
					assertTrue(Files.isRegularFile(resource), resource.toString());
				}
			}
		}
	}

	@Test
	void authoredSitesHaveLootPuzzlesAndProcessorLists() throws Exception {
		Path data = Path.of(System.getProperty("user.dir"), "src/main/resources/data/powers");
		assertTrue(Files.isRegularFile(data.resolve("loot_table/chests/realm_memory.json")));
		for (String alignment : java.util.List.of("light", "dark")) {
			Path processors = data.resolve("worldgen/processor_list/realm/" + alignment + ".json");
			assertTrue(Files.readString(processors).contains("processor_type"));
		}
	}
}
