package com.powers.realm;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmLandmarkBlueprintTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void everyAuthoredLandmarkIsDistinctBoundedAndContainsItsMemoryCore() {
		for (RealmKind kind : RealmKind.values()) {
			for (MemorySite site : RealmLayout.sites(kind)) {
				var blueprint = RealmLandmarkBlueprint.preview(kind, site, -63);
				assertFalse(blueprint.isEmpty(), site.id());
				assertTrue(blueprint.size() <= RealmLandmarkBlueprint.MAX_PLACEMENTS, site.id());
				assertEquals(site.x(), blueprint.getFirst().position().getX(), site.id());
				assertEquals(site.z(), blueprint.getFirst().position().getZ(), site.id());
				assertTrue(blueprint.stream().anyMatch(placement -> placement.position().getX() == site.x()
						&& placement.position().getZ() == site.z()
						&& placement.block() != Blocks.AIR), site.id());
				assertTrue(blueprint.stream().map(RealmBlockPlacement::position).distinct().count()
						== blueprint.size(), "duplicate placement in " + site.id());
			}
		}
	}
}
