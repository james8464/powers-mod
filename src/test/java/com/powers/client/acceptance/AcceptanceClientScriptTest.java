package com.powers.client.acceptance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcceptanceClientScriptTest {
	@Test
	void parsesACompleteOrderedClientScenario() {
		var steps = AcceptanceClientScript.parse(List.of(
				"20\tcommand\tpowers testing on",
				"40\tactivate\t0",
				"60\tselect\t0 3",
				"80\tchat\tshadow, reveal yourself",
				"100\tscreenshot\tforcefield-break"));

		assertEquals(5, steps.size());
		assertEquals(AcceptanceClientScript.Operation.ACTIVATE, steps.get(1).operation());
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
	}
}
