package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import java.util.List;
	
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreationBlueprintPreflightTest {
	@Test
	void oneDeniedTargetRejectsTheWholeMutationBeforeAnyWrite() {
		var targets = List.of("first", "protected", "third");
		int[] writes = {0};
		var result = CreationBlueprintPreflight.placeAtomically(
				targets,
				target -> !target.equals("protected"),
				target -> writes[0]++);
		assertEquals(CreationBlueprintPreflight.Result.DENIED, result);
		assertEquals(0, writes[0]);
	}

	@Test
	void everyTargetIsWrittenOnlyAfterTheCompletePreflightPasses() {
		var targets = List.of("first", "second", "third");
		int[] writes = {0};
		var result = CreationBlueprintPreflight.placeAtomically(
				targets, ignored -> true, ignored -> writes[0]++);
		assertEquals(CreationBlueprintPreflight.Result.PLACED, result);
		assertEquals(3, writes[0]);
	}
}
