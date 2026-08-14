package com.powers.client.acceptance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcceptanceClientScriptTest {
	@Test
	void parsesACompleteOrderedClientScenario() {
		var steps = AcceptanceClientScript.parse(List.of(
				"5\tclose\tscreen",
				"10\trespawn\tnow",
				"20\tcommand\tpowers testing on",
				"30\tkey\tforward on",
				"35\tlook\t0 -8.5",
				"40\tactivate\t0",
				"60\tselect\t0 3",
				"65\tuse\tmain",
				"70\tattack\tCombatTarget",
				"75\tgrimoire\tbook_grimoire_wild 2",
				"80\tcrystal\t4",
				"85\tartifact\tdarkness unique/blight_ground -1",
				"86\tteleport\t0 32.5 101 -48.5 minecraft:overworld",
				"87\tartifact_teleport\tdarkness 48.5 101 -64.5 minecraft:overworld self",
				"88\tlocator\tSoulWitness",
				"90\tchat\tshadow, reveal yourself",
				"100\tscreenshot\tforcefield-break"));

		assertEquals(17, steps.size());
		assertEquals(AcceptanceClientScript.Operation.CLOSE, steps.getFirst().operation());
		assertEquals(AcceptanceClientScript.Operation.KEY, steps.get(3).operation());
		assertEquals(AcceptanceClientScript.Operation.LOOK, steps.get(4).operation());
		assertEquals(AcceptanceClientScript.Operation.ACTIVATE, steps.get(5).operation());
		assertEquals(AcceptanceClientScript.Operation.TELEPORT, steps.get(12).operation());
		assertEquals(AcceptanceClientScript.Operation.ARTIFACT_TELEPORT,
				steps.get(13).operation());
		assertEquals(AcceptanceClientScript.Operation.LOCATOR, steps.get(14).operation());
		assertEquals("forcefield-break", steps.getLast().argument());
	}

	@Test
	void rejectsMalformedUnsafeOrOutOfOrderSteps() {
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tunknown\tvalue")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tactivate\t3 extra")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of(
						"40\tchat\tfirst", "20\tchat\tsecond")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tscreenshot\t../escape")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\trespawn\tlater")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tuse\toffhand")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tattack\t@e[type=zombie]")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tgrimoire\tbook 16")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tcrystal\t256")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tartifact\tdarkness action -2")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of(
						"20\tartifact_teleport\tdarkness NaN 64 0 minecraft:overworld self")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of(
						"20\tartifact_teleport\tneutral 0 64 0 minecraft:overworld self")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of(
						"20\tteleport\t0 NaN 64 0 minecraft:overworld")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of(
						"20\tteleport\t3 0 64 0 minecraft:overworld")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tlocator\t" + "x".repeat(65))));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tclose\twindow")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tkey\tattack on")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tkey\tforward maybe")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tlook\t0 -91")));
		assertThrows(IllegalArgumentException.class,
				() -> AcceptanceClientScript.parse(List.of("20\tlook\tNaN 0")));
	}

	@Test
	void acceptsTheVanillaAdvancementsKeyForLiveScreenEvidence() {
		var steps = AcceptanceClientScript.parse(List.of(
				"20\tkey\tadvancements on",
				"21\tkey\tadvancements off"));

		assertEquals("advancements on", steps.getFirst().argument());
		assertEquals("advancements off", steps.getLast().argument());
	}
}
