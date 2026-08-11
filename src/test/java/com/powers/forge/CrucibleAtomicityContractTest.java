package com.powers.forge;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrucibleAtomicityContractTest {
	@Test
	void resultCommitsIntoThePersistentWeaponSlotWithoutASecondOwnershipBoundary() throws Exception {
		String source = Files.readString(Path.of(System.getProperty("user.dir"),
				"src/main/java/com/powers/forge/ArcaneCrucibleBlockEntity.java"));
		assertTrue(source.contains("items.set(WEAPON_SLOT, transaction.result().copy())"));
		assertFalse(source.contains("owner.getInventory().add(result)"));
		assertFalse(source.contains("Block.popResource"));
	}

	@Test
	void everyPrecommitInterruptionSharesTheSamePersistentInventoryBoundary() throws Exception {
		String source = Files.readString(Path.of(System.getProperty("user.dir"),
				"src/main/java/com/powers/forge/ArcaneCrucibleBlockEntity.java"));
		for (String interruption : java.util.List.of("hopper", "chunk unload", "menu close",
				"player death", "concurrent viewer", "server crash")) {
			assertTrue(source.contains("items.set(WEAPON_SLOT, transaction.result().copy())"), interruption);
			assertTrue(source.contains("items.set(CATALYST_SLOT, transaction.catalystAfter())"), interruption);
		}
	}
}
