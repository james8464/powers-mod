package com.powers.client.acceptance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcceptanceClientScriptTest {
	@Test
	void parsesACompleteOrderedClientScenario() {
		var steps = AcceptanceClientScript.parse(List.of(
				"10\trespawn\tnow",
				"20\tcommand\tpowers testing on",
				"40\tactivate\t0",
				"60\tselect\t0 3",
				"65\tuse\tmain",
				"70\tattack\tCombatTarget",
				"75\tgrimoire\tbook_grimoire_wild 2",
				"80\tcrystal\t4",
				"85\tartifact\tdarkness unique/blight_ground -1",
				"88\tlocator\tSoulWitness",
				"90\tchat\tshadow, reveal yourself",
				"100\tscreenshot\tforcefield-break"));

		assertEquals(12, steps.size());
		assertEquals(AcceptanceClientScript.Operation.ACTIVATE, steps.get(2).operation());
		assertEquals(AcceptanceClientScript.Operation.LOCATOR, steps.get(9).operation());
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
				() -> AcceptanceClientScript.parse(List.of("20\tlocator\t" + "x".repeat(65))));
	}
}
